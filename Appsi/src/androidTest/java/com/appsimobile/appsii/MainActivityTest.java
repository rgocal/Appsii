/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.appsii;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.appsimobile.appsii.preference.PreferencesFactory;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by nick on 14/01/15.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    MainActivity mMainActivity;

    SimplePreferences mPreferences;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPreferences = new SimplePreferences();
        mPreferences.put("cling_preferences_shown", Boolean.TRUE);
        PreferencesFactory.setPreferences(mPreferences);
        mMainActivity = getActivity();
    }

    public void testStartAndStopAppsii() throws InterruptedException {
//        onView(withId(R.id.status_view)).check(matches(withText(R.string.appsi_status_stopped)));
        Thread.sleep(1000);
        onView(withId(R.id.status_view)).check(matches(withText(R.string.appsi_status_running)));
        mMainActivity.stopService(new Intent(mMainActivity, Appsi.class));
        Thread.sleep(500);
        onView(withId(R.id.status_view)).check(matches(withText(R.string.appsi_status_stopped)));
    }

}
