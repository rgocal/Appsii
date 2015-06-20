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
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.StyleRes;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A simple drawable that can render the start time of an event
 * Created by Nick on 24/09/14.
 */
public class EventTimeDrawable extends Drawable {

    public static final int AMPM_NONE = 0;

    public static final int AMPM_AM = 1;

    public static final int AMPM_PM = 2;

    static final SimpleDateFormat sMonthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

    static final Date sFormatDate = new Date();

    static final Calendar sFormatCalendar = Calendar.getInstance();

    private static TextPaint sPaint;

    private final Context mContext;

    final SpannableStringBuilder mTimeBuilder;

    SpannableStringBuilder mAmPmBuilder;

    /**
     * The hour to display in the drawable
     */
    private int mHour;

    /**
     * The minute to display in the drawable
     */
    private int mMinutes;

    private int mAmpm;

    private Layout mTimeLayout;

    private Layout mAmpmLayout;

    private final Object mHourSpan;

    private final Object mMinuteSpan;

    @StyleRes
    private final int mAmpmStyle;

    private int mAlpha = 255;

    private ColorFilter mColorFilter;

    private int mMonth;

    private int mDay;

    private boolean mAllDay;

    public EventTimeDrawable(Context context, @StyleRes int hourStyle,
            @StyleRes int minuteStyle, @StyleRes int amPmStyle) {
        if (sPaint == null) {
            sPaint = new TextPaint();
            sPaint.setAntiAlias(true);
        }
        mContext = context;
        mAmpmStyle = amPmStyle;
        mTimeBuilder = new SpannableStringBuilder();
        mHourSpan = new TextAppearanceSpan(mContext, hourStyle);
        mMinuteSpan = new TextAppearanceSpan(mContext, minuteStyle);
    }

    public void setTime(boolean allDay, int day, int month, int hour, int minutes, int ampm) {
        mAllDay = allDay;
        mDay = day;
        mMonth = month;
        mHour = hour;
        mMinutes = minutes;
        mAmpm = ampm;
        updateLayout();
    }

    private void updateLayout() {
        Rect bounds = getBounds();
        if (bounds.width() <= 0) return;

        if (mAllDay) {
            updateLayoutAllDay();
            return;
        }

        // clear the entire layout
        mTimeBuilder.clear();

        // append the hour and set it's style
        mTimeBuilder.append(String.valueOf(mHour));
        mTimeBuilder.setSpan(mHourSpan, 0, mTimeBuilder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // append the minutes and set it's style
        // first remember the current start position
        int start = mTimeBuilder.length();
        String minutes = String.valueOf(mMinutes);
        // when the string is only 1 digit in length prefix it with
        // a 0. This makes '5' -> '05'
        if (minutes.length() < 2) {
            minutes = "0" + minutes;
        }
        mTimeBuilder.append(minutes);
        mTimeBuilder.setSpan(mMinuteSpan, start, mTimeBuilder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        mTimeLayout = new StaticLayout(mTimeBuilder, sPaint, bounds.width(),
                Layout.Alignment.ALIGN_CENTER, 1, 0, true);
        if (mAmpm == AMPM_NONE) {
            mAmpmLayout = null;
        } else {
            if (mAmPmBuilder == null) {
                mAmPmBuilder = new SpannableStringBuilder();
            }
            mAmPmBuilder.clear();

            mAmPmBuilder.append(mAmpm == AMPM_AM ? "am" : "pm");
            mAmPmBuilder.setSpan(new TextAppearanceSpan(mContext, mAmpmStyle), 0, 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mAmpmLayout = new StaticLayout(mAmPmBuilder, sPaint, bounds.width(),
                    Layout.Alignment.ALIGN_CENTER, 1, 0, true);
        }
    }

    private void updateLayoutAllDay() {
        Rect bounds = getBounds();

        // clear the entire layout
        mTimeBuilder.clear();

        // append the hour and set it's style
        mTimeBuilder.append(String.valueOf(mDay));
        mTimeBuilder.setSpan(mHourSpan, 0, mTimeBuilder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        mTimeLayout = new StaticLayout(mTimeBuilder, sPaint, bounds.width(),
                Layout.Alignment.ALIGN_CENTER, 1, 0, true);

        // this builder may be null to make sure it is only used when needed
        // so check if it is null and create it if needed.
        if (mAmPmBuilder == null) {
            mAmPmBuilder = new SpannableStringBuilder();
        }
        mAmPmBuilder.clear();

        sFormatCalendar.set(Calendar.MONTH, mMonth);
        sFormatDate.setTime(sFormatCalendar.getTimeInMillis());

        mAmPmBuilder.append(sMonthFormat.format(sFormatDate));

        mAmPmBuilder.setSpan(new TextAppearanceSpan(mContext, mAmpmStyle), 0, mAmPmBuilder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mAmpmLayout = new StaticLayout(mAmPmBuilder, sPaint, bounds.width(),
                Layout.Alignment.ALIGN_CENTER, 1, 0, true);

    }

    @Override
    public void draw(Canvas canvas) {

        if (mAmpmLayout == null && mTimeLayout == null) return;

        sPaint.setAlpha(mAlpha);
        sPaint.setColorFilter(mColorFilter);

        Rect bounds = getBounds();

        sPaint.setColor(0x18000000);
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2, sPaint);

        if (mAmpmLayout == null) {
            // simply center the time layout
            int h = mTimeLayout.getHeight();
            int top = (bounds.height() - h) / 2;
            canvas.translate(0, top);
            mTimeLayout.draw(canvas);

            // translate back to restore the state of the canvas
            canvas.translate(0, -top);
        } else {
            int h = mTimeLayout.getHeight();
            int top = ((bounds.height() - h) / 5) * 2;
            canvas.translate(0, top);
            mTimeLayout.draw(canvas);
            canvas.translate(0, -top);

            int ampmHeight = mAmpmLayout.getHeight();

            canvas.translate(0, bounds.height());
            canvas.translate(0, -ampmHeight);
            // add 1/3rd of the height of the am/pm text as margin
            canvas.translate(0, -ampmHeight / 3);

            mAmpmLayout.draw(canvas);

            // translate back and restore the state of the canvas
            canvas.translate(0, ampmHeight / 3);
            canvas.translate(0, ampmHeight);
            canvas.translate(0, -bounds.height());
        }
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        updateLayout();
        Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mColorFilter = cf;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
