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

package com.appsimobile.appsii.module;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.appsimobile.appsii.BitmapUtils;
import com.appsimobile.appsii.R;

import java.io.File;

/**
 * Created by nick on 01/11/14.
 */
public class ParallaxListViewHeader extends View {

    /**
     * The default drawable id to use for the parallax view.
     */
    private int mParallaxDrawableResId;

    /**
     * The name of the file the user can put on the device in a special folder,
     * that will be used as a parallax drawable if present.
     */
    private String mCustomDrawableFileName;

    /**
     * The loaded bitmap
     */
    private Bitmap mBitmap;

    public ParallaxListViewHeader(Context context) {
        super(context);
    }

    public ParallaxListViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ParallaxListViewHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ParallaxListViewHeader(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.
                obtainStyledAttributes(attrs, R.styleable.ParallaxListViewHeader, defStyle, 0);

        mParallaxDrawableResId =
                a.getResourceId(R.styleable.ParallaxListViewHeader_parallaxDrawable, 0);
        mCustomDrawableFileName =
                a.getString(R.styleable.ParallaxListViewHeader_customDrawableFileName);

        a.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateDrawable(w, h);
    }

    private void updateDrawable(int w, int h) {
        mBitmap = null;
        File userFile = BitmapUtils.userImageFile(mCustomDrawableFileName);
        if (userFile.exists()) {
            Log.i("ParallaxListViewHeader", "custom header file exists at: " +
                    userFile.getAbsolutePath());

            mBitmap = BitmapUtils.decodeSampledBitmapFromFile(userFile, w, h);
        }
        // fall back to the default if it was not loaded or does not exist
        if (mBitmap == null) {
            mBitmap = BitmapUtils.decodeSampledBitmapFromResource(
                    getResources(), mParallaxDrawableResId, w, h);
        }
    }

    @Override
    public void offsetTopAndBottom(int offset) {
        super.offsetTopAndBottom(offset);
        invalidate();
    }

    final Rect mSrcRect = new Rect();

    final Rect mDstRect = new Rect();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int top = getTop();
        canvas.translate(0, top / -2);
        mSrcRect.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        mDstRect.set(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(mBitmap, mSrcRect, mDstRect, null);
        canvas.translate(0, top / 2);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
//        int height = (width * 2) / 3;

        Resources res = getResources();
        boolean landscape =
                res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        int height;
        if (landscape) {
            height = res.getDimensionPixelSize(R.dimen.action_bar_size_x2);
        } else {
            height = res.getDimensionPixelSize(R.dimen.action_bar_size_x4);
        }
        setMeasuredDimension(width, height);
    }

    void trimMemory() {
        mBitmap = null;
    }
}
