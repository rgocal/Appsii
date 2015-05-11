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

package com.appsimobile.appsii.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by nick on 25/05/14.
 */
public class AppsiPreferences {

    public static void initializeDefaults(SharedPreferences preferences, boolean restoreDefaults) {
        ModulePreferences.initializeDefaults(preferences, restoreDefaults);
    }

    public static SharedPreferences.Editor editor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit();
    }

    public static class ModulePreferences {

        public static final String MODULE_ID_APPS = "apps";

        public static final String DEFAULT_MOD_0 = MODULE_ID_APPS;

        public static final String MODULE_ID_CALLS = "calls";

        public static final String DEFAULT_MOD_1 = MODULE_ID_CALLS;

        public static final String MODULE_ID_CONTACTS = "contacts";

        public static final String DEFAULT_MOD_2 = MODULE_ID_CONTACTS;

        public static final String MODULE_ID_SETTINGS = "settings";

        public static final String DEFAULT_MOD_3 = MODULE_ID_SETTINGS;

        public static final String MODULE_ID_SMS = "sms";

        public static final String DEFAULT_MOD_4 = MODULE_ID_SMS;

        public static final int VIEW_TYPE_LIST = 0;

        public static final int VIEW_TYPE_TILES = 1;

        public static final String KEY_DEFAULTS_VERSION = "mod_defaults_version";

        public static final String KEY_MOD_0 = "mod_0";

        public static final String KEY_MOD_1 = "mod_1";

        public static final String KEY_MOD_2 = "mod_2";

        public static final String KEY_MOD_3 = "mod_3";

        public static final String KEY_MOD_4 = "mod_4";

        public static final int VERSION = 1;

        static void initializeDefaults(SharedPreferences preferences, boolean restoreDefaults) {
            Apps.initializeDefaults(preferences, restoreDefaults);
            int version = preferences.getInt(KEY_DEFAULTS_VERSION, 0);
            if (version < VERSION) {
                preferences.edit().
                        putString(KEY_MOD_0, DEFAULT_MOD_0).
                        putString(KEY_MOD_1, DEFAULT_MOD_1).
                        putString(KEY_MOD_2, DEFAULT_MOD_2).
                        putString(KEY_MOD_3, DEFAULT_MOD_3).
                        putString(KEY_MOD_4, DEFAULT_MOD_4).
                        putInt(KEY_DEFAULTS_VERSION, 1).
                        apply();
            }
        }

        static SharedPreferences.Editor setModuleOrder(SharedPreferences.Editor editor, String mod1,
                String mod2, String mod3, String mod4,
                String mod5) {
            return editor.
                    putString(KEY_MOD_0, mod1).
                    putString(KEY_MOD_1, mod2).
                    putString(KEY_MOD_2, mod3).
                    putString(KEY_MOD_3, mod4).
                    putString(KEY_MOD_4, mod5);
        }


        public static class Apps {

            public static final String KEY_DISPLAY_MODE = "apps_display_mode";

            public static final String KEY_DEFAULTS_SET = "apps_defaults_initialized";

            public static final int DEFAULT_DISPLAY_MODE = VIEW_TYPE_TILES;

            static void initializeDefaults(SharedPreferences preferences, boolean restoreDefaults) {
                boolean defaultsInitialized = preferences.getBoolean(KEY_DEFAULTS_SET, false);
                if (!restoreDefaults && defaultsInitialized) return;

                preferences.edit().
                        putInt(KEY_DISPLAY_MODE, DEFAULT_DISPLAY_MODE).
                        putBoolean(KEY_DEFAULTS_SET, true).
                        apply();
            }

            public static int getDisplayMode(SharedPreferences preferences) {
                return preferences.getInt(KEY_DISPLAY_MODE, DEFAULT_DISPLAY_MODE);
            }

            public static SharedPreferences.Editor setDisplayMode(SharedPreferences.Editor editor,
                    int displayMode) {
                return editor.putInt(KEY_DISPLAY_MODE, displayMode);
            }


        }
    }

}
