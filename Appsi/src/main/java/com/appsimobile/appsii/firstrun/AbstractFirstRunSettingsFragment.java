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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.appsimobile.appsii.AccountHelper;
import com.appsimobile.appsii.AppsiiUtils;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.crashlytics.android.Crashlytics;

import javax.inject.Inject;

/**
 * Base class for the settings fragment. The list of checkboxes is different in
 * the community edition from those in the google edition
 * Created by nick on 10/06/15.
 */
public abstract class AbstractFirstRunSettingsFragment
        extends Fragment implements View.OnClickListener {

    Button mNextButton;

    View mPermissionsCaption;

    View mPermissionsText;

    View mPermissionsButton;

    @Inject AccountHelper mAccountHelper;

    @Inject PermissionUtils mPermissionUtils;

    private OnSettingsCompletedListener mOnSettingsCompletedListener;

    public void setOnSettingsCompletedListener(
            OnSettingsCompletedListener onSettingsCompletedListener) {
        mOnSettingsCompletedListener = onSettingsCompletedListener;
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

    private void onPermissionButtonPressed() {
        mPermissionUtils.requestPermission(
                this, 1,
//                Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private void onNextButtonPressed() {
        mOnSettingsCompletedListener.onSettingsCompleted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        if (requestCode == 1 && Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[0]) &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mNextButton.setEnabled(true);
            mPermissionsButton.setEnabled(false);
            try {
                mAccountHelper.createAccountIfNeeded();
            } catch (SecurityException e) {
                // I am not really sure what causes this; but since this normally passes
                // without problems, I suspect something like privacy guard. Just inform
                // the user and exit.
                Log.e("FirstRun", "error creating account. This is needed for Appsi", e);
                Toast.makeText(getActivity(),
                        "There was an error creating the sync account. " +
                                "Appsii can't run without this and will now exit",
                        Toast.LENGTH_SHORT).show();

                // Log this in crashlytics as well
                Crashlytics.logException(e);
                mOnSettingsCompletedListener.onSettingsFatalError();
            }
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We know we came from step 1, and step 1
        // can't be completed without setting the
        // permissions if needed. So we can start
        // Appsii safely from here.
        AppsiiUtils.startAppsi(getActivity());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPermissionsCaption = view.findViewById(R.id.permissions_caption);
        mPermissionsText = view.findViewById(R.id.permissions_text);
        mPermissionsButton = view.findViewById(R.id.permissions_button);

        mNextButton = (Button) view.findViewById(R.id.next_button);
        mNextButton.setOnClickListener(this);
        mPermissionsButton.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissions();
    }

    private void updatePermissions() {
        if (mPermissionUtils.runtimePermissionsAvailable()) {
            boolean holdsPermission = mPermissionUtils.holdsPermission(getActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            mNextButton.setEnabled(holdsPermission);
            mPermissionsButton.setEnabled(!holdsPermission);
        } else {
            mPermissionsCaption.setVisibility(View.GONE);
            mPermissionsText.setVisibility(View.GONE);
            mPermissionsButton.setVisibility(View.GONE);
        }
    }

    interface OnSettingsCompletedListener {

        void onSettingsCompleted();

        void onSettingsFatalError();
    }

}
