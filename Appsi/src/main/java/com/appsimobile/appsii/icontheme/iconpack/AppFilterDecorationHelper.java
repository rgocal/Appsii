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

package com.appsimobile.appsii.icontheme.iconpack;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.util.SparseArray;

import com.appsimobile.appsii.AppsiiUtils;

import java.util.Random;

/**
 * Created by Nick Martens on 9/25/13.
 */
public class AppFilterDecorationHelper {

    private static final Rect mBoundsRect = new Rect();

    final PorterDuffXfermode mDstOut = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

    final PorterDuffXfermode mDstOver = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);

    private final Random mRandom = new Random();

    private final SparseArray<Bitmap> mIconCache = new SparseArray<Bitmap>();

    private final AppFilterParser.AppFilterData mAppFilterData;

    private final Resources mResources;

    private final String mPackageName;

    private final Paint mPaint;

    private AppFilterDecorationHelper(String packageName, Resources resources,
            AppFilterParser.AppFilterData appFilterData) {
        mResources = resources;
        mAppFilterData = appFilterData;
        mPackageName = packageName;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
    }

    public static AppFilterDecorationHelper getInstance(String packageName, Resources resources,
            AppFilterParser.AppFilterData appFilterData) {
        return new AppFilterDecorationHelper(packageName, resources, appFilterData);
    }

    public Bitmap applyDecorations(Bitmap original, Uri uri) {
        if (original == null) return null;

        if (mAppFilterData == null) {
            return original;
        }
        if (mAppFilterData.mIconBack == null && mAppFilterData.mIconMask == null &&
                mAppFilterData.mIconUpon == null) {
            return original;
        }

        if (uri != null) {
            mRandom.setSeed(uri.hashCode());
        }


        int[] load = new int[2];
        if (uri == null) {
            load[0] = original.getWidth();
            load[1] = original.getHeight();
        } else {
            AppsiiUtils.getIconDimensionsFromQuery(uri, load);
        }
        int w = load[0];
        int h = load[1];

        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        drawMaskedIcon(canvas, original, w, h);
        drawback(canvas, w, h);
        drawupon(canvas, w, h);

        return result;
    }

    private synchronized void drawMaskedIcon(Canvas canvas, Bitmap original, int w, int h) {
        Resources res = mAppFilterData.mResources;
        float scaleFactor = mAppFilterData.mScaleFactor;

        String[] iconMask = mAppFilterData.mIconMask;

        mBoundsRect.set(0, 0, w, h);
        //Bitmap target = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        //Canvas targetCanvas = new Canvas(target);

        int scaledWidth = (int) (scaleFactor * w);
        int scaledHeight = (int) (scaleFactor * h);

        int x = (w - scaledWidth) / 2;
        int y = (h - scaledHeight) / 2;
        mPaint.setXfermode(null);
        canvas.drawBitmap(original, null, new Rect(x, y, x + scaledWidth, y + scaledHeight),
                mPaint);

        if (iconMask != null) {
            int idx = mRandom.nextInt(iconMask.length);
            String maskDrawableName = iconMask[idx];
            int id = res.getIdentifier(maskDrawableName, "drawable", mPackageName);
            if (id == 0) return;
            Bitmap maskBitmap = getBitmap(id);

            if (maskBitmap != null) {
                mPaint.setXfermode(mDstOut);
                canvas.drawBitmap(maskBitmap, null, mBoundsRect, mPaint);
            }

        }

    }

    private synchronized void drawback(Canvas canvas, int w, int h) {
        mBoundsRect.set(0, 0, w, h);
        if (mAppFilterData.mIconBack == null) return;
        int idx = mRandom.nextInt(mAppFilterData.mIconBack.length);
        String backDrawableName = mAppFilterData.mIconBack[idx];
        int id = mResources.getIdentifier(backDrawableName, "drawable", mPackageName);
        Bitmap backBitmap = getBitmap(id);

        if (backBitmap == null) return;

        mPaint.setXfermode(mDstOver);
        canvas.drawBitmap(backBitmap, null, mBoundsRect, mPaint);
    }

    private synchronized void drawupon(Canvas canvas, int w, int h) {
        mBoundsRect.set(0, 0, w, h);
        if (mAppFilterData.mIconUpon == null) return;
        int idx = mRandom.nextInt(mAppFilterData.mIconUpon.length);
        String backDrawableName = mAppFilterData.mIconUpon[idx];
        int id = mResources.getIdentifier(backDrawableName, "drawable", mPackageName);
        Bitmap uponBitmap = getBitmap(id);

        if (uponBitmap == null) return;

        mPaint.setXfermode(null);
        canvas.drawBitmap(uponBitmap, null, mBoundsRect, mPaint);
    }

    private Bitmap getBitmap(int resId) {
        if (resId == 0) return null;
        Bitmap result = mIconCache.get(resId);
        if (result != null) return result;
        result = BitmapFactory.decodeResource(mResources, resId);
        mIconCache.put(resId, result);
        return result;
    }

}
