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

import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.appsimobile.appsii.HotspotPageEntry;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.ViewHolder;
import com.appsimobile.appsii.module.home.provider.HomeContract;

/**
 * The view-holder for a HotpsotPage where the page can be enabled and disabled
 * Created by nick on 31/01/15.
 */
public class PageHotspotViewHolder extends ViewHolder
        implements CompoundButton.OnCheckedChangeListener {

    /**
     * Listener where we send all event
     */
    final OnPageEnabledChangedListener mOnPageEnabledChangedListener;

    /**
     * The switch to enable or disable the page
     */
    final SwitchCompat mSwitchCompat;

    /**
     * The entry to which this view-holder is bound
     */
    HotspotPageEntry mHotspotPageEntry;

    public PageHotspotViewHolder(View view,
            OnPageEnabledChangedListener onPageEnabledChangedListener) {
        super(view);
        mOnPageEnabledChangedListener = onPageEnabledChangedListener;
        mSwitchCompat = (SwitchCompat) view.findViewById(R.id.tag_title);
    }

    public void bind(HotspotPageEntry item) {
        mHotspotPageEntry = item;
        mSwitchCompat.setOnCheckedChangeListener(null);
        mSwitchCompat.setText(item.mPageName);
        mSwitchCompat.setChecked(item.mEnabled);
        mSwitchCompat.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked && (mHotspotPageEntry.mPageType == HomeContract.Pages.PAGE_SMS ||
                mHotspotPageEntry.mPageType == HomeContract.Pages.PAGE_SETTINGS
        )) {
            Context context = itemView.getContext();
            Toast.makeText(context, R.string.page_not_yet_available, Toast.LENGTH_SHORT).show();

            mSwitchCompat.setChecked(false);
            return;
        }
        mHotspotPageEntry.mEnabled = isChecked;
        mOnPageEnabledChangedListener.onPageEnabledStateChanged(
                mHotspotPageEntry.mPageId, mHotspotPageEntry.mHotspotId, isChecked,
                mHotspotPageEntry.mPageType);

    }

    interface OnPageEnabledChangedListener {

        void onPageEnabledStateChanged(long pageId, long hotspotId, boolean enabled, int pageType);
    }

}
