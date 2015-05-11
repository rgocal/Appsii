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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.appsimobile.appsii.R;

import static com.appsimobile.appsii.module.home.provider.HomeContract.Cells;

/**
 * Created by nick on 24/01/15.
 */
class GridLayoutDecoration extends RecyclerView.ItemDecoration {

    final Paint mPaint;

    final Rect mRect = new Rect();

    final RectF mRectf = new RectF();

    final int mCornerRadius;

    final int mDefaultColor;

    GridLayoutDecoration(Context context) {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mCornerRadius = (int) (context.getResources().getDisplayMetrics().density * 2);
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.appsiTextShadow});
        int color = a.getColor(0, Color.BLACK);
        mDefaultColor = color;
        a.recycle();
        mPaint.setColor(color);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        int count = parent.getChildCount();
        HomeAdapter adapter = (HomeAdapter) parent.getAdapter();
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildLayoutPosition(child);
            if (position == -1) continue;

            int itemCount = adapter.getItemCount();
            if (position < itemCount) {


                HomeItem homeItem = adapter.getItemAt(position);
                int type = homeItem.mDisplayType;
                int color = homeItem.mEffectColor;
                if (!adapter.mAppWidgetsEnabled || type != Cells.DISPLAY_TYPE_APP_WIDGET) {
                    mRect.set(0, 0, child.getWidth(), child.getHeight());
                    parent.offsetDescendantRectToMyCoords(child, mRect);
                    mRectf.set(mRect);

                    if (color == Color.TRANSPARENT) {
                        mPaint.setColor(mDefaultColor);
                    } else {
                        mPaint.setColor(color);
                    }
//                    mPaint.setAlpha(128);
                    c.drawRoundRect(mRectf, mCornerRadius, mCornerRadius, mPaint);
                }
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        outRect.set(mCornerRadius, mCornerRadius, mCornerRadius, mCornerRadius);
    }
}
