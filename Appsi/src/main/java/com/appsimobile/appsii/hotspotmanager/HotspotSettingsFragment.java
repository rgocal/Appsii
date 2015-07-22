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

import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.appsimobile.appsii.HotspotPageEntry;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

/**
 * A dialog that shows the general options for a hotspot. The user
 * can select and re-order the pages for the hotspot, change the
 * title and set the remember last page switch.
 * Any changes are saved instantly
 * <p/>
 * Created by nick on 31/01/15.
 */
public class HotspotSettingsFragment extends DialogFragment
        implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, TextWatcher {

    /**
     * A handler used to save the title of the hotspot with a delay
     */
    final Handler mHandler;

    /**
     * The listview used to change the order of the pages with
     */
    DragSortListView mDragSortListView;

    /**
     * The title edit-text
     */
    EditText mHotspotTitleView;

    /**
     * ok button to close the dialog.
     */
    View mOkButton;

    /**
     * The id of the hotspot that is being edited
     */
    long mHotspotId;

    /**
     * The name of the hotspot
     */
    String mHotspotName;

    /**
     * True when the fragment state was restored from the instance state.
     * This allows us not to initialize the values of some fields
     */
    boolean mRestoredInstanceState;

    /**
     * A query-handler to perform the operations on
     */
    QueryHandlerImpl mQueryHandler;

    ReorderController mReorderController;

    View mDefaultPage;

    ImageView mDefaultPageIconView;

    TextView mDefaultPageValueView;

    long mDefaultPageId;

    ArrayList<HotspotPageEntry> mHotspotPages;

    public HotspotSettingsFragment() {
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                onHandleMessage(msg);
                return true;
            }
        });
    }

    void onHandleMessage(Message msg) {
        String title = (String) msg.obj;
        mQueryHandler.saveHotspotTitle(mHotspotId, title);
    }

    /**
     * Creates an instance of the fragment with the arguments properly initialized
     *
     * @param hotspotId The hotspot id to edit
     * @param hotspotName The original name of the hotspot
     * @param defaultPageId The id of the default page
     */
    static HotspotSettingsFragment createEditInstance(long hotspotId, String hotspotName,
            long defaultPageId) {

        HotspotSettingsFragment result = new HotspotSettingsFragment();
        Bundle args = new Bundle();
        args.putLong("hotspot_id", hotspotId);
        args.putString("hotspot_name", hotspotName);
        args.putLong("default_page_id", defaultPageId);
        result.setArguments(args);
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            mHotspotId = -1L;
        } else {
            // get the default values from the arguments
            mHotspotId = args.getLong("hotspot_id", -1L);
            mHotspotName = args.getString("hotspot_name");
            mDefaultPageId = args.getLong("default_page_id");
        }
        // track if the state was restored
        if (savedInstanceState != null) {
            mRestoredInstanceState = true;
            mDefaultPageId = savedInstanceState.getLong("default_page_id");
        }

        mQueryHandler = new QueryHandlerImpl(getActivity().getContentResolver());
        mReorderController = new ReorderController(getActivity(), mHotspotId);

        getLoaderManager().initLoader(1, null, new HotspotPagesLoaderCallbacks());
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("default_page_id", mDefaultPageId);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hotspot_options, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mOkButton = view.findViewById(R.id.ok_button);
        mHotspotTitleView = (EditText) view.findViewById(R.id.hotspot_title);

        mDragSortListView = (DragSortListView) view.findViewById(R.id.sort_list_view);
        mReorderController.configure(mDragSortListView);

        mDefaultPage = view.findViewById(R.id.default_page);
        mDefaultPageIconView = (ImageView) view.findViewById(R.id.default_page_icon);
        mDefaultPageValueView = (TextView) view.findViewById(R.id.default_page_value);

        Drawable drawable = DrawableCompat.wrap(mDefaultPageIconView.getDrawable());
        DrawableCompat.setTint(drawable, 0xFF666666);
        mDefaultPageIconView.setImageDrawable(drawable);

        mOkButton.setOnClickListener(this);
        mDefaultPage.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mRestoredInstanceState) {
            // When we were not restored from the instance state, we can initialize
            // the values provided by the callee (from the args).
            mHotspotTitleView.setText(mHotspotName);
        }
        // Next register the listeners. This is done after setting the values
        // to make sure the event is not triggered before it should
        //
        // make sure to remove the listener before adding it again
        mHotspotTitleView.removeTextChangedListener(this);
        mHotspotTitleView.addTextChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHotspotTitleView.removeTextChangedListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.default_page) {
            showDefaultPageSelector();
        } else if (id == R.id.ok_button) {
            dismiss();
        }
    }

    private void showDefaultPageSelector() {
        int selection = 0;

        int len = getEnabledCount(mHotspotPages);

        String[] choices = new String[len + 1];
        final long[] pageIds = new long[len + 1];

        choices[0] = getString(R.string.reopen_last_plugin);
        pageIds[0] = -1L;

        int N = mHotspotPages.size();
        int pos = 0;
        for (int i = 0; i < N; i++) {
            HotspotPageEntry page = mHotspotPages.get(i);
            if (!page.mEnabled) continue;

            pos++;
            choices[pos] = page.mPageName;
            pageIds[pos] = page.mPageId;

            if (page.mPageId == mDefaultPageId) {
                selection = pos;
            }
        }

        new AlertDialog.Builder(getActivity()).
                setSingleChoiceItems(choices, selection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        long id = pageIds[which];
                        onDefaultPageSelected(id);
                        dialog.dismiss();
                    }
                }).show();
        // TODO: add boolean that the dialog was showing for orientation changes
    }

    private static int getEnabledCount(ArrayList<HotspotPageEntry> hotspotPages) {
        int result = 0;
        for (int i = 0; i < hotspotPages.size(); i++) {
            HotspotPageEntry e = hotspotPages.get(i);
            if (e.mEnabled) result++;
        }
        return result;
    }

    void onDefaultPageSelected(long id) {
        if (mDefaultPageId != id) {
            mDefaultPageId = id;
            notifyDefaultPageChanged();
        }
    }

    private void notifyDefaultPageChanged() {
        updateSelectedPageText(mDefaultPageId, mHotspotPages);
        mQueryHandler.updateDefaultPage(mHotspotId, mDefaultPageId);
    }

    private void updateSelectedPageText(long defaultPageId, ArrayList<HotspotPageEntry> pages) {
        String text = getString(R.string.reopen_last_plugin);
        for (int i = 0; i < pages.size(); i++) {
            HotspotPageEntry page = pages.get(i);
            if (page.mEnabled && page.mPageId == defaultPageId) {
                text = page.mPageName;
                break;
            }
        }

        mDefaultPageValueView.setText(text);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mQueryHandler.updateHotspotRemembersLast(mHotspotId, isChecked);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        mHandler.removeMessages(0);
        Message message = mHandler.obtainMessage(0, s.toString());
        mHandler.sendMessageDelayed(message, 500);
    }

    void onHotspotPagesLoaded(ArrayList<HotspotPageEntry> pages) {
        mHotspotPages = pages;
        mReorderController.setHotspotPages(pages);
        validateSelectedDefaultPage(mDefaultPageId, pages);
        updateSelectedPageText(mDefaultPageId, pages);
    }

    private void validateSelectedDefaultPage(long defaultPageId, ArrayList<HotspotPageEntry> pages) {
        boolean found = false;
        for (int i = 0; i < pages.size(); i++) {
            HotspotPageEntry page = pages.get(i);
            if (page.mPageId == defaultPageId) {
                found = true;
                break;
            }
        }

        if (!found && mDefaultPageId != -1L) {
            mDefaultPageId = -1L;
            notifyDefaultPageChanged();
        }
    }

    static class QueryHandlerImpl extends AsyncQueryHandler {

        public QueryHandlerImpl(ContentResolver cr) {
            super(cr);
        }

        public void saveHotspotTitle(long hotspotId, String title) {
            Uri uri = ContentUris.withAppendedId(HomeContract.Hotspots.CONTENT_URI, hotspotId);
            ContentValues values = new ContentValues(1);
            values.put(HomeContract.Hotspots.NAME, title);
            startUpdate(0, null, uri, values, null, null);
        }

        public void updateHotspotRemembersLast(long hotspotId, boolean alwaysOpenLast) {
            Uri uri = ContentUris.withAppendedId(HomeContract.Hotspots.CONTENT_URI, hotspotId);
            ContentValues values = new ContentValues(1);
            values.put(HomeContract.Hotspots.ALWAYS_OPEN_LAST, alwaysOpenLast);
            startUpdate(0, null, uri, values, null, null);
        }

        public void updateDefaultPage(long hotspotId, long defaultPageId) {
            Uri uri = ContentUris.withAppendedId(HomeContract.Hotspots.CONTENT_URI, hotspotId);
            if (defaultPageId != -1L) {
                ContentValues values = new ContentValues(1);
                values.put(HomeContract.Hotspots._DEFAULT_PAGE, defaultPageId);
                startUpdate(0, null, uri, values, null, null);
            } else {
                ContentValues values = new ContentValues(1);
                values.putNull(HomeContract.Hotspots._DEFAULT_PAGE);
                startUpdate(0, null, uri, values, null, null);

            }

        }
    }

    private class HotspotPagesLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<ArrayList<HotspotPageEntry>> {

        HotspotPagesLoaderCallbacks() {

        }

        @Override
        public Loader<ArrayList<HotspotPageEntry>> onCreateLoader(int id, Bundle args) {
            return new HotspotsPagesLoader(getActivity(), mHotspotId);
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<HotspotPageEntry>> loader,
                ArrayList<HotspotPageEntry> data) {
            onHotspotPagesLoaded(data);
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<HotspotPageEntry>> loader) {

        }
    }


}
