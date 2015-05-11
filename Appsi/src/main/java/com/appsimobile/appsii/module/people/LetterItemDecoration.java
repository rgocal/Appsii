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
package com.appsimobile.appsii.module.people;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.appsimobile.appsii.R;

public class LetterItemDecoration extends RecyclerView.ItemDecoration {

    private static final int[] ATTRS = new int[]{
            R.attr.colorAccent
    };

    final Paint mPaint;

    int mColor;

    float m72Dps;

    float m12Dps;

    float m24dps;

    private int mOffset;

    public LetterItemDecoration(Context context) {
        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        mColor = a.getColor(0, Color.BLACK);
        a.recycle();
        m72Dps = context.getResources().getDisplayMetrics().density * 72;
        m12Dps = context.getResources().getDisplayMetrics().density * 12;
        m24dps = context.getResources().getDisplayMetrics().density * 24;
        int _24sp = (int) (context.getResources().getDisplayMetrics().scaledDensity * 24);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(_24sp);
        mPaint.setColor(mColor);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int childCount = parent.getChildCount();

        boolean nextIsOther;

        float ascent = -mPaint.getFontMetrics().ascent + m24dps;


        int mLastTop = Integer.MAX_VALUE;

        float minY;
        float maxY;

        String lastDrawnLetter = null;

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();

            if (child instanceof LetterDecoratable) {

                LetterDecoratable v = (LetterDecoratable) child;

                String letter = v.getLetter();

                if (TextUtils.equals(lastDrawnLetter, letter)) continue;

                if (letter == null) continue;

                lastDrawnLetter = letter;

                if (i < childCount) {
                    View nextChild = parent.getChildAt(i + 1);
                    if (nextChild instanceof LetterDecoratable) {
                        LetterDecoratable next = (LetterDecoratable) nextChild;
                        nextIsOther = next.isFirstLetterOfKind();
                    } else {
                        nextIsOther = false;
                    }
                } else {
                    nextIsOther = false;
                }

                int childTop = child.getTop();
                float top = (childTop + ascent);

                float y;
                boolean shouldDraw = nextIsOther || i == 0 || v.isFirstLetterOfKind();

                if (nextIsOther) {
                    maxY = child.getBottom();
                } else {
                    maxY = Integer.MAX_VALUE;
                }

                if (!shouldDraw) continue;


                if (nextIsOther || v.isFirstLetterOfKind()) {
                    y = top;
                } else {
                    y = 0;
                }

                if (y < mOffset) {
                    y = mOffset;
                }
                if (y < ascent + m24dps) {
                    y = ascent + m24dps;
                }
                if (y > maxY) {
                    y = maxY;
                }

                c.drawText(letter, m12Dps, y, mPaint);
            }
        }
    }
//    ascent = -66.796875
//    bottom = 19.511719
//    descent = 17.578125
//    leading = 0.0
//    top = -76.04297

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
    }

    public void setOffset(int offset) {
        mOffset = offset;
    }
}