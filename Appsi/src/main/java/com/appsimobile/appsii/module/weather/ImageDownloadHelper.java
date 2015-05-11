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

package com.appsimobile.appsii.module.weather;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.ResponseParserException;
import com.appsimobile.appsii.SimpleJson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Created by nick on 12/04/15.
 */
public class ImageDownloadHelper {

    public static final int MAX_CACHE_SIZE = 10 * 1024 * 1024;

    public static final String FLICKR_API_KEY = "11ed8a19c24662dc6b18979902dc6494";

    static String extras = "tags, url_o, url_h";

    private static ImageDownloadHelper sImageDownloadHelper;

    RequestQueue sRequestQueue;

    ImageLoader sImageLoader;

    private ImageDownloadHelper(Context context) {
        sRequestQueue = Volley.newRequestQueue(context, null, MAX_CACHE_SIZE);
        sImageLoader = new ImageLoader(sRequestQueue, createImageCache()) {
            RetryPolicy mRetryPolicy = new DefaultRetryPolicy(
                    30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

            @Override
            protected Request<Bitmap> makeImageRequest(String requestUrl, int maxWidth,
                    int maxHeight,
                    ImageView.ScaleType scaleType, String cacheKey) {
                Request<Bitmap> result = super.
                        makeImageRequest(requestUrl, maxWidth, maxHeight, scaleType, cacheKey);

                result.setRetryPolicy(mRetryPolicy);
                return result;
            }
        };

    }

    /**
     * Creates a new ImageCache object, used to simplify the constructor
     */
    private static ImageLoader.ImageCache createImageCache() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        return new FlickrImageCache(cacheSize);
    }

    public synchronized static ImageDownloadHelper getInstance(Context context) {
        if (sImageDownloadHelper == null) {
            sImageDownloadHelper = new ImageDownloadHelper(context.getApplicationContext());
        }
        return sImageDownloadHelper;
    }

    public static int getRotationFromJson(JSONObject result) {

        SimpleJson simpleJson = new SimpleJson(result);
        try {
            return simpleJson.getInt("rotation", 0);
        } catch (ResponseParserException e) {
            Log.e("ImageDownloadHelper", "No rotation attr", e);
            return 0;
        }
    }

    public static int getFallbackDrawableForConditionCode(boolean isDay, int conditionCode) {
        // http://developer.yahoo.com/weather/
        switch (conditionCode) {
            case 19: // dust or sand
            case 21: // haze
            case 20: // foggy
            case 22: // smoky
            case 24: // windy
            case 25: // cold
            case 26: // cloudy
            case 27: // mostly cloudy (night)
            case 28: // mostly cloudy (day)
            case 29: // partly cloudy (night)
            case 30: // partly cloudy (day)
            case 44: // partly cloudy
                return isDay ? R.drawable.weather_cloudy_day : R.drawable.weather_cloudy_night;
            case 31: // clear (night)
            case 33: // fair (night)
                return R.drawable.weather_clear_night;
            default:
            case 34: // fair (day)
            case 32: // sunny
            case 36: // hot
                return isDay ? R.drawable.weather_clear_day : R.drawable.weather_clear_night;
            case 0: // tornado
            case 2: // hurricane
            case 1: // tropical storm
            case 4: // thunderstorms
            case 23: // blustery
                // TODO: add wind
            case 3: // severe thunderstorms
                // TODO: add thunder
            case 5: // mixed rain and snow
            case 6: // mixed rain and sleet
            case 7: // mixed snow and sleet
            case 8: // freezing drizzle
            case 10: // freezing rain
            case 13: // snow flurries
            case 14: // light snow showers
            case 42: // scattered snow showers
            case 15: // blowing snow
            case 16: // snow
            case 41: // heavy snow
            case 43: // heavy snow
            case 46: // snow showers
                return isDay ? R.drawable.weather_snowy_day : R.drawable.weather_snowy_night;

            case 18: // sleet
            case 9: // drizzle
            case 11: // showers
            case 12: // showers
            case 17: // hail
            case 35: // mixed rain and hail
            case 37: // isolated thunderstorms
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
            case 40: // scattered showers
            case 45: // thundershowers
            case 47: // isolated thundershowers
                return isDay ? R.drawable.weather_cloudy_day : R.drawable.weather_cloudy_night;
        }
    }

