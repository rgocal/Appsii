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

import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.appsimobile.appsii.HotspotItem;
import com.appsimobile.appsii.PopupMenuHelper;
import com.appsimobile.appsii.R;

/**
 * A simple viewholder for the hotspots. This shows the title of the hotspot and
 * an overflow button with additional options
 * Created by nick on 31/01/15.
 */
public class HotspotViewHolder extends AbsHotspotViewHolder implements View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    /**
     * The textview that shows the title of the hotspot
     */
    final TextView mTextView;

    /**
     * The overflow menu button
     */
    final View mOverflowView;

    /**
     * The listener used to communicate actions on the hotspot to the client
     */
    final HotspotActionListener mActionListener;

    /**
     * The hotspot this view-holder is bound to
     */
    HotspotItem mHotspotItem;

    public HotspotViewHolder(View itemView, HotspotActionListener listener) {
        super(itemView);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActionListener.performMainAction(mHotspotItem);
            }
        });
        mActionListener = listener;
        mTextView = (TextView) itemView.findViewById(R.id.text);
        mOverflowView = itemView.findViewById(R.id.overflow);
        mOverflowView.setOnClickListener(this);

    }

    @Override
    public void bind(HotspotItem hotspotItem) {
        mHotspotItem = hotspotItem;
        mTextView.setText(hotspotItem.mName);
    }

    public void setText(CharSequence title) {
        mTextView.setText(title);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (mActionListener == null) return false;
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_move:
                mActionListener.performMoveHotspotAction(mHotspotItem);
                return true;
            case R.id.action_delete:
                mActionListener.performDeleteHotspotAction(mHotspotItem);
                return true;
            case R.id.action_set_height:
                mActionListener.performSetHeightHotspotAction(mHotspotItem);
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        PopupMenuHelper.showPopupMenu(v, R.menu.viewholder_hotspot, this);
    }


}
