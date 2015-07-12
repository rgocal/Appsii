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

package com.appsimobile.appsii.module.home.provider;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetHost;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.appwidget.AppsiiAppWidgetHost;

import java.util.Map;

import static com.appsimobile.appsii.module.home.WeatherFragment.PREFERENCE_WEATHER_LOCATION;
import static com.appsimobile.appsii.module.home.WeatherFragment.PREFERENCE_WEATHER_WOEID;
import static com.appsimobile.appsii.module.home.provider.HomeContract.CELLS_TABLE_NAME;
import static com.appsimobile.appsii.module.home.provider.HomeContract.Cells;
import static com.appsimobile.appsii.module.home.provider.HomeContract.HOTSPOTS_TABLE_NAME;
import static com.appsimobile.appsii.module.home.provider.HomeContract
        .HOTSPOT_PAGES_DETAILS_TABLE_NAME;
import static com.appsimobile.appsii.module.home.provider.HomeContract.HOTSPOT_PAGES_TABLE_NAME;
import static com.appsimobile.appsii.module.home.provider.HomeContract.HotspotColumns;
import static com.appsimobile.appsii.module.home.provider.HomeContract.HotspotPageDetailsColumns;
import static com.appsimobile.appsii.module.home.provider.HomeContract.HotspotPagesColumns;
import static com.appsimobile.appsii.module.home.provider.HomeContract.PAGES_TABLE_NAME;
import static com.appsimobile.appsii.module.home.provider.HomeContract.ROWS_TABLE_NAME;

public class HomeContentProvider extends ContentProvider {


    public static final String AUTHORITY = HomeContract.AUTHORITY;

    /**
     * The version of the local database
     */
    public static final int DATABASE_VERSION = 13;

    /**
     * The database in which everything is saved. Do not change this.
     */
    public static final String DATABASE_NAME = "Home.db";

    static final String MATCH_SORT_POSITION =
            "(^" + HomeContract.JoinedRowColumns.ROW_POSITION +
                    ")|( "
                    + HomeContract.JoinedRowColumns.ROW_POSITION + ")";

    static final String MATCH_PAGE_ID =
            "(^" + HomeContract.JoinedPageColumns.PAGE_ID +
                    ")|( "
                    + HomeContract.JoinedPageColumns.PAGE_ID + ")";

    /**
     * The query for the cells table. The cells table joins with the rows list and pages table to
     * add additional information to the cells.
     */
    private static final String CELLS_QUERY_TABLES =
            HomeContract.CELLS_TABLE_NAME +
                    " INNER JOIN " + ROWS_TABLE_NAME +
                    " ON (" + HomeContract.CELLS_TABLE_NAME + "." +
                    HomeContract.CellColumns._ROW_ID +
                    "=" + ROWS_TABLE_NAME + "." + BaseColumns._ID + ")" +
                    " INNER JOIN " + PAGES_TABLE_NAME +
                    " ON (" + ROWS_TABLE_NAME + "." + HomeContract.RowColumns._PAGE_ID +
                    "=" + PAGES_TABLE_NAME + "." + BaseColumns._ID + ")";

    /**
     * The tables to use in the rows query. This query joins with the accounts table.
     */
    private static final String ROWS_QUERY_TABLES =
            ROWS_TABLE_NAME +
                    " INNER JOIN " + PAGES_TABLE_NAME +
                    " ON (" + ROWS_TABLE_NAME + "." + HomeContract.RowColumns._PAGE_ID +
                    "=" + PAGES_TABLE_NAME + "." + BaseColumns._ID + ")";

    /**
     * The tables to use in the rows query. This query joins with the accounts table.
     */
    private static final String HOTSPOTS_QUERY_TABLES =
            HOTSPOTS_TABLE_NAME +
                    " LEFT OUTER JOIN " + PAGES_TABLE_NAME +
                    " ON (" + HOTSPOTS_TABLE_NAME + "." + HotspotColumns._DEFAULT_PAGE +
                    "=" + PAGES_TABLE_NAME + "." + BaseColumns._ID + ")";

    /**
     * The tables to use in the rows query. This query joins with the accounts table.
     */
    private static final String HOTSPOTPAGE_DETAILS_QUERY_TABLES =
            PAGES_TABLE_NAME + " _pt CROSS JOIN " +
                    HOTSPOTS_TABLE_NAME + " _ht";


//            PAGES_TABLE_NAME +
//                    " CROSS JOIN " + HOTSPOT_PAGES_TABLE_NAME + " _ht " +
//                    " ON (" + PAGES_TABLE_NAME + "." + BaseColumns._ID +
//                    "=_ht." + HomeContract.HotspotPages._PAGE_ID + ")";

    /**
     * Query map for the cells query. Maps the columns the user can query (as defined in {@link
     * Cells}) to actual database columns.
     */
    private final static Map<String, String> CELLS_QUERY_MAP = new ArrayMap<>();

    /**
     * Query map for the rows query. Maps the columns the user can query (as defined in {@link
     * com.appsimobile.appsii.module.home.provider.HomeContract.Rows}) to the actual database
     * columns.
     */
    private final static Map<String, String> ROWS_QUERY_MAP = new ArrayMap<>();

    /**
     * Query map for the hotspots query. Maps the columns the user can query (as defined in {@link
     * com.appsimobile.appsii.module.home.provider.HomeContract.Hotspots}) to the actual database
     * columns.
     */
    private final static Map<String, String> HOTSPOTS_QUERY_MAP = new ArrayMap<>();

