/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.widget.AbsListView;

/**
 * Created by nick on 21/09/14.
 */
public class ParallaxHelper {

    private static Paint sPaint;

    private static Rect sSrcRect = new Rect();

    private static Rect sTargetRect = new Rect();

    public static void draw(Canvas canvas, Bitmap parallaxBitmap, AbsListView listView) {
        if (sPaint == null) {
            sPaint = new Paint();
            sPaint.setColor(Color.BLACK);
        }

        int padding = listView.getPaddingTop();
        float pct = getVisibilityPercentage(listView);
        if (pct == -1) return;

        int offset = (int) (padding * (1 - pct));
        offset = offset / 2;

        canvas.save(Canvas.CLIP_SAVE_FLAG | Canvas.MATRIX_SAVE_FLAG);

        canvas.clipRect(0, 0, listView.getWidth(), padding - offset * 2);
        canvas.translate(0, -offset);

        sPaint.setFilterBitmap(true);
        sSrcRect.set(0, 0, parallaxBitmap.getWidth(), parallaxBitmap.getHeight());
        sTargetRect.set(0, 0, listView.getWidth(), padding);
//        canvas.drawRect(sTargetRect, sPaint);
        canvas.drawBitmap(parallaxBitmap, sSrcRect, sTargetRect, sPaint);

    }

    public static float getVisibilityPercentage(AbsListView listView) {
        int firstVisible = listView.getFirstVisiblePosition();
        if (firstVisible > 0) return -1;
        int childCount = listView.getChildCount();
        if (childCount == 0) return -1;

        View firstChild = listView.getChildAt(0);

        int topOffset = firstChild.getTop();
        int padding = listView.getPaddingTop();

        return topOffset / (float) padding;

    }

}
