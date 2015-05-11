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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;

import com.appsimobile.appsii.compat.LauncherActivityInfoCompat;

import net.jcip.annotations.GuardedBy;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * This class holds the per-item data in our Loader.
 */
public class ResolveInfoAppEntry implements AppEntry {

    @GuardedBy("this")
    private LauncherActivityInfoCompat mInfo;

    @GuardedBy("this")
    private File mApkFile;

    @GuardedBy("this")
    private CharSequence mLabel;

    @GuardedBy("this")
    private WeakReference<Drawable> mIcon;

    @GuardedBy("this")
    private boolean mMounted;

    @GuardedBy("this")
    private ComponentName mComponentName;

    public ResolveInfoAppEntry(LauncherActivityInfoCompat info, CharSequence label) {
        mInfo = info;
        mLabel = label;
        mApkFile = new File(info.getApplicationInfo().sourceDir);
    }

    /**
     * Updates the info with the most recent version from the system. Calling this is
     * especially important when the app was updated. Otherwise the icon can not be
     * loaded because the file reference is not up-to-date
     */
    public synchronized void update(LauncherActivityInfoCompat info, CharSequence label) {
        mInfo = info;
        mLabel = label;
        mApkFile = new File(info.getApplicationInfo().sourceDir);

    }

    @Override
    public synchronized ApplicationInfo getApplicationInfo() {
        return mInfo.getApplicationInfo();
    }

    @Override
    public synchronized ComponentName getComponentName() {
        if (mComponentName == null) {
            mComponentName = mInfo.getComponentName();
        }
        return mComponentName;
    }

    @Override
    public synchronized CharSequence getLabel() {
        return mLabel;
    }

    @Override
    public synchronized Drawable getIconIfReady() {
        // when just mounted, return null to force a load
        if (!mMounted && mApkFile.exists()) return null;

        return mIcon == null ? null : mIcon.get();
    }

    @Override
    public synchronized Drawable getIcon(PackageManager packageManager) {
        Drawable icon = getIconIfReady();
        int density;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            density = DisplayMetrics.DENSITY_XXXHIGH;
        } else {
            density = DisplayMetrics.DENSITY_XXHIGH;
        }
        if (icon == null) {
            if (mApkFile.exists()) {
                Drawable result = mInfo.getBadgedIcon(density);
                mIcon = new WeakReference<>(result);
                mMounted = true;
                return result;
            } else {
                mMounted = false;
            }
        } else if (!mMounted) {
            // If the app wasn't mounted but is now mounted, reload
            // its icon.
            if (mApkFile.exists()) {
                mMounted = true;
                Drawable result = mInfo.getBadgedIcon(density);
                mIcon = new WeakReference<>(result);

                return result;
            }
        } else {
            return icon;
        }

        return null;
    }


    @Override
    public synchronized void trimMemory() {

    }

    @Override
    public synchronized String toString() {
        return String.valueOf(mLabel);
    }

}