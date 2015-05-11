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

package com.appsimobile.appsii.module.apps;

/**
 * Created by nick on 24/08/14.
 */
public class AppTagQuery {

    public static final String[] PROJECTION = {
            AppsContract.TagColumns._ID,
            AppsContract.TagColumns.DEFAULT_EXPANDED,
            AppsContract.TagColumns.NAME,
            AppsContract.TagColumns.POSITION,
            AppsContract.TagColumns.VISIBLE,
            AppsContract.TagColumns.TAG_TYPE,
            AppsContract.TagColumns.COLUMN_COUNT,
    };

    public static final String ORDER = AppsContract.TagColumns.POSITION + " ASC";

    public static final int _ID = 0;

    public static final int DEFAULT_EXPANDED = 1;

    public static final int NAME = 2;

    public static final int POSITION = 3;

    public static final int VISIBLE = 4;

    public static final int TAG_TYPE = 5;

    public static final int COLUMN_COUNT = 6;

}
