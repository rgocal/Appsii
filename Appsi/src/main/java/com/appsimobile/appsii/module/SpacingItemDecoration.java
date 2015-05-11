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

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class SpacingItemDecoration extends RecyclerView.ItemDecoration {

    int mSpacingLeft;

    int mSpacingTop;

    int mSpacingRight;

    int mSpacingBottom;

    public SpacingItemDecoration(Context context, int spacingDps) {
        mSpacingLeft = mSpacingTop = mSpacingRight = mSpacingBottom =
                (int) (spacingDps * context.getResources().getDisplayMetrics().density);
    }

    public SpacingItemDecoration(Context context, int left, int top, int right, int bottom) {
        float density = context.getResources().getDisplayMetrics().density;
        mSpacingLeft = (int) (left * density);
        mSpacingTop = (int) (top * density);
        mSpacingRight = (int) (right * density);
        mSpacingBottom = (int) (bottom * density);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {

        outRect.set(mSpacingLeft, mSpacingTop, mSpacingRight, mSpacingBottom);

    }

}