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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import com.appsimobile.appsii.module.home.provider.HomeContract;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SidebarHotspot extends View {


    public static final int VIBRATE_DURATION = 20;

    static final int STATE_AWAITING_RELEASE = 3;

    private static final int STATE_WAITING = 0;

    private static final int STATE_GESTURE_IN_PROGRESS = 2;

    private static final int SIDEBAR_MINIMUM_MOVE = 0;

    final ArrayList<HotspotPageEntry> mHotspotPageEntries = new ArrayList<>(8);

    final Handler mHandler = new Handler();

    int mState = STATE_WAITING;

    boolean mSwipeInProgress;

    SidebarGestureCallback mCallback;

    boolean mIsDragOpening;

    float mStartX;

    float mStartY;

    float mRawStartY;

    SwipeListener mSwipeListener;

    ContentObserver mHotspotsPagesObserver;

    AsyncTask<Void, Void, ArrayList<HotspotPageEntry>> mLoadDataTask;

    private float mMinimumMove;

    private boolean mVibrate;

    private boolean mLeft;

    private int mTop;

    private int mLeftPos;

    private boolean mVisibleHotspots;

    /**
     * The hotspot-item this hotspot is bound to.
     * This contains the properties of the hotspot.
     */
    private HotspotItem mHotspotItem;

    /**
     * A shared preferences listener
     */
    private SharedPreferencesListener mSharedPreferencesListener;

    /**
     * A velocity tracked given to the listeners so they can determine the speed if needed
     */
    private VelocityTracker mVelocityTracker;

    /**
     * The current background drawable
     */
    private Drawable mBackground;

    public SidebarHotspot(Context context) {
        super(context);
    }

    public SidebarHotspot(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SidebarHotspot(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(e);
                // remove the background to make sure it does not overlap
                // the sidebar

                mIsDragOpening = true;
                setBackgroundResource(0);

                float x = e.getX();
                float y = e.getY();

                if (mCallback != null) {
                    mSwipeListener =
                            mCallback.open(this, Gesture.TO_CENTER, (int) x, (int) y);
                    mSwipeInProgress = mSwipeListener != null;
                    mState = STATE_AWAITING_RELEASE;
                    if (mVibrate) {
                        vibrate();
                    }

                    return true;
                }
                return false;
            }
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(e);
                float x = e.getX();
                float y = e.getY();
                return detectSwipe(x, y, e);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelMotionHandling(e, false);
                return false;
        }


        return super.onTouchEvent(e);
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VIBRATE_DURATION);
    }

    private boolean detectSwipe(float x, float y, MotionEvent e) {
        if (mState == STATE_AWAITING_RELEASE && mSwipeInProgress) {
            if (mCallback != null) {
                mSwipeListener.setSwipeLocation(this, (int) e.getX(), (int) e.getY(),
                        (int) e.getRawX(), (int) e.getRawY());
            }
            return true;
        }
        if (mState != STATE_GESTURE_IN_PROGRESS) return false;
        float deltaX = Math.abs(x - mStartX);
        float deltaY = y - mStartY;
        Gesture action = detectAction(deltaX, deltaY, mMinimumMove);
        if (action != null) {
            mSwipeListener = mCallback.open(this, action, (int) x, (int) y);
            mSwipeInProgress = mSwipeListener != null;
            mState = STATE_AWAITING_RELEASE;
            return true;
        }
        return true;
    }

    private void cancelMotionHandling(MotionEvent e, boolean cancelled) {
        mState = STATE_WAITING;
        if (mSwipeListener != null) {
            if (e == null) {
                mSwipeListener.onSwipeEnd(this, 0, 0, cancelled, mVelocityTracker);
            } else {
                mSwipeListener.onSwipeEnd(this, (int) e.getRawX(), (int) e.getRawY(), cancelled,
                        mVelocityTracker);
            }
        }
        if (mCallback != null) {
            mCallback.cancelVisualHints(this);
        }
        mSwipeInProgress = false;
        mSwipeListener = null;

        // restore the background
        setBackground(mBackground);
        mIsDragOpening = false;

        if (mCallback != null) {
            mCallback.removeIfNeeded(this);
        }
        invalidate();

        mVelocityTracker.addMovement(e);
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    public static Gesture detectAction(float deltaX, float deltaY, float minDistance) {
        Gesture swipeAction = null;
        if (deltaX >= minDistance) {
            swipeAction = Gesture.TO_CENTER;
        } else if (deltaY > minDistance) {
            swipeAction = Gesture.DOWN;
        } else if (deltaY < -minDistance) {
            swipeAction = Gesture.UP;
        }
        return swipeAction;

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSwipeInProgress = false;

        if (mHotspotsPagesObserver == null) {
            mHotspotsPagesObserver = new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    onChange(selfChange, null);
                }

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    reloadHotspotData();
                }
            };
            getContext().getContentResolver().registerContentObserver(
                    HomeContract.HotspotPages.CONTENT_URI,
                    true,
                    mHotspotsPagesObserver);
        }

        setupBackground();
    }

    void reloadHotspotData() {

        Log.d("SidebarHotspot", "reloading hotspots");

        if (mHotspotItem == null) return;

        final long hotspotId = mHotspotItem.mId;

        final Context context = getContext();
        if (mLoadDataTask != null) mLoadDataTask.cancel(true);
        mLoadDataTask =
                new AsyncTask<Void, Void, ArrayList<HotspotPageEntry>>() {


                    @Override
                    protected ArrayList<HotspotPageEntry> doInBackground(Void... params) {

                        Cursor c = context.getContentResolver().
                                query(HotspotPagesQuery.createUri(hotspotId),
                                        HotspotPagesQuery.PROJECTION,
                                        null,
                                        null,
                                        HomeContract.HotspotDetails.POSITION + " ASC"
                                );
                        ArrayList<HotspotPageEntry> result = new ArrayList<>(c.getCount());
                        while (c.moveToNext()) {
                            HotspotPageEntry entry = new HotspotPageEntry();
                            entry.mEnabled = c.getInt(HotspotPagesQuery.ENABLED) == 1;
                            entry.mPageId = c.getLong(HotspotPagesQuery.PAGE_ID);
                            entry.mHotspotId = c.getLong(HotspotPagesQuery.HOTSPOT_ID);
                            entry.mPageName = c.getString(HotspotPagesQuery.PAGE_NAME);
                            entry.mHotspotName = c.getString(HotspotPagesQuery.HOTSPOT_NAME);
                            entry.mPosition = c.getInt(HotspotPagesQuery.POSITION);
                            entry.mPageType = c.getInt(HotspotPagesQuery.PAGE_TYPE);
                            result.add(entry);
                        }
                        c.close();
                        return result;
                    }

                    @Override
                    protected void onPostExecute(ArrayList<HotspotPageEntry> hotspotPageEntries) {
                        onHotspotEntriesLoaded(hotspotPageEntries);
                    }
                };
        mLoadDataTask.execute();
    }

    private void setupBackground() {
        Drawable bg;
        if (mHotspotItem == null || !mVisibleHotspots) {
            mBackground = null;
            setBackground(null);
            return;
        }
        if (mHotspotItem.mLeft) {
            bg = getResources().getDrawable(R.drawable.floating_navigation_drawer_handle).mutate();
        } else {
            bg = getResources().getDrawable(R.drawable.floating_navigation_drawer_handle_right)
                    .mutate();
        }

        mBackground = bg;
        setBackground(bg);
    }

    void onHotspotEntriesLoaded(ArrayList<HotspotPageEntry> hotspotPageEntries) {
        mHotspotPageEntries.clear();
        mHotspotPageEntries.addAll(hotspotPageEntries);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mHotspotsPagesObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(
                    mHotspotsPagesObserver);
            mHotspotsPagesObserver = null;
        }
        if (mLoadDataTask != null) {
            mLoadDataTask.cancel(true);
        }

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init(getContext());
    }

    void init(Context context) {

        float scale = AppsiApplication.getDensity(context);
        mMinimumMove = scale * SIDEBAR_MINIMUM_MOVE;

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        mVisibleHotspots = !sharedPreferences.getBoolean("pref_hide_hotspots", false);
        mSharedPreferencesListener = new SharedPreferencesListener(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferencesListener);

        setClickable(true);

        setupBackground();
    }

    public ArrayList<HotspotPageEntry> getHotspotPageEntries() {
        return mHotspotPageEntries;
    }

    public void setVibrateOnTouch(boolean vibrate) {
        mVibrate = vibrate;
    }

    public void setCallback(SidebarGestureCallback callback) {
        mCallback = callback;
    }

    public void setPosition(boolean left, int x, int y) {
        mLeft = left;
        mTop = y;
        mLeftPos = x;
    }

    public boolean isLeft() {
        return mLeft;
    }

    public int getHPos() {
        return mLeftPos;
    }

    public int getVPos() {
        return mTop;
    }

    void setVisibleHotspotsEnabled(boolean visibleHotspotsEnabled) {
        mVisibleHotspots = visibleHotspotsEnabled;
        setupBackground();
        invalidate();
    }

    void bind(HotspotItem hotspotItem) {
        mHotspotItem = hotspotItem;
        setupBackground();
        reloadHotspotData();
    }

    long getHotspotId() {
        return mHotspotItem.mId;
    }

    public HotspotItem getConfiguration() {
        return mHotspotItem;
    }


    public enum Gesture {
        UP,
        DOWN,
        TO_CENTER,
        TAP,
        DOUBLE_TAP,
        LONG_PRESS,
    }

    public interface SidebarGestureCallback {

        /**
         * Show the swyper
         *
         * @return true if the swyper was added and the gesture must be tracked to it's end
         */
        SwipeListener open(SidebarHotspot hotspot, Gesture action, int x, int y);

        void cancelVisualHints(SidebarHotspot sidebarHotspot);

        SwipeListener longPressGesturePerformed(SidebarHotspot hotspot, int localX, int localY,
                int rawStartY);

        void removeIfNeeded(SidebarHotspot hotspot);
    }

    public interface SwipeListener {

        /**
         * Update the location of the swype with the raw x and y coordinates
         */
        void setSwipeLocation(SidebarHotspot hotspot, int localX, int localY, int screenX,
                int screenY);

        /**
         * @param hotspot
         * @param screenX
         * @param screenY
         */
        void onSwipeEnd(SidebarHotspot hotspot, int screenX, int screenY, boolean cancelled,
                VelocityTracker velocityTracker);
    }

    static class TapGestureDetector implements Handler.Callback {

        static final int mMinMovePx = 40;

        static final int WHAT_TAP_EVENT = 1;

        final int mMinMoveDip;

        final GestureDetector.OnDoubleTapListener mOnDoubleTapListener;

        final Handler mHandler;

        boolean mCouldBeTap;

        int mStartX;

        int mStartY;

        long mLastTapMillis = -1;

        int mLastTapX = -1;

        int mLastTapY = -1;

        TapGestureDetector(Context context, GestureDetector.OnDoubleTapListener doubleTapListener) {
            mMinMoveDip = (int) (mMinMovePx * context.getResources().getDisplayMetrics().density);
            mOnDoubleTapListener = doubleTapListener;
            mHandler = new Handler(this);
        }


        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mCouldBeTap = true;
                    mStartX = (int) e.getX();
                    mStartY = (int) e.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!mCouldBeTap) break;
                    int x = (int) e.getX();
                    int y = (int) e.getY();
                    int deltaX = Math.abs(x - mStartX);
                    int deltaY = Math.abs(y - mStartY);
                    if (deltaX > mMinMoveDip || deltaY > mMinMoveDip) {
                        mCouldBeTap = false;
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mCouldBeTap = false;
                    mLastTapMillis = -1;
                    break;
                case MotionEvent.ACTION_UP:
                    if (mCouldBeTap) {
                        int tapX = (int) e.getX();
                        int tapY = (int) e.getY();
                        return onTapEvent(tapX, tapY, e);
                    }
                    break;
            }
            return false;
        }

        private boolean onTapEvent(int x, int y, MotionEvent event) {
            cancelWatchForSigleTap();
            long time = System.currentTimeMillis();
            long tapDelta = Math.abs(mLastTapMillis - time);
            if (tapDelta > 50 && tapDelta < 300) {
                int deltaX = Math.abs(x - mLastTapX);
                int deltaY = Math.abs(y - mLastTapY);
                if (deltaX <= mMinMoveDip && deltaY <= mMinMoveDip) {
                    mOnDoubleTapListener.onDoubleTap(event);
                    mLastTapMillis = -1;
                    return true;
                }
            }
            startWatchForSingleTap(x, y, event);

            mLastTapMillis = time;
            mLastTapX = x;
            mLastTapY = y;
            return false;
        }

        private void cancelWatchForSigleTap() {
            mHandler.removeMessages(WHAT_TAP_EVENT);
        }

        private void startWatchForSingleTap(int x, int y, MotionEvent event) {
            Message message = mHandler.obtainMessage(WHAT_TAP_EVENT, x, y, event);
            mHandler.sendMessageDelayed(message, 100);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_TAP_EVENT:
                    int x = msg.arg1;
                    int y = msg.arg2;
                    MotionEvent event = (MotionEvent) msg.obj;
                    handleSigleTap(x, y, event);
                    break;
            }
            return false;
        }

        private void handleSigleTap(int x, int y, MotionEvent event) {
            mOnDoubleTapListener.onSingleTapConfirmed(event);
        }

    }

    static class SharedPreferencesListener
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private final WeakReference<SidebarHotspot> mSidebarHotspot;

        SharedPreferencesListener(SidebarHotspot sidebarHotspot) {
            mSidebarHotspot = new WeakReference<>(sidebarHotspot);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            SidebarHotspot hotspot = mSidebarHotspot.get();
            if (hotspot == null) {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
                return;
            }

            if (key.equals("pref_hide_hotspots")) {
                boolean prefHideHotspots =
                        sharedPreferences.getBoolean("pref_hide_hotspots", false);
                hotspot.setVisibleHotspotsEnabled(!prefHideHotspots);

            }
        }
    }


}
