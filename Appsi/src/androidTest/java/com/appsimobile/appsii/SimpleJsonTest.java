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

package com.appsimobile.appsii;

import android.test.AndroidTestCase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nick on 09/10/14.
 */
public class SimpleJsonTest extends AndroidTestCase {


    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testPathParsing() throws ResponseParserException {
        StringBuilder sb = new StringBuilder();

        String testData = AssetUtils.readAssetToString(getContext().getAssets(),
                "parser_test_data/parser_json_object_test_restaurant.json", sb);

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(testData);
        } catch (JSONException e) {
            AssertionError error = new AssertionError("Unexpected error");
            error.initCause(e);
            throw error;
        }

        final SimpleJson simpleJson = new SimpleJson(jsonObject);

        SimpleJson test = simpleJson.childForPath("id");
        assertEquals(simpleJson, test);

        SimpleJson location = simpleJson.childForPath("location.lat");
        assertNotNull(location);
        assertNotSame(simpleJson, location);

        SimpleJson test2 = simpleJson.childForPath("location.long");
        assertTrue(location == test2);

        SimpleJson streetName1 = simpleJson.childForPath("location.streetname.id");
        SimpleJson streetName2 = location.childForPath("streetname.id");

        assertTrue(streetName1 == streetName2);

        assertEquals("E-sites cafe", simpleJson.getString("name"));
        assertEquals("4814", location.getString("zipcode"));
        assertEquals("4814", simpleJson.getString("location.zipcode"));
        assertEquals("NL", simpleJson.getString("location.country.code"));
        assertNull(simpleJson.getString("location.country.codexxx"));

        try {
            assertEquals("", simpleJson.getString("xxx_does_not_exist.zipcode"));
            assertTrue(false);
        } catch (ResponseParserException ignore) {
            // expected to get here
        }


        double latitude = simpleJson.getDouble("location.lat", Double.MIN_VALUE);
        assertEquals(51.5906127, latitude);

        double longitude = simpleJson.getDouble("location.long", Double.MIN_VALUE);
        assertEquals(4.7622492, longitude);


    }

}
