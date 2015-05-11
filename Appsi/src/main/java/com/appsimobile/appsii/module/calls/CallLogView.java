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

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.AbstractContactView;
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.appsii.module.PopupMenuBuilder;
import com.appsimobile.util.CallTypeIconsView;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.List;

/**
 * Created by nick on 29/05/14.
 */
public class CallLogView extends AbstractContactView implements View.OnClickListener {

    public static final int ACTION_CALL_NUMBER = 0;

    public static final int ACTION_EDIT = 1;

    public static final int ACTION_ADD_CONTACT = 2;

    CallTypeIconsView mCallTypeIconsView;

    private TextView mCallLogType;

//    private TextView mCallLogNumber;

    private View mOverflow;

    private TextView mCallLogCount;

    private String mPrivateNumberString;

    private CallLogEntry mCallLogEntry;

    public CallLogView(Context context) {
        this(context, null);
    }

    public CallLogView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CallLogView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPrivateNumberString = getResources().getString(R.string.private_num);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
//        mCallLogNumber = (TextView) findViewById(R.id.call_log_number);
        mCallLogType = (TextView) findViewById(R.id.call_log_type);
        mCallTypeIconsView = (CallTypeIconsView) findViewById(R.id.call_types_view);
        mOverflow = findViewById(R.id.overflow);
        mCallLogCount = (TextView) findViewById(R.id.call_log_count);
        mOverflow.setOnClickListener(this);
    }

    public void bind(CallLogEntry entry) {
        if (mCallLogEntry == entry) return;
        mCallLogEntry = entry;
        String name = entry.mCachedName;
        if (TextUtils.isEmpty(name)) {
            if (entry.mCanRenderAsNational) {
                name = entry.mNumberNational;
            } else {
                name = entry.mNumberInternational;
            }
        }

        String title = entry.mPrivateNumner ? mPrivateNumberString : name;
        bindToData(entry.mBaseContactInfo, title);

//        if (mCallLogNumber != null) {
//            mCallLogNumber.setText(entry.mNumber);
//        }
        if (mCallLogType != null) {
            if (entry.isContact()) {
                mCallLogType.setText(entry.mNumberTypeLabel);
            } else {
                if (!TextUtils.isEmpty(entry.mGeoCodedLocation)) {
                    mCallLogType.setText(entry.mGeoCodedLocation);
                } else {
                    mCallLogType.setText(R.string.other);
                }
            }
        }
//
//        if (mCallLogWhen != null) {
//            int type = entry.getCallType(0);
//            int drawableResId = CallLogEntry.getDrawableForType(type);
//            Drawable drawable = getResources().getDrawable(drawableResId);
//            TextViewCompat.setCompoundDrawablesWithIntrinsicBounds(mCallLogType, drawable, null,
//                    null, null);
//            int today = TimeUtils.getJulianDay();
//            int day = Time.getJulianDay(entry.mMillis, 0);
//
//            CharSequence time;
//            if (day == today) {
//                time = DateUtils.getRelativeTimeSpanString(
//                        entry.mMillis, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS);
//            } else if (day == today - 1) {
//                time = getContext().getString(R.string.yesterday);
//            } else {
//                time = DateUtils
//                        .formatDateTime(getContext(), entry.mMillis, DateUtils.FORMAT_SHOW_DATE);
//            }
//            mCallLogWhen.setText(time);
//        }
        mCallTypeIconsView.clear();
        if (mCallTypeIconsView != null) {
            int realCount = entry.getCallTypeCount();
            int count = Math.min(3, realCount);
            for (int i = 0; i < count; i++) {
                mCallTypeIconsView.add(entry.getCallType(i));
            }
            if (mCallLogCount != null) {
                if (realCount > 3) {
                    mCallLogCount.setText("(" + realCount + ")");
                } else {
                    mCallLogCount.setText("");
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        PopupMenuBuilder builder = new PopupMenuBuilder(v);
        BaseContactInfo baseContactInfo = mCallLogEntry.mBaseContactInfo;
        List<BaseContactInfo.TypedPhoneNumber> phoneNumbers =
                baseContactInfo == null ? null : baseContactInfo.mPhoneNumbers;
        if (phoneNumbers != null && !phoneNumbers.isEmpty()) {
            builder.addAction(ACTION_CALL_NUMBER, R.string.action_call);
        }

        if (mCallLogEntry.isContact()) {
            builder.addAction(ACTION_EDIT, R.string.action_edit);
        } else {
            PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            String country = CallLogLoader.getCountry(v.getContext()).toUpperCase();
            try {
                Phonenumber.PhoneNumber number =
                        phoneNumberUtil.parse(mCallLogEntry.mNumber, country);
                if (phoneNumberUtil.isValidNumber(number)) {
                    builder.addAction(ACTION_ADD_CONTACT, R.string.action_call);

                }
            } catch (NumberParseException e) {
                // simply fail
            }
            builder.addAction(ACTION_ADD_CONTACT, R.string.calls_action_add_to_contacts);
        }
        builder.show();
    }
}
