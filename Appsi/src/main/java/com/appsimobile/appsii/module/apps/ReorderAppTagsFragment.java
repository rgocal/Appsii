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
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppsModule;
import com.appsimobile.appsii.module.BaseListAdapter;
import com.appsimobile.appsii.module.ViewHolder;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.Updatable;
import com.mobeta.android.dslv.ConditionalRemovableAdapter;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by nick on 31/08/14.
 */
public class ReorderAppTagsFragment extends Fragment implements DragSortListView.RemoveListener,
        DragSortListView.DropListener, Receiver<List<AppTag>>, Updatable {

    /**
     * The list-view used to re-order the tags. We register a few listeners on the
     * list to get updates when items are re-ordered or removed
     */
    DragSortListView mDragSortListView;

    /**
     * The adapter applied to the list-view.
     */
    TagAdapter mTagAdapter;

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

    @Inject
    @Named(AppsModule.NAME_APPS_TAGS)
    Repository<Result<List<AppTag>>> mTagsRepository;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // create the adapter here to make sure we can set it's data and register it's listener
        // This ensures the life cycles are the same
        mTagAdapter = new TagAdapter();
        Result<List<AppTag>> appTags = mTagsRepository.get();
        appTags.ifSucceededSendTo(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        mTagsRepository.addUpdatable(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mTagsRepository.removeUpdatable(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reorder_app_tags, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDragSortListView = (DragSortListView) view.findViewById(R.id.sort_list_view);
        mDragSortListView.setDropListener(this);
        mDragSortListView.setRemoveListener(this);
        mDragSortListView.setAdapter(mTagAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // frees the listener on the adapter
        mDragSortListView.setAdapter(null);
    }

    @Override
    public void remove(int i) {
        mIsChangeInProgress = true;
        AppTag item = mTagAdapter.getItem(i);
        mTagAdapter.removeItem(i);
        doRemoveTag(item);
    }

    void doRemoveTag(AppTag item) {
        // We only create the query-handler if it is really needed. So create
        // it when null
        if (mQueryHandler == null) {
            mQueryHandler = new QueryHandler(getActivity().getContentResolver());
        }
        mQueryHandler.removeTag(item);
    }

    @Override
    public void drop(int from, int to) {
        if (from != to) {
            mIsChangeInProgress = true;
            doUpdateOrder(mTagAdapter.getItems(), from, to);
        }

    }

    /**
     * Starts an update in the order. The order in the database is changed to
     * reflect the order in the given list with the item at position from
     * changed to 'to'.
     */
    private void doUpdateOrder(List<AppTag> items, int from, int to) {
        // We only create the query-handler if it is really needed. So create
        // it when null
        if (mQueryHandler == null) {
            mQueryHandler = new QueryHandler(getActivity().getContentResolver());
        }
        // create a new list, make this list the same size as the original one,
        // and apply the change to it.
        int count = items.size();
        List<AppTag> newOrdering = new ArrayList<>(count);

        newOrdering.addAll(items);
        AppTag moved = newOrdering.remove(from);
        newOrdering.add(to, moved);

        // now update the adapter to make sure it already reflects the change
        mTagAdapter.setItems(newOrdering);

        // start the update on the database
        mQueryHandler.updateOrdering(newOrdering);
    }

    void onDeleteFinished() {
        mIsChangeInProgress = false;
    }

    void onUpdateFinished() {
        mIsChangeInProgress = false;
    }

    @Override
    public void accept(@NonNull List<AppTag> value) {
        if (!mIsChangeInProgress) {
            mTagAdapter.setItems(value);
        }
    }

    @Override
    public void update() {
        mTagsRepository.get().ifSucceededSendTo(this);
    }

    public static class TagViewHolder extends ViewHolder {

        final TextView mTextView;

        public TagViewHolder(View view) {
            super(view);
            mTextView = (TextView) view.findViewById(R.id.tag_title);
        }
    }

    public class TagAdapter extends BaseListAdapter<AppTag, TagViewHolder> implements
            ConditionalRemovableAdapter {

        @Override
        protected long getItemId(AppTag appTag) {
            return appTag.id;
        }

        @Override
        protected TagViewHolder newViewHolder(LayoutInflater inflater, ViewGroup parent) {
            View view = inflater.inflate(R.layout.list_item_tag, parent, false);
            return new TagViewHolder(view);
        }

        @Override
        protected void bindViewHolder(AppTag item, TagViewHolder holder) {
            holder.mTextView.setText(item.title);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean canRemove(int pos) {
            AppTag tag = getItem(pos);
            return tag.tagType == AppsContract.TagColumns.TAG_TYPE_USER;
        }
    }

    public class QueryHandler extends AsyncQueryHandler {

        static final int DELETE_APPS_TOKEN = 0;

        static final int DELETE_TAG_TOKEN = 1;

        private int mUpdatingCount;

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        public void updateOrdering(List<AppTag> newOrdering) {
            int size = newOrdering.size();
            mUpdatingCount = size;
            for (int i = 0; i < size; i++) {
                ContentValues values = new ContentValues();
                values.put(AppsContract.TagColumns.POSITION, i);
                AppTag tag = newOrdering.get(i);
                Uri uri = ContentUris.withAppendedId(AppsContract.TagColumns.CONTENT_URI, tag.id);
                startUpdate(i, tag, uri, values, null, null);
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            super.onUpdateComplete(token, cookie, result);
            mUpdatingCount--;
            if (mUpdatingCount == 0) {
                onUpdateFinished();
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            if (token == DELETE_APPS_TOKEN) {
                // The apps contained in this tag have been removed.
                // Now remove the tag itself.
                AppTag tag = (AppTag) cookie;
                Uri uri = ContentUris.withAppendedId(AppsContract.TagColumns.CONTENT_URI, tag.id);
                startDelete(1, tag, uri, null, null);
            } else {
                onDeleteFinished();
            }
        }

        public void removeTag(AppTag tag) {
            // first remove all apps that are using this tag,
            // this uses token DELETE_APPS_TOKEN. Once this
            // is done, the tag itself must be deleted
            startDelete(DELETE_APPS_TOKEN, tag,
                    AppsContract.TaggedAppColumns.CONTENT_URI,
                    AppsContract.TaggedAppColumns.TAG_ID + "=?",
                    new String[]{String.valueOf(tag.id)});
        }
    }

}
