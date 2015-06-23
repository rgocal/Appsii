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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.appsimobile.appsii.HotspotPageEntry;
import com.appsimobile.appsii.HotspotPagesQuery;
import com.appsimobile.appsii.PermissionDeniedException;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.util.ConvertedCursorLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * A loader that can load the hotspots present in the system
 * Created by nick on 22/09/14.
 */
public class HotspotsPagesLoader extends ConvertedCursorLoader<List<HotspotPageEntry>> {


    /**
     * An observer used to reload when a dependency changes.
     * This is bound to the Hotspots.CONTENT_URI
     */
    ForceLoadContentObserver mForceLoadObserver;

    public HotspotsPagesLoader(Context context, long hotspotId) {

        super(context);

        Uri uri = HotspotPagesQuery.createUri(hotspotId);

        setUri(uri);
        setProjection(HotspotPagesQuery.PROJECTION);
        setSelection(null);
        setSelectionArgs(null);
        setSortOrder(HomeContract.HotspotDetails.POSITION + " ASC");
    }

    @Override
    protected void checkPermissions() throws PermissionDeniedException {
    }

    @Override
    protected List<HotspotPageEntry> convertPermissionDeniedException(PermissionDeniedException e) {
        return null;
    }

    @Override
    protected List<HotspotPageEntry> convertCursor(@NonNull Cursor c) {
        c.moveToPosition(-1);

        List<HotspotPageEntry> result = new ArrayList<>(c.getCount());
        while (c.moveToNext()) {

            int pageType = c.getInt(HotspotPagesQuery.PAGE_TYPE);
            // The page types SMS and SETTINGS are not yet ready.
            // They will be added once implemented
            if (pageType == HomeContract.Pages.PAGE_SMS || pageType ==
                    HomeContract.Pages.PAGE_SETTINGS) {
                continue;
            }

            HotspotPageEntry entry = new HotspotPageEntry();
            entry.mEnabled = c.getInt(HotspotPagesQuery.ENABLED) == 1;
            entry.mPageId = c.getLong(HotspotPagesQuery.PAGE_ID);
            entry.mHotspotId = c.getLong(HotspotPagesQuery.HOTSPOT_ID);
            entry.mPageName = c.getString(HotspotPagesQuery.PAGE_NAME);
            entry.mHotspotName = c.getString(HotspotPagesQuery.HOTSPOT_NAME);
            entry.mPosition = c.getInt(HotspotPagesQuery.POSITION);
            entry.mPageType = pageType;
            result.add(entry);
        }
        c.close();
        return result;
    }

    @Override
    protected void cleanup(List<HotspotPageEntry> old) {

    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        mForceLoadObserver = new ForceLoadContentObserver();
        ContentResolver contentResolver = getContext().getContentResolver();
        contentResolver.registerContentObserver(
                HomeContract.HotspotPages.CONTENT_URI, true, mForceLoadObserver);

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
