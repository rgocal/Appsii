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

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.module.BaseListAdapter;
import com.appsimobile.appsii.module.ViewHolder;
import com.mobeta.android.dslv.ConditionalRemovableAdapter;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by nick on 31/08/14.
 */
public class ReorderAppsFragment extends Fragment implements DragSortListView.RemoveListener,
        DragSortListView.DropListener, AdapterView.OnItemClickListener {

    /**
     * The list-view used to re-order the tags. We register a few listeners on the
     * list to get updates when items are re-ordered or removed
     */
    DragSortListView mDragSortListView;

    /**
     * The adapter applied to the list-view.
     */
    AppsAdapter mAppsAdapter;

    /**
     * True when we are updating the database. While the database is being updated,
     * we ignore changes to the tags and don't update the underlying list to prevent
     * flickering.
     */
    boolean mIsChangeInProgress;

    /**
     * The handler used to apply the user's changes
     */
    @Nullable
    QueryHandler mQueryHandler;

    RetainQueryHelperFragment mRetainFragment;
    @Inject
    LauncherAppsCompat mLauncherAppsCompat;
    private AppTag mAppTag;

    public static Fragment createInstance(AppTag appTag) {
        ReorderAppsFragment result = new ReorderAppsFragment();
        Bundle args = new Bundle();
        args.putParcelable("tag", appTag);

        result.setArguments(args);
        return result;
    }

    @Override
    public void onStart() {
        super.onStart();
        mRetainFragment.setAdapter(mAppsAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);
        // create the adapter here to make sure we can set it's data and register it's listener
        // This ensures the life cycles are the same
        mAppsAdapter = new AppsAdapter();

        // now get the tag we need to display the data from
        if (savedInstanceState != null) {
            mAppTag = savedInstanceState.getParcelable("tag");
        }

        // see if the retain fragment is already present. Otherwise create it.
        // the key has to be unique because multiple pages may be added by the pager.
        String key = "retain";
        mRetainFragment =
                (RetainQueryHelperFragment) getFragmentManager().findFragmentByTag(key);

        // when the fragment was null, create it and add it.
        if (mRetainFragment == null) {
            mRetainFragment = new RetainQueryHelperFragment();
            getFragmentManager().beginTransaction().add(mRetainFragment, key).commit();
        }

        // now that the fragment is added, initialize the loader.
        getLoaderManager().initLoader(100, null, new AllAppsLoaderCallbacks());

        if (mAppTag != null) {
            mRetainFragment.loadTaggedApps(mAppTag.id);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mRetainFragment.setAdapter(null);
    }

    @Override
    public void remove(int i) {
        mIsChangeInProgress = true;
        TaggedApp item = mAppsAdapter.getItem(i);
        doRemoveTaggedApp(item);
        // set this after removal. Otherwise the query-handler does
        // not know if it is a newly deleted item or not.
        // The queryHandler uses the mDeleted field to know if it is
        // a second delete
        if (item.mDeleted) {
            mAppsAdapter.removeItem(i);
        } else {
            item.mDeleted = true;
            mAppsAdapter.notifyDataSetChanged();
        }
    }

    void doRemoveTaggedApp(TaggedApp item) {
        // We only create the query-handler if it is really needed. So create
        // it when null
        if (mQueryHandler == null) {
            mQueryHandler = new QueryHandler(getActivity().getContentResolver());
        }
        mQueryHandler.removeTaggedApp(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("tag", mAppTag);
    }

    @Override
    public void drop(int from, int to) {
        if (from != to) {
            mIsChangeInProgress = true;
            doUpdateOrder(mAppsAdapter.getItems(), from, to);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reorder_app_tags, container, false);
    }

    /**
     * Starts an update in the order. The order in the database is changed to
     * reflect the order in the given list with the item at position from
     * changed to 'to'.
     */
    private void doUpdateOrder(List<TaggedApp> items, int from, int to) {
        // We only create the query-handler if it is really needed. So create
        // it when null
        if (mQueryHandler == null) {
            mQueryHandler = new QueryHandler(getActivity().getContentResolver());
        }
        // create a new list, make this list the same size as the original one,
        // and apply the change to it.
        int count = items.size();
        List<TaggedApp> newOrdering = new ArrayList<>(count);

        newOrdering.addAll(items);
        TaggedApp moved = newOrdering.remove(from);
        newOrdering.add(to, moved);

        // now update the adapter to make sure it already reflects the change
        mAppsAdapter.setItems(newOrdering);

        // start the update on the database
        mQueryHandler.updateOrdering(newOrdering);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDragSortListView = (DragSortListView) view.findViewById(R.id.sort_list_view);
        mDragSortListView.setDropListener(this);
        mDragSortListView.setRemoveListener(this);
        mDragSortListView.setAdapter(mAppsAdapter);
        mDragSortListView.setOnItemClickListener(this);
    }

    public void setAppTag(AppTag appTag) {
        mAppTag = appTag;
        mRetainFragment.loadTaggedApps(appTag.id);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TaggedApp app = mAppsAdapter.getItem(position);
        if (app.mDeleted) {
            if (mQueryHandler == null) {
                mQueryHandler = new QueryHandler(getActivity().getContentResolver());
            }
            mQueryHandler.undeleteTaggedApp(app);
            app.mDeleted = false;
            mAppsAdapter.notifyDataSetChanged();
        }
    }

    void onDeleteFinished() {
        mIsChangeInProgress = false;
    }

    void onUpdateFinished() {
        mIsChangeInProgress = false;
    }

    void onAllAppsLoaded(List<AppEntry> entries) {
        mRetainFragment.setAllApps(entries);
    }

    public static class TaggedAppViewHolder extends ViewHolder {

        final TextView mTextView;

        final View mDragHandle;

        final View mUndoDelete;
        final Context mContext;
        AppIconLoaderTaskImpl mAppIconLoaderTask;

        public TaggedAppViewHolder(View view) {
            super(view);
            mContext = view.getContext();
            mTextView = (TextView) view.findViewById(R.id.tag_title);
            mDragHandle = view.findViewById(R.id.drag_handle);
            mUndoDelete = view.findViewById(R.id.undo_delete_text);
        }

        public void loadIcon(Context context, TaggedApp item) {
            if (mAppIconLoaderTask != null) {
                mAppIconLoaderTask.cancel(true);
            }
            mAppIconLoaderTask =
                    new AppIconLoaderTaskImpl(mContext, item.mAppEntry,
                            context.getPackageManager());
            mAppIconLoaderTask.execute();
        }

        /**
         * When true, this item was just removed. We display an undo text
         * to restore the view
         */
        void setLeaveBehindVisible(boolean visible) {
            if (visible) {
                mUndoDelete.setVisibility(View.VISIBLE);
                mTextView.setVisibility(View.GONE);
                mDragHandle.setVisibility(View.GONE);
            } else {
                mUndoDelete.setVisibility(View.GONE);
                mTextView.setVisibility(View.VISIBLE);
                mDragHandle.setVisibility(View.VISIBLE);
            }
        }

        void onIconLoaded(Drawable drawable) {
            int dimen = (int) (mContext.getResources().getDisplayMetrics().density * 42);
            drawable.setBounds(0, 0, dimen, dimen);
            mTextView.setCompoundDrawables(drawable, null, null, null);
        }

        class AppIconLoaderTaskImpl extends AppIconLoaderTask {

            AppIconLoaderTaskImpl(Context context, AppEntry appEntry,
                    PackageManager packageManager) {
                super(context, appEntry, packageManager);
            }

            @Override
            protected void onPostExecute(Drawable drawable) {
                onIconLoaded(drawable);
            }
        }

    }

    /**
     * A simple fragment that will retain some of the loading state to make it quicker to
     * restore the state of the adapter
     */
    public static class RetainQueryHelperFragment extends Fragment {

        final SimpleArrayMap<ComponentName, AppEntry> mAppsByComponent = new SimpleArrayMap<>();

        List<AppEntry> mAllApps;

        List<TaggedApp> mTaggedApps;

        long mAppTagId = Long.MIN_VALUE;

        QueryHandler mQueryHandler;

        AppsAdapter mAppsAdapter;

        boolean mLoadRequested;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            mQueryHandler = new QueryHandler(getActivity().getContentResolver());
        }

        public void setAdapter(AppsAdapter adapter) {
            mAppsAdapter = adapter;
            if (adapter != null) {
                if (mTaggedApps != null) {
                    mAppsAdapter.setItems(mTaggedApps);
                } else {
                    mAppsAdapter.clear();
                }
            }
        }

        public void setAllApps(List<AppEntry> allApps) {
            mAllApps = allApps;
            mAppsByComponent.clear();

            int size = allApps.size();
            for (int i = 0; i < size; i++) {
                AppEntry e = allApps.get(i);
                mAppsByComponent.put(e.getComponentName(), e);
            }
            loadIfNeeded();
        }

        private void loadIfNeeded() {
            if (mLoadRequested) {
                if (mAllApps != null) {
                    mQueryHandler.loadTaggedApps();
                    mLoadRequested = false;
                }
            }
        }

        void loadTaggedApps(long tagId) {
            if (mAppTagId != tagId) {
                mAppTagId = tagId;
                if (mAllApps == null) {
                    mLoadRequested = true;
                } else {
                    mQueryHandler.loadTaggedApps();
                }
            }
        }

        void setAppsInTag(List<TaggedApp> result) {
            mTaggedApps = result;
            if (mAppsAdapter != null) {
                mAppsAdapter.setItems(result);
            }
        }

        class QueryHandler extends AsyncQueryHandler {

            int mActiveToken = 0;

            public QueryHandler(ContentResolver cr) {
                super(cr);
            }

            void loadTaggedApps() {
                cancelOperation(mActiveToken);
                mActiveToken++;
                startQuery(mActiveToken, null,
                        ContentUris.withAppendedId(
                                AppsContract.TaggedAppColumns.CONTENT_URI, mAppTagId),
                        AppQuery.PROJECTION,
                        null, null, AppQuery.ORDER);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                // only handle the result when the token was not changed
                if (token == mActiveToken) {
                    List<TaggedApp> result = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        String comp = cursor.getString(AppQuery.COMPONENT_NAME);
                        ComponentName componentName = ComponentName.unflattenFromString(comp);
                        AppEntry appEntry = mAppsByComponent.get(componentName);

                        if (appEntry == null) continue;

                        TaggedApp app = new TaggedApp();

                        app.mId = cursor.getLong(AppQuery._ID);
                        app.mTagId = cursor.getLong(AppQuery.TAG_ID);
                        app.mTagName = cursor.getString(AppQuery.TAG_NAME);
                        app.mAppEntry = appEntry;
                        app.mComponentName = componentName;

                        result.add(app);
                    }
                    setAppsInTag(result);
                }
            }
        }


    }

    public class AppsAdapter extends BaseListAdapter<TaggedApp, TaggedAppViewHolder> implements
            ConditionalRemovableAdapter {

        @Override
        protected long getItemId(TaggedApp appTag) {
            return appTag.mId;
        }

        @Override
        protected TaggedAppViewHolder newViewHolder(LayoutInflater inflater, ViewGroup parent) {
            View view = inflater.inflate(R.layout.list_item_tagged_app, parent, false);
            return new TaggedAppViewHolder(view);
        }

        @Override
        protected void bindViewHolder(TaggedApp item, TaggedAppViewHolder holder) {
            holder.mTextView.setText(item.mAppEntry.getLabel());
            holder.loadIcon(holder.mTextView.getContext(), item);
            holder.setLeaveBehindVisible(item.mDeleted);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            // when the item is deleted, clicking it is enabled
            return getItem(position).mDeleted;
        }

        @Override
        public boolean canRemove(int pos) {
            return true;
        }
    }

    public class QueryHandler extends AsyncQueryHandler {

        private int mUpdatingCount;

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        public void updateOrdering(List<TaggedApp> newOrdering) {
            int size = newOrdering.size();
            mUpdatingCount = size;
            for (int i = 0; i < size; i++) {
                ContentValues values = new ContentValues();
                values.put(AppsContract.TagColumns.POSITION, i);
                TaggedApp tag = newOrdering.get(i);
                Uri uri = ContentUris.withAppendedId(AppsContract.TaggedAppColumns.CONTENT_URI,
                        tag.mId);
                startUpdate(i, tag, uri, values, null, null);
            }
        }

        public void undeleteTaggedApp(TaggedApp app) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(AppsContract.TaggedAppColumns.DELETED, 0);

            startUpdate(2, app,
                    ContentUris.withAppendedId(AppsContract.TaggedAppColumns.CONTENT_URI, app.mId),
                    contentValues,
                    null,
                    null);

        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            if (token == 0) {
                mUpdatingCount--;
                if (mUpdatingCount == 0) {
                    onUpdateFinished();
                }
            } else if (token == 1) {
                onDeleteFinished();
            } else if (token == 2) {
                // undelete finished
            }
        }

        public void removeTaggedApp(TaggedApp app) {
            startDelete(0, null,
                    AppsContract.TaggedAppColumns.CONTENT_URI, AppQuery.WHERE_DELETED, null);
            if (app.mDeleted) return;

            ContentValues contentValues = new ContentValues();
            contentValues.put(AppsContract.TaggedAppColumns.DELETED, 1);

            startUpdate(1, app,
                    ContentUris.withAppendedId(AppsContract.TaggedAppColumns.CONTENT_URI, app.mId),
                    contentValues,
                    null,
                    null);
        }


    }

    class AllAppsLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<AppEntry>> {

        @Override
        public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args) {
            return new AppListLoader(getActivity(), mLauncherAppsCompat);
        }

        @Override
        public void onLoadFinished(Loader<List<AppEntry>> loader, List<AppEntry> data) {
            onAllAppsLoaded(data);
        }

        @Override
        public void onLoaderReset(Loader<List<AppEntry>> loader) {

        }
    }


}
