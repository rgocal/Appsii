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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.appsimobile.annotation.KeepName;
import com.appsimobile.appsii.annotation.VisibleForTesting;

/**
 * A simple wrapper for a page, used to save instance state of the
 * view and persist it's state while the sidebar is closed.
 * Functions mostly similar to a normal fragment
 */
public abstract class PageController implements ViewTreeObserver.OnGlobalLayoutListener {

    protected static final int CLOSE_ACTION_AUTO_CLOSE = 1;

    protected static final int CLOSE_ACTION_KEEP_OPEN = 2;

    protected static final int CLOSE_ACTION_DONT_KNOW = 3;

    protected static final int CLOSE_ACTION_ASK = 4;

    protected final int mPrimaryColor;

    protected final int mAccentColor;

    protected final int mDefaultTintColor;

    protected final String mTitle;

    private final SidebarContext mContext;

    protected boolean mSidebarAttached;

    @VisibleForTesting
    boolean mUserVisible;

    private HotspotPageEntry mPage;

    private View mView;

    private SparseArray<Parcelable> mSavedHierarchyState = new SparseArray<>();

    private Bundle mClientSaveState = new Bundle();

    public PageController(Context context, String title) {
        mContext = (SidebarContext) context;
        mTitle = title;

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{
                R.attr.colorPrimary,
                R.attr.appsiMenuItemTint,
                R.attr.colorAccent,
        });
        mPrimaryColor = a.getColor(0, Color.WHITE);
        mDefaultTintColor = a.getColor(1, Color.BLACK);
        mAccentColor = a.getColor(2, Color.BLUE);
        a.recycle();
    }

    public HotspotPageEntry getPage() {
        return mPage;
    }

    public void setPage(HotspotPageEntry page) {
        mPage = page;
    }

    public LoaderManager getLoaderManager() {
        return mContext.getLoaderManager();
    }

    public int getContentWidth() {
        return mContext.getContentWidth();
    }

    public Context getContext() {
        return mContext;
    }

    public void track(String action, String category) {
        mContext.track(action, category);
    }

    public void trackPageView(String page) {
        mContext.trackPageView(page);
    }

    public void track(String action, String category, String label) {
        mContext.track(action, category, label);
    }

    public Resources getResources() {
        return mContext.getResources();
    }

    /**
     * Creates the view, calls onCreateView with the saved instance state
     */
    public final View performCreateView(LayoutInflater inflater, ViewGroup container) {
        return onCreateView(inflater, container, mClientSaveState);
    }

    public abstract View onCreateView(LayoutInflater inflater,
            ViewGroup parent, Bundle savedInstanceState);

    public final void performDestroy() {
        onDestroy();
        onViewDestroyed(mView);
        mView = null;
    }

    public void onDestroy() {

    }

    /**
     * Called after onDetach in case the adapter decides to
     * destroy the view
     */
    protected void onViewDestroyed(View view) {

    }

    public final void performViewCreated(View view) {
        mView = view;
        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        // Note: This happens in test cases.
        if (viewTreeObserver != null) {
            viewTreeObserver.addOnGlobalLayoutListener(this);
        }
        onViewCreated(view, mClientSaveState);
        postViewCreated(view);
    }

    protected abstract void onViewCreated(View view, Bundle savedInstanceState);

    private void postViewCreated(View view) {
        StatusbarUnderlay underlay = (StatusbarUnderlay) view.findViewById(R.id.underlay);
        if (underlay != null) {
            underlay.setSidebarContext(mContext);
        }
    }

    public final void performStart() {
        onStart();
    }

    protected void onStart() {

    }

    public final void performPause() {
        onPause();
    }

    protected void onPause() {

    }

    public final void performStop() {
        onStop();
    }

    protected void onStop() {

    }

    public final void performRestoreInstanceState() {
        restoreViewState();
    }

    private void restoreViewState() {
        if (mSavedHierarchyState != null) {
            mView.restoreHierarchyState(mSavedHierarchyState);
        }
    }

    public final void performResume() {
        onResume();
    }

    protected void onResume() {

    }

    /**
     * This page has become the visible page in the adapter
     */
    protected void onUserVisible() {

    }

    /**
     * This page is no longer the visible page in the adapter.
     */
    protected void onUserInvisible() {

    }

    @VisibleForTesting
    public final void performCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
        onCreate(mClientSaveState);
    }

    private void restoreInstanceState(@NonNull Bundle savedInstanceState) {
        mSavedHierarchyState = savedInstanceState.getSparseParcelableArray("viewState");
        mClientSaveState = savedInstanceState.getBundle("clientState");
    }

    protected void onCreate(Bundle savedInstanceState) {

    }

    final void performSaveInstanceState(Bundle outState) {
        saveViewState();

        outState.putSparseParcelableArray("viewState", mSavedHierarchyState);

        mClientSaveState.clear();
        onSaveInstanceState(mClientSaveState);
        outState.putBundle("clientState", mClientSaveState);
    }

    private void saveViewState() {
        mSavedHierarchyState.clear();
        mView.saveHierarchyState(mSavedHierarchyState);
    }

    /**
     * Client hook to save the view state. Will be called before destroying
     * the view. Will be provided in onCreate, onCreateView and onViewCreated
     */
    protected void onSaveInstanceState(Bundle outState) {

    }

    public final void performOnAttach() {
        onAttach();
    }

    protected void onAttach() {

    }

    public final void performOnDetach() {
        onDetach();
    }

    protected void onDetach() {

    }

    @Override
    public void onGlobalLayout() {
        onFirstLayout();
        // Can mView ever be null here?
        ViewTreeObserver viewTreeObserver = mView == null ? null : mView.getViewTreeObserver();
        if (viewTreeObserver != null) {
            viewTreeObserver.removeOnGlobalLayoutListener(this);
        }
    }

    protected void onFirstLayout() {

    }

    public final View getView() {
        return mView;
    }

    public boolean isUserVisible() {
        return mUserVisible;
    }

    public void setUserVisible(boolean userVisible) {
        mUserVisible = userVisible;
    }

    public void onTrimMemory(int level) {

    }

    @KeepName
    public void setToolbarBackgroundAlpha(float visiblePct) {
        // 0 = completely hidden
        // 1 = completely visible
        if (visiblePct == 0) {
            applyToolbarColor(Color.TRANSPARENT);
        } else {
            int color = evaluate(visiblePct, mPrimaryColor & 0x00FFFFFF, mPrimaryColor);
            applyToolbarColor(color);
        }
    }

    protected abstract void applyToolbarColor(int color);

    public int evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (startA + (int) (fraction * (endA - startA))) << 24 |
                (startR + (int) (fraction * (endR - startR))) << 16 |
                (startG + (int) (fraction * (endG - startG))) << 8 |
                (startB + (int) (fraction * (endB - startB)));
    }

    /**
     * Can be implemented by clients to indicate certain desired behaviors
     * to the sidebar. For example to hide the background
     */
    public int getFlags() {
        return 0;
    }

    public void setDeferLoads(boolean deferLoads) {
    }

    public void onSidebarAttached() {
        mSidebarAttached = true;
    }

    public void onSidebarDetached() {
        mSidebarAttached = false;
    }

    /**
     * Called to identify if the sidebar should close. Expensive state can be saved in the bundle
     */
    public int shouldClose(Bundle state) {
        return SidebarPagerAdapter.CLOSE_ACTION_DONT_KNOW;
    }

    /**
     * Called when the user would like to remember a action for a specific button.
     * The provided state is the same state provided to the shouldClose call.
     */
    public void rememberCloseAction(Bundle state, int action) {

    }
}
