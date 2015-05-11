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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.R;

import java.util.HashMap;

import static com.appsimobile.appsii.module.apps.AppsContract.LaunchHistoryColumns;
import static com.appsimobile.appsii.module.apps.AppsContract.TagColumns;
import static com.appsimobile.appsii.module.apps.AppsContract.TaggedAppColumns;

/**
 * Created by Nick on 19/02/14.
 */
public class AppsProvider extends ContentProvider {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".apps";

    public static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "apps.db";

    private static final int TABLE_APPS = 1;

    private static final int TABLE_APPS_ITEM = 2;

    private static final int TABLE_TAGS = 3;

    private static final int TABLE_TAGS_ITEM = 4;

    private static final int TABLE_HISTORY = 5;

    private static final int TABLE_HISTORY_ITEM = 6;

    private static final UriMatcher sURLMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final HashMap<String, String> sAppsProjectionMap;

    private static final HashMap<String, String> sTagsProjectionMap;

    private static final HashMap<String, String> sHistoryProjectionMap;

    static {
        sURLMatcher.addURI(AUTHORITY, "taggedApps", TABLE_APPS);
        sURLMatcher.addURI(AUTHORITY, "taggedApps/#", TABLE_APPS_ITEM);
        sURLMatcher.addURI(AUTHORITY, "tags", TABLE_TAGS);
        sURLMatcher.addURI(AUTHORITY, "tags/#", TABLE_TAGS_ITEM);
        sURLMatcher.addURI(AUTHORITY, "launchHistory", TABLE_HISTORY);
        sURLMatcher.addURI(AUTHORITY, "launchHistory/#", TABLE_HISTORY_ITEM);


        sAppsProjectionMap = new HashMap<>();
        // Events columns
        sAppsProjectionMap.put(TaggedAppColumns._ID,
                TaggedAppColumns.TABLE_NAME + "." + TaggedAppColumns._ID);

        sAppsProjectionMap.put(TaggedAppColumns.POSITION,
                TaggedAppColumns.TABLE_NAME + "." + TaggedAppColumns.POSITION);

        sAppsProjectionMap.put(TaggedAppColumns.DELETED,
                TaggedAppColumns.TABLE_NAME + "." + TaggedAppColumns.DELETED);

        sAppsProjectionMap.put(TaggedAppColumns.COMPONENT_NAME,
                TaggedAppColumns.TABLE_NAME + "." + TaggedAppColumns.COMPONENT_NAME);

        sAppsProjectionMap.put(AppsContract.JOINED_APP_TAGS.TAG_NAME,
                TagColumns.TABLE_NAME + "." + TagColumns.NAME);

        sAppsProjectionMap.put(AppsContract.JOINED_APP_TAGS.TAG_ID,
                TagColumns.TABLE_NAME + "." + TagColumns._ID);


        sTagsProjectionMap = new HashMap<>();
        // Events columns
        sTagsProjectionMap.put(TagColumns._ID, TagColumns._ID);
        sTagsProjectionMap.put(TagColumns.POSITION, TagColumns.POSITION);
        sTagsProjectionMap.put(TagColumns.DEFAULT_EXPANDED, TagColumns.DEFAULT_EXPANDED);
        sTagsProjectionMap.put(TagColumns.VISIBLE, TagColumns.VISIBLE);
        sTagsProjectionMap.put(TagColumns.NAME, TagColumns.NAME);
        sTagsProjectionMap.put(TagColumns.COLUMN_COUNT, TagColumns.COLUMN_COUNT);
        sTagsProjectionMap.put(TagColumns.TAG_TYPE, TagColumns.TAG_TYPE);

        sHistoryProjectionMap = new HashMap<>();
        // Events columns
        sHistoryProjectionMap.put(LaunchHistoryColumns._ID, LaunchHistoryColumns._ID);
        sHistoryProjectionMap.put(LaunchHistoryColumns.COMPONENT_NAME,
                LaunchHistoryColumns.COMPONENT_NAME);
        sHistoryProjectionMap.put(LaunchHistoryColumns.LAST_LAUNCHED,
                LaunchHistoryColumns.LAST_LAUNCHED);
        sHistoryProjectionMap.put(LaunchHistoryColumns.LAUNCH_COUNT,
                LaunchHistoryColumns.LAUNCH_COUNT);
    }

