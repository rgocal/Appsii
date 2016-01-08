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
import android.support.v4.util.CircularArray;
import android.util.Log;

import com.appsimobile.appsii.module.weather.loader.CantGetWeatherException;
import com.appsimobile.appsii.module.weather.loader.YahooWeatherApiClient;

import java.lang.ref.WeakReference;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * Utility helper to load location info
 * Created by nick on 18/06/15.
 */
public class LocationLoader {

    final LocationReceiver mLocationReceiver;

    LocationManager mLocationManager;

    AsyncTask<Location, Void, YahooWeatherApiClient.LocationInfo> mTask;

    /**
     * True when the loader has been destroyed
     */
    boolean mDestroyed;

    private LocationListenerImpl mLocationListener;

    public LocationLoader(LocationReceiver locationReceiver) {
        mLocationReceiver = locationReceiver;
    }

    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public boolean requestLocationUpdate(Context context) throws SecurityException {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return false;
        }
        
        Location lastKnown =
                mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnown != null) {
            onLocationChanged(lastKnown);
        }
        mLocationListener = new LocationListenerImpl(this);
        mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,
                mLocationListener, null);
        return true;
    }

    public void onLocationChanged(final Location location) {
        if (mDestroyed) return;
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
        CircularArray<String> woeids = locationInfo == null ?
                null : locationInfo.woeids;

        String town = locationInfo == null ? null : locationInfo.town;
        String country = locationInfo == null ? null : locationInfo.country;
        String timezone = locationInfo == null ? null : locationInfo.timezone;

        Log.i("WeatherFragment",
                "town: " + town + " country: " + country + " woeids: " + woeids);
        if (woeids != null && town != null) {
            String woeid = woeids.get(0);
            mLocationReceiver.onCurrentLocationInfoReady(woeid, country, town, timezone);
        } else {
            mLocationReceiver.onCurrentLocationInfoReady(null, null, null, null);
        }
    }


    public void destroy() {
        mDestroyed = true;
        if (mLocationListener != null) {
            mLocationListener.destroy();
        }
        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    private static class LocationListenerImpl implements LocationListener {

        WeakReference<LocationLoader> mLocationLoaderRef;

        public LocationListenerImpl(LocationLoader locationLoader) {
            mLocationLoaderRef = new WeakReference<>(locationLoader);
        }

        @Override
        public void onLocationChanged(Location location) {
            LocationLoader locationLoader = mLocationLoaderRef.get();
            if (locationLoader != null) {
                locationLoader.onLocationChanged(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        public void destroy() {
            mLocationLoaderRef.clear();
        }
    }
}
