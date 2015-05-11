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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

/**
 * Created by Nick Martens on 9/14/13.
 */
public interface IconPack {

    void initialize(Context context);

    Bitmap loadIcon(Context context, ComponentName componentName, Uri fallback,
            boolean applyDecorations);

    String loadTitle(Context context);

    Bitmap applyDecorations(Context context, Bitmap original, Uri uri);

    Drawable getIconPackIcon(Context context);

    /**
     * Gets the uri of the iconpack. Used to identify the iconpack
     */
    Uri getIconPackUri();
}
