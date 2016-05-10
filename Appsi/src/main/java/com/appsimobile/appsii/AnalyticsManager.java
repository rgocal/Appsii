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
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.ecommerce.Product;
import com.google.android.gms.analytics.ecommerce.ProductAction;

/**
 * Created by nick on 05/04/15.
 */
public class AnalyticsManager {


    public static final String ACTION_OPEN_PAGE = "open_page";

    public static final String ACTION_PREVIEW = "preview";

    public static final String ACTION_ADD_TO_TAG = "add_to_tag";

    public static final String ACTION_SORT_APPS = "sort_apps";

    public static final String ACTION_CHANGE_COLUMN_COUNT = "column_count";

    public static final String ACTION_EDIT_TAGS = "edit_tags";

    public static final String ACTION_ADD_TAG = "add_tag";

    public static final String ACTION_SCROLL_PAGE = "scroll_page";

    public static final String ACTION_COLLAPSE_ITEM = "collapse_item";

    public static final String ACTION_EXPAND_ITEM = "expand_item";

    public static final String ACTION_OPEN_ITEM = "open_item";

    public static final String ACTION_OPEN_HOME_ITEM = "open_home_item";

    public static final String ACTION_CREATE_ITEM = "create_item";

    public static final String ACTION_EDIT_ITEM = "edit_item";

    public static final String ACTION_DELETE_ITEM = "delete_item";

    public static final String ACTION_START_APPSI = "start_appsii";

    public static final String ACTION_STOP_APPSI = "stop_appsii";

    public static final String ACTION_PURCHASE = "purchase";

    public static final String ACTION_APPSI_UNLOCK = "unlock";

    public static final String CATEGORY_APPS = "apps";

    public static final String CATEGORY_HOME = "home";

    public static final String CATEGORY_SEARCH = "search";

    public static final String CATEGORY_PEOPLE = "people";

    public static final String CATEGORY_CALLS = "calls";

    public static final String CATEGORY_AGENDA = "agenda";

    public static final String CATEGORY_OTHER = "configuration";

    public static final String CATEGORY_PAGES = "pages";

    public static final String ACTION_OPEN_GOOGLE_COMMUNITY = "google_community";

    public static final String ACTION_OPEN_GOOGLE_MODERATOR = "google_moderator";

    public static final String ACTION_OPEN_BETA_TESTER = "beta_tester";

    public static final String ACTION_OPEN_FOLLOW_ME = "follow_me";

    public static final String CATEGORY_ABOUT = "about";

    public static final String CATEGORY_WELCOME = "welcome";

    static AnalyticsManager sInstance;

    final Context mContext;

    Tracker mTracker;

    public AnalyticsManager(Context context) {
        mContext = context.getApplicationContext();
        getTracker();
    }

    @NonNull
    public static AnalyticsManager getInstance() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException("Bad thread!");
        }
        return sInstance;
    }

    public static AnalyticsManager getInstance(Context context) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException("Bad thread!");
        }
        if (sInstance == null) {
            sInstance = new AnalyticsManager(context);
        }
        return sInstance;
    }

    public Tracker getTracker() {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(mContext);
        if (BuildConfig.DEBUG) {
            analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            analytics.setDryRun(true);
        }
        Tracker tracker;
        synchronized (this) {
            if (mTracker == null) {
                mTracker = analytics.newTracker(R.xml.ga_tracker);
                mTracker.enableAdvertisingIdCollection(true);
            }
            tracker = mTracker;
        }
        return tracker;
    }

    public void trackAppsiEvent(String action, String category) {
        Tracker tracker = getTracker();

        tracker.send(new HitBuilders.EventBuilder().
                setAction(action).
                setCategory(category).build());
    }

    public void trackAppsiEvent(String action, String category,
            String label) {
        Tracker tracker = getTracker();

        tracker.send(new HitBuilders.EventBuilder().
                setAction(action).
                setCategory(category).
                setLabel(label).build());
    }

    public void trackPageView(String page) {
        Tracker tracker = getTracker();
        tracker.setScreenName("pages/" + page);
        tracker.send(new HitBuilders
                        .AppViewBuilder()
                        .build()
        );
    }



}
