/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.view;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ListView;

import com.appsimobile.appsii.BitmapUtils;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;

import javax.inject.Inject;

/**
 * Created by nick on 21/09/14.
 */
public class ParallaxListView extends ListView {

    @DrawableRes
    private final int mParallaxDrawableResId;
    @Inject
    BitmapUtils mBitmapUtils;
    private Bitmap mParallaxBitmap;

    public ParallaxListView(Context context) {
        super(context);
        mParallaxDrawableResId = 0;
    }

    public ParallaxListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public ParallaxListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();

        TypedArray a = context.
                obtainStyledAttributes(attrs, R.styleable.ParallaxListViewHeader, defStyle, 0);

        mParallaxDrawableResId =
                a.getResourceId(R.styleable.ParallaxListViewHeader_parallaxDrawable, 0);

        a.recycle();

    }

    private void init() {
        AppInjector.inject(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float w3 = w / 3f;
        w3 *= 2;
        setPadding(getPaddingLeft(), (int) w3, getPaddingRight(), getPaddingBottom());

        mParallaxBitmap = mBitmapUtils.decodeSampledBitmapFromResource(mParallaxDrawableResId,
                w,
                (int) w3);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        ParallaxHelper.draw(canvas, mParallaxBitmap, this);
    }

    public void onTrimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            mParallaxBitmap = null;
        }
    }
}
