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

package com.appsimobile.appsii.module.weather.loader;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.ResponseParserException;
import com.appsimobile.appsii.SimpleJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by nick on 21/01/15.
 */
public class WeatherDataParser {


    public static void parseWeatherData(List<WeatherData> result, InputStream inputStream,
            String[] woeids)
            throws JSONException, ResponseParserException, ParseException, IOException {
        String json = readStreamToString(inputStream, new StringBuilder());
        if (BuildConfig.DEBUG) Log.d("WeatherDataParser", "json: " + json);
        parseWeatherData(result, json, woeids);
    }

    private static String readStreamToString(InputStream in, StringBuilder stringBuilder)
            throws IOException {

        stringBuilder.setLength(0);

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }

        return stringBuilder.toString();

    }

    public static void parseWeatherData(List<WeatherData> result, String jsonString,
            String[] woeids)
            throws JSONException, ResponseParserException, ParseException {

        if (woeids.length == 0) return;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(Time.TIMEZONE_UTC));

        SimpleJson json = new SimpleJson(jsonString);

        // when only a single woeid was requested, this is an object, otherwise it is an array.
        // So we need to check if we got an array; if so, handle each of the objects.
        // Otherwise get it as an object
        JSONArray resultsArray = json.getJsonArray("query.results.channel");

        if (resultsArray == null) {
            JSONObject weatherObject = json.optJsonObject("query.results.channel");
            if (weatherObject == null) return;

            String woeid = woeids[0];
            WeatherData weatherData = parseWeatherData(woeid, simpleDateFormat, weatherObject);
            if (weatherData != null) {
                result.add(weatherData);
            }
            return;
        }


        int length = resultsArray.length();
        for (int i = 0; i < length; i++) {
            JSONObject weatherJson = resultsArray.getJSONObject(i);
            WeatherData weatherData = parseWeatherData(woeids[i], simpleDateFormat, weatherJson);
            if (weatherData == null) continue;

            result.add(weatherData);

        }
    }

    @Nullable
    private static WeatherData parseWeatherData(String woeid, SimpleDateFormat simpleDateFormat,
            JSONObject weatherJsonObj)
            throws ResponseParserException, JSONException, ParseException {
        SimpleJson weatherJson = new SimpleJson(weatherJsonObj);
        String city = weatherJson.optString("location.city");

        if (city == null) {
            String country = weatherJson.optString("location.country");
            String region = weatherJson.optString("location.region");
            if (country != null && region != null) {
                city = TextUtils.join(", ", new String[]{country, region});
            } else if (country != null) {
                city = country;
            }
        }


        if (city == null) {
            Log.w("WeatherDataParser", "Error in weather-query. Ignoring location");
            return null;
        }


        WeatherData weatherData = new WeatherData();

        weatherData.location = city;

        weatherData.windChill = weatherJson.getInt("wind.chill", Integer.MIN_VALUE);
        weatherData.windDirection = weatherJson.getInt("wind.direction", Integer.MIN_VALUE);
        weatherData.windSpeed = (float) weatherJson.getDouble("wind.speed", Float.MIN_VALUE);

        weatherData.atmosphereHumidity =
                weatherJson.getInt("atmosphere.humidity", Integer.MIN_VALUE);
        weatherData.atmospherePressure =
                (float) weatherJson.getDouble("atmosphere.pressure", Float.MIN_VALUE);
        weatherData.atmosphereRising =
                weatherJson.getInt("atmosphere.rising", Integer.MIN_VALUE);
        weatherData.atmosphereVisible =
                (float) weatherJson.getDouble("atmosphere.visibility", Float.MIN_VALUE);

        weatherData.sunrise = weatherJson.optString("astronomy.sunrise");
        weatherData.sunset = weatherJson.optString("astronomy.sunset");

        weatherData.nowConditionCode =
                weatherJson.getInt("item.condition.code", WeatherData.INVALID_CONDITION);
        weatherData.nowConditionText = weatherJson.optString("item.condition.text");
        weatherData.nowTemperature = weatherJson.getInt("item.condition.temp",
                WeatherData.INVALID_TEMPERATURE);

        JSONArray forecastArray = weatherJson.optJsonArray("item.forecast");

        if (forecastArray != null) {

            int fl = forecastArray.length();
            for (int k = 0; k < fl; k++) {
                JSONObject forecastJson = forecastArray.getJSONObject(k);
                WeatherData.Forecast forecast = new WeatherData.Forecast();

                String date = forecastJson.optString("date");
                long millis = simpleDateFormat.parse(date).getTime();

                forecast.julianDay = Time.getJulianDay(millis, 0);
                forecast.conditionCode = forecastJson.optInt("code",
                        WeatherData.INVALID_CONDITION);
                forecast.forecastText = forecastJson.optString("text");
                forecast.low = forecastJson.optInt("low", WeatherData.INVALID_TEMPERATURE);
                forecast.high = forecastJson.optInt("high", WeatherData.INVALID_TEMPERATURE);

                weatherData.forecasts.add(forecast);
            }

        }
        weatherData.woeid = woeid;
        return weatherData;
    }


}
