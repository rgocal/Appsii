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
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.HotspotItem;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.provider.HomeContract;

/**
 * A fragment that uses the HotspotPositionEditorView to allow the user to edit
 * the hotspot's position.
 * Created by nick on 01/02/15.
 */
public class MoveHotspotFragment extends Fragment
        implements HotspotPositionEditorView.OnPositionChangedListener {

    /**
     * The view doing the heavy lifting of the touch handling and everything related
     */
    HotspotPositionEditorView mHotspotPositionEditorView;

    /**
     * The query handler to perform the actions on
     */
    AsyncQueryHandlerImpl mAsyncQueryHandler;

    /**
     * The id of the hotspot we are bound to
     */
    long mHotspotId;

    /**
     * True if the hotspot we are bound to in on the left
     */
    boolean mLeft;

    /**
     * The y position of the hotspot
     */
    float mYPosition;

    /**
     * The height of the hotspot. The height is also needed to show the hotspot correctly
     */
    float mHeight;

    /**
     * The title of the hostpot.
     */
    String mTitle;


    /**
     * Creates an instance of the editor with the proper arguments set
     */
    static MoveHotspotFragment createInstance(
            long hotspotId, boolean left, float yPosition, float height, String title) {

        MoveHotspotFragment result = new MoveHotspotFragment();
        Bundle args = new Bundle();

        args.putLong("hotspot_id", hotspotId);
        args.putBoolean("left", left);
        args.putFloat("y", yPosition);
        args.putFloat("height", height);
        args.putString("title", title);

        result.setArguments(args);
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        mHotspotId = args.getLong("hotspot_id");
        mLeft = args.getBoolean("left");
        mYPosition = args.getFloat("y");
        mHeight = args.getFloat("height");
        mTitle = args.getString("title");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_move_hotspot, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // ensure no presses go through this view.
        view.setClickable(true);

        mHotspotPositionEditorView =
                (HotspotPositionEditorView) view.findViewById(R.id.configuration_view);

        // create a minimal item to provide to the editor
        HotspotItem item = new HotspotItem();
        item.mId = mHotspotId;
        item.mLeft = mLeft;
        item.mHeightRelativeToViewHeight = mHeight;
        item.mYPosRelativeToView = mYPosition;
        item.mName = mTitle;

        // bind the view to the hotspot
        mHotspotPositionEditorView.setHotspotItem(item);
        mHotspotPositionEditorView.setOnPositionChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onPositionChanged(long hotspotId, boolean isLeft, float yPosition) {
        if (mAsyncQueryHandler == null) {
            mAsyncQueryHandler = new AsyncQueryHandlerImpl(getActivity().getContentResolver());
        }
        mAsyncQueryHandler.saveHotspotPosition(hotspotId, isLeft, yPosition);
    }

    static class AsyncQueryHandlerImpl extends AsyncQueryHandler {

        public AsyncQueryHandlerImpl(ContentResolver cr) {
            super(cr);
        }

        void saveHotspotPosition(long id, boolean left, float yPosition) {
            ContentValues values = new ContentValues();
            values.put(HomeContract.Hotspots.Y_POSITION, yPosition);
            values.put(HomeContract.Hotspots.LEFT_BORDER, left ? 1 : 0);
            Uri uri = ContentUris.withAppendedId(HomeContract.Hotspots.CONTENT_URI, id);
            startUpdate(0, null, uri, values, null, null);
        }

    }
}
