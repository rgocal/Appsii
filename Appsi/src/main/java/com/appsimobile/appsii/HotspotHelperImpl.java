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

package com.appsimobile.appsii;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.util.CircularArray;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import com.appsimobile.appsii.SidebarHotspot.SidebarGestureCallback;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.crashlytics.android.Crashlytics;

public class HotspotHelperImpl extends AbstractHotspotHelper
        implements OnClickListener, View.OnLongClickListener, SidebarGestureCallback,
        SidebarHotspot.SwipeListener {

    final int[] mOffsetInt = new int[2];

    final HotspotHelperListener mCallback;

    final PopupLayer mPopupLayer;

    final LayoutInflater mLayoutInflater;

    final View mFullScreenWatcher;

    private final Context mContext;

    private final WindowManager mWindowManager;

    private final LongSparseArray<HotspotContainerHelper> mSidebarHotspots =
            new LongSparseArray<>();

    boolean mDraggingHotspot;

    int mDragLocalX;

    int mRawStartDragY;

    int mDragLocalY;

    boolean mIsLeftHotspot;

    int mHotspotX;

    int mHotspotY;

    boolean mFullScreenWatcherAttached;

    private boolean mVibrateOnTouch;

    private boolean mHotspotsActive;

    private CircularArray<HotspotItem> mHotspotItems;

    public HotspotHelperImpl(Context context, HotspotHelperListener callback,
            PopupLayer popupLayer) {
        super(context);
        mLayoutInflater = LayoutInflater.from(context);
        mPopupLayer = popupLayer;
        mContext = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mVibrateOnTouch = prefs.getBoolean("pref_sidebar_haptic_feedback", false);
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mCallback = callback;
        mFullScreenWatcher = new View(context) {
            {
                setFitsSystemWindows(true);
            }

        };
    }

    public static boolean isPageGesture(String stringUri) {
        if (stringUri == null) return false;
        Uri uri = Uri.parse(stringUri);
        return isPageGesture(uri);
    }

    public static boolean isPageGesture(Uri uri) {
        if (uri == null) return false;
        return true;
    }

    @Override
    public void onOrientationChanged() {
        if (mHotspotsActive) {
            updateHotspotLayoutParams();
        }
    }

    @Override
    public void onDestroy() {
        removeHotspots();
    }

    @Override
    public void setVibrate(boolean vibrate) {
        int count = mSidebarHotspots.size();
        mVibrateOnTouch = vibrate;
        for (int i = 0; i < count; i++) {
            HotspotContainerHelper sidebarHotspot = mSidebarHotspots.valueAt(i);
            sidebarHotspot.setVibrateOnTouch(vibrate);
        }
    }

    @Override
    public void addHotspots() {
        try {
            removeHotspots();
            mHotspotsActive = true;
            if (mHotspotItems != null) {
                int count = mHotspotItems.size();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                for (int i = 0; i < count; i++) {
                    HotspotItem conf = mHotspotItems.get(i);


                    HotspotContainerHelper hotspotContainerHelper = mSidebarHotspots.get(conf.mId);
                    boolean isAdded = hotspotContainerHelper != null;
                    if (!isAdded) {
                        hotspotContainerHelper = createSidebarHotspot();
                    }
                    hotspotContainerHelper.bind(conf);

                    mSidebarHotspots.put(conf.mId, hotspotContainerHelper);

                    if (!isAdded) {
                        WindowManager.LayoutParams lp =
                                configureHotspot(hotspotContainerHelper, conf, prefs);
                        try {
                            PermissionUtils.throwIfNotPermitted(
                                    mContext, Manifest.permission.SYSTEM_ALERT_WINDOW);

                            mWindowManager.addView(hotspotContainerHelper.mHotspotParent, lp);
                        } catch (PermissionDeniedException e) {
                            throw e;
                        } catch (Exception e) {
                            Log.w("HotspotHelperImpl", "error adding hotspot", e);
                            Crashlytics.logException(e);
                        }
                    }
                }
            }
            addScreenWatcher();
        } catch (PermissionDeniedException e) {
            PermissionUtils.showPermissionNotification(mContext, 1001,
                    Manifest.permission.SYSTEM_ALERT_WINDOW, 0);
            mHotspotsActive = false;
            AppsiiUtils.stopAppsi(mContext);
        }
    }

    private HotspotContainerHelper createSidebarHotspot() {
        View result = mLayoutInflater.inflate(R.layout.hotspot, null);

        HotspotContainerHelper helper = new HotspotContainerHelper(result,
                (SidebarHotspot) result.findViewById(R.id.sidebar_hotspot));

        helper.setOnClickListener(this);
        helper.setOnLongClickListener(this);
        helper.setVibrateOnTouch(mVibrateOnTouch);
        helper.setCallback(this);
        return helper;
    }

    private void addScreenWatcher() throws PermissionDeniedException {
        PermissionUtils.throwIfNotPermitted(mContext, Manifest.permission.SYSTEM_ALERT_WINDOW);
        mWindowManager.addView(mFullScreenWatcher, createInsetParams());
        mFullScreenWatcherAttached = true;
    }

    @Override
    public void removeHotspots() {
        int count = mSidebarHotspots.size();
        for (int i = count - 1; i >= 0; i--) {
            HotspotContainerHelper hotspotContainerHelper = mSidebarHotspots.valueAt(i);
            SidebarHotspot sidebarHotspot = hotspotContainerHelper.mSidebarHotspot;
            View sidebarParent = hotspotContainerHelper.mHotspotParent;
            if (!sidebarHotspot.mIsDragOpening) {
                if (sidebarParent.getParent() != null) {
                    mWindowManager.removeView(sidebarParent);
                }
                hotspotContainerHelper.setOnClickListener(null);
                hotspotContainerHelper.setOnLongClickListener(null);
                hotspotContainerHelper.setCallback(null);
                long id = sidebarHotspot.getConfiguration().mId;
                mSidebarHotspots.remove(id);
            }
        }
        removeScreenWatcher();
        mHotspotsActive = false;
    }

    @Override
    public void onHotspotsLoaded(CircularArray<HotspotItem> configurations) {
        mHotspotItems = configurations;
    }

    @Override
    public int getTopOffset() {
        mOffsetInt[1] = -1;
        mFullScreenWatcher.getLocationOnScreen(mOffsetInt);
        return mOffsetInt[1];
    }

    private void removeScreenWatcher() {
        if (mFullScreenWatcherAttached) {
            mWindowManager.removeView(mFullScreenWatcher);
            mFullScreenWatcherAttached = false;
        }
    }

    public void updateHotspotLayoutParams() {
        if (!mHotspotsActive) return;

        if (mHotspotItems != null) {
            int count = mHotspotItems.size();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            for (int i = 0; i < count; i++) {
                HotspotItem conf = mHotspotItems.get(i);


                HotspotContainerHelper hotspotContainerHelper = mSidebarHotspots.get(conf.mId);
                boolean isAdded = hotspotContainerHelper != null;
                if (!isAdded) {
                    continue;
                }

                WindowManager.LayoutParams lp =
                        configureHotspot(hotspotContainerHelper, conf, prefs);
                try {
                    mWindowManager.updateViewLayout(hotspotContainerHelper.mHotspotParent, lp);
                } catch (Exception e) {
                    Log.w("HotspotHelperImpl", "error adding hotspot", e);
                }
            }
        }
    }

    private WindowManager.LayoutParams configureHotspot(HotspotContainerHelper hotspot,
            HotspotItem hotspotItem,
            SharedPreferences prefs) {
        WindowManager.LayoutParams lp = createHotspotParams(hotspotItem, prefs);
        hotspot.mSidebarHotspot.setPosition(hotspotItem.mLeft, -lp.width, lp.y);
        return lp;
    }

    @Override
    public void onClick(View v) {
        // We can implement some feature here !
    }

    public boolean onLongClick(View v) {
        return true;
    }

    @Override
    public SidebarHotspot.SwipeListener open(SidebarHotspot hotspot, SidebarHotspot.Gesture action,
            int x, int y) {
        mIsLeftHotspot = hotspot.isLeft();
        mHotspotX = hotspot.getHPos();
        mHotspotY = hotspot.getVPos();
        HotspotItem conf = hotspot.getConfiguration();
        if (action == SidebarHotspot.Gesture.TAP) {
            //return mCallback.openSidebar(conf, null, 0);
            return null;
        } else {
            CircularArray<HotspotPageEntry> entries = hotspot.getHotspotPageEntries();
            return mCallback.openSidebar(conf, entries, Appsi.OPEN_FLAG_LIKE_NOTIFICATION_BAR);
        }
    }

    @Override
    public void cancelVisualHints(SidebarHotspot sidebarHotspot) {
        mCallback.cancelVisualHints();
    }

    @Override
    public SidebarHotspot.SwipeListener longPressGesturePerformed(SidebarHotspot hotspot,
            int startX, int startY,
            int rawStartY) {
        return null;
    }

    @Override
    public void removeIfNeeded(SidebarHotspot hotspot) {
        if (!mHotspotsActive) {
            HotspotItem configuration = hotspot.getConfiguration();
            HotspotContainerHelper added = mSidebarHotspots.get(configuration.mId);
            if (added.isHotspot(hotspot)) {
                removeHotspots();
            }
        }
    }

    @Override
    public void setSwipeLocation(SidebarHotspot hotspot, int localX, int localY, int rawX,
            int rawY) {
        WindowManager wm = mWindowManager;
        Display display = wm.getDefaultDisplay();
        int width = display.getWidth();
        if (mDraggingHotspot) {
            boolean left = rawX < width / 2;
            int deltaY = mRawStartDragY - rawY;
            setSwipeLocationForHotspotDrag(hotspot, localX, deltaY, left);
        }
    }

    @Override
    public void onSwipeEnd(SidebarHotspot hotspot, int screenX, int screenY, boolean cancelled,
            VelocityTracker velocityTracker) {

    }

    private void setSwipeLocationForHotspotDrag(SidebarHotspot hotspot, int localX, int deltaY,
            boolean left) {
        long id = hotspot.getHotspotId();
        HotspotContainerHelper helper = mSidebarHotspots.get(id);
        WindowManager wm = mWindowManager;
        WindowManager.LayoutParams params =
                (WindowManager.LayoutParams) helper.mHotspotParent.getLayoutParams();
        int y = mHotspotY - deltaY;
        params = updateLayoutParams(left, y, params);
        wm.updateViewLayout(helper.mHotspotParent, params);
    }

    static class HotspotContainerHelper {

        final View mHotspotParent;

        final SidebarHotspot mSidebarHotspot;

        HotspotContainerHelper(View hotspotParent, SidebarHotspot sidebarHotspot) {
            mHotspotParent = hotspotParent;
            mSidebarHotspot = sidebarHotspot;
        }

        public boolean isHotspot(SidebarHotspot hotspot) {
            return hotspot == mSidebarHotspot;
        }

        public void setVibrateOnTouch(boolean vibrate) {
            mSidebarHotspot.setVibrateOnTouch(vibrate);
        }

        public void setOnClickListener(HotspotHelperImpl appsiHotspotHelper) {
            mSidebarHotspot.setOnClickListener(appsiHotspotHelper);
        }

        public void setOnLongClickListener(HotspotHelperImpl appsiHotspotHelper) {
            mSidebarHotspot.setOnLongClickListener(appsiHotspotHelper);
        }


        public void setCallback(HotspotHelperImpl appsiHotspotHelper) {
            mSidebarHotspot.setCallback(appsiHotspotHelper);
        }

        public void bind(HotspotItem conf) {
            mSidebarHotspot.bind(conf);
        }
    }

}
