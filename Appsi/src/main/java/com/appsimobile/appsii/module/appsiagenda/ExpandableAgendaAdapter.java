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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.ExpandCollapseDrawable;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.SidebarContext;
import com.appsimobile.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 03/11/14.
 */
class ExpandableAgendaAdapter extends AgendaAdapter<AgendaViewHolder>
        implements HeaderViewHolder.ExpandListener {

    static final int VIEW_TYPE_PX_HEADER = 0;

    static final int VIEW_TYPE_HEADER = 1;

    static final int VIEW_TYPE_AGENDA_ITEM = 2;

    private final Context mContext;

    private final Time mTime = new Time(Time.TIMEZONE_UTC);

    List<Object> mVisibleItems = new ArrayList<>();

    private ParallaxHeaderItem mHeaderView;

    private ItemAgendaViewHolder.OnItemClickListener mOnAgendaItemClickListener;

    private Handler mHandler;

    private View mParallaxView;

    ExpandableAgendaAdapter(Context context) {
        mContext = context;
        mHeaderView = new ParallaxHeaderItem();
        mVisibleItems.add(mHeaderView);
        mHandler = new Handler();
        setHasStableIds(true);
    }

    @Override
    public AgendaViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_PX_HEADER:
                return onCreateParallaxViewHolder(viewGroup);
            case VIEW_TYPE_HEADER:
                return newGroupViewHolder(viewGroup);
            case VIEW_TYPE_AGENDA_ITEM:
                return newChildViewHolder(viewGroup);
        }
        throw new IllegalArgumentException("Unknown viewType: " + viewType);
    }

    protected AgendaViewHolder onCreateParallaxViewHolder(ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View result = layoutInflater.inflate(R.layout.page_agenda_px_header, parent, false);
        mParallaxView = result;
        return new ParallaxHeaderViewHolder(result);
    }

    protected HeaderViewHolder newGroupViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View result =
                inflater.inflate(R.layout.list_item_header_collapsible, parent, false);
        Resources res = mContext.getResources();
        int dp48 = (int) (res.getDisplayMetrics().density * 24);
        Drawable drawable = new ExpandCollapseDrawable(res);
        drawable.setBounds(0, 0, dp48, dp48);

        ((TextView) result).setCompoundDrawables(null, null, drawable, null);

        return new HeaderViewHolder(result, this);
    }

    protected AgendaViewHolder newChildViewHolder(ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        View result = layoutInflater.inflate(R.layout.list_item_agenda, parent, false);

        return new ItemAgendaViewHolder(result, mOnAgendaItemClickListener);
    }

    @Override
    public void onBindViewHolder(AgendaViewHolder agendaViewHolder, int position) {
        Object item = mVisibleItems.get(position);
        if (!agendaViewHolder.bind(item)) {
            throw new IllegalStateException(
                    "Invalid holder for viewtype. position: " + position + " viewtype: " +
                            getItemViewType(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object item = mVisibleItems.get(position);
        if (item instanceof ParallaxHeaderItem) {
            return VIEW_TYPE_PX_HEADER;
        }
        if (item instanceof HeaderItem) {
            return VIEW_TYPE_HEADER;
        }
        if (item instanceof AgendaEvent) {
            return VIEW_TYPE_AGENDA_ITEM;
        }
        throw new IllegalArgumentException("unknown item: " + item.getClass());
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) return Long.MIN_VALUE;
        Object item = mVisibleItems.get(position);
        if (item instanceof HeaderItem) {
            HeaderItem headerItem = (HeaderItem) item;
            return -headerItem.mJulianDay;
        }
        AgendaEvent event = (AgendaEvent) item;
        return event.id;
    }

    @Override
    public int getItemCount() {
        return mVisibleItems.size();
    }

    @Override
    public void onCollapse(int position) {
        HeaderItem item = (HeaderItem) mVisibleItems.get(position);

        // track how many days ahead items are frequently collapsed
        int today = TimeUtils.getJulianDay();
        int daysAhead = item.mJulianDay - today;
        SidebarContext.track(mContext, AnalyticsManager.ACTION_COLLAPSE_ITEM,
                AnalyticsManager.CATEGORY_AGENDA, "day: " + daysAhead);

        removeItemRange(position + 1, item.mEvents.size());
    }

    @Override
    public void onExpand(int position) {
        HeaderItem item = (HeaderItem) mVisibleItems.get(position);

        // track how many days ahead items are frequently expanded
        int today = TimeUtils.getJulianDay();
        int daysAhead = item.mJulianDay - today;
        SidebarContext.track(mContext, AnalyticsManager.ACTION_EXPAND_ITEM,
                AnalyticsManager.CATEGORY_AGENDA, "day: " + daysAhead);


        List<AgendaEvent> events = item.mEvents;
        mVisibleItems.addAll(position + 1, events);
        notifyItemRangeInserted(position + 1, events.size());
        notifyDataSetChanged();
    }

    private void removeItemRange(int start, int count) {
        for (int i = start + count - 1; i >= start; i--) {
            mVisibleItems.remove(i);
        }
        notifyDataSetChanged();
        notifyItemRangeRemoved(start, count);
    }

    @Override
    public void setOnAgendaItemClickListener(
            ItemAgendaViewHolder.OnItemClickListener onItemClickListener) {
        mOnAgendaItemClickListener = onItemClickListener;
    }

    @Override
    public float getHeaderScrollPercentage() {
        if (mParallaxView == null) return 1f;
        if (!mParallaxView.isShown()) return 1f;
        float top = mParallaxView.getTop();
        if (top > 0) top = 0;
        return top / mParallaxView.getHeight();
    }

    @Override
    public boolean getTimeAtPosition(Time time, int position) {
        if (position >= mVisibleItems.size()) return false;

        Object item = mVisibleItems.get(position);
        if (item instanceof ParallaxHeaderItem) {
            return getTimeAtPosition(time, position + 1);
        } else if (item instanceof HeaderItem) {
            HeaderItem headerItem = (HeaderItem) item;
            time.setJulianDay(headerItem.mJulianDay);
            return true;
        } else {
            return getTimeAtPosition(time, position - 1);
        }
    }

    @Override
    public int positionOfDate(int year, int month, int day) {
        mTime.set(day, month, year);
        long millis = mTime.normalize(true);
        // time is in utc, so no offset needed.
        int requestedJulianDay = Time.getJulianDay(millis, 0);

        return positionOfJulianDay(requestedJulianDay);
    }

    @Override
    public int positionOfJulianDay(int julianDay) {
        int position = 0;
        for (Object o : mVisibleItems) {

            if (o instanceof HeaderItem) {
                HeaderItem item = (HeaderItem) o;
                if (item.mJulianDay >= julianDay) {
                    return position;
                }
            }
            position++;
        }
        return -1;
    }

    @Override
    public int setAgendaEvents(List<AgendaEvent> data) {
        int lastGroupDay = 0;

        int today = TimeUtils.getJulianDay();
        HeaderItem item = null;

        List<HeaderItem> headers = new ArrayList<>();

        int oldCount = getItemCount();

        int todayPosition = -1;

        for (int i = 0; i < data.size(); i++) {
            AgendaEvent e = data.get(i);
            if (e.startDay != lastGroupDay) {
                lastGroupDay = e.startDay;
                item = new HeaderItem();
                if (e.startDay == today) {
                    item.mTitle = mContext.getString(R.string.today);
                } else if (e.startDay == today + 1) {
                    item.mTitle = mContext.getString(R.string.tomorrow);
                } else {
                    boolean showDayOnly = e.startDay < today + 7 && e.startDay > today;
                    if (showDayOnly) {
                        item.mTitle = DateUtils.formatDateTime(mContext, e.startMillis,
                                DateUtils.FORMAT_SHOW_WEEKDAY);
                    } else {
                        item.mTitle = DateUtils.formatDateTime(mContext, e.startMillis,
                                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY |
                                        DateUtils.FORMAT_ABBREV_WEEKDAY);
                    }
                }
                if (todayPosition == -1 && e.startDay >= today) {
                    todayPosition = headers.size();
                }
                item.mEvents = new ArrayList<>();
                item.mJulianDay = e.startDay;
                headers.add(item);
            }
            if (item != null) {
                item.mEvents.add(e);
            }
        }
        int count = headers.size();
        if (oldCount <= 1 && todayPosition != -1) {
            count = Math.min(count, todayPosition + 3);
            for (int i = todayPosition; i < count; i++) {
                headers.get(i).mExpanded = true;
            }
        }
        setItems(headers);
        return todayPosition;
    }

    public void setItems(List<HeaderItem> items) {
        mVisibleItems.clear();
        for (HeaderItem item : items) {
            mVisibleItems.add(item);

            if (item.mExpanded) {
                mVisibleItems.addAll(item.mEvents);
            }
        }
        mVisibleItems.add(0, mHeaderView);
        notifyDataSetChanged();
    }

    @Override
    public boolean hasWeekNumberAtPosition(int position) {
        return false;
    }

    @Override
    int getJulianDayAtPosition(int position) {
        return 0;
    }

    @Override
    public void focusPosition(int agendaPosition) {
        if (agendaPosition != -1) {
            expand(agendaPosition);
        }
    }

    @Override
    public void clear() {
        int count = mVisibleItems.size();

        removeItemRange(1, count - 1);
    }

    public void expand(int position) {
        HeaderItem item = (HeaderItem) mVisibleItems.get(position);

        if (!item.mExpanded) {
            item.mExpanded = true;
            onExpand(position);
        }
    }

    public static class ParallaxHeaderViewHolder extends AgendaViewHolder {

        public ParallaxHeaderViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public boolean bind(Object object) {
            return true;
        }
    }
}