    public static void getEligiblePhotosFromResponse(@Nullable JSONObject jsonObject,
            List<PhotoInfo> result,
            int minDimension) {
        result.clear();

        if (jsonObject == null) return;

        JSONObject photos = jsonObject.optJSONObject("photos");
        if (photos == null) return;

        JSONArray photoArr = photos.optJSONArray("photo");
        if (photoArr == null) return;

        int N = photoArr.length();
        for (int i = 0; i < N; i++) {
            JSONObject object = photoArr.optJSONObject(i);
            if (object == null) continue;
            String id = object.optString("id");
            if (TextUtils.isEmpty(id)) continue;
            String urlH = urlFromImageObject(
                    object, "url_h", "width_h", "height_h", minDimension - 100);
            String urlO = urlFromImageObject(
                    object, "url_o", "width_o", "height_o", minDimension - 100);

            if (urlH != null) {
                result.add(new PhotoInfo(id, urlH));
            } else if (urlO != null) {
                result.add(new PhotoInfo(id, urlO));
            }
        }

    }

    static String urlFromImageObject(JSONObject object, String u, String w, String h,
            int minDimension) {
        String url = object.optString(u);
        if (TextUtils.isEmpty(url)) return null;
        int heightO = object.optInt(h);
        int widthO = object.optInt(w);
        if (heightO < minDimension || widthO < minDimension) return null;
        return url;
    }

    private static String getTagForConditionCode(int conditionCode) {
        // http://developer.yahoo.com/weather/
        switch (conditionCode) {
            case 19: // dust or sand
                return "dust";
            case 21: // haze
                return "haze";
            case 20: // foggy
                return "fog";
            case 22: // smoky
                return "smoky";
            case 24: // windy
                return "windy";
            case 25: // cold
                return "cold";
            case 26: // cloudy
            case 27: // mostly cloudy (night)
            case 28: // mostly cloudy (day)
            case 29: // partly cloudy (night)
            case 30: // partly cloudy (day)
            case 44: // partly cloudy
                return "cloudy";
            case 31: // clear (night)
            case 33: // fair (night)
            case 34: // fair (day)
                return "clear";
            case 32: // sunny
                return "sunny";
            case 36: // hot
                return "hot";
            case 0: // tornado
                return "tornado";
            case 2: // hurricane
                return "hurricane";
            case 1: // tropical storm
            case 3: // severe thunderstorms
            case 4: // thunderstorms
            case 23: // blustery
                return "thunder";
            case 5: // mixed rain and snow
            case 6: // mixed rain and sleet
            case 7: // mixed snow and sleet
            case 8: // freezing drizzle
            case 10: // freezing rain
            case 18: // sleet
                return "snow";
            case 9: // drizzle
                return "drizzle";
            case 11: // showers
            case 12: // showers
                return "showers";
            case 17: // hail
            case 35: // mixed rain and hail
                return "hail";
            case 37: // isolated thunderstorms
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
                return "thunder";
            case 40: // scattered showers
                return "rainy";
            case 45: // thundershowers
            case 47: // isolated thundershowers
                return "thunder";
            case 13: // snow flurries
            case 14: // light snow showers
            case 42: // scattered snow showers
            case 15: // blowing snow
            case 16: // snow
            case 41: // heavy snow
            case 43: // heavy snow
            case 46: // snow showers
                return "snow";
        }

        return "clear";
    }

    public ImageLoader getImageLoader() {
        return sImageLoader;
    }

    @Nullable
    public JSONObject searchCityWeatherPhotos(String woeid, int conditionCode, boolean day)
            throws VolleyError {

        String tags = getTagForConditionCode(conditionCode) + "," + (day ? "day" : "night");

        Uri uri = Uri.parse("https://api.flickr.com/services/rest/").buildUpon().
                appendQueryParameter("method", "flickr.photos.search").
                appendQueryParameter("api_key", FLICKR_API_KEY).
                appendQueryParameter("sort", "relevance").
                appendQueryParameter("tags", tags).
                appendQueryParameter("tag_mode", "all").
                appendQueryParameter("privacy_filter", "1").
                appendQueryParameter("content_type", "1").
                appendQueryParameter("group_id", "1463451@N25").
//                appendQueryParameter("group_id", "1553326@N24").
        appendQueryParameter("woe_id", woeid).
                appendQueryParameter("media", "photos").
                appendQueryParameter("extras", extras).
                appendQueryParameter("format", "json").
                appendQueryParameter("nojsoncallback", "1").
                build();

        String url = uri.toString();

        RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();

        JsonObjectRequest request =
                new JsonObjectRequest(Request.Method.GET, url, requestFuture, requestFuture);
        sRequestQueue.add(request);

        return getResult(requestFuture);
    }

