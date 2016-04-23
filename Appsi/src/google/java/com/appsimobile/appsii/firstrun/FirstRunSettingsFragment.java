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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.appsimobile.appsii.PageHelper;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.module.home.provider.HomeContract;

/**
 * Created by nick on 10/06/15.
 */
public final class FirstRunSettingsFragment extends AbstractFirstRunSettingsFragment
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    CheckBox mHomeCheckbox;

    CheckBox mAppsCheckbox;

    boolean mInitiallyEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);
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

        mAppsCheckbox = (CheckBox) view.findViewById(R.id.apps_checkbox);
        mHomeCheckbox = (CheckBox) view.findViewById(R.id.home_checkbox);

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

}
