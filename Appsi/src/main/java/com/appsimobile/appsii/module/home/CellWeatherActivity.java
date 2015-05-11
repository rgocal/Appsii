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

package com.appsimobile.appsii.module.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.appsimobile.appsii.ActivityUtils;
import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.R;

/**
 * The activity that shows the options for a weather cell. This is handled by a fragment.
 * Created by nick on 22/01/15.
 */
public class CellWeatherActivity extends AppCompatActivity {

    static final String EXTRA_CELL_ID = BuildConfig.APPLICATION_ID + ".cell_id";

    static final String EXTRA_CELL_TYPE = BuildConfig.APPLICATION_ID + ".cell_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtils.setContentView(this, R.layout.activity_cell_options);

        handleIntent(getIntent(), false);
    }

    private void handleIntent(Intent intent, boolean isNewIntent) {
        long cellId = intent.getLongExtra(EXTRA_CELL_ID, -1);
        int cellType = intent.getIntExtra(EXTRA_CELL_TYPE, -1);

        WeatherFragment fragment =
                (WeatherFragment) getFragmentManager().findFragmentByTag("weather");

        if (isNewIntent && fragment != null) {
            getFragmentManager().beginTransaction().remove(fragment).commit();
        }


        if (isNewIntent || fragment == null) {
            fragment = WeatherFragment.createInstance(cellId, cellType);
            getFragmentManager().beginTransaction().
                    add(R.id.container, fragment, "weather").
                    commit();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent, true);
    }
}