    private <T> T getResult(RequestFuture<T> requestFuture) throws VolleyError {
        try {
            return requestFuture.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            return null;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof VolleyError) throw (VolleyError) cause;
            return null;
        }

    }
//
//    public static void getImageUrlsFromResult(JSONObject jsonObject, List<String> result,
//            int minDimension) {
//        result.clear();
//
//        JSONObject photos = jsonObject.optJSONObject("photos");
//        if (photos == null) return;
//
//        JSONArray photoArr = photos.optJSONArray("photo");
//        if (photoArr == null) return;
//
//        int N = photoArr.length();
//        for (int i = 0; i < N; i++) {
//            JSONObject object = photoArr.optJSONObject(i);
//            if (object == null) continue;
//
//            String urlH = urlFromImageObject(
//                    object, "url_h", "width_h", "height_h", minDimension - 100);
//
//            if (urlH != null) {
//                result.add(urlH);
//            } else {
//                String urlO = urlFromImageObject(
//                        object, "url_o", "width_o", "height_o", minDimension - 100);
//                if (urlO != null) {
//                    result.add(urlO);
//                }
//            }
//        }
//    }

    public JSONObject searchCityImage(String woeid) throws VolleyError {

        Uri uri = Uri.parse("https://api.flickr.com/services/rest/").buildUpon().
                appendQueryParameter("method", "flickr.photos.search").
                appendQueryParameter("api_key", FLICKR_API_KEY).
                appendQueryParameter("sort", "relevance").
                appendQueryParameter("tag_mode", "all").
                appendQueryParameter("privacy_filter", "1").
                appendQueryParameter("content_type", "1").
                appendQueryParameter("group_id", "1463451@N25").
                appendQueryParameter("woe_id", woeid).
                appendQueryParameter("media", "photos").
                appendQueryParameter("extras", extras).
                appendQueryParameter("format", "json").
                appendQueryParameter("nojsoncallback", "1").
                build();

        String url = uri.toString();

        RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();

        JsonObjectRequest request =
                new JsonObjectRequest(Request.Method.GET, url, requestFuture, requestFuture);
        sRequestQueue.add(request);
        return getResult(requestFuture);
    }

    public JSONObject loadPhotoInfo(Context context, String photoId) throws VolleyError {

        Uri uri = Uri.parse("https://api.flickr.com/services/rest/").buildUpon().
                appendQueryParameter("method", "flickr.photos.getInfo").
                appendQueryParameter("api_key", FLICKR_API_KEY).
                appendQueryParameter("photo_id", photoId).
                appendQueryParameter("format", "json").
                appendQueryParameter("nojsoncallback", "1").
                build();

        String url = uri.toString();

        RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();

        JsonObjectRequest request =
                new JsonObjectRequest(Request.Method.GET, url, requestFuture, requestFuture);
        sRequestQueue.add(request);
        return getResult(requestFuture);
    }

    public static class PhotoInfo {

        public final String id;

        public final String url;

        public PhotoInfo(String id, String url) {
            this.id = id;
            this.url = url;
        }
    }

    /**
     * Implements the ImageCache. This is needed for the ImageLoader.
     * This class uses a simply implementation of LruCache to provide
     * memory sensitive caching.
     */
    private static class FlickrImageCache implements ImageLoader.ImageCache {

        private final LruCache<String, Bitmap> mCache;

        private final int mCacheSize;

        FlickrImageCache(int cacheSize) {
            mCacheSize = cacheSize;
            mCache = new ImageLruCache(mCacheSize);
        }

        @Override
        public Bitmap getBitmap(String url) {
            return mCache.get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            mCache.put(url, bitmap);
        }
    }

    /**
     * The memory cache for the images. This is used by the ImageLoader
     */
    private static class ImageLruCache extends LruCache<String, Bitmap> {

        ImageLruCache(int cacheSize) {
            super(cacheSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            // The cache size will be measured in kilobytes rather than
            // number of items.
            return bitmap.getByteCount() / 1024;
        }
    }

}
