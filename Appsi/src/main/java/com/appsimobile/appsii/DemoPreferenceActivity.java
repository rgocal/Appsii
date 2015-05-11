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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * Created by Nick Martens on 6/9/13.
 */
public class DemoPreferenceActivity extends PreferenceActivity
        implements Preference.OnPreferenceClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preference_view);
        addPreferencesFromResource(R.xml.prefs_demo);

        Preference openDemo = findPreference("demo_open_sidebar");
        openDemo.setOnPreferenceClickListener(this);
        Preference browseOnline = findPreference("demo_browse_online");
        browseOnline.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        String page;
        if (key.equals("demo_browse_online")) {
            page = "http://appsii.appsimobile.com/videos/instructions";

        } else if (key.equals("demo_open_sidebar")) {
            page = "http://youtu.be/l9qxwHz4FEk";

        } else {
            page = null;

        }
        if (page != null) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(page));
            startActivity(i);
            return true;
        }
        return false;
    }
}
