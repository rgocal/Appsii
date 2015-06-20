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

package com.appsimobile.appsii.module.search;

import android.net.Uri;
import android.provider.BaseColumns;

import com.appsimobile.appsii.BuildConfig;

/**
 * Created by Nick on 19/02/14.
 */
public class SearchSuggestionContract {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".suggestions";

    public SearchSuggestionContract mSearchSuggestionContract;

    /**
     * This table holds the weather information for Appsii. Historic information is kept for past
     * events. This database is intended to be used for multiple locations
     */
    public interface SearchSuggestionColumns extends BaseColumns {

        String TABLE_NAME = "suggestions";
        String QUERY = "query";
        Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        /**
         * Long, The timestamp when this query was last executed
         * Can be used to determine if an update is needed.
         */
        String LAST_USED = "last_used";

    }

}
