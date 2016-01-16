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

package com.appsimobile.appsii.module.appsisettings;

import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.compat.MapCompat;

import java.util.Map;

/**
 * Created by Nick Martens on 8/7/13.
 */
public class SettingsDefinition {


    // header
    private static final int WIRELESS_AND_NETWORKS =
            R.string.settings_header_category_wireless_networks;

    private static final String WIFI_SETTINGS = Settings.ACTION_WIFI_SETTINGS;

    private static final int WIFI_SETTINGS_TITLE = R.string.settings_wifi_settings_title;

    private static final int WIFI_SETTINGS_ICON = R.drawable.ic_settings_wireless;

    private static final String BLUETOOTH_SETTINGS = Settings.ACTION_BLUETOOTH_SETTINGS;

    private static final int BLUETOOTH_SETTINGS_TITLE = R.string.settings_bluetooth_settings_title;

    private static final int BLUETOOTH_SETTINGS_ICON = R.drawable.ic_settings_bluetooth2;

    private static final String DATA_USAGE_SETTINGS = "_DATA_USAGE_";

    private static final int DATA_USAGE_SETTINGS_TITLE = R.string.settings_data_usage_summary_title;

    private static final int DATA_USAGE_SETTINGS_ICON = R.drawable.ic_settings_data_usage;

    private static final String OPERATOR_SETTINGS = Settings.ACTION_NETWORK_OPERATOR_SETTINGS;

    private static final int OPERATOR_TITLE = 0;

    private static final int OPERATOR_ICON = R.drawable.empty_icon;

    private static final String MORE_SETTINGS = Settings.ACTION_WIRELESS_SETTINGS;

    private static final int MORE_SETTINGS_TITLE = R.string.settings_radio_controls_title;

    private static final int MORE_SETTINGS_ICON = R.drawable.ic_empty;

    /* DEVICE SECTION */
    private static final int DEVICE = R.string.settings_header_category_device;

    private static final String SOUND_SETTINGS = Settings.ACTION_SOUND_SETTINGS;

    private static final int SOUND_SETTINGS_TITLE = R.string.settings_sound_settings;

    private static final int SOUND_SETTINGS_ICON = R.drawable.ic_settings_sound;

    private static final String DISPLAY_SETTINGS = Settings.ACTION_DISPLAY_SETTINGS;

    private static final int DISPLAY_SETTINGS_TITLE = R.string.settings_display_settings;

    private static final int DISPLAY_SETTINGS_ICON = R.drawable.ic_settings_display;

    private static final String STORAGE_SETTINGS = Settings.ACTION_INTERNAL_STORAGE_SETTINGS;

    private static final int STORAGE_SETTINGS_TITLE = R.string.settings_storage_settings;

    private static final int STORAGE_SETTINGS_ICON = R.drawable.ic_settings_storage;

    private static final String BATTERY_SETTINGS = Intent.ACTION_POWER_USAGE_SUMMARY;

    private static final int BATTERY_SETTINGS_TITLE = R.string.settings_power_usage_summary_title;

    private static final int BATTERY_SETTINGS_ICON = R.drawable.ic_settings_battery;

    private static final String APPLICATION_SETTINGS = Settings.ACTION_APPLICATION_SETTINGS;

    private static final int APPLICATION_SETTINGS_TITLE = R.string.settings_applications_settings;

    private static final int APPLICATION_SETTINGS_ICON = R.drawable.ic_settings_applications;

    private static final String MANAGE_USER_SETTINGS = Settings.ACTION_ADD_ACCOUNT;

    private static final int MANAGE_USERS_SETTINGS_TITLE = R.string.settings_user_settings_title;

    private static final int MANAGE_USERS_SETTINGS_ICON = R.drawable.ic_settings_multiuser;

    /* Personal section */
    private static final int PERSONAL = R.string.settings_header_category_personal;

