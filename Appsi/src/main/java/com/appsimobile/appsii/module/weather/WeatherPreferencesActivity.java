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

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import com.appsimobile.appsii.ActivityUtils;
import com.appsimobile.appsii.R;

/**
 * Created by nick on 22/04/15.
 */
public class WeatherPreferencesActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtils.setContentView(this, R.layout.preference_view);
        Fragment fragment = getFragmentManager().findFragmentByTag("weather_fragment");
        if (fragment == null) {
            fragment = new WeatherPreferencesFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment, "weather_fragment")
                    .commit();
        }
        ActivityUtils.setupToolbar(this, R.id.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    public static class WeatherPreferencesFragment extends PreferenceFragment {

        Context mContext;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mContext = getActivity();

            addPreferencesFromResource(R.xml.prefs_weather);

        }

    }

}
