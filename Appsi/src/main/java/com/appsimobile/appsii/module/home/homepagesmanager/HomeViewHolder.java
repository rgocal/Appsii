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

package com.appsimobile.appsii.module.home.homepagesmanager;

import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.appsimobile.appsii.PopupMenuHelper;
import com.appsimobile.appsii.R;

/**
 * The view-holder for the home items view.
 * Created by nick on 01/02/15.
 */
public final class HomeViewHolder extends AbsHomeViewHolder implements View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    /**
     * The listener to which we will send posiible actions
     */
    final HomeViewActionListener mHomeViewActionListener;

    /**
     * The overflow button to display an action menu on press.
     */
    final View mOverflowButton;

    /**
     * The view showing the title of the home-page
     */
    final TextView mTextView;

    /**
     * The item we are bound to.
     */
    HomePageItem mItem;

    public HomeViewHolder(View itemView, HomeViewActionListener listener) {
        super(itemView);
        mOverflowButton = itemView.findViewById(R.id.overflow);
        mTextView = (TextView) itemView.findViewById(R.id.title);
        mOverflowButton.setOnClickListener(this);
        mHomeViewActionListener = listener;
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHomeViewActionListener.onMainAction(mItem);
            }
        });
    }

    /**
     * Binds the view to the given item
     */
    void bind(HomePageItem item) {
        mItem = item;
        mTextView.setText(item.mTitle);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_delete:
                onDeleteClicked();
                return true;
            case R.id.action_edit_layout:
                onEditLayoutClicked();
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        PopupMenuHelper.showPopupMenu(v, R.menu.viewholder_home_view, this);
    }

    /**
     * Called when the delete action was selected in the actions menu.
     */
    private void onDeleteClicked() {
        mHomeViewActionListener.onDeleteSelected(mItem);
    }

    /**
     * Called when the delete action was selected in the actions menu.
     */
    private void onEditLayoutClicked() {
        mHomeViewActionListener.onChangeLayoutSelected(mItem);
    }

    /**
     * A listener interface to which the actions selected are set to.
     */
    interface HomeViewActionListener {

        /**
         * The user choose the delete action on the bound item
         */
        void onDeleteSelected(HomePageItem item);

        /**
         * The user performed the main action (press) on the item
         */
        void onMainAction(HomePageItem item);

        void onChangeLayoutSelected(HomePageItem item);
    }


}
