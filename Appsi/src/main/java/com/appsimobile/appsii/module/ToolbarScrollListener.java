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

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.appsimobile.appsii.PageController;

/**
 * Created by nick on 04/05/15.
 */
public class ToolbarScrollListener extends RecyclerView.OnScrollListener {

    final PageController mPageController;

    final Toolbar mToolbar;

    public ToolbarScrollListener(PageController pageController, Toolbar toolbar) {
        if (toolbar == null) throw new IllegalArgumentException("toolbar == null");
        if (pageController == null) throw new IllegalArgumentException("pageController == null");
        mPageController = pageController;
        mToolbar = toolbar;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        float ty = mToolbar.getTranslationY();
        if (dy < 0) {
            ty -= dy;
            if (ty > 0) ty = 0;
            mToolbar.setTranslationY(ty);
        } else {
            int toolbarHeight = mToolbar.getHeight();
            ty -= dy;
            if (ty < -toolbarHeight) {
                ty = -toolbarHeight;
            }
            mToolbar.setTranslationY(ty);
        }
        updateToolbarAlpha(recyclerView);

    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            float ty = Math.abs(mToolbar.getTranslationY());
            View child0 = recyclerView.getChildAt(0);
            boolean child0Visible =
                    child0 == null || recyclerView.getChildLayoutPosition(child0) == 0;

            if (!child0Visible) {
                boolean halfWay = ty >= (mToolbar.getHeight() / 2);
                if (halfWay) {
                    hideToolbar();
                } else {
                    showToolbar();
                }
            }
        }
    }

    void updateToolbarAlpha(RecyclerView recyclerView) {
        View firstChild = recyclerView.getChildAt(0);
        boolean visible =
                firstChild != null && recyclerView.getChildLayoutPosition(firstChild) == 0;

        if (visible) {
            float top = firstChild.getTop();
            if (top == 0) {
                mPageController.setToolbarBackgroundAlpha(0f);
            } else {
                float pct = getHeaderScrollPercentage(firstChild);
                mPageController.setToolbarBackgroundAlpha(pct);
            }
        } else {
            mPageController.setToolbarBackgroundAlpha(1f);
        }

    }

    private float getHeaderScrollPercentage(View child0) {
        float top = child0.getTop();
        if (top > 0) top = 0;
        return Math.abs(top / child0.getHeight());
    }

    public void hideToolbar() {
        ControllerUtils.hideToolbar(mToolbar);
    }

    public void showToolbar() {
        ControllerUtils.showToolbar(mToolbar);
    }

}
