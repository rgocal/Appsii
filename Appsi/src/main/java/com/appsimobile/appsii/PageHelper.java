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
 *
 */

package com.appsimobile.appsii;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import com.appsimobile.appsii.iab.FeatureManager;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.crashlytics.android.Crashlytics;

/**
 * Created by nick on 16/06/15.
 */
public class PageHelper {

    static PageHelper sInstance;

    final AsyncQueryHandlerImpl mQueryHandler;

    final ContentResolver mContentResolver;

    private PageHelper(Context context) {
        context = context.getApplicationContext();
        mContentResolver = context.getContentResolver();
        mQueryHandler = new AsyncQueryHandlerImpl(context, mContentResolver);
    }

    public static PageHelper getInstance(Context context) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException("PageHelper can only be used on the main thread");
        }

        if (sInstance == null) {
            sInstance = new PageHelper(context);
        }
        return sInstance;
    }

    public void enablePageAccess(int pageType, boolean forceAddToHotspots) {
        Log.d("Appsii", "enable page: " + pageType + " force: " + forceAddToHotspots);
        mQueryHandler.ensurePageEnabled(pageType, forceAddToHotspots);
    }

    public void enablePageAccess(String sku, boolean forceAddToHotspots) {
        Log.d("Appsii", "enable sku: " + sku + " force: " + forceAddToHotspots);
        mQueryHandler.ensurePageEnabled(sku, forceAddToHotspots);
    }

    public void disablePageAccess(int pageType) {
        Log.d("Appsii", "disable page: " + pageType);
        mQueryHandler.disablePage(pageType);
    }

    public void removePageFromHotspots(int pageType) {
        Log.d("Appsii", "disable page: " + pageType);
        mQueryHandler.removePageFromHotspots(pageType);
    }

    static class AsyncQueryHandlerImpl extends AsyncQueryHandler {

        static final int QUERY_PAGE_INSERTED = 1;

        static final int QUERY_PAGE_INSERTED_AND_ADD_TO_HOTSPOTS = 5;

        static final int QUERY_PAGE_ID = 6;

        static final int QUERY_HOTSPOTS = 2;

        static final int INSERT_ENABLE_PAGE = 3;

        static final int INSERT_HOTSPOT_PAGE = 4;

        final Context mContext;

        public AsyncQueryHandlerImpl(Context context, ContentResolver cr) {
            super(cr);
            mContext = context.getApplicationContext();
        }

        public void ensurePageEnabled(String sku, boolean forceOnHotspots) {
            switch (sku) {
                case FeatureManager.AGENDA_FEATURE:
                    ensurePageEnabled(HomeContract.Pages.PAGE_AGENDA, forceOnHotspots);
                    break;
                case FeatureManager.SETTINGS_AGENDA_FEATURE:
                    ensurePageEnabled(HomeContract.Pages.PAGE_AGENDA, forceOnHotspots);
                    ensurePageEnabled(HomeContract.Pages.PAGE_SETTINGS, forceOnHotspots);
                    break;
                case FeatureManager.SMS_CALLS_PEOPLE_FEATURE:
                    ensurePageEnabled(HomeContract.Pages.PAGE_SMS, forceOnHotspots);
                    ensurePageEnabled(HomeContract.Pages.PAGE_CALLS, forceOnHotspots);
                    ensurePageEnabled(HomeContract.Pages.PAGE_PEOPLE, forceOnHotspots);
                    break;
                case FeatureManager.SETTINGS_FEATURE:
                    ensurePageEnabled(HomeContract.Pages.PAGE_SETTINGS, forceOnHotspots);
                    break;
                case FeatureManager.ALL_FEATURE:
                    ensurePageEnabled(HomeContract.Pages.PAGE_AGENDA, forceOnHotspots);
                    ensurePageEnabled(HomeContract.Pages.PAGE_CALLS, forceOnHotspots);
                    ensurePageEnabled(HomeContract.Pages.PAGE_PEOPLE, forceOnHotspots);
                    break;
                case FeatureManager.CALLS_FEATURE:
                    ensurePageEnabled(HomeContract.Pages.PAGE_CALLS, forceOnHotspots);
                    break;
                case FeatureManager.PEOPLE_FEATURE:
                    ensurePageEnabled(HomeContract.Pages.PAGE_PEOPLE, forceOnHotspots);
                    break;
                case FeatureManager.SMS_FEATURE:
                    ensurePageEnabled(HomeContract.Pages.PAGE_SMS, forceOnHotspots);
                    break;
            }
        }

        /**
         * Enables a page type.
         * When forceOnHotspots is true, the page is added to all of the
         * hotspots, always. When it is false it is only added if the
         * page was disabled before.
         * <p/>
         * Internally this executes a query to see if a page is in the
         * pages table. If a page is in the pages table, this means it
         * is available to the user. Typically disabled pages need to
         * be purchased first.
         */
        public void ensurePageEnabled(int pageType, boolean forceOnHotspots) {
            // we use a different token to differentiate between
            // forcing the hotspots or not.
            int token = forceOnHotspots ?
                    QUERY_PAGE_INSERTED_AND_ADD_TO_HOTSPOTS : QUERY_PAGE_INSERTED;

            startQuery(token, pageType,
                    HomeContract.Pages.CONTENT_URI,
                    new String[]{HomeContract.Pages._ID},
                    HomeContract.Pages.TYPE + "=?",
                    new String[]{String.valueOf(pageType)},
                    null
            );
        }

        public void disablePage(int pageType) {
            // we can simply delete this page from the pages table.
            // this will cascade into the hotspot_pages table.
            startDelete(0, null, HomeContract.Pages.CONTENT_URI,
                    HomeContract.Pages.TYPE + "=?",
                    new String[]{
                            String.valueOf(pageType)
                    });
        }

        public void removePageFromHotspots(int pageType) {
            startQuery(QUERY_PAGE_ID, pageType,
                    HomeContract.Pages.CONTENT_URI,
                    new String[]{HomeContract.Pages._ID},
                    HomeContract.Pages.TYPE + "=?",
                    new String[]{String.valueOf(pageType)},
                    null);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

            if (cursor == null) {
                Log.e("PageHelper", "cursor == null");
                Crashlytics.logException(new NullPointerException("cursor == null"));
            }

            switch (token) {
                case QUERY_PAGE_INSERTED:
                case QUERY_PAGE_INSERTED_AND_ADD_TO_HOTSPOTS:

                    boolean add = token == QUERY_PAGE_INSERTED_AND_ADD_TO_HOTSPOTS;

                    int pageType = (int) cookie;
                    int count = cursor.getCount();
                    if (count == 0) {
                        // if not enabled, insert into the pages table
                        enablePage(pageType);
                    } else if (add) {
                        // When enabled, we want to add the page to each of the hotspots
                        // we nee the uri of the page for that.
                        cursor.moveToFirst();
                        long pageId = cursor.getLong(0);
                        Uri pageUri =
                                ContentUris.withAppendedId(HomeContract.Pages.CONTENT_URI, pageId);
                        queryHotspots(pageUri);
                    }

                    cursor.close();
                    break;
                case QUERY_PAGE_ID:
                    if (cursor.moveToFirst()) {
                        long pageId = cursor.getLong(0);
                        startDelete(0, null, HomeContract.HotspotPages.CONTENT_URI,
                                HomeContract.HotspotPages._PAGE_ID + "=?",
                                new String[]{
                                        String.valueOf(pageId)
                                });
                    }
                    break;
                case QUERY_HOTSPOTS:

                    // The query to get the hotspots in the system completed.
                    // Now add the page (given in the cookie) to each of the
                    // hotspots.
                    Uri pageUri = (Uri) cookie;
                    long pageId = ContentUris.parseId(pageUri);
                    while (cursor.moveToNext()) {
                        long hotspotId = cursor.getLong(0);
                        ContentValues values = new ContentValues(3);
                        values.put(HomeContract.HotspotPages._PAGE_ID, pageId);
                        values.put(HomeContract.HotspotPages._HOTPSOT_ID, hotspotId);
                        values.put(HomeContract.HotspotPages.POSITION, 12);
                        startInsert(INSERT_HOTSPOT_PAGE, null,
                                HomeContract.HotspotPages.CONTENT_URI, values);
                    }
                    cursor.close();

                    break;
            }
        }

        public void enablePage(int pageType) {
            ContentValues values = new ContentValues();
            String displayName = getTitleForPageType(pageType);

            values.put(HomeContract.Pages.TYPE, pageType);
            values.put(HomeContract.Pages.DISPLAY_NAME, displayName);

            startInsert(INSERT_ENABLE_PAGE, null, HomeContract.Pages.CONTENT_URI, values);
        }

        /**
         * Query the hotspots in the system. The provided uri is the uri
         * of the page we want to add to each of the found hotspots.
         */
        private void queryHotspots(Uri pageUri) {
            startQuery(QUERY_HOTSPOTS, pageUri,
                    HomeContract.Hotspots.CONTENT_URI,
                    new String[]{HomeContract.Hotspots._ID},
                    null,
                    null,
                    null);
        }

        private String getTitleForPageType(int pageType) {
            int resId;
            switch (pageType) {
                case HomeContract.Pages.PAGE_AGENDA:
                    resId = R.string.agenda_page_name;
                    break;
                case HomeContract.Pages.PAGE_CALLS:
                    resId = R.string.calls_page_name;
                    break;
                case HomeContract.Pages.PAGE_PEOPLE:
                    resId = R.string.people_page_name;
                    break;
                default:
                    return null;
            }
            return mContext.getString(resId);
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            if (token == INSERT_ENABLE_PAGE) {
                queryHotspots(uri);
            }
        }
    }


}
