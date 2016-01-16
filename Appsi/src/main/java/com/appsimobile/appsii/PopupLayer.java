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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.appsimobile.appsii.dagger.AppInjector;

import javax.inject.Inject;

/**
 * A wrapper around the window-manager, intended to make adding and removing
 * different components easier. For example this adds the root-layer, that
 * allows Appsii to animate different floating layers
 */
public class PopupLayer extends FrameLayout {

    /**
     * The last known alpha. So we know if it has changed
     */
    final float mLastAlpha = -1;
    /**
     * The window-manager we will be adding stuff to.
     */
    @Inject
    WindowManager mWindowManager;
    /**
     * The default dim amount that is used for Appsii
     */
    float mDefaultDimAlpha;
    /**
     * True when the root layer is added
     */
    private boolean mAdded;

    private View mDimLayerView;

    private boolean mDimLayerVisible;

    private int mChildCount;

    private PopupLayerListener mPopuplayerListener;

    public PopupLayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PopupLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PopupLayer(Context context) {
        super(context);
    }

    public void setDefaultDimAlpha(float pct) {
        mDefaultDimAlpha = pct;
        if (mDefaultDimAlpha > 1) mDefaultDimAlpha = 1;
        if (mDefaultDimAlpha < 0) mDefaultDimAlpha = 0;
    }

    public void addPopupChild(View view) {
        addPopupChild(view, true);
    }

    public void addPopupChild(View view, boolean bringToFront) {
        if (mChildCount == 0) {
            addLayer();
            showDimLayer();
        }
        mChildCount++;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }
        ViewParent vp = view.getParent();
        if (vp != null) {
            ((ViewGroup) vp).removeView(view);
        }
        addView(view, lp);
        setDimLayerAlpha(.5f);
        if (bringToFront) {
            view.bringToFront();
        }
    }

    public void addLayer() {
        if (!mAdded) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);

            addHardwareAcceleratedFlag(lp);
            addKitkatFlags(lp);

            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;

            mWindowManager.addView(this, lp);
            mAdded = true;
        } else {
            setVisibility(VISIBLE);
            if (mPopuplayerListener != null) {
                mPopuplayerListener.opPopupLayerShown();
            }

        }
    }

    private boolean showDimLayer() {
        if (mDimLayerVisible) return false;

        mDimLayerVisible = true;
        if (!AppsiApplication.API19) {
            mDimLayerView.setVisibility(View.VISIBLE);
        }
        return true;
    }

    public void setDimLayerAlpha(float newAlpha) {
        if (mLastAlpha == newAlpha) return;
        if (AppsiApplication.API19) {

            WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
            if (params != null) {
                params.dimAmount = newAlpha * mDefaultDimAlpha;
                if (mAdded) {
                    mWindowManager.updateViewLayout(this, params);
                }
            }
        } else {
            float pct = newAlpha * mDefaultDimAlpha;
            int alpha = (int) (255 * pct);
            if (alpha > 255) alpha = 255;
            if (alpha < 0) alpha = 0;
            alpha = alpha << 24;
            int color = alpha;

            mDimLayerView.setBackgroundColor(color);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void addHardwareAcceleratedFlag(WindowManager.LayoutParams params) {
        params.flags = params.flags | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void addKitkatFlags(WindowManager.LayoutParams params) {
        if (AppsiApplication.API19) {
            params.flags = params.flags |
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;

            params.dimAmount = mDefaultDimAlpha;
        }
    }

    public void removePopupChild(View view) {
        removeView(view);
        mChildCount--;
        if (mChildCount <= 0) {
            removeDimLayer();
            removeLayer();
            setDimLayerAlpha(1f);
            mChildCount = 0;
        }
    }

    private boolean removeDimLayer() {
        if (!mDimLayerVisible) return false;
        mDimLayerVisible = false;
        if (!AppsiApplication.API19) {
            mDimLayerView.setVisibility(View.GONE);
        }
        return true;
    }

    public void removeLayer() {
        if (mAdded) {
            mWindowManager.removeView(this);
            mAdded = false;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK ||
                event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
            Intent i = new Intent(Appsi.ACTION_CLOSE_SIDEBAR);
            getContext().sendBroadcast(i);
            forceClose();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        AppInjector.inject(this);

        mDimLayerView = findViewById(R.id.dim_view);
        if (AppsiApplication.API19) {
            mDimLayerView.setVisibility(GONE);
        }
        setFocusableInTouchMode(true);
    }

    public void forceClose() {
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View v = getChildAt(i);
            if (v == mDimLayerView) continue;
            removeView(v);
        }
        if (mPopuplayerListener != null) {
            mPopuplayerListener.onPopupLayerForceClosed();
        }
        removeDimLayer();
        removeLayer();
        mChildCount = 0;
    }

    public void setPopuplayerListener(PopupLayerListener listener) {
        mPopuplayerListener = listener;
    }

    public void onSuspend() {
        if (mAdded) {
            mWindowManager.removeView(this);
        }
        mAdded = false;
    }

    public interface PopupLayerListener {

        void onPopupLayerForceClosed();

        void opPopupLayerHidden() throws PermissionDeniedException;

        void opPopupLayerShown();
    }

}
