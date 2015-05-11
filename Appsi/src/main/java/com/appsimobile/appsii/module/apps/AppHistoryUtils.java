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

package com.appsimobile.appsii.module.apps;

import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import static com.appsimobile.appsii.module.apps.AppsContract.LaunchHistoryColumns;

/**
 * Utility methods to update the App-History when an app is launched
 * Created by nick on 20/10/14.
 */
public class AppHistoryUtils {

    /**
     * A QueryHandler used to get the app and increment the launch count
     */
    private static AsyncQueryHelper sAsyncQueryHelper;

    /**
     * Tracks a launch of the app described by the given component name
     */
    public static void trackAppLaunch(Context context, ComponentName componentName) {
        if (sAsyncQueryHelper == null) {
            sAsyncQueryHelper = new AsyncQueryHelper(context.getContentResolver());
        }
        sAsyncQueryHelper.trackAppLaunch(componentName);

    }


    private static class AsyncQueryHelper extends AsyncQueryHandler {

        public AsyncQueryHelper(ContentResolver cr) {
            super(cr);
        }

        public void trackAppLaunch(ComponentName componentName) {
            String name = componentName.flattenToShortString();
            startQuery(0, name, LaunchHistoryColumns.CONTENT_URI,
                    AppHistoryQuery.PROJECTION,
                    LaunchHistoryColumns.COMPONENT_NAME + "=?",
                    new String[]{name}, null);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            int count = cursor.getCount();
            if (count == 0) {
                ContentValues insert = new ContentValues();
                insert.put(LaunchHistoryColumns.COMPONENT_NAME, (String) cookie);
                insert.put(LaunchHistoryColumns.LAST_LAUNCHED, System.currentTimeMillis());
                insert.put(LaunchHistoryColumns.LAUNCH_COUNT, 1);
                startInsert(0, cookie, LaunchHistoryColumns.CONTENT_URI, insert);
            } else {
                cursor.moveToNext();
                int launchCount = cursor.getInt(AppHistoryQuery.LAUNCH_COUNT);
                long id = cursor.getLong(AppHistoryQuery.ID);
                ContentValues update = new ContentValues();
                update.put(LaunchHistoryColumns.LAUNCH_COUNT, launchCount + 1);
                update.put(LaunchHistoryColumns.LAST_LAUNCHED, System.currentTimeMillis());
                Uri uri = ContentUris.withAppendedId(LaunchHistoryColumns.CONTENT_URI, id);
                startUpdate(0, cookie, uri, update, null, null);
            }
            cursor.close();
        }
    }

}