    /**
     * Query map for the hotspots query. Maps the columns the user can query (as defined in {@link
     * com.appsimobile.appsii.module.home.provider.HomeContract.Hotspots}) to the actual database
     * columns.
     */
    private final static Map<String, String> HOTSPOTPAGE_DETAILS_QUERY_MAP = new ArrayMap<>();

    /**
     * Initializer for the CELLS_QUERY_MAP. Simply initializes the map when the class is loaded
     */
    static {
        // map from user column to db column

        String c = HomeContract.CELLS_TABLE_NAME + ".";
        String r = ROWS_TABLE_NAME + ".";
        String p = PAGES_TABLE_NAME + ".";

        // id is simply the cell id
        CELLS_QUERY_MAP.put(BaseColumns._ID, c + BaseColumns._ID);

        // pages columns
        CELLS_QUERY_MAP.put(HomeContract.JoinedPageColumns.PAGE_ID, p + BaseColumns._ID);
        CELLS_QUERY_MAP.put(HomeContract.JoinedPageColumns.PAGE_NAME,
                p + HomeContract.PageColumns.DISPLAY_NAME);

        // row columns
        CELLS_QUERY_MAP.put(HomeContract.JoinedRowColumns.ROW_HEIGHT,
                r + HomeContract.RowColumns.HEIGHT);
        CELLS_QUERY_MAP.put(HomeContract.JoinedRowColumns.ROW_ID, r + BaseColumns._ID);
        CELLS_QUERY_MAP.put(HomeContract.JoinedRowColumns.ROW_POSITION,
                r + HomeContract.RowColumns.POSITION);

        CELLS_QUERY_MAP.put(HomeContract.CellColumns.POSITION,
                c + HomeContract.CellColumns.POSITION);
        CELLS_QUERY_MAP.put(HomeContract.CellColumns.EFFECT_COLOR,
                c + HomeContract.CellColumns.EFFECT_COLOR);
        CELLS_QUERY_MAP.put(HomeContract.CellColumns.TYPE, c + HomeContract.CellColumns.TYPE);
        CELLS_QUERY_MAP.put(HomeContract.CellColumns.COLSPAN, c + HomeContract.CellColumns.COLSPAN);
    }

    /**
     * Initializer for the ROWS_QUERY_MAP. Simply initializes the map when the class is loaded
     */
    static {
        String r = ROWS_TABLE_NAME + ".";
        String p = PAGES_TABLE_NAME + ".";

        // id is simply the row id
        ROWS_QUERY_MAP.put(BaseColumns._ID, r + BaseColumns._ID);

        // columns from pages
        ROWS_QUERY_MAP.put(HomeContract.JoinedPageColumns.PAGE_ID, p + BaseColumns._ID);
        ROWS_QUERY_MAP.put(HomeContract.JoinedPageColumns.PAGE_NAME,
                p + HomeContract.PageColumns.DISPLAY_NAME);

        // simple 1 on 1 mappings.
        ROWS_QUERY_MAP.put(HomeContract.RowColumns.HEIGHT, r + HomeContract.RowColumns.HEIGHT);
        ROWS_QUERY_MAP.put(HomeContract.RowColumns.POSITION, r + HomeContract.RowColumns.POSITION);

    }

    /**
     * Initializer for the HOTSPOTS_QUERY_MAP. Simply initializes the map when the class is loaded
     */
    static {
        String h = HOTSPOTS_TABLE_NAME + ".";
        String p = PAGES_TABLE_NAME + ".";

        // id is simply the row id
        HOTSPOTS_QUERY_MAP.put(BaseColumns._ID, h + BaseColumns._ID);

        // columns from pages
        HOTSPOTS_QUERY_MAP.put(HomeContract.JoinedDefaultPageColumns.DEFAULT_PAGE_NAME,
                p + HomeContract.PageColumns.DISPLAY_NAME);
        HOTSPOTS_QUERY_MAP.put(HomeContract.JoinedDefaultPageColumns.DEFAULT_PAGE_TYPE,
                p + HomeContract.PageColumns.TYPE);

        // simple 1 on 1 mappings.
        HOTSPOTS_QUERY_MAP.put(HotspotColumns.ALWAYS_OPEN_LAST,
                h + HotspotColumns.ALWAYS_OPEN_LAST);
        HOTSPOTS_QUERY_MAP.put(HotspotColumns._DEFAULT_PAGE, h + HotspotColumns._DEFAULT_PAGE);
        HOTSPOTS_QUERY_MAP.put(HotspotColumns.HEIGHT, h + HotspotColumns.HEIGHT);
        HOTSPOTS_QUERY_MAP.put(HotspotColumns.LEFT_BORDER, h + HotspotColumns.LEFT_BORDER);
        HOTSPOTS_QUERY_MAP.put(HotspotColumns.NAME, h + HotspotColumns.NAME);
        HOTSPOTS_QUERY_MAP.put(HotspotColumns.NEEDS_CONFIGURATION,
                h + HotspotColumns.NEEDS_CONFIGURATION);
        HOTSPOTS_QUERY_MAP.put(HotspotColumns.Y_POSITION, h + HotspotColumns.Y_POSITION);

    }

