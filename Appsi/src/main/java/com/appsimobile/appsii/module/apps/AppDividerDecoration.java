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
package com.appsimobile.appsii.module.apps;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class AppDividerDecoration extends RecyclerView.ItemDecoration {

    final AppsAdapter mAppsAdapter;

    private Drawable mDivider = new ColorDrawable(0x33000000);

    private final Paint mPaint;

    private int mOrientation;

    public AppDividerDecoration(AppsAdapter appsAdapter) {
        mPaint = new Paint();
        mPaint.setColor(0x33000000);
        mAppsAdapter = appsAdapter;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int parentLeft = parent.getPaddingLeft();
        final int parentRight = parent.getWidth() - parent.getPaddingRight();
        final int childCount = parent.getChildCount();
        boolean skipped = true;
        int dontSkipForY = -1;


        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            int position = parent.getChildLayoutPosition(child);
            if (position == -1) continue;
            final int viewType = mAppsAdapter.getItemViewType(position);

            if (viewType == AppsAdapter.VIEW_TYPE_APP) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();

                float alpha = child.getAlpha();
                int borderAlpha = (int) (alpha * 0x33);
                mPaint.setColor(borderAlpha << 24);

                final int left = child.getLeft();
                final int top = child.getTop();
                final int right = child.getRight();
                final int bottom = child.getBottom();

                float tx = child.getTranslationX();
                float ty = child.getTranslationY();

                c.translate(tx, ty);

                if (skipped || top == dontSkipForY) {
                    skipped = false;
                    c.drawLine(left, top, right, top, mPaint);
                    dontSkipForY = top;
                }

                if (right < parentRight - 1) {
                    c.drawLine(right, top, right, bottom, mPaint);
                }
                c.drawLine(left, bottom, right, bottom, mPaint);

                c.translate(-tx, -ty);

            } else {
                skipped = true;
            }
        }
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
    }

}