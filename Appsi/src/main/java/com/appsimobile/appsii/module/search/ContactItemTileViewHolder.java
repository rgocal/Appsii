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
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.appsii.module.people.ContactView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 16/02/15.
 */
class ContactItemTileViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    final int mId;

    final OnShowAllClickedListener mOnShowAllClickedListener;

    final ContactItemAdapter mItemAdapter;

    final OnPersonClickedListener mOnPersonClickedListener;

    final ContactView.PeopleActionListener mPeopleActionListener;

    TextView mTitle;

    Button mActionButton;

    RecyclerView mRecyclerView;

    List<BaseContactInfo> mItems = new ArrayList<>();

    public ContactItemTileViewHolder(
            View itemView,
            int id,
            OnShowAllClickedListener onShowAllClickedListener,
            OnPersonClickedListener onPersonClickedListener,
            ContactView.PeopleActionListener peopleActionListener) {

        super(itemView);
        mId = id;
        mOnShowAllClickedListener = onShowAllClickedListener;
        mOnPersonClickedListener = onPersonClickedListener;
        mPeopleActionListener = peopleActionListener;

        mTitle = (TextView) itemView.findViewById(R.id.title);
        mRecyclerView = (RecyclerView) itemView.findViewById(R.id.recycler);
        mActionButton = (Button) itemView.findViewById(R.id.action_button);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(
                itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));

        mRecyclerView.addItemDecoration(new GridLayoutDecoration(itemView.getContext()));

        mActionButton.setOnClickListener(this);

        mItemAdapter = new ContactItemAdapter(onPersonClickedListener, peopleActionListener);

        mRecyclerView.setAdapter(mItemAdapter);
    }

    public void bindResults(String title, List<BaseContactInfo> items) {
        mItems.clear();
        for (BaseContactInfo item : items) {
            mItems.add(item);
        }

        mItemAdapter.setItems(mItems);
        mTitle.setText(title);
    }

    @Override
    public void onClick(View v) {
        mOnShowAllClickedListener.onShowAllPeopleClicked(mId, this);
    }

    static abstract class ItemAdapter<T extends RecyclerView.ViewHolder>
            extends RecyclerView.Adapter<T> {

        final List<BaseContactInfo> mItems = new ArrayList<>();

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        void setItems(List<BaseContactInfo> items) {
            mItems.clear();
            mItems.addAll(items);
            notifyDataSetChanged();
        }
    }

    static class ContactItemAdapter extends ItemAdapter<PeopleViewHolder> {

        final OnPersonClickedListener mOnPersonClickedListener;

        final ContactView.PeopleActionListener mPeopleActionListener;

        public ContactItemAdapter(OnPersonClickedListener onPersonClickedListener,
                ContactView.PeopleActionListener peopleActionListener) {
            mOnPersonClickedListener = onPersonClickedListener;
            mPeopleActionListener = peopleActionListener;
        }

        @Override
        public PeopleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

            View view = layoutInflater.inflate(
                    R.layout.grid_item_people_entry_small, parent, false);

            view.getLayoutParams().width =
                    (int) (64 * context.getResources().getDisplayMetrics().density);

            return new PeopleViewHolder(view, mOnPersonClickedListener, mPeopleActionListener);

        }

        @Override
        public void onBindViewHolder(PeopleViewHolder holder, int position) {
            BaseContactInfo o = mItems.get(position);
            holder.bind(o);
        }

    }

}
