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

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.appsimobile.appsii.PageHelper;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.provider.HomeContract.Pages;
import com.appsimobile.appsii.permissions.PermissionUtils;

import static android.Manifest.permission.READ_CALENDAR;
import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_CONTACTS;

/**
 * Created by nick on 10/06/15.
 */
public final class FirstRunSettingsFragment extends AbstractFirstRunSettingsFragment
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    CheckBox mHomeCheckbox;

    CheckBox mAppsCheckbox;

    CheckBox mAgendaCheckbox;

    CheckBox mPeopleCheckbox;

    CheckBox mCallsCheckbox;

    boolean mInitiallyEnabled;

    PageHelper mPageHelper;

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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        final PageHelper ph = mPageHelper;

        switch (id) {
            case R.id.home_checkbox:
                if (isChecked) {
                    ph.enablePageAccess(Pages.PAGE_HOME, true /* force */);
                } else {
                    ph.removePageFromHotspots(Pages.PAGE_HOME);
                }
                break;
            case R.id.apps_checkbox:
                if (isChecked) {
                    ph.enablePageAccess(Pages.PAGE_APPS, true /* force */);
                } else {
                    ph.removePageFromHotspots(Pages.PAGE_APPS);
                }
                break;
            case R.id.agenda_checkbox:
                if (isChecked) {
                    enablePageOrRequestPermissions(1, ph, Pages.PAGE_AGENDA, READ_CALENDAR);
                } else {
                    ph.removePageFromHotspots(Pages.PAGE_AGENDA);
                }
                break;
            case R.id.people_checkbox:
                if (isChecked) {
                    enablePageOrRequestPermissions(2, ph, Pages.PAGE_PEOPLE, READ_CONTACTS);
                } else {
                    ph.removePageFromHotspots(Pages.PAGE_PEOPLE);
                }
                break;
            case R.id.call_checkbox:
                if (isChecked) {
                    enablePageOrRequestPermissions(3, ph, Pages.PAGE_CALLS,
                            READ_CONTACTS, READ_CALL_LOG);
                } else {
                    ph.removePageFromHotspots(Pages.PAGE_CALLS);
                }
                break;
        }
    }

    void enablePageOrRequestPermissions(int rc, PageHelper ph, int page, String... permissions) {
        if (checkAndRequestPermission(rc, permissions)) {
            ph.enablePageAccess(page, true /* force */);
        }
    }

    private boolean checkAndRequestPermission(int rc, String... permission) {
        if (!PermissionUtils.holdsAllPermissions(getContext(), permission)) {
            PermissionUtils.requestPermission(this, rc, permission);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: // agenda
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPageHelper.enablePageAccess(Pages.PAGE_AGENDA, true /* force */);
                } else {
                    mAgendaCheckbox.setOnCheckedChangeListener(null);
                    mAgendaCheckbox.setChecked(false);
                    mAgendaCheckbox.setOnCheckedChangeListener(this);
                }
                break;
            case 2: // people
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPageHelper.enablePageAccess(Pages.PAGE_PEOPLE, true /* force */);
                } else {
                    mPeopleCheckbox.setOnCheckedChangeListener(null);
                    mPeopleCheckbox.setChecked(false);
                    mPeopleCheckbox.setOnCheckedChangeListener(this);
                }
                break;
            case 3: // calls
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mPageHelper.enablePageAccess(Pages.PAGE_CALLS, true /* force */);
                } else {
                    mCallsCheckbox.setOnCheckedChangeListener(null);
                    mCallsCheckbox.setChecked(false);
                    mCallsCheckbox.setOnCheckedChangeListener(this);
                }
                break;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPageHelper = PageHelper.getInstance(getActivity());

        mAppsCheckbox = (CheckBox) view.findViewById(R.id.apps_checkbox);
        mHomeCheckbox = (CheckBox) view.findViewById(R.id.home_checkbox);
        mAgendaCheckbox = (CheckBox) view.findViewById(R.id.agenda_checkbox);
        mPeopleCheckbox = (CheckBox) view.findViewById(R.id.people_checkbox);
        mCallsCheckbox = (CheckBox) view.findViewById(R.id.call_checkbox);


        final PageHelper pageHelper = PageHelper.getInstance(getActivity());

        // With initiallyEnabled we track if we enabled the pages. So if the user
        // rotates the screen we do not want to enable them again, especially when
        // the user disabled one of them.
        if (savedInstanceState != null) {
            // TODO: add a flag here to see if this is a new install. If so, dont overwrite
            mInitiallyEnabled = savedInstanceState.getBoolean("initially_enabled");
        }
        if (!mInitiallyEnabled) {
            // enable both pages
            pageHelper.enablePageAccess(Pages.PAGE_HOME, true);
            pageHelper.enablePageAccess(Pages.PAGE_APPS, true);
            pageHelper.enablePageAccess(Pages.PAGE_AGENDA, true);
            pageHelper.enablePageAccess(Pages.PAGE_PEOPLE, true);
            pageHelper.enablePageAccess(Pages.PAGE_CALLS, true);
            mInitiallyEnabled = true;
        }

        mHomeCheckbox.setOnCheckedChangeListener(this);
        mAppsCheckbox.setOnCheckedChangeListener(this);
        mAgendaCheckbox.setOnCheckedChangeListener(this);
        mPeopleCheckbox.setOnCheckedChangeListener(this);
        mCallsCheckbox.setOnCheckedChangeListener(this);
    }

}
