package com.appsimobile.util;

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
        public boolean addAll(Collection collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, Collection collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, Object object) {
            throw new UnsupportedOperationException();
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> ArrayList<T> emptyList() {
        return (ArrayList<T>) EMPTY_ARRAY_LIST;
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
    public static <T> ArrayList<T> asList(T... items) {
        ArrayList<T> result = new ArrayList<>();
        if (items != null) {
            for (T t : items) {
                result.add(t);
            }
        }
        return result;
    }

}
