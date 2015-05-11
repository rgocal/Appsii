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

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import net.jcip.annotations.GuardedBy;

import java.io.File;

import static android.app.ActivityManager.RecentTaskInfo;

/**
 * This class holds the per-item data in our Loader.
 */
public class RecentTaskInfoAppEntry implements AppEntry {

    private final File mApkFile;

    private final CharSequence mLabel;

    private final ActivityInfo mActivityInfo;

    @GuardedBy("this")
    private RecentTaskInfo mInfo;

    @GuardedBy("this")
    private Drawable mIcon;

    @GuardedBy("this")
    private boolean mMounted;

    private ComponentName mComponentName;

    public RecentTaskInfoAppEntry(RecentTaskInfo info, ActivityInfo activityInfo,
            CharSequence label) {
        mInfo = info;
        mComponentName =
                info.origActivity == null ? info.baseIntent.getComponent() : info.origActivity;
        mLabel = label;
        mActivityInfo = activityInfo;
        mApkFile = new File(activityInfo.applicationInfo.sourceDir);
    }

    public ApplicationInfo getApplicationInfo() {
        return mActivityInfo.applicationInfo;
    }

    public ActivityInfo getActivityInfo() {
        return mActivityInfo;
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    @Override
    public synchronized Drawable getIconIfReady() {
        // when just mounted, return null to force a load
        if (!mMounted && mApkFile.exists()) return null;

        return mIcon;
    }

    public synchronized Drawable getIcon(PackageManager packageManager) {
        if (mIcon == null) {
            if (mApkFile.exists()) {
                mIcon = mActivityInfo.loadIcon(packageManager);
                mMounted = true;
                return mIcon;
            } else {
                mMounted = false;
            }
        } else if (!mMounted) {
            // If the app wasn't mounted but is now mounted, reload
            // its icon.
            if (mApkFile.exists()) {
                mMounted = true;
                mIcon = mActivityInfo.loadIcon(packageManager);
                return mIcon;
            }
        } else {
            return mIcon;
        }

        return null;
    }

    @Override
    public synchronized void trimMemory() {
        mIcon = null;
    }

    @Override
    public String toString() {
        return String.valueOf(mLabel);
    }

    synchronized void updateInfo(RecentTaskInfo app) {
        mInfo = app;
    }
}