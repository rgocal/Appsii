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

package com.appsimobile.appsii.module.appsiagenda;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by nick on 08/01/15.
 */
public class AgendaViewLayout extends FrameLayout {

    final RectF mDrawRect = new RectF();

    private final Paint mPaint = new Paint();

    private int mColor = Color.TRANSPARENT;

    private float mRadius;

    public AgendaViewLayout(Context context) {
        super(context);
        init();
    }

    public AgendaViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AgendaViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        mPaint.setAntiAlias(true);
        mRadius = (int) (getResources().getDisplayMetrics().density * 2);
    }

    public void setColor(int color) {
        mColor = color;
        int r = (int) (Color.red(color) * .85f);
        int g = (int) (Color.green(color) * .85f);
        int b = (int) (Color.blue(color) * .85f);
        mPaint.setColor(Color.rgb(r, g, b));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mDrawRect.set(getPaddingLeft(), getPaddingTop(),
                w - getPaddingRight(), h - getPaddingBottom());
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (mColor != Color.TRANSPARENT) {
            canvas.drawRoundRect(mDrawRect,
                    mRadius,
                    mRadius,
                    mPaint);
        }
        super.dispatchDraw(canvas);
    }
}
