/*
 * Copyright 2015. Appsi Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appsimobile.appsii.module.weather;

import android.Manifest;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.v4.util.CircularArray;
import android.support.v4.util.SimpleArrayMap;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.WindowManager;

import com.android.volley.VolleyError;
import com.appsimobile.appsii.BitmapUtils;
import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.module.home.WeatherFragment;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.weather.loader.CantGetWeatherException;
import com.appsimobile.appsii.module.weather.loader.WeatherData;
import com.appsimobile.appsii.module.weather.loader.YahooWeatherApiClient;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.appsii.preference.PreferenceHelper;
import com.appsimobile.util.ArrayUtils;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * Created by Nick on 19/02/14.
 * Please note: This service is running in a different process. So do not
 * use anything like shared-preferences, or anything backed by
 * shared-preferences.
 * <p/>
 */
public class WeatherLoadingService {

    public static final int MAX_PHOTO_COUNT = 2;

    public static final String PREFERENCE_LAST_KNOWN_WOEID = "last_known_woeid";

    public static final String PREFERENCE_LAST_UPDATED_MILLIS =
            BuildConfig.APPLICATION_ID + ".last_updated_millis";

    public static final String EXTRA_INCLUDE_WOEID = BuildConfig.APPLICATION_ID + ".with_woeid";

    public static final String EXTRA_UNIT = BuildConfig.APPLICATION_ID + ".unit";

    public static final String ACTION_WEATHER_UPDATED =
            BuildConfig.APPLICATION_ID + ".weather_updated";
    final Context mContext;
    @Inject
    HomeItemConfiguration mConfigurationHelper;
    @Inject
    SharedPreferences mPreferences;
    @Inject
    PreferenceHelper mPreferenceHelper;
    @Inject
    ConnectivityManager mConnectivityManager;
    @Inject
    LocationManager mLocationManager;
    @Inject
    PermissionUtils mPermissionUtils;
    @Inject
    BitmapUtils mBitmapUtils;
    @Inject
    WindowManager mWindowManager;
    WeatherUtils mWeatherUtils;

    public WeatherLoadingService(Context context) {
        mContext = context.getApplicationContext();
        AppInjector.inject(this);

    }

    /**
     * Returns true when the interval to request a sync has been expired.
     * Normally this is determined in the sync adapter mechanism itself.
     * But if it decides to stop syncing correctly, this method can
     * determine if now would be a good time to call
     * {@link ContentResolver#requestSync(Account, String, Bundle)} to
     * make sure the weather data is up to date.
     * <p/>
     * Returns true when now is a good time to update the weatherdata.
     */
    public static boolean hasTimeoutExpired(SharedPreferences preferences) {

        long lastUpdate = preferences.getLong(PREFERENCE_LAST_UPDATED_MILLIS, 0);

        long timePassedMillis = System.currentTimeMillis() - lastUpdate;
        long minutesPassed = timePassedMillis / DateUtils.MINUTE_IN_MILLIS;

        return minutesPassed > 45;
    }

    static void bailOut(String reason) {
        Log.i("WeatherLoadingService", "not updating weather for reason: " + reason);
    }