    /**
     * Initializer for the HOTSPOTS_QUERY_MAP. Simply initializes the map when the class is loaded
     */
    static {
        // the hotspots table is added as an alias, so use this alias
        String h = "_ht.";
        String p = "_pt.";

        // hotspot id id in the hotspots table
        HOTSPOTPAGE_DETAILS_QUERY_MAP.put(HotspotPageDetailsColumns._HOTSPOT_ID,
                h + BaseColumns._ID + " as " + HotspotPageDetailsColumns._HOTSPOT_ID);
        HOTSPOTPAGE_DETAILS_QUERY_MAP.put(HotspotPageDetailsColumns.HOTSPOT_NAME,
                h + HotspotColumns.NAME);

        // exists is a sub-query
        String exitst = "EXISTS (SELECT 1 FROM " + HOTSPOT_PAGES_TABLE_NAME +
                " WHERE " + HotspotPagesColumns._HOTPSOT_ID + "=" + h + BaseColumns._ID +
                " AND " + HotspotPagesColumns._PAGE_ID + "=" + p + BaseColumns._ID +
                ")";

        // position is a sub-query
        String position = "(SELECT " + HotspotPagesColumns.POSITION +
                " FROM " + HOTSPOT_PAGES_TABLE_NAME +
                " WHERE " + HotspotPagesColumns._HOTPSOT_ID + "=" + h + BaseColumns._ID +
                " AND " + HotspotPagesColumns._PAGE_ID + "=" + p + BaseColumns._ID + ")";

        HOTSPOTPAGE_DETAILS_QUERY_MAP.put(HotspotPageDetailsColumns.ENABLED,
                exitst + " as " + HotspotPageDetailsColumns.ENABLED);

        HOTSPOTPAGE_DETAILS_QUERY_MAP.put(HotspotPageDetailsColumns.POSITION,
                position + " as " + HotspotPageDetailsColumns.POSITION);

        // the page id and name comes from the pages table.
        HOTSPOTPAGE_DETAILS_QUERY_MAP.put(HotspotPageDetailsColumns._PAGE_ID,
                p + BaseColumns._ID + " as " + HotspotPageDetailsColumns._PAGE_ID);
        HOTSPOTPAGE_DETAILS_QUERY_MAP.put(HotspotPageDetailsColumns.PAGE_NAME,
                p + HomeContract.PageColumns.DISPLAY_NAME);
        HOTSPOTPAGE_DETAILS_QUERY_MAP.put(HotspotPageDetailsColumns.PAGE_TYPE,
                p + HomeContract.PageColumns.TYPE);

    }

    /**
     * The open helper for the database. It will update/create the db when needed.
     */
    private SQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new HomeDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        selection = fixWhereProjection(uri, selection);
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String tables = createTablesFromTableQuery(args.table);

        qb.setTables(tables);

        Map<String, String> projectionMap = getProjectionMapForTable(args.table);

        if (projectionMap != null) {
            qb.setProjectionMap(projectionMap);
            qb.setStrict(true);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        if (HomeContract.CELLS_TABLE_NAME.equals(args.table)) {
            sortOrder = fixSortOrder(sortOrder);
        }
        String where = args.where;
        if (HomeContract.HOTSPOT_PAGES_DETAILS_TABLE_NAME.equals(args.table)) {
            where = "_ht." + BaseColumns._ID + "=" + ContentUris.parseId(uri);
        }

        Cursor result = qb.query(db, projection, where, args.args, null, null, sortOrder);

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

        checkInsertConstraints(args.table, initialValues);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = db.insert(args.table, null, initialValues);
        if (rowId <= 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);

        return uri;
    }

    private void checkInsertConstraints(String table, ContentValues initialValues) {
        // no special checks needed atm.
    }

    private void sendNotify(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null, false);
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                ContentValues value = values[i];
                if (db.insert(args.table, null, value) < 0) return 0;
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

        try {
            int count = db.delete(args.table, args.where, args.args);

            if (count > 0) sendNotify(uri);
            return count;
        } catch (SQLiteConstraintException e) {
            SQLiteConstraintException ex =
                    new SQLiteConstraintException("Constraint violation on delete of: " + uri);
            ex.initCause(e);
            throw ex;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }

    @Override
    public void shutdown() {
        if (mOpenHelper != null) {
            mOpenHelper.close();
        }
    }

    /**
     * The where projection of the queries to the rows and cells table needs to be changed
     * because the projection is mapped and the id may become ambiguous. This method returns
     * the correct where for the args provided.
     */
    private static String fixWhereProjection(Uri uri, String selection) {
        if (selection == null) return null;
        String table = uri.getPathSegments().get(0);

        switch (table) {
            case HomeContract.CELLS_TABLE_NAME:
                return selection.
                        replaceAll("(^_id)|( _id)", " " + HomeContract.CELLS_TABLE_NAME + "._id");
            case ROWS_TABLE_NAME:
                return selection.
                        replaceAll("(^_id)|( _id)", " " + ROWS_TABLE_NAME + "._id");
        }
        return selection;
    }

    /**
     * Return the actual tables to be queries when the user queries the specified table.
     */
    private static String createTablesFromTableQuery(String queriedTable) {
        if (HomeContract.CELLS_TABLE_NAME.equals(queriedTable)) {
            return CELLS_QUERY_TABLES;
        } else if (ROWS_TABLE_NAME.equals(queriedTable)) {
            return ROWS_QUERY_TABLES;
        } else if (HOTSPOTS_TABLE_NAME.equals(queriedTable)) {
            return HOTSPOTS_QUERY_TABLES;
        } else if (HOTSPOT_PAGES_DETAILS_TABLE_NAME.equals(queriedTable)) {
            return HOTSPOTPAGE_DETAILS_QUERY_TABLES;
        }
        return queriedTable;
    }

    /**
     * Return the projection map for a query on the given table.
     */
    private static Map<String, String> getProjectionMapForTable(String queriedTable) {
        if (HomeContract.CELLS_TABLE_NAME.equals(queriedTable)) {
            return CELLS_QUERY_MAP;
        } else if (ROWS_TABLE_NAME.equals(queriedTable)) {
            return ROWS_QUERY_MAP;
        } else if (HOTSPOTS_TABLE_NAME.equals(queriedTable)) {
            return HOTSPOTS_QUERY_MAP;
        } else if (HOTSPOT_PAGES_DETAILS_TABLE_NAME.equals(queriedTable)) {
            return HOTSPOTPAGE_DETAILS_QUERY_MAP;
        }
        return null;
    }

