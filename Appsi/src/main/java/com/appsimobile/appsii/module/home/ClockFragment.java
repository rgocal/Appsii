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

package com.appsimobile.appsii.module.home;

import android.app.Fragment;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.timezonepicker.TimeZoneInfo;
import com.appsimobile.appsii.timezonepicker.TimeZonePickerDialog;

import java.util.TimeZone;

import javax.inject.Inject;


/**
 * Created by nick on 24/01/15.
 */
public class ClockFragment extends Fragment implements View.OnClickListener,
        TimeZonePickerDialog.OnTimeZoneSetListener {

    TextView mTimezonePickerView;

    EditText mTitleText;

    @Inject
    HomeItemConfiguration mConfigurationHelper;

    long mCellId;

    String mCurrentTimeZone;

    String mCurrentTimeZoneTitle;

    String mTitle;

    public static ClockFragment createInstance(long cellId) {
        ClockFragment result = new ClockFragment();
        Bundle args = new Bundle();
        args.putLong("cellId", cellId);
        result.setArguments(args);
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);
        Bundle arguments = getArguments();
        mCellId = arguments.getLong("cellId");

        TimeZonePickerDialog dialog =
                (TimeZonePickerDialog) getFragmentManager().findFragmentByTag("tz_picker");

        if (dialog != null) {
            dialog.setOnTimeZoneSetListener(this);
        }

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clock, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTimezonePickerView = (TextView) view.findViewById(R.id.timezone_picker);
        mTitleText = (EditText) view.findViewById(R.id.title_text);

        View timezoneContainer = view.findViewById(R.id.timezone_container);
        timezoneContainer.setOnClickListener(this);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.home_item_clock_title);

        mCurrentTimeZone = mConfigurationHelper.getProperty(mCellId, "timezone_id",
                TimeZone.getDefault().getID());
        mCurrentTimeZoneTitle = mConfigurationHelper.getProperty(mCellId, "timezone_title", null);

        if (mCurrentTimeZoneTitle == null) {
            mCurrentTimeZoneTitle = mCurrentTimeZone;
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mTitleText.getBackground().setColorFilter(0xFF9E80, PorterDuff.Mode.SRC_ATOP);
        }

        mTitle = mConfigurationHelper.getProperty(mCellId, "title", "Unknown");
        mTitleText.setText(mTitle);
        mTimezonePickerView.setText(mCurrentTimeZoneTitle);

        mTitleText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.equals(s, mTitle)) {
                    mTitle = String.valueOf(s);
                    mConfigurationHelper.updateProperty(mCellId, "title", mTitle);
                }

            }
        });


    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.timezone_container:
                showTimezonePicker();
                break;
        }
    }

    private void showTimezonePicker() {
        TimeZonePickerDialog dialog = new TimeZonePickerDialog();
        Bundle args = new Bundle();
        args.putString(TimeZonePickerDialog.BUNDLE_TIME_ZONE, mCurrentTimeZone);
        dialog.setOnTimeZoneSetListener(this);
        dialog.show(getFragmentManager(), "tz_picker");
    }

    @Override
    public void onTimeZoneSet(TimeZoneInfo tzi) {
        mCurrentTimeZone = tzi.mTzId;
        mTitle = tzi.mDisplayName;
        mCurrentTimeZoneTitle = tzi.mDisplayName;

        mConfigurationHelper.updateProperty(mCellId, "timezone_id", mCurrentTimeZone);
        mConfigurationHelper.updateProperty(mCellId, "timezone_title", mCurrentTimeZoneTitle);
        mConfigurationHelper.updateProperty(mCellId, "title", mTitle);

        mTimezonePickerView.setText(mCurrentTimeZoneTitle);
        mTitleText.setText(mTitle);
    }


}