    /**
     * Downloads the header images for the given woeid and weather-data. Failure is considered
     * non-fatal.
     *
     * @throws VolleyError
     */
    public static void downloadWeatherImages(Context context, BitmapUtils bitmapUtils,
            String woeid, WeatherData weatherData, String timezone) throws VolleyError {

        WindowManager windowManager = AppInjector.provideWindowManager();

        // first we need to determine if it is day or night.
        // TODO: this needs the timezone


        if (timezone == null) {
            timezone = TimeZone.getDefault().getID();
        }

        WeatherUtils weatherUtils = AppInjector.provideWeatherUtils();
        boolean isDay = weatherUtils.isDay(timezone, weatherData);
        ImageDownloadHelper downloadHelper = ImageDownloadHelper.getInstance(context);

        // call into the download-helper this will return a json object with
        // city photos matching the current weather condition.
        JSONObject photos = downloadHelper.searchCityWeatherPhotos(
                woeid, weatherData.nowConditionCode, isDay);

        // Now we need the screen dimension to know which photos have a usable size.
        int dimen = getMaxScreenDimension(windowManager);

        // determine the photos that can be used.
        List<ImageDownloadHelper.PhotoInfo> result = new ArrayList<>();
        ImageDownloadHelper.getEligiblePhotosFromResponse(photos, result, dimen);

        // when no usable photos have been found try photos at the city level with
        // no weather condition info.
        if (result.isEmpty()) {
            photos = downloadHelper.searchCityImage(woeid);
            ImageDownloadHelper.getEligiblePhotosFromResponse(photos, result, dimen);
            // when still no photo was found, clear the existing photos and return
            if (result.isEmpty()) {
                weatherUtils.clearCityPhotos(context, woeid, 0);
                return;
            }
        }

        // Now determine the amount of photos we should download
        int N = Math.min(MAX_PHOTO_COUNT, result.size());
        // idx keeps the index of the actually downloaded photo count
        int idx = 0;
        // note the idx < N instead of i < N.
        // this loop must continue until the amount is satisfied.
        for (int i = 0; idx < N; i++) {
            // quit when the end of the list is reached
            if (i >= result.size()) break;

            // try to download the photo details from the webservice.
            ImageDownloadHelper.PhotoInfo info = result.get(i);
            JSONObject photoInfo = downloadHelper.loadPhotoInfo(context, info.id);
            if (photoInfo != null) {

                // we need to know if the photo is rotated. If so, we need to apply this
                // rotation after download.
                int rotation = ImageDownloadHelper.getRotationFromJson(photoInfo);
                if (downloadFile(context, info, woeid, idx)) {
                    // Apply rotation when non zero
                    if (rotation != 0) {
                        File cacheDir = weatherUtils.getWeatherPhotoCacheDir(context);
                        String fileName = weatherUtils.createPhotoFileName(woeid, idx);
                        File photoImage = new File(cacheDir, fileName);
                        Bitmap bitmap =
                                bitmapUtils.decodeSampledBitmapFromFile(photoImage, dimen, dimen);
                        if (bitmap == null) {
                            Log.wtf("WeatherLoadingService", "error decoding bitmap");
                            continue;
                        }

                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                bitmap.getHeight(),
                                matrix, false);
                        weatherUtils.saveBitmap(context, bitmap, woeid, idx);
                    }
                    // success, handle the next one.
                    idx++;
                }
            }
        }
        // remove photos at higher indexes than the amount downloaded.
        weatherUtils.clearCityPhotos(context, woeid, idx + 1);

    }

    private static int getMaxScreenDimension(WindowManager windowManager) {
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        int dimen = Math.max(point.x, point.y);
        dimen = (dimen * 3) / 4;
        return dimen;
    }

    private static boolean downloadFile(Context context,
            ImageDownloadHelper.PhotoInfo photoInfo, String woeid, int idx) {

        WeatherUtils weatherUtils = AppInjector.provideWeatherUtils();

        File cacheDir = weatherUtils.getWeatherPhotoCacheDir(context);
        String fileName = weatherUtils.createPhotoFileName(woeid, idx);
        File photoImage = new File(cacheDir, fileName);
        try {
            URL url = new URL(photoInfo.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            InputStream in = new BufferedInputStream(connection.getInputStream());
            try {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(photoImage));

                int totalRead = 0;
                try {
                    byte[] bytes = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(bytes)) != -1) {
                        out.write(bytes, 0, read);
                        totalRead += read;
                    }
                    out.flush();
                } finally {
                    out.close();
                }
                if (BuildConfig.DEBUG) {
                    Log.d("WeatherLoadingService",
                            "received " + totalRead + " bytes for: " + photoInfo.url);
                }
            } finally {
                in.close();
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // if e.g. the location was changed, this is a forced update.
    void doSync(String defaultUnit, String extraWoeid, SyncResult result) {

        if (defaultUnit == null) throw new IllegalArgumentException("defaultUnit == null");

        NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();

        boolean online = netInfo != null && netInfo.isConnected();


        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "Handling sync");

        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "Checking online");
        if (!online) {
            bailOut("No network connection");
            result.stats.numIoExceptions++;
            return;
        }
        boolean syncWhenRoaming = mPreferenceHelper.getSyncWhenRoaming();
        if (netInfo.isRoaming() && !syncWhenRoaming) {
            bailOut("Not syncing because of roaming connection");
            result.stats.numIoExceptions++;
            return;
        }

        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "- Checking online");

        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "get woeids");
        String[] woeids = mConfigurationHelper.getWeatherWidgetWoeids(
                WeatherFragment.PREFERENCE_WEATHER_WOEID);
        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "- get woeids");

        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "extra woeids");
        if (extraWoeid != null) {
            int length = woeids.length;

            String[] temp = new String[length + 1];
            System.arraycopy(woeids, 0, temp, 0, length);
            temp[length] = extraWoeid;
            woeids = temp;
        }
        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "- extra woeids");

        if (woeids.length == 0) {
            bailOut("Not syncing because there are no woeids");
            // tell the service to reschedule normally
            result.stats.numUpdates++;
            return;
        }

        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "find timezones");
        int N = woeids.length;
        SimpleArrayMap<String, String> woeidTimezones = new SimpleArrayMap<>(N);
        for (int i = 0; i < N; i++) {
            String woeid = woeids[i];
            long cellId = mConfigurationHelper.findCellWithPropertyValue(
                    WeatherFragment.PREFERENCE_WEATHER_WOEID, woeid);
            if (cellId != -1) {
                String timezone = mConfigurationHelper.
                        getProperty(cellId, WeatherFragment.PREFERENCE_WEATHER_TIMEZONE, null);
                if (BuildConfig.DEBUG) {
                    Log.d("WeatherLoadingService",
                            "woeid -> timezone: " + woeid + " -> " + timezone);
                }
                woeidTimezones.put(woeid, timezone);
            }
        }
        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "- find timezones");

        try {
            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "request location");
            Location location;

            if (mPermissionUtils.holdsPermission(
                    mContext, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                location = requestLocationInfoBlocking();
            } else {
                woeids = addFallbackWoeid(woeids, woeidTimezones);
                location = null;
            }
            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "- request location");


            SimpleArrayMap<String, WeatherData> previousData = new SimpleArrayMap<>(woeids.length);
            for (String woeid : woeids) {
                WeatherData data = mWeatherUtils.getWeatherData(mContext, woeid);
                previousData.put(woeid, data);
            }

            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "load data");
            WeatherDataLoader loader = new WeatherDataLoader(location, woeids, defaultUnit);
            CircularArray<WeatherData> data = loader.queryWeather();
            result.stats.numUpdates++;
            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "- load data");

            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "sync images");

            int size = data.size();
            for (int i = 0; i < size; i++) {
                WeatherData weatherData = data.get(i);
                try {
                    syncImages(result, mConnectivityManager, mPreferenceHelper, woeidTimezones,
                            previousData,
                            weatherData);
                } catch (VolleyError e) {
                    Log.w("WeatherLoadingService", "error getting images", e);
                }
            }
            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "- sync images");
        } catch (InterruptedException ignore) {
            // we have been requested to stop, so simply stop
            result.stats.numIoExceptions++;
        } catch (CantGetWeatherException e) {
            Log.e("WeatherLoadingService", "error loading weather. Waiting for next retry", e);
            result.stats.numIoExceptions++;
        }

    }

    @Nullable
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    private Location requestLocationInfoBlocking() throws InterruptedException {

        List<String> providers = mLocationManager.getAllProviders();

        if (!providers.contains(LocationManager.NETWORK_PROVIDER)) return null;
        if (!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return null;

        SimpleLocationListener listener = new SimpleLocationListener(mLocationManager);

        mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener,
                Looper.getMainLooper());

        Location result = listener.waitForResult();
        if (result == null) {
            result = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "location: " + result);
        return result;
    }

    private String[] addFallbackWoeid(String[] woeids,
            SimpleArrayMap<String, String> woeidTimezones) {

        PreferenceHelper preferenceHelper = mPreferenceHelper;
        String woeid = preferenceHelper.getDefaultLocationWoeId();
        if (woeid != null) {
            String[] tmp = new String[woeids.length + 1];
            System.arraycopy(woeids, 0, tmp, 1, woeids.length);
            tmp[0] = woeid;
            woeids = tmp;

            String woeidTimezone = preferenceHelper.getDefaultLocationTimezone();
            if (!woeidTimezones.containsKey(woeid)) {
                woeidTimezones.put(woeid, woeidTimezone);
            }
        }
        return woeids;
    }

    private void syncImages(SyncResult result, ConnectivityManager cm,
            PreferenceHelper preferenceHelper, SimpleArrayMap<String, String> woeidTimezones,
            SimpleArrayMap<String, WeatherData> previousData, WeatherData weatherData) throws VolleyError {

        NetworkInfo netInfo;
        String woeid = weatherData.woeid;
        WeatherData previous = previousData.get(woeid);
        File[] photos = mWeatherUtils.getCityPhotos(mContext, woeid);

        boolean changed = photos == null || previous == null ||
                previous.nowConditionCode != weatherData.nowConditionCode;

        if (changed) {

            boolean downloadEnabled = preferenceHelper.getUseFlickrImages();
            boolean downloadOnWifiOnly = preferenceHelper.getDownloadImagesOnWifiOnly();
            boolean downloadWhenRoaming = preferenceHelper.getDownloadWhenRoaming();

            netInfo = cm.getActiveNetworkInfo();
            if (netInfo == null) return;
            boolean wifi = netInfo.getType() == ConnectivityManager.TYPE_WIFI;
            boolean roaming = netInfo.isRoaming();

            // tell the sync we got an io exception
            // so it knows we would like to try again later
            if (roaming && !downloadWhenRoaming) {
                result.stats.numIoExceptions++;
                return;
            }

            // well, we are not on wifi, and the user
            // only wants to sync this when wifi
            // is enabled.
            if (!wifi && downloadOnWifiOnly) {
                result.stats.numIoExceptions++;
                return;
            }

            // only download when the user has the option
            // to download flickr images enabled in
            // settings
            if (downloadEnabled) {
                String timezone = woeidTimezones.get(woeid);
                downloadWeatherImages(mContext, mBitmapUtils, woeid, weatherData, timezone);
                result.stats.numInserts++;
            }
        }
    }

    void onWeatherDataLoaded(@Nullable CircularArray<WeatherData> weatherDataList, String unit) {

        int N = weatherDataList == null ? 0 : weatherDataList.size();
        for (int i1 = 0; i1 < N; i1++) {
            WeatherData weatherData = weatherDataList.get(i1);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_LAST_UPDATED,
                    System.currentTimeMillis());
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_WOEID, weatherData.woeid);

            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_HUMIDITY,
                    weatherData.atmosphereHumidity);
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_PRESSURE,
                    weatherData.atmospherePressure);
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_RISING,
                    weatherData.atmosphereRising);
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_VISIBILITY,
                    weatherData.atmosphereVisible);

            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_NOW_CONDITION_CODE,
                    weatherData.nowConditionCode);
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_NOW_TEMPERATURE,
                    weatherData.nowTemperature);

            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_SUNRISE,
                    weatherData.sunrise);
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_SUNSET,
                    weatherData.sunset);

            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_WIND_CHILL,
                    weatherData.windChill);
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_WIND_DIRECTION,
                    weatherData.windDirection);
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_WIND_SPEED,
                    weatherData.windSpeed);
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_CITY,
                    weatherData.location);
            weatherValues.put(WeatherContract.WeatherColumns.COLUMN_NAME_UNIT,
                    unit);

            ContentResolver contentResolver = mContext.getContentResolver();
            contentResolver.insert(WeatherContract.WeatherColumns.CONTENT_URI, weatherValues);

            List<WeatherData.Forecast> forecasts = weatherData.forecasts;
            if (!forecasts.isEmpty()) {

                int count = forecasts.size();
                ContentValues[] forecastValues = new ContentValues[count];

                for (int i = 0; i < count; i++) {
                    WeatherData.Forecast forecast = forecasts.get(i);

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(WeatherContract.ForecastColumns.COLUMN_NAME_FORECAST_DAY,
                            forecast.julianDay);
                    contentValues.put(WeatherContract.ForecastColumns.COLUMN_NAME_LAST_UPDATED,
                            System.currentTimeMillis());
                    contentValues.put(WeatherContract.ForecastColumns.COLUMN_NAME_LOCATION_WOEID,
                            weatherData.woeid);
                    contentValues.put(WeatherContract.ForecastColumns.COLUMN_NAME_CONDITION_CODE,
                            forecast.conditionCode);
                    contentValues.put(WeatherContract.ForecastColumns.COLUMN_NAME_TEMPERATURE_HIGH,
                            forecast.high);
                    contentValues.put(WeatherContract.ForecastColumns.COLUMN_NAME_TEMPERATURE_LOW,
                            forecast.low);
                    contentValues.put(WeatherContract.ForecastColumns.COLUMN_NAME_UNIT,
                            unit);
                    forecastValues[i] = contentValues;
                }
                Uri uri = WeatherContract.ForecastColumns.CONTENT_URI;
                contentResolver.bulkInsert(uri, forecastValues);
            }
        }

        Intent i = new Intent(ACTION_WEATHER_UPDATED);
        i.setPackage(mContext.getPackageName());
        mContext.sendBroadcast(i);
    }

    private class SimpleLocationListener implements LocationListener {

        final CountDownLatch mCountDownLatch = new CountDownLatch(1);

        final AtomicReference<Location> mLocationResult = new AtomicReference<>();

        private final LocationManager mLocationManager;

        public SimpleLocationListener(LocationManager locationManager) {
            mLocationManager = locationManager;
        }

        @Override
        public void onLocationChanged(final Location location) {
            mLocationManager.removeUpdates(this);
            mLocationResult.set(location);
            mCountDownLatch.countDown();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }

        public Location waitForResult() throws InterruptedException {
            mCountDownLatch.await(5, TimeUnit.SECONDS);
            return mLocationResult.get();
        }
    }

    private class WeatherDataLoader {

        final String[] mWoeids;

        final String mUnit;

        @Nullable
        private final Location mLocation;

        public WeatherDataLoader(@Nullable Location location, String[] woeids, String unit) {
            mLocation = location;
            mWoeids = woeids;
            mUnit = unit;
        }

        public CircularArray<WeatherData> queryWeather() throws CantGetWeatherException {
            YahooWeatherApiClient.LocationInfo locationInfo = mLocation == null ?
                    null : YahooWeatherApiClient.getLocationInfo(mLocation);

            // get the woeids from the list
            String currentWoeid = saveAndGetCurrentWoeid(locationInfo);
            CircularArray<String> woeidsToLoad = new CircularArray<>();
            if (currentWoeid != null) {
                woeidsToLoad.addLast(currentWoeid);
            }

            for (String woeid : mWoeids) {
                if (!ArrayUtils.contains(woeidsToLoad, woeid)) {
                    woeidsToLoad.addLast(woeid);
                }
            }

            CircularArray<WeatherData> data =
                    YahooWeatherApiClient.getWeatherForWoeids(woeidsToLoad, mUnit);

            processResult(data);
            return data;
        }

        private String saveAndGetCurrentWoeid(YahooWeatherApiClient.LocationInfo locationInfo) {

            if (locationInfo == null) {
                return null;
            }

            CircularArray<String> currentWoeids = locationInfo.woeids;
            if (!currentWoeids.isEmpty()) {
                String currentWoeid = currentWoeids.get(0);

                if (BuildConfig.DEBUG) {
                    Log.d("WeatherLoadingService", "saving current woeid: " + currentWoeid);
                }
                // save this woeid in the preferences to make sure
                // this is used as the latest weather info
                mPreferences.edit().putString(PREFERENCE_LAST_KNOWN_WOEID, currentWoeid).apply();

                return currentWoeid;
            }
            return null;


        }

        protected void processResult(CircularArray<WeatherData> weatherData) {
            onWeatherDataLoaded(weatherData, mUnit);
        }
    }


}
