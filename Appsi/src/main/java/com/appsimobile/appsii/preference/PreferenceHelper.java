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

package com.appsimobile.appsii.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.appsimobile.appsii.module.weather.loader.YahooWeatherApiClient;

/**
 * Created by nick on 22/04/15.
 */
public class PreferenceHelper {

    private static final Object INSTANCE_MUTEX = new Object();

    private static PreferenceHelper sInstance;

    private final Context mContext;

    private final SharedPreferences mSharedPreferences;

    public PreferenceHelper(Context context, SharedPreferences preferences) {
        mContext = context.getApplicationContext();
        mSharedPreferences = preferences;
    }

    public boolean getAutoStart() {
        return mSharedPreferences.getBoolean("pref_autostart", true);
    }

    public void setAutoStart(boolean autoStart) {
        mSharedPreferences.edit().putBoolean("pref_autostart", autoStart).apply();
    }

    public String getDefaultWeatherTemperatureUnit() {
        return mSharedPreferences.getString("weather_default_unit", "c");
    }

    public void setDefaultWeatherTemperatureUnit(String unit) {
        mSharedPreferences.edit().putString("weather_default_unit", unit).apply();
    }

    public String getDefaultWeatherTitleType() {
        return mSharedPreferences.getString("weather_default_title_type", "location");
    }

    public boolean getUseFlickrImages() {
        return mSharedPreferences.getBoolean("weather_use_flickr_images", true);
    }

    public boolean getDownloadImagesOnWifiOnly() {
        return mSharedPreferences.getBoolean("weather_flickr_wifi_only", true);
    }

    public boolean getDownloadWhenRoaming() {
        return mSharedPreferences.getBoolean("weather_flickr_roaming", false);
    }

    public boolean getSyncWhenRoaming() {
        return mSharedPreferences.getBoolean("weather_sync_when_roaming", false);
    }

    public boolean getHotspotsHapticFeedbackEnabled() {
        return mSharedPreferences.getBoolean("pref_sidebar_haptic_feedback", false);
    }

    public boolean getHotspotsHidden() {
        return mSharedPreferences.getBoolean("pref_hide_hotspots", false);
    }

    public int getSidebarDimLevel() {
        return mSharedPreferences.getInt("pref_sidebar_dimming_level", 50);
    }

    public int getSidebarWidth() {
        return mSharedPreferences.getInt("pref_sidebar_size", 80);
    }

    public void updateDefaultUserLocation(YahooWeatherApiClient.LocationSearchResult result) {
        mSharedPreferences.edit().
                putString("default_user_country", result.country).
                putString("default_user_display_name", result.displayName).
                putString("default_user_timezone", result.timezone).
                putString("default_user_woeid", result.woeid).
                apply();
    }

    @Nullable
    public YahooWeatherApiClient.LocationSearchResult getDefaultUserLocation() {
        if (!mSharedPreferences.contains("default_user_woeid")) return null;

        YahooWeatherApiClient.LocationSearchResult result =
                new YahooWeatherApiClient.LocationSearchResult();
        result.country = getDefaultLocationCountry();
        result.displayName = getDefaultLocationDisplayName();
        result.timezone = getDefaultLocationTimezone();
        result.woeid = getDefaultLocationWoeId();

        return result;
    }

    public String getDefaultLocationWoeId() {
        return mSharedPreferences.getString("default_user_woeid", null);
    }

    public String getDefaultLocationCountry() {
        return mSharedPreferences.getString("default_user_country", null);
    }

    public String getDefaultLocationTimezone() {
        return mSharedPreferences.getString("default_user_timezone", null);
    }

    public String getDefaultLocationDisplayName() {
        return mSharedPreferences.getString("default_user_display_name", null);
    }
}
