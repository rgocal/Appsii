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

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 20/01/15.
 */
public class HomeViewWrapper extends FrameLayout {

    static final List<HomeItemListener> ACTIVATION_LISTENERS = new ArrayList<>();

    private static boolean sAllowLoads;

    int mRowHeight;

    HomeItemListener mHomeItemListener;

    boolean mDelegatingTouchEvents;

    public HomeViewWrapper(Context context) {
        super(context);
    }

    public HomeViewWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HomeViewWrapper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static boolean areLoadsDeferred() {
        return !sAllowLoads;
    }

    public static void deferLoads(boolean defer) {
        sAllowLoads = !defer;
        if (sAllowLoads) {
            runDelayedListeners();
        }
    }

    private static void runDelayedListeners() {
        for (HomeItemListener l : ACTIVATION_LISTENERS) {
            l.onAllowLoads();
        }
        ACTIVATION_LISTENERS.clear();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mDelegatingTouchEvents = false;
        if (mHomeItemListener != null) {
            mDelegatingTouchEvents = mHomeItemListener.onInterceptTouchEvent(ev);
            if (mDelegatingTouchEvents) return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mHomeItemListener != null) {
            mHomeItemListener.onAttached(sAllowLoads);
            if (!sAllowLoads) {
                ACTIVATION_LISTENERS.add(mHomeItemListener);
            } else {
                mHomeItemListener.onAllowLoads();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mHomeItemListener != null) {
            mHomeItemListener.onDetached(sAllowLoads);

            ACTIVATION_LISTENERS.remove(mHomeItemListener);
            if (sAllowLoads) {
                mHomeItemListener.onDisallowLoads();
            }
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (mDelegatingTouchEvents && mHomeItemListener != null) {
            return mHomeItemListener.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mHomeItemListener != null) {
            mHomeItemListener.onSizeChanged(w, h, oldw, oldh);
        }
    }

    public void setHomeItemListener(HomeItemListener homeItemListener) {
        if (mHomeItemListener != null) {
            ACTIVATION_LISTENERS.remove(mHomeItemListener);
        }
        mHomeItemListener = homeItemListener;
    }

    public void setRowHeight(int rowHeight) {
        if (mRowHeight != rowHeight) {
            mRowHeight = rowHeight;
            ViewGroup.LayoutParams layoutParams = getLayoutParams();

            if (layoutParams != null) {
                layoutParams.height = rowHeight;
                ViewGroup parent = (ViewGroup) getParent();
                if (parent != null) {
                    parent.updateViewLayout(this, layoutParams);
                }
            }
        }
    }

    static interface HomeItemListener {

        /**
         * Called when the home view has become active and is allowed to load
         * data
         */
        void onAllowLoads();

        /**
         * Called when the views size has changed. Usually called before onAllowLoads.
         * This should not be a trigger to load data
         */
        void onSizeChanged(int w, int h, int oldw, int oldh);

        /**
         * Called when the home view has become inactive and is no longer allowed
         * to load data
         */
        void onDisallowLoads();

        /**
         * Called when the items has become visible. Do not load data from here
         */
        void onAttached(boolean allowLoads);

        /**
         * Called when the items has become invisible. Do not load data from here
         */
        void onDetached(boolean allowLoads);

        boolean onInterceptTouchEvent(MotionEvent e);

        boolean onTouchEvent(MotionEvent e);

    }

}
