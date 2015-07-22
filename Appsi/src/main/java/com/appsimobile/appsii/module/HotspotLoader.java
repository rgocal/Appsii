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

package com.appsimobile.appsii.module;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import com.appsimobile.appsii.HotspotItem;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.util.CollectionUtils;

import java.util.ArrayList;

/**
 * Created by nick on 30/01/15.
 */
public class HotspotLoader {

    public static ArrayList<HotspotItem> loadHotspots(Context c) {
        ContentResolver r = c.getContentResolver();
        Cursor cursor = r.query(HomeContract.Hotspots.CONTENT_URI,
                HotspotQuery.PROJECTION,
                null, null, null);
        if (cursor != null) {
            ArrayList<HotspotItem> result = new ArrayList<>(cursor.getCount());

            try {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(HotspotQuery.ID);
                    float height = cursor.getFloat(HotspotQuery.HEIGHT);
                    float ypos = cursor.getFloat(HotspotQuery.Y_POSITION);
                    boolean left = cursor.getInt(HotspotQuery.LEFT_BORDER) == 1;
                    boolean needsConfiguration =
                            cursor.getInt(HotspotQuery.NEEDS_CONFIGURATION) == 1;
                    long defaultPageId = cursor.isNull(HotspotQuery._DEFAULT_PAGE) ? -1L :
                            cursor.getLong(HotspotQuery._DEFAULT_PAGE);

                    String name = cursor.getString(HotspotQuery.NAME);

                    HotspotItem conf = new HotspotItem();
                    conf.init(id, name, height, ypos, left, needsConfiguration,
                            defaultPageId);

                    result.add(conf);
                }
            } finally {
                cursor.close();
            }
            return result;
        }
        return CollectionUtils.emptyList();
    }
}
