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

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;

import javax.inject.Inject;

/**
 * Created by nick on 23/06/15.
 */
public class ProcessMonitorFragment extends Fragment implements View.OnClickListener,
        IntervalSelectionDialogFragment.OnIntervalSelectedListener {

    static final long[] sIntervals = {1000, 2000, 3000, 5000, 8000, 10000, 15000, 20000};

    Context mContext;

    View mIntervalContainer;

    RecyclerView mDisallowedProcessesList;

    long mSelectedInterval;

    @Inject
    SharedPreferences mPreferences;

    private static int getSelectedIndex(long selectedInterval, long[] sIntervals) {
        for (int i = 0, sIntervalsLength = sIntervals.length; i < sIntervalsLength; i++) {
            long interval = sIntervals[i];
            if (interval == selectedInterval) return i;
        }
        return -1;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);

        mContext = getActivity();

        mSelectedInterval = mPreferences.getLong("procmon_poll_interval", 3000);

        FragmentManager fm = getFragmentManager();
        IntervalSelectionDialogFragment fragment =
                (IntervalSelectionDialogFragment) fm.findFragmentByTag("interval_selection");
        if (fragment != null) {
            fragment.setOnIntervalSelectedListener(this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_process_monitor, container, false);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.interval_container) {
            showIntervalSelectionDialog();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIntervalContainer = view.findViewById(R.id.interval_container);
        mDisallowedProcessesList =
                (RecyclerView) view.findViewById(R.id.disallowed_app_packages_list);

        mIntervalContainer.setOnClickListener(this);
        mDisallowedProcessesList.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    private void showIntervalSelectionDialog() {
        int index = getSelectedIndex(mSelectedInterval, sIntervals);
        IntervalSelectionDialogFragment intervalFragment = IntervalSelectionDialogFragment.
                createInstance(index, sIntervals);
        intervalFragment.setOnIntervalSelectedListener(this);
        intervalFragment.show(getFragmentManager(), "interval_selection");
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = mPreferences;

        @SuppressWarnings("SimplifiableConditionalExpression")
        boolean dismissed = BuildConfig.DEBUG ?
                false : prefs.getBoolean("process_monitor_got_it_dismissed", false);

        if (!dismissed) {
            GotItFragment fragment =
                    (GotItFragment) getFragmentManager().findFragmentByTag("got_it_dialog");
            if (fragment == null) {
                fragment = new GotItFragment();
                fragment.show(getFragmentManager(), "got_it_dialog");
            }
        }
    }

    @Override
    public void onIntervalSelected(long intervalMillis) {
        mSelectedInterval = intervalMillis;
        mPreferences.edit().putLong("procmon_poll_interval", intervalMillis).apply();
    }

    public static class GotItFragment extends DialogFragment implements View.OnClickListener {

        View mGotItButton;

        @Inject
        SharedPreferences mSharedPreferences;

        public GotItFragment() {
            setStyle(STYLE_NO_TITLE, 0);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            AppInjector.inject(this);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            // TODO: do we want to mark the got-it dismissed??
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.got_it_process_list, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mGotItButton = view.findViewById(R.id.got_it_button);
            mGotItButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            dismiss();
            mSharedPreferences.edit().putBoolean("process_monitor_got_it_dismissed", true).apply();
        }


    }


}
