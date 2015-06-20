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
 *
 */

package com.appsimobile.appsii;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.appsimobile.appsii.module.weather.loader.CantGetWeatherException;
import com.appsimobile.appsii.module.weather.loader.YahooWeatherApiClient;

import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * Created by nick on 18/06/15.
 */
public class LocationLoader implements LocationListener {

    final LocationReceiver mLocationReceiver;

    LocationManager mLocationManager;

    AsyncTask<Location, Void, YahooWeatherApiClient.LocationInfo> mTask;

    public LocationLoader(LocationReceiver locationReceiver) {
        mLocationReceiver = locationReceiver;
    }

    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public void requestLocationUpdate(Context context) throws SecurityException {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnown =
                mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnown != null) {
            onLocationChanged(lastKnown);
        }
        mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
    }

    @Override
    public void onLocationChanged(final Location location) {
        if (mTask != null) {
            mTask.cancel(true);
        }
        mTask = new AsyncTask<Location, Void, YahooWeatherApiClient.LocationInfo>() {
            @Override
            protected YahooWeatherApiClient.LocationInfo doInBackground(
                    Location... locations) {
                Location location = locations[0];
                try {
                    return YahooWeatherApiClient.getLocationInfo(location);
                } catch (CantGetWeatherException e) {
                    Log.w("WeatherFragment", "Error getting locationInfo", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(
                    YahooWeatherApiClient.LocationInfo locationInfo) {
                onLocationInfoLoaded(locationInfo);
            }
        };
        mTask.execute(location);
    }

    void onLocationInfoLoaded(@Nullable YahooWeatherApiClient.LocationInfo locationInfo) {
        List<String> woeids = locationInfo == null ?
                Collections.<String>emptyList() : locationInfo.woeids;

        String town = locationInfo == null ? null : locationInfo.town;
        String country = locationInfo == null ? null : locationInfo.country;
        String timezone = locationInfo == null ? null : locationInfo.timezone;

        Log.i("WeatherFragment",
                "town: " + town + " country: " + country + " woeids: " + woeids);
        if (!woeids.isEmpty() && town != null) {
            String woeid = woeids.get(0);
            mLocationReceiver.onCurrentLocationInfoReady(woeid, country, town, timezone);
        } else {
            mLocationReceiver.onCurrentLocationInfoReady(null, null, null, null);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public void destroy() {
        if (mTask != null) {
            mTask.cancel(true);
        }
    }
}
