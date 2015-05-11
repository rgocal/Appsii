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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.appsimobile.appsii.BuildConfig;

/**
 * Created by Nick Martens on 8/5/13.
 */
public interface HomeContract extends BaseColumns {

    /**
     * This authority is used for writing to or querying from the home provider. Note: This is set
     * at first run and cannot be changed without breaking apps that access the provider.
     */
    String AUTHORITY = BuildConfig.APPLICATION_ID + ".home";

    /**
     * The content:// style URL for the top-level Home authority
     */
    Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    String CELLS_TABLE_NAME = "cells";

    String ROWS_TABLE_NAME = "rows";

    String PAGES_TABLE_NAME = "pages";

    String CONFIG_TABLE_NAME = "conf";

    String HOTSPOTS_TABLE_NAME = "hotspots";

    String HOTSPOT_PAGES_TABLE_NAME = "hotspot_pages";

    String HOTSPOT_PAGES_DETAILS_TABLE_NAME = "hotspot_pages_details";


    interface PageColumns {

        /**
         * The account name Type: String
         */
        String DISPLAY_NAME = "title";

        /**
         * The type of the page. Home, apps etc. Type: int
         */
        String TYPE = "type";

    }

    interface RowColumns {

        /**
         * The account id for this task list Type: long
         */
        String _PAGE_ID = "_page_id";

        /**
         * The height of the row: int (custom dimension)
         */
        String HEIGHT = "height";

        /**
         * The position of the row in its parent page: int
         */
        String POSITION = "color";

    }

    interface JoinedRowColumns {

        /**
         * The row id for this entity Type: long
         */
        String ROW_ID = "_row_id";

        /**
         * The height of the row: int (custom dimension)
         */
        String ROW_HEIGHT = "row_height";

        /**
         * The position of the row in its parent page: int
         */
        String ROW_POSITION = "row_position";


    }

    interface JoinedPageColumns {

        /**
         * The page name for this entity Type: String
         */
        String PAGE_NAME = "page_name";
        /**
         * The page id for this entity Type: String
         */
        String PAGE_ID = "page_id";

    }

    interface JoinedDefaultPageColumns {

        /**
         * The account id for this entity Type: String
         */
        String DEFAULT_PAGE_NAME = "default_page_name";
        String DEFAULT_PAGE_TYPE = "default_page_type";

    }


    interface CellColumns {

        /**
         * the id of the row this cell belongs to Type: long
         */
        String _ROW_ID = "_row_id";

        /**
         * the number of columns to span. Type: int.
         */
        String COLSPAN = "span";

        /**
         * The position of the cell among its siblings under the same parent row:
         * String
         */
        String POSITION = "position";

        /**
         * The type of the cell
         */
        String TYPE = "type";

        /**
         * The type of the cell
         */
        String EFFECT_COLOR = "effectColor";

    }

    interface ConfigurationColumns {

        /**
         * the id of the row this cell belongs to Type: long
         */
        String _CELL_ID = "_cell_id";

        /**
         * the id of the page this cell belongs to Type: long
         */
        String KEY = "key";

        /**
         * the number of columns to span. Type: int.
         */
        String VALUE = "value";

    }

    /**
     * Defines the link between the pages to show and the hotspots
     */
    interface HotspotPagesColumns {


        /**
         * The Id of the hotspot. type: long
         */
        String _HOTPSOT_ID = "_hotspot_id";

        /**
         * The Id of the page. type: long
         */
        String _PAGE_ID = "_page_id";

        String POSITION = "position";
    }

    /**
     * A virtual table holding a special join between pages and hotspots, to query the
     * enabled pages of an hotspot in a simple way
     */
    interface HotspotPageDetailsColumns {

        String ENABLED = "page_enabled";
        /**
         * The Id of the page. type: long
         */
        String _PAGE_ID = "_page_id";

        /**
         * The Id of the page. type: long
         */
        String _HOTSPOT_ID = "_hotspot_id";

        String POSITION = "position";

        /**
         * The page name for this entity Type: String
         */
        String PAGE_NAME = "page_name";

        /**
         * The page type for this entity. Type: Int
         */
        String PAGE_TYPE = "page_type";

        /**
         * The page name for this entity Type: String
         */
        String HOTSPOT_NAME = "hotspot_name";

    }

    /**
     * Defines the hotspot columns
     */
    interface HotspotColumns {


        /**
         * The id of the default page. Long
         */
        String _DEFAULT_PAGE = "_default_page_id";

        /**
         * The y position of this hotspot. Type float (screen percentage)
         */
        String Y_POSITION = "y";

        /**
         * The y position of the hotspot. Type float (screen percentage)
         */
        String HEIGHT = "height";

        /**
         * The edge on which the hotspot is added. Left=1, Right=0
         */
        String LEFT_BORDER = "left";

        /**
         * True (1) when this hotspot has not yet been set up completely: boolean
         */
        String NEEDS_CONFIGURATION = "needs_config";

        /**
         * The name of the hotspot. Type: String
         */
        String NAME = "name";

        /**
         * The value of the property to always open the last opened hotspot. Type: boolean
         */
        String ALWAYS_OPEN_LAST = "alwaysOpenLast";

