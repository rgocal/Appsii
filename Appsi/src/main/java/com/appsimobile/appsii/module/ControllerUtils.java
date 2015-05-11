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

import android.support.v7.widget.Toolbar;

import com.appsimobile.appsii.PageController;

/**
 * Created by nick on 01/10/14.
 */
public class ControllerUtils {

    public static void handleScrollChange(
            PageController pageController, float pct, int firstVisibleItem) {

        if (firstVisibleItem != 0) {
            return;
        }

        if (pct == -1) {
            pageController.setToolbarBackgroundAlpha(1);
        } else {
            pageController.setToolbarBackgroundAlpha(pct);
        }

    }

    public static void showToolbar(Toolbar toolbar) {
        toolbar.animate().translationY(0);
    }

    public static void hideToolbar(Toolbar toolbar) {
        toolbar.animate().translationY(-toolbar.getHeight());
    }


}
