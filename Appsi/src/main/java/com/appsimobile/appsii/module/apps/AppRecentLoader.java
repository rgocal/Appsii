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

import android.content.AsyncTaskLoader;
import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class AppRecentLoader extends AsyncTaskLoader<List<HistoryItem>> {

    final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();

    final Context mContext;

    final Handler mHandler;

    private final Map<ComponentName, CharSequence> mLabelCache = AppListLoader.sLabelCache;

    List<HistoryItem> mApps;

    PackageIntentReceiver mPackageObserver;

    private ContentObserver mHistoryObserver;

    public AppRecentLoader(Context context) {
        super(context);

        mContext = context.getApplicationContext();
        mHandler = new Handler();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<HistoryItem> apps) {
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
    public List<HistoryItem> loadInBackground() {

        Cursor cursor =
                mContext.getContentResolver().query(AppsContract.LaunchHistoryColumns.CONTENT_URI,
                        AppHistoryQuery.PROJECTION,
                        null,
                        null,
                        AppsContract.LaunchHistoryColumns.LAST_LAUNCHED);

        List<HistoryItem> result = new ArrayList<>();

        while (cursor.moveToNext()) {
            int count = cursor.getInt(AppHistoryQuery.LAUNCH_COUNT);
            String flattenedComponentName = cursor.getString(AppHistoryQuery.COMPONENT_NAME);
            long lastLaunched = cursor.getLong(AppHistoryQuery.LAST_LAUNCHED);

            ComponentName cn = ComponentName.unflattenFromString(flattenedComponentName);
            HistoryItem item = new HistoryItem();
            item.componentName = cn;
            item.launchCount = count;
            item.lastLaunched = lastLaunched;

            result.add(item);
        }


        return result;
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<HistoryItem> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(List<HistoryItem> apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        List<HistoryItem> oldApps = mApps;
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

        // Start watching for changes in the app data.
        if (mPackageObserver == null) {
            mPackageObserver = new PackageIntentReceiver(this);
        }

        // Has something interesting in the configuration changed since we
        // last built the app list?
        boolean configChange = mLastConfig.applyNewConfig(getContext().getResources());

        if (takeContentChanged() || mApps == null || configChange) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }


        mHistoryObserver = new ForceLoadContentObserver();
        mContext.getContentResolver()
                .registerContentObserver(AppsContract.LaunchHistoryColumns.CONTENT_URI, true,
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
        if (mPackageObserver != null) {
            getContext().unregisterReceiver(mPackageObserver);
            mPackageObserver = null;
        }
        if (mHistoryObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mHistoryObserver);
            mHistoryObserver = null;
        }
    }


}