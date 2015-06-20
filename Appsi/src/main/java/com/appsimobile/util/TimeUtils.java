/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.util;

import android.text.format.Time;

import java.util.TimeZone;

/**
 * Created by nick on 04/06/14.
 */
public class TimeUtils {

    private TimeUtils() {
    }

    public static int getJulianDay() {
        long offsetMillis = TimeZone.getDefault().getOffset(System.currentTimeMillis());
        long offsetSeconds = offsetMillis / 1000;
        return Time.getJulianDay(System.currentTimeMillis(), offsetSeconds);
//        return Time.getJulianDay(System.currentTimeMillis(), 0);
    }
//
//    public static int getJulianDay(Time recycle) {
//        Time time = recycle;
//        time.setToNow();
//        long millis = time.toMillis(false /* use isDst */);
//        int today = Time.getJulianDay(millis, time.gmtoff);
//        return today;
//    }

    public static int getJulianDay(long millis, Time recycle) {
        recycle.set(millis);
        return Time.getJulianDay(millis, recycle.gmtoff);
    }

    public static int getJulianDayForTime(Time time, boolean ignoreDst) {
        long millis = time.toMillis(ignoreDst);
        return getJulianDayForTimeMillis(millis);
    }

    public static int getJulianDayForTimeMillis(long millis) {
        int offsetSeconds = TimeZone.getDefault().getOffset(millis) / 1000;
        return Time.getJulianDay(millis, offsetSeconds);
    }

}
