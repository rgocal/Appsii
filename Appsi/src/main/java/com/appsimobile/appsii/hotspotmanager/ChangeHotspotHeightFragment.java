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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.preference.PreferenceHelper;

import javax.inject.Inject;

/**
 * This fragment allows the user to set the height of the given hotspot
 * <p/>
 * Created by nick on 01/02/15.
 */
public class ChangeHotspotHeightFragment extends DialogFragment
        implements SeekBar.OnSeekBarChangeListener {

    /**
     * A seekbar the user can move to set the height
     */
    SeekBar mSeekBar;

    /**
     * A switch that allows the user to hide the bar again when done
     */
    SwitchCompat mHideWhenDoneSwitch;

    /**
     * The value of the hostpots visibilty status. Used to save instance state
     * of the switch
     */
    boolean mHideHotspotsWhenDone;

    /**
     * The current height of the hotspot. Retained in instance state
     */
    float mHeight;

    /**
     * The id of the hotspot we are working on
     */
    long mHotspotId;

    /**
     * A query handler to perform the update operations
     */
    AsyncQueryHandlerImpl mAsyncQueryHandler;

    /**
     * The ok button to close the dialog
     */
    Button mOkButton;

    /**
     * A handler to delay the change with while the user is changing the
     * height. This waits until the move is idle before it is committed
     * to the db.
     */
    Handler mHandler;

    @Inject
    PreferenceHelper mPreferenceHelper;

    @Inject
    SharedPreferences mPreferences;

    /**
     * Creates a new instance of the fragment. This sets the arguments
     * properly.
     */
    static ChangeHotspotHeightFragment createInstance(long hotspotId, float hotspotHeight) {
        ChangeHotspotHeightFragment result = new ChangeHotspotHeightFragment();
        result.setStyle(STYLE_NO_TITLE, 0);

        Bundle args = new Bundle();
        args.putLong("hotspot_id", hotspotId);
        args.putFloat("height", hotspotHeight);

        result.setArguments(args);
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        mHotspotId = args.getLong("hotspot_id");

        if (savedInstanceState != null) {
            mHeight = savedInstanceState.getFloat("height");
            mHideHotspotsWhenDone = savedInstanceState.getBoolean("hidden");
        } else {
            mHideHotspotsWhenDone = mPreferenceHelper.getHotspotsHidden();
            mHeight = args.getFloat("height", .15f);
        }

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return ChangeHotspotHeightFragment.this.handleMessage(msg);
            }
        });
    }

    /**
     * Performs the actual handling of the received message.
     */
    boolean handleMessage(Message msg) {
        mAsyncQueryHandler.setHotspotHeight(mHotspotId, mHeight);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        // While the editor is visible, we make sure the hotspots are shown
        // resetting this is controlled by the mHideHotspotsWhenDone variable
        mPreferences.edit().putBoolean("pref_hide_hotspots", false).apply();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("hidden", mHideHotspotsWhenDone);
        outState.putFloat("height", mHeight);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mHideHotspotsWhenDone) {
            mPreferences.edit().putBoolean("pref_hide_hotspots", true).apply();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_hotspot_height, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
        mHideWhenDoneSwitch = (SwitchCompat) view.findViewById(R.id.hide_when_done);
        mOkButton = (Button) view.findViewById(R.id.ok_button);

        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        // the real range is 10-100, so use 0-90 as range
        mSeekBar.setMax(90);
        int value = (int) ((100 * mHeight) - 10);
        mSeekBar.setProgress(value);
        mSeekBar.setOnSeekBarChangeListener(this);

        mHideWhenDoneSwitch.setChecked(mHideHotspotsWhenDone);
        mHideWhenDoneSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mHideHotspotsWhenDone = isChecked;
                    }
                });

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (mAsyncQueryHandler == null) {
                mAsyncQueryHandler = new AsyncQueryHandlerImpl(getActivity().getContentResolver());
            }
            mHeight = progress / 90f + .1f;

            mHandler.removeMessages(0);
            Message msg = mHandler.obtainMessage(0);
            mHandler.sendMessageDelayed(msg, 100);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    static class AsyncQueryHandlerImpl extends AsyncQueryHandler {

        public AsyncQueryHandlerImpl(ContentResolver cr) {
            super(cr);
        }

        /**
         * Updates the height of the hotspot in the db.
         */
        void setHotspotHeight(long hotspotId, float height) {
            Uri uri = ContentUris.withAppendedId(HomeContract.Hotspots.CONTENT_URI, hotspotId);
            ContentValues values = new ContentValues();
            values.put(HomeContract.Hotspots.HEIGHT, height);
            startUpdate(0, null, uri, values, null, null);
        }
    }

}
