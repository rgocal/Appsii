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
import android.graphics.PixelFormat;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;

import java.util.ArrayList;

/**
 * Created by nick on 13/02/15.
 */
public abstract class AbstractHotspotHelper {

    final int mDp56;

    private final Context mContext;

    private int mHeight;

    public AbstractHotspotHelper(Context context) {
        mContext = context;
        mDp56 = (int) (context.getResources().getDisplayMetrics().density * 56);
    }

    public abstract void onOrientationChanged();

    public abstract void onDestroy();

    public abstract void setVibrate(boolean mVibrate);

    public abstract void addHotspots();

    public abstract void removeHotspots();

    public abstract void onHotspotsLoaded(ArrayList<HotspotItem> configurations);

    public abstract int getTopOffset();

    protected WindowManager.LayoutParams createInsetParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSPARENT);
        layoutParams.gravity = Gravity.TOP;
        layoutParams.y = 0;
        return layoutParams;
    }

    protected WindowManager.LayoutParams updateLayoutParams(boolean left, int y,
            WindowManager.LayoutParams params) {
        if (left) {
            params.gravity = Gravity.LEFT | Gravity.TOP;
        } else {
            params.gravity = Gravity.RIGHT | Gravity.TOP;
        }
        params.y = y;
        return params;
    }

    @SuppressWarnings("deprecation")
    protected WindowManager.LayoutParams createHotspotParams(HotspotItem conf,
            SharedPreferences prefs) {

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        mHeight = display.getHeight();


        float pct = conf.mHeightRelativeToViewHeight;
        int realHeight = (int) (mHeight * pct);
        if (realHeight < mDp56) realHeight = mDp56;

        int y = (int) (conf.mYPosRelativeToView * mHeight);

        int hotspotWidth = prefs.getInt("pref_hotspot_width", 22);

        int width = (int) (hotspotWidth * AppsiApplication.getDensity(mContext));

        int xOffset = 0;

        if (conf.mLeft) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.LEFT | Gravity.TOP;
            params.x = xOffset;
            params.y = y;
            params.width = width;
            params.height = realHeight;
            return params;
        } else {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.RIGHT | Gravity.TOP;
            params.x = xOffset;
            params.y = y;
            params.width = width;
            params.height = realHeight;
            return params;
        }
    }

}