    private static String fixSortOrder(String sortOrder) {
        if (sortOrder == null) return null;


        return sortOrder.
                replaceAll(MATCH_SORT_POSITION,
                        ROWS_TABLE_NAME + "." + HomeContract.RowColumns.POSITION).
                replaceAll(MATCH_PAGE_ID, PAGES_TABLE_NAME + "._id");

    }

    /**
     * The sqliteOpenHelper for the Home database. This class is responsible for upgrading and
     * creating the database when needed. Changes to the structure must be added to the {@link
     * #onCreate} method and they have to be added to the
     * {@link #onUpgrade(SQLiteDatabase, int, int)} method as well to allow seamless upgrades to
     * the database.
     */
    public static class HomeDatabaseHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.

        static final String CREATE_PAGES = "CREATE TABLE " + PAGES_TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                HomeContract.PageColumns.TYPE + " INTEGER NOT NULL DEFAULT " +
                HomeContract.Pages.PAGE_HOME + ", " +
                HomeContract.PageColumns.DISPLAY_NAME + " TEXT " +
                ");";

        static final String CREATE_ROWS = "CREATE TABLE " + ROWS_TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                HomeContract.RowColumns._PAGE_ID + " INTEGER NOT NULL, " +
                HomeContract.RowColumns.HEIGHT + " INTEGER, " +
                HomeContract.RowColumns.POSITION + " INTEGER, " +
                " FOREIGN KEY (" + HomeContract.RowColumns._PAGE_ID +
                ") REFERENCES " +
                PAGES_TABLE_NAME + " (" + BaseColumns._ID + ") ON DELETE CASCADE" +
                ");";

        static final String CREATE_CELLS = "CREATE TABLE " + HomeContract.CELLS_TABLE_NAME + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                HomeContract.CellColumns._ROW_ID + " INTEGER NOT NULL, " +
                HomeContract.CellColumns.COLSPAN + " INTEGER NOT NULL DEFAULT 1, " +
                HomeContract.CellColumns.TYPE + " INTEGER NOT NULL, " +
                HomeContract.CellColumns.EFFECT_COLOR + " INTEGER NOT NULL DEFAULT " +
                String.valueOf(Color.TRANSPARENT) + ", " +
                HomeContract.CellColumns.POSITION + " INTEGER NOT NULL, " +
                " FOREIGN KEY (" + HomeContract.CellColumns._ROW_ID +
                ") REFERENCES " + ROWS_TABLE_NAME +
                " (" + BaseColumns._ID + ") ON DELETE CASCADE" +
                ");";

        static final String CREATE_CONFIG =
                "CREATE TABLE " + HomeContract.CONFIG_TABLE_NAME + " (" +
                        // The id in our local db
                        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        HomeContract.ConfigurationColumns.KEY + " TEXT , " +
                        HomeContract.ConfigurationColumns.VALUE + " TEXT , " +
                        HomeContract.ConfigurationColumns._CELL_ID + " INTEGER NOT NULL ," +
                        "UNIQUE(" + HomeContract.ConfigurationColumns.KEY + "," +
                        HomeContract.ConfigurationColumns._CELL_ID + ") ON CONFLICT REPLACE, " +

                        " FOREIGN KEY (" + HomeContract.ConfigurationColumns._CELL_ID +
                        ") REFERENCES " + HomeContract.CELLS_TABLE_NAME +
                        " (" + BaseColumns._ID + ") ON DELETE CASCADE" +
                        ");";

        static final String CREATE_HOTSPOTS = "CREATE TABLE " + HOTSPOTS_TABLE_NAME + "( " +
                BaseColumns._ID + " INTEGER PRIMARY KEY, " +
                HotspotColumns.HEIGHT + " FLOAT," +
                HotspotColumns.Y_POSITION + " FLOAT," +
                HotspotColumns.NEEDS_CONFIGURATION + " INTEGER," +
                HotspotColumns.NAME + " TEXT, " +
                HotspotColumns.LEFT_BORDER + " INTEGER," +
                HotspotColumns.ALWAYS_OPEN_LAST + " INTEGER," +
                HotspotColumns._DEFAULT_PAGE + " INTEGER, " +
                " FOREIGN KEY (" + HotspotColumns._DEFAULT_PAGE +
                ") REFERENCES " +
                PAGES_TABLE_NAME + " (" + BaseColumns._ID + ") ON DELETE SET NULL" +
                ");";


        static final String CREATE_HOTSPOT_PAGES =
                "CREATE TABLE " + HOTSPOT_PAGES_TABLE_NAME + "( " +
                        BaseColumns._ID + " INTEGER PRIMARY KEY, " +
                        HotspotPagesColumns._HOTPSOT_ID + " INTEGER NOT NULL," +
                        HotspotPagesColumns._PAGE_ID + " INTEGER NOT NULL," +
                        HotspotPagesColumns.POSITION + " INTEGER NOT NULL," +

                        " FOREIGN KEY (" + HotspotPagesColumns._PAGE_ID +
                        ") REFERENCES " +
                        PAGES_TABLE_NAME + " (" + BaseColumns._ID + ") ON DELETE CASCADE," +

