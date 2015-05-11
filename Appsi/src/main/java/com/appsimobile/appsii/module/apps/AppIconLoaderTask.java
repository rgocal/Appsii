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
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.appsimobile.appsii.plugins.AppIconHelper;

abstract class AppIconLoaderTask extends AsyncTask<Void, Void, Drawable> {

    private final AppEntry mAppEntry;

    private final PackageManager mPackageManager;

    private final Context mContext;

    AppIconLoaderTask(Context context, AppEntry appEntry, PackageManager packageManager) {
        mAppEntry = appEntry;
        mContext = context;
        mPackageManager = packageManager;
    }

    @Override
    protected Drawable doInBackground(Void... params) {
        ComponentName componentName = mAppEntry.getComponentName();
        int wh = (int) (mContext.getResources().getDisplayMetrics().density * 40);
        Bitmap result = AppIconHelper.loadAppIcon(mContext, componentName, wh, wh);
        if (result != null) {
            return new BitmapDrawable(mContext.getResources(), result);
        }
        return mAppEntry.getIcon(mPackageManager);
    }

}