        String[] PROJECTION_ALL = new String[]{
                _ID, NAME, _DEFAULT_PAGE, Y_POSITION, HEIGHT, LEFT_BORDER, NEEDS_CONFIGURATION,
                ALWAYS_OPEN_LAST
        };

    }


    /**
     * Represents the columns of a cell
     */
    final class Cells
            implements BaseColumns, CellColumns, JoinedRowColumns, JoinedPageColumns {

        /**
         * The component type has not yet been set
         */
        public static final int DISPLAY_TYPE_UNSET = 0;

        /**
         * The component type is a widget
         */
        public static final int DISPLAY_TYPE_WIDGET = 1;

        /**
         * The component is a welcome appsii-widget
         */
        public static final int DISPLAY_TYPE_INTERNAL_WELCOME = 3;

        /**
         * The component is a clock
         */
        public static final int DISPLAY_TYPE_CLOCK = 4;

        /**
         * The component is an unread-count item
         */
        public static final int DISPLAY_TYPE_UNREAD_COUNT = 5;

        /**
         * The component displays the user's profile image
         */
        public static final int DISPLAY_TYPE_PROFILE_IMAGE = 6;

        /**
         * The component displays the current weather
         */
        public static final int DISPLAY_TYPE_WEATHER_TEMP = 7;

        /**
         * The component displays the current weather with wallpaper
         */
        public static final int DISPLAY_TYPE_WEATHER_TEMP_WALLPAPER = 16;

        /**
         * The component displays the current wind status
         */
        public static final int DISPLAY_TYPE_WEATHER_WIND = 8;

        /**
         * The component displays the current wind status with wallpaper
         */
        public static final int DISPLAY_TYPE_WEATHER_WIND_WALLPAPER = 17;

        /**
         * The component displays the user's profile image
         */
        public static final int DISPLAY_TYPE_WIFI = 9;

        /**
         * The component displays the user's profile image
         */
        public static final int DISPLAY_TYPE_CUSTOM_IMAGE = 10;

        /**
         * The component displays the user's profile image
         */
        public static final int DISPLAY_TYPE_APP_WIDGET = 11;

        /**
         * The component displays the user's profile image
         */
        public static final int DISPLAY_TYPE_INTENT = 12;

        /**
         * The component displays the user's profile image
         */
        public static final int DISPLAY_TYPE_BLUETOOTH_TOGGLE = 13;

        /**
         * The component displays the sunrise and sunset
         */
        public static final int DISPLAY_TYPE_WEATHER_SUNRISE = 14;

        /**
         * The component displays the sunrise and sunset with
         * wallpaer on the background
         */
        public static final int DISPLAY_TYPE_WEATHER_SUNRISE_WALLPAPER = 15;

        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + CELLS_TABLE_NAME);

        private static final String QUERY_WHERE =
                JoinedPageColumns.PAGE_ID + "=?";

        /**
         * Query the content provider for a list of tasks available in the given task list. The
         * result will not include any sub-tasks. Tasks with all statuses are returned.
         */
        public static Cursor query(ContentResolver contentResolver, long pageId,
                String[] projection) {
            return contentResolver.query(CONTENT_URI,
                    projection,
                    QUERY_WHERE,
                    new String[]{String.valueOf(pageId)},
                    POSITION);
        }


    }

    final class Rows
            implements BaseColumns, RowColumns, JoinedPageColumns {

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" +
                ROWS_TABLE_NAME);

        static final String WHERE = JoinedPageColumns.PAGE_ID + "=?";

        /**
         * Query the content provider for a list of available tasklists.
         */
        public static Cursor query(ContentResolver contentResolver, long pageId,
                String[] projection) {
            return contentResolver.query(CONTENT_URI,
                    projection,
                    WHERE,
                    new String[]{String.valueOf(pageId)},
                    null);
        }


    }

    /**
     * Represents the pages that are available to the user. If the user has no access to a
     * certain page, it will be removes from this table. When the user purchases a page,
     * it is added to this page.
     */
    final class Pages implements BaseColumns, PageColumns {

        public static final int PAGE_HOME = 0;

        public static final int PAGE_APPS = 1;

        public static final int PAGE_AGENDA = 2;

        public static final int PAGE_CALLS = 3;

        public static final int PAGE_PEOPLE = 4;

        public static final int PAGE_SETTINGS = 5;

        public static final int PAGE_SMS = 6;

        public static final int PAGE_SEARCH = 7;


        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + PAGES_TABLE_NAME);


    }

    final class Configuration implements BaseColumns, ConfigurationColumns {

        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + CONFIG_TABLE_NAME);


    }

    final class Hotspots
            implements BaseColumns, HotspotColumns, JoinedDefaultPageColumns {

        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + HOTSPOTS_TABLE_NAME);


    }

    final class HotspotPages implements BaseColumns, HotspotPagesColumns {

        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + HOTSPOT_PAGES_TABLE_NAME);


    }

    /**
     * A way to query the configuration of all pages for a single hotspots. Needs a hotspot
     * id as a uri suffix. This table can only be uses to select data from.
     */
    final class HotspotDetails implements HotspotPageDetailsColumns {

        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + HOTSPOT_PAGES_DETAILS_TABLE_NAME);


    }


}
