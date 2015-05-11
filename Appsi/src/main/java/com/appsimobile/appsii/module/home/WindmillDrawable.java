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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import com.appsimobile.appsii.DrawableCompat;
import com.appsimobile.appsii.R;

/**
 * Created by nick on 22/01/15.
 */
public class WindmillDrawable extends Drawable implements Animator.AnimatorListener {

    Drawable mFanDrawable;

    Drawable mBodyDrawable;

    int mTopOffset;

    int mRotation;

    ObjectAnimator mObjectAnimator;

    boolean mStarted;

    public WindmillDrawable(Context context) {
        Resources res = context.getResources();
        final TypedArray a = context.obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, R.attr.colorAccent,
                        android.R.attr.textColorPrimaryNoDisable,
                        R.attr.appsiHomeWidgetPrimaryColor,
                });

        int textColor = a.getColor(3, Color.BLACK);
        a.recycle();

        mFanDrawable = res.getDrawable(R.drawable.ic_windmill_fan);
        mBodyDrawable = res.getDrawable(R.drawable.ic_windmill_body);

        DrawableCompat.setTintColorCompat(mFanDrawable, textColor);
        DrawableCompat.setTintColorCompat(mBodyDrawable, textColor);

        mFanDrawable.setBounds(0, 0, mFanDrawable.getIntrinsicWidth(),
                mFanDrawable.getIntrinsicHeight());
        mBodyDrawable.setBounds(0, 0, mBodyDrawable.getIntrinsicWidth(),
                mBodyDrawable.getIntrinsicHeight());
        mTopOffset = (int) (res.getDisplayMetrics().density * -1);
        setRotation(0);


        mObjectAnimator = createObjectAnimator();
    }

    public void setRotation(int i) {
        if (mStarted) {
            mRotation = i;
            invalidateSelf();
        }
    }

    private ObjectAnimator createObjectAnimator() {
        ObjectAnimator result = ObjectAnimator.ofInt(this, "rotation", 0, 120);
        result.setInterpolator(new LinearInterpolator());
        result.setRepeatCount(ObjectAnimator.INFINITE);
        result.setRepeatMode(ObjectAnimator.RESTART);
        result.addListener(this);
        return result;
    }

    public void setWindSpeedKmh(float speed) {
        int duration;
        if (speed < 1) {
            duration = 7200;
        } else if (speed < 3) {
            duration = 1100;
        } else if (speed < 5) {
            duration = 1000;
        } else if (speed < 8) {
            duration = 900;
        } else if (speed < 10) {
            duration = 800;
        } else if (speed < 15) {
            duration = 700;
        } else if (speed < 20) {
            duration = 600;
        } else if (speed < 25) {
            duration = 550;
        } else if (speed < 30) {
            duration = 500;
        } else if (speed < 35) {
            duration = 450;
        } else if (speed < 40) {
            duration = 400;
        } else if (speed < 45) {
            duration = 350;
        } else if (speed < 50) {
            duration = 300;
        } else {
            duration = 250;
        }
        mObjectAnimator.setDuration(duration);
    }

    public void start() {
        if (!mStarted) {
            mStarted = true;
            mObjectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            mObjectAnimator.start();
        }
    }

    public void stop() {
        if (mStarted) {
            mStarted = false;
            mObjectAnimator.setRepeatCount(1);
            mObjectAnimator.cancel();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        int w = bounds.width();
        int h = bounds.height();

        int bodyLeft = (w - mBodyDrawable.getIntrinsicWidth()) / 2;
        int bodyTop = h - mBodyDrawable.getIntrinsicHeight();

        canvas.translate(bodyLeft, bodyTop);
        mBodyDrawable.draw(canvas);
        canvas.translate(-bodyLeft, -bodyTop);

        canvas.save();
        canvas.translate(w / 2, bodyTop + mTopOffset);
        canvas.rotate(mRotation, 0, 0);
        canvas.translate(-mFanDrawable.getIntrinsicWidth() / 2,
                -mFanDrawable.getIntrinsicHeight() / 2);

        mFanDrawable.draw(canvas);
        canvas.restore();

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
        return (int) Math.max(mFanDrawable.getIntrinsicWidth() * 1.0f,
                mBodyDrawable.getIntrinsicWidth() * 1.0f);
    }

    @Override
    public int getIntrinsicHeight() {
        int fanHeight = mFanDrawable.getIntrinsicHeight();
        int bodyHeight = mBodyDrawable.getIntrinsicHeight();
        return bodyHeight + (fanHeight / 2) - mTopOffset;
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (mStarted) {
            mObjectAnimator = createObjectAnimator();
            mObjectAnimator.start();
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }
}
