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

package com.appsimobile.appsii.module.home;

import android.content.Context;
import android.content.Intent;
import android.support.test.espresso.matcher.ViewMatchers;
import android.test.ActivityInstrumentationTestCase2;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationFactory;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.module.home.config.MockHomeItemConfiguration;
import com.appsimobile.appsii.module.home.provider.HomeContract;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by nick on 24/03/15.
 */
public class WeatherActivityTest extends ActivityInstrumentationTestCase2<CellWeatherActivity> {

    CellWeatherActivity mWeatherActivity;

    MockHomeItemConfiguration mConfiguration;

    public WeatherActivityTest() {
        super(CellWeatherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Intent i = new Intent(getInstrumentation().getContext(), CellWeatherActivity.class);
        i.putExtra(CellWeatherActivity.EXTRA_CELL_ID, 1L);
        i.putExtra(CellWeatherActivity.EXTRA_CELL_TYPE,
                HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP);
        setActivityIntent(i);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        nullifyStaticFields(HomeItemConfigurationHelper.class);
        nullifyStaticFields(YahooLocationChooserDialogFragment.class);
    }

    /**
     * This function is called by various TestCase implementations, at tearDown() time, in order
     * to scrub out any class variables.  This protects against memory leaks in the case where a
     * test case creates a non-static inner class (thus referencing the test case) and gives it to
     * someone else to hold onto.
     *
     * @param testCaseClass The class of the derived TestCase implementation.
     *
     * @throws IllegalAccessException
     */
    protected void nullifyStaticFields(final Class<?> testCaseClass)
            throws IllegalAccessException {
        final Field[] fields = testCaseClass.getDeclaredFields();
        for (Field field : fields) {
            if (!field.getType().isPrimitive()
                    && (field.getModifiers() & Modifier.STATIC) != 0
                    && (field.getModifiers() & Modifier.FINAL) == 0) {
                try {
                    field.setAccessible(true);
                    field.set(null, null);
                } catch (Exception e) {
                    android.util.Log.d("TestCase", "Error: Could not nullify field!");
                }

                if (field.get(null) != null) {
                    android.util.Log.d("TestCase", "Error: Could not nullify field!");
                }
            }
        }
    }

    public void testPreSelections_setToImperial() {

        HomeItemConfigurationHelper.setFactory(new MockImperialLocationFactory());

        mWeatherActivity = getActivity();
        mConfiguration = (MockHomeItemConfiguration) HomeItemConfigurationHelper.
                getInstance(mWeatherActivity);


        onView(withId(R.id.weather_location)).check(matches(withText("mock_location")));
        onView(withId(R.id.weather_unit)).check(matches(withText(R.string.imperial)));
    }

    public void testPreSelections_setToMetric() {

        HomeItemConfigurationHelper.setFactory(new MockMetricLocationFactory());

        mWeatherActivity = getActivity();
        mConfiguration = (MockHomeItemConfiguration) HomeItemConfigurationHelper.
                getInstance(mWeatherActivity);


        onView(withId(R.id.weather_location)).check(matches(withText("mock_location")));
        onView(withId(R.id.weather_unit)).check(matches(withText(R.string.metric)));
    }

    public void testPreSelections_none() {

        HomeItemConfigurationHelper.setFactory(new MockLocationFactory());

        mWeatherActivity = getActivity();
        mConfiguration = (MockHomeItemConfiguration) HomeItemConfigurationHelper.
                getInstance(mWeatherActivity);

        onView(withId(R.id.weather_location)).check(
                matches(withText(R.string.weather_auto_location)));
        onView(withId(R.id.weather_unit)).check(matches(withText(R.string.metric)));
    }

    public void testPickLocation_locationDisabled() {

        HomeItemConfigurationHelper.setFactory(new MockLocationFactory());

        YahooLocationChooserDialogFragment.sLocationUpdateHelper = new DisabledLocationUpdate();

        mWeatherActivity = getActivity();
        mConfiguration = (MockHomeItemConfiguration) HomeItemConfigurationHelper.
                getInstance(mWeatherActivity);

        onView(withId(R.id.weather_location)).perform(click());
        onView(withId(R.id.location_title)).check(matches(withText(R.string.cant_access_location)));

        onView(withId(R.id.location_search)).
                check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.location_unavailable)).
                check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.cancel_enable_location_button)).perform(click());
        onView(withId(R.id.location_unavailable)).
                check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // TODO choose auto option and verify

    }

    public void testPickLocation_locationPermissionDenied() {

        HomeItemConfigurationHelper.setFactory(new MockLocationFactory());

        YahooLocationChooserDialogFragment.sLocationUpdateHelper =
                new PermissionDeniedLocationUpdate();

        mWeatherActivity = getActivity();
        mConfiguration = (MockHomeItemConfiguration) HomeItemConfigurationHelper.
                getInstance(mWeatherActivity);

        onView(withId(R.id.weather_location)).perform(click());
        onView(withId(R.id.location_title)).check(
                matches(withText(R.string.location_permission_denied)));

        onView(withId(R.id.location_search)).
                check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.location_unavailable)).
                check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.cancel_enable_location_button)).perform(click());
        onView(withId(R.id.location_unavailable)).
                check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // TODO choose auto option and verify

    }

    private static class MockImperialLocationFactory implements HomeItemConfigurationFactory {

        MockImperialLocationFactory() {
        }

        @Override
        public HomeItemConfiguration createInstance(Context context) {
            MockHomeItemConfiguration configuration =
                    new MockHomeItemConfiguration(context);
            configuration.initProperty(WeatherFragment.PREFERENCE_WEATHER_UNIT, "f");
            configuration.initProperty(WeatherFragment.PREFERENCE_WEATHER_LOCATION,
                    "mock_location");
            configuration.initProperty(WeatherFragment.PREFERENCE_WEATHER_WOEID, "10000");
            configuration.initProperty(WeatherFragment.PREFERENCE_WEATHER_TIMEZONE,
                    "Europe/Amsterdam");

            return configuration;
        }
    }

    private static class MockMetricLocationFactory implements HomeItemConfigurationFactory {

        MockMetricLocationFactory() {
        }

        @Override
        public HomeItemConfiguration createInstance(Context context) {
            MockHomeItemConfiguration configuration =
                    new MockHomeItemConfiguration(context);
            configuration.initProperty(WeatherFragment.PREFERENCE_WEATHER_UNIT, "c");
            configuration.initProperty(WeatherFragment.PREFERENCE_WEATHER_LOCATION,
                    "mock_location");
            configuration.initProperty(WeatherFragment.PREFERENCE_WEATHER_WOEID, "10000");
            configuration.initProperty(WeatherFragment.PREFERENCE_WEATHER_TIMEZONE,
                    "Europe/Amsterdam");

            return configuration;
        }
    }

    private static class DisabledLocationUpdate
            extends YahooLocationChooserDialogFragment.AbstractDefaultLocationUpdate {

        DisabledLocationUpdate() {
        }

        @Override
        protected int doStartLocationUpdateIfNeeded(Context context, int passedMinutes) {
            return YahooLocationChooserDialogFragment.LOCATION_REQUEST_RESULT_DISABLED;
        }

        @Override
        public void setFragment(YahooLocationChooserDialogFragment fragment) {

        }

        @Override
        public void onStop() {

        }
    }

    private static class PermissionDeniedLocationUpdate
            extends YahooLocationChooserDialogFragment.AbstractDefaultLocationUpdate {

        PermissionDeniedLocationUpdate() {
        }

        @Override
        protected int doStartLocationUpdateIfNeeded(Context context, int passedMinutes) {
            return YahooLocationChooserDialogFragment.LOCATION_REQUEST_RESULT_PERMISSION_DENIED;
        }

        @Override
        public void setFragment(YahooLocationChooserDialogFragment fragment) {

        }

        @Override
        public void onStop() {

        }
    }

    private class MockLocationFactory implements HomeItemConfigurationFactory {

        MockLocationFactory() {
        }

        @Override
        public HomeItemConfiguration createInstance(Context context) {
            return new MockHomeItemConfiguration(context);
        }
    }


}