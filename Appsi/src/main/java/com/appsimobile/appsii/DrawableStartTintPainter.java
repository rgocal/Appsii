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

package com.appsimobile.appsii;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.TextView;

import com.appsimobile.paintjob.PaintJob;

/**
 * Created by nick on 27/04/15.
 */
public class DrawableStartTintPainter extends PaintJob.BaseViewPainter {

    public static DrawableStartTintPainter withAlpha(int alpha, int... viewIds) {
        return new DrawableStartTintPainter(alpha, viewIds);
    }

    public static DrawableStartTintPainter forIds(int... viewIds) {
        return new DrawableStartTintPainter(viewIds);

    }

    private DrawableStartTintPainter(int[] viewIds) {
        this(0xFF, viewIds);
    }

    private DrawableStartTintPainter(int alpha, int[] viewIds) {
        super(alpha, viewIds);
    }

    @Override
    protected int getCurrentColorFromView(View view) {
        return Color.BLACK;
    }

    @Override
    protected int getTargetColorFromSwatch(Palette.Swatch swatch) {
        return swatch.getBodyTextColor();
    }

    @Override
    protected void applyColorToView(View view, int color) {
        Drawable drawable = ((TextView) view).getCompoundDrawablesRelative()[0];
        DrawableCompat.setTintColorCompat(drawable, color);
    }
}
