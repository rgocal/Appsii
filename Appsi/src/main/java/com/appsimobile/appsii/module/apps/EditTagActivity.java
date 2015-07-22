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

package com.appsimobile.appsii.module.apps;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.appsimobile.appsii.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 24/08/14.
 */
public class EditTagActivity extends Activity implements View.OnClickListener,
        AppTagUtils.AppTagListener {

    public static final String EXTRA_TAG = "com.appsimobile.appsii.EXTRA_TAG";

    CheckBox mExpandByDefault;

    AppTag mAppTag;

    /**
     * The confirmation button to add the tag
     */
    private View mOkButton;

    /**
     * The cancel button to cancel the dialog
     */
    private View mCancelButton;

    /**
     * The name of the tag
     */
    private EditText mEditText;

    /**
     * The query handler used to update the tag
     */
    private QueryHandler mQueryHandler;

    private List<AppTag> mAppTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_apptag);
        TextView title = (TextView) findViewById(R.id.title);
        TextView content = (TextView) findViewById(R.id.content);

        Intent intent = getIntent();

        mOkButton = findViewById(R.id.accept);
        mCancelButton = findViewById(R.id.cancel);

        mOkButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);

        mEditText = (EditText) findViewById(R.id.editText);

        mExpandByDefault = (CheckBox) findViewById(R.id.expand_by_default);

        mAppTag = intent.getParcelableExtra(EXTRA_TAG);

        title.setText(R.string.edit_tag);
        content.setText(getString(R.string.tag_name));

        setup(getIntent());
        AppTagUtils.getInstance(this).registerAppTagListener(this);


    }

    /**
     * This method sets up the app-tag to edit and sets the values on the edit-text and
     * the checkbox.
     * <p/>
     * The launch-mode of this activity is set to singleTask. This however does not prevent
     * the user from trying to start this activity multiple times. So, the onNewIntent
     * method may be called with a new tag to edit. In this case overwrite the tag we
     * already have and update the fields accordingly.
     */
    private void setup(Intent intent) {
        mAppTag = intent.getParcelableExtra(EXTRA_TAG);

        if (mAppTag == null) {
            finish();
            return;
        }

        mEditText.setText(mAppTag.title);
        mExpandByDefault.setChecked(mAppTag.defaultExpanded);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setup(intent);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.accept) {
            CharSequence text = mEditText.getText();
            if (TextUtils.isEmpty(text)) {
                mEditText.setError(getString(R.string.tag_error_empty));
                return;
            }
            boolean exists = exists(text);
            if (exists) {
                mEditText.setError(getString(R.string.tag_already_exists, text));
                return;
            }

            mOkButton.setEnabled(false);
            mCancelButton.setEnabled(false);
            mQueryHandler = new QueryHandler(getContentResolver());
            mQueryHandler.updateTag(text, mAppTag, mExpandByDefault.isChecked());
        } else if (id == R.id.cancel) {
            finish();
        }
    }

    boolean exists(CharSequence text) {
        if (mAppTags == null) return false;
        int count = mAppTags.size();
        for (int i = 0; i < count; i++) {
            AppTag tag = mAppTags.get(i);
            if (tag.id != mAppTag.id) {
                if (TextUtils.equals(tag.title, text)) return true;
            }
        }
        return false;
    }

    @Override
    public void onTagsChanged(ArrayList<AppTag> appTags) {
        mAppTags = appTags;
    }

    void onUpdateComplete() {
        finish();
    }

    class QueryHandler extends AsyncQueryHandler {

        private final int TOKEN_TAG = 0;

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        public void updateTag(CharSequence tagName, AppTag appTag,
                boolean expandByDefault) {
            Uri uri = ContentUris.withAppendedId(AppsContract.TagColumns.CONTENT_URI, appTag.id);

            ContentValues contentValues = new ContentValues();
            contentValues.put(AppsContract.TagColumns.NAME, String.valueOf(tagName));
            contentValues.put(AppsContract.TagColumns.DEFAULT_EXPANDED, expandByDefault ? 1 : 0);
            startUpdate(TOKEN_TAG, appTag, uri, contentValues, null, null);
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            if (token == TOKEN_TAG) {
                EditTagActivity.this.onUpdateComplete();
            }
        }
    }

}
