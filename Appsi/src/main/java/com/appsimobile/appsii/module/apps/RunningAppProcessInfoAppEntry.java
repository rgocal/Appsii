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

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/**
 * Created by nick on 17/08/14.
 */
public class RunningAppProcessInfoAppEntry implements AppEntry {

    private final ActivityManager.RunningAppProcessInfo mRunningTaskInfo;

    private final ApplicationInfo mApplicationInfo;

    private final CharSequence mLabel;

    public RunningAppProcessInfoAppEntry(ActivityManager.RunningAppProcessInfo runningTaskInfo,
            ApplicationInfo applicationInfo, CharSequence label) {
        mRunningTaskInfo = runningTaskInfo;
        mApplicationInfo = applicationInfo;
        mLabel = label;

    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return mApplicationInfo;
    }

    @Override
    public ComponentName getComponentName() {
        return mRunningTaskInfo.importanceReasonComponent;
    }

    @Override
    public CharSequence getLabel() {
        return mLabel;
    }

    @Override
    public synchronized Drawable getIconIfReady() {
        return null;
    }


    @Override
    public Drawable getIcon(PackageManager packageManager) {
        if (mRunningTaskInfo.importanceReasonComponent != null) {
            try {
                return packageManager.getActivityLogo(mRunningTaskInfo.importanceReasonComponent);
            } catch (PackageManager.NameNotFoundException e) {
                // ignore, try something else
            }
        }
        return packageManager.getApplicationLogo(mApplicationInfo);
    }

    @Override
    public void trimMemory() {

    }
}
