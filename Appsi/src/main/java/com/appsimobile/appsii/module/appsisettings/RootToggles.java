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

import java.io.File;

/**
 * Created by Nick Martens on 11/27/13.
 */
public final class RootToggles {

    private static boolean isRooted() {
        return findBinary("su");
    }


    public static boolean findBinary(String binaryName) {
        String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
        for (String where : places) {
            if (new File(where + binaryName).exists()) {
                return true;
            }
        }
        return false;
    }

    /*

    Airplane mode on (4.2 +)
        su
        settings put global airplane_mode_on 1
        am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true

    Airplane mode off (4.2 +)
        su
        settings put global airplane_mode_on 0
        am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false

    Bluetooth toggle (4.2 +)
        su
        settings put global bluetooth_on 0
        settings put global bluetooth_on 1
        ??am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false

    Wifi toggle (4.2 +)
        su
        settings put global wifi_on 0
        settings put global wifi_on_on 1
        ??am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false

    Brightness toggle (4.2 +) // The screen backlight brightness between 0 and 255.
        su
        settings put global screen_brightness 0
        settings put global screen_brightness 255
        ??am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false

    Brightness auto toggle (4.2 +) // The screen backlight brightness between 0 and 255.
        su
        settings put global screen_brightness_mode 1 // automatic
        settings put global screen_brightness_mode 0 // manual
        ??am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false

    if  (android.provider.Settings.System.getInt(getContentResolver(),
    Settings.System.ACCELEROMETER_ROTATION, 0) == 1){
        android.provider.Settings.System.putInt(getContentResolver(),
        Settings.System.ACCELEROMETER_ROTATION, 0);
        Toast.makeText(Rotation.this, "Rotation OFF", Toast.LENGTH_SHORT).show();
    } else{
        android.provider.Settings.System.putInt(getContentResolver(),
        Settings.System.ACCELEROMETER_ROTATION, 1);
        Toast.makeText(Rotation.this, "Rotation ON", Toast.LENGTH_SHORT).show();
    }

    */
}
