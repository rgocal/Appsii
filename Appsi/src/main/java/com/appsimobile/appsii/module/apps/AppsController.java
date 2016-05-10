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

import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.PageController;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.annotation.VisibleForTesting;
import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.compat.UserHandleCompat;
import com.appsimobile.appsii.dagger.AppsModule;
import com.appsimobile.appsii.module.ToolbarScrollListener;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.Updatable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * The controller for the apps page. Shows the app from the different folders
 * on the apps page.
 * Created by nick on 25/05/14.
 */
public class AppsController extends PageController
        implements View.OnClickListener, AppView.AppActionListener,
        Toolbar.OnMenuItemClickListener, AppView.TagActionListener, Receiver<AppPageData>,
        Updatable {


    /**
     * A loader id for the all-apps list.
     */
    @VisibleForTesting
    static final int APPS_LOADER_ID = 5001;

    /**
     * The list-view used to display all of the items in.
     */
    RecyclerView mRecyclerView;

    /**
     * The adapter used to display the apps in the
     * list-view
     */
    AppsAdapter mAppsAdapter;

    /**
     * The last saved amount of alpha to be applied to
     * the app-bar
     */
    float mLastActionBarAlpha;

    /**
     * A query handler used to load the app tags and
     * tagged-apps from
     */
    QueryHandler mQueryHandler;

    int mColumnCount;

    @Inject
    @Named(AppsModule.NAME_APPS)
    Repository<Result<AppPageData>> mAppPageRepository;

    /**
     * A handler for the content observer to post the event into
     */
    private Handler mHandler;

    /**
     * The toolbar for this page
     */
    private Toolbar mToolbar;

    /**
     * The layout-manager that is being used in the recycler-view
     */
    private GridLayoutManager mLayoutManager;

    /**
     * A helper class providing access to the bottom sheet functionality
     */
    private BottomSheetHelper mBottomSheetHelper;

    public AppsController(Context context, String title) {
        super(context, title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_apps, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.apps_recycler_view);

        mBottomSheetHelper.onViewCreated(view);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        int defaultColumnCount = getResources().getInteger(R.integer.default_app_columns);
        mColumnCount = preferences.getInt("page_apps_column_count", defaultColumnCount);

        mLayoutManager = new GridLayoutManager(getContext(), mColumnCount);
        mLayoutManager.setSpanSizeLookup(new AppsSpanSizeLookup());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new AppDividerDecoration(mAppsAdapter));

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(mTitle);
        setToolbarBackgroundAlpha(0);

        mRecyclerView.addOnScrollListener(new ToolbarScrollListener(this, mToolbar));

        mAppsAdapter.setOnClickListener(this);
        mRecyclerView.setAdapter(mAppsAdapter);

        Menu menu = mToolbar.getMenu();
        menu.clear();
        MenuInflater menuInflater = new MenuInflater(getContext());
        menuInflater.inflate(R.menu.page_apps, menu);

        mToolbar.setOnMenuItemClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // update the visibility and alpha of the toolbar based on the scroll.
        // The scroll is only available after the view-state has been restored.
        //
        // That's why it is handled in onResume; after restoring the state.
        updateToolbarAlpha();
    }

    @Override
    protected void onUserVisible() {
        // when the app becomes visible to the user, track a page-view
        trackPageView(AnalyticsManager.CATEGORY_APPS);
        track(AnalyticsManager.ACTION_OPEN_PAGE, AnalyticsManager.CATEGORY_APPS);
    }

    @Override
    protected void onUserInvisible() {
        super.onUserInvisible();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        component().inject(this);
        mBottomSheetHelper = new BottomSheetHelper(getContext(), this /* appController */);
        mAppsAdapter = new AppsAdapter(this /* appActionListener */, this /* tagActionListener */);

        mHandler = new Handler();

        ContentResolver contentResolver = getContext().getContentResolver();
        mQueryHandler = new QueryHandler(contentResolver);

        mAppPageRepository.get().ifSucceededSendTo(this);
    }


    @Override
    protected void onDetach() {
        super.onDetach();
    }

    @Override
    protected void onFirstLayout() {
        super.onFirstLayout();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // when this is called, try to clean up as much memory as possible
        if (mAppsAdapter != null) {
            mAppsAdapter.onTrimMemory(level);
        }
    }

    @Override
    protected void applyToolbarColor(int color) {
        mToolbar.setBackgroundColor(color);
    }

