/*
 * Copyright 2013 Google Inc.
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

package com.appsimobile.appsii.module.weather.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper class representing weather data
 */
public class WeatherData {

    public static final int INVALID_TEMPERATURE = Integer.MIN_VALUE;

    public static final int INVALID_CONDITION = -1;

    static final Pattern sMinutePattern = Pattern.compile("(\\d?\\d):(\\d\\d)\\s((a|p)m)");

    public final List<Forecast> forecasts = new ArrayList<>();

    // codes now
    public int nowTemperature = INVALID_TEMPERATURE;

    public int nowConditionCode = INVALID_CONDITION;

    public String woeid;

    public String nowConditionText;

    public String location;

    public int windChill;

    public int windDirection;

    public float windSpeed;

    public int atmosphereHumidity;

    public float atmospherePressure;

    public int atmosphereRising;

    public float atmosphereVisible;

    public String sunrise;

    public String sunset;

    public String unit;

    public long lastUpdated;

    public WeatherData() {
    }

    public int getSunriseMinuteOfDay() {
        return toMinuteOfDay(sunrise);
    }

    static int toMinuteOfDay(String time) {
        if (time == null) return -1;
        Matcher matcher = sMinutePattern.matcher(time);
        if (matcher.find()) {
            String hs = matcher.group(1);
            String ms = matcher.group(2);
            String ampm = matcher.group(3);
            int hour = Integer.parseInt(hs);
            int minute = Integer.parseInt(ms);
            boolean pm = "pm".equals(ampm);

            if (hour < 12 && pm) hour += 12;
            if (hour == 12 && !pm) hour = 0;
            return hour * 60 + minute;
        }
        return -1;
    }

    public int getSunsetMinuteOfDay() {
        return toMinuteOfDay(sunset);
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "nowTemperature=" + nowTemperature +
                ", nowConditionCode=" + nowConditionCode +
                ", nowConditionText='" + nowConditionText + '\'' +
                ", location='" + location + '\'' +
                ", forecasts=" + forecasts +
                '}';
    }

    public static class Forecast {

        public int julianDay;

        public String forecastText;

        public int low = INVALID_TEMPERATURE;

        public int high = INVALID_TEMPERATURE;

        @Override
        public String toString() {
            return "Forecast{" +
                    "julianDay=" + julianDay +
                    ", low=" + low +
                    ", high=" + high +
                    ", conditionCode=" + conditionCode +
                    ", forecastText='" + forecastText + '\'' +
                    '}';
        }

        public int conditionCode = INVALID_CONDITION;


    }


}