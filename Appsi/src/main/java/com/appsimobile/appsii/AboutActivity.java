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
import android.support.annotation.XmlRes;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * Created by Nick Martens on 6/21/13.
 */
public class AboutActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtils.setContentView(this, R.layout.preference_view);
        AboutPreferenceFragment
                fragment = (AboutPreferenceFragment) getFragmentManager().
                findFragmentByTag("prefs");

        if (fragment == null) {
            fragment = AboutPreferenceFragment.createInstance(
                    R.xml.prefs_about);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment, "prefs")
                    .commit();
        }
        ActivityUtils.setupToolbar(this, R.id.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public static class AboutPreferenceFragment
            extends PreferenceFragmentImpl
            implements Preference.OnPreferenceClickListener {

        AnalyticsManager mAnalyticsManager = AnalyticsManager.getInstance();

        public static AboutPreferenceFragment createInstance(
                @XmlRes int preferenceResId) {
            Bundle args = new Bundle();
            args.putInt("preference_res_id", preferenceResId);

            AboutPreferenceFragment result = new AboutPreferenceFragment();
            result.setArguments(args);
            return result;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Preference pref = findPreference("pref_appsi_version");
            String version = BuildConfig.VERSION_NAME;
            pref.setSummary(version);
            setPreferenceUri("google_plus_community",
                    "https://plus.google.com/communities/111374377186674137148",
                    AnalyticsManager.ACTION_OPEN_GOOGLE_COMMUNITY);

            setPreferenceUri("google_moderator",
                    "http://www.google.com/moderator/#16/e=215186",
                    AnalyticsManager.ACTION_OPEN_GOOGLE_MODERATOR);

            setPreferenceUri("beta_tester",
                    "http://appsi.appsimobile.com/become-a-beta-tester",
                    AnalyticsManager.ACTION_OPEN_BETA_TESTER);

            setPreferenceUri("me_google_plus",
                    "https://plus.google.com/+NickMartens/about",
                    AnalyticsManager.ACTION_OPEN_FOLLOW_ME);

        }

        private void setPreferenceUri(String key, final String url,
                final String trackinAction) {
            Preference gpPref = findPreference(key);
            gpPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    track(trackinAction, AnalyticsManager.CATEGORY_ABOUT);
                    return true;
                }
            });


        }

        public void track(String action, String category) {
            mAnalyticsManager.trackAppsiEvent(action, category);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            StringBuffer result = new StringBuffer();
            Intent i = ErrorHandler.createTroubleshootingReport(getActivity(), result);
            try {
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.no_email_client, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
    }


}
