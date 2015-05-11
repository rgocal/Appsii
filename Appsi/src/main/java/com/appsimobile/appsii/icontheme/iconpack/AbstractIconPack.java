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

package com.appsimobile.appsii.icontheme.iconpack;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

/**
 * Created by Nick Martens on 9/22/13.
 */
public abstract class AbstractIconPack implements IconPack {

    public static Uri createIconPackUri(String iconPackTypeId, String packageName) {
        return Uri.parse("iconpack://" + iconPackTypeId + "/" + packageName);
    }

    public static String getIconPackTypeIdFromUri(Uri iconPackUri) {
        return iconPackUri.getAuthority();
    }

    public static String getIconPackPackageFromUri(Uri iconPackUri) {
        return iconPackUri.getPathSegments().get(0);
    }

    public Bitmap loadFallBack(Context context, Uri uri) {
        return ActiveIconPackInfo.loadIconFromUriWithoutIconPack(context, uri);
    }

    @Override
    public String loadTitle(Context context) {
        String packageName = getIconPackPackageFromUri(getIconPackUri());
        try {
            ApplicationInfo applicationInfo =
                    context.getPackageManager().getApplicationInfo(packageName, 0);
            CharSequence result = context.getPackageManager().getApplicationLabel(applicationInfo);
            return result == null ? null : result.toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("AbstractIconPack", "Error loading icon pack icon", e);
        }
        return null;
    }

    @Override
    public Drawable getIconPackIcon(Context context) {
        String packageName = getIconPackPackageFromUri(getIconPackUri());
        try {
            return context.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("AbstractIconPack", "Error loading icon pack icon", e);
        }
        return null;
    }
}
