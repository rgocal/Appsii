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
 *
 */

package com.appsimobile.appsii.processmon;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Size;
import android.support.v7.app.AlertDialog;

import com.appsimobile.appsii.R;

/**
 * Created by nick on 24/06/15.
 */
public class IntervalSelectionDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener {

    OnIntervalSelectedListener mOnIntervalSelectedListener;

    int mSelectedIdx;

    long[] mChoices;

    CharSequence[] mChoiceDisplay;

    public static IntervalSelectionDialogFragment createInstance(
            int selectedIdx, @Size(min = 1) long... intervalsMillis) {

        IntervalSelectionDialogFragment result = new IntervalSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putLongArray("choices", intervalsMillis);
        args.putInt("selection", selectedIdx);
        result.setArguments(args);
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mChoices = args.getLongArray("choices");
        mSelectedIdx = args.getInt("selection");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mChoiceDisplay == null) {
            mChoiceDisplay = createDisplayValues(mChoices);
        }

        return new AlertDialog.Builder(getActivity()).
                setSingleChoiceItems(mChoiceDisplay, mSelectedIdx, this).
                create();
    }

    private CharSequence[] createDisplayValues(long[] choices) {
        Resources res = getActivity().getResources();
        int N = choices.length;
        CharSequence[] result = new CharSequence[N];
        for (int i = 0; i < N; i++) {
            int valueSecs = (int) (choices[i] / 1000);
            result[i] = res.getQuantityString(R.plurals.interval_seconds, valueSecs, valueSecs);
        }
        return result;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mOnIntervalSelectedListener.onIntervalSelected(mChoices[which]);
        dismiss();
    }

    public void setOnIntervalSelectedListener(
            OnIntervalSelectedListener onIntervalSelectedListener) {
        mOnIntervalSelectedListener = onIntervalSelectedListener;
    }

    interface OnIntervalSelectedListener {

        void onIntervalSelected(long intervalMillis);
    }

}
