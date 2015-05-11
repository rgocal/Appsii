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

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.appsimobile.appsii.module.apps.AppEntry;
import com.appsimobile.appsii.module.apps.AppView;
import com.appsimobile.appsii.module.apps.TaggedApp;

import java.util.Collections;

/**
 * Created by nick on 18/02/15.
 */
class AppsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    final OnAppClickedListener mOnAppClickedListener;

    AppEntry mAppEntry;

    public AppsViewHolder(View itemView, OnAppClickedListener onAppClickedListener) {
        super(itemView);
        mOnAppClickedListener = onAppClickedListener;
        itemView.setOnClickListener(this);
    }

    void bind(AppEntry o) {
        mAppEntry = o;
        ((AppView) itemView).bind(o, Collections.<TaggedApp>emptyList());
    }

    @Override
    public void onClick(View v) {
        mOnAppClickedListener.onAppClicked(mAppEntry);
    }
}
