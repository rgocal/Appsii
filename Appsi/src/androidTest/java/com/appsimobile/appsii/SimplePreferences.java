/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.appsii;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.appsimobile.appsii.preference.Preferences;

import net.jcip.annotations.GuardedBy;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by nick on 14/01/15.
 */
public class SimplePreferences implements Preferences, SharedPreferences {

    final Handler mHandler = new Handler(Looper.getMainLooper());

    final List<OnSharedPreferenceChangeListener> mListeners = new LinkedList<>();

    @GuardedBy("this")
    SimplePreferenceState mState = new SimplePreferenceState();

    @Override
    public Map<String, ?> getAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean contains(String key) {
        return mState.contains(key);
    }

    @Override
    public synchronized String getString(String key, String defValue) {
        return mState.getString(key, defValue);
    }

    @Override
    public synchronized int getInt(String key, int defValue) {
        return mState.getInt(key, defValue);
    }

    @Override
    public synchronized long getLong(String key, long defValue) {
        return mState.getLong(key, defValue);
    }

    @Override
    public synchronized float getFloat(String key, float defValue) {
        return mState.getFloat(key, defValue);
    }

    @Override
    public synchronized boolean getBoolean(String key, boolean defValue) {
        return mState.getBoolean(key, defValue);
    }

    @Override
    public synchronized void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        mListeners.add(listener);
    }

    @Override
    public synchronized void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public synchronized Editor edit() {
        return new SimpleEditor(mState);
    }

    public synchronized void put(String key, Object value) {
        mState.put(key, value);
    }

    void saveNewValuesAndNotify(final SimplePreferenceState state, final List<String> changedKeys) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            doSaveNewValuesAndNotify(state, changedKeys);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    doSaveNewValuesAndNotify(state, changedKeys);
                }
            });
        }
    }

    synchronized void doSaveNewValuesAndNotify(SimplePreferenceState state,
            List<String> changedKeys) {
        mState = state;
        for (OnSharedPreferenceChangeListener listener : mListeners) {
            for (String key : changedKeys) {
                listener.onSharedPreferenceChanged(this, key);
            }
        }
    }

    private class SimpleEditor implements Editor {

        final SimplePreferenceState mState;

        final List<String> mChangedKeys = new LinkedList<>();


        public SimpleEditor(
                SimplePreferenceState state) {
            mState = new SimplePreferenceState(state);
        }

        @Override
        public Editor putString(String key, String value) {
            mChangedKeys.add(key);
            mState.mPreferences.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Editor putInt(String key, int value) {
            mChangedKeys.add(key);
            mState.mPreferences.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            mChangedKeys.add(key);
            mState.mPreferences.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            mChangedKeys.add(key);
            mState.mPreferences.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            mChangedKeys.add(key);
            mState.mPreferences.put(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            mChangedKeys.add(key);
            mState.mPreferences.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            mChangedKeys.addAll(mState.mPreferences.keySet());
            return this;
        }

        @Override
        public boolean commit() {
            saveNewValuesAndNotify(mState, mChangedKeys);
            return true;
        }

        @Override
        public void apply() {
            saveNewValuesAndNotify(mState, mChangedKeys);
        }
    }
}
