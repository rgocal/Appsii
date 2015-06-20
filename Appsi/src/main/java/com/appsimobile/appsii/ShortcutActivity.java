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

package com.appsimobile.appsii;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick Martens on 8/28/13.
 */
public class ShortcutActivity extends Activity implements View.OnClickListener {

    public static final String EXTRA_ACTION_TO_PERFORM =
            "com.appsimobile.appsii.ShortcutActivity.EXTRA_ACTION_TO_PERFORM";

    public static final String EXTRA_DATASET_URI =
            "com.appsimobile.appsii.ShortcutActivity.EXTRA_DATASET_URI";

    public static final String EXTRA_ACTION_OPEN_SIDEBAR_LEFT =
            "com.appsimobile.appsii.ShortcutActivity.EXTRA_ACTION_OPEN_SIDEBAR_LEFT";

    public static final String EXTRA_ACTION_OPEN_SIDEBAR_RIGHT =
            "com.appsimobile.appsii.ShortcutActivity.EXTRA_ACTION_OPEN_SIDEBAR_RIGHT";

    CheckBox mCheckBox;

    EditText mEditText;

    Spinner mSpinner;

//    DatasetAdapter mDatasetAdapter = new DatasetAdapter();

    private final List<DatasetInfo> mDatasetInfo = new ArrayList<DatasetInfo>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (true) {
            Toast.makeText(this,
                    "The shortcut picker is not yet available in this version of Appsii",
                    Toast.LENGTH_SHORT).show();

            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        Intent i = getIntent();
        String action = i == null ? null : i.getAction();

        if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            super.onCreate(savedInstanceState);
            createChooserDialog();
            ActionbarUtils.enableSaveButton(this, this);
        }
    }

    private void createChooserDialog() {
        setContentView(R.layout.shortcut_creator);
        mEditText = (EditText) findViewById(R.id.editText);
        mCheckBox = (CheckBox) findViewById(R.id.checkbox);
        mSpinner = (Spinner) findViewById(R.id.spinner);


        updateDatasetInfo();

//        mSpinner.setAdapter(mDatasetAdapter);
    }

    private void updateDatasetInfo() {
        mDatasetInfo.clear();

        String name = getString(R.string.home_screen_name);
        mDatasetInfo.add(new DatasetInfo(name, null, "Appsi"));


//        mDatasetAdapter.setData(mDatasetInfo);
    }

    @Override
    public void onClick(View v) {
        boolean left = mCheckBox.isChecked();
        String title = String.valueOf(mEditText.getText());
        DatasetInfo info = (DatasetInfo) mSpinner.getSelectedItem();
        if (TextUtils.isEmpty(title)) {
            title = info.mName;
        }
        onShortcutPicked(left, title);
    }

    public void onShortcutPicked(boolean openFromLeft, String actionName) {
        Intent shortcutIntent = new Intent(this, ShortcutActionActivity.class);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shortcutIntent.putExtra(EXTRA_ACTION_TO_PERFORM, openFromLeft);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, actionName);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_logo));
        setResult(RESULT_OK, intent);
        finish();
    }
}
