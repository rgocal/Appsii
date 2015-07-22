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
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 12/02/15.
 */
class AppPageData {

    // contains a mapping of tag-id to a list of apps in the tag
    final LongSparseArray<List<AppEntry>> mAppsPerTag =
            new LongSparseArray<>();

    // contains a mapping of components to app-tags. To quickly identify
    // the tags a component has.
    final SimpleArrayMap<ComponentName, List<TaggedApp>> mTagsPerComponent =
            new SimpleArrayMap<>();

    @Nullable
    final List<AppEntry> mAllApps;

    final List<HistoryItem> mRecentApps;

    final List<AppTag> mAppTags;


    AppPageData(@Nullable List<? extends AppEntry> allApps, List<HistoryItem> recentApps,
            List<AppTag> tags) {
        mAllApps = new ArrayList<>();
        if (allApps != null) {
            mAllApps.addAll(allApps);
        }
        mRecentApps = recentApps;
        mAppTags = tags;

        SimpleArrayMap<ComponentName, AppEntry> appEntriesByComponent;
        if (allApps != null) {
            int N = allApps.size();
            appEntriesByComponent = new SimpleArrayMap<>(N);
            for (int i = 0; i < N; i++) {
                AppEntry app = allApps.get(i);
                appEntriesByComponent.put(app.getComponentName(), app);
            }
        } else {
            appEntriesByComponent = new SimpleArrayMap<>(0);
        }


        long recentAppsTagId = recentAppsTagId(tags);
        long allAppsTagId = allAppsTagId(tags);

        mAppsPerTag.put(allAppsTagId, mAllApps);

        List<AppEntry> recentAppEntries = new ArrayList<>(recentApps.size());
        for (int i = 0; i < recentApps.size(); i++) {
            HistoryItem item = recentApps.get(i);
            AppEntry e = appEntriesByComponent.get(item.componentName);
            if (e != null) {
                recentAppEntries.add(e);
            }
        }

        mAppsPerTag.put(recentAppsTagId, recentAppEntries);
    }

    private long recentAppsTagId(List<AppTag> tags) {
        for (AppTag tag : tags) {
            if (tag.tagType == AppsContract.TagColumns.TAG_TYPE_RECENT) {
                return tag.id;
            }
        }
        return 0;
    }

    private long allAppsTagId(List<AppTag> tags) {
        int N = tags.size();
        for (int i = 0; i < N; i++) {
            AppTag tag = tags.get(i);
            if (tag.tagType == AppsContract.TagColumns.TAG_TYPE_ALL) {
                return tag.id;
            }
        }
        return 0;
    }

}