                        " FOREIGN KEY (" + HotspotPagesColumns._HOTPSOT_ID +
                        ") REFERENCES " +
                        HomeContract.HOTSPOTS_TABLE_NAME + " (" + BaseColumns._ID +
                        ") ON DELETE CASCADE," +
                        "UNIQUE(" + HotspotPagesColumns._HOTPSOT_ID + "," +
                        HotspotPagesColumns._PAGE_ID + ") ON CONFLICT REPLACE " +
                        ");";


        private final Context mContext;


        public HomeDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public void onConfigure(SQLiteDatabase db) {
            super.onConfigure(db);
            db.setForeignKeyConstraintsEnabled(true);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            AppWidgetHost appWidgetHost = new AppsiiAppWidgetHost(mContext,
                    AppsiApplication.APPWIDGET_HOST_ID);

            appWidgetHost.deleteHost();

            db.beginTransaction();
            try {

                // create the pages table
                db.execSQL(CREATE_PAGES);

                // create the rows table
                db.execSQL(CREATE_ROWS);

                // create the cells table
                db.execSQL(CREATE_CELLS);

                // create the config table
                db.execSQL(CREATE_CONFIG);

                // create the hotspots
                db.execSQL(CREATE_HOTSPOTS);

                // create the hotspots/pages link
                db.execSQL(CREATE_HOTSPOT_PAGES);

                // insert default values
                insertDefaultValuesV11(db);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }

        private void insertDefaultValuesV11(SQLiteDatabase db) {
            String homePageName = mContext.getString(R.string.home_screen_name);
            String appsPageName = mContext.getString(R.string.apps_page_name);
            String agendaPageName = mContext.getString(R.string.agenda_page_name);
            String peoplePageName = mContext.getString(R.string.people_page_name);
            String callsPageName = mContext.getString(R.string.calls_page_name);
            String searchPageName = mContext.getString(R.string.search_page_name);

            // insert the default, home page
            ContentValues v = new ContentValues();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, homePageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_HOME);
            long homePageId = db.insert(PAGES_TABLE_NAME, null, v);

            v.clear();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, appsPageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_APPS);
            long appsPageId = db.insert(PAGES_TABLE_NAME, null, v);

