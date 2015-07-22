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
 *
 */

package com.appsimobile.appsii.hotspotmanager;

import android.Manifest;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.HotspotPageEntry;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.BaseListAdapter;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.mobeta.android.dslv.ConditionalRemovableAdapter;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

/**
 * Created by nick on 14/06/15.
 */
public class ReorderController implements DragSortListView.DropListener,
        PageHotspotViewHolder.OnPageEnabledChangedListener {

    final Activity mContext;

    final long mHotspotId;

    /**
     * The list-adapter showing all the pages in the hotspot
     */
    final HotspotAdapter mHotspotAdapter;

    final QueryHandlerImpl mQueryHandler;

    public ReorderController(Activity context, long hotspotId) {
        mContext = context;
        mHotspotId = hotspotId;
        mHotspotAdapter = new HotspotAdapter(this);
        mQueryHandler = new QueryHandlerImpl(context.getContentResolver());
    }

    /**
     * Called when the hotspot-pages are loaded. Sets them in the adapter
     */
    void setHotspotPages(ArrayList<HotspotPageEntry> hotspotPageEntries) {
        mHotspotAdapter.setItems(hotspotPageEntries);
    }

    public void configure(DragSortListView dragSortListView) {
        dragSortListView.setDropListener(this);
        dragSortListView.setAdapter(mHotspotAdapter);

    }

    @Override
    public void drop(int from, int to) {
        mHotspotAdapter.handleDrop(from, to);
        updatePositions();
    }

    /**
     * Updates the positions of all hotspot-pages. This is called after a page was dropped
     * somewhere else in the adapter
     */
    void updatePositions() {
        int count = mHotspotAdapter.getCount();
        int nextPosition = 0;
        for (int i = 0; i < count; i++) {
            HotspotPageEntry hotspotPageEntry = mHotspotAdapter.getItem(i);
            if (hotspotPageEntry.mEnabled) {
                // now if the position has changed, update it in the database
                if (hotspotPageEntry.mPosition != nextPosition) {
                    mQueryHandler.updateItemPosition(
                            hotspotPageEntry.mPageId, hotspotPageEntry.mHotspotId, nextPosition);
                    hotspotPageEntry.mPosition = nextPosition;
                }
                nextPosition++;
            }
        }

    }

    @Override
    public void onPageEnabledStateChanged(long pageId, long hotspotId, boolean enabled,
            int pageType) {
        mQueryHandler.updateEnabledState(pageId, hotspotId, enabled);
        updatePositions();
        if (enabled) {
            checkPermissions(pageType);
        }
    }

    private void checkPermissions(int pageType) {
        switch (pageType) {
            case HomeContract.Pages.PAGE_AGENDA:
                if (!PermissionUtils.holdsPermission(mContext, Manifest.permission.READ_CALENDAR)) {
                    PermissionUtils.requestPermission(mContext,
                            PermissionUtils.REQUEST_CODE_PERMISSION_READ_CALENDAR,
                            Manifest.permission.READ_CALENDAR);
                }
                break;
            case HomeContract.Pages.PAGE_PEOPLE:
                if (!PermissionUtils.holdsPermission(mContext, Manifest.permission.READ_CONTACTS)) {
                    PermissionUtils.requestPermission(mContext,
                            PermissionUtils.REQUEST_CODE_PERMISSION_READ_CONTACTS,
                            Manifest.permission.READ_CONTACTS);
                }
                break;
            case HomeContract.Pages.PAGE_CALLS:
                if (!PermissionUtils.holdsPermission(mContext, Manifest.permission.READ_CALL_LOG)) {
                    PermissionUtils.requestPermission(mContext,
                            PermissionUtils.REQUEST_CODE_PERMISSION_READ_CALL_LOG,
                            Manifest.permission.READ_CALL_LOG);
                }
                break;
        }
    }

    public static class HotspotAdapter
            extends BaseListAdapter<HotspotPageEntry, PageHotspotViewHolder>
            implements ConditionalRemovableAdapter {

        final PageHotspotViewHolder.OnPageEnabledChangedListener mOnPageEnabledChangedListener;

        public HotspotAdapter(
                PageHotspotViewHolder.OnPageEnabledChangedListener onPageEnabledChangedListener) {
            mOnPageEnabledChangedListener = onPageEnabledChangedListener;
        }

        @Override
        protected long getItemId(HotspotPageEntry entry) {
            return entry.genId();
        }

        @Override
        protected PageHotspotViewHolder newViewHolder(LayoutInflater inflater, ViewGroup parent) {
            View view = inflater.inflate(R.layout.list_item_hotspot, parent, false);
            return new PageHotspotViewHolder(view, mOnPageEnabledChangedListener);
        }

        @Override
        protected void bindViewHolder(HotspotPageEntry item, PageHotspotViewHolder holder) {
            holder.bind(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean canRemove(int pos) {
            return false;
        }

        /**
         * Updates the position in the adapter after a drop
         */
        public void handleDrop(int from, int to) {
            HotspotPageEntry entry = removeItem(from, false);
            addItemAt(to, entry);
        }


    }

    static class QueryHandlerImpl extends AsyncQueryHandler {

        public QueryHandlerImpl(ContentResolver cr) {
            super(cr);
        }

        public void updateItemPosition(long pageId, long hotspotId, int position) {
            ContentValues values = new ContentValues(1);
            values.put(HomeContract.HotspotPages.POSITION, position);
            startUpdate(0, null, HomeContract.HotspotPages.CONTENT_URI,
                    values,
                    HomeContract.HotspotPages._HOTPSOT_ID + "=? AND " +
                            HomeContract.HotspotPages._PAGE_ID + "=?",
                    new String[]{
                            String.valueOf(hotspotId),
                            String.valueOf(pageId)
                    });

        }

        public void updateEnabledState(long pageId, long hotspotId, boolean enabled) {
            ContentValues values = new ContentValues(1);
            if (enabled) {
                values.put(HomeContract.HotspotPages.POSITION, Integer.MAX_VALUE);
                values.put(HomeContract.HotspotPages._HOTPSOT_ID, hotspotId);
                values.put(HomeContract.HotspotPages._PAGE_ID, pageId);
                startInsert(0, null, HomeContract.HotspotPages.CONTENT_URI, values);
            } else {
                startDelete(0, null, HomeContract.HotspotPages.CONTENT_URI,
                        HomeContract.HotspotPages._HOTPSOT_ID + "=? AND " +
                                HomeContract.HotspotPages._PAGE_ID + "=?",
                        new String[]{
                                String.valueOf(hotspotId),
                                String.valueOf(pageId)
                        });
            }
        }
    }


}
