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

import android.content.ContentUris;
import android.net.Uri;

import com.appsimobile.appsii.module.home.provider.HomeContract;

/**
 * Created by nick on 31/01/15.
 */
public class HotspotPagesQuery {

    public static final String[] ARGS = {
            HomeContract.HotspotDetails._HOTSPOT_ID,
            HomeContract.HotspotDetails._PAGE_ID,
            HomeContract.HotspotDetails.POSITION,
            HomeContract.HotspotDetails.ENABLED,
            HomeContract.HotspotDetails.PAGE_NAME,
            HomeContract.HotspotDetails.HOTSPOT_NAME,
            HomeContract.HotspotDetails.PAGE_TYPE,
    };

    public static final int HOTSPOT_ID = 0;

    public static final int PAGE_ID = 1;

    public static final int POSITION = 2;

    public static final int ENABLED = 3;

    public static final int PAGE_NAME = 4;

    public static final int HOTSPOT_NAME = 5;

    public static final int PAGE_TYPE = 6;

    public static Uri createUri(long hotspotId) {
        return ContentUris.withAppendedId(HomeContract.HotspotDetails.CONTENT_URI, hotspotId);
    }

}
