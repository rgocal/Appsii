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
import android.content.Context;

import com.appsimobile.appsii.compat.LauncherActivityInfoCompat;
import com.appsimobile.appsii.compat.LauncherAppsCompat;

import java.text.Collator;
import java.util.Comparator;
import java.util.Map;

/**
 * Created by nick on 20/02/15.
 */
public class ShortcutNameComparator implements Comparator<LauncherActivityInfoCompat> {

    final LauncherAppsCompat mLauncherAppsCompat;

    private final Collator mCollator;

    private final Map<ComponentName, CharSequence> mLabelCache;

    public ShortcutNameComparator(Context context, Map<ComponentName, CharSequence> labelCache) {
        mLabelCache = labelCache;
        mCollator = Collator.getInstance();
        mLauncherAppsCompat = LauncherAppsCompat.getInstance(context);
    }

    public final int compare(LauncherActivityInfoCompat lhr, LauncherActivityInfoCompat rhs) {
        CharSequence labelA, labelB;
        ComponentName keyA = lhr.getComponentName();
        ComponentName keyB = rhs.getComponentName();
        if (mLabelCache.containsKey(keyA)) {
            labelA = mLabelCache.get(keyA);
        } else {
            labelA = lhr.getLabel();
            mLabelCache.put(keyA, labelA);
        }
        if (mLabelCache.containsKey(keyB)) {
            labelB = mLabelCache.get(keyB);
        } else {
            labelB = rhs.getLabel();
            mLabelCache.put(keyB, labelB);
        }
        return mCollator.compare(String.valueOf(labelA), String.valueOf(labelB));
    }
}
