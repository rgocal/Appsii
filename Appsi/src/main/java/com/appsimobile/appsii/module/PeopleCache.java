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

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.LruCache;

import net.jcip.annotations.GuardedBy;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by nick on 29/05/14.
 */
public class PeopleCache {

    static PeopleCache sInstance = new PeopleCache();

    private final LruCache<Uri, Bitmap> mMemoryCache;

    @GuardedBy("this")
    private final Set<Uri> mUrisWithoutBitmaps = new HashSet<>();

    PeopleCache() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/96th of the available memory for this memory cache.
        // This is just to speed things up a bit, not to retain too much
        final int cacheSize = maxMemory / 96;

        mMemoryCache = new LruCache<Uri, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Uri key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static PeopleCache getInstance() {
        return sInstance;
    }

    public synchronized boolean isKnownNoBitmap(Uri uri) {
        return mUrisWithoutBitmaps.contains(uri);
    }

    public Bitmap getBitmap(Uri uri) {
        return mMemoryCache.get(uri);
    }

    public void addToCache(Uri uri, Bitmap bitmap) {
        if (bitmap == null) {
            synchronized (this) {
                mUrisWithoutBitmaps.add(uri);
            }
        } else {
            synchronized (mMemoryCache) {
                if (mMemoryCache.get(uri) == null) {
                    mMemoryCache.put(uri, bitmap);
                }
            }
        }
    }

    public void onTrimMemory() {
        mMemoryCache.evictAll();
    }
}
