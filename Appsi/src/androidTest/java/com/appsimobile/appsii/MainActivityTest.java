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

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import com.appsimobile.appsii.permissions.PermissionUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import javax.inject.Inject;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;

/**
 * Created by nick on 14/01/15.
 */
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False so we can customize the intent per test method


    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    PermissionUtils mPermissionUtils;

    MainActivity mMainActivity;

    static Context anyContext() {
        return org.mockito.Matchers.any(Context.class);
    }

    static <T> ArrayList<T> anyList(Class<T> c) {
        return org.mockito.Matchers.any(ArrayList.class);
    }

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        MockAppsiApplication app =
                (MockAppsiApplication) instrumentation.getTargetContext().getApplicationContext();
        MockApplicationComponent component =
                (MockApplicationComponent) app.getApplicationComponent();
        component.inject(this);

        Mockito.reset(mSharedPreferences, mPermissionUtils);

        Mockito.when(mSharedPreferences.getBoolean(eq("cling_preferences_shown"), anyBoolean())).thenReturn(true);

        mMainActivity = mActivityRule.launchActivity(null);

    }

    @Test
    public void testStartAndStopAppsii() throws InterruptedException {

//        onView(withId(R.id.status_view)).check(matches(withText(R.string.appsi_status_stopped)));
        Thread.sleep(1000);
        onView(withId(R.id.status_view)).check(matches(withText(R.string.appsi_status_running)));
        mMainActivity.stopService(new Intent(mMainActivity, Appsi.class));
        Thread.sleep(500);
        onView(withId(R.id.status_view)).check(matches(withText(R.string.appsi_status_stopped)));
    }

}
