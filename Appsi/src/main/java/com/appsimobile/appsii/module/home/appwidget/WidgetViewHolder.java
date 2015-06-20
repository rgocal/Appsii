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

package com.appsimobile.appsii.module.home.appwidget;

import android.appwidget.AppWidgetProviderInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.appwidget.AppWidgetUtils;

/**
 * Created by nick on 19/02/15.
 */
public class WidgetViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    final OnWidgetClickedListener mOnWidgetClickedListener;

    final ImageView mImageView;

    final TextView mWidgetTitleView;

    @Nullable
    Bitmap mBitmap;

    AppWidgetProviderInfo mAppWidgetProviderInfo;

    AsyncTask<Void, Void, Bitmap> mBitmapLoader;

    public WidgetViewHolder(View itemView, OnWidgetClickedListener onWidgetClickedListener) {
        super(itemView);

        mOnWidgetClickedListener = onWidgetClickedListener;
        mImageView = (ImageView) itemView.findViewById(R.id.widget_image);
        mWidgetTitleView = (TextView) itemView.findViewById(R.id.widget_title);

        itemView.setOnClickListener(this);
    }


    public void bind(AppWidgetProviderInfo info) {
        if (mBitmapLoader != null) {
            mBitmapLoader.cancel(true);
        }
        mAppWidgetProviderInfo = info;
        if (mBitmap != null) {
            mBitmap.eraseColor(Color.TRANSPARENT);
        }
        mBitmapLoader = new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return AppWidgetUtils.getWidgetPreviewBitmap(
                        itemView.getContext(), mAppWidgetProviderInfo, null);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mBitmap = bitmap;
                mImageView.setImageBitmap(mBitmap);
            }
        };
        mImageView.setImageDrawable(null);
        mWidgetTitleView.setText(AppWidgetUtils.getWidgetTitle(itemView.getContext(), info));
        mBitmapLoader.execute();

    }

    @Override
    public void onClick(View v) {
        mOnWidgetClickedListener.onWidgetClicked(mAppWidgetProviderInfo, this);
    }

    interface OnWidgetClickedListener {

        void onWidgetClicked(AppWidgetProviderInfo info, WidgetViewHolder holder);
    }


}
