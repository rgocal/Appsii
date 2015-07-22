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

package com.appsimobile.appsii.module.apps;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Handles automatic loading and reloading of the app-tags.
 * Simply registering a listener will trigger the initial load.
 * <p/>
 * The listener will be called whenever the the data is changed again
 * Created by nick on 23/08/14.
 */
public class AppTagUtils {

    /**
     * The tags still need to be loaded
     */
    static final int STATUS_NEEDS_LOAD = 0;

    private static final int STATUS_LOADING = 1;

    private static final int STATUS_READY = 2;

    private static AppTagUtils sInstance;

    final Context mContext;

    private final Handler mHandler;

    int mStatus = STATUS_NEEDS_LOAD;

    final ContentObserver mContentObserver;

    private final ArrayList<AppTag> mAppTags = new ArrayList<>(6);

    private final WeakHashMap<AppTagListener, Void> mListeners = new WeakHashMap<>();

    private AppTagUtils(Context context) {
        mContext = context;
        mHandler = new Handler();
        mContentObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                mStatus = STATUS_NEEDS_LOAD;
                ensureLoaded();
            }
        };
        mContext.getContentResolver().registerContentObserver(
                AppsContract.TagColumns.CONTENT_URI, true, mContentObserver);
    }

    static List<AppTag> loadTagsBlocking(Context context) {
        Uri uri = AppsContract.TagColumns.CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor =
                contentResolver.query(uri, AppTagQuery.PROJECTION, null, null, AppTagQuery.ORDER);

        int count = cursor.getCount();
        List<AppTag> result = new ArrayList<>(count);

        while (cursor.moveToNext()) {
            long id = cursor.getLong(AppTagQuery._ID);
            boolean defaultExpanded = cursor.getInt(AppTagQuery.DEFAULT_EXPANDED) == 1;
            String name = cursor.getString(AppTagQuery.NAME);
            int position = cursor.getInt(AppTagQuery.POSITION);
            int columnCount = cursor.getInt(AppTagQuery.COLUMN_COUNT);
            int tagType = cursor.getInt(AppTagQuery.TAG_TYPE);
            boolean visible = cursor.getInt(AppTagQuery.VISIBLE) == 1;
            AppTag tag = new AppTag(id, name, position, defaultExpanded,
                    visible, columnCount, tagType);
            result.add(tag);
        }

        cursor.close();

        return result;
    }

    public static AppTagUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AppTagUtils(context.getApplicationContext());
        }
        return sInstance;
    }

    public static long insertTagBlocking(Context context, String name, int position,
            boolean defaultExpanded, boolean visible) {

        Uri uri = AppsContract.TagColumns.CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(AppsContract.TagColumns.NAME, name);
        contentValues.put(AppsContract.TagColumns.POSITION, position);
        contentValues.put(AppsContract.TagColumns.DEFAULT_EXPANDED, defaultExpanded ? 1 : 0);
        contentValues.put(AppsContract.TagColumns.VISIBLE, visible ? 1 : 0);
        Uri result = contentResolver.insert(uri, contentValues);
        return ContentUris.parseId(result);

    }

    void ensureLoaded() {
        // when we are already loading, ignore the request
        if (mStatus > STATUS_NEEDS_LOAD) return;

        mStatus = STATUS_LOADING;

        AsyncTask<Void, Void, List<AppTag>> task = new AsyncTask<Void, Void, List<AppTag>>() {
            @Override
            protected List<AppTag> doInBackground(Void... params) {
                return loadTagsBlocking(mContext);
            }

            @Override
            protected void onPostExecute(List<AppTag> appTags) {
                onAppTagsLoaded(appTags);
            }
        };
        task.execute();
    }

    void onAppTagsLoaded(List<AppTag> appTags) {
        mStatus = STATUS_READY;
        mAppTags.clear();
        mAppTags.addAll(appTags);
        notifyAppTagsLoaded();
    }

    private void notifyAppTagsLoaded() {
        for (AppTagListener l : mListeners.keySet()) {
            l.onTagsChanged(mAppTags);
        }
    }

    public void unregisterAppTagListener(AppTagListener listener) {
        mListeners.remove(listener);
    }

    public List<AppTag> registerAppTagListener(AppTagListener listener) {
        mListeners.put(listener, null);
        ensureLoaded();
        return mAppTags;
    }

    public interface AppTagListener {

        void onTagsChanged(ArrayList<AppTag> appTags);
    }

}
