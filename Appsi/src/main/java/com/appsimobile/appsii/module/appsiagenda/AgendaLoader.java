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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.text.format.Time;

import com.appsimobile.util.ConvertedCursorListLoader;
import com.appsimobile.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 22/09/14.
 */
public class AgendaLoader extends ConvertedCursorListLoader<AgendaEvent> {


    private static final String WHERE_CALENDARS_SELECTED =
            CalendarContract.Calendars.VISIBLE + "=?";

    private static final String[] WHERE_CALENDARS_ARGS = {
            "1"
    };

    /**
     * The default sort order for this table.
     */
    private static final String DEFAULT_SORT_ORDER = "begin ASC";

    int mLastLoadedDay = TimeUtils.getJulianDay();

    private BroadcastReceiver mDayChangeReceiver;

    public AgendaLoader(Context context, DatePickerController controller) {

        super(context);

        Time time = new Time(Time.TIMEZONE_UTC);
        time.set(1, 0, controller.getMinYear());
        long millis = time.normalize(true);

        int startDay = Time.getJulianDay(millis, 0);

        time.year = controller.getMaxYear();
        millis = time.normalize(true);
        int endDay = Time.getJulianDay(millis, 0);

        Uri uri = CalendarContract.Instances.CONTENT_BY_DAY_URI.
                buildUpon().
                appendPath(String.valueOf(startDay)).
                appendPath(String.valueOf(endDay)).build();

        setUri(uri);
        setProjection(CalendarQuery.projection);
        setSelection(WHERE_CALENDARS_SELECTED);
        setSelectionArgs(WHERE_CALENDARS_ARGS);
        setSortOrder(DEFAULT_SORT_ORDER);
    }


    @Override
    protected List<AgendaEvent> convertCursor(@NonNull Cursor c) {


        List<AgendaEvent> result = new ArrayList<>();

        c.moveToPosition(-1);

        if (c.getCount() > 0) {

            while (c.moveToNext()) {
                AgendaEvent e = new AgendaEvent();

                e.allDay = c.getInt(CalendarQuery.ALL_DAY) == 1;
                e.calendarName = c.getString(CalendarQuery.CALENDAR_DISPLAY_NAME);

                e.startDay = c.getInt(CalendarQuery.START_DAY);
                e.startMillis = c.getLong(CalendarQuery.BEGIN);
                e.calendarId = c.getLong(CalendarQuery.CALENDAR_ID);

                e.endDay = c.getInt(CalendarQuery.END_DAY);
                e.endMillis = c.getLong(CalendarQuery.END);

                int loadColorFrom = CalendarQuery.DISPLAY_COLOR;

                e.color = 0xff000000 | c.getInt(loadColorFrom);
                e.id = c.getLong(CalendarQuery.EVENT_ID);
                e.title = c.getString(CalendarQuery.TITLE);
                result.add(e);
            }
        }

        return result;
    }

    @Override
    protected void cleanup(List<AgendaEvent> old) {

    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        int julianDay = TimeUtils.getJulianDay();
        if (mLastLoadedDay != julianDay) {
            mLastLoadedDay = julianDay;
            onContentChanged();
        }

        // start monitoring for day changes to make sure the list is reloaded
        // whenever the date changes.
        mDayChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onContentChanged();
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_DATE_CHANGED);
        getContext().registerReceiver(mDayChangeReceiver, filter);

    }

    @Override
    protected void onReset() {
        super.onReset();

        // on reset, we need to remove the receiver
        if (mDayChangeReceiver != null) {
            getContext().unregisterReceiver(mDayChangeReceiver);
        }
    }

    static class CalendarQuery {


        public static final int DISPLAY_COLOR = 0;

        public static final int ALL_DAY = 1;

        public static final int BEGIN = 2;

        public static final int END = 3;

        public static final int START_DAY = 4;

        public static final int END_DAY = 5;

        public static final int CALENDAR_ID = 6;

        public static final int TITLE = 7;

        public static final int EVENT_TIMEZONE = 8;

        public static final int EVENT_ID = 9;

        public static final int CALENDAR_DISPLAY_NAME = 10;

        static String[] projection = {
                CalendarContract.Instances.DISPLAY_COLOR,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.START_DAY,
                CalendarContract.Instances.END_DAY,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.EVENT_TIMEZONE,
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        };


    }

}
