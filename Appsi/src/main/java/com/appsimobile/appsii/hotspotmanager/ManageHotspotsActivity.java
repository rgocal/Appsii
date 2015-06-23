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

package com.appsimobile.appsii.hotspotmanager;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.ActivityUtils;
import com.appsimobile.appsii.GotItDismissListener;
import com.appsimobile.appsii.HotspotItem;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.preference.PreferencesFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The main view of this feature. This shows a list of the hot-spots and
 * handles the actions performed on it. It also allows adding new
 * hot-spots.
 * <p/>
 * Created by Nick Martens on 8/16/13.
 */
public class ManageHotspotsActivity extends AppCompatActivity
        implements HotspotActionListener,
        LoaderManager.LoaderCallbacks<List<HotspotItem>> {

    /**
     * The adapter showing all of the hotspots
     */
    HotspotAdapter mHotspotAdapter;

    /**
     * A handler to perform the updates with
     */
    AsyncQueryHandlerImpl mAsyncQueryHandler;

    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtils.setContentViewWithFab(this, R.layout.activity_manage_hotspots);

        View addPanelButton = ActivityUtils.setupFab(this, R.id.add_panel_button);

        ActivityUtils.setupToolbar(this, R.id.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mHotspotAdapter = new HotspotAdapter(this, this);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);

        mRecyclerView.setAdapter(mHotspotAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAsyncQueryHandler = new AsyncQueryHandlerImpl(this);

        addPanelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddClicked();
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * Called when the add action is performed
     */
    void onAddClicked() {
        mAsyncQueryHandler.addNewHotspot();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void performMainAction(HotspotItem configuration) {
        onItemClicked(configuration);
    }

    void onItemClicked(HotspotItem configuration) {
        String hotspotName = configuration.mName;
        long id = configuration.mId;
        long defaultPageId = configuration.mDefaultPageId;
        HotspotSettingsFragment fragment = HotspotSettingsFragment.
                createEditInstance(id, hotspotName, configuration.mDefaultPageId);

        fragment.show(getFragmentManager(), "edit_hotspot");

    }

    @Override
    public void performMoveHotspotAction(HotspotItem configuration) {
        // Create an instance of the move-fragment and add it.
        Fragment moveFragment = MoveHotspotFragment.createInstance(
                configuration.mId, configuration.mLeft,
                configuration.mYPosRelativeToView,
                configuration.mHeightRelativeToViewHeight,
                configuration.mName);

        getFragmentManager().beginTransaction().
                addToBackStack(null).
                add(R.id.container, moveFragment).
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
                commit();
    }

    @Override
    public void performDeleteHotspotAction(HotspotItem configuration) {
        mAsyncQueryHandler.deleteHotspot(configuration.mId);
    }

    @Override
    public void performSetHeightHotspotAction(HotspotItem configuration) {
        ChangeHotspotHeightFragment fragment = ChangeHotspotHeightFragment.
                createInstance(configuration.mId, configuration.mHeightRelativeToViewHeight);

        fragment.show(getFragmentManager(), "height_editor");
    }

    @Override
    public Loader<List<HotspotItem>> onCreateLoader(int id, Bundle args) {
        return new HotspotsLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<HotspotItem>> loader,
            List<HotspotItem> data) {
        onHotspotsLoaded(data);
    }

    /**
     * Called when the hotspots are loaded from the database
     */
    void onHotspotsLoaded(List<HotspotItem> hotspotItems) {
        mHotspotAdapter.setHotspots(hotspotItems);
    }

    @Override
    public void onLoaderReset(Loader<List<HotspotItem>> loader) {

    }

    /**
     * The adapter showing the hotspots. Forwards the listener to the
     * view-holders and handles everything related
     */
    static class HotspotAdapter extends RecyclerView.Adapter<AbsHotspotViewHolder>
            implements GotItDismissListener {

        final HotspotActionListener mActionListener;

        final List<HotspotItem> mHotspotItems = new ArrayList<>();

        final SharedPreferences mPreferences;

        boolean mGotItDismissed;

        HotspotAdapter(Context context, HotspotActionListener actionListener) {
            mActionListener = actionListener;
            mPreferences = PreferencesFactory.getPreferences(context);
            mGotItDismissed = mPreferences.getBoolean("hotspots_got_it_dismissed", false);
        }

        public HotspotItem getItem(int position) {
            return mHotspotItems.get(position);
        }

        @Override
        public AbsHotspotViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            if (viewType == R.layout.got_it_hotspots) {
                View view = inflater.inflate(R.layout.got_it_hotspots, parent, false);
                return new GotItViewHolder(view, this);
            }

            View convertView = inflater.inflate(R.layout.hotspot_view, parent, false);
            return new HotspotViewHolder(convertView, mActionListener);
        }

        @Override
        public void onBindViewHolder(AbsHotspotViewHolder holder, int position) {

            if (!mGotItDismissed && position == 0) {
                return;
            }

            if (!mGotItDismissed) {
                position--;
            }

            HotspotItem configuration = mHotspotItems.get(position);

            holder.bind(configuration);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 && !mGotItDismissed) {
                return R.layout.got_it_hotspots;
            }
            return super.getItemViewType(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            if (!mGotItDismissed) {
                return mHotspotItems.size() + 1;
            }
            return mHotspotItems.size();
        }

        public void setHotspots(List<HotspotItem> hotspotItems) {
            mHotspotItems.clear();
            mHotspotItems.addAll(hotspotItems);
            notifyDataSetChanged();
        }

        @Override
        public void onDismissed() {
            if (!mGotItDismissed) {
                mGotItDismissed = true;
                mPreferences.edit().putBoolean("hotspots_got_it_dismissed", true).apply();
                notifyItemRemoved(0);
            }
        }
    }

    static class GotItViewHolder extends AbsHotspotViewHolder implements View.OnClickListener {

        final GotItDismissListener mGotItDismissListener;

        public GotItViewHolder(View itemView, GotItDismissListener l) {
            super(itemView);
            mGotItDismissListener = l;
            View gotItButton = itemView.findViewById(R.id.got_it_button);
            gotItButton.setOnClickListener(this);
        }

        @Override
        public void bind(HotspotItem hotspotItem) {

        }

        @Override
        public void onClick(View v) {
            mGotItDismissListener.onDismissed();
        }
    }

    /**
     * An implementation of the query handler to perform operations on the hotspots with
     */
    static class AsyncQueryHandlerImpl extends AsyncQueryHandler {

        final int TOKEN_INSERT_HOTSPOT = 0;

        final int TOKEN_INSERT_HOTSPOT_PAGE = 1;

        final Context mContext;

        public AsyncQueryHandlerImpl(Context context) {
            super(context.getContentResolver());
            mContext = context.getApplicationContext();
        }

        /**
         * Inserts a new hotspot into the database
         */
        public void addNewHotspot() {
            ContentValues values = new ContentValues();
            values.put(HomeContract.Hotspots.ALWAYS_OPEN_LAST, 0);
            values.put(HomeContract.Hotspots.NAME,
                    mContext.getString(R.string.default_hotspot_name));
            values.put(HomeContract.Hotspots.LEFT_BORDER, 1);
            values.put(HomeContract.Hotspots.NEEDS_CONFIGURATION, 1);
            values.put(HomeContract.Hotspots.Y_POSITION, .2f);
            values.put(HomeContract.Hotspots.HEIGHT, .2f);
            startInsert(TOKEN_INSERT_HOTSPOT, null, HomeContract.Hotspots.CONTENT_URI, values);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // when we get here, this means we were querying the pages table to
            // add link to a newly inserted hotspot.
            // The cookie is the uri of the newly inserted hotspot, the cursor
            // only contains page ids, so we can get them and insert them one
            // by one.
            long hotspotId = ContentUris.parseId((Uri) cookie);

            int position = 0;
            while (cursor.moveToNext()) {
                long pageId = cursor.getLong(0);

                ContentValues values = new ContentValues();
                values.put(HomeContract.HotspotPages.POSITION, position++);
                values.put(HomeContract.HotspotPages._HOTPSOT_ID, hotspotId);
                values.put(HomeContract.HotspotPages._PAGE_ID, pageId);

                startInsert(TOKEN_INSERT_HOTSPOT_PAGE, null,
                        HomeContract.HotspotPages.CONTENT_URI, values);
            }
            cursor.close();
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            // in case a hotspot was added, start a query to load all pages, so we can link
            // them all to the new hotspot
            if (token == TOKEN_INSERT_HOTSPOT) {
                startQuery(0, uri /* cookie */,
                        HomeContract.Pages.CONTENT_URI,
                        new String[]{
                                HomeContract.Pages._ID
                        },
                        null,
                        null,
                        null);
            }
        }

        public void deleteHotspot(long id) {

            // first delete the hotspot-pages links
            startDelete(0, null,
                    HomeContract.HotspotPages.CONTENT_URI,
                    HomeContract.HotspotPages._HOTPSOT_ID + "=?",
                    new String[]{
                            String.valueOf(id)
                    });

            // then delete the hotspot itself
            Uri uri = ContentUris.withAppendedId(HomeContract.Hotspots.CONTENT_URI, id);
            startDelete(0, null, uri, null, null);
        }
    }


}
