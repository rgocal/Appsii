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

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.appsii.preference.PreferenceHelper;

import javax.inject.Inject;

/**
 * Created by nick on 10/06/15.
 */
public final class FirstRunWelcomeFragment extends Fragment implements View.OnClickListener {

    View mPermissionsCaption;

    View mPermissionsText;

    View mPermissionsButton;

    Button mNextButton;

    Switch mAutoStartSwitch;
    @Inject
    PreferenceHelper mPreferenceHelper;
    @Inject
    PermissionUtils mPermissionUtils;
    private OnWelcomeCompletedListener mOnFirstRunCompletedListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first_run_welcome, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPermissionsCaption = view.findViewById(R.id.permissions_caption);
        mPermissionsText = view.findViewById(R.id.permissions_text);
        mPermissionsButton = view.findViewById(R.id.permissions_button);
        mNextButton = (Button) view.findViewById(R.id.next_button);
        mAutoStartSwitch = (Switch) view.findViewById(R.id.pref_autostart);

        mAutoStartSwitch.setChecked(true);
        mPermissionsButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissions();
    }

    private void updatePermissions() {
        if (mPermissionUtils.runtimePermissionsAvailable()) {
            boolean holdsPermission = Settings.canDrawOverlays(getActivity());
            mNextButton.setEnabled(holdsPermission);
            mPermissionsButton.setEnabled(!holdsPermission);
        } else {
            mPermissionsCaption.setVisibility(View.GONE);
            mPermissionsText.setVisibility(View.GONE);
            mPermissionsButton.setVisibility(View.GONE);
        }
    }

    public void setOnFirstRunCompletedListener(
            OnWelcomeCompletedListener onFirstRunCompletedListener) {
        mOnFirstRunCompletedListener = onFirstRunCompletedListener;
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
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + BuildConfig.APPLICATION_ID));
        startActivity(intent);
    }

    private void onNextButtonPressed() {
        mPreferenceHelper.setAutoStart(mAutoStartSwitch.isChecked());
        mOnFirstRunCompletedListener.onWelcomeCompleted();
    }

    public interface OnWelcomeCompletedListener {

        void onWelcomeCompleted();
    }
}
