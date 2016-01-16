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

package com.appsimobile.appsii.plugins;

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.appsimobile.appsii.compat.MapCompat;

import java.lang.ref.WeakReference;
import java.util.Map;

import javax.inject.Inject;

public class IconCache {

    private static IconCache sInstance;

    // the lru cache maintains a list of all cached icons
    private final LruCache<CacheKey, Bitmap> mCache;

    // the weakCache will hold on to any icon really in use.
    // by using this we prevent loading icons twice
    private final Map<CacheKey, WeakReference<Bitmap>> mWeakCache = MapCompat.createMap();

    private final int mCacheSize;

    @Inject
    public IconCache(ActivityManager activityManager) {
        final int memClass = activityManager.getMemoryClass();
        int sizeInBytes = memClass * 1024 * 1024;
        int desiredSize = sizeInBytes / 20;
        // for 50 mb devices this will be 2.5 mb
        // for 12 mb devices this is 0.6 mb
        mCacheSize = desiredSize;
        mCache = new LruCache<CacheKey, Bitmap>(desiredSize) {
            @Override
            protected int sizeOf(CacheKey key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };
    }

    public void onClose() {
        mCache.trimToSize((int) (mCacheSize / 1.5));
    }

    public void cacheIcon(String externalId, String method, Bitmap result, boolean large) {
        cacheIcon(new CacheKey(externalId, method, large), result);
    }

    private void cacheIcon(CacheKey cn, Bitmap result) {
        mCache.put(cn, result);
        mWeakCache.put(cn, new WeakReference<>(result));
    }

    public Bitmap getCachedIcon(String iconResolveUriPrefix, String externalId, boolean large) {
        CacheKey c = new CacheKey(externalId, iconResolveUriPrefix, large);
        Bitmap result = mCache.get(c);
        if (result == null) {
            WeakReference<Bitmap> b = mWeakCache.get(c);
            if (b != null) {
                result = b.get();
            }
            if (result != null) {
                // add it to the cache again so that the cache knows it is in use
                // and lru hit is know. also the memory limits are more realistic
                mCache.put(c, result);
            }
        }
        return result;
    }

    void onTrimMemory(int level) {
        mCache.evictAll();
    }

    void onLowMemory() {
        mCache.evictAll();
    }

    public void clearAllIcons() {
        mCache.evictAll();
        mWeakCache.clear();
    }

//	public static Bitmap loadApplicationIcon(String uriPrefix, String id, int w, int h) {
//		
//		return AppIconHelper.loadIcon(cn, w, h);
//	}

    static class CacheKey {

        final String mExternalId;

        final String mIconResolveUriPrefix;

        final boolean mLargeImage;

        int mHashCode;

        boolean mHashcodeValid = false;

        public CacheKey(String externalId, String method, boolean largeImage) {
            mExternalId = externalId;
            mIconResolveUriPrefix = method;
            mLargeImage = largeImage;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            if (mLargeImage != other.mLargeImage) {
                return false;
            }

            if (mExternalId == null) {
                if (other.mExternalId != null) return false;
            } else if (!mExternalId.equals(other.mExternalId)) {
                return false;
            }

            if (mIconResolveUriPrefix == null) {
                if (other.mIconResolveUriPrefix != null) return false;
            } else if (!mIconResolveUriPrefix.equals(other.mIconResolveUriPrefix)) {
                return false;
            }
            return true;
        }

        @Override
        public synchronized int hashCode() {
            if (mHashcodeValid) {
                return mHashCode;
            }

            mHashcodeValid = true;
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((mExternalId == null) ? 0 : mExternalId.hashCode());
            result = prime
                    * result
                    + ((mIconResolveUriPrefix == null) ? 0
                    : mIconResolveUriPrefix.hashCode());
            result = prime * result + (mLargeImage ? 1231 : 1237);
            mHashCode = result;

            return result;
        }


    }


}
