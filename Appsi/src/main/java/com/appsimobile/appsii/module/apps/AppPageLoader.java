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
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.support.v4.util.SimpleArrayMap;
import android.util.LongSparseArray;

import com.appsimobile.appsii.compat.LauncherActivityInfoCompat;
import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.compat.UserHandleCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class AppPageLoader extends AsyncTaskLoader<AppPageData> {

    static final Map<ComponentName, CharSequence> sLabelCache = new ConcurrentHashMap<>();

    static final SimpleArrayMap<ComponentName, ResolveInfoAppEntry> sEntryCache
            = new SimpleArrayMap<>();

    final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();

    final Context mContext;

    private final ShortcutNameComparator mShortcutNameComparator;

    private final Map<ComponentName, CharSequence> mLabelCache;

    final PackageManager mPackageManager;

    AppPageData mApps;

    PackageIntentReceiver mPackageObserver;

    ForceLoadContentObserver mForceLoadContentObserver;

    public AppPageLoader(Context context) {
        super(context);

        mContext = context;

        mLabelCache = sLabelCache;

        // Retrieve the package manager for later use; note we don't
        // use 'context' directly but instead the save global application
        // context returned by getContext().
        mPackageManager = getContext().getPackageManager();

        mShortcutNameComparator = new ShortcutNameComparator(context, mLabelCache);
    }

    /**
     * Helper method to add items to a list in a long sparse array that maps keys to lists.
     */
    static <V> void addItemToLongSparseArray(LongSparseArray<List<V>> map, long key, V item) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(item);
    }

    /**
     * Helper method to add items to a list in a map that maps keys to lists.
     */
    static <K, V> void addItemToMapList(SimpleArrayMap<K, List<V>> map, K key, V item) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(item);
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(AppPageData apps) {
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
    public AppPageData loadInBackground() {

        long start = System.currentTimeMillis();
        List<ResolveInfoAppEntry> allApps = loadAllApps();

        List<HistoryItem> recentApps = loadRecentApps();
        List<AppTag> tags = AppTagUtils.loadTagsBlocking(mContext);

        AppPageData result = new AppPageData(allApps, recentApps, tags);

        loadTaggedApps(allApps, result);


        sEntryCache.clear();
        if (allApps != null) {

            int N = allApps.size();
            for (int i = 0; i < N; i++) {
                ResolveInfoAppEntry app = allApps.get(i);
                sEntryCache.put(app.getComponentName(), app);
            }
        }

        return result;
    }

    private List<ResolveInfoAppEntry> loadAllApps() {
        List<ResolveInfoAppEntry> result = new ArrayList<>();

        LauncherAppsCompat lap = LauncherAppsCompat.getInstance(mContext);
        List<LauncherActivityInfoCompat> apps =
                lap.getActivityList(null, UserHandleCompat.myUserHandle());

        // Fail if we don't have any apps
        if (apps == null || apps.isEmpty()) {
            return null;
        }
        // Sort the applications by name
        Collections.sort(apps, mShortcutNameComparator);

        // Create the AppEntries
        int N = apps.size();
        for (int i = 0; i < N; i++) {
            LauncherActivityInfoCompat app = apps.get(i);

            ResolveInfoAppEntry entry = sEntryCache.get(app.getComponentName());

            if (entry == null) {
                CharSequence label;
                ComponentName componentName = app.getComponentName();

                if (mLabelCache.containsKey(componentName)) {
                    label = mLabelCache.get(componentName);
                } else {
                    label = app.getLabel();
                    mLabelCache.put(componentName, label);
                }

                entry = new ResolveInfoAppEntry(app, label);
            } else {
                // if it already exists, update it with the entry and label.
                // this makes sure the icon is displayed correctly after update
                entry.update(app, app.getLabel());
            }

            result.add(entry);
        }

        return result;
    }

    private List<HistoryItem> loadRecentApps() {

        Cursor cursor =
                mContext.getContentResolver().query(AppsContract.LaunchHistoryColumns.CONTENT_URI,
                        AppHistoryQuery.PROJECTION,
                        null,
                        null,
                        AppsContract.LaunchHistoryColumns.LAUNCH_COUNT + " DESC, " +
                                AppsContract.LaunchHistoryColumns.LAST_LAUNCHED + " DESC "
                );

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
            if (result.size() >= 9) break;
        }
        cursor.close();


        return result;
    }

    private void loadTaggedApps(List<? extends AppEntry> allApps, AppPageData result) {
        if (allApps == null || allApps.isEmpty()) return;

        Cursor cursor =
                mContext.getContentResolver().query(AppsContract.TaggedAppColumns.CONTENT_URI,
                        AppQuery.PROJECTION, AppQuery.WHERE_NOT_DELETED, null, AppQuery.ORDER);


        int appsSize = allApps.size();
        SimpleArrayMap<ComponentName, AppEntry> entriesByComponent = new SimpleArrayMap<>(appsSize);
        for (int i = 0; i < appsSize; i++) {
            AppEntry app = allApps.get(i);
            entriesByComponent.put(app.getComponentName(), app);
        }

        while (cursor.moveToNext()) {
            String shortComponentName = cursor.getString(AppQuery.COMPONENT_NAME);
            ComponentName componentName = ComponentName.unflattenFromString(shortComponentName);


            // find the app entry from all apps. If it does not exists, the component
            // was changed or uninstalled. In that case, ignore it.
            AppEntry appEntry = entriesByComponent.get(componentName);
            if (appEntry == null) continue;

            // now create the tagged-app object. This holds the details of the
            // tagged instance
            TaggedApp taggedApp = new TaggedApp();
            long tagId = cursor.getLong(AppQuery.TAG_ID);
            String tagName = cursor.getString(AppQuery.TAG_NAME);

            taggedApp.mComponentName = componentName;
            taggedApp.mId = cursor.getLong(AppQuery._ID);
            taggedApp.mTagName = tagName;
            taggedApp.mTagId = tagId;
            taggedApp.mAppEntry = appEntry;

            addItemToLongSparseArray(result.mAppsPerTag, tagId, appEntry);
            addItemToMapList(result.mTagsPerComponent, componentName, taggedApp);

        }

        cursor.close();
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(AppPageData apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(AppPageData apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        AppPageData oldApps = mApps;
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
        if (mForceLoadContentObserver == null) {
            mForceLoadContentObserver = new ForceLoadContentObserver();
            ContentResolver contentResolver = mContext.getContentResolver();

            contentResolver.registerContentObserver(AppsContract.TaggedAppColumns.CONTENT_URI,
                    true, mForceLoadContentObserver);

            contentResolver.registerContentObserver(AppsContract.TagColumns.CONTENT_URI,
                    true, mForceLoadContentObserver);

            contentResolver.registerContentObserver(AppsContract.LaunchHistoryColumns.CONTENT_URI,
                    true, mForceLoadContentObserver);
        }

        // Has something interesting in the configuration changed since we
        // last built the app list?
        boolean configChange = mLastConfig.applyNewConfig(getContext().getResources());

        if (takeContentChanged() || mApps == null || configChange) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
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
        if (mForceLoadContentObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mForceLoadContentObserver);
        }
    }


}