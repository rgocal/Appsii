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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * A receiver that handles the boot complete and user present events.
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    public static void autoStartAppsi(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean autostart = prefs.getBoolean("pref_autostart", true);
        if (autostart) {
            Intent startServiceIntent = new Intent(context, Appsi.class);
            context.startService(startServiceIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        autoStartAppsi(context);
    }

}
