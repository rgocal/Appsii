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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Nick on 19/02/14.
 */
public class AppsContract {

    public AppsContract mWeatherContract;

    public static boolean isLaunchHistoryUri(Uri uri) {
        return uri.toString().startsWith(LaunchHistoryColumns.CONTENT_URI.toString());
    }

    /**
     * This table holds the tags for the apps page in appsi.
     */
    public static interface TagColumns extends BaseColumns {

        public static final String TABLE_NAME = "tags";

        public static Uri CONTENT_URI = Uri.parse("content://" + AppsProvider.AUTHORITY + "/tags");

        public static final int TAG_TYPE_USER = 0;
        public static final int TAG_TYPE_ALL = 1;
        public static final int TAG_TYPE_RECENT = 2;
        public static final int TAG_TYPE_RUNNING = 3;

        /**
         * String, The name of the tag
         */
        public static final String NAME = "name";

        /**
         * Boolean, The whether this tag should be shown in the apps list or not
         */
        public static final String VISIBLE = "visible";

        /**
         * Boolean, The whether this tag should be expanded by default if it is in the apps list
         */
        public static final String DEFAULT_EXPANDED = "default_expanded";

        /**
         * Boolean, The position of the app in the list
         */
        public static final String POSITION = "position";

        /**
         * Integer, The number of app-columns in the list
         */
        public static final String COLUMN_COUNT = "col_count";

        /**
         * Integer, The type id of the folder
         */
        public static final String TAG_TYPE = "tag_type";


    }

    public static interface JOINED_APP_TAGS {

        public static final String TAG_NAME = "tagName";
        public static final String TAG_ID = "tag_id";
    }

    /**
     * This table holds the tagged apps in Appsi.
     */
    public static interface TaggedAppColumns extends BaseColumns {

        public static final String TABLE_NAME = "taggedApps";

        public static Uri CONTENT_URI =
                Uri.parse("content://" + AppsProvider.AUTHORITY + "/taggedApps");

        /**
         * String, The name of the tag
         */
        public static final String COMPONENT_NAME = "component_name";

        /**
         * Boolean, The position of the app in the list
         */
        public static final String POSITION = "position";

        /**
         * The id of the tag the app described by the component name has
         */
        public static final String TAG_ID = "tag_id";

        /**
         * Boolean, True if the item has been deleted
         */
        public static final String DELETED = "deleted";

    }


    /**
     * This table holds the tagged apps in Appsi.
     */
    public static interface LaunchHistoryColumns extends BaseColumns {

        public static final String TABLE_NAME = "launchHistory";

        public static Uri CONTENT_URI =
                Uri.parse("content://" + AppsProvider.AUTHORITY + "/launchHistory");

        /**
         * String, The name of the tag
         */
        public static final String COMPONENT_NAME = "component_name";

        /**
         * Long, millis since epoch, The last time this app was started from appsi
         */
        public static final String LAST_LAUNCHED = "last_launched";

        /**
         * Int, the total amount of times this app was launched from appsi
         */
        public static final String LAUNCH_COUNT = "tag_id";


    }

}
