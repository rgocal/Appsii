/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.appsii.module.home;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.appsimobile.appsii.R;

/**
 * An edit dialog for the title of a page
 * Created by nick on 24/01/15.
 */
public class HomeItemTitleEditDialog extends DialogFragment
        implements TextView.OnEditorActionListener {

    EditTitleDialogListener mEditTitleDialogListener;

    String mTitle;

    long mPageId;

    private EditText mEditText;

    public HomeItemTitleEditDialog() {
        setStyle(STYLE_NO_TITLE, 0);
    }

    public static HomeItemTitleEditDialog createDialog(long pageId, String title) {
        HomeItemTitleEditDialog result = new HomeItemTitleEditDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putLong("page_id", pageId);
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
        mPageId = args.getLong("page_id");
    }

    public void setEditTitleDialogListener(
            EditTitleDialogListener editTitleDialogListener) {
        mEditTitleDialogListener = editTitleDialogListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_title, container);
        mEditText = (EditText) view.findViewById(R.id.title);

        // Show soft keyboard automatically
        mEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mEditText.setOnEditorActionListener(this);

        if (mTitle != null) {
            mEditText.setText(mTitle);
        }

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            // Return input text to activity
            if (mEditTitleDialogListener != null) {
                mEditTitleDialogListener.onFinishEditDialog(mPageId,
                        mEditText.getText().toString());
            }
            this.dismiss();
            return true;
        }
        return false;
    }

    public interface EditTitleDialogListener {

        void onFinishEditDialog(long pageId, String title);
    }
}
