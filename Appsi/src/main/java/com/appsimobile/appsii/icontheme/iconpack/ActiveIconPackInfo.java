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

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.appsimobile.appsii.dagger.AppInjector;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by Nick Martens on 9/14/13.
 */
public class ActiveIconPackInfo {

    public static final String APP_ICON_AUTHORITY = "com.appsimobile.appsii.appsplugin.icon";

    private static ActiveIconPackInfo sInstance;
    private final Context mContext;
    private IconPack mActiveIconPack;


    public ActiveIconPackInfo(Context applicationContext) {
        mContext = applicationContext;

    }

    public static synchronized ActiveIconPackInfo getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ActiveIconPackInfo(context.getApplicationContext());
            SharedPreferences preferences = AppInjector.provideSharedPreferences();
            String stringUri = preferences.getString("pref_icon_theme", null);
            if (stringUri != null) {
                Uri uri = Uri.parse(stringUri);
                sInstance.setActiveIconPackUri(uri);
            }
        }
        return sInstance;
    }

    public static Bitmap loadIconFromUriWithoutIconPack(Context context, Uri path) {
        try {
            ContentResolver res = context.getContentResolver();
            InputStream in = res.openInputStream(path);
            if (in == null) return null;
            try {
                return BitmapFactory.decodeStream(in);
            } finally {
                in.close();
            }
        } catch (FileNotFoundException e) {
            Log.w("Appsi", "asset not found: " + e.getMessage());
        } catch (IOException e) {
            Log.w("Appsi", "error loading asset", e);
        } catch (RuntimeException e) {
            Log.w("Appsi", "error loading asset", e);
        }
        return null;

    }

    public void setActiveIconPackUri(Uri uri) {
        mActiveIconPack = IconPackFactory.createIconPack(uri);
        if (mActiveIconPack != null) {
            mActiveIconPack.initialize(mContext);
        }
    }

    public IconPack getActiveIconPack() {
        return mActiveIconPack;
    }

    public Bitmap loadIconFromUri(Context context, Uri path) {
        if (mActiveIconPack == null) {
            return loadIconFromUriWithoutIconPack(context, path);
        }
        String authority = path.getAuthority();
        if (APP_ICON_AUTHORITY.equals(authority)) {
            return loadThemedAppIconFromUri(context, path);
        } else {
            return loadThemedIconFromUri(context, path);
        }
    }

    public Bitmap loadThemedAppIconFromUri(Context context, Uri uri) {
        if (mActiveIconPack == null) return null;

        List<String> segments = uri.getPathSegments();
        if (segments == null) return null;
        String elem = segments.get(1);
        ComponentName componentName = ComponentName.unflattenFromString(elem);

        return mActiveIconPack.loadIcon(context, componentName, uri, true);
    }

    private Bitmap loadThemedIconFromUri(Context context, Uri uri) {
        Bitmap icon = loadIconFromUriWithoutIconPack(context, uri);
        if (icon == null) return null;

        return mActiveIconPack.applyDecorations(context, icon, uri);
    }

    public Bitmap loadThemedAppIconFromComponentName(Context context, ComponentName cn) {
        if (mActiveIconPack == null) return null;

        return mActiveIconPack.loadIcon(context, cn, null, true);
    }

    public Bitmap decorateDefaultIcon(Context context, Bitmap bitmap) {
        if (bitmap == null) return null;

        if (mActiveIconPack != null) {
            bitmap = mActiveIconPack.applyDecorations(context, bitmap, null);
        }
        return bitmap;
    }

}
