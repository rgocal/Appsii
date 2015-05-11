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

import android.support.v4.util.ArrayMap;

/**
 * Created by nick on 14/01/15.
 */
class SimplePreferenceState {

    final ArrayMap<String, Object> mPreferences;

    SimplePreferenceState() {
        mPreferences = new ArrayMap<>();
    }

    SimplePreferenceState(SimplePreferenceState state) {
        mPreferences = new ArrayMap<>(state.mPreferences);
    }

    public void put(String key, Object value) {
        mPreferences.put(key, value);
    }

    public String getString(String key, String defValue) {
        if (mPreferences.containsKey(key)) {
            return (String) mPreferences.get(key);
        }
        return defValue;
    }

    public int getInt(String key, int defValue) {
        if (mPreferences.containsKey(key)) {
            return (Integer) mPreferences.get(key);
        }
        return defValue;
    }

    public long getLong(String key, long defValue) {
        if (mPreferences.containsKey(key)) {
            return (Long) mPreferences.get(key);
        }
        return defValue;
    }

    public float getFloat(String key, float defValue) {
        if (mPreferences.containsKey(key)) {
            return (Float) mPreferences.get(key);
        }
        return defValue;
    }

    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences.containsKey(key)) {
            return (Boolean) mPreferences.get(key);
        }
        return defValue;
    }

    public boolean contains(String key) {
        return mPreferences.containsKey(key);
    }
}
