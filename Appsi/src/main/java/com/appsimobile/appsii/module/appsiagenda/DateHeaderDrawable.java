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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.StyleRes;
import android.text.Layout;
import android.text.SpannableStringBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by nick on 25/09/14.
 */
public class DateHeaderDrawable extends Drawable {

    public static final SimpleDateFormat MONTH_FORMAT =
            new SimpleDateFormat("MMM", Locale.getDefault());

    public static final SimpleDateFormat DAY_FORMAT =
            new SimpleDateFormat("d", Locale.getDefault());

    public static final SimpleDateFormat YEAR_FORMAT =
            new SimpleDateFormat("yyyy", Locale.getDefault());

    final SpannableStringBuilder mStringBuilder;

    @StyleRes
    final int mDayResId;

    @StyleRes
    final int mMonthResId;

    @StyleRes
    final int mYearResId;

    Layout mLayout;

    int mAlpha;

    public DateHeaderDrawable(@StyleRes int dayResId, @StyleRes int monthResId,
            @StyleRes int yearResId) {

        mDayResId = dayResId;
        mMonthResId = monthResId;
        mYearResId = yearResId;
        mStringBuilder = new SpannableStringBuilder();
        updateLayout();
    }

    private void updateLayout() {
        Date date = new Date();
        String month = MONTH_FORMAT.format(date);
        String day = DAY_FORMAT.format(date);
        String year = YEAR_FORMAT.format(date);
    }

    @Override
    public void draw(Canvas canvas) {

    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }
}
