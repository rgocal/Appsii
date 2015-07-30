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

import android.content.res.Resources;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.appsii.module.apps.AppEntry;
import com.appsimobile.appsii.module.people.ContactView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 18/02/15.
 */
class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements
        OnShowAllClickedListener {


    static final int TYPE_APP_SUMMARY = 0;

    static final int TYPE_PEOPLE_SUMMARY = 1;

    static final int TYPE_APP = 2;

    static final int TYPE_PERSON = 3;

    final ArrayList<Object> mItems = new ArrayList<>(12);

    final OnAppClickedListener mOnAppClickedListener;

    final OnPersonClickedListener mOnPersonClickedListener;

    final ContactView.PeopleActionListener mPeopleActionListener;

    final ArrayList<BaseContactInfo> mContactInfos = new ArrayList<>(12);

    final ArrayList<AppEntry> mApps = new ArrayList<>(12);

    boolean mShowPlainApps;

    boolean mShowPlainContacts;

    int mSpanCount;

    final GridLayoutManager.SpanSizeLookup mSpanSizeLookup =
            new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            return SearchAdapter.this.getSpanSize(position);
        }
    };

    SearchAdapter(OnAppClickedListener onAppClickedListener,
            OnPersonClickedListener onPersonClickedListener,
            ContactView.PeopleActionListener peopleActionListener) {
        mOnAppClickedListener = onAppClickedListener;
        mOnPersonClickedListener = onPersonClickedListener;
        mPeopleActionListener = peopleActionListener;
        setHasStableIds(true);
    }

    int getSpanSize(int position) {
        Object item = mItems.get(position);
        if (item == mApps) {
            return mSpanCount;
        } else if (item == mContactInfos) {
            return mSpanCount;
        } else {
            return 1;
        }
    }

    public void setSpanCount(int spanCount) {
        mSpanCount = spanCount;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_APP_SUMMARY:
                return onCreateAppsSummaryViewHolder(parent, layoutInflater);
            case TYPE_PEOPLE_SUMMARY:
                return onCreatePeopleSummaryViewHolder(parent, layoutInflater);
            case TYPE_PERSON:
                return onCreatePersonViewHolder(parent, layoutInflater);
            case TYPE_APP:
                return onCreateAppViewHolder(parent, layoutInflater);
            default:
                throw new IllegalArgumentException("Invalid viewtype: " + viewType);
        }
    }

    private AppItemTileViewHolder onCreateAppsSummaryViewHolder(
            ViewGroup parent, LayoutInflater layoutInflater) {

        View view = layoutInflater.inflate(
                R.layout.list_item_search_result_tile, parent, false);

        return new AppItemTileViewHolder(view, TYPE_APP_SUMMARY, this, mOnAppClickedListener);
    }

    private ContactItemTileViewHolder onCreatePeopleSummaryViewHolder(
            ViewGroup parent, LayoutInflater layoutInflater) {

        View view = layoutInflater.inflate(
                R.layout.list_item_search_result_tile, parent, false);
        return new ContactItemTileViewHolder(view, TYPE_PEOPLE_SUMMARY, this,
                mOnPersonClickedListener, mPeopleActionListener);
    }

    private PeopleViewHolder onCreatePersonViewHolder(
            ViewGroup parent, LayoutInflater layoutInflater) {


        View view = layoutInflater.inflate(
                R.layout.grid_item_people_tile_search, parent, false);

        return new PeopleViewHolder(view, mOnPersonClickedListener, mPeopleActionListener);
    }

    private AppsViewHolder onCreateAppViewHolder(
            ViewGroup parent, LayoutInflater layoutInflater) {


        View view = layoutInflater.inflate(
                R.layout.grid_item_app_tile, parent, false);
        return new AppsViewHolder(view, mOnAppClickedListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AppItemTileViewHolder) {
            onBindItemTileViewHolder((AppItemTileViewHolder) holder, position);
        } else if (holder instanceof ContactItemTileViewHolder) {
            onBindItemTileViewHolder((ContactItemTileViewHolder) holder, position);
        } else if (holder instanceof AppsViewHolder) {
            ((AppsViewHolder) holder).bind((AppEntry) mItems.get(position));
        } else if (holder instanceof PeopleViewHolder) {
            ((PeopleViewHolder) holder).bind((BaseContactInfo) mItems.get(position));
        }

    }

    @Override
    public int getItemViewType(int position) {

        Object item = mItems.get(position);
        if (item == mApps) {
            return TYPE_APP_SUMMARY;
        } else if (item == mContactInfos) {
            return TYPE_PEOPLE_SUMMARY;
        } else if (item instanceof AppEntry) {
            return TYPE_APP;
        } else {
            return TYPE_PERSON;
        }
    }

    @Override
    public long getItemId(int position) {
        Object item = mItems.get(position);
        if (item == mContactInfos) {
            return 14324;
        } else if (item == mApps) {
            return 14334;
        } else if (item instanceof AppEntry) {
            return ((AppEntry) item).getComponentName().hashCode();
        } else {
            return ((BaseContactInfo) item).mContactId;
        }

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void onBindItemTileViewHolder(AppItemTileViewHolder holder, int position) {
        ArrayList<AppEntry> o = (ArrayList<AppEntry>) mItems.get(position);
        Resources res = holder.itemView.getResources();
        String title = res.getQuantityString(R.plurals.search_app_count, o.size(), o.size());

        holder.bindResults(title, o);
    }

    public void onBindItemTileViewHolder(ContactItemTileViewHolder holder, int position) {
        ArrayList<BaseContactInfo> o = (ArrayList<BaseContactInfo>) mItems.get(position);
        Resources res = holder.itemView.getResources();

        String title = res.getQuantityString(R.plurals.search_people_count, o.size(), o.size());

        holder.bindResults(title, o);
    }

    public void clear() {
        mItems.clear();
        mShowPlainContacts = false;
        mShowPlainApps = false;
        notifyDataSetChanged();
    }

    public void setPeopleInfos(List<? extends BaseContactInfo> contactInfos) {
        mContactInfos.clear();
        mContactInfos.addAll(contactInfos);
        rebuildItemList();
        notifyDataSetChanged();
    }

    private void rebuildItemList() {
        mItems.clear();
        if (!mApps.isEmpty()) {
            if (mShowPlainApps || mApps.size() <= 6) {
                mItems.addAll(mApps);
            } else {
                mItems.add(mApps);
            }
        }
        if (!mContactInfos.isEmpty()) {
            if (mShowPlainContacts || mContactInfos.size() <= 6) {
                mItems.addAll(mContactInfos);
            } else {
                mItems.add(mContactInfos);
            }
        }
    }

    public void setApps(List<AppEntry> apps) {
        mApps.clear();
        if (apps != null) {
            // this may be null. Probably in case it failed to load the list
            // of apps for some reason
            mApps.addAll(apps);
        }
        rebuildItemList();
        notifyDataSetChanged();
    }

    @Override
    public void onShowAllPeopleClicked(int id, ContactItemTileViewHolder holder) {
        mShowPlainContacts = true;
        int position = holder.getPosition();
        List<?> items = (List<?>) mItems.remove(position);
        mItems.addAll(position, items);
        notifyDataSetChanged();
    }

    @Override
    public void onShowAllAppsClicked(int id, AppItemTileViewHolder holder) {
        mShowPlainApps = true;
        int position = holder.getPosition();
        List<?> items = (List<?>) mItems.remove(position);
        mItems.addAll(position, items);
        notifyDataSetChanged();
    }
}
