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

package com.appsimobile.appsii.module.appsiagenda;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import com.appsimobile.appsii.ExpandCollapseDrawable;

/**
 * Created by nick on 03/11/14.
 */
class HeaderViewHolder extends AgendaViewHolder implements View.OnClickListener {

    private final ExpandListener mExpandListener;

    private HeaderItem mHeaderItem;

    public HeaderViewHolder(View view, ExpandListener listener) {
        super(view);
        mExpandListener = listener;
        view.setOnClickListener(this);
    }

    @Override
    public boolean bind(Object object) {
        if (object instanceof HeaderItem) {
            HeaderItem item = (HeaderItem) object;
            mHeaderItem = item;

            TextView textView = (TextView) itemView;
            textView.setText(item.mTitle);

            Drawable[] drawables = textView.getCompoundDrawables();

            ExpandCollapseDrawable drawable = (ExpandCollapseDrawable) drawables[2];

            drawable.setExpanded(true, false);
            drawable.setExpanded(false, false);
            drawable.setExpanded(item.mExpanded, false);
            return true;
        } else {
            mHeaderItem = null;
            return false;
        }
    }

    public void expand() {
        if (!mHeaderItem.mExpanded) {
            toggle();
        }
    }

    private void toggle() {
        boolean isExpanded = !mHeaderItem.mExpanded;
        mHeaderItem.mExpanded = isExpanded;

        TextView textView = (TextView) itemView;
        Drawable[] drawables = textView.getCompoundDrawables();
        ExpandCollapseDrawable drawable = (ExpandCollapseDrawable) drawables[2];
        drawable.setExpanded(isExpanded, true);

        if (isExpanded) {
            mExpandListener.onExpand(getLayoutPosition());
        } else {
            mExpandListener.onCollapse(getLayoutPosition());
        }

    }

    @Override
    public void onClick(View v) {
        toggle();
    }

    interface ExpandListener {

        void onCollapse(int position);

        void onExpand(int position);
    }


}
