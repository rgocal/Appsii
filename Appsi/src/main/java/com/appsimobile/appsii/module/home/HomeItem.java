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

/**
 * Created by nick on 20/01/15.
 */
public class HomeItem {

    public long mId;

    public long mPageId;

    public long mRowId;

    public int mDisplayType;

    public int mColspan;

    public int mRowHeight;

    public int mPosition;

    public int mRowPosition;

    public int mEffectColor;


    public String mPageName;

    public HomeItem() {
    }

    public HomeItem(HomeItem item) {
        mId = item.mId;
        mPageId = item.mPageId;
        mRowId = item.mRowId;
        mDisplayType = item.mDisplayType;
        mColspan = item.mColspan;
        mRowHeight = item.mRowHeight;
        mRowPosition = item.mRowPosition;
        mPosition = item.mPosition;
        mPageName = item.mPageName;
        mEffectColor = item.mEffectColor;
    }

    @Override
    public String toString() {
        return "HomeItem{" +
                "mId=" + mId +
                ", mPageId=" + mPageId +
                ", mRowId=" + mRowId +
                ", mColspan=" + mColspan +
                ", mPosition=" + mPosition +
                ", mRowPosition=" + mRowPosition +
                ", mEffectColor=" + mEffectColor +
                '}';
    }
}
