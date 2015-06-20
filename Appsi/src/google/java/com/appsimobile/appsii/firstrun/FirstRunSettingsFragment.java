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

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.appsimobile.appsii.AccountHelper;
import com.appsimobile.appsii.AppsiiUtils;
import com.appsimobile.appsii.PageHelper;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.permissions.PermissionUtils;

/**
 * Created by nick on 10/06/15.
 */
public final class FirstRunSettingsFragment extends Fragment implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private OnSettingsCompletedListener mOnSettingsCompletedListener;

    Button mNextButton;

    View mPermissionsCaption;

    View mPermissionsText;

    View mPermissionsButton;

    CheckBox mHomeCheckbox;

    CheckBox mAppsCheckbox;

    boolean mInitiallyEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We know we came from step 1, and step 1
        // can't be completed without setting the
        // permissions if needed. So we can start
        // Appsii safely from here.
        AppsiiUtils.startAppsi(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first_run_settings, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("initially_enabled", mInitiallyEnabled);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPermissionsCaption = view.findViewById(R.id.permissions_caption);
        mPermissionsText = view.findViewById(R.id.permissions_text);
        mPermissionsButton = view.findViewById(R.id.permissions_button);

        mAppsCheckbox = (CheckBox) view.findViewById(R.id.apps_checkbox);
        mHomeCheckbox = (CheckBox) view.findViewById(R.id.home_checkbox);

        mNextButton = (Button) view.findViewById(R.id.next_button);
        mNextButton.setOnClickListener(this);
        mPermissionsButton.setOnClickListener(this);

        final PageHelper pageHelper = PageHelper.getInstance(getActivity());

        // With initiallyEnabled we track if we enabled the pages. So if the user
        // rotates the screen we do not want to enable them again, especially when
        // the user disabled one of them.
        if (savedInstanceState != null) {
            mInitiallyEnabled = savedInstanceState.getBoolean("initially_enabled");
        }
        if (!mInitiallyEnabled) {
            // enable both pages
            pageHelper.enablePageAccess(HomeContract.Pages.PAGE_HOME, true);
            pageHelper.enablePageAccess(HomeContract.Pages.PAGE_APPS, true);
            mInitiallyEnabled = true;
        }

        mHomeCheckbox.setOnCheckedChangeListener(this);
        mAppsCheckbox.setOnCheckedChangeListener(this);
    }

    public void setOnSettingsCompletedListener(
            OnSettingsCompletedListener onSettingsCompletedListener) {
        mOnSettingsCompletedListener = onSettingsCompletedListener;
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissions();
    }

    private void updatePermissions() {
        if (PermissionUtils.runtimePermissionsAvailable()) {
            boolean holdsPermission = PermissionUtils.holdsPermission(getActivity(),
                    Manifest.permission.AUTHENTICATE_ACCOUNTS);
            mNextButton.setEnabled(holdsPermission);
            mPermissionsButton.setEnabled(!holdsPermission);
        } else {
            mPermissionsCaption.setVisibility(View.GONE);
            mPermissionsText.setVisibility(View.GONE);
            mPermissionsButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.permissions_button:
                onPermissionButtonPressed();
                break;
            case R.id.next_button:
                onNextButtonPressed();
                break;
        }
    }

    private void onNextButtonPressed() {
        mOnSettingsCompletedListener.onSettingsCompleted();
    }

    private void onPermissionButtonPressed() {
        PermissionUtils.requestPermission(
                this, 1,
                Manifest.permission.AUTHENTICATE_ACCOUNTS,
                Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        if (requestCode == 1 && Manifest.permission.AUTHENTICATE_ACCOUNTS.equals(permissions[0]) &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mNextButton.setEnabled(true);
            mPermissionsButton.setEnabled(false);
            AccountHelper.getInstance(getContext()).createAccountIfNeeded();
        }

        if (requestCode == 1 && Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[1]) &&
                grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            showLocationChooser();
        }
    }

    private void showLocationChooser() {

        // TODO: implement
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        final PageHelper pageHelper = PageHelper.getInstance(getActivity());

        switch (id) {
            case R.id.home_checkbox:
                if (isChecked) {
                    pageHelper.enablePageAccess(HomeContract.Pages.PAGE_HOME, true /* force */);
                } else {
                    pageHelper.removePageFromHotspots(HomeContract.Pages.PAGE_HOME);
                }
                break;
            case R.id.apps_checkbox:
                if (isChecked) {
                    pageHelper.enablePageAccess(HomeContract.Pages.PAGE_APPS, true /* force */);
                } else {
                    pageHelper.removePageFromHotspots(HomeContract.Pages.PAGE_APPS);
                }
                break;
        }

    }

    interface OnSettingsCompletedListener {

        void onSettingsCompleted();
    }

}
