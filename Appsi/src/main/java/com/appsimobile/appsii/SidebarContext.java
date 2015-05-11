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

package com.appsimobile.appsii;

import android.content.Context;
import android.content.ContextWrapper;

/**
 * Created by nick on 11/03/15.
 */
public class SidebarContext extends ContextWrapper {

    public boolean mIsFullScreen;

    AnalyticsManager mAnalyticsManager = AnalyticsManager.getInstance();

    private LoaderManager mLoaderManager;

    public SidebarContext(Context base) {
        super(base);
    }

    public static void track(Context context, String action, String category, String label) {
        ((SidebarContext) context).track(action, category, label);
    }

    public static void track(Context context, String action, String category) {
        ((SidebarContext) context).track(action, category);
    }

    public void track(String action, String category, String label) {
        mAnalyticsManager.trackAppsiEvent(action, category, label);
    }

    public void track(String action, String category) {
        mAnalyticsManager.trackAppsiEvent(action, category);
    }

    public void trackPageView(String page) {
        mAnalyticsManager.trackPageView(page);
    }

    public LoaderManager getLoaderManager() {
        return mLoaderManager;
    }

    public void setLoaderManager(LoaderManager loaderManager) {
        mLoaderManager = loaderManager;
    }

//        @Override
//        public int getColor(Resources res, AccentPalette palette, int resId) {
//            int result = res.getColor(resId);
//            if (result == 0xff009688) {
//                return Color.RED;
//            }
//            return result;
//        }
}
