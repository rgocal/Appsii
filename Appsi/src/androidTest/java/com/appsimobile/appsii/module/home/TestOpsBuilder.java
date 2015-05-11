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

package com.appsimobile.appsii.module.home;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;

import com.appsimobile.appsii.module.home.HomeAdapter.Operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nick on 11/03/15.
 */
public class TestOpsBuilder implements HomeAdapter.OpsBuilder {

    List<OperationWrapper> mOperationWrappers = new ArrayList<>();

    void reset() {
        mOperationWrappers.clear();
    }

    @Override
    public Operation newInsert(Uri uri) {
        OperationWrapper operation = OperationWrapper.newInsert(uri);
        mOperationWrappers.add(operation);
        return operation;
    }

    @Override
    public Operation newDelete(Uri uri) {
        OperationWrapper operation = OperationWrapper.newDelete(uri);
        mOperationWrappers.add(operation);
        return operation;
    }

    @Override
    public Operation newUpdate(Uri uri) {
        OperationWrapper operation = OperationWrapper.newUpdate(uri);
        mOperationWrappers.add(operation);
        return operation;
    }

    static class OperationWrapper implements Operation {

        static final int TYPE_INSERT = 0;

        static final int TYPE_UPDATE = 1;

        static final int TYPE_DELETE = 2;

        final Uri mUri;

        final int mType;

        ContentProviderOperation.Builder mBuilder;

        ContentValues mContentValues = new ContentValues();

        Map<String, Integer> mBackReferences = new HashMap<>();

        public OperationWrapper(ContentProviderOperation.Builder builder, int type, Uri uri) {
            mBuilder = builder;
            mType = type;
            mUri = uri;
        }

        static OperationWrapper newInsert(Uri uri) {
            return new OperationWrapper(ContentProviderOperation.newInsert(uri), TYPE_INSERT, uri);
        }

        static OperationWrapper newDelete(Uri uri) {
            return new OperationWrapper(ContentProviderOperation.newDelete(uri), TYPE_DELETE, uri);
        }

        static OperationWrapper newUpdate(Uri uri) {
            return new OperationWrapper(ContentProviderOperation.newUpdate(uri), TYPE_UPDATE, uri);
        }

        long uriId() {
            return ContentUris.parseId(mUri);
        }

        @Override
        public Operation withValue(String position, int value) {
            mContentValues.put(position, value);
            mBuilder.withValue(position, value);
            return this;
        }

        @Override
        public Operation withValue(String position, long value) {
            mContentValues.put(position, value);
            mBuilder.withValue(position, value);
            return this;
        }

        @Override
        public Operation withValueBackReference(String columnName, int resultIdx) {
            mBackReferences.put(columnName, resultIdx);
            mBuilder.withValueBackReference(columnName, resultIdx);
            return this;
        }

        @Override
        public Operation withSelection(String selection, String[] selectionArgs) {
            mBuilder.withSelection(selection, selectionArgs);
            return this;
        }

        @Override
        public ContentProviderOperation build() {
            return mBuilder.build();
        }
    }


}
