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

package com.appsimobile.appsii.module.people;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;

import com.appsimobile.appsii.PermissionDeniedException;
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.appsii.module.PeopleQuery;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.util.ConvertedCursorLoader;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by nick on 22/09/14.
 */
public class PeopleLoader extends ConvertedCursorLoader<PeopleLoaderResult> {


    /**
     * The default sort order for this table.
     */
    private static final String DEFAULT_SORT_ORDER =
            ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
    PermissionUtils mPermissionUtils;
    private BroadcastReceiver mPermissionGrantedReceiver;

    @Inject
    public PeopleLoader(Context context, PermissionUtils permissionUtils) {
        super(context);
        mPermissionUtils = permissionUtils;

        Uri uri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(ContactsContract.Directory.DEFAULT)).build();

        String select = "((" + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL) AND ("
                + ContactsContract.Contacts.DISPLAY_NAME + " != '' ))";


        setUri(uri);
        setProjection(PeopleQuery.CONTACTS_SUMMARY_PROJECTION);
        setSelection(select);
        setSelectionArgs(null);
        setSortOrder(DEFAULT_SORT_ORDER);
    }

    @Override
    protected void checkPermissions() throws PermissionDeniedException {
        mPermissionUtils.throwIfNotPermitted(getContext(), Manifest.permission.READ_CONTACTS);
    }

    @Override
    protected PeopleLoaderResult convertPermissionDeniedException(PermissionDeniedException e) {
        return new PeopleLoaderResult(e);
    }

    @Override
    protected PeopleLoaderResult convertCursor(@NonNull Cursor c) {

        List<? extends BaseContactInfo> contactInfos = PeopleQuery.cursorToContactInfos(c);
        return new PeopleLoaderResult(contactInfos);

    }

    @Override
    protected void cleanup(PeopleLoaderResult old) {

    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        mPermissionGrantedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int req = intent.getIntExtra(PermissionUtils.EXTRA_REQUEST_CODE, 0);
                if (req == PermissionUtils.REQUEST_CODE_PERMISSION_READ_CONTACTS) {
                    onContentChanged();
                }
            }
        };
        IntentFilter filter2 = new IntentFilter(PermissionUtils.ACTION_PERMISSION_RESULT);
        getContext().registerReceiver(mPermissionGrantedReceiver, filter2);

    }

    @Override
    protected void onReset() {
        super.onReset();

        if (mPermissionGrantedReceiver != null) {
            getContext().unregisterReceiver(mPermissionGrantedReceiver);
        }
    }


}
