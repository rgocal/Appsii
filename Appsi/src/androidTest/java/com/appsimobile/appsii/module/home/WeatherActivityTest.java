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

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.util.LongSparseArray;
import android.test.mock.MockContentProvider;

import com.appsimobile.appsii.MockApplicationComponent;
import com.appsimobile.appsii.MockAppsiApplication;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.YahooLocationChooserDialogFragment.LocationUpdateHelper;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration.ConfigurationProperty;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper
        .HomeItemConfigurationLoader;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.permissions.PermissionUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import javax.inject.Inject;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.appsimobile.appsii.module.home.YahooLocationChooserDialogFragment
        .LOCATION_REQUEST_RESULT_PERMISSION_DENIED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by nick on 24/03/15.
 */
@RunWith(AndroidJUnit4.class)
public class WeatherActivityTest {

    @Rule
    public ActivityTestRule<CellWeatherActivity> mActivityRule = new ActivityTestRule<>(
            CellWeatherActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False so we can customize the intent per test method

    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    PermissionUtils mPermissionUtils;

    @Inject
    HomeItemConfigurationLoader mHomeItemConfigurationLoader;

    @Inject
    LocationUpdateHelper mLocationUpdateHelper;

    MockContentProvider mContentProvider;

    ConfigurationProperty mConfigurationProperty;
    private Intent mLaunchIntent;

    static Context anyContext() {
        return any(Context.class);
    }

    static <T> ArrayList<T> anyList(Class<T> c) {
        return any(ArrayList.class);
    }

    private static void initializeConfigurationProperty(ConfigurationProperty prop,
            String unit, String location, String woeid, String timezone) {

        prop.put(WeatherFragment.PREFERENCE_WEATHER_UNIT, unit)
                .put(WeatherFragment.PREFERENCE_WEATHER_LOCATION, location)
                .put(WeatherFragment.PREFERENCE_WEATHER_WOEID, woeid)
                .put(WeatherFragment.PREFERENCE_WEATHER_TIMEZONE, timezone);
    }

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        MockAppsiApplication app =
                (MockAppsiApplication) instrumentation.getTargetContext().getApplicationContext();
        MockApplicationComponent component =
                (MockApplicationComponent) app.getApplicationComponent();
        component.inject(this);

        mContentProvider = new MockContentProvider(app);
        Mockito.reset(mSharedPreferences, mPermissionUtils);

        when(mSharedPreferences.getBoolean(eq("cling_preferences_shown"), anyBoolean()))
                .thenReturn(true);

        mLaunchIntent = new Intent(app, CellWeatherActivity.class);
        mLaunchIntent.putExtra(CellWeatherActivity.EXTRA_CELL_ID, 1L);
        mLaunchIntent.putExtra(CellWeatherActivity.EXTRA_CELL_TYPE,
                HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP);

        mConfigurationProperty = new ConfigurationProperty();
    }

    @After
    public void tearDown() throws Exception {
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

    @Test
    public void testPreSelections_setToImperial() {
        LongSparseArray<ConfigurationProperty> result = new LongSparseArray<>();

        initializeConfigurationProperty(
                mConfigurationProperty, "f", "mock_location", "10000", "Europe/Amsterdam");

        result.put(1, mConfigurationProperty);
        when(mHomeItemConfigurationLoader.loadConfigurations(any(Context.class)))
                .thenReturn(result);

        mActivityRule.launchActivity(mLaunchIntent);

        onView(withId(R.id.weather_location)).check(matches(withText("mock_location")));
        onView(withId(R.id.weather_unit)).check(matches(withText(R.string.imperial)));
    }

    @Test
    public void testPreSelections_setToMetric() {
        LongSparseArray<ConfigurationProperty> result = new LongSparseArray<>();
        initializeConfigurationProperty(
                mConfigurationProperty, "c", "mock_location", "10000", "Europe/Amsterdam");

        result.put(1, mConfigurationProperty);
        when(mHomeItemConfigurationLoader.loadConfigurations(any(Context.class)))
                .thenReturn(result);

        mActivityRule.launchActivity(mLaunchIntent);

        onView(withId(R.id.weather_location)).check(matches(withText("mock_location")));
        onView(withId(R.id.weather_unit)).check(matches(withText(R.string.metric)));
    }

    @Test
    public void testPreSelections_none() {
        LongSparseArray<ConfigurationProperty> result = new LongSparseArray<>();
        when(mHomeItemConfigurationLoader.loadConfigurations(any(Context.class)))
                .thenReturn(result);

        result.put(1, mConfigurationProperty);
        when(mHomeItemConfigurationLoader.loadConfigurations(any(Context.class)))
                .thenReturn(result);

        mActivityRule.launchActivity(mLaunchIntent);

        onView(withId(R.id.weather_location)).check(
                matches(withText(R.string.weather_auto_location)));
        onView(withId(R.id.weather_unit)).check(matches(withText(R.string.metric)));
    }

    @Test
    public void testPickLocation_locationDisabled() {
        when(mHomeItemConfigurationLoader.loadConfigurations(any(Context.class)))
                .thenReturn(new LongSparseArray<ConfigurationProperty>());

        when(mLocationUpdateHelper.startLocationUpdateIfNeeded(Mockito.any(Context.class)))
                .thenReturn(YahooLocationChooserDialogFragment.LOCATION_REQUEST_RESULT_DISABLED);

        mActivityRule.launchActivity(mLaunchIntent);

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

    @Test
    public void testPickLocation_locationPermissionDenied() {
        when(mLocationUpdateHelper.startLocationUpdateIfNeeded(Mockito.any(Context.class)))
                .thenReturn(LOCATION_REQUEST_RESULT_PERMISSION_DENIED);

        LongSparseArray<ConfigurationProperty> result = new LongSparseArray<>();
        when(mHomeItemConfigurationLoader.loadConfigurations(any(Context.class)))
                .thenReturn(result);

        mActivityRule.launchActivity(mLaunchIntent);

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

}