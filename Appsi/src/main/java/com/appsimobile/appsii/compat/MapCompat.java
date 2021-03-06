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

package com.appsimobile.appsii.compat;

import android.os.Build;
import android.util.ArrayMap;

import java.util.Map;

/**
 * Created by Nick Martens on 15/12/13.
 */
public class MapCompat {

    public static <K, V> Map<K, V> createMap(int capacity) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new ArrayMap<>(capacity);
        }
        return new android.support.v4.util.ArrayMap<>(capacity);
    }

    public static <K, V> Map<K, V> createMap() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new ArrayMap<>();
        }
        return new android.support.v4.util.ArrayMap<>();
    }

}
