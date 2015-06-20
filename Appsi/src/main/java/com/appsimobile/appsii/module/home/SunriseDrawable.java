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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.text.TextUtils;
import android.view.View;

import com.appsimobile.appsii.DrawableCompat;
import com.appsimobile.appsii.R;

import java.util.Locale;

/**
 * Created by nick on 22/01/15.
 */
public class SunriseDrawable extends Drawable {

    final Paint mArcPaint;

    final Paint mArcFillPaint;

    final Paint mLinePaint;

    final Paint mDotPaint;

    final int mTopOffset;

    final int mLeftOffset;

    final int mRightOffset;

    /**
     * Offset from the bottom
     */
    final int mBottomOffset;

    /**
     * The arc that displays the path of the sun
     */
    final Path mArc = new Path();

    /**
     * The path used to fill the arc with
     */
    final Path mArcFill = new Path();

    /**
     * The rect used to clip the mArcFill path with to make it fill until now
     */
    final Rect mClipRect = new Rect();

    /**
     * True when this drawable is in ltr mode. False otherwise
     */
    final boolean mIsRtl;

    /**
     * The image of the sun
     */
    Drawable mSunImage;

    /**
     * The sunrise time in minutes
     */
    int mRiseMinutes;

    /**
     * The sunset time in minutes
     */
    int mSetMinutes;

    /**
     * The current time in minutes
     */
    int mNowMinutes;

    /**
     * The size of the dot at the start and end in px
     */
    final int mDotRadius;


    int mBezier1x;

    int mBezier1y;

    int mBezier2x;

    int mBezier2y;

    int mBezier3x;

    int mBezier3y;

    int mBezier4x;

    int mBezier4y;

    final Context mContext;

