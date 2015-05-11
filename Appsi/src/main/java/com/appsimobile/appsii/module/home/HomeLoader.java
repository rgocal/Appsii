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

package com.appsimobile.appsii.module.home;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.util.ConvertedCursorLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by nick on 22/09/14.
 */
public class HomeLoader extends ConvertedCursorLoader<List<HomeItem>> {


    static final HomeItemComparator sComparator = new HomeItemComparator();

    /**
     * The default sort order for this table.
     */
    private static final String DEFAULT_SORT_ORDER = HomeContract.Cells.PAGE_ID + " ASC, " +
            HomeContract.Cells.ROW_POSITION + " ASC, " +
            HomeContract.Cells.POSITION + " ASC ";

    ForceLoadContentObserver mForceLoadObserver;


    public HomeLoader(Context context) {

        super(context);

        Uri uri = HomeContract.Cells.CONTENT_URI;

        setUri(uri);
        setProjection(HomeQuery.PROJECTION);
        setSelection(null);
        setSelectionArgs(null);
        setSortOrder(DEFAULT_SORT_ORDER);
    }

    public static int longCompare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static int intCompare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    @Override
    protected List<HomeItem> convertCursor(@NonNull Cursor c) {

        c.moveToPosition(-1);

        int count = c.getCount();
        if (count > 0) {

            List<HomeItem> result = new ArrayList<>(count);

            while (c.moveToNext()) {
                HomeItem e = new HomeItem();

                e.mId = c.getLong(HomeQuery.ID);
                e.mPageId = c.getLong(HomeQuery.PAGE_ID);
                e.mRowId = c.getLong(HomeQuery.ROW_ID);
                e.mPageName = c.getString(HomeQuery.PAGE_NAME);
                e.mRowHeight = c.getInt(HomeQuery.ROW_HEIGHT);
                e.mRowPosition = c.getInt(HomeQuery.ROW_POSITION);
                e.mColspan = c.getInt(HomeQuery.COLSPAN);
                e.mPosition = c.getInt(HomeQuery.POSITION);
                e.mDisplayType = c.getInt(HomeQuery.TYPE);
                e.mEffectColor = c.getInt(HomeQuery.EFFECT_COLOR);

                result.add(e);
            }
            Collections.sort(result, sComparator);
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    protected void cleanup(List<HomeItem> old) {

    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        mForceLoadObserver = new ForceLoadContentObserver();
        getContext().getContentResolver().
                registerContentObserver(HomeContract.Rows.CONTENT_URI, true, mForceLoadObserver);

        // start monitoring for day changes to make sure the list is reloaded
        // whenever the date changes.

    }

    @Override
    protected void onReset() {
        super.onReset();

        if (mForceLoadObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mForceLoadObserver);
        }
        // on reset, we need to remove any receivers etc.
    }

    static class HomeQuery {

        public static final int ROW_ID = 0;

        public static final int ROW_HEIGHT = 1;

        public static final int ROW_POSITION = 2;

        public static final int PAGE_ID = 3;

        public static final int PAGE_NAME = 4;

        public static final int COLSPAN = 5;

        public static final int POSITION = 6;

        public static final int TYPE = 7;

        public static final int ID = 8;

        public static final int EFFECT_COLOR = 9;

        static final String[] PROJECTION = {
                HomeContract.Cells.ROW_ID,
                HomeContract.Cells.ROW_HEIGHT,
                HomeContract.Cells.ROW_POSITION,
                HomeContract.Cells.PAGE_ID,
                HomeContract.Cells.PAGE_NAME,
                HomeContract.Cells.COLSPAN,
                HomeContract.Cells.POSITION,
                HomeContract.Cells.TYPE,
                HomeContract.Cells._ID,
                HomeContract.Cells.EFFECT_COLOR,
        };


    }

    static class HomeItemComparator implements Comparator<HomeItem> {

        @Override
        public int compare(HomeItem lhs, HomeItem rhs) {
            int result = longCompare(lhs.mPageId, rhs.mPageId);
            if (result == 0) {
                result = intCompare(lhs.mRowPosition, rhs.mRowPosition);
            }
            if (result == 0) {
                result = intCompare(lhs.mPosition, rhs.mPosition);
            }
            return result;
        }
    }

}
