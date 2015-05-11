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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import com.appsimobile.appsii.annotation.VisibleForTesting;
import com.appsimobile.appsii.module.home.provider.HomeContract;

import net.jcip.annotations.GuardedBy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A helper facility to ease the process of loading and saving configurations for multiple
 * small views.
 * Created by nick on 21/01/15.
 */
public final class HomeItemConfigurationHelper extends AbstractHomeItemConfiguration {

    private static HomeItemConfigurationFactory sFactory = new HomeItemConfigurationFactoryImpl();

    private static HomeItemConfiguration sInstance;

    // Confined to main thread
    QueryHandlerImpl mQueryHandler;

    @GuardedBy("this")
    private LongSparseArray<ConfigurationProperty> mConfigurationProperties;

    protected HomeItemConfigurationHelper(Context context) {
        super(context);
        mConfigurationProperties = loadConfigurations(context);
    }

    @VisibleForTesting
    public static void setFactory(HomeItemConfigurationFactory factory) {
        HomeItemConfigurationHelper.sFactory = factory;
    }

    public static synchronized HomeItemConfiguration getInstance(Context context) {
        if (sInstance == null) {
            sInstance = sFactory.createInstance(context);
        }
        return sInstance;
    }

    @VisibleForTesting
    final LongSparseArray<ConfigurationProperty> loadConfigurations(Context context) {
        ContentResolver resolver = context.getContentResolver();
        String[] projection = new String[]{
                HomeContract.Configuration._CELL_ID,
                HomeContract.Configuration.KEY,
                HomeContract.Configuration.VALUE,
        };

        Cursor cursor = resolver.query(HomeContract.Configuration.CONTENT_URI,
                projection,
                null,
                null,
                null);

        if (cursor == null) return null;

        LongSparseArray<ConfigurationProperty> result = new LongSparseArray<>();
        try {
            while (cursor.moveToNext()) {

                long cellId = cursor.getLong(0);

                ConfigurationProperty info = result.get(cellId);
                if (info == null) {
                    info = createProperty(result, cellId);
                }
                String key = cursor.getString(1);
                String value = cursor.getString(2);
                info.put(key, value);
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    void ensureQueryHandler() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Method can only be called on the main thread");
        }
        mQueryHandler = new QueryHandlerImpl(mContext.getContentResolver());
    }

    @Override
    public void updateProperty(long cellId, String key, String value) {
        ensureQueryHandler();
        mQueryHandler.updateProperty(cellId, key, value);
    }

    @Override
    public synchronized String getProperty(long cellId, String key, String nullValue) {
        ConfigurationProperty property = mConfigurationProperties.get(cellId);
        if (property == null) return nullValue;

        return property.getProperty(key, nullValue);
    }

    @Override
    public void removeProperty(long cellId, String key) {
        ensureQueryHandler();
        mQueryHandler.deleteProperty(cellId, key);
    }

    @Override
    public void removeAllProperties(long cellId) {
        ensureQueryHandler();
        synchronized (this) {
            ConfigurationProperty props = mConfigurationProperties.get(cellId);
            if (props != null) {
                int N = props.size();
                for (int i = 0; i < N; i++) {
                    String key = props.keyAt(i);
                    mQueryHandler.deleteProperty(cellId, key);
                }
            }
        }
    }


    @Override
    public String[] getWeatherWidgetWoeids(String key) {
        Set<String> result = new HashSet<>();
        synchronized (this) {
            int len = mConfigurationProperties.size();
            for (int i = 0; i < len; i++) {
                ConfigurationProperty property = mConfigurationProperties.valueAt(i);
                String value = property.getProperty(key, null);
                if (value != null) {
                    result.add(value);
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public long findCellWithPropertyValue(String propertyName, String value) {
        synchronized (this) {
            int len = mConfigurationProperties.size();
            for (int i = 0; i < len; i++) {
                ConfigurationProperty property = mConfigurationProperties.valueAt(i);
                String propValue = property.getProperty(propertyName, null);
                if (propValue != null && TextUtils.equals(value, propValue)) {
                    return property.mCellId;
                }
            }
        }

        return -1;
    }


    void onPropertyUpdated(long cellId, String key, String value) {
        synchronized (this) {
            ConfigurationProperty property = mConfigurationProperties.get(cellId);
            if (property == null) {
                property = createProperty(mConfigurationProperties, cellId);
            }
            property.put(key, value);
        }
        notifyPropertyChanged(cellId, key, value);
    }

    void onPropertyDeleted(long cellId, String key) {
        synchronized (this) {
            ConfigurationProperty property = mConfigurationProperties.get(cellId);
            if (property != null) {
                property.remove(key);
            }
        }
        notifyPropertyDeleted(cellId, key);
    }

    static final class HomeItemConfigurationFactoryImpl implements HomeItemConfigurationFactory {

        @Override
        public HomeItemConfiguration createInstance(Context context) {
            return new HomeItemConfigurationHelper(context);
        }
    }

    class QueryHandlerImpl extends AsyncQueryHandler {

        final String mSelection = HomeContract.Configuration._CELL_ID + "=? AND " +
                HomeContract.Configuration.KEY + "=?";

        /**
         * This is a cookie pool, this prevents that we create lots of objects that need
         * to be garbage collected. Once an object is acquired from the pool, it is used
         * as a cookie in a query.
         * When done with this cookie it must be released, which will add it to the pool
         * again.
         * SimpleCookie instances must not be leaked outside this class.
         */
        List<SimpleCookie> mCookiePool = new LinkedList<>();

        public QueryHandlerImpl(ContentResolver cr) {
            super(cr);
        }

        void deleteProperty(long cellId, String key) {
            SimpleCookie cookie = acquireCookie(cellId, key);
            startDelete(0, cookie, HomeContract.Configuration.CONTENT_URI, mSelection, new String[]{
                    String.valueOf(cellId),
                    key
            });
        }

        SimpleCookie acquireCookie(long cellId, String key) {
            if (mCookiePool.isEmpty()) return new SimpleCookie(cellId, key);
            return mCookiePool.remove(0).set(cellId, key);
        }

        void updateProperty(long cellId, String key, String value) {
            SimpleCookie cookie = acquireCookie(cellId, key);
            cookie.mValue = value;

            ContentValues values = new ContentValues(3);
            values.put(HomeContract.Configuration._CELL_ID, cellId);
            values.put(HomeContract.Configuration.KEY, key);
            values.put(HomeContract.Configuration.VALUE, value);
            startInsert(0, cookie, HomeContract.Configuration.CONTENT_URI, values);
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            SimpleCookie simpleCookie = (SimpleCookie) cookie;
            onPropertyUpdated(simpleCookie.mId, simpleCookie.mKey, simpleCookie.mValue);
            simpleCookie.release();
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            SimpleCookie simpleCookie = (SimpleCookie) cookie;
            onPropertyDeleted(simpleCookie.mId, simpleCookie.mKey);
            simpleCookie.release();
        }

        class SimpleCookie {

            long mId;

            String mKey;

            String mValue;

            public SimpleCookie(long cellId, String key) {
                mId = cellId;
                mKey = key;
            }

            void release() {
                mValue = null;
                mCookiePool.add(this);
            }

            public SimpleCookie set(long cellId, String key) {
                mId = cellId;
                mKey = key;
                return this;
            }
        }
    }

}