    /*
    <!-- Manufacturer hook -->
    <header
    android:id="@+id/manufacturer_settings">
    <intent android:action="com.android.settings.MANUFACTURER_APPLICATION_SETTING" />
    </header>
    */

    private static final String LOCATION_SETTINGS = Settings.ACTION_LOCATION_SOURCE_SETTINGS;

    private static final int LOCATION_SETTINGS_TITLE = R.string.settings_location_settings_title;

    private static final int LOCATION_SETTINGS_ICON = R.drawable.ic_settings_location;

    private static final String SECURITY_SETTINGS = Settings.ACTION_SECURITY_SETTINGS;

    private static final int SECURITY_SETTINGS_TITLE = R.string.settings_security_settings_title;

    private static final int SECURITY_SETTINGS_ICON = R.drawable.ic_settings_security;

    private static final String LANGUAGE_SETTINGS = Settings.ACTION_LOCALE_SETTINGS;

    private static final int LANGUAGE_SETTINGS_TITLE = R.string.settings_language_settings;

    private static final int LANGUAGE_SETTINGS_ICON = R.drawable.ic_settings_language;

    private static final String BACKUP_SETTINGS = Settings.ACTION_PRIVACY_SETTINGS;

    private static final int BACKUP_SETTINGS_TITLE = R.string.settings_privacy_settings;

    private static final int BACKUP_SETTINGS_ICON = R.drawable.ic_settings_backup;

    /* Account section */
    private static final int ACCOUNTS = R.string.settings_account_settings;

    private static final String ACCOUNT_SETTINGS_SETTINGS = Settings.ACTION_SYNC_SETTINGS;

    private static final int ACCOUNT_SETTINGS_ICON = R.drawable.empty_icon;

    private static final int ACCOUNT_SETTINGS_TITLE = R.string.settings_account_settings;

    private static final int SYSTEM = R.string.settings_header_category_system;


    /* System section */

    private static final String DATE_TIME_SETTINGS = Settings.ACTION_DATE_SETTINGS;

    private static final int DATE_TIME_SETTINGS_TITLE =
            R.string.settings_date_and_time_settings_title;

    private static final int DATE_TIME_SETTINGS_ICON = R.drawable.ic_settings_date_time;

    private static final String ACCESSIBILITY_SETTINGS = Settings.ACTION_ACCESSIBILITY_SETTINGS;

    private static final int ACCESSIBILITY_SETTINGS_TITLE =
            R.string.settings_accessibility_settings;

    private static final int ACCESSIBILITY_SETTINGS_ICON = R.drawable.ic_settings_accessibility;

    private static final String PRINT_SETTINGS = Settings.ACTION_PRINT_SETTINGS;

    private static final int PRINT_SETTINGS_TITLE = R.string.settings_settings_printing_settings;

    private static final int PRINT_SETTINGS_ICON = R.drawable.ic_settings_print;

    private static final String DEVELOPMENT_SETTINGS =
            Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS;

    private static final int DEVELOPMENT_SETTINGS_TITLE =
            R.string.settings_development_settings_title;

    private static final int DEVELOPMENT_SETTINGS_ICON = R.drawable.ic_settings_development;

    private static final String ABOUT_SETTINGS = Settings.ACTION_DEVICE_INFO_SETTINGS;

    private static final int ABOUT_SETTINGS_TITLE = R.string.settings_about_settings;

    private static final int ABOUT_SETTINGS_ICON = R.drawable.ic_settings_about;

    private static final SettingsHelper.Section[] SECTIONS;

    private static final Map<String, Integer> ICON_LOOKUP = MapCompat.createMap();

    private static final String AIRPLANE_MODE_SETTINGS = Settings.ACTION_AIRPLANE_MODE_SETTINGS;

    private static final int AIRPLANE_MODE_SETTINGS_TITLE = R.string.settings_airplane_mode;


    /* Additional settings actions */

    private static final int AIRPLANE_MODE_SETTINGS_ICON = R.drawable.ic_airplane_mode;

