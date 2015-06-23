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

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;

import com.appsimobile.appsii.module.home.WeatherFragment;
import com.appsimobile.appsii.preference.PreferenceHelper;
import com.appsimobile.appsii.preference.PreferencesFactory;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class WeatherSyncAdapter extends AbstractThreadedSyncAdapter {

    // Global variables
    // Define a variable to contain a content resolver instance
    final ContentResolver mContentResolver;

    /**
     * Set up the sync adapter
     */
    public WeatherSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public WeatherSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {

        WeatherLoadingService service = new WeatherLoadingService(getContext());
        String extraWoeid =
                extras == null ? null : extras.getString(WeatherLoadingService.EXTRA_INCLUDE_WOEID);
        String defaultUnit =
                extras == null ? null : extras.getString(WeatherLoadingService.EXTRA_UNIT);

        if (defaultUnit == null) {
            PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getContext());
            String systemDefault = preferenceHelper.getDefaultWeatherTemperatureUnit();

            SharedPreferences prefs = PreferencesFactory.getPreferences(getContext());
            defaultUnit = prefs.getString(WeatherFragment.PREFERENCE_WEATHER_UNIT, systemDefault);
        }

        service.doSync(defaultUnit, extraWoeid, syncResult);
    }
}
