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

package com.appsimobile.appsii.module.home.config;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;

/**
 * Created by nick on 24/03/15.
 */
public class MockHomeItemConfiguration extends AbstractHomeItemConfiguration {

    final SimpleArrayMap<String, String> mProperties;

    final Handler mHandler;

    public MockHomeItemConfiguration(Context context) {
        super(context);
        mProperties = new SimpleArrayMap<>();
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void initProperty(String key, String value) {
        mProperties.put(key, value);
    }

    @Override
    public void updateProperty(final long cellId, final String key, final String value) {
        mProperties.put(key, value);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyPropertyChanged(cellId, key, value);
            }
        });
    }

    @Override
    public String getProperty(long cellId, String key, String fallback) {
        if (!mProperties.containsKey(key)) return fallback;
        return mProperties.get(key);
    }

    @Override
    public void removeProperty(final long cellId, final String key) {
        mProperties.remove(key);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyPropertyDeleted(cellId, key);
            }
        });
    }

    @Override
    public void removeAllProperties(long cellId) {
        int N = mProperties.size();
        for (int i = 0; i < N; i++) {
            String key = mProperties.keyAt(i);
            removeProperty(cellId, key);
        }
    }

    @Override
    public String[] getWeatherWidgetWoeids(String key) {
        return new String[0];
    }

    @Override
    public long findCellWithPropertyValue(String propertyName, String value) {
        if (mProperties.containsKey(propertyName)) {
            if (TextUtils.equals(value, mProperties.get(propertyName))) return 1L;
        }
        return -1L;
    }
}
