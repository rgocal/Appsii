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

package com.appsimobile.appsii.module;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.appsimobile.appsii.R;

/**
 * A helper class to show a permission 'dialog' on top of a page.
 * <p/>
 * Created by nick on 17/06/15.
 */
public class PermissionHelper implements View.OnClickListener {

    final String[] mPermissions;

    @StringRes
    final int mReasonResId;

    final boolean mIsOptional;

    final PermissionListener mPermissionListener;

    CheckBox mDontShowAgainCheckBox;

    ViewGroup mParent;

    View mPermissionView;

    public PermissionHelper(int reasonResId, boolean isOptional,
            @NonNull PermissionListener permissionListener, @NonNull String... permissions) {

        mPermissions = permissions;
        mReasonResId = reasonResId;
        mIsOptional = isOptional;
        mPermissionListener = permissionListener;
    }

    public void show(@NonNull ViewGroup parent) {
        if (mParent != null) {
            throw new IllegalStateException("PermissionHelper can only be used once");
        }

        mParent = parent;

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.include_permission_denied, parent, false);
        mPermissionView = view;

        mDontShowAgainCheckBox = (CheckBox) view.findViewById(R.id.dont_ask_again);
        Button denyButton = (Button) view.findViewById(R.id.deny_permission_button);
        Button requestButton = (Button) view.findViewById(R.id.request_permission_button);
        TextView reasonText = (TextView) view.findViewById(R.id.permission_reason);

        reasonText.setText(mReasonResId);

        if (!mIsOptional) {
            mDontShowAgainCheckBox.setVisibility(View.GONE);
            denyButton.setVisibility(View.GONE);
        }

        denyButton.setOnClickListener(this);
        requestButton.setOnClickListener(this);

        parent.addView(view);
    }

    @Override
    public void onClick(View v) {
        mParent.removeView(mPermissionView);

        int id = v.getId();
        switch (id) {
            case R.id.deny_permission_button:
                boolean dontShowAgain = mDontShowAgainCheckBox.isChecked();
                mPermissionListener.onCancelled(this, dontShowAgain);
                break;
            case R.id.request_permission_button:
                mPermissionListener.onAccepted(this);
                break;
        }
    }

    public String[] getPermissions() {
        return mPermissions;
    }

    public interface PermissionListener {

        void onAccepted(PermissionHelper permissionHelper);

        void onCancelled(PermissionHelper permissionHelper, boolean dontShowAgain);
    }
}
