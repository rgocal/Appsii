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

package com.appsimobile.appsii;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by nick on 13/02/15.
 */
public class StatusbarUnderlay extends FrameLayout {

    SidebarContext mSidebarContext;


    public StatusbarUnderlay(Context context) {
        super(context);
    }

    public StatusbarUnderlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StatusbarUnderlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSidebarContext(SidebarContext sidebarContext) {
        mSidebarContext = sidebarContext;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mSidebarContext == null || !mSidebarContext.mIsFullScreen) {
            super.draw(canvas);
        }
    }
}
