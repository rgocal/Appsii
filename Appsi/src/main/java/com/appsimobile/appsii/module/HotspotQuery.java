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

import com.appsimobile.appsii.module.home.provider.HomeContract;

/**
 * Created by nick on 01/02/15.
 */
public class HotspotQuery {

    public static final String[] PROJECTION = {
            HomeContract.Hotspots._ID,
            HomeContract.Hotspots.HEIGHT,
            HomeContract.Hotspots.Y_POSITION,
            HomeContract.Hotspots.LEFT_BORDER,
            HomeContract.Hotspots._DEFAULT_PAGE,
            HomeContract.Hotspots.NEEDS_CONFIGURATION,
            HomeContract.Hotspots.ALWAYS_OPEN_LAST,
            HomeContract.Hotspots.NAME,
            HomeContract.Hotspots.DEFAULT_PAGE_NAME,
            HomeContract.Hotspots.DEFAULT_PAGE_TYPE,
    };

    public static final int ID = 0;

    public static final int HEIGHT = 1;

    public static final int Y_POSITION = 2;

    public static final int LEFT_BORDER = 3;

    public static final int _DEFAULT_PAGE = 4;

    public static final int NEEDS_CONFIGURATION = 5;

    public static final int ALWAYS_OPEN_LAST = 6;

    public static final int NAME = 7;

    public static final int DEFAULT_PAGE_NAME = 8;

    public static final int DEFAULT_PAGE_TYPE = 9;
}
