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

package com.appsimobile.appsii.module.weather;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Created by Nick on 19/02/14.
 */
public class WeatherProvider extends ContentProvider {

    public static final String AUTHORITY = WeatherContract.AUTHORITY;

    public static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "Weather.db";

    private SQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
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
    public int bulkInsert(Uri uri, ContentValues[] values) {
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
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) sendNotify(uri);

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

    public class WeatherDatabaseHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.

        public WeatherDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE " + WeatherContract.WeatherColumns.TABLE_NAME + " (" +
                                WeatherContract.WeatherColumns._ID +
                                " INTEGER PRIMARY KEY, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_WOEID +
                                " TEXT NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_CITY +
                                " TEXT NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_HUMIDITY +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_PRESSURE +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_RISING +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_ATMOSPHERE_VISIBILITY +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_LAST_UPDATED +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_NOW_CONDITION_CODE +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_NOW_TEMPERATURE +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_SUNRISE +
                                " TEXT NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_SUNSET +
                                " TEXT NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_WIND_CHILL +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_WIND_DIRECTION +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_WIND_SPEED +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_UNIT +
                                " TEXT NOT NULL, " +
                                "UNIQUE (" +
                                WeatherContract.WeatherColumns.COLUMN_NAME_WOEID +
                                ") ON CONFLICT REPLACE" +
                                ");"
                );
                db.execSQL("CREATE TABLE " + WeatherContract.ForecastColumns.TABLE_NAME + " (" +
                                WeatherContract.ForecastColumns._ID +
                                " INTEGER PRIMARY KEY, " +
                                WeatherContract.ForecastColumns.COLUMN_NAME_FORECAST_DAY +
                                " INTEGER NOT NULL, " +
                                WeatherContract.ForecastColumns.COLUMN_NAME_LOCATION_WOEID +
                                " TEXT NOT NULL, " +
                                WeatherContract.ForecastColumns.COLUMN_NAME_TEMPERATURE_LOW +
                                " INTEGER NOT NULL, " +
                                WeatherContract.ForecastColumns.COLUMN_NAME_TEMPERATURE_HIGH +
                                " INTEGER NOT NULL, " +
                                WeatherContract.WeatherColumns.COLUMN_NAME_LAST_UPDATED +
                                " INTEGER NOT NULL, " +
                                WeatherContract.ForecastColumns.COLUMN_NAME_CONDITION_CODE +
                                " INTEGER NOT NULL, " +
                                WeatherContract.ForecastColumns.COLUMN_NAME_UNIT +
                                " TEXT NOT NULL, " +
                                "UNIQUE (" +
                                WeatherContract.ForecastColumns.COLUMN_NAME_FORECAST_DAY + ", " +
                                WeatherContract.ForecastColumns.COLUMN_NAME_LOCATION_WOEID +
                                ") ON CONFLICT REPLACE" +
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
