/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.appsimobile.appsii.module.calls;

import android.os.Build;
import android.provider.CallLog.Calls;

/**
 * The query for the call log table.
 */
public final class CallLogQuery {

    public static final String COMPAT_GEOCODED_LOCATION = "geocoded_location";

    // If you alter this, you must also alter the method that inserts a fake row to the headers
    // in the CallLogQueryHandler class called createHeaderCursorFor().
    public static final String[] PROJECTION;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PROJECTION = new String[]{
                    Calls._ID,                       // 0
                    Calls.NUMBER,                    // 1
                    Calls.DATE,                      // 2
                    Calls.DURATION,                  // 3
                    Calls.TYPE,                      // 4
                    Calls.CACHED_NAME,               // 5
                    Calls.CACHED_NUMBER_TYPE,        // 6
                    Calls.CACHED_NUMBER_LABEL,       // 7
                    Calls.IS_READ,                   // 8
                    Calls.NUMBER_PRESENTATION,       // 9
                    Calls.CACHED_LOOKUP_URI,         // 10
                    Calls.CACHED_PHOTO_ID,           // 11
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PROJECTION = new String[]{
                    Calls._ID,                       // 0
                    Calls.NUMBER,                    // 1
                    Calls.DATE,                      // 2
                    Calls.DURATION,                  // 3
                    Calls.TYPE,                      // 4
                    Calls.CACHED_NAME,               // 5
                    Calls.CACHED_NUMBER_TYPE,        // 6
                    Calls.CACHED_NUMBER_LABEL,       // 7
                    Calls.IS_READ,                   // 8
                    Calls.NUMBER_PRESENTATION,       // 9
            };
        } else {
            PROJECTION = new String[]{
                    Calls._ID,                       // 0
                    Calls.NUMBER,                    // 1
                    Calls.DATE,                      // 2
                    Calls.DURATION,                  // 3
                    Calls.TYPE,                      // 4
                    Calls.CACHED_NAME,               // 5
                    Calls.CACHED_NUMBER_TYPE,        // 6
                    Calls.CACHED_NUMBER_LABEL,       // 7
                    Calls.IS_READ,                   // 8
            };
        }
    }


    public static final int ID = 0;

    public static final int NUMBER = 1;

    public static final int DATE = 2;

    public static final int DURATION = 3;

    public static final int CALL_TYPE = 4;

    public static final int CACHED_NAME = 5;

    public static final int CACHED_NUMBER_TYPE = 6;

    public static final int CACHED_NUMBER_LABEL = 7;

    public static final int IS_READ = 8;

    /**
     * Only available in API level 19
     */
    public static final int NUMBER_PRESENTATION = 9;

    /**
     * Only available in API level 21
     */
    public static final int CACHED_LOOKUP_URI = 10;

    /**
     * Only available in API level 21
     */
    public static final int CACHED_PHOTO_ID = 11;
}
