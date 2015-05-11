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

import android.content.ComponentName;
import android.support.annotation.NonNull;

/**
 * Created by nick on 20/10/14.
 */
public class HistoryItem implements Comparable<HistoryItem> {

    public int launchCount;

    public long lastLaunched;

    public ComponentName componentName;

    public static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static int compare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    @Override
    public int compareTo(@NonNull HistoryItem another) {
        int result = -compare(launchCount, another.launchCount);
        if (result == 0) {
            result = -compare(lastLaunched, another.lastLaunched);
        }
        return result;
    }

}
