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

package com.appsimobile.appsii.module.people;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.BaseContactInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 02/11/14.
 */
class PeopleAdapter extends RecyclerView.Adapter<AbstractPeopleViewHolder> {

    final ContactView.PeopleActionListener mPeopleActionListener;

    private final List<BaseContactInfo> mContactInfos = new ArrayList<>();

    int mColumnCount = 1;

    final SparseArray<String> mFirstLetterPositions = new SparseArray<>();

    PeopleViewHolder.OnItemClickListener mOnItemClickListener;

    private final Context mContext;

    private View mParallaxView;

    public PeopleAdapter(ContactView.PeopleActionListener peopleActionListener, Context context) {
        mPeopleActionListener = peopleActionListener;
        mContext = context;
        setHasStableIds(true);
        mContactInfos.add(null);
    }

    public void setColumnCount(int columnCount) {
        mColumnCount = columnCount;
        notifyDataSetChanged();
    }

    public BaseContactInfo getItem(int position) {
        return mContactInfos.get(position);
    }

    @Override
    public AbstractPeopleViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        if (viewType == 0) {
            View view = inflater.inflate(R.layout.page_people_px_header, viewGroup, false);
            mParallaxView = view;
            return new ParallaxViewHolder(view);
        }
        View view = inflater.inflate(R.layout.list_item_people_entry, viewGroup, false);
        return new PeopleViewHolder(view, mOnItemClickListener, mPeopleActionListener);
    }

    @Override
    public void onBindViewHolder(AbstractPeopleViewHolder viewHolder,
            int i) {
        BaseContactInfo info = mContactInfos.get(i);
        // if there is no contact info, bind to null
        // and no need to get any previous items
        if (info == null) {
            viewHolder.bind(null, null, false);
            return;
        }

        String firstLetter = info.getFirstLetter();

        boolean isFirstWithLetter;
        if (i == 0) {
            isFirstWithLetter = true;
        } else {
            BaseContactInfo prev = mContactInfos.get(i - 1);
            if (prev != null) {
                String prevLetter = prev.getFirstLetter();
                isFirstWithLetter =
                        !TextUtils.equals(prevLetter.toLowerCase(), firstLetter.toLowerCase());
            } else {
                isFirstWithLetter = true;
            }
        }
        viewHolder.bind(info, firstLetter, isFirstWithLetter);

    }


    @Override
    public int getItemViewType(int position) {
        if (position == 0) return 0;
        return 1;
    }

    @Override
    public long getItemId(int position) {
        BaseContactInfo contactInfo = mContactInfos.get(position);
        if (contactInfo == null) {
            return 1000000 + position;
        }
        return contactInfo.mContactId;
    }

    @Override
    public int getItemCount() {
        return mContactInfos.size();
    }

    private View newDividerView(Context context, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.list_inset_divider, parent, false);
    }

    public void bindView(View view, Context context, BaseContactInfo contactInfo,
            String letter) {
        ContactView contactView = (ContactView) view;
        contactView.bindToData(contactInfo);
        contactView.setFirstLetter(letter);
    }

    public void setData(List<? extends BaseContactInfo> contactInfos) {
        mContactInfos.clear();
        mFirstLetterPositions.clear();

        // add a header view
        mContactInfos.add(null);

        String letter = null;

        for (int i = 0; i < contactInfos.size(); i++) {
            BaseContactInfo contactInfo = contactInfos.get(i);
            String first = contactInfo.getFirstLetter();
            boolean addLetter = first != null && !first.equals(letter);
            if (addLetter) {
                letter = first;
//                if (i > 0) {
//                    mContactInfos.add(null); // indicates a divider
//                }
                mFirstLetterPositions.put(mContactInfos.size(), letter);
            }
            mContactInfos.add(contactInfo);
        }
        notifyDataSetChanged();
    }

    public void clear() {
        mContactInfos.clear();
        notifyDataSetChanged();
    }

    public void onTrimMemory(int level) {
    }

    public void setOnItemClickListener(PeopleViewHolder.OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public float getHeaderScrollPercentage() {
        if (mParallaxView == null) return 1f;
        if (!mParallaxView.isShown()) return 1f;
        float top = mParallaxView.getTop();
        if (top > 0) top = 0;
        return top / mParallaxView.getHeight();
    }

}
