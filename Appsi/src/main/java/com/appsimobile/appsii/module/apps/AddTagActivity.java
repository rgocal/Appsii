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
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.appsimobile.BaseActivity;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppsModule;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.Updatable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by nick on 24/08/14.
 */
public class AddTagActivity extends BaseActivity implements View.OnClickListener,
        Updatable, Receiver<List<AppTag>> {

    public static final String EXTRA_APP_ENTRY = "com.appsimobile.appsii.EXTRA_APP_ENTRY";

    CheckBox mExpandByDefault;

    @Inject
    @Named(AppsModule.NAME_APPS_TAGS)
    Repository<Result<List<AppTag>>> mTagsRepository;

    /**
     * The component that needs to be added to the the tag
     */
    @Nullable
    private ComponentName mComponentName;

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
     * The query handler used to insert the tag and app
     */
    private QueryHandler mQueryHandler;

    private ArrayList<AppTag> mAppTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        component().inject(this);

        setContentView(R.layout.dialog_add_apptag);
        TextView title = (TextView) findViewById(R.id.title);
        TextView content = (TextView) findViewById(R.id.content);

        Intent intent = getIntent();
        mComponentName = intent.getParcelableExtra(EXTRA_APP_ENTRY);

        mOkButton = findViewById(R.id.accept);
        mCancelButton = findViewById(R.id.cancel);

        mOkButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);

        mEditText = (EditText) findViewById(R.id.editText);

        mExpandByDefault = (CheckBox) findViewById(R.id.expand_by_default);

        title.setText(R.string.add_tag);
        content.setText(getString(R.string.tag_name));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTagsRepository.addUpdatable(this);
        mTagsRepository.get().ifSucceededSendTo(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTagsRepository.removeUpdatable(this);
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
            mQueryHandler = new QueryHandler(this, getContentResolver());
            mQueryHandler.insertTag(text, mComponentName, mExpandByDefault.isChecked());
        } else if (id == R.id.cancel) {
            finish();
        }
    }

    boolean exists(CharSequence text) {
        if (mAppTags == null) return false;
        int count = mAppTags.size();
        for (int i = 0; i < count; i++) {
            AppTag tag = mAppTags.get(i);
            if (TextUtils.equals(tag.title, text)) return true;
        }
        return false;
    }

    public void onTagsChanged(List<AppTag> appTags) {
        mAppTags = new ArrayList<>(appTags);
    }

    void onInsertComplete() {
        finish();
    }

    @Override
    public void update() {
        mTagsRepository.get().ifSucceededSendTo(this);
    }

    @Override
    public void accept(@NonNull List<AppTag> value) {
        onTagsChanged(value);
    }

    static class QueryHandler extends AsyncQueryHandler {

        final WeakReference<AddTagActivity> mActivityRef;

        private final int TOKEN_TAG = 0;

        private final int TOKEN_APP = 1;

        public QueryHandler(AddTagActivity activity, ContentResolver cr) {
            super(cr);
            mActivityRef = new WeakReference<>(activity);
        }

        public void insertTag(CharSequence tagName, @Nullable ComponentName componentName,
                boolean expandByDefault) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(AppsContract.TagColumns.NAME, String.valueOf(tagName));
            contentValues.put(AppsContract.TagColumns.VISIBLE, 1);
            contentValues.put(AppsContract.TagColumns.DEFAULT_EXPANDED, expandByDefault ? 1 : 0);
            contentValues.put(AppsContract.TagColumns.POSITION, 0);
            contentValues.put(AppsContract.TagColumns.COLUMN_COUNT, 3);
            startInsert(TOKEN_TAG, componentName, AppsContract.TagColumns.CONTENT_URI,
                    contentValues);
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            AddTagActivity a = mActivityRef.get();
            if (token == TOKEN_TAG) {
                long tagId = ContentUris.parseId(uri);
                ComponentName componentName = (ComponentName) cookie;
                if (componentName != null) {
                    ContentValues values = new ContentValues();
                    values.put(AppsContract.TaggedAppColumns.COMPONENT_NAME,
                            componentName.flattenToShortString());
                    values.put(AppsContract.TaggedAppColumns.TAG_ID, tagId);
                    values.put(AppsContract.TaggedAppColumns.POSITION, 0);
                    startInsert(TOKEN_APP, cookie, AppsContract.TaggedAppColumns.CONTENT_URI,
                            values);
                } else {
                    if (a != null) {
                        a.onInsertComplete();
                    }
                }
            } else if (token == TOKEN_APP) {
                if (a != null) {
                    a.onInsertComplete();
                }
            }
        }
    }

}
