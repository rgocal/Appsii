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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;

import com.appsimobile.appsii.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple json wrapper that can be used to parse json paths in a memory
 * efficient and effective way.
 * <p/>
 * Created by Nick on 09/10/14.
 */
public class SimpleJson {

    final JSONObject mJsonObject;

    SimpleArrayMap<String, SimpleJson> mParsedChildren;

    public SimpleJson(String json) throws JSONException {
        this(new JSONObject(json));
    }

    public SimpleJson(JSONObject object) {
        mJsonObject = object;
    }

    @Nullable
    static SimpleJson getChild(@NonNull SimpleJson parent, String segment) {
        JSONObject object = parent.mJsonObject;
        SimpleArrayMap<String, SimpleJson> map = parent.mParsedChildren;

        if (map == null) {
            JSONObject obj = object.optJSONObject(segment);
            if (obj == null) return null;

            parent.mParsedChildren = new SimpleArrayMap<>();
            map = parent.mParsedChildren;

            map.put(segment, new SimpleJson(obj));
        }


        SimpleJson result;
        if (map.containsKey(segment)) {
            result = map.get(segment);
        } else {
            JSONObject obj = object.optJSONObject(segment);
            if (obj == null) return null;

            result = new SimpleJson(obj);
            map.put(segment, result);
        }


        return result;
    }

    /**
     * Gets a string for the given path. The path must exist, and the value must exist
     *
     * @throws ResponseParserException when the object specified does not exist
     */
    @NonNull
    public String requiredString(String path) throws ResponseParserException {
        String result = optString(path);
        ResponseParserException.throwIfNull(result, path);
        return result;
    }

    @Nullable
    public String optString(String path) {
        SimpleJson immediateParent = optChildForPath(path);

        if (immediateParent == null) return null;

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return null;
        return immediateParent.mJsonObject.optString(segment);
    }

    @Nullable
    private SimpleJson optChildForPath(String path) {
        return childForPath(path);
    }

    @Nullable
    private String lastPathSegment(String path) {
        String[] segments = path.split("\\.");
        int length = segments.length;
        if (length == 0) return null;
        return segments[length - 1];
    }

    @Nullable
    @VisibleForTesting
    SimpleJson childForPath(String path) {
        String[] pathSegments = path.split("\\.");
        int count = pathSegments.length;

        SimpleJson currentObject = this;
        for (int i = 0; i < count - 1; i++) {
            String segment = pathSegments[i];
            currentObject = getChild(currentObject, segment);

            if (currentObject == null) return null;
        }
        return currentObject;
    }

    @NonNull
    public JSONObject requiredJsonObject(String path) throws ResponseParserException {
        JSONObject result = optJsonObject(path);
        ResponseParserException.throwIfNull(result, path);
        return result;
    }

    @Nullable
    public JSONObject optJsonObject(String path) {
        SimpleJson immediateParent = optChildForPath(path);

        if (immediateParent == null) return null;

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return null;
        return immediateParent.mJsonObject.optJSONObject(segment);
    }

    @NonNull
    public JSONArray requiredJsonArray(String path) throws ResponseParserException {
        JSONArray result = optJsonArray(path);
        ResponseParserException.throwIfNull(result, path);
        return result;
    }

    @Nullable
    public JSONArray optJsonArray(String path) throws ResponseParserException {
        SimpleJson immediateParent = optChildForPath(path);

        if (immediateParent == null) return null;

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return null;
        return immediateParent.mJsonObject.optJSONArray(segment);
    }

    /**
     * Gets a string for the given path. The path must exist, the value is optional
     *
     * @throws ResponseParserException when the path does not exist
     */
    @Nullable
    public String getString(String path) throws ResponseParserException {
        SimpleJson immediateParent = requiredChildForPath(path);

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return null;
        return immediateParent.mJsonObject.optString(segment);
    }

    @NonNull
    private SimpleJson requiredChildForPath(String path) throws ResponseParserException {
        SimpleJson immediateParent = childForPath(path);
        ResponseParserException.throwIfNull(immediateParent, path);

        // ResponseParserException.throwIfNull will make sure this value is non-null
        //noinspection ConstantConditions
        return immediateParent;
    }

    @Nullable
    public JSONArray getJsonArray(String path) throws ResponseParserException {
        SimpleJson immediateParent = requiredChildForPath(path);

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return null;
        return immediateParent.mJsonObject.optJSONArray(segment);
    }

    public long requiredLong(String path) throws ResponseParserException {
        SimpleJson immediateParent = requiredChildForPath(path);

        String segment = lastPathSegment(path);
        try {
            if (immediateParent.mJsonObject.isNull(segment)) {
                throw ResponseParserException.forPath(path);
            }
            return immediateParent.mJsonObject.getLong(segment);
        } catch (JSONException e) {
            throw ResponseParserException.forPath(path);
        }
    }

    public long getLong(String path, long fallback) throws ResponseParserException {
        SimpleJson immediateParent = requiredChildForPath(path);

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return fallback;
        return immediateParent.mJsonObject.optLong(segment, fallback);
    }

    public double getDouble(String path, double fallback) throws ResponseParserException {
        SimpleJson immediateParent = requiredChildForPath(path);

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return fallback;
        return immediateParent.mJsonObject.optDouble(segment, fallback);
    }

    public double getFloat(String path, float fallback) throws ResponseParserException {
        SimpleJson immediateParent = requiredChildForPath(path);

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return fallback;
        return immediateParent.mJsonObject.optDouble(segment, fallback);
    }

    public int getInt(String path, int fallback) throws ResponseParserException {
        SimpleJson immediateParent = requiredChildForPath(path);

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return fallback;
        return immediateParent.mJsonObject.optInt(segment, fallback);
    }

    public int optInt(String path, int fallback) {
        SimpleJson immediateParent = optChildForPath(path);

        if (immediateParent == null) return fallback;

        String segment = lastPathSegment(path);
        if (immediateParent.mJsonObject.isNull(segment)) return fallback;
        return immediateParent.mJsonObject.optInt(segment, fallback);
    }

    public boolean hasPath(String path) {
        SimpleJson immediateParent = optChildForPath(path);
        return immediateParent != null;
    }

}