            v.clear();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, agendaPageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_AGENDA);
            long agendaPageId = db.insert(PAGES_TABLE_NAME, null, v);

            v.clear();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, peoplePageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_PEOPLE);
            long peoplePageId = db.insert(PAGES_TABLE_NAME, null, v);

            v.clear();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, callsPageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_CALLS);
            long callsPageId = db.insert(PAGES_TABLE_NAME, null, v);

            v.clear();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, searchPageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_SEARCH);
            long searchPageId = db.insert(PAGES_TABLE_NAME, null, v);


            long defaultHotspotId = insertDefaultHotspotsV7(v, db);


            insertDefaultHomePageValuesV7(db, v, homePageId);
        }

        private static long insertDefaultHotspotsV7(ContentValues values, SQLiteDatabase db) {

            values.clear();
            values.put(HotspotColumns.HEIGHT, .1f);
            values.put(HotspotColumns.Y_POSITION, .15f);
            values.put(HotspotColumns.NEEDS_CONFIGURATION, 0);
            values.put(HotspotColumns.NAME, "Appsii");
            values.put(HotspotColumns.LEFT_BORDER, 1);
            values.put(HotspotColumns.ALWAYS_OPEN_LAST, 1);
            values.putNull(HotspotColumns._DEFAULT_PAGE);
            return db.insert(HOTSPOTS_TABLE_NAME, null, values);

        }

        private void insertDefaultHomePageValuesV7(SQLiteDatabase db, ContentValues v,
                long homePageId) {
            // insert the default rows
            v.clear();
            v.put(HomeContract.RowColumns._PAGE_ID, homePageId);
            v.put(HomeContract.RowColumns.HEIGHT, 2);
            v.put(HomeContract.RowColumns.POSITION, 0);
            long idRowHeader = db.insert(ROWS_TABLE_NAME, null, v);
            v.clear();
            v.put(HomeContract.RowColumns._PAGE_ID, homePageId);
            v.put(HomeContract.RowColumns.HEIGHT, 1);
            v.put(HomeContract.RowColumns.POSITION, 1);
            long idRow0 = db.insert(ROWS_TABLE_NAME, null, v);
            v.clear();
            v.put(HomeContract.RowColumns._PAGE_ID, homePageId);
            v.put(HomeContract.RowColumns.HEIGHT, 1);
            v.put(HomeContract.RowColumns.POSITION, 2);
            long idRow1 = db.insert(ROWS_TABLE_NAME, null, v);
            v.clear();
            v.put(HomeContract.RowColumns._PAGE_ID, homePageId);
            v.put(HomeContract.RowColumns.HEIGHT, 1);
            v.put(HomeContract.RowColumns.POSITION, 3);
            long idRow2 = db.insert(ROWS_TABLE_NAME, null, v);
            v.clear();
            v.put(HomeContract.RowColumns._PAGE_ID, homePageId);
            v.put(HomeContract.RowColumns.HEIGHT, 1);
            v.put(HomeContract.RowColumns.POSITION, 4);
            long idRow3 = db.insert(ROWS_TABLE_NAME, null, v);

            // insert data into row 0
            v.clear();

            insertCell(db, v, idRow0, 1, 0, Cells.DISPLAY_TYPE_WEATHER_TEMP);
            insertCell(db, v, idRow0, 2, 1, Cells.DISPLAY_TYPE_WEATHER_SUNRISE);

            // data for row 1
            insertCell(db, v, idRow1, 1, 0, Cells.DISPLAY_TYPE_WEATHER_WIND);
            long windIstanbul = insertCell(db, v, idRow1, 1, 1, Cells.DISPLAY_TYPE_WEATHER_WIND);

            // data for row 2
            long cameraId = insertCell(db, v, idRow2, 1, 0, Cells.DISPLAY_TYPE_INTENT);
            long bluetoothId = insertCell(db, v, idRow2, 1, 1, Cells.DISPLAY_TYPE_BLUETOOTH_TOGGLE);
            long musicId = insertCell(db, v, idRow2, 1, 2, Cells.DISPLAY_TYPE_INTENT);

            // data for row 3
            long clockParisId = insertCell(db, v, idRow3, 1, 0, Cells.DISPLAY_TYPE_CLOCK);
            long clockLondonId = insertCell(db, v, idRow3, 1, 1, Cells.DISPLAY_TYPE_CLOCK);
            long clockNewYorkId = insertCell(db, v, idRow3, 1, 2, Cells.DISPLAY_TYPE_CLOCK);
            long clockTokyoId = insertCell(db, v, idRow3, 1, 3, Cells.DISPLAY_TYPE_CLOCK);

            // insert header row data
            insertCell(db, v, idRowHeader, 1, 0, Cells.DISPLAY_TYPE_PROFILE_IMAGE);

            // Apply some config values
            insertKeyValue(db, v, clockParisId, "timezone_id", "Europe/Berlin");
            insertKeyValue(db, v, clockParisId, "title", "Berlin");

            insertKeyValue(db, v, clockLondonId, "timezone_id", "europe/London");
            insertKeyValue(db, v, clockLondonId, "title", "London");

            insertKeyValue(db, v, clockNewYorkId, "timezone_id", "America/New_York");
            insertKeyValue(db, v, clockNewYorkId, "title", "New York");

            insertKeyValue(db, v, windIstanbul, PREFERENCE_WEATHER_WOEID, "2344116");
            insertKeyValue(db, v, windIstanbul, PREFERENCE_WEATHER_LOCATION, "Instanbul");

            insertKeyValue(db, v, clockTokyoId, "timezone_id", "Asia/Tokyo");
            insertKeyValue(db, v, clockTokyoId, "title", "Tokyo");

            insertKeyValue(db, v, musicId, "action", Intent.ACTION_MAIN);
            insertKeyValue(db, v, musicId, "category", Intent.CATEGORY_APP_MUSIC);
            insertKeyValue(db, v, musicId, "icon", "play_music");
            insertKeyValue(db, v, musicId, "title", "Music");

            insertKeyValue(db, v, cameraId, "action",
                    MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            insertKeyValue(db, v, cameraId, "category", Intent.CATEGORY_DEFAULT);
            insertKeyValue(db, v, cameraId, "icon", "camera");
            insertKeyValue(db, v, cameraId, "title", "Camera");
        }

        private long insertCell(SQLiteDatabase db, ContentValues vals, long id, int colspan,
                int position, int type) {
            vals.clear();
            vals.put(HomeContract.CellColumns._ROW_ID, id);
            vals.put(HomeContract.CellColumns.COLSPAN, colspan);
            vals.put(HomeContract.CellColumns.POSITION, position);
            vals.put(HomeContract.CellColumns.TYPE, type);
            return db.insert(HomeContract.CELLS_TABLE_NAME, null, vals);
        }

        private static void insertKeyValue(SQLiteDatabase db, ContentValues vals, long cellId,
                String key, String value) {
            vals.clear();
            vals.put(HomeContract.ConfigurationColumns._CELL_ID, cellId);
            vals.put(HomeContract.ConfigurationColumns.KEY, key);
            vals.put(HomeContract.ConfigurationColumns.VALUE, value);
            db.insert(HomeContract.CONFIG_TABLE_NAME, null, vals);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Once the db evolves, update here.
            // version 2 through 6 are internal, upgrade the db by dropping the old db.
            // 7 needs a full refresh because a lot of changes where done to the model
            if (oldVersion < 7) {
                db.beginTransaction();
                try {
                    db.execSQL("drop table " + HomeContract.CONFIG_TABLE_NAME + ";");
                    db.execSQL("drop table " + HomeContract.CELLS_TABLE_NAME + ";");
                    db.execSQL("drop table " + HomeContract.ROWS_TABLE_NAME + ";");
                    db.execSQL("drop table " + HomeContract.PAGES_TABLE_NAME + ";");

                    // create the pages table
                    db.execSQL(CREATE_PAGES);

                    // create the rows table
                    db.execSQL(CREATE_ROWS);

                    // create the cells table
                    db.execSQL(CREATE_CELLS);

                    // create the config table
                    db.execSQL(CREATE_CONFIG);

                    // create the hotspots
                    db.execSQL(CREATE_HOTSPOTS);

                    // create the hotspots/pages link
                    db.execSQL(CREATE_HOTSPOT_PAGES);

                    // insert default values
                    insertDefaultValuesV7(db);

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                oldVersion = 7;
            }
            // Version 8 & 9 are a new full drop and recreate
            if (oldVersion < 9) {
                db.beginTransaction();
                try {
                    db.execSQL("drop table " + HomeContract.HOTSPOT_PAGES_TABLE_NAME + ";");
                    db.execSQL("drop table " + HomeContract.HOTSPOTS_TABLE_NAME + ";");
                    db.execSQL("drop table " + HomeContract.CONFIG_TABLE_NAME + ";");
                    db.execSQL("drop table " + HomeContract.CELLS_TABLE_NAME + ";");
                    db.execSQL("drop table " + HomeContract.ROWS_TABLE_NAME + ";");
                    db.execSQL("drop table " + HomeContract.PAGES_TABLE_NAME + ";");

                    // create the pages table
                    db.execSQL(CREATE_PAGES);

                    // create the rows table
                    db.execSQL(CREATE_ROWS);

                    // create the cells table
                    db.execSQL(CREATE_CELLS);

                    // create the config table
                    db.execSQL(CREATE_CONFIG);

                    // create the hotspots
                    db.execSQL(CREATE_HOTSPOTS);

                    // create the hotspots/pages link
                    db.execSQL(CREATE_HOTSPOT_PAGES);

                    // insert default values
                    insertDefaultValuesV7(db);

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                oldVersion = 9;
            }
            if (oldVersion < 10) {
                // This version added additional delete actions to foreign key constraints:
                // Hotspots.Default_page_id -> ON DELETE SET NULL
                // Config.cell_id -> ON DELETE CASCADE
                // HotspotPages.hotspot_id -> ON DELETE CASCADE
                // HotspotPages.page_id -> ON DELETE CASCADE
                // Updrade strategy:
                // 1. rename all tables
                // 2. create tables with the additional actions
                // 3. move data and delete temp tables
                db.beginTransaction();
                try {

                    // rename the old tables, with the old constraints
                    db.execSQL(
                            "ALTER TABLE " + HomeContract.HOTSPOT_PAGES_TABLE_NAME +
                                    " RENAME TO A_HP;");
                    db.execSQL(
                            "ALTER TABLE " + HomeContract.HOTSPOTS_TABLE_NAME + " RENAME TO A_HS;");
                    db.execSQL(
                            "ALTER TABLE " + HomeContract.CONFIG_TABLE_NAME + " RENAME TO A_CF;");
                    db.execSQL("ALTER TABLE " + HomeContract.CELLS_TABLE_NAME + " RENAME TO A_CL;");
                    db.execSQL("ALTER TABLE " + HomeContract.ROWS_TABLE_NAME + " RENAME TO A_RW;");
                    db.execSQL("ALTER TABLE " + HomeContract.PAGES_TABLE_NAME + " RENAME TO A_PG;");

                    // now recreate the tables; with the new constraints
                    // create the pages table
                    db.execSQL(CREATE_PAGES);

                    // create the rows table
                    db.execSQL(CREATE_ROWS);

                    // create the cells table
                    db.execSQL(CREATE_CELLS);

                    // create the config table
                    db.execSQL(CREATE_CONFIG);

                    // create the hotspots
                    db.execSQL(CREATE_HOTSPOTS);

                    // create the hotspots/pages link
                    db.execSQL(CREATE_HOTSPOT_PAGES);

                    // transfer data for pages table
                    db.execSQL("INSERT INTO " + HomeContract.PAGES_TABLE_NAME + "(" +
                            BaseColumns._ID + ", " +
                            HomeContract.PageColumns.TYPE + ", " +
                            HomeContract.PageColumns.DISPLAY_NAME + ")" +
                            " SELECT " +
                            BaseColumns._ID + ", " +
                            HomeContract.PageColumns.TYPE + ", " +
                            HomeContract.PageColumns.DISPLAY_NAME +
                            " FROM A_PG");

                    // transfer data for rows table
                    db.execSQL("INSERT INTO " + HomeContract.ROWS_TABLE_NAME + "(" +
                            BaseColumns._ID + ", " +
                            HomeContract.RowColumns._PAGE_ID + ", " +
                            HomeContract.RowColumns.HEIGHT + ", " +
                            HomeContract.RowColumns.POSITION +
                            ")" +
                            " SELECT " +
                            BaseColumns._ID + ", " +
                            HomeContract.RowColumns._PAGE_ID + ", " +
                            HomeContract.RowColumns.HEIGHT + ", " +
                            HomeContract.RowColumns.POSITION +
                            " FROM A_RW");

                    // transfer data for cells table
                    db.execSQL("INSERT INTO " + HomeContract.CELLS_TABLE_NAME + "(" +
                            BaseColumns._ID + ", " +
                            HomeContract.CellColumns._ROW_ID + ", " +
                            HomeContract.CellColumns.COLSPAN + ", " +
                            HomeContract.CellColumns.TYPE + ", " +
                            HomeContract.CellColumns.POSITION +
                            ")" +
                            " SELECT " +
                            BaseColumns._ID + ", " +
                            HomeContract.CellColumns._ROW_ID + ", " +
                            HomeContract.CellColumns.COLSPAN + ", " +
                            HomeContract.CellColumns.TYPE + ", " +
                            HomeContract.CellColumns.POSITION +
                            " FROM A_CL");


                    // transfer data for config table
                    db.execSQL("INSERT INTO " + HomeContract.CONFIG_TABLE_NAME + "(" +
                            BaseColumns._ID + ", " +
                            HomeContract.ConfigurationColumns.KEY + ", " +
                            HomeContract.ConfigurationColumns.VALUE + ", " +
                            HomeContract.ConfigurationColumns._CELL_ID +
                            ")" +
                            " SELECT " +
                            BaseColumns._ID + ", " +
                            HomeContract.ConfigurationColumns.KEY + ", " +
                            HomeContract.ConfigurationColumns.VALUE + ", " +
                            HomeContract.ConfigurationColumns._CELL_ID +
                            " FROM A_CF");


                    // transfer data for hotspots table
                    db.execSQL("INSERT INTO " + HomeContract.HOTSPOTS_TABLE_NAME + "(" +
                            BaseColumns._ID + ", " +
                            HotspotColumns.HEIGHT + ", " +
                            HotspotColumns.Y_POSITION + ", " +
                            HotspotColumns.NEEDS_CONFIGURATION + ", " +
                            HotspotColumns.NAME + ", " +
                            HotspotColumns.LEFT_BORDER + ", " +
                            HotspotColumns.ALWAYS_OPEN_LAST + ", " +
                            HotspotColumns._DEFAULT_PAGE +
                            ")" +
                            " SELECT " +
                            BaseColumns._ID + ", " +
                            HotspotColumns.HEIGHT + ", " +
                            HotspotColumns.Y_POSITION + ", " +
                            HotspotColumns.NEEDS_CONFIGURATION + ", " +
                            HotspotColumns.NAME + ", " +
                            HotspotColumns.LEFT_BORDER + ", " +
                            HotspotColumns.ALWAYS_OPEN_LAST + ", " +
                            HotspotColumns._DEFAULT_PAGE +
                            " FROM A_HS");


                    // transfer data for hotspot-pages table
                    db.execSQL("INSERT INTO " + HomeContract.HOTSPOT_PAGES_TABLE_NAME + "(" +
                            BaseColumns._ID + ", " +
                            HotspotPagesColumns._HOTPSOT_ID + ", " +
                            HotspotPagesColumns._PAGE_ID + ", " +
                            HotspotPagesColumns.POSITION +
                            ")" +
                            " SELECT " +
                            BaseColumns._ID + ", " +
                            HotspotPagesColumns._HOTPSOT_ID + ", " +
                            HotspotPagesColumns._PAGE_ID + ", " +
                            HotspotPagesColumns.POSITION +
                            " FROM A_HP");

                    db.execSQL("DROP TABLE A_HP");
                    db.execSQL("DROP TABLE A_HS");
                    db.execSQL("DROP TABLE A_CF");
                    db.execSQL("DROP TABLE A_CL");
                    db.execSQL("DROP TABLE A_RW");
                    db.execSQL("DROP TABLE A_PG");

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }


                oldVersion = 10;
            }

            // Insert the search page into the pages table
            if (oldVersion < 11) {

                db.beginTransaction();

                // get the name of the search page.
                String searchPageName = mContext.getString(R.string.search_page_name);
                ContentValues v = new ContentValues();

                try {

                    // insert the search page
                    v.clear();
                    v.put(HomeContract.PageColumns.DISPLAY_NAME, searchPageName);
                    v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_SEARCH);
                    long searchPageId = db.insert(PAGES_TABLE_NAME, null, v);

                    // add it to all of the hotspots. First query the hotspots that are available
                    Cursor c = db.query(HOTSPOTS_TABLE_NAME,
                            new String[]{HomeContract.Hotspots._ID},
                            null,
                            null,
                            null,
                            null,
                            null);
                    while (c.moveToNext()) {
                        long hotspotId = c.getLong(0);

                        v.clear();
                        v.put(HomeContract.HotspotPages._HOTPSOT_ID, hotspotId);
                        v.put(HomeContract.HotspotPages._PAGE_ID, searchPageId);
                        v.put(HomeContract.HotspotPages.POSITION, 12);
                        db.insert(HOTSPOT_PAGES_TABLE_NAME, null, v);
                    }
                    c.close();
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                oldVersion = 11;
            }

            // Update the name of the search-page
            if (oldVersion < 12) {

                db.beginTransaction();

                try {
                    // get the name of the search page.
                    String searchPageName = mContext.getString(R.string.search_page_name);
                    ContentValues v = new ContentValues();

                    v.put(HomeContract.PageColumns.DISPLAY_NAME, searchPageName);

                    // execute the update.
                    db.update(PAGES_TABLE_NAME,
                            v,
                            HomeContract.PageColumns.TYPE + "=?",
                            new String[]{String.valueOf(HomeContract.Pages.PAGE_SEARCH)});

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                oldVersion = 12;
            }

            // Add color column to cells
            if (oldVersion < 13) {

                db.beginTransaction();

                try {
                    db.execSQL("ALTER TABLE " + CELLS_TABLE_NAME + " ADD COLUMN " +
                            HomeContract.CellColumns.EFFECT_COLOR + " INTEGER NOT NULL " +
                            "DEFAULT " + String.valueOf(Color.TRANSPARENT) + ";");

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                oldVersion = 13;
            }

        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }

        private void insertDefaultValuesV7(SQLiteDatabase db) {
            String homePageName = mContext.getString(R.string.home_screen_name);
            String appsPageName = mContext.getString(R.string.apps_page_name);
            String agendaPageName = mContext.getString(R.string.agenda_page_name);
            String peoplePageName = mContext.getString(R.string.people_page_name);
            String callsPageName = mContext.getString(R.string.calls_page_name);

            // insert the default, home page
            ContentValues v = new ContentValues();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, homePageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_HOME);
            long homePageId = db.insert(PAGES_TABLE_NAME, null, v);

            v.clear();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, appsPageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_APPS);
            long appsPageId = db.insert(PAGES_TABLE_NAME, null, v);

            v.clear();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, agendaPageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_AGENDA);
            long agendaPageId = db.insert(PAGES_TABLE_NAME, null, v);

            v.clear();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, peoplePageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_PEOPLE);
            long peoplePageId = db.insert(PAGES_TABLE_NAME, null, v);

            v.clear();
            v.put(HomeContract.PageColumns.DISPLAY_NAME, callsPageName);
            v.put(HomeContract.PageColumns.TYPE, HomeContract.Pages.PAGE_CALLS);
            long callsPageId = db.insert(PAGES_TABLE_NAME, null, v);

            insertDefaultHomePageValuesV7(db, v, homePageId);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                if (!db.isReadOnly()) {
                    // Enable foreign key constraints
                    db.execSQL("PRAGMA foreign_keys=ON;");
                }
            }
        }
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
}
