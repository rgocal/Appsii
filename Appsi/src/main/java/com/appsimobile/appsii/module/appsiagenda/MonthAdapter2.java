/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import android.text.format.Time;
import android.util.SparseBooleanArray;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;

import java.util.HashMap;

/**
 * An adapter for a list of {@link com.appsimobile.appsii.module.appsiagenda.MonthView} items.
 */
public abstract class MonthAdapter2 extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements MonthView.OnDayClickListener {

    private static final String TAG = "SimpleMonthAdapter";

    private final Context mContext;

    protected final DatePickerController mController;

    private MonthAdapter.CalendarDay mSelectedDay;

    protected static int WEEK_7_OVERHANG_HEIGHT = 7;

    protected static final int MONTHS_IN_YEAR = 12;


    final Time mTime = new Time(Time.TIMEZONE_UTC);

    public MonthAdapter2(Context context,
            DatePickerController controller) {
        mContext = context;
        mController = controller;
        init();
        setSelectedDay(mController.getSelectedDay());
        setHasStableIds(true);
    }

    /**
     * Updates the selected day and related parameters.
     *
     * @param day The day to highlight
     */
    public void setSelectedDay(MonthAdapter.CalendarDay day) {
        mSelectedDay = day;
        notifyDataSetChanged();
    }

    public MonthAdapter.CalendarDay getSelectedDay() {
        return mSelectedDay;
    }

    /**
     * Set up the gesture detector and selected time
     */
    protected void init() {
        mSelectedDay = new MonthAdapter.CalendarDay(System.currentTimeMillis());
    }

    @Override
    public int getItemCount() {
        return ((mController.getMaxYear() - mController.getMinYear()) + 1) * MONTHS_IN_YEAR;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MonthView v = createMonthView(mContext, parent);
        v.setDatePickerController(mController);

        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        v.setLayoutParams(params);
        v.setClickable(true);
        v.setOnDayClickListener(this);

        return new RecyclerView.ViewHolder(v) {
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        MonthView v = (MonthView) holder.itemView;
        HashMap<String, Integer> drawingParams = null;
        // We store the drawing parameters in the view so it can be recycled
        drawingParams = (HashMap<String, Integer>) v.getTag();

        if (drawingParams == null) {
            drawingParams = new HashMap<String, Integer>();
        }
        drawingParams.clear();

        final int month = position % MONTHS_IN_YEAR;
        final int year = position / MONTHS_IN_YEAR + mController.getMinYear();

        int selectedDay = -1;
        if (isSelectedDayInMonth(year, month)) {
            selectedDay = mSelectedDay.day;
        }

        // Invokes requestLayout() to ensure that the recycled view is set with the appropriate
        // height/number of weeks before being displayed.
        v.reuse();
        mTime.set(1, month, year);
        long millis = mTime.normalize(true);
        int startJulianDay = Time.getJulianDay(millis, 0);

        drawingParams.put(MonthView.VIEW_PARAMS_SELECTED_DAY, selectedDay);
        drawingParams.put(MonthView.VIEW_PARAMS_YEAR, year);
        drawingParams.put(MonthView.VIEW_PARAMS_MONTH, month);
        drawingParams.put(MonthView.VIEW_PARAMS_START_JULIAN_DAY, startJulianDay);
        drawingParams.put(MonthView.VIEW_PARAMS_WEEK_START, mController.getFirstDayOfWeek());
        v.setMonthParams(drawingParams);
        v.invalidate();
    }

    public abstract MonthView createMonthView(Context context, ViewGroup parent);

    private boolean isSelectedDayInMonth(int year, int month) {
        return mSelectedDay.year == year && mSelectedDay.month == month;
    }


    @Override
    public void onDayClick(MonthView view, MonthAdapter.CalendarDay day) {
        if (day != null) {
            onDayTapped(day);
        }
    }

    /**
     * Maintains the same hour/min/sec but moves the day to the tapped day.
     *
     * @param day The day that was tapped
     */
    protected void onDayTapped(MonthAdapter.CalendarDay day) {
        mController.tryVibrate();
        mController.onDayOfMonthSelected(day.year, day.month, day.day, true);
        setSelectedDay(day);
    }

    public void setEventDays(SparseBooleanArray data) {

    }


}
