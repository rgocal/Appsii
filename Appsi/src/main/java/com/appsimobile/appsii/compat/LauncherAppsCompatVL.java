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

package com.appsimobile.appsii.compat;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v4.util.SimpleArrayMap;

import com.appsimobile.appsii.AppsiiUtils;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LauncherAppsCompatVL extends LauncherAppsCompat {

    final Context mContext;

    private final SimpleArrayMap<OnAppsChangedCallbackCompat, WrappedCallback> mCallbacks =
            new SimpleArrayMap<>();

    private final LauncherApps mLauncherApps;

    LauncherAppsCompatVL(Context context) {
        super();
        mLauncherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        mContext = context;
    }

    @Override
    public List<LauncherActivityInfoCompat> getActivityList(String packageName,
            UserHandleCompat user) {
        List<LauncherActivityInfo> list = mLauncherApps.getActivityList(packageName,
                user.getUser());
        if (list.size() == 0) {
            return Collections.emptyList();
        }
        ArrayList<LauncherActivityInfoCompat> compatList =
                new ArrayList<>(list.size());
        for (LauncherActivityInfo info : list) {
            compatList.add(new LauncherActivityInfoCompatVL(info));
        }
        return compatList;
    }

    @Override
    public LauncherActivityInfoCompat resolveActivity(Intent intent, UserHandleCompat user) {
        LauncherActivityInfo activity = mLauncherApps.resolveActivity(intent, user.getUser());
        if (activity != null) {
            return new LauncherActivityInfoCompatVL(activity);
        } else {
            return null;
        }
    }

    @Override
    public void startActivityForProfile(ComponentName component, UserHandleCompat user,
            Rect sourceBounds, Bundle opts) {
        try {
            mLauncherApps.startMainActivity(component, user.getUser(), sourceBounds, opts);
        } catch (RuntimeException e) {
            // for some reason a report with an NPE here occurred. This may indicate that
            // the app was uninstalled and the list was not updated. This is strange
            Crashlytics.logException(e);
        }
        AppsiiUtils.closeSidebar(mContext);
    }

    @Override
    public void showAppDetailsForProfile(ComponentName component, UserHandleCompat user) {
        mLauncherApps.startAppDetailsActivity(component, user.getUser(), null, null);
        AppsiiUtils.closeSidebar(mContext);
    }

    @Override
    public void addOnAppsChangedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat callback) {
        WrappedCallback wrappedCallback = new WrappedCallback(callback);
        synchronized (mCallbacks) {
            mCallbacks.put(callback, wrappedCallback);
        }
        mLauncherApps.registerCallback(wrappedCallback);
    }

    @Override
    public void removeOnAppsChangedCallback(
            LauncherAppsCompat.OnAppsChangedCallbackCompat callback) {
        WrappedCallback wrappedCallback;
        synchronized (mCallbacks) {
            wrappedCallback = mCallbacks.remove(callback);
        }
        if (wrappedCallback != null) {
            mLauncherApps.unregisterCallback(wrappedCallback);
        }
    }

    @Override
    public boolean isPackageEnabledForProfile(String packageName, UserHandleCompat user) {
        return mLauncherApps.isPackageEnabled(packageName, user.getUser());
    }

    @Override
    public boolean isActivityEnabledForProfile(ComponentName component, UserHandleCompat user) {
        return mLauncherApps.isActivityEnabled(component, user.getUser());
    }

    private static class WrappedCallback extends LauncherApps.Callback {

        private final LauncherAppsCompat.OnAppsChangedCallbackCompat mCallback;

        public WrappedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat callback) {
            mCallback = callback;
        }

        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            mCallback.onPackageRemoved(packageName, UserHandleCompat.fromUser(user));
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            mCallback.onPackageAdded(packageName, UserHandleCompat.fromUser(user));
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            mCallback.onPackageChanged(packageName, UserHandleCompat.fromUser(user));
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            mCallback.onPackagesAvailable(packageNames, UserHandleCompat.fromUser(user), replacing);
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user,
                boolean replacing) {
            mCallback.onPackagesUnavailable(packageNames, UserHandleCompat.fromUser(user),
                    replacing);
        }
    }
}

