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

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.List;

/**
 * This class is the basic implementation of the sidebar. It communicates various
 * state events from Appsii to the SidebarPagerAdapter and manages the ViewPager
 */
public class Sidebar extends RelativeLayout
        implements LoaderListener, View.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        AbstractSidebarPagerAdapter.FlagListener {

    /** The delay before the sidebar is closed after an app-widget action was clicked */
    static final int DELAYED_CLOSE_DURATION = 2500;

    /**
     * The delay before the sidebar is closed after an app-widget action was clicked
     * that needs user feedback
     */
    static final int DELAYED_CLOSE_DURATION_WHEN_ASKED = 8000;

    /**
     * A bundle used to manage state in by any client after an app-widget close
     * message is being shown
     */
    final Bundle mCloseStateBundle = new Bundle();

    /**
     * A listener that can be called when the sidebar needs to be closed
     */
    SidebarListener mSidebarListener;

    /**
     * True when we are opening from the left side. False otherwise.
     * Left and right are absolute and also used in rtl locales
     */
    boolean mIsLeft;

    View mSidebarBackgroundView;

    View mLeftShadow;

    View mRightShadow;

    OnCancelCloseListener mOnCancelCloseListener;

    View mCancelClosingOverlay;

    ProgressBar mCancelClosingProgressBar;

    ObjectAnimator mClosingProgressBarAnimator;

    View mCloseText;

    View mCancelClosingOverlayAlwaysCloseButton;

    View mCancelClosingOverlayNeverCloseButton;

    private ViewPager mAppsiViewPager;

    private SidebarPagerAdapter mAdapter;


    private SidebarContext mAppsiContext;

    private final OnPageChangeListenerImpl mOnPageChangeListener = new OnPageChangeListenerImpl();

    public Sidebar(Context context) {
        super(context);
        init();
    }

    public Sidebar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Sidebar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mAppsiContext = new SidebarContext(getContext());
    }

    public void onTrimMemory(int level) {
        if (mAdapter != null) {
            mAdapter.onTrimMemory(level);
        }
    }

    public void setOnCancelCloseListener(OnCancelCloseListener onCancelCloseListener) {
        mOnCancelCloseListener = onCancelCloseListener;
    }

    void setLoaderManager(LoaderManager loaderManager) {
        mAppsiContext.setLoaderManager(loaderManager);

        mAdapter = new SidebarPagerAdapter(mAppsiContext);
        mAppsiViewPager.setAdapter(mAdapter);
        mAdapter.setFlagListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "pref_paging_animation":
                setupPagingAnimation(sharedPreferences);
                break;
        }
    }

    private void setupPagingAnimation(SharedPreferences sharedPreferences) {
        String pagingAnimation =
                sharedPreferences.getString("pref_paging_animation", "fade");
        updatePagingAnimation(pagingAnimation);
    }

    private void updatePagingAnimation(String pagingAnimation) {
        if (pagingAnimation == null) return;

        switch (pagingAnimation) {
            case "fade":
                mAppsiViewPager.setPageTransformer(true, new FadePageTransformer());
                break;
            case "depth":
                mAppsiViewPager.setPageTransformer(true, new DepthPageTransformer());
                break;
            case "zoom":
                mAppsiViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
                break;
            case "tablet":
                mAppsiViewPager.setPageTransformer(true, new TabletPageTransformer());
                break;
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            closeSidebar();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // we need to find the actionbar before we initialize the view
        // pager adapter. All views inside the adapter will assume a
        // special SidebarContext that provides access to the actionbar.
        mAppsiViewPager = (ViewPager) findViewById(R.id.appsi_view_pager);
        mAppsiViewPager.addOnPageChangeListener(mOnPageChangeListener);

        mSidebarBackgroundView = findViewById(R.id.sidebar_back);
        mLeftShadow = findViewById(R.id.sidebar_left_shadow);
        mRightShadow = findViewById(R.id.sidebar_right_shadow);

        mCloseText = findViewById(R.id.always_close_text);
        mCancelClosingOverlayAlwaysCloseButton = findViewById(R.id.always_close);
        mCancelClosingOverlayNeverCloseButton = findViewById(R.id.never_close);

        mCancelClosingOverlayAlwaysCloseButton.
                setBackground(ThemingUtils.createButtonDrawable(mAppsiContext));
        mCancelClosingOverlayNeverCloseButton.
                setBackground(ThemingUtils.createButtonDrawable(mAppsiContext));

        mCancelClosingOverlay = findViewById(R.id.widget_action_overlay);
        mCancelClosingProgressBar = (ProgressBar) findViewById(R.id.closing_progress_bar);

        mCancelClosingOverlay.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cancelClosingOverlay();
                return true;
            }
        });
        mClosingProgressBarAnimator =
                ObjectAnimator.ofInt(mCancelClosingProgressBar, "progress", 100, 0);
        mClosingProgressBarAnimator.setDuration(DELAYED_CLOSE_DURATION);

        setFocusableInTouchMode(true);

        if (!isInEditMode()) {

            findViewById(R.id.sidebar_close_area).setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    closeSidebar();
                    return true;
                }
            });
        }

        mCancelClosingOverlayAlwaysCloseButton.setOnClickListener(this);
        mCancelClosingOverlayNeverCloseButton.setOnClickListener(this);
    }

    protected void cancelClosingOverlay() {
        mOnCancelCloseListener.onCloseCancelled();
        mCancelClosingOverlay.animate().alpha(0);
        mCancelClosingOverlay.setVisibility(View.GONE);
    }

    boolean closeSidebar() {
//        mSearchBox.setText("");
        if (mSidebarListener != null) {
            mSidebarListener.onCloseSidebar();
            return true;
        }
        return false;
    }

    public void setSidebarListener(SidebarListener sidebarListener) {
        mSidebarListener = sidebarListener;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.always_close) {
            mAdapter.rememberCloseAction(mCloseStateBundle,
                    SidebarPagerAdapter.CLOSE_ACTION_AUTO_CLOSE);
            mSidebarListener.onCloseSidebar();
        } else if (id == R.id.never_close) {
            mAdapter.rememberCloseAction(mCloseStateBundle,
                    SidebarPagerAdapter.CLOSE_ACTION_KEEP_OPEN);
            cancelClosingOverlay();
        }
    }

    public void showCloseOverlay(CloseCallback closeCallback) {
        mCloseStateBundle.clear();
        int closeAction = mAdapter.shouldClose(mCloseStateBundle);

//        boolean showButtons;
//        int delay;

        switch (closeAction) {
            default:
            case SidebarPagerAdapter.CLOSE_ACTION_DONT_KNOW:
            case SidebarPagerAdapter.CLOSE_ACTION_AUTO_CLOSE:
//                showButtons = false;
//                delay = DELAYED_CLOSE_DURATION;
                closeCallback.close();
                return;
            case SidebarPagerAdapter.CLOSE_ACTION_ASK:
//                showButtons = true;
//                delay = DELAYED_CLOSE_DURATION_WHEN_ASKED;
                break;
            case SidebarPagerAdapter.CLOSE_ACTION_KEEP_OPEN:
                return;
        }

        mClosingProgressBarAnimator.setDuration(DELAYED_CLOSE_DURATION_WHEN_ASKED);
        mCancelClosingOverlay.setVisibility(View.VISIBLE);
        mCancelClosingOverlay.setAlpha(0);
        mCancelClosingOverlay.animate().alpha(1);
        mCancelClosingProgressBar.setProgress(100);
        mClosingProgressBarAnimator.start();
//        if (showButtons) {
        mCloseText.setVisibility(VISIBLE);
        mCancelClosingOverlayAlwaysCloseButton.setVisibility(VISIBLE);
        mCancelClosingOverlayNeverCloseButton.setVisibility(VISIBLE);
//        } else {
//            mCancelClosingOverlayAlwaysCloseButton.setVisibility(GONE);
//            mCancelClosingOverlayNeverCloseButton.setVisibility(GONE);
//            mCloseText.setVisibility(GONE);
//        }
        closeCallback.closeDelayed(DELAYED_CLOSE_DURATION_WHEN_ASKED);

    }

    @Override
    public void onStartLoad() {
    }

    @Override
    public void onEndLoad() {
    }

    public boolean getIsLeft() {
        return mIsLeft;
    }

    public void setIsLeft(boolean left) {
        mIsLeft = left;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            prefs.registerOnSharedPreferenceChangeListener(this);
            setupPagingAnimation(prefs);
            mAdapter.onAttachedToWindow();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCancelClosingOverlay.setVisibility(GONE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        mAdapter.onDetachedFromWindow();
    }

    public void updateAdapterData(List<HotspotPageEntry> entries) {
        mAdapter.setPages(entries);
    }

    @Override
    public void onFlagsChanged(int flags) {
        handleNewFlags(flags);
    }

    private void handleNewFlags(int flags) {
        if ((flags & SidebarPagerAdapter.FLAG_NO_DECORATIONS) ==
                AbstractSidebarPagerAdapter.FLAG_NO_DECORATIONS) {
            mSidebarBackgroundView.animate().alpha(.5f);
            mLeftShadow.animate().alpha(.5f);
            mRightShadow.animate().alpha(.5f);
        } else {
            mSidebarBackgroundView.animate().alpha(1);
            mLeftShadow.animate().alpha(1);
            mRightShadow.animate().alpha(1);
        }
    }

    public void setInFullScreenMode(boolean fullscreen) {
        mAppsiContext.mIsFullScreen = fullscreen;
    }

    public void setSidebarOpening(boolean sidebarOpening) {
        mAdapter.setSidebarOpening(sidebarOpening);
    }

    public void onSidebarClosing() {
        mAdapter.setSidebarClosed();
    }

    public interface OnCancelCloseListener {

        void onCloseCancelled();
    }

    public interface SidebarListener {

        void onCloseSidebar();


    }

    @TargetApi(11)
    public static class DepthPageTransformer extends AbstractPageTransformer {

        private static final float MIN_SCALE = 0.75f;

        @Override
        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            reset(view);

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left page
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);

            } else if (position <= 1) { // (0,1]
                // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE
                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
            }
        }
    }

    public static class ZoomOutPageTransformer extends AbstractPageTransformer {

        private static final float MIN_SCALE = 0.85f;

        private static final float MIN_ALPHA = 0.5f;

        @Override
        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();
            reset(view);

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
            }
        }
    }

    public static class TabletPageTransformer extends AbstractPageTransformer {

        private static float MIN_SCALE = 0.85f;

        private static float MIN_ALPHA = 0.5f;

        public TabletPageTransformer() {
        }

        @Override
        public void transformPage(View view, float position) {
            reset(view);
            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
            } else if (position <= 1) { // [-1, 0]
                view.setRotationY(position * -30);
            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
            }
        }
    }

    public static class FadePageTransformer extends AbstractPageTransformer {

        @Override
        public void transformPage(View view, float position) {
            reset(view);
            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
            } else if (position <= 0) { // [-1, 0]
                view.setAlpha(position + 1);
            } else if (position <= 1) { // [0 ,1]
                view.setAlpha(1 - position);
            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
            }
        }
    }

    static abstract class AbstractPageTransformer implements ViewPager.PageTransformer {

        public void reset(View view) {
            view.setAlpha(1);
            view.setRotationX(0);
            view.setRotationY(0);
            view.setTranslationX(0);
            view.setScaleX(1);
            view.setScaleY(1);
        }
    }

    class OnPageChangeListenerImpl implements ViewPager.OnPageChangeListener {

        int mCurrentPage;

        @Override
        public void onPageScrolled(int i, float v, int i2) {
        }

        @Override
        public void onPageSelected(int i) {
            mCurrentPage = i;
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }

    }

}
