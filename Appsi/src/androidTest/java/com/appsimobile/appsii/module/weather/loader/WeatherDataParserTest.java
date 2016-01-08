/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.appsii.module.weather.loader;

import android.content.res.AssetManager;
import android.support.v4.util.CircularArray;
import android.test.AndroidTestCase;

import com.appsimobile.appsii.AssetUtils;
import com.appsimobile.appsii.ResponseParserException;
import com.appsimobile.util.ArrayUtils;

import org.json.JSONException;

import java.text.ParseException;

/**
 * Created by nick on 21/01/15.
 */
public class WeatherDataParserTest extends AndroidTestCase {


    public void testWeatherParser_multipleResults()
            throws ParseException, ResponseParserException, JSONException {
        AssetManager manager = getContext().getAssets();
        String json = AssetUtils.readAssetToString(manager, "weather.json", new StringBuilder());
        CircularArray<WeatherData> result = new CircularArray<>();
        WeatherDataParser.parseWeatherData(result, json, ArrayUtils.asArray("1", "2", "3"));

        assertEquals(3, result.size());

        {
            WeatherData data1 = result.get(0);

            assertEquals("Breda", data1.location);
            assertEquals(34, data1.windChill);
            assertEquals(80, data1.windDirection);
            assertEquals(3.0f, data1.windSpeed);

            assertEquals(81, data1.atmosphereHumidity);
            assertEquals(29.85f, data1.atmospherePressure);
            assertEquals(0, data1.atmosphereRising);
            assertEquals(6.21f, data1.atmosphereVisible);

            assertEquals("8:34 am", data1.sunrise);
            assertEquals("5:11 pm", data1.sunset);

            assertEquals(26, data1.nowConditionCode);
            assertEquals(34, data1.nowTemperature);
            assertEquals("Cloudy", data1.nowConditionText);

            assertEquals("1", data1.woeid);

            assertEquals(5, data1.forecasts.size());
            {
                WeatherData.Forecast fc = data1.forecasts.get(0);
                assertEquals(2457044, fc.julianDay);
                assertEquals(36, fc.high);
                assertEquals(28, fc.low);
                assertEquals(29, fc.conditionCode);
                assertEquals("Partly Cloudy", fc.forecastText);
            }
        }

    }

    public void testWeatherParser_singleResult()
            throws ParseException, ResponseParserException, JSONException {
        AssetManager manager = getContext().getAssets();
        String json = AssetUtils.readAssetToString(manager, "weather1.json", new StringBuilder());
        CircularArray<WeatherData> result = new CircularArray<>();
        WeatherDataParser.parseWeatherData(result, json, ArrayUtils.asArray("1"));

        assertEquals(1, result.size());

        {
            WeatherData data1 = result.get(0);

            assertEquals("Etten-Leur", data1.location);
            assertEquals(-6, data1.windChill);
            assertEquals(70, data1.windDirection);
            assertEquals(14.48f, data1.windSpeed);

            assertEquals(83, data1.atmosphereHumidity);
            assertEquals(1015.92f, data1.atmospherePressure);
            assertEquals(0, data1.atmosphereRising);
            assertEquals(5f, data1.atmosphereVisible);

            assertEquals("8:32 am", data1.sunrise);
            assertEquals("5:13 pm", data1.sunset);

            assertEquals(26, data1.nowConditionCode);
            assertEquals(-1, data1.nowTemperature);
            assertEquals("Cloudy", data1.nowConditionText);

            assertEquals("1", data1.woeid);

            assertEquals(5, data1.forecasts.size());
            {
                WeatherData.Forecast fc = data1.forecasts.get(0);
                assertEquals(2457045, fc.julianDay);
                assertEquals(1, fc.high);
                assertEquals(-2, fc.low);
                assertEquals(29, fc.conditionCode);
                assertEquals("Partly Cloudy", fc.forecastText);
            }
        }

    }

    public void testWeatherParserWithError()
            throws ParseException, ResponseParserException, JSONException {
        AssetManager manager = getContext().getAssets();
        String json = AssetUtils.readAssetToString(manager, "weather_with_error.json",
                new StringBuilder());
        CircularArray<WeatherData> result = new CircularArray<>();
        WeatherDataParser.parseWeatherData(result, json, ArrayUtils.asArray("a", "b"));

        assertEquals(1, result.size());

        {
            WeatherData data1 = result.get(0);

            assertEquals("Etten-Leur", data1.location);
            assertEquals(24, data1.windChill);
            assertEquals(60, data1.windDirection);
            assertEquals(6.0f, data1.windSpeed);

            assertEquals(91, data1.atmosphereHumidity);
            assertEquals(29.85f, data1.atmospherePressure);
            assertEquals(0, data1.atmosphereRising);
            assertEquals(3.73f, data1.atmosphereVisible);

            assertEquals("8:34 am", data1.sunrise);
            assertEquals("5:11 pm", data1.sunset);

            assertEquals(26, data1.nowConditionCode);
            assertEquals(30, data1.nowTemperature);
            assertEquals("Cloudy", data1.nowConditionText);

            assertEquals("b", data1.woeid);
            assertEquals(5, data1.forecasts.size());
            {
                WeatherData.Forecast fc = data1.forecasts.get(0);
                assertEquals(2457044, fc.julianDay);
                assertEquals(35, fc.high);
                assertEquals(27, fc.low);
                assertEquals(29, fc.conditionCode);
                assertEquals("Partly Cloudy", fc.forecastText);
            }
        }

    }


}
