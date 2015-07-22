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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * An abstract implementation of BaseAdapter that makes the adapter behave almost
 * the same as the Recycler-Views adapter.
 * Created by nick on 16/09/14.
 */
public abstract class BaseListAdapter<I, VH extends ViewHolder> extends BaseAdapter {

    private final ArrayList<I> mItems = new ArrayList<>();

    private final WeakHashMap<View, VH> mViewHolders = new WeakHashMap<>();

    private LayoutInflater mLayoutInflater;

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public I getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItemId(mItems.get(position));
    }

    protected abstract long getItemId(I type);

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        VH viewHolder = mViewHolders.get(convertView);
        if (viewHolder == null) {
            viewHolder = newViewHolder(mLayoutInflater, parent);
            mViewHolders.put(viewHolder.itemView, viewHolder);
        }
        I item = mItems.get(position);
        bindViewHolder(item, viewHolder);
        return viewHolder.itemView;
    }

    protected abstract VH newViewHolder(LayoutInflater inflater, ViewGroup parent);

    protected abstract void bindViewHolder(I item, VH holder);

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public ArrayList<I> getItems() {
        return mItems;
    }

    public void setItems(List<I> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public I removeItem(int i) {
        I removed = mItems.remove(i);
        notifyDataSetChanged();
        return removed;
    }

    public I removeItem(int i, boolean notify) {
        I removed = mItems.remove(i);
        if (notify) {
            notifyDataSetChanged();
        }
        return removed;
    }


    public void addItemAt(int location, I entry) {
        mItems.add(location, entry);
        notifyDataSetChanged();
    }
}
