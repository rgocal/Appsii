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

package com.appsimobile.appsii.firstrun;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.R;

/**
 * Created by nick on 10/06/15.
 */
public final class FirstRunFragment extends Fragment
        implements FirstRunWelcomeFragment.OnWelcomeCompletedListener,
        FirstRunSettingsFragment.OnSettingsCompletedListener,
        FirstRunLocationFragment.OnLocationCompletedListener,
        FirstRunDoneFragment.OnDoneCompletedListener {

    boolean mFragmentAdded;

    private OnFirstRunCompletedListener mOnFirstRunCompletedListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // set the listener here to prevent it from not being
        // set due to backstack-rotation reasons
        if (mOnFirstRunCompletedListener == null) {
            mOnFirstRunCompletedListener = (OnFirstRunCompletedListener) getActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(com.appsimobile.appsii.R.layout.fragment_first_run, container,
                false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            mFragmentAdded = savedInstanceState.getBoolean("fragment_added");
        }
        if (!mFragmentAdded) {
            mFragmentAdded = true;
            getFragmentManager().
                    beginTransaction().
                    add(R.id.first_run_contents, new FirstRunWelcomeFragment(), "welcome").
                    commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FirstRunWelcomeFragment welcomeFragment =
                (FirstRunWelcomeFragment) getFragmentManager().findFragmentByTag("welcome");
        FirstRunSettingsFragment initialSettingsFragment =
                (FirstRunSettingsFragment) getFragmentManager().findFragmentByTag("settings");
        FirstRunLocationFragment locationFragment =
                (FirstRunLocationFragment) getFragmentManager().findFragmentByTag("location");
        FirstRunDoneFragment doneFragment =
                (FirstRunDoneFragment) getFragmentManager().findFragmentByTag("done");

        if (welcomeFragment != null) {
            welcomeFragment.setOnFirstRunCompletedListener(this);
        }
        if (initialSettingsFragment != null) {
            initialSettingsFragment.setOnSettingsCompletedListener(this);
        }
        if (locationFragment != null) {
            locationFragment.setOnLocationCompletedListener(this);
        }
        if (doneFragment != null) {
            doneFragment.setDoneCompletedListener(this);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("fragment_added", mFragmentAdded);
    }

    @Override
    public void onWelcomeCompleted() {
        FirstRunSettingsFragment fragment = new FirstRunSettingsFragment();
        fragment.setOnSettingsCompletedListener(this);

        getFragmentManager().beginTransaction().
                replace(R.id.first_run_contents, fragment, "settings").
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
                commit();
    }

    @Override
    public void onSettingsCompleted() {
        FirstRunLocationFragment fragment = new FirstRunLocationFragment();
        fragment.setOnLocationCompletedListener(this);

        getFragmentManager().beginTransaction().
                replace(R.id.first_run_contents, fragment, "location").
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
                commit();
    }

    @Override
    public void onSettingsFatalError() {
        mOnFirstRunCompletedListener.onFatalError();
    }

    @Override
    public void onLocationCompleted() {
        showDoneFragment();
    }

    private void showDoneFragment() {
        FirstRunDoneFragment fragment = new FirstRunDoneFragment();
        fragment.setDoneCompletedListener(this);

        getFragmentManager().beginTransaction().
                replace(R.id.first_run_contents, fragment, "done").
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
                commit();
    }

    @Override
    public void onDoneCompleted() {
        mOnFirstRunCompletedListener.onFirstRunCompleted();
    }

    public interface OnFirstRunCompletedListener {

        void onFirstRunCompleted();

        void onFatalError();
    }
}
