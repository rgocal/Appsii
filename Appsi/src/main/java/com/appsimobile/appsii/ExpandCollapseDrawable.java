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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.appsimobile.annotation.KeepName;

/**
 * Created by nick on 22/08/14.
 */
public class ExpandCollapseDrawable extends Drawable {

    private static Paint sPaint;

    final int mColor;

    int mAlpha;

    final float[] mPositionsX = new float[]{.25f, .5f, .75f};

    final float[] mExpandedYPositions = new float[]{.35f, .6f, .35f};

    final float[] mCollapsedYPositions = new float[]{.65f, .4f, .65f};

    // we need a new array, don't reference mCollapsedYPositions
    final float[] mCurrentYPositions = new float[]{.6f, .4f, .6f};

    private ColorFilter mColorFilter;

    private boolean mExpanded;

    private final ObjectAnimator mY1Animator;

    private final ObjectAnimator mY2Animator;

    private final ObjectAnimator mY3Animator;

    private final Path mPath = new Path();

    public ExpandCollapseDrawable(Resources resources) {
        this(resources, 0x3d000000);
    }

    public ExpandCollapseDrawable(Resources resources, int color) {
        if (sPaint == null) {
            sPaint = new Paint();
            sPaint.setStrokeWidth(2.5f * resources.getDisplayMetrics().density);
            sPaint.setStyle(Paint.Style.STROKE);
            sPaint.setAntiAlias(true);
            sPaint.setStrokeCap(Paint.Cap.SQUARE);
        }
        mY1Animator = ObjectAnimator.ofFloat(this, "y1", 0, 0).setDuration(220);
        mY2Animator = ObjectAnimator.ofFloat(this, "y2", 0, 0).setDuration(220);
        mY3Animator = ObjectAnimator.ofFloat(this, "y3", 0, 0).setDuration(220);
        mY1Animator.setFloatValues();
        mColor = color;
    }

    /**
     * Setter for the mY1Animator. Used by the animator.
     */
    @SuppressWarnings("UnusedDeclaration")
    @KeepName
    public void setY1(float y1) {
        mCurrentYPositions[0] = y1;
        notifyCallback();
    }

    private void notifyCallback() {
        Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    /**
     * Setter for the value of {@link #mY2Animator}. Used by the animator
     */
    @SuppressWarnings("UnusedDeclaration")
    @KeepName
    public void setY2(float y2) {
        mCurrentYPositions[1] = y2;
        notifyCallback();
    }

    /**
     * Setter for the value of {@link #mY3Animator}. Used by the animator
     */
    @SuppressWarnings("UnusedDeclaration")
    @KeepName
    public void setY3(float y3) {
        mCurrentYPositions[2] = y3;
        notifyCallback();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect rect = getBounds();

        sPaint.setColor(mColor);
        sPaint.setColorFilter(mColorFilter);

        int x1 = (int) (rect.width() * mPositionsX[0]);
        int x2 = (int) (rect.width() * mPositionsX[1]);
        int x3 = (int) (rect.width() * mPositionsX[2]);

        int y1 = (int) (rect.height() * mCurrentYPositions[0]);
        int y2 = (int) (rect.height() * mCurrentYPositions[1]);
        int y3 = (int) (rect.height() * mCurrentYPositions[2]);

        mPath.reset();
        mPath.moveTo(x1, y1);
        mPath.lineTo(x2, y2);
        mPath.lineTo(x3, y3);

        canvas.drawPath(mPath, sPaint);
//        canvas.drawLine(x1, y1, x2, y2, sPaint);
//        canvas.drawLine(x3, y3, x2, y2, sPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        notifyCallback();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mColorFilter = cf;
        notifyCallback();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = super.onStateChange(state);

        boolean foundExpanded = false;
        int count = state.length;
        for (int i = 0; i < count; i++) {
            if (state[i] == android.R.attr.state_expanded) {
                foundExpanded = true;
                break;
            }
        }
        if (mExpanded != foundExpanded) {
            mExpanded = foundExpanded;

            startAnimation();
            changed = true;
        }
        return changed;
    }

    private void startAnimation() {
        float[] targetPositions = mExpanded ? mExpandedYPositions : mCollapsedYPositions;

        mY1Animator.setFloatValues(mCurrentYPositions[0], targetPositions[0]);
        mY2Animator.setFloatValues(mCurrentYPositions[1], targetPositions[1]);
        mY3Animator.setFloatValues(mCurrentYPositions[2], targetPositions[2]);

        mY2Animator.setStartDelay(100);
        mY3Animator.setStartDelay(150);

        mY1Animator.start();
        mY2Animator.start();
        mY3Animator.start();

    }

    public void setExpanded(boolean expanded, boolean animate) {
        if (mExpanded != expanded) {
            mExpanded = expanded;
            if (animate) {
                startAnimation();
            } else {
                if (expanded) {
                    System.arraycopy(mExpandedYPositions, 0, mCurrentYPositions, 0,
                            mCurrentYPositions.length);
                } else {
                    System.arraycopy(mCollapsedYPositions, 0, mCurrentYPositions, 0,
                            mCurrentYPositions.length);
                }
            }
            notifyCallback();
        }
    }

    public boolean isExpanded() {
        return mExpanded;
    }
}
