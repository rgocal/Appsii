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

import android.net.Uri;
import android.provider.BaseColumns;

import com.appsimobile.appsii.BuildConfig;

/**
 * Created by Nick on 19/02/14.
 */
public class WeatherContract {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".weather";

    public WeatherContract mWeatherContract;

    /**
     * This table holds the weather information for Appsii. Historic information is kept for past
     * events. This database is intended to be used for multiple locations
     */
    public interface WeatherColumns extends BaseColumns {

        String TABLE_NAME = "weather";
        /**
         * Long, The timestamp when this forecast was last updated
         * Can be used to determine if an update is needed.
         */
        String COLUMN_NAME_LAST_UPDATED = "lastUpdate";
        Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        String COLUMN_NAME_WOEID = "woeid";
        String COLUMN_NAME_CITY = "city";
        String COLUMN_NAME_NOW_CONDITION_CODE = "now_condition_code";
        String COLUMN_NAME_NOW_TEMPERATURE = "now_temp";
        String COLUMN_NAME_WIND_CHILL = "wind_chill";
        String COLUMN_NAME_WIND_DIRECTION = "wind_direction";
        String COLUMN_NAME_WIND_SPEED = "wind_speed";
        String COLUMN_NAME_ATMOSPHERE_HUMIDITY = "atmosphere_humidity";
        String COLUMN_NAME_ATMOSPHERE_PRESSURE = "atmosphere_pressure";
        String COLUMN_NAME_ATMOSPHERE_RISING = "atmosphere_rising";
        String COLUMN_NAME_ATMOSPHERE_VISIBILITY = "atmosphere_visibility";
        String COLUMN_NAME_SUNRISE = "sunrise";
        String COLUMN_NAME_SUNSET = "sunset";
        String COLUMN_NAME_UNIT = "unit";


    }

    /**
     * This table holds the weather information for Appsii. Historic information is kept for
     * past
     * events. This database is intended to be used for multiple locations
     */
    public interface ForecastColumns extends BaseColumns {

        String TABLE_NAME = "forecast";
        /**
         * Integer, The julian day for which this is the forecast
         */
        String COLUMN_NAME_FORECAST_DAY = "forecastDay";
        /**
         * Long, The timestamp when this forecast was last updated
         * Can be used to determine if an update is needed.
         */
        String COLUMN_NAME_LAST_UPDATED = "lastUpdate";
        /**
         * Long, The location for which this is the forecast. Expressed in Yahoo weather WoeId
         */
        String COLUMN_NAME_LOCATION_WOEID = "location_woeid";
        Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        String COLUMN_NAME_CONDITION_CODE = "condition_code";
        String COLUMN_NAME_TEMPERATURE_LOW = "temp_low";
        String COLUMN_NAME_TEMPERATURE_HIGH = "temp_high";
        String COLUMN_NAME_UNIT = "unit";


    }


}
