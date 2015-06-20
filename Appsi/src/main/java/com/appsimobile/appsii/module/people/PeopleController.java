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

package com.appsimobile.appsii.module.people;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.LoaderManager;
import com.appsimobile.appsii.PageController;
import com.appsimobile.appsii.PermissionDeniedException;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.AppsiPreferences;
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.appsii.module.PeopleCache;
import com.appsimobile.appsii.module.PermissionHelper;
import com.appsimobile.appsii.module.ToolbarScrollListener;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.google.android.gms.common.annotation.KeepName;

/**
 * Created by nick on 25/05/14.
 */
public class PeopleController extends PageController
        implements LoaderManager.LoaderCallbacks<PeopleLoaderResult>,
        PeopleViewHolder.OnItemClickListener, ContactView.PeopleActionListener,
        PermissionHelper.PermissionListener {

    private static final int PEOPLE_LOADER = 7001;

    String mCurFilter;

    private RecyclerView mPeopleGrid;

    private SharedPreferences mSharedPreferences;

    private PeopleAdapter mPeopleAdapter;

    private Toolbar mToolbar;

    private LetterItemDecoration mLetterItemDecoration;

    boolean mPendingPermissionError;

    public PeopleController(Context context, String title) {
        super(context, title);
    }

    ViewGroup mPermissionOverlay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_contacts, container, false);
    }

    @Override
    protected void onViewDestroyed(View view) {

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        int displayMode =
                AppsiPreferences.ModulePreferences.Apps.getDisplayMode(mSharedPreferences);
        boolean displayAsList = displayMode == AppsiPreferences.ModulePreferences.VIEW_TYPE_LIST;

        mPeopleGrid = (RecyclerView) view.findViewById(R.id.contacts_recycler);
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(mTitle);
        mPermissionOverlay = (ViewGroup) view.findViewById(R.id.permission_overlay);
        setToolbarBackgroundAlpha(0);

        // TODO: define these in integers.xml
        if (displayAsList) {
            mPeopleGrid.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            mPeopleGrid.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        mLetterItemDecoration = new LetterItemDecoration(getContext());
        mPeopleGrid.addItemDecoration(mLetterItemDecoration);

        mPeopleGrid.addOnScrollListener(new ToolbarScrollListener(this, mToolbar));

        mPeopleGrid.setAdapter(mPeopleAdapter);

        if (mPendingPermissionError) {
            showPermissionError();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onUserVisible() {
        super.onUserVisible();
        trackPageView(AnalyticsManager.CATEGORY_PEOPLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mPeopleAdapter = new PeopleAdapter(this, getContext());
        mPeopleAdapter.setOnItemClickListener(this);
        getLoaderManager().initLoader(PEOPLE_LOADER, null, this);
    }

    @Override
    protected void onFirstLayout() {
        super.onFirstLayout();
        // TODO: set the offset from the listener..
        mLetterItemDecoration.setOffset(mToolbar.getHeight() + mToolbar.getPaddingTop());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (mPeopleAdapter != null) {
            mPeopleAdapter.onTrimMemory(level);
        }
        PeopleCache.getInstance().onTrimMemory();
//        if (mPeopleGrid != null) {
//            mPeopleGrid.onTrimMemory(level);
//        }
    }

    @Override
    protected void applyToolbarColor(int color) {
        mToolbar.setBackgroundColor(color);
    }


    void updateToolbarAlpha() {
        // this can happen when the page is recycled, in that case the load
        // completes instantly and we end up here with a null recycler view.
        if (mPeopleGrid == null) return;
        View firstChild = mPeopleGrid.getChildAt(0);
        boolean pxVisible =
                firstChild == null || mPeopleGrid.getChildLayoutPosition(firstChild) == 0;

        if (pxVisible) {
            float pct = mPeopleAdapter.getHeaderScrollPercentage();
            setToolbarBackgroundAlpha(pct);
        } else {
            setToolbarBackgroundAlpha(.001f);
        }
    }

    // this is called by an animator and is required!!
    @KeepName
    public void setToolbarY(int y) {
        mToolbar.setTranslationY(y);
        int offset = mToolbar.getHeight() + y;
        mLetterItemDecoration.setOffset(offset + mToolbar.getPaddingTop());
    }

    @Override
    public Loader<PeopleLoaderResult> onCreateLoader(int id, Bundle args) {
        return new PeopleLoader(getContext());
    }
    @Override
    public void onLoadFinished(Loader<PeopleLoaderResult> loader, PeopleLoaderResult data) {
        if (data.mPermissionDeniedException != null) {
            onPermissionDenied(data.mPermissionDeniedException);
        } else {
            mPeopleAdapter.setData(data.mResult);
            if (mPermissionOverlay != null) {
                mPermissionOverlay.removeAllViews();
            }
        }
        updateToolbarAlpha();
    }


    @Override
    public void onAccepted(PermissionHelper permissionHelper) {
        Intent intent = PermissionUtils.
                buildRequestPermissionsIntent(getContext(),
                        PermissionUtils.REQUEST_CODE_PERMISSION_READ_CONTACTS,
                        permissionHelper.getPermissions());

        getContext().startActivity(intent);

    }

    @Override
    public void onCancelled(PermissionHelper permissionHelper, boolean dontShowAgain) {
        // this option is not available so this method won't be called

    }


    private void onPermissionDenied(PermissionDeniedException permissionDeniedException) {
        if (mPermissionOverlay == null) {
            mPendingPermissionError = true;
        } else {
            showPermissionError();
        }
    }

    private void showPermissionError() {
        mPendingPermissionError = false;
        PermissionHelper permissionHelper = new PermissionHelper(
                R.string.permission_reason_contacts,
                false, this, Manifest.permission.READ_CONTACTS);

        permissionHelper.show(mPermissionOverlay);
    }


    @Override
    public void onLoaderReset(Loader<PeopleLoaderResult> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mPeopleAdapter.clear();

    }


    @Override
    public void onItemClick(AbstractPeopleViewHolder viewHolder) {
        PeopleViewHolder holder = (PeopleViewHolder) viewHolder;
        int position = holder.getLayoutPosition();
        BaseContactInfo info = mPeopleAdapter.getItem(position);
        Uri uri = info.mContactLookupUri;
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        track(AnalyticsManager.ACTION_OPEN_ITEM, AnalyticsManager.CATEGORY_PEOPLE);
        getContext().startActivity(intent);

    }

    @Override
    public void onEditSelected(BaseContactInfo contactInfo) {
        Uri uri = contactInfo.mContactLookupUri;
        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
        track(AnalyticsManager.ACTION_EDIT_ITEM, AnalyticsManager.CATEGORY_PEOPLE);
        getContext().startActivity(intent);
    }
}