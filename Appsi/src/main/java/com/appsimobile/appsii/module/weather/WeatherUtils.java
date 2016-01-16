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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;

import com.appsimobile.appsii.BitmapUtils;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.weather.loader.WeatherData;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by Nick on 20/02/14.
 */
@Singleton
public class WeatherUtils {

    public static final int FLAG_TEMPERATURE_NO_UNIT = 1;

    static final Time sTime = new Time();

    final BitmapUtils mBitmapUtils;

    @Inject
    public WeatherUtils(BitmapUtils bitmapUtils) {
        mBitmapUtils = bitmapUtils;
    }


    public SparseArray<ForecastInfo> getForecastForDays(Context context, int startJulianDay,
            String woeid) {
        ContentResolver resolver = context.getContentResolver();
        String[] projection = new String[]{
                WeatherContract.ForecastColumns.COLUMN_NAME_CONDITION_CODE,
                WeatherContract.ForecastColumns.COLUMN_NAME_FORECAST_DAY,
                WeatherContract.ForecastColumns.COLUMN_NAME_TEMPERATURE_HIGH,
                WeatherContract.ForecastColumns.COLUMN_NAME_TEMPERATURE_LOW,
                WeatherContract.ForecastColumns.COLUMN_NAME_LAST_UPDATED,
                WeatherContract.ForecastColumns.COLUMN_NAME_UNIT,
        };
        String selection = WeatherContract.ForecastColumns.COLUMN_NAME_LOCATION_WOEID + "= ? AND " +
                WeatherContract.ForecastColumns.COLUMN_NAME_FORECAST_DAY + " >= ?";

        String[] selectionArgs = new String[]{woeid, String.valueOf(startJulianDay)};

        Cursor cursor = resolver.query(WeatherContract.ForecastColumns.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                WeatherContract.ForecastColumns.COLUMN_NAME_LAST_UPDATED + " DESC");

        if (cursor == null) return null;

        SparseArray<ForecastInfo> result = new SparseArray<>();
        try {
            while (cursor.moveToNext()) {
                int julianDay = cursor.getInt(1); //julian day
                if (julianDay >= startJulianDay) {
                    ForecastInfo info = new ForecastInfo();
                    info.conditionCode = cursor.getInt(0);
                    info.tempHigh = cursor.getInt(2);
                    info.tempLow = cursor.getInt(3);
                    info.lastUpdate = cursor.getLong(4);
                    info.unit = cursor.getString(5);
                    info.julianDay = julianDay;
                    result.put(julianDay, info);
                }
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public WeatherData getWeatherData(Context context, String woeid) {
        ContentResolver resolver = context.getContentResolver();
        String selection = WeatherContract.WeatherColumns.COLUMN_NAME_WOEID + "= ?";

        String[] selectionArgs = new String[]{woeid};

        Cursor cursor = resolver.query(
                WeatherContract.WeatherColumns.CONTENT_URI,
                WeatherDataQuery.PROJECTION,
                selection,
                selectionArgs,
                null);

        if (cursor == null) return null;

        try {
            if (cursor.moveToNext()) {
                WeatherData result = new WeatherData();
                result.unit = cursor.getString(WeatherDataQuery.UNIT);
                result.lastUpdated = cursor.getLong(WeatherDataQuery.LAST_UPDATED);
                result.location = cursor.getString(WeatherDataQuery.CITY);

                result.nowConditionCode = cursor.getInt(WeatherDataQuery.NOW_CONDITION_CODE);
                result.nowTemperature = cursor.getInt(WeatherDataQuery.NOW_TEMPERATURE);

                result.windChill = cursor.getInt(WeatherDataQuery.WIND_CHILL);
                result.windDirection = cursor.getInt(WeatherDataQuery.WIND_DIRECTION);
                result.windSpeed = cursor.getInt(WeatherDataQuery.WIND_SPEED);

                result.atmosphereHumidity = cursor.getInt(WeatherDataQuery.ATMOSPHERE_HUMIDITY);
                result.atmospherePressure = cursor.getFloat(WeatherDataQuery.ATMOSPHERE_PRESSURE);
                result.atmosphereRising = cursor.getInt(WeatherDataQuery.ATMOSPHERE_RISING);
                result.atmosphereVisible = cursor.getFloat(WeatherDataQuery.ATMOSPHERE_VISIBILITY);

                result.sunrise = cursor.getString(WeatherDataQuery.SUNRISE);
                result.sunset = cursor.getString(WeatherDataQuery.SUNSET);
                result.woeid = woeid;
                return result;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public String formatTemperature(Context context, int temp, String tempUnit,
            String displayUnit) {
        return formatTemperature(context, temp, tempUnit, displayUnit, 0);
    }

    public String formatTemperature(Context context, int temp, String tempUnit,
            String displayUnit, int flags) {
        if (!TextUtils.equals(tempUnit, displayUnit)) {
            if ("c".equals(displayUnit)) {
                temp = Math.round(toCelsius(temp));
            } else {
                temp = Math.round(toFahrenheit(temp));
            }
        }

        int unitResId;
        if (flags == FLAG_TEMPERATURE_NO_UNIT) {
            unitResId = R.string.degrees;
        } else {
            unitResId = "c".equals(displayUnit) ? R.string.degrees_c : R.string.degrees_f;
        }
        String suffix = context.getString(unitResId);

        return context.getString(R.string.temperature_high_unit, temp, suffix);
    }

    public float toCelsius(int degrees) {
        return (degrees - 32f) * (5f / 9.0f);
    }

    public float toFahrenheit(int degrees) {
        return (9f * degrees) / 5f + 32;
    }

    public String formatWindSpeed(Context context, float speed, String tempUnit,
            String displayUnit) {
        int displaySpeed;
        if (!TextUtils.equals(tempUnit, displayUnit)) {
            if ("c".equals(displayUnit)) {
                displaySpeed = Math.round(toKph(speed));
            } else {
                displaySpeed = Math.round(toMph(speed));
            }
        } else {
            displaySpeed = Math.round(speed);
        }

        int unitResId = "c".equals(displayUnit) ? R.string.speed_kph : R.string.speed_mph;
        String suffix = context.getString(unitResId);

        return context.getString(R.string.temperature_high_unit, displaySpeed, suffix);
    }

    public float toKph(float speedMph) {
        return speedMph * 1.609344f;
    }

    public float toMph(float speeKph) {
        return speeKph / 1.609344f;
    }

    public boolean isDay(String timezone, WeatherData weatherData) {
        sTime.timezone = timezone;
        sTime.normalize(false);
        sTime.setToNow();
        int minuteNow = sTime.hour * 60 + sTime.minute;

        int sunrise = weatherData.getSunriseMinuteOfDay();
        if (minuteNow < sunrise) return false;

        int sunset = weatherData.getSunsetMinuteOfDay();
        if (minuteNow > sunset) return false;

        return true;
    }

    public int getConditionCodeIconResId(int conditionCode, boolean day) {
        // http://developer.yahoo.com/weather/
        switch (conditionCode) {
            case 19: // dust or sand
            case 21: // haze
                return R.drawable.ic_weather_haze;
            case 20: // foggy
            case 22: // smoky
                return R.drawable.ic_weather_fog;
            case 24: // windy
                return R.drawable.ic_weather_wind;
            case 25: // cold
            case 26: // cloudy
                return day ? R.drawable.ic_weather_clouds : R.drawable.ic_weather_clouds_night;
            case 27: // mostly cloudy (night)
                return R.drawable.ic_weather_clouds_night;
            case 28: // mostly cloudy (day)
                return R.drawable.ic_weather_clouds;
            case 29: // partly cloudy (night)
                return R.drawable.ic_weather_few_clouds_night;
            case 30: // partly cloudy (day)
                return R.drawable.ic_weather_few_clouds;
            case 44: // partly cloudy
                return day ? R.drawable.ic_weather_few_clouds :
                        R.drawable.ic_weather_few_clouds_night;
            case 31: // clear (night)
                return R.drawable.ic_weather_clear_night;
            case 33: // fair (night)
                return R.drawable.ic_weather_few_clouds_night;
            case 34: // fair (day)
                return R.drawable.ic_weather_few_clouds;
            case 32: // sunny
            case 36: // hot
                return day ? R.drawable.ic_weather_clear : R.drawable.ic_weather_clear_night;
            case 0: // tornado
            case 2: // hurricane
                return R.drawable.ic_weather_wind;
            case 1: // tropical storm
            case 3: // severe thunderstorms
            case 4: // thunderstorms
            case 23: // blustery
                return R.drawable.ic_weather_storm;
            case 5: // mixed rain and snow
            case 6: // mixed rain and sleet
            case 7: // mixed snow and sleet
            case 8: // freezing drizzle
            case 10: // freezing rain
            case 18: // sleet
                return R.drawable.ic_weather_snow_rain;
            case 9: // drizzle
                return day ? R.drawable.ic_weather_drizzle : R.drawable.ic_weather_drizzle_night;
            case 11: // showers
            case 12: // showers
                return day ? R.drawable.ic_weather_showers : R.drawable.ic_weather_showers_night;
            case 17: // hail
            case 35: // mixed rain and hail
                return R.drawable.ic_weather_hail;
            case 37: // isolated thunderstorms
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
                return day ? R.drawable.ic_weather_light_storm :
                        R.drawable.ic_weather_light_storm_night;
            case 40: // scattered showers
                return day ? R.drawable.ic_weather_rain : R.drawable.ic_weather_rain_night;
            case 45: // thundershowers
            case 47: // isolated thundershowers
                return R.drawable.ic_weather_storm;
            case 13: // snow flurries
            case 14: // light snow showers
            case 42: // scattered snow showers
            case 15: // blowing snow
                return day ? R.drawable.ic_weather_snow_scattered :
                        R.drawable.ic_weather_snow_scattered_night;
            case 16: // snow
            case 41: // heavy snow
            case 43: // heavy snow
            case 46: // snow showers
                return R.drawable.ic_weather_big_snow;
        }

        return R.drawable.ic_weather_unknown;
    }


    public int getConditionCodeTinyIconResId(int conditionCode, boolean day) {
        // http://developer.yahoo.com/weather/
        switch (conditionCode) {
            case 19: // dust or sand
            case 21: // haze
            case 20: // foggy
            case 22: // smoky
                return R.drawable.ic_small_heavy_fog;
            case 24: // windy
            case 25: // cold
            case 26: // cloudy
            case 27: // mostly cloudy (night)
            case 28: // mostly cloudy (day)
            case 29: // partly cloudy (night)
            case 30: // partly cloudy (day)
            case 44: // partly cloudy
                return R.drawable.ic_small_clouds;
            case 31: // clear (night)
            case 33: // fair (night)
                return R.drawable.ic_small_clear_night;
            case 34: // fair (day)
            case 32: // sunny
            case 36: // hot
                return day ? R.drawable.ic_small_clear_day : R.drawable.ic_small_clear_night;
            case 0: // tornado
            case 2: // hurricane
            case 1: // tropical storm
            case 3: // severe thunderstorms
            case 4: // thunderstorms
            case 23: // blustery
            case 37: // isolated thunderstorms
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
            case 45: // thundershowers
            case 47: // isolated thundershowers
                return R.drawable.ic_small_thunder;
            case 5: // mixed rain and snow
            case 6: // mixed rain and sleet
            case 7: // mixed snow and sleet
            case 8: // freezing drizzle
            case 10: // freezing rain
            case 18: // sleet
            case 9: // drizzle
            case 11: // showers
            case 12: // showers
            case 17: // hail
            case 35: // mixed rain and hail
            case 40: // scattered showers
            case 13: // snow flurries
            case 14: // light snow showers
            case 42: // scattered snow showers
            case 15: // blowing snow
            case 16: // snow
            case 41: // heavy snow
            case 43: // heavy snow
            case 46: // snow showers
                return R.drawable.ic_small_rain;
        }

        return R.drawable.ic_small_clear_day;
    }

    public String formatConditionCode(int conditionCode) {

        // http://developer.yahoo.com/weather/
        switch (conditionCode) {
            case 19: // dust or sand
                return "Dust or sand";
            case 21: // haze
                return "Haze";
            case 20: // foggy
                return "Foggy";
            case 22: // smoky
                return "Smoky";
            case 24: // windy
                return "Windy";
            case 25: // cold
                return "Cold";
            case 26: // cloudy
                return "Cloudy";
            case 27: // mostly cloudy (night)
            case 28: // mostly cloudy (day)
                return "Mostly cloudy";
            case 29: // partly cloudy (night)
            case 30: // partly cloudy (day)
            case 44: // partly cloudy
                return "Partly cloudy";
            case 31: // clear (night)
                return "Clear";
            case 33: // fair (night)
            case 34: // fair (day)
                return "Fair";
            case 32: // sunny
                return "Sunny";
            case 36: // hot
                return "Hot";
            case 0: // tornado
                return "Tornado";
            case 2: // hurricane
                return "Hurricane";
            case 1: // tropical storm
                return "Tropical storm";
            case 3: // severe thunderstorms
                return "Severe thunderstorms";
            case 4: // thunderstorms
                return "Thunderstorms";
            case 23: // blustery
                return "Blustery";
            case 5: // mixed rain and snow
                return "Mixed rain and snow";
            case 6: // mixed rain and sleet
                return "Mixed rain and sleet";
            case 7: // mixed snow and sleet
                return "Mixed snow and sleet";
            case 8: // freezing drizzle
                return "Freezing drizzle";
            case 10: // freezing rain
                return "Freezing rain";
            case 18: // sleet
                return "Sleet";
            case 9: // drizzle
                return "Drizzle";
            case 11: // showers
            case 12: // showers
                return "Showers";
            case 17: // hail
                return "Hail";
            case 35: // mixed rain and hail
                return "Mixed rain and hail";
            case 37: // isolated thunderstorms
                return "Isolated thunderstorms";
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
                return "Scattered thunderstorms";
            case 40: // scattered showers
                return "Scattered showers";
            case 45: // thundershowers
                return "Thundershowers";
            case 47: // isolated thundershowers
                return "Isolated thundershowers";
            case 13: // snow flurries
                return "Snow flurries";
            case 14: // light snow showers
                return "Light snow showers";
            case 42: // scattered snow showers
                return "Scattered snow showers";
            case 15: // blowing snow
                return "Blowing snow";
            case 16: // snow
                return "Snow";
            case 41: // heavy snow
            case 43: // heavy snow
                return "Heavy Snow";
            case 46: // snow showers
                return "Heavy Showers";
        }

        return "Unknown";
    }

    public File[] getCityPhotos(Context context, String woeid) {
        File cacheDir = getWeatherPhotoCacheDir(context);
        int validCount = 0;
        for (int i = 0; i < 5; i++) {
            String name = createPhotoFileName(woeid, i);
            File photoImage = new File(cacheDir, name);
            if (photoImage.exists()) {
                validCount++;
            } else {
                break;
            }
        }

        if (validCount == 0) return null;

        File[] result = new File[validCount];
        for (int i = 0; i < validCount; i++) {
            String name = createPhotoFileName(woeid, i);
            File photoImage = new File(cacheDir, name);
            result[i] = photoImage;
        }
        return result;
    }

    public File getWeatherPhotoCacheDir(Context context) {
        File path = mBitmapUtils.externalFilesFolder();
        File result = new File(path, "weather");
        if (!result.exists()) {
            result.mkdirs();
        }
        return result;
    }

    public String createPhotoFileName(String woeid, int idx) {
        return woeid + "-" + idx + ".jpg";
    }

    public void clearCityPhotos(Context context, String woeid, int startIdx) {
        File cacheDir = getWeatherPhotoCacheDir(context);
        for (int i = startIdx; i < 5; i++) {
            String name = createPhotoFileName(woeid, i);
            File photoImage = new File(cacheDir, name);
            photoImage.delete();
        }
    }

    boolean saveBitmap(Context context, Bitmap bitmap, String woeid, int idx) {
        File photoImage = getWeatherImageFile(context, woeid, idx);

        boolean result;

        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(photoImage));
            result = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e("Weather", "error saving weather image", e);
            result = false;
        }

        return result;
    }

    @NonNull
    public File getWeatherImageFile(Context context, String woeid, int idx) {
        File cacheDir = getWeatherPhotoCacheDir(context);
        String name = createPhotoFileName(woeid, idx);
        return new File(cacheDir, name);
    }

    public static class ForecastInfo implements Comparable<ForecastInfo> {

        public int julianDay;

        public int conditionCode;

        public int tempLow;

        public int tempHigh;

        public long lastUpdate;

        public String unit;

        private static int intCompare(int lhs, int rhs) {
            return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
        }

        @Override
        public int compareTo(@NonNull ForecastInfo another) {
            return intCompare(julianDay, another.julianDay);
        }

    }

    static class WeatherDataQuery {

        static final String[] PROJECTION = new String[]{
                WeatherContract.WeatherColumns.COLUMN_NAME_UNIT,
                WeatherContract.WeatherColumns.COLUMN_NAME_LAST_UPDATED,
                WeatherContract.WeatherColumns.COLUMN_NAME_CITY,

                WeatherContract.WeatherColumns.COLUMN_NAME_NOW_CONDITION_CODE,
                WeatherContract.WeatherColumns.COLUMN_NAME_NOW_TEMPERATURE,

                WeatherContract.WeatherColumns.COLUMN_NAME_WIND_CHILL,
                WeatherContract.WeatherColumns.COLUMN_NAME_WIND_DIRECTION,
                WeatherContract.WeatherColumns.COLUMN_NAME_WIND_SPEED,

                WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_HUMIDITY,
                WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_PRESSURE,
                WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_RISING,
                WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_VISIBILITY,

                WeatherContract.WeatherColumns.COLUMN_NAME_SUNRISE,
                WeatherContract.WeatherColumns.COLUMN_NAME_SUNSET,
        };

        static final int UNIT = 0;

        static final int LAST_UPDATED = 1;

        static final int CITY = 2;

        static final int NOW_CONDITION_CODE = 3;

        static final int NOW_TEMPERATURE = 4;

        static final int WIND_CHILL = 5;

        static final int WIND_DIRECTION = 6;

        static final int WIND_SPEED = 7;

        static final int ATMOSPHERE_HUMIDITY = 8;

        static final int ATMOSPHERE_PRESSURE = 9;

        static final int ATMOSPHERE_RISING = 10;

        static final int ATMOSPHERE_VISIBILITY = 11;

        static final int SUNRISE = 12;

        static final int SUNSET = 13;
    }

}
