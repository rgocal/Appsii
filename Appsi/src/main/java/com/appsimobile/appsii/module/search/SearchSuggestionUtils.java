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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import static com.appsimobile.appsii.module.search.SearchSuggestionContract.SearchSuggestionColumns;

/**
 * Created by Nick on 20/02/14.
 */
public class SearchSuggestionUtils {

    private static SearchSuggestionUtils sInstance;

    SearchSuggestionHandler mSearchSuggestionHandler;

    Context mContext;

    private SearchSuggestionUtils(Context context) {
        mContext = context.getApplicationContext();
        mSearchSuggestionHandler = new SearchSuggestionHandler(mContext.getContentResolver());
    }

    public static SearchSuggestionUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SearchSuggestionUtils(context);
        }
        return sInstance;
    }

    public List<SearchSuggestion> getSearchSuggestions(String query) {
        ContentResolver resolver = mContext.getContentResolver();
        String[] projection = new String[]{
                SearchSuggestionColumns._ID,
                SearchSuggestionColumns.QUERY,
                SearchSuggestionColumns.LAST_USED,
        };

        String selection = SearchSuggestionColumns.QUERY + "like ? ";

        String[] selectionArgs = new String[]{"%" + query + "%"};

        Cursor cursor = resolver.query(SearchSuggestionColumns.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                SearchSuggestionColumns.LAST_USED + " DESC LIMIT 8");

        if (cursor == null) return null;

        List<SearchSuggestion> result = new ArrayList<>(cursor.getCount());
        try {
            while (cursor.moveToNext()) {
                SearchSuggestion suggestion = new SearchSuggestion();
                suggestion.id = cursor.getLong(0);
                suggestion.query = cursor.getString(1);
                suggestion.lastUsed = cursor.getLong(2);
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public void saveQuery(String query) {
        if (!TextUtils.isEmpty(query)) {
            mSearchSuggestionHandler.saveQuery(query);
        }
    }


    static class SearchSuggestionHandler extends AsyncQueryHandler {

        public SearchSuggestionHandler(ContentResolver cr) {
            super(cr);
        }

        public void saveQuery(String query) {
            ContentValues values = new ContentValues(2);
            values.put(SearchSuggestionColumns.LAST_USED, System.currentTimeMillis());
            values.put(SearchSuggestionColumns.QUERY, query);
            startInsert(0, null, SearchSuggestionColumns.CONTENT_URI, values);
        }
    }

}
