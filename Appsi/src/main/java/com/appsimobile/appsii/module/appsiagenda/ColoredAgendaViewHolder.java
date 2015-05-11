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
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.appsimobile.appsii.R;

import java.util.Formatter;
import java.util.TimeZone;

/**
 * Created by nick on 07/01/15.
 */
public class ColoredAgendaViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {

    final Formatter mFormatter;

    final StringBuilder mStringBuilder;

    ItemAgendaViewHolder.OnItemClickListener mListener;

    AgendaEvent mAgendaEvent;

    TextView mTitleView;

    TextView mTimeView;

    AgendaViewLayout mBackgroundView;

    public ColoredAgendaViewHolder(View itemView,
            ItemAgendaViewHolder.OnItemClickListener onAgendaItemClickListener) {
        super(itemView);
        itemView.setOnClickListener(this);
        mTitleView = (TextView) itemView.findViewById(R.id.text);
        mTimeView = (TextView) itemView.findViewById(R.id.time);
        mBackgroundView = (AgendaViewLayout) itemView;
        mListener = onAgendaItemClickListener;
        mStringBuilder = new StringBuilder();
        mFormatter = new Formatter(mStringBuilder);
    }

    public void bind(AgendaEvent event) {
        mAgendaEvent = event;
        mBackgroundView.setColor(event.color);
        mTitleView.setText(event.title);
        if (event.allDay) {
            mTimeView.setText(R.string.all_day);
        } else {
            Context context = itemView.getContext();
            mStringBuilder.setLength(0);
            DateUtils.formatDateRange(context, mFormatter, event.startMillis, event.endMillis,
                    DateUtils.FORMAT_SHOW_TIME, TimeZone.getDefault().getID());
            mTimeView.setText(mStringBuilder);
        }
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onItemClicked(mAgendaEvent);
        }
    }
}
