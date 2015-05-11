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

package com.appsimobile.appsii.appwidget;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Bitmap;

import com.appsimobile.appsii.compat.AppWidgetManagerCompat;

import java.util.List;

/**
 * Utility class to load certain things easily from the widgets.
 * <p/>
 * Created by nick on 19/02/15.
 */
public final class AppWidgetUtils {

    static WidgetPreviewLoader sWidgetPreviewLoader;

    private AppWidgetUtils() {
    }

    /**
     * Loads all app-widget provider infos.
     */
    public static List<AppWidgetProviderInfo> loadAppWidgetProviderInfos(Context context) {
        return AppWidgetManagerCompat.getInstance(context).getAllProviders();
    }

    /**
     * Loads the title of the given widget.
     */
    public static String getWidgetTitle(Context context, AppWidgetProviderInfo info) {
        return AppWidgetManagerCompat.getInstance(context).loadLabel(info);
    }


    public static Bitmap getWidgetPreviewBitmap(
            Context context, AppWidgetProviderInfo info, Bitmap reuse) {

        if (sWidgetPreviewLoader == null) {
            sWidgetPreviewLoader = new WidgetPreviewLoader(context, new IconCache(context));
        }
        return sWidgetPreviewLoader.generateWidgetPreview(info, reuse);

    }

}
