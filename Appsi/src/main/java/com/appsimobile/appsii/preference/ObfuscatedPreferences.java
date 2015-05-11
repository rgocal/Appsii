/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appsimobile.appsii.preference;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.vending.licensing.Obfuscator;
import com.google.android.vending.licensing.ValidationException;

/**
 * An wrapper for SharedPreferences that transparently performs data obfuscation.
 */
public class ObfuscatedPreferences {

    private static final String TAG = "ObfuscatedPreferences";

    private final SharedPreferences mPreferences;

    private final Obfuscator mObfuscator;

    /**
     * Constructor.
     *
     * @param sp A SharedPreferences instance provided by the system.
     * @param o The Obfuscator to use when reading or writing data.
     */
    public ObfuscatedPreferences(SharedPreferences sp, Obfuscator o) {
        mPreferences = sp;
        mObfuscator = o;
    }

    // It is intentional that we do not commit here, so suppress this warning
    @SuppressLint("CommitPrefEdits")
    public Editor edit() {
        return new Editor(mPreferences.edit());
    }

    public String getString(String key, String defValue) {
        String result;
        String value = mPreferences.getString(key, null);
        if (value != null) {
            try {
                result = mObfuscator.unobfuscate(value, key);
            } catch (ValidationException e) {
                // Unable to unobfuscate, data corrupt or tampered
                Log.w(TAG, "Validation error while reading preference: " + key);
                result = defValue;
            }
        } else {
            // Preference not found
            result = defValue;
        }
        return result;
    }

    public class Editor {

        SharedPreferences.Editor mEditor;

        boolean mValid;

        public Editor(SharedPreferences.Editor editor) {
            mEditor = editor;
            mValid = true;
        }

        public Editor putString(String key, String value) {
            if (!mValid) throw new IllegalStateException("Editor has already been committed");
            String obfuscatedValue = mObfuscator.obfuscate(value, key);
            mEditor.putString(key, obfuscatedValue);
            return this;
        }

        public void apply() {
            mEditor.apply();
            mValid = false;
        }
    }
}