    private static final String ACTION_APN_SETTINGS = Settings.ACTION_APN_SETTINGS;

    private static final int ACTION_APN_SETTINGS_TITLE = R.string.settings_apns_title;

    private static final int ACTION_APN_SETTINGS_ICON = R.drawable.settings_apn;

    // API 19
    private static final String ACTION_CAPTIONING_SETTINGS = Settings.ACTION_CAPTIONING_SETTINGS;

    private static final int ACTION_CAPTIONING_SETTINGS_TITLE =
            R.string.settings_captioning_settings_title;

    private static final int ACTION_CAPTIONING_SETTINGS_ICON = R.drawable.ic_settings_captioning;

    private static final String ACTION_DATA_ROAMING_SETTINGS =
            Settings.ACTION_DATA_ROAMING_SETTINGS;

    private static final int ACTION_DATA_ROAMING_SETTINGS_TITLE =
            R.string.settings_roaming_settings_title;

    private static final int ACTION_DATA_ROAMING_SETTINGS_ICON = R.drawable.ic_settings_roaming;

    // API 18
    private static final String ACTION_DREAM_SETTINGS = Settings.ACTION_DREAM_SETTINGS;

    private static final int ACTION_DREAM_SETTINGS_TITLE = R.string.settings_dream_settings_title;

    private static final int ACTION_DREAM_SETTINGS_ICON = R.drawable.ic_settings_daydream;

    private static final String ACTION_INPUT_METHOD_SETTINGS =
            Settings.ACTION_INPUT_METHOD_SETTINGS;

    private static final int ACTION_INPUT_METHOD_SETTINGS_TITLE =
            R.string.settings_input_methods_settings_title;

    private static final int ACTION_INPUT_METHOD_SETTINGS_ICON =
            R.drawable.ic_settings_input_methods;

    // API 9
    private static final String ACTION_MEMORY_CARD_SETTINGS = Settings.ACTION_MEMORY_CARD_SETTINGS;

    private static final int ACTION_MEMORY_CARD_SETTINGS_TITLE =
            R.string.settings_memory_card_settings_title;

    private static final int ACTION_MEMORY_CARD_SETTINGS_ICON = R.drawable.ic_settings_memory_card;

    // API 14
    private static final String ACTION_NFCSHARING_SETTINGS = Settings.ACTION_NFCSHARING_SETTINGS;

    private static final int ACTION_NFCSHARING_SETTINGS_TITLE =
            R.string.settings_nfc_sharing_settings_title;

    private static final int ACTION_NFCSHARING_SETTINGS_ICON = R.drawable.ic_settings_nfc_sharing;

    // API 19
    private static final String ACTION_NFC_PAYMENT_SETTINGS = Settings.ACTION_NFC_PAYMENT_SETTINGS;

    private static final int ACTION_NFC_PAYMENT_SETTINGS_TITLE =
            R.string.settings_nfc_payment_settings_title;

    private static final int ACTION_NFC_PAYMENT_SETTINGS_ICON = R.drawable.ic_settings_nfc_payment;

    // API 16
    private static final String ACTION_NFC_SETTINGS = Settings.ACTION_NFC_SETTINGS;

    private static final int ACTION_NFC_SETTINGS_TITLE = R.string.settings_nfc_settings_title;

    private static final int ACTION_NFC_SETTINGS_ICON = R.drawable.ic_settings_nfc;

    private static boolean UPDATED_SECTIONS;

