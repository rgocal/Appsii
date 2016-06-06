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

package com.appsimobile.appsii.module.home;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.StackView;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.LoaderManager;
import com.appsimobile.appsii.PageController;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.SidebarContext;
import com.appsimobile.appsii.appwidget.AppsiiAppWidgetHost;
import com.appsimobile.appsii.appwidget.AppsiiAppWidgetHost.AppsiAppWidgetHostView;
import com.appsimobile.appsii.module.PermissionHelper;
import com.appsimobile.appsii.module.ToolbarScrollListener;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 10/08/14.
 */
public class HomePageController extends PageController implements Toolbar.OnMenuItemClickListener,
        HomeAdapter.PermissionErrorListener, PermissionHelper.PermissionListener {

    // TODO implement start/stop on primary item switch

    private static final int HOME_LOADER_ID = 441001;

    final LoaderManager.LoaderCallbacks<List<HomeItem>> mHomeLoaderCallbacks =
            new HomeLoaderCallbacks();

    final long mPageId;

    final SidebarContext mContext;

    RecyclerView mRecyclerView;

    GridLayoutManager mLayoutManager;

    @Nullable
    HomeAdapter mHomeAdapter;

    Toolbar mToolbar;

    boolean mLoadsDeferred;

    boolean mUserVisible;

    RecyclerViewTouchListener mRecyclerViewTouchListener;

    ViewGroup mPermissionOverlay;

    ArrayList<String> mDismissedPermissions;

    PendingPermissionError mPendingPermissionError;

    private Rect mRect;

    public HomePageController(Context context, long pageId, String title) {
        super(context, title);
        mPageId = pageId;
        mContext = (SidebarContext) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_item_home, parent, false);
    }

    @Override
    protected void onViewDestroyed(View view) {

    }

    @Override
    protected void onViewCreated(View view, Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.home_recycler);
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(mTitle);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mHomeAdapter);
        mRecyclerView.addItemDecoration(new GridLayoutDecoration(getContext()));

        mPermissionOverlay = (ViewGroup) view.findViewById(R.id.permission_overlay);

        mRecyclerViewTouchListener = new RecyclerViewTouchListener();
        mRecyclerView.addOnItemTouchListener(mRecyclerViewTouchListener);
        mRecyclerView.addOnScrollListener(new ToolbarScrollListener(this, mToolbar));

        MenuInflater menuInflater = new MenuInflater(getContext());
        menuInflater.inflate(R.menu.page_home, mToolbar.getMenu());
        mToolbar.setOnMenuItemClickListener(this);
        if (mPendingPermissionError != null) {
            showPendingPermissionError(mPendingPermissionError);
        }
    }

    @Override
    protected void onUserVisible() {
        super.onUserVisible();
        trackPageView(AnalyticsManager.CATEGORY_HOME);
        mUserVisible = true;
        updateHomeAdapterRunningStatus();

    }

    private void updateHomeAdapterRunningStatus() {
        if (mHomeAdapter != null) {
            boolean started = !mLoadsDeferred && mSidebarAttached && mUserVisible;
            mHomeAdapter.setStarted(started);
        }
    }

    @Override
    protected void onUserInvisible() {
        super.onUserInvisible();
        mUserVisible = false;
        updateHomeAdapterRunningStatus();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHomeAdapter = new HomeAdapter(getContext(), mPageId);
        mHomeAdapter.setPermissionErrorListener(this);
        mLayoutManager = new GridLayoutManager(getContext(), 12);
        HomeAdapter.HomeSpanSizeLookup spanSizeLookup = mHomeAdapter.getHomeSpanSizeLookup();
        mLayoutManager.setSpanSizeLookup(spanSizeLookup);
        getLoaderManager().initLoader(HOME_LOADER_ID, null, mHomeLoaderCallbacks);
        if (savedInstanceState != null) {
            mDismissedPermissions =
                    savedInstanceState.getStringArrayList("dismissed_permission_errors");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("dismissed_permission_errors", mDismissedPermissions);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (mHomeAdapter != null) {
            mHomeAdapter.onTrimMemory(level);
        }
    }

    @Override
    protected void applyToolbarColor(int color) {

    }

    @Override
    public void setDeferLoads(boolean postponeLoads) {
        super.setDeferLoads(postponeLoads);
        mLoadsDeferred = postponeLoads;
        HomeViewWrapper.deferLoads(postponeLoads);
        updateHomeAdapterRunningStatus();
    }

    @Override
    public void onSidebarAttached() {
        super.onSidebarAttached();
        updateHomeAdapterRunningStatus();
    }

    @Override
    public void onSidebarDetached() {
        super.onSidebarDetached();
        updateHomeAdapterRunningStatus();
    }

    @Override
    public int shouldClose(Bundle state) {
        if (mHomeAdapter == null) return PageController.CLOSE_ACTION_AUTO_CLOSE;

        int lastX = mRecyclerViewTouchListener.mTouchDownX;
        int lastY = mRecyclerViewTouchListener.mTouchDownY;
        AppWidgetHostView hostView = findAppWidgetHostView(mRecyclerView, lastX, lastY);
        if (hostView != null) {
            View clicked = findAppWidgetButton(mRecyclerView, lastX, lastY);
            if (clicked != null && clicked.getId() != View.NO_ID) {
                return doShouldClose(hostView, clicked, state);
            }
        }
        return super.shouldClose(state);
    }

    @Override
    public void rememberCloseAction(Bundle state, int action) {
        if (action == CLOSE_ACTION_AUTO_CLOSE || action == CLOSE_ACTION_KEEP_OPEN) {
            long homeItemId = state.getLong("home_item_id", -1L);
            String viewName = state.getString("view_name");

            HomeItemConfiguration helper =
                    HomeItemConfigurationHelper.getInstance(mContext);


            if (action == CLOSE_ACTION_AUTO_CLOSE) {
                String closeButtonNames =
                        helper.getProperty(homeItemId, "always_close_buttons", null);
                if (closeButtonNames == null) {
                    closeButtonNames = viewName;
                } else {
                    closeButtonNames += "," + viewName;
                }
                helper.updateProperty(homeItemId, "always_close_buttons", closeButtonNames);
            } else {
                String keepOpenButtonNames =
                        helper.getProperty(homeItemId, "keep_open_buttons", null);
                if (keepOpenButtonNames == null) {
                    keepOpenButtonNames = viewName;
                } else {
                    keepOpenButtonNames += "," + viewName;
                }
                helper.updateProperty(homeItemId, "keep_open_buttons", keepOpenButtonNames);
            }
        }

    }

    private boolean isScrollableWidgetView(View view) {
        return view instanceof GridView || view instanceof ListView || view instanceof StackView;
    }

    @Nullable
    View findScrollableWidgetView(ViewGroup container, int x, int y) {
        if (mRect == null) {
            mRect = new Rect();
        }
        final Rect r = mRect;
        final int count = container.getChildCount();
        final int scrolledX = x + container.getScrollX();
        final int scrolledY = y + container.getScrollY();
        //final View ignoredDropTarget = (View) mIgnoredDropTarget;

        for (int i = count - 1; i >= 0; i--) {
            final View child = container.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(r);
                if (r.contains(scrolledX, scrolledY)) {
                    View target = null;
                    if (child instanceof ViewGroup) {
                        x = scrolledX - child.getLeft();
                        y = scrolledY - child.getTop();
                        target = findScrollableWidgetView((ViewGroup) child, x, y);
                    }
                    if (target == null) {
                        if (isScrollableWidgetView(child)) {
                            return child;
                        }
                    } else {
                        if (isScrollableWidgetView(target)) {
                            return target;
                        }
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    AppsiAppWidgetHostView findAppWidgetHostView(ViewGroup container, int x, int y) {
        if (mRect == null) {
            mRect = new Rect();
        }
        final Rect r = mRect;
        final int count = container.getChildCount();
        final int scrolledX = x + container.getScrollX();
        final int scrolledY = y + container.getScrollY();
        //final View ignoredDropTarget = (View) mIgnoredDropTarget;

        for (int i = count - 1; i >= 0; i--) {
            final View child = container.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(r);
                if (r.contains(scrolledX, scrolledY)) {
                    if (child instanceof AppWidgetHostView) {
                        return (AppsiAppWidgetHostView) child;
                    }
                    if (child instanceof ViewGroup) {
                        x = scrolledX - child.getLeft();
                        y = scrolledY - child.getTop();
                        return findAppWidgetHostView((ViewGroup) child, x, y);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    View findAppWidgetButton(ViewGroup container, int x, int y) {
        if (mRect == null) {
            mRect = new Rect();
        }
        final Rect r = mRect;
        final int count = container.getChildCount();
        final int scrolledX = x + container.getScrollX();
        final int scrolledY = y + container.getScrollY();
        //final View ignoredDropTarget = (View) mIgnoredDropTarget;

        for (int i = count - 1; i >= 0; i--) {
            final View child = container.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(r);
                if (r.contains(scrolledX, scrolledY)) {
                    if (child instanceof ViewGroup) {
                        x = scrolledX - child.getLeft();
                        y = scrolledY - child.getTop();
                        View result = findAppWidgetButton((ViewGroup) child, x, y);
                        if (result != null) return result;
                    }
                    if (child.getId() != View.NO_ID && child.isClickable()) {
                        return child;
                    }
                }
            }
        }

        return null;
    }

    void onHomeItemsLoaded(List<HomeItem> data) {
        // clear any permission errors that might be pending
        if (mPermissionOverlay != null) {
            mPermissionOverlay.removeAllViews();
        }
        if (mHomeAdapter == null) {
            FirebaseCrash.report(new NullPointerException("adapter not initialized?!?"));
            return;
        }
        mHomeAdapter.setHomeItems(data);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_edit_layout) {
            showLayoutEditor();
            return true;
        }
        return false;
    }

    private void showLayoutEditor() {
        Intent intent = new Intent(getContext(), HomeEditorActivity.class);
        intent.putExtra(HomeEditorActivity.EXTRA_PAGE_ID, mPageId);
        intent.putExtra(HomeEditorActivity.EXTRA_PAGE_TITLE, mTitle);
        getContext().startActivity(intent);
    }

    private int doShouldClose(AppWidgetHostView hostView, View clicked, Bundle state) {
        if (mHomeAdapter == null) return CLOSE_ACTION_DONT_KNOW;

        state.clear();

        int appWidgetId = hostView.getAppWidgetId();

        String viewName = getViewName(hostView.getAppWidgetInfo(), clicked);
        HomeItem homeItem = mHomeAdapter.getHomeItemForAppWidgetId(appWidgetId);

        HomeItemConfiguration helper =
                HomeItemConfigurationHelper.getInstance(mContext);

        String closeButtonNames = helper.getProperty(homeItem.mId, "always_close_buttons", null);
        String keepOpenButtonNames = helper.getProperty(homeItem.mId, "keep_open_buttons", null);

        String[] closeArr = splitArray(closeButtonNames);
        String[] openArr = splitArray(keepOpenButtonNames);

        if (arrayContains(viewName, closeArr)) return CLOSE_ACTION_AUTO_CLOSE;
        if (arrayContains(viewName, openArr)) return CLOSE_ACTION_KEEP_OPEN;

        state.putLong("home_item_id", homeItem.mId);
        state.putString("view_name", viewName);

        return CLOSE_ACTION_ASK;
    }

    <T> boolean arrayContains(T t, T[] ts) {
        if (ts == null) return false;
        int N = ts.length;

        for (int i = 0; i < N; i++) {
            if (t.equals(ts[i])) return true;
        }
        return false;
    }

    private String[] splitArray(String s) {
        return s == null ? null : s.split(",");
    }

    @Nullable
    String getViewName(AppWidgetProviderInfo info, View view) {
        ComponentName provider = info.provider;
        Resources resources;
        try {
            PackageManager packageManager = mContext.getPackageManager();
            resources = packageManager.getResourcesForApplication(provider.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf("Home", "App not installed??", e);
            return null;
        }

        return resources.getResourceEntryName(view.getId());
    }

    @Override
    public void onPermissionDenied(String permission, String id, @StringRes int textResId) {
        if (PermissionUtils.shouldShowPermissionError(mContext, id)) {
            if (mPermissionOverlay == null) {
                if (mPendingPermissionError == null) {
                    mPendingPermissionError = new PendingPermissionError(permission, id, textResId);
                }
            } else {
                showPermissionError(permission, id, textResId);
            }
        }
    }

    private void showPendingPermissionError(PendingPermissionError pendingPermissionError) {
        showPermissionError(pendingPermissionError.mPermission,
                pendingPermissionError.mId, pendingPermissionError.mTextResId);
    }

    private void showPermissionError(String permission, String id, @StringRes int textResId) {
        PermissionHelper permissionHelper =
                new PermissionHelper(textResId, true, this, permission);
        mPermissionOverlay.setTag(id);
        permissionHelper.show(mPermissionOverlay);
    }

    @Override
    public void onAccepted(PermissionHelper permissionHelper) {
        Intent intent = PermissionUtils.
                buildRequestPermissionsIntent(mContext, 1, permissionHelper.getPermissions());
        mContext.startActivity(intent);
    }

    @Override
    public void onCancelled(PermissionHelper permissionHelper, boolean dontShowAgain) {
        if (dontShowAgain) {
            String id = (String) mPermissionOverlay.getTag();
            PermissionUtils.setDontShowPermissionAgain(mContext, id);
        }

    }

    static class PendingPermissionError {

        final String mPermission;

        final String mId;

        @StringRes
        final int mTextResId;

        PendingPermissionError(String permission, String id, int textResId) {
            mPermission = permission;
            mId = id;
            mTextResId = textResId;
        }
    }

    class HomeLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<HomeItem>> {

        @Override
        public Loader<List<HomeItem>> onCreateLoader(int id, Bundle args) {
            return new HomeLoader(getContext());
        }

        @Override
        public void onLoadFinished(Loader<List<HomeItem>> loader, List<HomeItem> data) {
            onHomeItemsLoaded(data);
        }

        @Override
        public void onLoaderReset(Loader<List<HomeItem>> loader) {

        }
    }

    private class RecyclerViewTouchListener implements RecyclerView.OnItemTouchListener {

        final Rect mRect = new Rect();

        final int mTouchSlop;

        AppsiiAppWidgetHost.CapturedEventQueue mEventQueue;

        int mTouchDownX;

        int mTouchDownY;

        AppWidgetHostView mHostView;

        RecyclerViewTouchListener() {
            mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop() / 2;
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            int action = e.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                // Clear previous state, in case we never received the up/cancel.
                // According to the source in ViewGroup this may happen (see below).
                //
                // Throw away all previous state when starting a new touch gesture.
                // The framework may have dropped the up or cancel event for the previous gesture
                // due to an app switch, ANR, or some other state change.
                if (mEventQueue != null) {
                    mEventQueue.release();
                    mEventQueue = null;
                }
                mHostView = null;

                // remember initial touchdown position
                int x = (int) e.getX();
                int y = (int) e.getY();

                mTouchDownX = x;
                mTouchDownY = y;

                // Find out if an app-widget-host-view is below the given x/y
                AppsiAppWidgetHostView hostView = findAppWidgetHostView(rv, x, y);
                if (hostView == null) {
                    // if not the touch down was outside the widget area; ignore the event
                    mEventQueue = null;
                    return false;
                }


                // if it was on an app-widget, find out if it was on a scrollable widget
                // and if so, get it's capture queue.
                // The capture queue is a special queue we can send events to. The widget
                // will ignore all touch events coming from other sources than the queue
                View scrollableWidgetView = findScrollableWidgetView(rv, x, y);

                if (scrollableWidgetView != null) {
                    mEventQueue = hostView.captureEventQueue();
                }

                if (mEventQueue != null) {
                    mHostView = hostView;
                    dispatchTouchEvent(rv, e, mHostView);
                } else {
                    mHostView = null;
                }

                return false;
            } else if (mEventQueue != null && action == MotionEvent.ACTION_MOVE) {
                Log.i("Home", "Move on hostview; starting capture");
                return true;
            } else if (mEventQueue != null &&
                    (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)) {
                Log.i("Home", "Up/cancel on hostview (dispatching); finishing capture");


                dispatchTouchEvent(rv, e, mHostView);

                mEventQueue.release();
                mEventQueue = null;
                mHostView = null;

                return false;
            }

            return mEventQueue != null;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            dispatchTouchEvent(rv, e, mHostView);
            int action = e.getAction();
            // clear the current queue on cancel or up
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                if (mEventQueue != null) {
                    mEventQueue.release();
                    mEventQueue = null;
                } else {
                    // This can't really be null at this point
                    FirebaseCrash.report(new NullPointerException("Should not happen"));
                }
            }
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }

        private void dispatchTouchEvent(RecyclerView rv, MotionEvent e,
                final AppWidgetHostView delegateView) {
            float ox = (int) e.getX();
            float oy = (int) e.getY();


            // Offset event coordinates to be inside the target view
            mRect.set((int) ox, (int) oy, 0, 0);
            rv.offsetRectIntoDescendantCoords(delegateView, mRect);

            int x = mRect.left;
            int y = mRect.top;

            final MotionEvent copy = MotionEvent.obtain(e);
            // deliver the event
            copy.setLocation(x, y);
            mEventQueue.dispatchTouchEvent(copy);

        }
    }

}
