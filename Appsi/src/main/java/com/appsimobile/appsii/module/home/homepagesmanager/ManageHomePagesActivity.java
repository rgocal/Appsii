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

package com.appsimobile.appsii.module.home.homepagesmanager;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.BaseActivity;
import com.appsimobile.appsii.ActivityUtils;
import com.appsimobile.appsii.GotItDismissListener;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.HomeEditorActivity;
import com.appsimobile.appsii.module.home.HomeItemTitleEditDialog;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.preference.PreferencesFactory;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.Updatable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * An activity showing a list of home pages that can be managed through
 * this activity.
 * <p/>
 * Created by nick on 01/02/15.
 */
public class ManageHomePagesActivity extends AppCompatActivity
        implements HomeViewHolder.HomeViewActionListener,
        HomeItemTitleEditDialog.EditTitleDialogListener, Updatable, Receiver<List<HomePageItem>> {

    /**
     * The recycler view.
     */
    RecyclerView mHomePagesRecycler;

    /**
     * The home-pages adapter.
     */
    HomeAdapter mHomeAdapter;

    /**
     * The handler we post queries to.
     */
    AsyncQueryHandlerImpl mAsyncQueryHandler;

    @Inject
    Repository<Result<List<HomePageItem>>> mHomePagesRepository;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BaseActivity.componentFrom(this).inject(this);

        ActivityUtils.setContentViewWithFab(this, R.layout.activity_manage_home_pages);
        ActivityUtils.setupToolbar(this, R.id.toolbar);

        mHomeAdapter = new HomeAdapter(this, this);
        mAsyncQueryHandler = new AsyncQueryHandlerImpl(this, getContentResolver());
        HomeItemTitleEditDialog dialog =
                (HomeItemTitleEditDialog) getFragmentManager().findFragmentByTag("edit_title");
        if (dialog != null) {
            dialog.setEditTitleDialogListener(this);
        }

        View addPageButton = ActivityUtils.setupFab(this, R.id.add_page_button);
        addPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mHomePagesRecycler = (RecyclerView) findViewById(R.id.home_pages_recycler);

        mHomePagesRecycler.setAdapter(mHomeAdapter);
        mHomePagesRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    public void onAddClicked() {
        mAsyncQueryHandler.addEmptyHomePage();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHomePagesRepository.addUpdatable(this);
        mHomePagesRepository.get().ifSucceededSendTo(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHomePagesRepository.removeUpdatable(this);
    }

    @Override
    public void onDeleteSelected(HomePageItem item) {
        mAsyncQueryHandler.removeHomePage(item.mId);
    }

    @Override
    public void onMainAction(HomePageItem item) {
        HomeItemTitleEditDialog dialog =
                HomeItemTitleEditDialog.createDialog(item.mId, item.mTitle);
        dialog.show(getFragmentManager(), "edit_title");
        dialog.setEditTitleDialogListener(this);
    }

    @Override
    public void onChangeLayoutSelected(HomePageItem item) {
        Intent intent = new Intent(this, HomeEditorActivity.class);
        intent.putExtra(HomeEditorActivity.EXTRA_PAGE_ID, item.mId);
        intent.putExtra(HomeEditorActivity.EXTRA_PAGE_TITLE, item.mTitle);
        startActivity(intent);
    }

    @Override
    public void onFinishEditDialog(long pageId, String title) {
        mAsyncQueryHandler.updatePageTitle(pageId, title);
    }

    @Override
    public void update() {
        mHomePagesRepository.get().ifSucceededSendTo(this);

    }

    @Override
    public void accept(@NonNull List<HomePageItem> value) {
        mHomeAdapter.setItems(value);
    }

    /**
     * The adapter that contains all home page items.
     */
    static class HomeAdapter extends RecyclerView.Adapter<AbsHomeViewHolder>
            implements GotItDismissListener {

        /**
         * The list of items.
         */
        final List<HomePageItem> mItems = new ArrayList<>();

        /**
         * The listener we forward to the view-holders
         */
        final HomeViewHolder.HomeViewActionListener mActionListener;

        final SharedPreferences mPreferences;

        boolean mGotItDismissed;

        HomeAdapter(Context context, HomeViewHolder.HomeViewActionListener actionListener) {
            mActionListener = actionListener;
            mPreferences = PreferencesFactory.getPreferences(context);
            mGotItDismissed = mPreferences.getBoolean("home_got_it_dismissed", false);
        }

        @Override
        public AbsHomeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            if (viewType == R.layout.got_it_pages) {
                View view = inflater.inflate(R.layout.got_it_pages, parent, false);
                return new GotItViewHolder(view, this);
            }

            View view = inflater.inflate(R.layout.list_item_home_page, parent, false);
            return new HomeViewHolder(view, mActionListener);
        }

        @Override
        public void onBindViewHolder(AbsHomeViewHolder holder, int position) {
            if (!mGotItDismissed && position == 0) return;

            if (!mGotItDismissed) {
                position--;
            }

            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 && !mGotItDismissed) {
                return R.layout.got_it_pages;
            }
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            if (!mGotItDismissed) {
                return mItems.size() + 1;
            }
            return mItems.size();
        }

        /**
         * Called to update the items in the list
         */
        public void setItems(List<HomePageItem> data) {
            mItems.clear();
            mItems.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public void onDismissed() {
            if (!mGotItDismissed) {
                mGotItDismissed = true;
                mPreferences.edit().putBoolean("home_got_it_dismissed", true).apply();
                notifyItemRemoved(0);
            }
        }
    }

    static class GotItViewHolder extends AbsHomeViewHolder implements View.OnClickListener {

        final GotItDismissListener mGotItDismissListener;

        public GotItViewHolder(View itemView, GotItDismissListener l) {
            super(itemView);
            mGotItDismissListener = l;
            View gotItButton = itemView.findViewById(R.id.got_it_button);
            gotItButton.setOnClickListener(this);
        }

        @Override
        void bind(HomePageItem item) {

        }

        @Override
        public void onClick(View v) {
            mGotItDismissListener.onDismissed();
        }
    }

    static class AsyncQueryHandlerImpl extends AsyncQueryHandler {

        static final int INSERT_HOME_PAGE = 0;

        static final int INSERT_HOTSPOT_PAGE = 1;

        static final int INSERT_ROW = 2;

        static final int INSERT_CELL = 3;

        static final int SELECT_HOTSPOT_FOR_INSERT = 0;

        final Context mContext;

        public AsyncQueryHandlerImpl(Context context, ContentResolver cr) {
            super(cr);
            mContext = context;
        }

        public void removeHomePage(long id) {
            // TODO: verify this is no longer needed with the cascade action
            startDelete(0, null,
                    HomeContract.HotspotPages.CONTENT_URI,
                    HomeContract.HotspotPages._PAGE_ID + "=?",
                    new String[]{
                            String.valueOf(id)
                    }
            );

            Uri uri = ContentUris.withAppendedId(HomeContract.Pages.CONTENT_URI, id);
            startDelete(0, null, uri, null, null);
        }

        public void addEmptyHomePage() {
            String title = mContext.getString(R.string.home_new_page_title);
            ContentValues values = new ContentValues(2);
            values.put(HomeContract.Pages.DISPLAY_NAME, title);
            values.put(HomeContract.Pages.TYPE, HomeContract.Pages.PAGE_HOME);
            startInsert(INSERT_HOME_PAGE, null, HomeContract.Pages.CONTENT_URI, values);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (token == SELECT_HOTSPOT_FOR_INSERT) {
                // The query to get all hotspot ids from the db completed
                // now iterate over all of them to add them to the
                // hotspot-pages table.
                Uri pageUri = (Uri) cookie;
                long homePageId = ContentUris.parseId(pageUri);

                while (cursor.moveToNext()) {
                    long hotspotId = cursor.getLong(0);
                    ContentValues values = new ContentValues(3);
                    values.put(HomeContract.HotspotPages._PAGE_ID, homePageId);
                    values.put(HomeContract.HotspotPages._HOTPSOT_ID, hotspotId);
                    // set position to 10 to make it the last item in the hotspot
                    values.put(HomeContract.HotspotPages.POSITION, 10);
                    startInsert(INSERT_HOTSPOT_PAGE, null, HomeContract.HotspotPages.CONTENT_URI,
                            values);
                }
            }

        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            if (token == INSERT_HOME_PAGE) {
                // query a list of all hotspots, so we
                // can add the new page to all of them
                startQuery(SELECT_HOTSPOT_FOR_INSERT,
                        uri /* cookie to get when the query completes */,
                        HomeContract.Hotspots.CONTENT_URI,
                        new String[]{
                                HomeContract.Hotspots._ID
                        },
                        null, null, null);

                // insert an empty row to go into the page.
                long pageId = ContentUris.parseId(uri);
                ContentValues values = new ContentValues();
                values.put(HomeContract.Rows.HEIGHT, 1);
                values.put(HomeContract.Rows._PAGE_ID, pageId);
                values.put(HomeContract.Rows.POSITION, 0);
                startInsert(INSERT_ROW, null, HomeContract.Rows.CONTENT_URI, values);

            } else if (token == INSERT_ROW) {
                // when the token is from inser row, insert a new empty cell
                long rowId = ContentUris.parseId(uri);
                ContentValues values = new ContentValues();
                values.put(HomeContract.Cells.COLSPAN, 1);
                values.put(HomeContract.Cells._ROW_ID, rowId);
                values.put(HomeContract.Cells.POSITION, 0);
                values.put(HomeContract.Cells.TYPE, HomeContract.Cells.DISPLAY_TYPE_UNSET);
                startInsert(INSERT_CELL, null, HomeContract.Cells.CONTENT_URI, values);
            }
        }

        public void updatePageTitle(long pageId, String title) {
            Uri uri = ContentUris.withAppendedId(HomeContract.Pages.CONTENT_URI, pageId);
            ContentValues values = new ContentValues(1);
            values.put(HomeContract.Pages.DISPLAY_NAME, title);
            startUpdate(0, null, uri, values, null, null);
        }
    }

}
