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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.util.SparseBooleanArray;

import com.appsimobile.appsii.PermissionDeniedException;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.util.ConvertedCursorLoader;
import com.appsimobile.util.TimeUtils;

/**
 * Created by nick on 22/09/14.
 */
public class AgendaDaysLoader extends ConvertedCursorLoader<AgendaDaysResult> {

    private static final String EVENT_DAYS_SELECTION = CalendarContract.Events.VISIBLE + "=1";

    int mLastLoadedDay = TimeUtils.getJulianDay();
    PermissionUtils mPermissionUtils;
    private BroadcastReceiver mDayChangeReceiver;
    private BroadcastReceiver mPermissionGrantedReceiver;

    public AgendaDaysLoader(Context context, DatePickerController controller) {

        super(context);

        AppInjector.inject(this);

        Time time = new Time(Time.TIMEZONE_UTC);
        time.set(1, 0, controller.getMinYear());
        long millis = time.normalize(true);

        int startDay = Time.getJulianDay(millis, 0);

        time.year = controller.getMaxYear();
        millis = time.normalize(true);
        int endDay = Time.getJulianDay(millis, 0);


        Uri uri = CalendarContract.EventDays.CONTENT_URI.
                buildUpon().
                appendPath(String.valueOf(startDay)).
                appendPath(String.valueOf(endDay)).build();

        setUri(uri);
        setProjection(new String[]{
                CalendarContract.EventDays.STARTDAY, CalendarContract.EventDays.ENDDAY
        });
        setSelection(EVENT_DAYS_SELECTION);
    }


    @Override
    protected void checkPermissions() throws PermissionDeniedException {
        mPermissionUtils.throwIfNotPermitted(getContext(), Manifest.permission.READ_CALENDAR);
    }

    @Override
    protected AgendaDaysResult convertPermissionDeniedException(PermissionDeniedException e) {
        return new AgendaDaysResult(e);
    }

    @Override
    protected AgendaDaysResult convertCursor(@NonNull Cursor c) {
        c.moveToPosition(-1);

        int count = c.getCount();

        if (count > 0) {
            SparseBooleanArray result = new SparseBooleanArray(count);

            while (c.moveToNext()) {
                int startDay = c.getInt(0);
                int endDay = c.getInt(1);
                for (int i = startDay; i <= endDay; i++) {
                    result.put(i, true);
                }
            }
            return new AgendaDaysResult(result);
        }
        return new AgendaDaysResult((SparseBooleanArray) null);
    }

    @Override
    protected void cleanup(AgendaDaysResult old) {

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

        mPermissionGrantedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int req = intent.getIntExtra(PermissionUtils.EXTRA_REQUEST_CODE, 0);
                if (req == PermissionUtils.REQUEST_CODE_PERMISSION_READ_CALENDAR) {
                    onContentChanged();
                }
            }
        };
        IntentFilter filter2 = new IntentFilter(PermissionUtils.ACTION_PERMISSION_RESULT);
        getContext().registerReceiver(mPermissionGrantedReceiver, filter2);

    }

    @Override
    protected void onReset() {
        super.onReset();

        // on reset, we need to remove the receiver
        if (mDayChangeReceiver != null) {
            getContext().unregisterReceiver(mDayChangeReceiver);
        }

        if (mPermissionGrantedReceiver != null) {
            getContext().unregisterReceiver(mPermissionGrantedReceiver);
        }

    }

}
