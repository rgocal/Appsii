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
import android.graphics.Bitmap;

import com.appsimobile.appsii.compat.AppWidgetManagerCompat;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility class to load certain things easily from the widgets.
 * <p/>
 * Created by nick on 19/02/15.
 */
@Singleton
public final class AppWidgetUtils {

    final WidgetPreviewLoader mWidgetPreviewLoader;

    final AppWidgetManagerCompat mAppWidgetManagerCompat;

    final AppWidgetIconCache mAppWidgetIconCache;

    @Inject
    public AppWidgetUtils(WidgetPreviewLoader widgetPreviewLoader,
            AppWidgetManagerCompat appWidgetManagerCompat, AppWidgetIconCache appWidgetIconCache) {
        mWidgetPreviewLoader = widgetPreviewLoader;
        mAppWidgetManagerCompat = appWidgetManagerCompat;
        mAppWidgetIconCache = appWidgetIconCache;
    }

    /**
     * Loads all app-widget provider infos.
     */
    public List<AppWidgetProviderInfo> loadAppWidgetProviderInfos() {
        return mAppWidgetManagerCompat.getAllProviders();
    }

    /**
     * Loads the title of the given widget.
     */
    public String getWidgetTitle(AppWidgetProviderInfo info) {
        return mAppWidgetManagerCompat.loadLabel(info);
    }


    public Bitmap getWidgetPreviewBitmap(AppWidgetProviderInfo info, Bitmap reuse) {

        return mWidgetPreviewLoader.generateWidgetPreview(info, reuse);

    }

}
