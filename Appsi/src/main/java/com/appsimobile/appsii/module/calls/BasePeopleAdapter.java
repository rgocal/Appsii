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

package com.appsimobile.appsii.module.calls;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by nick on 02/11/14.
 */
abstract class BasePeopleAdapter extends RecyclerView.Adapter<CallLogViewHolder> {

    static final int VIEW_TYPE_PX_HEADER = 0;

    static final int VIEW_TYPE_HEADER = 1;

    static final int VIEW_TYPE_NORMAL = 2;

    private OnItemClickListener mOnItemClickListener;

    private final Context mContext;

    public BasePeopleAdapter(Context context) {
        mContext = context;
        setHasStableIds(true);
    }

    @Override
    public CallLogViewHolder onCreateViewHolder(ViewGroup parent, int itemType) {
        CallLogViewHolder result;
        switch (itemType) {
            case VIEW_TYPE_PX_HEADER:
                result = onCreateParallaxViewHolder(parent);
                break;
            case VIEW_TYPE_HEADER:
                result = onCreateHeaderViewHolder(parent);
                break;
            case VIEW_TYPE_NORMAL:
                result = onCreateNormalViewHolder(parent);
                break;
            default:
                throw new IllegalArgumentException("Unknown view type: " + itemType);
        }
        result.setOnClickListener(mOnItemClickListener);
        return result;
    }

    protected abstract CallLogViewHolder onCreateParallaxViewHolder(ViewGroup parent);

    protected abstract CallLogViewHolder onCreateHeaderViewHolder(ViewGroup parent);

    protected abstract CallLogViewHolder onCreateNormalViewHolder(ViewGroup parent);

    public abstract void setData(List<CallLogEntry> data);

    public abstract void clear();

    public void setOnItemClickListener(
            OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {

        void onItemClick(CallLogViewHolder item);
    }

}
