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

package com.appsimobile.appsii.hotspotmanager;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.appsimobile.appsii.HotspotItem;
import com.appsimobile.appsii.PermissionDeniedException;
import com.appsimobile.appsii.module.HotspotQuery;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.util.ConvertedCursorLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * A loader that can load the hotspots present in the system
 * Created by nick on 22/09/14.
 */
public class HotspotsLoader extends ConvertedCursorLoader<List<HotspotItem>> {


    /**
     * An observer used to reload when a dependency changes.
     * This is bound to the Hotspots.CONTENT_URI
     */
    ForceLoadContentObserver mForceLoadObserver;

    public HotspotsLoader(Context context) {

        super(context);

        Uri uri = HomeContract.Hotspots.CONTENT_URI;

        setUri(uri);
        setProjection(HotspotQuery.PROJECTION);
        setSelection(null);
        setSelectionArgs(null);
        setSortOrder(null);
    }

    @Override
    protected void checkPermissions() throws PermissionDeniedException {
    }

    @Override
    protected List<HotspotItem> convertPermissionDeniedException(PermissionDeniedException e) {
        return null;
    }

    @Override
    protected List<HotspotItem> convertCursor(@NonNull Cursor cursor) {


        cursor.moveToPosition(-1);

        List<HotspotItem> result = new ArrayList<>();

        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(HotspotQuery.ID);
                float height = cursor.getFloat(HotspotQuery.HEIGHT);
                float ypos = cursor.getFloat(HotspotQuery.Y_POSITION);
                boolean left = cursor.getInt(HotspotQuery.LEFT_BORDER) == 1;
                boolean needsConfiguration =
                        cursor.getInt(HotspotQuery.NEEDS_CONFIGURATION) == 1;
                boolean alwaysReopen = !cursor.isNull(HotspotQuery.ALWAYS_OPEN_LAST) &&
                        cursor.getInt(HotspotQuery.ALWAYS_OPEN_LAST) == 1;

                String name = cursor.getString(HotspotQuery.NAME);

                HotspotItem conf = new HotspotItem();
                conf.init(id, name, height, ypos, left, needsConfiguration,
                        alwaysReopen);

                result.add(conf);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    @Override
    protected void cleanup(List<HotspotItem> old) {

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
