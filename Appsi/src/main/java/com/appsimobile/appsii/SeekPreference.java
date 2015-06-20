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

package com.appsimobile.appsii;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    private static final String androidns = "http://schemas.android.com/apk/res/android";

    private final Context mContext;

    private final String mLeftValueText;

    private final String mRightValueText;

    private final boolean mShowValue;

    private final String mDialogMessage;

    private final String mSuffix;

    private final int mDefault;

    private final int mMin;

    final CharSequence mSummary;

    int mLastSetValue;

    private SeekBar mSeekBar;

    private TextView mSplashText;

    private TextView mValueText;

    private int mMax;

    private int mValue;

    public SeekPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mSuffix = attrs.getAttributeValue(androidns, "text");
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
        mMax = attrs.getAttributeIntValue(androidns, "max", 100);

        mDialogMessage = String.valueOf(getDialogMessage());
        mSummary = getSummary();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekPreference, 0, 0);
        mRightValueText = a.getString(R.styleable.SeekPreference_rightText);
        mLeftValueText = a.getString(R.styleable.SeekPreference_leftText);
        mMin = a.getInt(R.styleable.SeekPreference_minValue, 0);
        mShowValue = a.getBoolean(R.styleable.SeekPreference_valueVisible, false);
        a.recycle();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        if (a.hasValue(index)) {
            return a.getInt(index, 0);
        }
        return super.onGetDefaultValue(a, index);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        return super.onCreateView(parent);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore) {
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
        } else {
            mValue = (Integer) defaultValue;
        }
        updateSummaryText(mValue);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
    }

    @Override
    protected View onCreateDialogView() {
        LayoutInflater inf = LayoutInflater.from(mContext);
        View layout = inf.inflate(R.layout.seekpreference_dialog_view, null);


        mSplashText = (TextView) layout.findViewById(R.id.splash_text);
        if (mDialogMessage != null) mSplashText.setText(mDialogMessage);

        TextView left = (TextView) layout.findViewById(R.id.value_left_text);
        TextView right = (TextView) layout.findViewById(R.id.value_right_text);

        if (mLeftValueText != null) left.setText(mLeftValueText);
        if (mRightValueText != null) right.setText(mRightValueText);

        mValueText = (TextView) layout.findViewById(R.id.value_text);
        if (mShowValue) {
            mValueText.setVisibility(View.VISIBLE);
        } else {
            mValueText.setVisibility(View.GONE);
        }

        mSeekBar = (SeekBar) layout.findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);

        if (shouldPersist()) mValue = getPersistedInt(mDefault);

        mSeekBar.setMax(mMax - mMin);
        mSeekBar.setProgress(mValue - mMin);
        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mSeekBar.setMax(mMax - mMin);
        mSeekBar.setProgress(mValue - mMin);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (shouldPersist() && positiveResult) {
            if (callChangeListener(Integer.valueOf(mLastSetValue))) {
                persistInt(mLastSetValue);
            }
        }
        if (positiveResult) {
            updateSummaryText(mLastSetValue);
        }
    }

    void updateSummaryText(int value) {
        String summary;
        try {
            summary = mSummary == null ? null : String.format(String.valueOf(mSummary), value);
        } catch (Exception e) {
            summary = mSummary == null ? null : String.valueOf(mSummary);
        }
        setSummary(summary);
    }

    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        value += mMin;
        String t = String.valueOf(value);
        mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
        mLastSetValue = value;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seek) {
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getProgress() {
        return mValue;
    }

    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null) {
            mSeekBar.setProgress(progress);
        }
    }
}
