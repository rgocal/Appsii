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

package com.appsimobile.appsii;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


public class MainActivity extends AppCompatActivity {

    AppsiServiceStatusView mAppsiServiceStatusView;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            BootCompleteReceiver.autoStartAppsi(this);
            ActivityUtils.setContentView(this, R.layout.activity_main);
            ActivityUtils.setupToolbar(this, R.id.toolbar);

            mAppsiServiceStatusView = (AppsiServiceStatusView) findViewById(R.id.running_status);
        } catch (RuntimeException e) {
            Log.w("MainActivity", "", e);
            throw e;
        }
    }

    public static class MainPreferencesFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // There are two of these. One for each flavor.
            // The community version does not contain the pages option
            addPreferencesFromResource(R.xml.prefs_main);
        }

    }

}
