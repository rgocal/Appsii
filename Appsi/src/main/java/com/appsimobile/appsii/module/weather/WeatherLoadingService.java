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
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.appsimobile.appsii.BitmapUtils;
import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.module.home.WeatherFragment;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.module.weather.loader.CantGetWeatherException;
import com.appsimobile.appsii.module.weather.loader.WeatherData;
import com.appsimobile.appsii.module.weather.loader.YahooWeatherApiClient;
import com.appsimobile.appsii.preference.PreferenceHelper;
import com.appsimobile.appsii.preference.PreferencesFactory;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Nick on 19/02/14.
 * <p/>
 */
public class WeatherLoadingService {

    public static final int MAX_PHOTO_COUNT = 2;

    public static final String PREFERENCE_LAST_KNOWN_WOEID = "last_known_woeid";

    public static final String PREFERENCE_LAST_UPDATED_MILLIS =
            BuildConfig.APPLICATION_ID + ".last_updated_millis";

    public static final String EXTRA_INCLUDE_WOEID = BuildConfig.APPLICATION_ID + ".with_woeid";

    public static final String ACTION_WEATHER_UPDATED =
            BuildConfig.APPLICATION_ID + ".weather_updated";

    private static Handler sMainHandler = new Handler(Looper.getMainLooper());

    final HomeItemConfiguration mConfigurationHelper;

    final SharedPreferences mPreferences;

    final Context mContext;

