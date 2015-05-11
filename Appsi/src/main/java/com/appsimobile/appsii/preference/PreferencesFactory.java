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
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.annotation.VisibleForTesting;
import com.google.android.vending.licensing.AESObfuscator;

/**
 * Created by nick on 09/01/15.
 */
public class PreferencesFactory {

    private static final byte[] SALT =
            "http://developer.android.com/google/play/billing/billing_reference.html".getBytes();

    private static volatile SharedPreferences sPreferences;

    private static volatile ObfuscatedPreferences sObfuscatedPreferences;

    @VisibleForTesting
    public static void setPreferences(SharedPreferences preferences) {
        sPreferences = preferences;
    }

    public static SharedPreferences getPreferences(Context context) {
        if (sPreferences == null) {
            sPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return sPreferences;
    }

    public static ObfuscatedPreferences getObfuscatedPreferences(Context context) {
        if (sObfuscatedPreferences == null) {
            sObfuscatedPreferences = obfuscator(getPreferences(context));

        }
        return sObfuscatedPreferences;
    }

    private static ObfuscatedPreferences obfuscator(SharedPreferences preferences) {
        AESObfuscator obfuscator = new AESObfuscator(SALT,
                BuildConfig.APPLICATION_ID, Settings.Secure.ANDROID_ID);

        return new ObfuscatedPreferences(preferences, obfuscator);
    }

}