    static {
        ICON_LOOKUP.put(WIFI_SETTINGS, WIFI_SETTINGS_ICON);
        ICON_LOOKUP.put(BLUETOOTH_SETTINGS, BLUETOOTH_SETTINGS_ICON);
        ICON_LOOKUP.put(DATA_USAGE_SETTINGS, DATA_USAGE_SETTINGS_ICON);
        ICON_LOOKUP.put(OPERATOR_SETTINGS, OPERATOR_ICON);
        ICON_LOOKUP.put(MORE_SETTINGS, MORE_SETTINGS_ICON);

        ICON_LOOKUP.put(SOUND_SETTINGS, SOUND_SETTINGS_ICON);
        ICON_LOOKUP.put(DISPLAY_SETTINGS, DISPLAY_SETTINGS_ICON);
        ICON_LOOKUP.put(STORAGE_SETTINGS, STORAGE_SETTINGS_ICON);
        ICON_LOOKUP.put(BATTERY_SETTINGS, BATTERY_SETTINGS_ICON);
        ICON_LOOKUP.put(APPLICATION_SETTINGS, APPLICATION_SETTINGS_ICON);
        ICON_LOOKUP.put(MANAGE_USER_SETTINGS, MANAGE_USERS_SETTINGS_ICON);

        ICON_LOOKUP.put(LOCATION_SETTINGS, LOCATION_SETTINGS_ICON);
        ICON_LOOKUP.put(SECURITY_SETTINGS, SECURITY_SETTINGS_ICON);
        ICON_LOOKUP.put(LANGUAGE_SETTINGS, LANGUAGE_SETTINGS_ICON);
        ICON_LOOKUP.put(BACKUP_SETTINGS, BACKUP_SETTINGS_ICON);

        ICON_LOOKUP.put(ACCOUNT_SETTINGS_SETTINGS, ACCOUNT_SETTINGS_ICON);

        ICON_LOOKUP.put(DATE_TIME_SETTINGS, DATE_TIME_SETTINGS_ICON);
        ICON_LOOKUP.put(ACCESSIBILITY_SETTINGS, ACCESSIBILITY_SETTINGS_ICON);
        ICON_LOOKUP.put(PRINT_SETTINGS, PRINT_SETTINGS_ICON);
        ICON_LOOKUP.put(DEVELOPMENT_SETTINGS, DEVELOPMENT_SETTINGS_ICON);
        ICON_LOOKUP.put(ABOUT_SETTINGS, ABOUT_SETTINGS_ICON);

        ICON_LOOKUP.put(AIRPLANE_MODE_SETTINGS, AIRPLANE_MODE_SETTINGS_ICON);
        ICON_LOOKUP.put(ACTION_APN_SETTINGS, ACTION_APN_SETTINGS_ICON);
        ICON_LOOKUP.put(ACTION_CAPTIONING_SETTINGS, ACTION_CAPTIONING_SETTINGS_ICON);
        ICON_LOOKUP.put(ACTION_DATA_ROAMING_SETTINGS, ACTION_DATA_ROAMING_SETTINGS_ICON);
        ICON_LOOKUP.put(ACTION_DREAM_SETTINGS, ACTION_DREAM_SETTINGS_ICON);
        ICON_LOOKUP.put(ACTION_INPUT_METHOD_SETTINGS, ACTION_INPUT_METHOD_SETTINGS_ICON);
        ICON_LOOKUP.put(ACTION_MEMORY_CARD_SETTINGS, ACTION_MEMORY_CARD_SETTINGS_ICON);
        ICON_LOOKUP.put(ACTION_NFCSHARING_SETTINGS, ACTION_NFCSHARING_SETTINGS_ICON);
        ICON_LOOKUP.put(ACTION_NFC_PAYMENT_SETTINGS, ACTION_NFC_PAYMENT_SETTINGS_ICON);
        ICON_LOOKUP.put(ACTION_NFC_SETTINGS, ACTION_NFC_SETTINGS_ICON);

        SECTIONS = new SettingsHelper.Section[]{

                new SettingsHelper.Section(WIRELESS_AND_NETWORKS,
                        new SettingsHelper.SectionItem[]{
                                new SettingsHelper.SectionItem(WIFI_SETTINGS,
                                        WIFI_SETTINGS_TITLE, WIFI_SETTINGS_ICON),
                                new SettingsHelper.SectionItem(BLUETOOTH_SETTINGS,
                                        BLUETOOTH_SETTINGS_TITLE, BLUETOOTH_SETTINGS_ICON),
                                new SettingsHelper.SectionItem(DATA_USAGE_SETTINGS,
                                        DATA_USAGE_SETTINGS_TITLE, DATA_USAGE_SETTINGS_ICON),
                        /* new SectionItem(OPERATOR_SETTINGS, OPERATOR_TITLE, OPERATOR_ICON), */
                                new SettingsHelper.SectionItem(MORE_SETTINGS,
                                        MORE_SETTINGS_TITLE, MORE_SETTINGS_ICON),
                        }),
                new SettingsHelper.Section(DEVICE, new SettingsHelper.SectionItem[]{
                        new SettingsHelper.SectionItem(SOUND_SETTINGS, SOUND_SETTINGS_TITLE,
                                SOUND_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(DISPLAY_SETTINGS, DISPLAY_SETTINGS_TITLE,
                                DISPLAY_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(STORAGE_SETTINGS, STORAGE_SETTINGS_TITLE,
                                STORAGE_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(BATTERY_SETTINGS, BATTERY_SETTINGS_TITLE,
                                BATTERY_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(APPLICATION_SETTINGS,
                                APPLICATION_SETTINGS_TITLE, APPLICATION_SETTINGS_ICON),
                        /*new SectionItem(MANAGE_USER_SETTINGS, MANAGE_USERS_SETTINGS_TITLE,
                        MANAGE_USERS_SETTINGS_ICON),*/
                }),
                new SettingsHelper.Section(PERSONAL, new SettingsHelper.SectionItem[]{
                        new SettingsHelper.SectionItem(LOCATION_SETTINGS,
                                LOCATION_SETTINGS_TITLE, LOCATION_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(SECURITY_SETTINGS,
                                SECURITY_SETTINGS_TITLE, SECURITY_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(LANGUAGE_SETTINGS,
                                LANGUAGE_SETTINGS_TITLE, LANGUAGE_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(BACKUP_SETTINGS, BACKUP_SETTINGS_TITLE,
                                BACKUP_SETTINGS_ICON),
                }), new SettingsHelper.Section(ACCOUNTS, new SettingsHelper.SectionItem[]{
                new SettingsHelper.SectionItem(MANAGE_USER_SETTINGS, ACCOUNT_SETTINGS_TITLE,
                        MANAGE_USERS_SETTINGS_ICON),
        }),
                new SettingsHelper.Section(SYSTEM, new SettingsHelper.SectionItem[]{
                        new SettingsHelper.SectionItem(DATE_TIME_SETTINGS,
                                DATE_TIME_SETTINGS_TITLE, DATE_TIME_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(ACCESSIBILITY_SETTINGS,
                                ACCESSIBILITY_SETTINGS_TITLE, ACCESSIBILITY_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(PRINT_SETTINGS, PRINT_SETTINGS_TITLE,
                                PRINT_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(DEVELOPMENT_SETTINGS,
                                DEVELOPMENT_SETTINGS_TITLE, DEVELOPMENT_SETTINGS_ICON),
                        new SettingsHelper.SectionItem(ABOUT_SETTINGS, ABOUT_SETTINGS_TITLE,
                                ABOUT_SETTINGS_ICON),
                }),
        };
    }

    public static SettingsHelper.Builder createSettingsConstants(
            ConnectivityManager connectivityManager,
            TelephonyManager telephonyManager,
            AudioManager audioManager,
            WifiManager wifiManager) {
        return new SettingsHelper.Builder(SECTIONS,
                R.drawable.ic_airplane_mode,
                R.drawable.ic_settings_bluetooth2,
                R.drawable.ic_settings_data,
                R.drawable.ic_settings_wireless,
                R.drawable.ic_settings_sound,
                connectivityManager,
                telephonyManager,
                audioManager,
                wifiManager
        );
    }

}
