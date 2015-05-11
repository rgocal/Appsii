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

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.util.Pair;
import android.util.SparseArray;

import com.appsimobile.appsii.module.weather.loader.WeatherData;
import com.appsimobile.util.TimeUtils;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class WeatherDataLoader extends
        AsyncTaskLoader<Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>>> {

    final Context mContext;

    final String mWoeid;

    Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>> mLastResult;

    ForceLoadContentObserver mForceLoadContentObserver;

    public WeatherDataLoader(Context context, String woeid) {
        super(context);

        mContext = context;
        mWoeid = woeid;
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>> apps) {
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(apps);
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override
    public Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>> loadInBackground() {
        WeatherData weatherData = WeatherUtils.getWeatherData(mContext, mWoeid);
        int today = TimeUtils.getJulianDay();
        SparseArray<WeatherUtils.ForecastInfo> forecast =
                WeatherUtils.getForecastForDays(mContext, today, mWoeid);

        return new Pair<>(weatherData, forecast);
    }


    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(
            Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>> apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>> oldApps = mLastResult;
        mLastResult = apps;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(apps);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }


    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mLastResult != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mLastResult);
        }

        if (mForceLoadContentObserver == null) {
            mForceLoadContentObserver = new ForceLoadContentObserver();
            ContentResolver contentResolver = mContext.getContentResolver();

            contentResolver.registerContentObserver(WeatherContract.ForecastColumns.CONTENT_URI,
                    true, mForceLoadContentObserver);

            contentResolver.registerContentObserver(WeatherContract.WeatherColumns.CONTENT_URI,
                    true, mForceLoadContentObserver);
        }

        if (takeContentChanged() || mLastResult == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (mLastResult != null) {
            onReleaseResources(mLastResult);
            mLastResult = null;
        }

        if (mForceLoadContentObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mForceLoadContentObserver);
        }
    }


}