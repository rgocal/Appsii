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

package com.appsimobile.appsii.module.search;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class RecentSearchLoader extends AsyncTaskLoader<List<SearchSuggestion>> {

    final Context mContext;

    final Handler mHandler;

    List<SearchSuggestion> mApps;

    private ContentObserver mHistoryObserver;

    public RecentSearchLoader(Context context) {
        super(context);

        mContext = context.getApplicationContext();
        mHandler = new Handler();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<SearchSuggestion> apps) {
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(apps);
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override
    public List<SearchSuggestion> loadInBackground() {

        String[] projection = new String[]{
                SearchSuggestionContract.SearchSuggestionColumns._ID,
                SearchSuggestionContract.SearchSuggestionColumns.QUERY,
                SearchSuggestionContract.SearchSuggestionColumns.LAST_USED,
        };

        Cursor cursor = mContext.getContentResolver().query(
                SearchSuggestionContract.SearchSuggestionColumns.CONTENT_URI,
                projection,
                null,
                null,
                SearchSuggestionContract.SearchSuggestionColumns.LAST_USED + " DESC LIMIT 8");

        List<SearchSuggestion> result = new ArrayList<>(cursor.getCount());
        try {
            while (cursor.moveToNext()) {
                SearchSuggestion suggestion = new SearchSuggestion();
                suggestion.id = cursor.getLong(0);
                suggestion.query = cursor.getString(1);
                suggestion.lastUsed = cursor.getLong(2);
                result.add(suggestion);
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<SearchSuggestion> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(List<SearchSuggestion> apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        List<SearchSuggestion> oldApps = mApps;
        mApps = apps;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(apps);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mApps != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mApps);
        }

        if (takeContentChanged() || mApps == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }


        mHistoryObserver = new ForceLoadContentObserver();
        mContext.getContentResolver().registerContentObserver(
                SearchSuggestionContract.SearchSuggestionColumns.CONTENT_URI,
                true,
                mHistoryObserver);

    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }


    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (mApps != null) {
            onReleaseResources(mApps);
            mApps = null;
        }

        // Stop monitoring for changes.
        if (mHistoryObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mHistoryObserver);
            mHistoryObserver = null;
        }
    }


}