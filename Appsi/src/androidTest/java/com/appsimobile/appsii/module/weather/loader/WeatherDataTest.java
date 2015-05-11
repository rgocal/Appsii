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

import junit.framework.TestCase;

/**
 * Created by nick on 22/01/15.
 */
public class WeatherDataTest extends TestCase {

    public void testToMinuteOfDay() {
        int noon = 12 * 60;

        assertEquals(1, WeatherData.toMinuteOfDay("0:01 am"));
        assertEquals(61, WeatherData.toMinuteOfDay("1:01 am"));
        assertEquals(121, WeatherData.toMinuteOfDay("2:01 am"));
        assertEquals(181, WeatherData.toMinuteOfDay("3:01 am"));

        assertEquals(noon + 60, WeatherData.toMinuteOfDay("1:00 pm"));

        assertEquals(noon, WeatherData.toMinuteOfDay("12:00 pm"));
        assertEquals(1, WeatherData.toMinuteOfDay("12:01 am"));
        assertEquals(59, WeatherData.toMinuteOfDay("12:59 am"));
    }

}
