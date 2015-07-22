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

import android.app.DialogFragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.appsimobile.appsii.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 24/01/15.
 */
public class EditPackageDialog extends DialogFragment implements TextView.OnEditorActionListener {

    final List<String> mPackageItems = new ArrayList<>();

    PackageDialogListener mPackageDialogListener;

    String mTitle;

    String mTag;

    private AutoCompleteTextView mAutoCompleteTextView;

    public EditPackageDialog() {
        setStyle(STYLE_NO_TITLE, 0);
    }

    public static EditPackageDialog createDialog(String title) {
        return createDialog(title, null);
    }

    public static EditPackageDialog createDialog(String title, String tag) {
        EditPackageDialog result = new EditPackageDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("tag", tag);
        result.setArguments(args);
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (savedInstanceState == null) {
            mTitle = args.getString("title");
        }
        mTag = args.getString("tag");

        PackageManager packageManager = getActivity().getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);

        int N = packageInfos.size();
        for (int i = 0; i < N; i++) {
            PackageInfo packageInfo = packageInfos.get(i);
            mPackageItems.add(packageInfo.packageName);
        }
    }

    public void setDialogListener(
            PackageDialogListener dialogListener) {
        mPackageDialogListener = dialogListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_package, container);
        mAutoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.package_name);

        // Show soft keyboard automatically
        mAutoCompleteTextView.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mAutoCompleteTextView.setOnEditorActionListener(this);

        if (mTitle != null) {
            mAutoCompleteTextView.setText(mTitle);
        }

        mAutoCompleteTextView.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, android.R.id.text1, mPackageItems));

        mAutoCompleteTextView.setThreshold(1);

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            // Return input text to activity
            if (mPackageDialogListener != null) {
                mPackageDialogListener.onFinishEditDialog(mTag,
                        mAutoCompleteTextView.getText().toString());
            }
            this.dismiss();
            return true;
        }
        return false;
    }

    public interface PackageDialogListener {

        void onFinishEditDialog(String tag, String inputText);
    }
}
