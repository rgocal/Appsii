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

package com.appsimobile.appsii.tinting;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Creates a LayoutInflater that returns properly tinted views.
 * Created by nick on 03/03/15.
 */
public class AppsiLayoutInflater {

//    final LayoutInflater mWrapper;

    final Context mContext;

    private AppsiLayoutInflater(Context context) {
//        mWrapper = new AppsiLayoutInflaterImpl(context);
        mContext = context;
    }

    protected static View onCreateView(Context context, String name, AttributeSet attrs) {
        if (Build.VERSION.SDK_INT < 21) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            switch (name) {
                case "EditText":
                    return new TintEditText(context, attrs);
                case "Spinner":
                    return new TintSpinner(context, attrs);
                case "CheckBox":
                    return new TintCheckBox(context, attrs);
                case "RadioButton":
                    return new TintRadioButton(context, attrs);
                case "CheckedTextView":
                    return new TintCheckedTextView(context, attrs);
            }
        }
        return null;
    }

//    public static LayoutInflater from(Context context) {
//        return new AppsiLayoutInflaterImpl(context);
//    }

    public static class FactoryImpl implements LayoutInflater.Factory2 {

        @Override
        public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
            return AppsiLayoutInflater.onCreateView(context, name, attrs);
        }

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            return AppsiLayoutInflater.onCreateView(context, name, attrs);
        }
    }
//
//    private static class AppsiLayoutInflaterImpl extends LayoutInflater {
//
//        private final Context mContext;
//
//        protected AppsiLayoutInflaterImpl(Context context) {
//            super(context);
//            mContext = context;
//        }
//
//        protected AppsiLayoutInflaterImpl(LayoutInflater original, Context newContext) {
//            super(original, newContext);
//            mContext = newContext;
//        }
//
//        @Override
//        public LayoutInflater cloneInContext(Context newContext) {
//            return new AppsiLayoutInflaterImpl(this, newContext);
//        }
//
//        @Override
//        protected View onCreateView(String name,
//                @NonNull AttributeSet attrs) throws ClassNotFoundException {
//            View result = AppsiLayoutInflater.onCreateView(mContext, name, attrs);
//            if (result == null) {
//                result = super.onCreateView(name, attrs);
//            }
//            return result;
//        }
//    }
}