//    @Override
//    public void hideToolbar() {
//        ControllerUtils.hideToolbar(this, mToolbar);
//    }
//
//    @Override
//    public void showToolbar() {
//        ControllerUtils.showToolbar(this, mToolbar);
//        updateToolbarAlpha();
//    }

    void updateToolbarAlpha() {
        View firstChild = mRecyclerView.getChildAt(0);
        boolean pxVisible =
                firstChild == null || mRecyclerView.getChildLayoutPosition(firstChild) == 0;

        if (pxVisible) {
            float pct = mAppsAdapter.getHeaderScrollPercentage();
            setToolbarBackgroundAlpha(pct);
        } else {
            setToolbarBackgroundAlpha(.001f);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAppPageRepository.addUpdatable(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAppPageRepository.removeUpdatable(this);
    }

    @Override
    public void onClick(View v) {

        AppView view = (AppView) v;
        AppEntry app = view.getAppEntry();

        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(getContext());
        launcherApps.startActivityForProfile(app.getComponentName(),
                UserHandleCompat.myUserHandle(),
                null,
                null);
        // track a launch of the app in the app history
        ComponentName componentName = app.getComponentName();
        AppHistoryUtils.trackAppLaunch(getContext(), componentName);

        // track in google analytics
        track(AnalyticsManager.ACTION_OPEN_ITEM, AnalyticsManager.CATEGORY_APPS,
                app.getComponentName().flattenToShortString());
    }

    @Override
    public void onEditAppliedTags(AppEntry entry, List<AppTag> allTags,
            List<TaggedApp> appliedTags) {
        mBottomSheetHelper.show(entry, appliedTags);
    }

    /**
     * Adds an app to a tag, this is called when the user performs an action to
     * add an app to a tag.
     */
    public void onAddAppToTag(AppTag tag, AppEntry entry) {
        List<AppEntry> appsInTag = mAppsAdapter.getAppsInTag(tag);
        // determine the right index, this is 0 when the tag is empty
        // otherwise make it the size of the tag
        int idx = 0;
        if (appsInTag != null) {
            idx = appsInTag.size();
        }
        // track the action in the ga tracker
        track(AnalyticsManager.ACTION_ADD_TO_TAG, AnalyticsManager.CATEGORY_APPS,
                entry.getComponentName().flattenToShortString());

        mQueryHandler.addAppToTag(tag, entry.getComponentName(), idx);
    }

    public void onAddAppToNewTag(AppEntry entry) {
        Context context = getContext();
        Intent intent = new Intent(context, AddTagActivity.class);
        intent.putExtra(AddTagActivity.EXTRA_APP_ENTRY, entry.getComponentName());
        context.startActivity(intent);
        track(AnalyticsManager.ACTION_ADD_TAG, AnalyticsManager.CATEGORY_APPS,
                entry.getComponentName().flattenToShortString());
    }

    public void onRemoveAppFromTag(TaggedApp tag) {
        mQueryHandler.removeAppFromTag(tag, tag.mComponentName);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.column_count_2:
                track(AnalyticsManager.ACTION_CHANGE_COLUMN_COUNT, AnalyticsManager.CATEGORY_APPS);
                saveColumnCount(2);
                return true;
            case R.id.column_count_3:
                track(AnalyticsManager.ACTION_CHANGE_COLUMN_COUNT, AnalyticsManager.CATEGORY_APPS);
                saveColumnCount(3);
                return true;
            case R.id.column_count_4:
                track(AnalyticsManager.ACTION_CHANGE_COLUMN_COUNT, AnalyticsManager.CATEGORY_APPS);
                saveColumnCount(4);
                return true;
            case R.id.column_count_5:
                track(AnalyticsManager.ACTION_CHANGE_COLUMN_COUNT, AnalyticsManager.CATEGORY_APPS);
                saveColumnCount(5);
                return true;
            case R.id.action_edit_tags: {
                track(AnalyticsManager.ACTION_EDIT_TAGS, AnalyticsManager.CATEGORY_APPS);
                Context context = getContext();

                Intent intent = new Intent(context, ReorderAppTagsActivity.class);
                context.startActivity(intent);
                return true;
            }
        }
        return false;
    }

    private void saveColumnCount(int columnCount) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        preferences.edit().
                putInt("page_apps_column_count", columnCount).
                apply();
        mColumnCount = columnCount;
        mLayoutManager.setSpanCount(columnCount);
        mAppsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onEditAppTag(AppTag entry) {
        Intent intent = new Intent(getContext(), EditTagActivity.class);
        intent.putExtra(EditTagActivity.EXTRA_TAG, entry);
        getContext().startActivity(intent);
    }

    @Override
    public void onReorderApps(AppTag entry) {
        reorderAppsInTag(entry);
    }

    void reorderAppsInTag(AppTag appTag) {
        track(AnalyticsManager.ACTION_SORT_APPS, AnalyticsManager.CATEGORY_APPS);
        Context context = getContext();

        Intent intent = new Intent(context, ReorderAppsActivity.class);
        intent.putExtra(ReorderAppsActivity.EXTRA_PRESELECT_TAG_ID, appTag.id);
        context.startActivity(intent);

    }

    @Override
    public void onToggleSingleRow(AppTag entry) {
        mQueryHandler.toggleDisplayAsList(entry);
    }

    @Override
    public void accept(@NonNull AppPageData value) {
        mAppsAdapter.setAppPageData(value);
        AppEntry entry = mBottomSheetHelper.getBoundAppEntry();
        if (entry != null) {
            List<TaggedApp> appliedTags = value.mTagsPerComponent.get(entry.getComponentName());
            mBottomSheetHelper.updateAppliedTags(appliedTags);
        }
        mBottomSheetHelper.setAppTags(value.mAppTags);

    }

    @Override
    public void update() {
        mAppPageRepository.get().ifSucceededSendTo(this);
    }

    static abstract class AbstractAppViewHolder extends RecyclerView.ViewHolder {

        public AbstractAppViewHolder(View itemView) {
            super(itemView);
        }

        abstract void bind(Object object);
    }

    protected static class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        public void toggleDisplayAsList(AppTag appTag) {
            int newCount = appTag.columnCount == 1 ? 3 : 1;
            ContentValues values = new ContentValues();
            values.put(AppsContract.TagColumns.COLUMN_COUNT, newCount);
            Uri contentUri = ContentUris.
                    withAppendedId(AppsContract.TagColumns.CONTENT_URI, appTag.id);

            startUpdate(0, appTag, contentUri, values, null, null);
        }

        public void addAppToTag(AppTag tag, ComponentName componentName, int idx) {
            ContentValues values = new ContentValues();
            values.put(AppsContract.TaggedAppColumns.COMPONENT_NAME,
                    componentName.flattenToShortString());
            values.put(AppsContract.TaggedAppColumns.TAG_ID, tag.id);
            values.put(AppsContract.TaggedAppColumns.POSITION, idx);
            startInsert(0, componentName, AppsContract.TaggedAppColumns.CONTENT_URI, values);
        }

        public void removeAppFromTag(TaggedApp tag, ComponentName componentName) {
            Uri appTagUri = ContentUris.withAppendedId(
                    AppsContract.TaggedAppColumns.CONTENT_URI, tag.mId);

            startDelete(0, tag, appTagUri, null, null);
        }
    }


    /**
     * Default implementation for {GridLayoutManager.SpanSizeLookup}. Each item occupies 1 span.
     */
    final class AppsSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

        public AppsSpanSizeLookup() {
            setSpanIndexCacheEnabled(true);
        }

        @Override
        public int getSpanSize(int position) {
            int itemType = mAppsAdapter.getItemViewType(position);
            if (itemType == AppsAdapter.VIEW_TYPE_PARALLAX_HEADER) return mColumnCount;
            if (itemType == AppsAdapter.VIEW_TYPE_HEADER) return mColumnCount;
            if (itemType == AppsAdapter.VIEW_TYPE_NO_RECENT_APPS) return mColumnCount;
            if (itemType == AppsAdapter.VIEW_TYPE_EMPTY_TAG) return mColumnCount;
            if (itemType == AppsAdapter.VIEW_TYPE_APP_SINGLE_ROW) return mColumnCount;
            return 1;
        }
    }


}
