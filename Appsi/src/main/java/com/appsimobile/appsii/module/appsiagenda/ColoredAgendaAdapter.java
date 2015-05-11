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

import android.text.format.Time;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.R;
import com.appsimobile.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 07/01/15.
 */
public class ColoredAgendaAdapter extends AgendaAdapter<ColoredAgendaViewHolder> {

    final List<AgendaEvent> mEvents = new ArrayList<>();

    final Time mRecycleTime = new Time(Time.TIMEZONE_UTC);

    /**
     * Maps encoded week-year numbers to their position in the list
     */
    final SparseIntArray mStartPositionsForWeeks = new SparseIntArray();

    /**
     * Maps a position to an encoded week-year number
     */
    final SparseIntArray mReversedStartPositionsForWeeks = new SparseIntArray();

    private ItemAgendaViewHolder.OnItemClickListener mOnAgendaItemClickListener;


    @Override
    public void setOnAgendaItemClickListener(
            ItemAgendaViewHolder.OnItemClickListener onItemClickListener) {
        mOnAgendaItemClickListener = onItemClickListener;
    }

    @Override
    public float getHeaderScrollPercentage() {
        // we don't have a scrolling header, always return 0 for now.
        return 0;
    }

    @Override
    public boolean getTimeAtPosition(Time time, int position) {
        if (position >= mEvents.size()) return false;
        AgendaEvent event = mEvents.get(position);
        time.setJulianDay(event.startDay);
        return true;
    }

    @Override
    public int positionOfDate(int year, int month, int day) {
        mRecycleTime.set(day, month, year);
        int julianDay = TimeUtils.getJulianDay();
        return positionOfJulianDay(julianDay);
    }

    public int positionOfJulianDay(int julianDay) {
        int position = 0;
        for (AgendaEvent event : mEvents) {
            if (event.startDay >= julianDay) {
                return position;
            }
            position++;
        }
        return -1;
    }

    @Override
    public int setAgendaEvents(List<AgendaEvent> data) {
        mStartPositionsForWeeks.clear();
        mReversedStartPositionsForWeeks.clear();

        mEvents.clear();

        if (data == null) {
            notifyDataSetChanged();
            return -1;
        }

        int todayPosition = -1;
        int today = TimeUtils.getJulianDay();
        for (AgendaEvent event : data) {

            if (todayPosition == -1 && event.startDay >= today) {
                todayPosition = mEvents.size();
            }

            // now determine the week-number.
            // The week-number is encoded with the year to create a unique id
            mRecycleTime.setJulianDay(event.startDay);
            int weekNumber = mRecycleTime.getWeekNumber();
            int yearWeek = encodeYearWeekNumber(mRecycleTime.year, weekNumber);

            // when we already have a position for the given week/year, do nothing
            // otherwise register it in the index.
            int existingPosition = mStartPositionsForWeeks.get(yearWeek, -1);
            if (existingPosition == -1) {
                mStartPositionsForWeeks.put(yearWeek, mEvents.size());
                mReversedStartPositionsForWeeks.put(mEvents.size(), yearWeek);
            }

            mEvents.add(event);
        }
        notifyDataSetChanged();
        return todayPosition;
    }

    @Override
    public boolean hasWeekNumberAtPosition(int position) {
        int weekYear = mReversedStartPositionsForWeeks.get(position, 0xFFFFFFFF);
        return weekYear != 0xFFFFFFFF;
    }

    @Override
    int getJulianDayAtPosition(int position) {
        return mEvents.get(position).startDay;
    }

    @Override
    public void focusPosition(int agendaPosition) {
        // We don't need to do anything special here.
    }

    @Override
    public void clear() {
        int count = mEvents.size();
        mEvents.clear();
        notifyItemRangeRemoved(0, count);
    }

    int encodeYearWeekNumber(int year, int weekNumber) {
        return ((year & 0x0000FFFF) << 16) | (weekNumber & 0x0000FFFF);
    }

    int decodeYear(int yearWeekNumber) {
        return (yearWeekNumber >> 16);
    }

    int decodeWeekNumber(int yearWeekNumber) {
        return (yearWeekNumber & 0xFFFF);
    }

    @Override
    public ColoredAgendaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View result = inflater.inflate(R.layout.list_item_agenda_colored, parent, false);
        return new ColoredAgendaViewHolder(result, mOnAgendaItemClickListener);
    }

    @Override
    public void onBindViewHolder(ColoredAgendaViewHolder holder, int position) {
        holder.bind(mEvents.get(position));
    }

    @Override
    public int getItemCount() {
        return mEvents.size();
    }
}
