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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

/**
 * Created by nick on 08/06/14.
 */
public class RoundedImageDrawable extends Drawable {

    private static final Paint sPaint = new Paint();

    private static final Rect sBitmapRect = new Rect();

    private static final Matrix sMatrix = new Matrix();

    private final Matrix mLocalMatrix = new Matrix();

    private final Bitmap mBitmap;

    private final Shader mShader;

    private final Resources mResources;

    private SparseArray<RoundedImageDrawable> mEffectColors = new SparseArray<>();

    public RoundedImageDrawable(Resources resources, Bitmap bitmap) {
        mResources = resources;
        mBitmap = bitmap;
        mShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }

    private RoundedImageDrawable(RoundedImageDrawable src) {
        mResources = src.mResources;
        mBitmap = src.mBitmap;
        mShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(Canvas canvas) {


        Rect targetRect = getBounds();
        sPaint.setShader(null);

        sBitmapRect.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());


        sPaint.setStyle(Paint.Style.FILL);
        sPaint.setShader(null);

        sPaint.setAntiAlias(true);
        sPaint.setFilterBitmap(true);
        sPaint.setDither(true);

        int maxDimen = Math.min(targetRect.width(), targetRect.height()) / 2;

        // draw rounded image
        sPaint.setColor(Color.WHITE);
        sPaint.setShader(mShader);
        int offsetX = (mBitmap.getWidth() - targetRect.width()) / 2;
        int offsetY = (mBitmap.getHeight() - targetRect.height()) / 2;
        mShader.getLocalMatrix(sMatrix);
        sMatrix.reset();
        sMatrix.setTranslate(-offsetX, -offsetY);
        mLocalMatrix.set(sMatrix);
        mShader.setLocalMatrix(mLocalMatrix);

        // calculate the center, we want the shadow on the bottom only
        int imageCenterX = targetRect.centerX();
        int imageCenterY = targetRect.centerY();

        canvas.drawCircle(imageCenterX, imageCenterY,
                maxDimen, sPaint);

        sPaint.setShader(null);
        sPaint.setStyle(Paint.Style.STROKE);
        sPaint.setColor(Color.LTGRAY);
        float strokeWidth = 2 * mResources.getDisplayMetrics().density;
        sPaint.setStrokeWidth(strokeWidth);
//        canvas.drawCircle(imageCenterX, imageCenterY, maxDimen - strokeWidth / 2, sPaint);

    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmap.getHeight();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

}