    public WeatherLoadingService(Context context) {
        mContext = context.getApplicationContext();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mConfigurationHelper = HomeItemConfigurationHelper.getInstance(mContext);

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
    public static boolean hasTimeoutExpired(Context context) {
        SharedPreferences preferences = PreferencesFactory.getPreferences(context);

        long lastUpdate = preferences.getLong(PREFERENCE_LAST_UPDATED_MILLIS, 0);

        long timePassedMillis = System.currentTimeMillis() - lastUpdate;
        long minutesPassed = timePassedMillis / DateUtils.MINUTE_IN_MILLIS;

        return minutesPassed > 45;
    }

    private static ImageLoader.ImageContainer downloadImage(final ImageDownloadHelper helper,
            final ImageDownloadHelper.PhotoInfo info, final int dimen) throws VolleyError {

        final AtomicReference<ImageLoader.ImageContainer> result = new AtomicReference<>();
        final AtomicReference<VolleyError> error = new AtomicReference<>();

        final CountDownLatch latch = new CountDownLatch(1);

        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                ImageLoader imageLoader = helper.getImageLoader();
                imageLoader.setBatchedResponseDelay(0);
                imageLoader.get(info.url,
                        new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer response,
                                    boolean isImmediate) {
                                result.set(response);
                                latch.countDown();
                            }

                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                error.set(volleyError);
                                latch.countDown();
                            }
                        }, dimen, dimen,
                        ImageView.ScaleType.CENTER_CROP);

            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            // should not happen
            return null;
        }

        VolleyError volleyError = error.get();
        if (volleyError != null) throw volleyError;
        return result.get();

    }

    /**
     * Downloads the header images for the given woeid and weather-data. Failure is considered
     * non-fatal.
     *
     * @throws VolleyError
     */
    public static void downloadWeatherImages(Context context, String woeid,
            WeatherData weatherData, String timezone) throws VolleyError {

        // first we need to determine if it is day or night.
        // TODO: this needs the timezone


        if (timezone == null) {
            timezone = TimeZone.getDefault().getID();
        }


        boolean isDay = WeatherUtils.isDay(timezone, weatherData);
        ImageDownloadHelper downloadHelper = ImageDownloadHelper.getInstance(context);

        // call into the download-helper this will return a json object with
        // city photos matching the current weather condition.
        JSONObject photos = downloadHelper.searchCityWeatherPhotos(
                woeid, weatherData.nowConditionCode, isDay);

        // Now we need the screen dimension to know which photos have a usable size.
        int dimen = getMaxScreenDimension(context);

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
                WeatherUtils.clearCityPhotos(context, woeid, 0);
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
                        File cacheDir = context.getCacheDir();
                        String fileName = WeatherUtils.createPhotoFileName(woeid, idx);
                        File photoImage = new File(cacheDir, fileName);
                        Bitmap bitmap =
                                BitmapUtils.decodeSampledBitmapFromFile(photoImage, dimen, dimen);
                        if (bitmap == null) {
                            Log.wtf("WeatherLoadingService", "error decoding bitmap");
                            continue;
                        }

                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                bitmap.getHeight(),
                                matrix, false);
                        WeatherUtils.saveBitmap(context, bitmap, woeid, idx);
                    }
                    // success, handle the next one.
                    idx++;
                }
            }
        }
        // remove photos at higher indexes than the amount downloaded.
        WeatherUtils.clearCityPhotos(context, woeid, idx + 1);

    }

    private static int getMaxScreenDimension(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        int dimen = Math.max(point.x, point.y);
        dimen = (dimen * 3) / 4;
        return dimen;
    }

    private static boolean downloadFile(Context context,
            ImageDownloadHelper.PhotoInfo photoInfo, String woeid, int idx) {

        File cacheDir = context.getCacheDir();
        String fileName = WeatherUtils.createPhotoFileName(woeid, idx);
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
    void doSync(String extraWoeid, SyncResult result) {

        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        boolean online = netInfo != null && netInfo.isConnected();
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(mContext);


        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "Handling sync");

        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "Checking online");
        if (!online) {
            bailOut("No network connection");
            result.stats.numIoExceptions++;
            return;
        }
        boolean syncWhenRoaming = preferenceHelper.getSyncWhenRoaming();
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
        Map<String, String> woeidTimezones = new HashMap<>(N);
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


        String defaultUnit = preferenceHelper.getDefaultWeatherTemperatureUnit();
        String unit = mPreferences.getString(WeatherFragment.PREFERENCE_WEATHER_UNIT, defaultUnit);

        try {
            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "request location");
            Location location = requestLocationInfo();
            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "- request location");

            Map<String, WeatherData> previousData = new HashMap<>(woeids.length);
            for (String woeid : woeids) {
                WeatherData data = WeatherUtils.getWeatherData(mContext, woeid);
                previousData.put(woeid, data);
            }

            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "load data");
            WeatherDataLoader loader = new WeatherDataLoader(location, woeids, unit);
            List<WeatherData> data = loader.queryWeather();
            result.stats.numUpdates++;
            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "- load data");

            if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "sync images");
            for (WeatherData weatherData : data) {
                try {
                    String woeid = weatherData.woeid;
                    WeatherData previous = previousData.get(woeid);
                    File[] photos = WeatherUtils.getCityPhotos(mContext, woeid);

                    boolean changed = photos == null || previous == null ||
                            previous.nowConditionCode != weatherData.nowConditionCode;

                    if (changed) {

                        boolean downloadEnabled = preferenceHelper.getUseFlickrImages();
                        boolean downloadOnWifiOnly = preferenceHelper.getDownloadImagesOnWifiOnly();
                        boolean downloadWhenRoaming = preferenceHelper.getDownloadWhenRoaming();

                        netInfo = cm.getActiveNetworkInfo();
                        if (netInfo == null) continue;
                        boolean wifi = netInfo.getType() == ConnectivityManager.TYPE_WIFI;
                        boolean roaming = netInfo.isRoaming();

                        // tell the sync we got an io exception
                        // so it knows we would like to try again later
                        if (roaming && !downloadWhenRoaming) {
                            result.stats.numIoExceptions++;
                            continue;
                        }

                        // well, we are not on wifi, and the user
                        // only wants to sync this when wifi
                        // is enabled.
                        if (!wifi && downloadOnWifiOnly) {
                            result.stats.numIoExceptions++;
                            continue;
                        }

                        // only download when the user has the option
                        // to download flickr images enabled in
                        // settings
                        if (downloadEnabled) {
                            String timezone = woeidTimezones.get(woeid);
                            downloadWeatherImages(mContext, woeid, weatherData, timezone);
                            result.stats.numInserts++;
                        }
                    }
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

    void bailOut(String reason) {
        Log.i("WeatherLoadingService", "not updating weather for reason: " + reason);
    }

    @Nullable
    private Location requestLocationInfo() throws InterruptedException {

        final LocationManager locationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getAllProviders();

        if (!providers.contains(LocationManager.NETWORK_PROVIDER)) return null;
        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return null;

        SimpleLocationListener listener = new SimpleLocationListener(locationManager);

        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener,
                Looper.getMainLooper());

        Location result = listener.waitForResult();
        if (result == null) {
            result = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (BuildConfig.DEBUG) Log.d("WeatherLoadingService", "location: " + result);
        return result;
    }

    void onWeatherDataLoaded(List<WeatherData> weatherDataList, String unit) {

        for (WeatherData weatherData : weatherDataList) {

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
        mPreferences.
                edit().
                putLong(PREFERENCE_LAST_UPDATED_MILLIS, System.currentTimeMillis()).
                apply();

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

        public List<WeatherData> queryWeather() throws CantGetWeatherException {
            YahooWeatherApiClient.LocationInfo locationInfo = mLocation == null ?
                    null : YahooWeatherApiClient.getLocationInfo(mLocation);

            // get the woeids from the list
            String currentWoeid = saveAndGetCurrentWoeid(locationInfo);
            List<String> woeidsToLoad = new ArrayList<>();
            if (currentWoeid != null) {
                woeidsToLoad.add(currentWoeid);
            }

            for (String woeid : mWoeids) {
                if (!woeidsToLoad.contains(woeid)) {
                    woeidsToLoad.add(woeid);
                }
            }

            String[] woeidsArray = woeidsToLoad.toArray(new String[woeidsToLoad.size()]);

            List<WeatherData> data =
                    YahooWeatherApiClient.getWeatherForWoeids(woeidsArray, mUnit);

            processResult(data);
            return data;
        }

        private String saveAndGetCurrentWoeid(YahooWeatherApiClient.LocationInfo locationInfo) {

            if (locationInfo == null) {
                return null;
            }

            List<String> currentWoeids = locationInfo.woeids;
            if (!currentWoeids.isEmpty()) {
                String currentWoeid = currentWoeids.get(0);

                if (BuildConfig.DEBUG) {
                    Log.d("WeatherLoadingService", "saving current woeid: " + currentWoeid);
                }
                // save this woeid in the preferences to make sure
                // this is used as the latest weather info
                SharedPreferences prefs = PreferencesFactory.getPreferences(mContext);
                prefs.edit().putString(PREFERENCE_LAST_KNOWN_WOEID, currentWoeid).apply();

                return currentWoeid;
            }
            return null;


        }

        protected void processResult(List<WeatherData> weatherData) {
            onWeatherDataLoaded(weatherData, mUnit);
        }
    }


}