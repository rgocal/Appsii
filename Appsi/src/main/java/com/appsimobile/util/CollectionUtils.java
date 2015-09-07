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
 *
 */

package com.appsimobile.util;

import android.support.v4.util.CircularArray;
import android.support.v4.util.SimpleArrayMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by nmartens on 22/07/15.
 */
public class CollectionUtils {

    private static final ArrayList<?> EMPTY_ARRAY_LIST = new ArrayList<Object>() {
        @Override
        public boolean add(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, Collection collection) {
            throw new UnsupportedOperationException();
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> ArrayList<T> emptyList() {
        return (ArrayList<T>) EMPTY_ARRAY_LIST;
    }

    public static <T> CircularArray<T> emptyArray() {
        return new CircularArray<>(1);
    }


    public static <T> void addKeys(SimpleArrayMap<T, ?> map, List<T> target) {
        int N = map.size();
        for (int i = 0; i < N; i++) {
            T p = map.keyAt(i);
            target.add(p);
        }
    }

    public static <T> void addValues(SimpleArrayMap<?, T> map, List<T> target) {
        int N = map.size();
        for (int i = 0; i < N; i++) {
            T p = map.valueAt(i);
            target.add(p);
        }
    }

    @SafeVarargs
    public static <T> ArrayList<?> asList(T... items) {
        ArrayList<T> result = new ArrayList<>();
        if (items != null) {
            for (T t : items) {
                result.add(t);
            }
        }
        return result;
    }

}
