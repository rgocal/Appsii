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

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by nick on 24/01/15.
 */
public class YahooWeatherApiClientTest extends AndroidTestCase {

    public void testParseLocationInfo()
            throws IOException, CantGetWeatherException, XmlPullParserException {
        YahooWeatherApiClient.LocationInfo locationInfo = new YahooWeatherApiClient.LocationInfo();

        AssetManager manager = getContext().getAssets();
        InputStream in = manager.open("places_query.xml");

        YahooWeatherApiClient.parseLocationInfo(locationInfo, in);

        assertEquals("Europe/Madrid", locationInfo.timezone);
        assertEquals("Spain", locationInfo.country);
        assertEquals("Castello de la Plana", locationInfo.town);
        assertEquals("Europe/Madrid", locationInfo.timezone);

        assertEquals(6, locationInfo.woeids.size());
        assertEquals("756804", locationInfo.woeids.get(0));
        assertEquals("12578038", locationInfo.woeids.get(1));
        assertEquals("12602138", locationInfo.woeids.get(2));
        assertEquals("12695443", locationInfo.woeids.get(3));
        assertEquals("23424950", locationInfo.woeids.get(4));
        // TODO: why is this one in here twice?
        assertEquals("756804", locationInfo.woeids.get(5));
    }

    public void testParseLocationSearchResult_tokyo()
            throws IOException, CantGetWeatherException, XmlPullParserException {
        CircularArray<YahooWeatherApiClient.LocationSearchResult> result = new CircularArray<>();

        AssetManager manager = getContext().getAssets();
        InputStream in = manager.open("places_query_tokyo.xml");

        YahooWeatherApiClient.parseLocationSearchResults(result, in);

        assertEquals(1, result.size());

        YahooWeatherApiClient.LocationSearchResult res = result.get(0);

        assertEquals("Asia/Tokyo", res.timezone);
        assertEquals("Tokyo, Tokyo Prefecture", res.displayName);
        assertEquals("1118370", res.woeid);
        assertEquals("Japan", res.country);

    }

    public void testParseLocationSearchResult_cambridge()
            throws IOException, CantGetWeatherException, XmlPullParserException {
        CircularArray<YahooWeatherApiClient.LocationSearchResult> result = new CircularArray<>();

        AssetManager manager = getContext().getAssets();
        InputStream in = manager.open("places_query_cambridge.xml");

        YahooWeatherApiClient.parseLocationSearchResults(result, in);

        assertEquals(1, result.size());

        YahooWeatherApiClient.LocationSearchResult res = result.get(0);

        assertEquals("Europe/London", res.timezone);
        assertEquals("Cambridge, England", res.displayName);
        assertEquals("14979", res.woeid);
        assertEquals("United Kingdom", res.country);

    }

}
