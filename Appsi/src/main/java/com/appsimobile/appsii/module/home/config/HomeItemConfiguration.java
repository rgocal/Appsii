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

package com.appsimobile.appsii.module.home.config;

import android.support.v4.util.SimpleArrayMap;

/**
 * A helper facility to ease the process of loading and saving configurations for multiple
 * small views.
 * Created by nick on 21/01/15.
 */
public interface HomeItemConfiguration {

    void addConfigurationListener(ConfigurationListener listener);

    void removeConfigurationListener(ConfigurationListener listener);

    void updateProperty(long cellId, String key, String value);

    String getProperty(long cellId, String key, String nullValue);

    void removeProperty(long cellId, String key);

    void removeAllProperties(long cellId);

    String[] getWeatherWidgetWoeids(String key);

    long findCellWithPropertyValue(String propertyName, String value);

    interface ConfigurationListener {

        void onConfigurationOptionUpdated(long cellId, String key, String value);

        void onConfigurationOptionDeleted(long cellId, String key);
    }

    class ConfigurationProperty {

        private final SimpleArrayMap<String, String> mValues = new SimpleArrayMap<>();

        long mCellId;

        public ConfigurationProperty put(String key, String value) {
            mValues.put(key, value);
            return this;
        }

        void remove(String key) {
            mValues.remove(key);
        }

        int size() {
            return mValues.size();
        }

        String keyAt(int index) {
            return mValues.keyAt(index);
        }

        String getProperty(String key, String nullValue) {
            if (mValues.containsKey(key)) {
                return mValues.get(key);
            }
            return nullValue;
        }
    }

}