    private SQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {


        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // Generate the body of the query
        int match = sURLMatcher.match(uri);
        switch (match) {
            // loads all tagged-apps in a given tag.
            case TABLE_APPS_ITEM:
                long id = ContentUris.parseId(uri);
                qb.setTables(TaggedAppColumns.TABLE_NAME + "," + TagColumns.TABLE_NAME);
                qb.setProjectionMap(sAppsProjectionMap);
                qb.appendWhere(TaggedAppColumns.TAG_ID + "=" + id + " AND ");
                qb.appendWhere("taggedApps.tag_id=tags._id");
                break;
            // loads all tagged apps
            case TABLE_APPS:
                qb.setTables(TaggedAppColumns.TABLE_NAME + "," + TagColumns.TABLE_NAME);
                qb.setProjectionMap(sAppsProjectionMap);
                qb.appendWhere("taggedApps.tag_id=tags._id");
                break;
            // loads items in the launch history table
            case TABLE_HISTORY:
                qb.setTables(LaunchHistoryColumns.TABLE_NAME);
                qb.setProjectionMap(sHistoryProjectionMap);
                break;
            // loads all tags
            case TABLE_TAGS:
                qb.setTables(TagColumns.TABLE_NAME);
                qb.setProjectionMap(sTagsProjectionMap);
                break;
            default:
                throw new IllegalArgumentException("Invalid uri: " + uri);
        }
        return qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = db.insert(args.table, null, initialValues);
        if (rowId <= 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);

        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (db.insert(args.table, null, values[i]) < 0) return 0;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        sendNotify(uri);
        return values.length;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        int match = sURLMatcher.match(uri);
        switch (match) {
            case TABLE_APPS_ITEM: {
                String id = uri.getLastPathSegment();
                count = db.delete(args.table, "_id=" + id, args.args);
                break;
            }
            case TABLE_APPS: {
                count = db.delete(args.table, args.where, args.args);
                break;
            }
            case TABLE_TAGS_ITEM: {
                String id = uri.getLastPathSegment();
                count = db.delete(args.table, "_id=" + id, args.args);
                break;
            }
            case TABLE_TAGS: {
                count = db.delete(args.table, args.where, args.args);
                break;
            }
            case TABLE_HISTORY_ITEM: {
                String id = uri.getLastPathSegment();
                count = db.delete(args.table, "_id=" + id, args.args);
                break;
            }
            case TABLE_HISTORY: {
                count = db.delete(args.table, args.where, args.args);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URL " + uri);
            }
        }
        if (count > 0) {
            sendNotify(uri);
        }

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }


    private void sendNotify(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
    }

    static class SqlArguments {

        public final String table;

        public final String where;

        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }

    public class DatabaseHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.beginTransaction();
            Context context = getContext();
            try {
                db.execSQL("CREATE TABLE " + TagColumns.TABLE_NAME + " (" +
                                TagColumns._ID + " INTEGER PRIMARY KEY, " +
                                TagColumns.DEFAULT_EXPANDED + " INTEGER NOT NULL DEFAULT 0, " +
                                TagColumns.POSITION + " INTEGER NOT NULL DEFAULT 1, " +
                                TagColumns.COLUMN_COUNT + " INTEGER NOT NULL DEFAULT 3, " +
                                TagColumns.TAG_TYPE + " INTEGER NOT NULL DEFAULT " +
                                TagColumns.TAG_TYPE_USER + ", " +
                                TagColumns.VISIBLE + " INTEGER NOT NULL DEFAULT 1, " +
                                TagColumns.NAME + " TEXT NOT NULL " +
                                ");"
                );
                // insert recent apps folder
                ContentValues values = new ContentValues();
                values.put(TagColumns.DEFAULT_EXPANDED, 0);
                values.put(TagColumns.POSITION, 0);
                values.put(TagColumns.COLUMN_COUNT, 3);
                values.put(TagColumns.TAG_TYPE, TagColumns.TAG_TYPE_RECENT);
                values.put(TagColumns.VISIBLE, 1);
                values.put(TagColumns.NAME, context.getString(R.string.folder_recent_apps));
                db.insert(TagColumns.TABLE_NAME, null, values);


                // insert all apps folder
                values.clear();
                values.put(TagColumns.DEFAULT_EXPANDED, 0);
                values.put(TagColumns.POSITION, 1);
                values.put(TagColumns.COLUMN_COUNT, 3);
                values.put(TagColumns.TAG_TYPE, TagColumns.TAG_TYPE_ALL);
                values.put(TagColumns.VISIBLE, 1);
                values.put(TagColumns.NAME, context.getString(R.string.all_apps));
                db.insert(TagColumns.TABLE_NAME, null, values);

                // create the table for tagged apps
                db.execSQL("CREATE TABLE " + TaggedAppColumns.TABLE_NAME + " (" +
                                TaggedAppColumns._ID + " INTEGER PRIMARY KEY, " +
                                TaggedAppColumns.DELETED + " INTEGER NOT NULL DEFAULT 0, " +
                                TaggedAppColumns.POSITION + " INTEGER NOT NULL DEFAULT 0, " +
                                TaggedAppColumns.TAG_ID + " INTEGER NOT NULL, " +
                                TaggedAppColumns.COMPONENT_NAME + " TEXT NOT NULL, " +
                                " FOREIGN KEY (" +
                                TaggedAppColumns.TAG_ID + ") " +
                                " REFERENCES " + TagColumns.TABLE_NAME + " (" + TagColumns._ID +
                                ")" +
                                ");"
                );

                // create table for app history
                db.execSQL("CREATE TABLE " + LaunchHistoryColumns.TABLE_NAME + " (" +
                                LaunchHistoryColumns._ID + " INTEGER PRIMARY KEY, " +
                                LaunchHistoryColumns.LAST_LAUNCHED + " INTEGER NOT NULL, " +
                                LaunchHistoryColumns.LAUNCH_COUNT + " INTEGER NOT NULL, " +
                                LaunchHistoryColumns.COMPONENT_NAME + " TEXT NOT NULL" +
                                ");"
                );
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Once the db evolves, update here.
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
