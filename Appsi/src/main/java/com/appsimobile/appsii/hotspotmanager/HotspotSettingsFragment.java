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
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.mobeta.android.dslv.DragSortListView;

/**
 * A dialog that shows the general options for a hotspot. The user
 * can select and re-order the pages for the hotspot, change the
 * title and set the remember last page switch.
 * Any changes are saved instantly
 * <p/>
 * TODO: this needs support for setting the default page.
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
     * The remember last switch
     */
    SwitchCompat mRememberLastSwitch;

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
     * True when remember last is enabled
     */
    boolean mRememberLastEnabled;

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

    /**
     * Creates an instance of the fragment with the arguments properly initialized
     *
     * @param hotspotId The hotspot id to edit
     * @param hotspotName The original name of the hotspot
     * @param rememberLastEnabled The state of the remember last option
     */
    static HotspotSettingsFragment createEditInstance(long hotspotId, String hotspotName,
            boolean rememberLastEnabled) {

        HotspotSettingsFragment result = new HotspotSettingsFragment();
        Bundle args = new Bundle();
        args.putLong("hotspot_id", hotspotId);
        args.putString("hotspot_name", hotspotName);
        args.putBoolean("remember_last", rememberLastEnabled);
        result.setArguments(args);
        return result;
    }

    void onHandleMessage(Message msg) {
        String title = (String) msg.obj;
        mQueryHandler.saveHotspotTitle(mHotspotId, title);
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
            mRememberLastEnabled = args.getBoolean("remember_last", false);
        }
        // track if the state was restored
        mRestoredInstanceState = savedInstanceState != null;

        mQueryHandler = new QueryHandlerImpl(getActivity().getContentResolver());
        mReorderController = new ReorderController(getActivity(), mHotspotId);
        mReorderController.loadHotspotPages();
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
        mRememberLastSwitch = (SwitchCompat) view.findViewById(R.id.remember_last_switch);

        mDragSortListView = (DragSortListView) view.findViewById(R.id.sort_list_view);
        mReorderController.configure(mDragSortListView);

        mOkButton.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mRestoredInstanceState) {
            // When we were not restored from the instance state, we can initialize
            // the values provided by the callee (from the args).
            mHotspotTitleView.setText(mHotspotName);
            mRememberLastSwitch.setChecked(mRememberLastEnabled);
        }
        // Next register the listeners. This is done after setting the values
        // to make sure the event is not triggered before it should
        mRememberLastSwitch.setOnCheckedChangeListener(this);
        mHotspotTitleView.removeTextChangedListener(this);
        mHotspotTitleView.addTextChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRememberLastSwitch.setOnCheckedChangeListener(null);
        mHotspotTitleView.removeTextChangedListener(this);

    }

    @Override
    public void onClick(View v) {
        dismiss();
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
    }


}
