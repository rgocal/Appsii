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

package com.appsimobile.paintjob;

/**
 * Created by nick on 27/04/15.
 */
public class ViewPainters {

    public static PaintJob.ViewPainter title(int... viewIds) {
        return new PaintJob.TitleViewPainter(0xFF, viewIds);
    }

    public static PaintJob.ViewPainter text(int... viewIds) {
        return new PaintJob.BodyTextViewPainter(0xFF, viewIds);
    }

    public static PaintJob.ViewPainter rgb(int... viewIds) {
        return new PaintJob.RgbViewPainter(0xFF, viewIds);
    }

    public static PaintJob.ViewPainter atitle(int alpha, int... viewIds) {
        return new PaintJob.TitleViewPainter(alpha, viewIds);
    }

    public static PaintJob.ViewPainter atext(int alpha, int... viewIds) {
        return new PaintJob.BodyTextViewPainter(alpha, viewIds);
    }

    public static PaintJob.ViewPainter argb(int alpha, int... viewIds) {
        return new PaintJob.RgbViewPainter(alpha, viewIds);
    }
}
