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

import android.content.Context;
import android.support.v4.util.LongSparseArray;

import com.appsimobile.appsii.annotation.VisibleForTesting;

import java.util.LinkedList;
import java.util.List;

/**
 * A helper facility to ease the process of loading and saving configurations for multiple
 * small views.
 * Created by nick on 21/01/15.
 */
@VisibleForTesting
public abstract class AbstractHomeItemConfiguration implements HomeItemConfiguration {

    final Context mContext;

    private final List<ConfigurationListener> mConfigurationListeners = new LinkedList<>();

    protected AbstractHomeItemConfiguration(Context context) {
        mContext = context.getApplicationContext();
    }

    @VisibleForTesting
    static ConfigurationProperty createProperty(
            LongSparseArray<ConfigurationProperty> result, long cellId) {
        ConfigurationProperty property;
        property = new ConfigurationProperty();
        property.mCellId = cellId;
        result.put(cellId, property);
        return property;
    }

    @Override
    public void addConfigurationListener(ConfigurationListener listener) {
        mConfigurationListeners.add(listener);
    }

    @Override
    public void removeConfigurationListener(ConfigurationListener listener) {
        mConfigurationListeners.remove(listener);
    }

    protected void notifyPropertyChanged(long cellId, String key, String value) {
        for (ConfigurationListener l : mConfigurationListeners) {
            l.onConfigurationOptionUpdated(cellId, key, value);
        }
    }

    protected void notifyPropertyDeleted(long cellId, String key) {
        for (ConfigurationListener l : mConfigurationListeners) {
            l.onConfigurationOptionDeleted(cellId, key);
        }
    }

}
