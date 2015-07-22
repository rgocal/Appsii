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

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.ResponseParserException;
import com.appsimobile.appsii.annotation.VisibleForTesting;
import com.appsimobile.appsii.module.weather.Utils;
import com.appsimobile.util.CollectionUtils;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Client code for the Yahoo! Weather RSS feeds and GeoPlanet API.
 */
public class YahooWeatherApiClient {

    private static final String TAG = "YahooWeatherApiClient";

    private static final int MAX_SEARCH_RESULTS = 10;

    private static final int PARSE_STATE_NONE = 0;

    private static final int PARSE_STATE_PLACE = 1;

    private static final int PARSE_STATE_WOEID = 2;

    private static final int PARSE_STATE_NAME = 3;

    private static final int PARSE_STATE_COUNTRY = 4;

    private static final int PARSE_STATE_ADMIN1 = 5;

    private static final int PARSE_STATE_TIMEZONE = 6;

    private static final XmlPullParserFactory sXmlPullParserFactory;

    static {
        try {
            sXmlPullParserFactory = XmlPullParserFactory.newInstance();
            sXmlPullParserFactory.setNamespaceAware(true);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Could not instantiate XmlPullParserFactory", e);
            throw new RuntimeException(e);
        }
    }

    public static ArrayList<WeatherData> getWeatherForWoeids(String[] woeids, String unit)
            throws CantGetWeatherException {

        if (woeids == null || woeids.length == 0) return CollectionUtils.emptyList();

        HttpURLConnection connection = null;
        try {
            String url = buildWeatherQueryUrl(woeids, unit);
            if (BuildConfig.DEBUG) Log.d("YahooWeatherApiClient", "loading weather from: " + url);

            connection = Utils.openUrlConnection(url);

            ArrayList<WeatherData> result = new ArrayList<>();
            WeatherDataParser.parseWeatherData(result, connection.getInputStream(), woeids);

            return result;
        } catch (JSONException | ResponseParserException | ParseException | IOException |
                NumberFormatException e) {
            throw new CantGetWeatherException(true, R.string.no_weather_data,
                    "Error parsing weather feed XML.", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String buildWeatherQueryUrl(String[] woeids, String unit) {
        // http://developer.yahoo.com/weather/
        String endPoint = "https://query.yahooapis.com/v1/public/yql?format=json&q=";
        String query = "select * from weather.forecast where woeid in (%s) and u=\"%s\"";
        String param = TextUtils.join(", ", woeids);
        String queryString = String.format(Locale.ROOT, query, param, unit);
        if (BuildConfig.DEBUG) Log.d("YahooWeatherApiClient", "yql query: " + queryString);
        try {
            queryString = URLEncoder.encode(queryString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.wtf("YahooWeatherApiClient", "error encoding url", e);
        }

        return endPoint + queryString;
    }

    public static LocationInfo getLocationInfo(Location location) throws CantGetWeatherException {
        LocationInfo li = new LocationInfo();

        // first=tagname (admin1, locality3) second=woeid

        HttpURLConnection connection = null;
        try {
            connection = Utils.openUrlConnection(buildPlaceSearchUrl(location));

            InputStream inputStream = connection.getInputStream();
            return parseLocationInfo(li, inputStream);

        } catch (IOException | XmlPullParserException e) {
            throw new CantGetWeatherException(true, R.string.no_weather_data,
                    "Error parsing place search XML", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String buildPlaceSearchUrl(Location l) {
        // GeoPlanet API
        return "http://where.yahooapis.com/v1/places.q('"
                + l.getLatitude() + "," + l.getLongitude() + "')"
                + "?appid=" + YahooWeatherApiConfig.APP_ID;
    }

    @VisibleForTesting
    static LocationInfo parseLocationInfo(
            LocationInfo li, InputStream in)
            throws XmlPullParserException, IOException, CantGetWeatherException {

        ArrayList<Pair<String, String>> alternateWoeids = new ArrayList<>();
        String primaryWoeid = null;

        XmlPullParser xpp = sXmlPullParserFactory.newPullParser();
        xpp.setInput(new InputStreamReader(in));

        boolean inWoe = false;
        boolean inTown = false;
        boolean inCountry = false;
        boolean inTimezone = false;
        int eventType = xpp.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = xpp.getName();

            if (eventType == XmlPullParser.START_TAG && "woeid".equals(tagName)) {
                inWoe = true;
            } else if (eventType == XmlPullParser.TEXT && inWoe) {
                primaryWoeid = xpp.getText();
            } else if (eventType == XmlPullParser.START_TAG && tagName.startsWith("timezone")) {
                inTimezone = true;
            } else if (eventType == XmlPullParser.TEXT && inTimezone) {
                li.timezone = xpp.getText();
            } else if (eventType == XmlPullParser.START_TAG &&
                    (tagName.startsWith("locality") || tagName.startsWith("admin") ||
                            tagName.startsWith("country"))) {
                for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
                    String attrName = xpp.getAttributeName(i);
                    if ("type".equals(attrName)
                            && "Town".equals(xpp.getAttributeValue(i))) {
                        inTown = true;
                    } else if ("type".equals(attrName)
                            && "Country".equals(xpp.getAttributeValue(i))) {
                        inCountry = true;
                    } else if ("woeid".equals(attrName)) {
                        String woeid = xpp.getAttributeValue(i);
                        if (!TextUtils.isEmpty(woeid)) {
                            alternateWoeids.add(
                                    new Pair<>(tagName, woeid));
                        }
                    }
                }
            } else if (eventType == XmlPullParser.TEXT && inTown) {
                li.town = xpp.getText();
            } else if (eventType == XmlPullParser.TEXT && inCountry) {
                li.country = xpp.getText();
            }

            if (eventType == XmlPullParser.END_TAG) {
                inWoe = false;
                inTown = false;
                inCountry = false;
                inTimezone = false;
            }

            eventType = xpp.next();
        }

        // Add the primary woeid if it was found.
        if (!TextUtils.isEmpty(primaryWoeid)) {
            li.woeids.add(primaryWoeid);
        }

        // Sort by descending tag name to order by decreasing precision
        // (locality3, locality2, locality1, admin3, admin2, admin1, etc.)
        Collections.sort(alternateWoeids, new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> pair1, Pair<String, String> pair2) {
                return pair1.first.compareTo(pair2.first);
            }
        });

        int N = alternateWoeids.size();
        for (int i = 0; i < N; i++) {
            Pair<String, String> pair = alternateWoeids.get(i);
            li.woeids.add(pair.second);
        }

        if (li.woeids.size() > 0) {
            return li;
        }

        throw new CantGetWeatherException(true, R.string.no_weather_data, "No WOEIDs found nearby.");
    }

    public static List<LocationSearchResult> findLocationsAutocomplete(String startsWith) {
        List<LocationSearchResult> results = new ArrayList<>();

        HttpURLConnection connection = null;
        try {
            connection = Utils.openUrlConnection(buildPlaceSearchStartsWithUrl(startsWith));
            InputStream inputStream = connection.getInputStream();

            parseLocationSearchResults(results, inputStream);

        } catch (IOException | XmlPullParserException e) {
            Log.w(TAG, "Error parsing place search XML");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return results;
    }

    private static String buildPlaceSearchStartsWithUrl(String startsWith) {
        // GeoPlanet API
        startsWith = startsWith.replaceAll("[^\\w ]+", "").replaceAll(" ", "%20");
        return "http://where.yahooapis.com/v1/places.q('" + startsWith + "%2A');"
                + "count=" + MAX_SEARCH_RESULTS
                + "?appid=" + YahooWeatherApiConfig.APP_ID;
    }

    @VisibleForTesting
    static void parseLocationSearchResults(
            List<LocationSearchResult> results, InputStream inputStream)
            throws XmlPullParserException, IOException {
        XmlPullParser xpp = sXmlPullParserFactory.newPullParser();
        xpp.setInput(new InputStreamReader(inputStream));

        LocationSearchResult result = null;
        String name = null, country = null, admin1 = null;
        String timezone = null;
        StringBuilder sb = new StringBuilder();

        int state = PARSE_STATE_NONE;
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = xpp.getName();

            if (eventType == XmlPullParser.START_TAG) {
                switch (state) {
                    case PARSE_STATE_NONE:
                        if ("place".equals(tagName)) {
                            state = PARSE_STATE_PLACE;
                            result = new LocationSearchResult();
                            name = country = admin1 = null;
                        }
                        break;

                    case PARSE_STATE_PLACE:
                        if ("name".equals(tagName)) {
                            state = PARSE_STATE_NAME;
                        } else if ("woeid".equals(tagName)) {
                            state = PARSE_STATE_WOEID;
                        } else if ("country".equals(tagName)) {
                            state = PARSE_STATE_COUNTRY;
                        } else if ("admin1".equals(tagName)) {
                            state = PARSE_STATE_ADMIN1;
                        } else if ("timezone".equals(tagName)) {
                            state = PARSE_STATE_TIMEZONE;
                        }
                        break;
                }

            } else if (eventType == XmlPullParser.TEXT) {
                switch (state) {
                    case PARSE_STATE_WOEID:
                        result.woeid = xpp.getText();
                        break;

                    case PARSE_STATE_NAME:
                        name = xpp.getText();
                        break;

                    case PARSE_STATE_COUNTRY:
                        country = xpp.getText();
                        break;

                    case PARSE_STATE_ADMIN1:
                        admin1 = xpp.getText();
                        break;

                    case PARSE_STATE_TIMEZONE:
                        timezone = xpp.getText();
                        break;
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if ("place".equals(tagName)) {
//                        // Sort by descending tag name to order by decreasing precision
//                        // (locality3, locality2, locality1, admin3, admin2, admin1, etc.)
//                        Collections.sort(alternateWoeids, new Comparator<Pair<String, String>>() {
//                            @Override
//                            public int compare(Pair<String, String> pair1,
//                                    Pair<String, String> pair2) {
//                                return pair1.first.compareTo(pair2.first);
//                            }
//                        });
                    sb.setLength(0);
                    if (!TextUtils.isEmpty(name)) {
                        sb.append(name);
                    }
                    if (!TextUtils.isEmpty(admin1)) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(admin1);
                    }
                    result.displayName = sb.toString();
                    result.country = country;
                    result.timezone = timezone;
                    results.add(result);
                    state = PARSE_STATE_NONE;

                } else if (state != PARSE_STATE_NONE) {
                    state = PARSE_STATE_PLACE;
                }
            }

            eventType = xpp.next();
        }
    }

    static class YahooWeatherApiConfig {

        public static final String APP_ID
                = "kGO140TV34HVTae_DDS93fM_w3AJmtmI23gxUFnHKWyrOGcRzoFjYpw8Ato6BxhvbTg-";
    }

    public static class LocationInfo {

        // Sorted by decreasing precision
        // (point of interest, locality3, locality2, locality1, admin3, admin2, admin1, etc.)
        public final ArrayList<String> woeids = new ArrayList<String>();

        public String town;

        public String country;

        public String timezone;
    }

    public static class LocationSearchResult implements Parcelable {

        public static final Creator<LocationSearchResult> CREATOR =
                new Creator<LocationSearchResult>() {
                    @Override
                    public LocationSearchResult createFromParcel(Parcel in) {
                        return new LocationSearchResult(in);
                    }

                    @Override
                    public LocationSearchResult[] newArray(int size) {
                        return new LocationSearchResult[size];
                    }
                };

        public String woeid;

        public String displayName;

        public String country;

        public String timezone;

        public LocationSearchResult() {

        }

        protected LocationSearchResult(Parcel in) {
            woeid = in.readString();
            displayName = in.readString();
            country = in.readString();
            timezone = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(woeid);
            dest.writeString(displayName);
            dest.writeString(country);
            dest.writeString(timezone);
        }
    }
}