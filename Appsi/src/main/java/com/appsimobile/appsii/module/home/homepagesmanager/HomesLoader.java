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

package com.appsimobile.appsii.module.home.homepagesmanager;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.util.ConvertedCursorLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * A loader for loading al the home pages from the database
 * Created by nick on 22/09/14.
 */
public class HomesLoader extends ConvertedCursorLoader<List<HomePageItem>> {


    ForceLoadContentObserver mForceLoadObserver;


    public HomesLoader(Context context) {

        super(context);

        Uri uri = HomeContract.Pages.CONTENT_URI;

        setUri(uri);
        setProjection(HomeQuery.PROJECTION);
        setSelection(HomeContract.Pages.TYPE + "=?");
        setSelectionArgs(new String[]{String.valueOf(HomeContract.Pages.PAGE_HOME)});
        setSortOrder(HomeContract.Pages._ID + " ASC");
    }

    @Override
    protected List<HomePageItem> convertCursor(@NonNull Cursor cursor) {


        cursor.moveToPosition(-1);

        List<HomePageItem> result = new ArrayList<>();

        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(HomeQuery.ID);
                String name = cursor.getString(HomeQuery.DISPLAY_NAME);

                HomePageItem item = new HomePageItem();
                item.mId = id;
                item.mTitle = name;

                result.add(item);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    @Override
    protected void cleanup(List<HomePageItem> old) {

    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        mForceLoadObserver = new ForceLoadContentObserver();
        getContext().getContentResolver().
                registerContentObserver(HomeContract.Hotspots.CONTENT_URI, true,
                        mForceLoadObserver);

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


}
