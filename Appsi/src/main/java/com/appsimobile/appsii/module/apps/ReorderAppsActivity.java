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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.BaseListAdapter;
import com.appsimobile.appsii.module.ViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 19/09/14.
 */
public class ReorderAppsActivity extends Activity implements AppTagUtils.AppTagListener,
        AdapterView.OnItemSelectedListener {

    public static final String EXTRA_PRESELECT_TAG_ID =
            BuildConfig.APPLICATION_ID + ".preselect_tag_id";

    /**
     * The view-pager containing each of the tabs (tags) the user created
     */
    SpinnerAdapter mSpinnerAdapter;

    Spinner mSpinner;

    ReorderAppsFragment mReorderAppsFragment;

    long mPreselectId;

    boolean mPreselected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // register the app-tag listener, and update the list in the actionbar.
        setContentView(R.layout.activity_reorder_apps);

        mSpinner = (Spinner) findViewById(R.id.spinner);

        mSpinnerAdapter = new SpinnerAdapter();
        mSpinner.setAdapter(mSpinnerAdapter);

        mReorderAppsFragment = (ReorderAppsFragment) getFragmentManager().
                findFragmentById(R.id.reorder_apps_fragment);

        mSpinner.setOnItemSelectedListener(this);

        mPreselectId = getIntent().getLongExtra(EXTRA_PRESELECT_TAG_ID, -1);

        if (savedInstanceState != null) {
            mPreselected = savedInstanceState.getBoolean("preselected");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        List<AppTag> tags = AppTagUtils.getInstance(this).registerAppTagListener(this);
        mSpinnerAdapter.setItems(tags);
        preselectSelection(tags);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("preselected", mPreselected);
    }

    @Override
    public void onStop() {
        super.onStop();
        AppTagUtils.getInstance(this).unregisterAppTagListener(this);
    }

    private void preselectSelection(List<AppTag> appTags) {
        if (!mPreselected) {
            mPreselected = true;
            int idx = 0;
            for (AppTag appTag : appTags) {
                if (appTag.id == mPreselectId) {
                    mSpinner.setSelection(idx);
                    break;
                }
                if (appTag.tagType == AppsContract.TagColumns.TAG_TYPE_USER) {
                    idx++;
                }
            }
        }
    }

    @Override
    public void onTagsChanged(List<AppTag> appTags) {
        mSpinnerAdapter.setItems(appTags);
        preselectSelection(appTags);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        AppTag appTag = mSpinnerAdapter.getItem(position);
        mReorderAppsFragment.setAppTag(appTag);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    static class SpinnerAdapter extends BaseListAdapter<AppTag, SpinnerViewHolder> {

        private final List<AppTag> mAppTags = new ArrayList<>();

        @Override
        protected long getItemId(AppTag item) {
            return item.id;
        }

        @Override
        protected SpinnerViewHolder newViewHolder(LayoutInflater inflater,
                ViewGroup parent) {
            View view = inflater.inflate(R.layout.list_item_tag_spinner, parent, false);
            return new SpinnerViewHolder(view);
        }

        @Override
        protected void bindViewHolder(AppTag item, SpinnerViewHolder holder) {
            holder.mTextView.setText(item.title);
        }

        @Override
        public void setItems(List<AppTag> items) {
            // we keep this in our own (temp) list
            // because we only want to display the user type tags
            mAppTags.clear();
            for (AppTag tag : items) {
                if (tag.tagType == AppsContract.TagColumns.TAG_TYPE_USER) {
                    mAppTags.add(tag);
                }
            }
            super.setItems(mAppTags);
        }

    }

    static class SpinnerViewHolder extends ViewHolder {

        final TextView mTextView;

        public SpinnerViewHolder(View view) {
            super(view);
            mTextView = (TextView) view.findViewById(R.id.tag_title);
        }
    }

}