    public SunriseDrawable(Context context) {

        mContext = context;

        Resources res = context.getResources();
        final TypedArray a = context.obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, R.attr.colorAccent,
                        R.attr.colorPrimaryDark,
                        R.attr.appsiHomeWidgetPrimaryColor,
                });

        int primaryColor = a.getColor(0, Color.BLACK);
        int accentColor = a.getColor(1, Color.BLACK);
        int primaryColorDark = a.getColor(2, Color.BLACK);
        int textColor = a.getColor(3, Color.BLACK);
        a.recycle();

        mIsRtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                View.LAYOUT_DIRECTION_RTL;

        float density = res.getDisplayMetrics().density;
        int sunBounds = (int) (density * 16);
        mTopOffset = (int) (density * 12);
        mLeftOffset = (int) (density * 24);
        mRightOffset = (int) (density * 24);
        mBottomOffset = (int) (density * 24);
        mDotRadius = (int) (density * 2);

        mSunImage = mContext.getResources().getDrawable(R.drawable.ic_weather_clear);
        mSunImage.setBounds(0, 0, sunBounds, sunBounds);

        mArcPaint = new Paint();
        mArcPaint.setColor(primaryColorDark);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setAntiAlias(true);

        mArcFillPaint = new Paint();
        mArcFillPaint.setColor(primaryColor);
        mArcFillPaint.setAlpha(64);
        mArcFillPaint.setStyle(Paint.Style.FILL);
        mArcFillPaint.setAntiAlias(true);

        mLinePaint = new Paint();
        mLinePaint.setColor(textColor);
        mLinePaint.setAlpha(128);
        mLinePaint.setStyle(Paint.Style.STROKE);

        mDotPaint = new Paint();
        mDotPaint.setStyle(Paint.Style.FILL);
        mDotPaint.setColor(primaryColorDark);
        mDotPaint.setAntiAlias(true);
    }

    void customTheme(int drawableResId, int colorPrimary, int textColor, int tintColor) {
        Resources res = mContext.getResources();
        mSunImage = res.getDrawable(drawableResId);
        DrawableCompat.setTintColorCompat(mSunImage, tintColor);
        mArcFillPaint.setColor(colorPrimary);
        mArcFillPaint.setColor(ColorUtils.setAlphaComponent(colorPrimary, 128));
        mLinePaint.setColor(textColor);
        mDotPaint.setColor(tintColor);

        float density = res.getDisplayMetrics().density;
        int sunBounds = (int) (density * 16);
        mSunImage.setBounds(0, 0, sunBounds, sunBounds);


        Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    public void setTime(int riseMinutes, int setMinutes, int nowMinutes) {
        mRiseMinutes = riseMinutes;
        mSetMinutes = setMinutes;
        mNowMinutes = nowMinutes;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (mIsRtl) {
            canvas.save();
            canvas.scale(-1, 1, bounds.centerX(), bounds.centerY());
        }
        float pct = drawArc(canvas, bounds);

        if (pct < 0) {
            pct = 0;
        } else if (pct > 1) {
            pct = 1;
        }

        int y = bounds.bottom - mBottomOffset;
        canvas.drawLine(bounds.left + mLeftOffset / 2, y, bounds.right - mRightOffset / 2, y,
                mLinePaint);

        drawSun(canvas, pct);

        if (mIsRtl) {
            canvas.restore();
        }

    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        updateArcs(left, top, right, bottom);
    }

    private void updateArcs(int left, int top, int right, int bottom) {
        mBezier1x = left + mLeftOffset;
        mBezier1y = bottom - mBottomOffset;
        mBezier2y = 0; //top + mTopOffset;
        mBezier2x = (left + mLeftOffset + right - mRightOffset) / 3;
        mBezier3x = mBezier2x * 2;
        mBezier3y = mBezier2y;
        mBezier4x = right - mRightOffset;
        mBezier4y = mBezier1y;//bottom - mBottomOffset;

        mArc.reset();
        mArc.moveTo(mBezier1x, mBezier1y);
        mArc.cubicTo(mBezier2x, mBezier2y, mBezier3x, mBezier3y, mBezier4x, mBezier4y);

        mArcFill.reset();
        mArcFill.moveTo(mBezier1x, mBezier1y);
        mArcFill.cubicTo(mBezier2x, mBezier2y, mBezier3x, mBezier3y, mBezier4x, mBezier4y);
        mArcFill.close();

    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public boolean isAutoMirrored() {
        return true;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return (mLeftOffset + mRightOffset) * 2;
    }

    @Override
    public int getIntrinsicHeight() {
        return (mTopOffset + mBottomOffset) * 2;
    }

    private float drawArc(Canvas canvas, Rect bounds) {
        canvas.drawPath(mArc, mArcPaint);

        int minuteSinceStart = mNowMinutes - mRiseMinutes;
        int totalMinutes = mSetMinutes - mRiseMinutes;
        float pct = (minuteSinceStart / (float) totalMinutes);

        int r = bounds.right - mRightOffset;
        int l = bounds.left + mLeftOffset;

        int px = (int) (pct * (r - l));

        mClipRect.set(0, 0, l + px, bounds.bottom);
        canvas.save(Canvas.CLIP_SAVE_FLAG);
        canvas.clipRect(mClipRect);
        canvas.drawPath(mArcFill, mArcFillPaint);
        canvas.restore();

        return pct;
    }

    private void drawSun(Canvas canvas, float pct) {

        // Draw the dots
        canvas.drawCircle(mBezier1x, mBezier1y, mDotRadius, mDotPaint);
        canvas.drawCircle(mBezier4x, mBezier4y, mDotRadius, mDotPaint);

        // calculate the center point of the sun image
        float x = bezierInterpolation(pct, mBezier1x, mBezier2x, mBezier3x, mBezier4x);
        float y = bezierInterpolation(pct, mBezier1y, mBezier2y, mBezier3y, mBezier4y);

        Rect sunBounds = mSunImage.getBounds();

        x -= sunBounds.width() / 2;
        y -= sunBounds.height() / 2;

        // if we are in rtl, we still want to show the image normally
        // so flip it back
        canvas.translate(x, y);
        if (mIsRtl) {
            canvas.save();
            canvas.scale(-1, 1, sunBounds.centerX(), sunBounds.centerY());
        }
        // draw the sun image
        mSunImage.draw(canvas);
        // restore the flip again
        if (mIsRtl) {
            canvas.restore();
        }
        canvas.translate(-x, -y);

    }

    float bezierInterpolation(float t, float a, float b, float c, float d) {
        float t2 = t * t;
        float t3 = t2 * t;
        return a + (-a * 3 + t * (3 * a - a * t)) * t
                + (3 * b + t * (-6 * b + b * 3 * t)) * t
                + (c * 3 - c * 3 * t) * t2
                + d * t3;
    }
}
