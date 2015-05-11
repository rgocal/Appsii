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

package com.appsimobile.appsii.module.calls;

import android.provider.CallLog;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.util.IntList;

/**
 * Created by nick on 29/05/14.
 */
public class CallLogEntry {

    private final IntList mCallTypes = new IntList();

    public int mJulianDay;

    public String mGeoCodedLocation;

    long mId;

    String mNumber;

    String mCachedName;

    CharSequence mNumberTypeLabel;

    boolean mIsRead;

    long mMillis;

    BaseContactInfo mBaseContactInfo;

    boolean mPrivateNumner;

    String mFormattedNumber;

    String mNumberInternational;

    String mNumberNational;

    String mNumberRfc3966;

    boolean mCanRenderAsNational;

    public static int getDrawableForType(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE:
                return R.drawable.ic_call_incoming_holo_dark;
            case CallLog.Calls.MISSED_TYPE:
                return R.drawable.ic_call_missed_holo_dark;
            case CallLog.Calls.OUTGOING_TYPE:
                return R.drawable.ic_call_outgoing_holo_dark;
        }
        return 0;
    }

    boolean isContact() {
        return mBaseContactInfo != null;
    }

    public void addCallType(int type) {
        mCallTypes.add(type);
    }

    public boolean isStarred() {
        return mBaseContactInfo == null ? false : mBaseContactInfo.mStarred;
    }

    public int getCallType(int idx) {
        return mCallTypes.get(idx);
    }

    public int getCallTypeCount() {
        return mCallTypes.size();
    }
}
