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
    public static interface WeatherColumns extends BaseColumns {

        public static final String TABLE_NAME = "weather";
        /**
         * Long, The timestamp when this forecast was last updated
         * Can be used to determine if an update is needed.
         */
        public static final String COLUMN_NAME_LAST_UPDATED = "lastUpdate";
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        public static final String COLUMN_NAME_WOEID = "woeid";
        public static final String COLUMN_NAME_CITY = "city";
        public static final String COLUMN_NAME_NOW_CONDITION_CODE = "now_condition_code";
        public static final String COLUMN_NAME_NOW_TEMPERATURE = "now_temp";
        public static final String COLUMN_NAME_WIND_CHILL = "wind_chill";
        public static final String COLUMN_NAME_WIND_DIRECTION = "wind_direction";
        public static final String COLUMN_NAME_WIND_SPEED = "wind_speed";
        public static final String COLUMN_NAME_ATMOSPHERE_HUMIDITY = "atmosphere_humidity";
        public static final String COLUMN_NAME_ATMOSPHERE_PRESSURE = "atmosphere_pressure";
        public static final String COLUMN_NAME_ATMOSPHERE_RISING = "atmosphere_rising";
        public static final String COLUMN_NAME_ATMOSPHERE_VISIBILITY = "atmosphere_visibility";
        public static final String COLUMN_NAME_SUNRISE = "sunrise";
        public static final String COLUMN_NAME_SUNSET = "sunset";
        public static final String COLUMN_NAME_UNIT = "unit";


    }

    /**
     * This table holds the weather information for Appsii. Historic information is kept for
     * past
     * events. This database is intended to be used for multiple locations
     */
    public static interface ForecastColumns extends BaseColumns {

        public static final String TABLE_NAME = "forecast";
        /**
         * Integer, The julian day for which this is the forecast
         */
        public static final String COLUMN_NAME_FORECAST_DAY = "forecastDay";
        /**
         * Long, The timestamp when this forecast was last updated
         * Can be used to determine if an update is needed.
         */
        public static final String COLUMN_NAME_LAST_UPDATED = "lastUpdate";
        /**
         * Long, The location for which this is the forecast. Expressed in Yahoo weather WoeId
         */
        public static final String COLUMN_NAME_LOCATION_WOEID = "location_woeid";
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        public static final String COLUMN_NAME_CONDITION_CODE = "condition_code";
        public static final String COLUMN_NAME_TEMPERATURE_LOW = "temp_low";
        public static final String COLUMN_NAME_TEMPERATURE_HIGH = "temp_high";
        public static final String COLUMN_NAME_UNIT = "unit";


    }


}
