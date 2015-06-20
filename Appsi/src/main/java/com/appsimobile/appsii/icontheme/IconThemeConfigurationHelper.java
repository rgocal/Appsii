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

package com.appsimobile.appsii.icontheme;

import android.content.Context;
import android.net.Uri;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Nick Martens on 9/12/13.
 */
public class IconThemeConfigurationHelper {

    private static IconThemeConfigurationHelper sInstance;

    private final Context mContext;

    private Map<String, String> mPrefixMapping = new ConcurrentHashMap<String, String>();

    public IconThemeConfigurationHelper(Context context) {
        mContext = context;
    }

    public static synchronized IconThemeConfigurationHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new IconThemeConfigurationHelper(context);
        }
        return sInstance;
    }

    public Uri createIconUri(String key, String prefix) {
        Uri u = Uri.parse(prefix);
        return Uri.withAppendedPath(u, Uri.encode(key));
    }
}
