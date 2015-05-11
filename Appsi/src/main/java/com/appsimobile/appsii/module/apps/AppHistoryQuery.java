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
 * Created by nick on 20/10/14.
 */
public class AppHistoryQuery {

    public static final String[] PROJECTION = {
            AppsContract.LaunchHistoryColumns._ID,
            AppsContract.LaunchHistoryColumns.LAUNCH_COUNT,
            AppsContract.LaunchHistoryColumns.LAST_LAUNCHED,
            AppsContract.LaunchHistoryColumns.COMPONENT_NAME,
    };


    public static final int ID = 0;

    public static final int LAUNCH_COUNT = 1;

    public static final int LAST_LAUNCHED = 2;

    public static final int COMPONENT_NAME = 3;


}
