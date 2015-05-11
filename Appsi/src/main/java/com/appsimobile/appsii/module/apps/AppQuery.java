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
public class AppQuery {

    public static final String[] PROJECTION = {
            AppsContract.TaggedAppColumns._ID,
            AppsContract.TaggedAppColumns.COMPONENT_NAME,
            AppsContract.TaggedAppColumns.POSITION,
            AppsContract.JOINED_APP_TAGS.TAG_NAME,
            AppsContract.JOINED_APP_TAGS.TAG_ID,
            AppsContract.TaggedAppColumns.DELETED,
    };

    public static final String ORDER = AppsContract.TaggedAppColumns.TABLE_NAME + "." +
            AppsContract.TaggedAppColumns.POSITION + " ASC";

    public static final String WHERE_NOT_DELETED = AppsContract.TaggedAppColumns.TABLE_NAME + "." +
            AppsContract.TaggedAppColumns.DELETED + "=0";

    public static final String WHERE_DELETED = AppsContract.TaggedAppColumns.TABLE_NAME + "." +
            AppsContract.TaggedAppColumns.DELETED + "=1";

    public static final int _ID = 0;

    public static final int COMPONENT_NAME = 1;

    public static final int POSITION = 2;

    public static final int TAG_NAME = 3;

    public static final int TAG_ID = 4;

    public static final int DELETED = 5;


}
