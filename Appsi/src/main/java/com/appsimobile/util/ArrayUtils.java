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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by nick on 09/08/15.
 */
public final class ArrayUtils {

    private ArrayUtils() {
    }

    public static String join(String separator, CircularArray<String> items) {
        StringBuilder result = new StringBuilder();
        return join(separator, items, result);
    }

    public static String join(String separator, CircularArray<String> items,
            StringBuilder appendTo) {
        int N = items.size();
        for (int i = 0; i < N; i++) {
            if (i > 0) {
                appendTo.append(separator);
            }
            appendTo.append(items.get(i));
        }
        return appendTo.toString();
    }

    public static <T> void sort(CircularArray<T> list, Comparator<? super T> comparator) {
        if (list.isEmpty()) return;
        T first = list.getFirst();
        T[] array = (T[]) Array.newInstance(first.getClass(), list.size());
        array = toArray(list, array);

        Arrays.sort(array, comparator);
        list.clear();
        for (T item : array) {
            list.addLast(item);
        }
    }

    private static <T> T[] toArray(CircularArray<T> items, T[] result) {
        int N = items.size();
        if (N != result.length) throw new IllegalArgumentException("Wrong array size");
        for (int i = 0; i < N; i++) {
            result[i] = items.get(i);
        }
        return result;
    }

    public static <T> boolean contains(CircularArray<T> array, T item) {
        int N = array.size();
        for (int i = 0; i < N; i++) {
            if (item == null && array.get(i) == null) return true;
            if (item != null && item.equals(array.get(i))) return true;
        }
        return false;
    }

    public static <T> void addAll(CircularArray<T> target, CircularArray<T> source) {
        int N = source.size();
        for (int i = 0; i < N; i++) {
            target.addLast(source.get(i));
        }
    }

    @SafeVarargs
    public static <T> CircularArray<T> asArray(T... items) {
        CircularArray<T> result = new CircularArray<>();
        if (items != null) {
            for (T t : items) {
                result.addLast(t);
            }
        }
        return result;
    }


}
