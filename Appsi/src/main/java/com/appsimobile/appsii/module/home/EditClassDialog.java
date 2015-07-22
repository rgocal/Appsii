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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
public class EditClassDialog extends DialogFragment implements TextView.OnEditorActionListener {

    ClassDialogListener mClassDialogListener;

    String mTitle;

    String mTag;

    String mPackageName;

    final List<String> mClassNameItems = new ArrayList<>();

    private AutoCompleteTextView mAutoCompleteTextView;

    public EditClassDialog() {
        setStyle(STYLE_NO_TITLE, 0);
    }

    public static EditClassDialog createDialog(String packageName, String title) {
        return createDialog(packageName, title, null);
    }

    public static EditClassDialog createDialog(String packageName, String title, String tag) {
        EditClassDialog result = new EditClassDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("packageName", packageName);
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
        mPackageName = args.getString("packageName");
        mTag = args.getString("tag");

        PackageManager packageManager = getActivity().getPackageManager();
        Intent intent = new Intent();
        intent.setPackage(mPackageName);

        {
            List<ResolveInfo> resolveInfos = packageManager.queryBroadcastReceivers(intent, 0);
            if (resolveInfos != null) {
                int N = resolveInfos.size();
                for (int i = 0; i < N; i++) {
                    ResolveInfo packageInfo = resolveInfos.get(i);
                    String className = packageInfo.activityInfo.name;
                    if (className.startsWith(mPackageName)) {
                        className = className.substring(mPackageName.length());
                    }
                    mClassNameItems.add(className);
                }
            }
        }
        {
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            if (activities != null) {
                int N = activities.size();
                for (int i = 0; i < N; i++) {
                    ResolveInfo packageInfo = activities.get(i);
                    String className = packageInfo.activityInfo.name;
                    if (className.startsWith(mPackageName)) {
                        className = className.substring(mPackageName.length());
                    }
                    mClassNameItems.add(className);
                }
            }
        }
        {
            List<ResolveInfo> activities = packageManager.queryIntentServices(intent, 0);
            if (activities != null) {
                int N = activities.size();
                for (int i = 0; i < N; i++) {
                    ResolveInfo packageInfo = activities.get(i);
                    String className = packageInfo.serviceInfo.name;
                    if (className.startsWith(mPackageName)) {
                        className = className.substring(mPackageName.length());
                    }
                    mClassNameItems.add(className);
                }
            }
        }
    }

    public void setDialogListener(
            ClassDialogListener dialogListener) {
        mClassDialogListener = dialogListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_class, container);
        mAutoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.class_name);

        // Show soft keyboard automatically
        mAutoCompleteTextView.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mAutoCompleteTextView.setOnEditorActionListener(this);

        if (mTitle != null) {
            mAutoCompleteTextView.setText(mTitle);
        }

        mAutoCompleteTextView.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, android.R.id.text1, mClassNameItems));
        mAutoCompleteTextView.setThreshold(1);

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            // Return input text to activity
            if (mClassDialogListener != null) {
                mClassDialogListener.onFinishEditDialog(mTag,
                        mAutoCompleteTextView.getText().toString());
            }
            this.dismiss();
            return true;
        }
        return false;
    }

    public interface ClassDialogListener {

        void onFinishEditDialog(String tag, String inputText);
    }
}
