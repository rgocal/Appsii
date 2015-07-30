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
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.Nullable;
import android.util.Log;

import com.appsimobile.appsii.compat.LauncherActivityInfoCompat;
import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.compat.UserHandleCompat;
import com.appsimobile.appsii.module.apps.AppEntry;
import com.appsimobile.appsii.module.apps.InterestingConfigChanges;
import com.appsimobile.appsii.module.apps.PackageIntentReceiver;
import com.appsimobile.appsii.module.apps.ResolveInfoAppEntry;
import com.appsimobile.appsii.module.apps.ShortcutNameComparator;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class AppSearchLoader extends AsyncTaskLoader<List<AppEntry>> {

    static final Map<ComponentName, CharSequence> sLabelCache = new ConcurrentHashMap<>();

    final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();

    final String mQuery;

    final PackageManager mPackageManager;

    private final ShortcutNameComparator mShortcutNameComparator;

    private final Map<ComponentName, CharSequence> mLabelCache;

    List<AppEntry> mApps;

    PackageIntentReceiver mPackageObserver;

    public AppSearchLoader(Context context, String query) {
        super(context);

        mLabelCache = sLabelCache;

        // Retrieve the package manager for later use; note we don't
        // use 'context' directly but instead the save global application
        // context returned by getContext().
        mPackageManager = getContext().getPackageManager();

        mShortcutNameComparator = new ShortcutNameComparator(context, mLabelCache);
        mQuery = query.toLowerCase();
    }

    static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
        if (info.activityInfo != null) {
            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        } else {
            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
        }
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<AppEntry> apps) {
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
    public List<AppEntry> loadInBackground() {


        LauncherAppsCompat lap = LauncherAppsCompat.getInstance(getContext());
        List<LauncherActivityInfoCompat> apps = getLauncherInfosSafely(lap);

        // Fail if we don't have any apps
        if (apps == null || apps.isEmpty()) {
            return null;
        }
        // Sort the applications by name
        Collections.sort(apps, mShortcutNameComparator);

        int N = apps.size();
        List<AppEntry> result = new ArrayList<>(N);
        // Create the ApplicationInfos
        for (int i = 0; i < N; i++) {
            LauncherActivityInfoCompat app = apps.get(i);

            CharSequence label;
            ComponentName componentName = app.getComponentName();

            if (mLabelCache.containsKey(componentName)) {
                label = mLabelCache.get(componentName);
            } else {
                label = app.getLabel().toString().trim();
                mLabelCache.put(componentName, label);
            }

            String lowerCaseTitle = String.valueOf(label).toLowerCase();
            if (label != null && lowerCaseTitle.contains(mQuery)) {
                AppEntry entry = new ResolveInfoAppEntry(app, label);
                result.add(entry);
            }

        }

        return result;
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<AppEntry> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    @Nullable
    private List<LauncherActivityInfoCompat> getLauncherInfosSafely(LauncherAppsCompat lap) {
        return getLauncherInfosSafely(lap, 0, null);
    }

    @Nullable
    private List<LauncherActivityInfoCompat> getLauncherInfosSafely(LauncherAppsCompat lap,
            int counter, Exception exception) {
        try {
            List<LauncherActivityInfoCompat> activityList =
                    lap.getActivityList(null, UserHandleCompat.myUserHandle());

            // we want these exceptions logged.
            if (exception != null) {
                Crashlytics.logException(new RuntimeException("count=" + counter, exception));
            }

            return activityList;
        } catch (RuntimeException e) {
            // We try to fix the package manager here to fix this exception
            // Caused by: java.lang.RuntimeException: Package manager has died
            if (counter < 6) {
                int sleepTime = counter * 100;
                Log.e("Apps", "error loading apps, retrying in " + sleepTime + "ms");
                counter++;
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                return getLauncherInfosSafely(lap, counter, e);

            } else {
                // Log this case. We need to know if 6 is sufficient. Otherwise we may need
                // to increase the repeat count
                Log.e("Apps", "error loading apps, for " + counter + " times. Giving up");
                Crashlytics.logException(new RuntimeException(
                        "Still failed after " + counter + " retries. Giving up", e));
            }
        }
        return null;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(List<AppEntry> apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        List<AppEntry> oldApps = mApps;
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
    }


}