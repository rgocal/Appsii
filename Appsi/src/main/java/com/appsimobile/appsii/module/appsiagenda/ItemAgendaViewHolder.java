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

import android.content.Context;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.appsimobile.appsii.R;

/**
 * Created by nick on 03/11/14.
 */
class ItemAgendaViewHolder extends AgendaViewHolder implements View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    private final Time mTime = new Time();

    ImageView mImageView;

    TextView mPrimaryText;

    TextView mSecondaryText;

    AgendaEvent mAgendaEvent;

    OnItemClickListener mOnItemClickListener;

    View mOverflow;

    PopupMenu mPopupMenu;

    public ItemAgendaViewHolder(View view,
            OnItemClickListener onAgendaItemClickListener) {
        super(view);
        mOnItemClickListener = onAgendaItemClickListener;
        mImageView = (ImageView) view.findViewById(R.id.agenda_time_image);
        EventTimeDrawable drawable = new EventTimeDrawable(view.getContext(),
                R.style.TextAppearance_Appsi_Hour,
                R.style.TextAppearance_Appsi_Minute,
                R.style.TextAppearance_Appsi_AmPm);

        int dp40 = (int) (view.getContext().getResources().getDisplayMetrics().density * 40);

        drawable.setBounds(0, 0, dp40, dp40);

        mImageView.setImageDrawable(drawable);
        mPrimaryText = (TextView) view.findViewById(R.id.primary_text);
        mSecondaryText = (TextView) view.findViewById(R.id.secondary_text);
        mOverflow = view.findViewById(R.id.overflow);
        view.setOnClickListener(this);
        mOverflow.setOnClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_edit:
                mOnItemClickListener.onItemEditClicked(mAgendaEvent);
                return true;
            case R.id.action_delete:
                mOnItemClickListener.onItemDeleteClicked(mAgendaEvent);
                return true;
        }
        return false;
    }

    @Override
    public boolean bind(Object object) {
        Context context = itemView.getContext();

        if (object instanceof AgendaEvent) {

            AgendaEvent item = (AgendaEvent) object;
            mAgendaEvent = item;

            mTime.set(item.startMillis);

            mPrimaryText.setText(item.title);
            mSecondaryText.setText(item.calendarName);
            EventTimeDrawable drawable = (EventTimeDrawable) mImageView.getDrawable();

            boolean is24 = android.text.format.DateFormat.is24HourFormat(context);
            if (item.allDay) {
                drawable.setTime(true /* allDay */, mTime.monthDay, mTime.month, mTime.hour,
                        mTime.minute, EventTimeDrawable.AMPM_NONE);

            } else if (is24 && false) {
                drawable.setTime(false /* allDay */, mTime.monthDay, mTime.month, mTime.hour,
                        mTime.minute, EventTimeDrawable.AMPM_NONE);

            } else {
                if (mTime.hour > 12) {
                    drawable.setTime(false /* allDay */, mTime.monthDay, mTime.month,
                            mTime.hour - 12, mTime.minute, EventTimeDrawable.AMPM_PM);
                } else {
                    drawable.setTime(false /* allDay */, mTime.monthDay, mTime.month, mTime.hour,
                            mTime.minute, EventTimeDrawable.AMPM_AM);
                }
            }
            return true;
        } else {
            mAgendaEvent = null;
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.overflow) {
            onOverflowClicked(v);
            return;
        }
        mOnItemClickListener.onItemClicked(mAgendaEvent);
    }

    private void onOverflowClicked(View v) {
        if (mPopupMenu == null) {
            mPopupMenu = new PopupMenu(v.getContext(), v);
            mPopupMenu.setOnMenuItemClickListener(this);
        } else {
            mPopupMenu.getMenu().clear();
        }

        Menu menu = mPopupMenu.getMenu();
        MenuInflater inflater = mPopupMenu.getMenuInflater();
        inflater.inflate(R.menu.popup_agenda_item, menu);

        mPopupMenu.show();
    }

    public static interface OnItemClickListener {

        void onItemClicked(AgendaEvent viewHolder);

        void onItemEditClicked(AgendaEvent viewHolder);

        void onItemDeleteClicked(AgendaEvent viewHolder);
    }


}
