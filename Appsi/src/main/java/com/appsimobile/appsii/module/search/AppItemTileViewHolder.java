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

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.apps.AppEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 16/02/15.
 */
class AppItemTileViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    final int mId;

    final OnShowAllClickedListener mOnShowAllClickedListener;

    final AppItemAdapter mItemAdapter;

    final OnAppClickedListener mOnAppClickedListener;

    final TextView mTitle;

    final Button mActionButton;

    final RecyclerView mRecyclerView;

    final List<AppEntry> mItems = new ArrayList<>();

    public AppItemTileViewHolder(
            View itemView,
            int id,
            OnShowAllClickedListener onShowAllClickedListener,
            OnAppClickedListener onAppClickedListener) {

        super(itemView);
        mId = id;
        mOnShowAllClickedListener = onShowAllClickedListener;
        mOnAppClickedListener = onAppClickedListener;

        mTitle = (TextView) itemView.findViewById(R.id.title);
        mRecyclerView = (RecyclerView) itemView.findViewById(R.id.recycler);
        mActionButton = (Button) itemView.findViewById(R.id.action_button);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(
                itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));

        mRecyclerView.addItemDecoration(new GridLayoutDecoration(itemView.getContext()));

        mActionButton.setOnClickListener(this);

        mItemAdapter = new AppItemAdapter(onAppClickedListener);

        mRecyclerView.setAdapter(mItemAdapter);
    }

    public void bindResults(String title, ArrayList<AppEntry> items) {
        mItems.clear();
        int N = items.size();
        for (int i = 0; i < N; i++) {
            AppEntry item = items.get(i);
            mItems.add(item);
        }

        mItemAdapter.setItems(mItems);
        mTitle.setText(title);
    }

    @Override
    public void onClick(View v) {
        mOnShowAllClickedListener.onShowAllAppsClicked(mId, this);
    }


    static abstract class ItemAdapter<T extends RecyclerView.ViewHolder>
            extends RecyclerView.Adapter<T> {

        final List<AppEntry> mItems = new ArrayList<>();

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        void setItems(List<AppEntry> items) {
            mItems.clear();
            mItems.addAll(items);
            notifyDataSetChanged();
        }
    }

    static class AppItemAdapter extends ItemAdapter<AppsViewHolder> {

        final OnAppClickedListener mOnAppClickedListener;

        AppItemAdapter(OnAppClickedListener onAppClickedListener) {
            mOnAppClickedListener = onAppClickedListener;
        }

        @Override
        public AppsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.grid_item_app_small, parent, false);
            view.getLayoutParams().width =
                    (int) (64 * context.getResources().getDisplayMetrics().density);

            return new AppsViewHolder(view, mOnAppClickedListener);
        }

        @Override
        public void onBindViewHolder(AppsViewHolder holder, int position) {
            AppEntry o = mItems.get(position);
            holder.bind(o);
        }
    }


}
