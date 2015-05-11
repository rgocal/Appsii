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

package com.appsimobile.appsii.module;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 16/02/15.
 */
public class PeopleQuery {

    public static final int CONTACTS_SUMMARY_IDX_ID = 0;

    public static final int CONTACTS_SUMMARY_IDX_DISPLAY_NAME = 1;

    public static final int CONTACTS_SUMMARY_IDX_PHONETIC_NAME = 2;

    public static final int CONTACTS_SUMMARY_IDX_CONTACT_PRESENCE = 3;

    public static final int CONTACTS_SUMMARY_IDX_PHOTO_ID = 4;

    public static final int CONTACTS_SUMMARY_IDX_LOOKUP_KEY = 5;

    public static final int CONTACTS_SUMMARY_IDX_PHOTO_URI = 6;

    public static final int CONTACTS_DISPLAY_NAME_SOURCE = 7;

    // These are the Contacts rows that we will retrieve.
    public static final String[] CONTACTS_SUMMARY_PROJECTION = new String[]{
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHONETIC_NAME,
            ContactsContract.Contacts.CONTACT_PRESENCE,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.DISPLAY_NAME_SOURCE,

    };

    public static List<? extends BaseContactInfo> cursorToContactInfos(Cursor cursor) {
        List<BaseContactInfo> result = new ArrayList<>();
        cursor.moveToPosition(-1);

        while (cursor.moveToNext()) {
            long id = cursor.getLong(CONTACTS_SUMMARY_IDX_ID);
            String lookupKey = cursor.getString(CONTACTS_SUMMARY_IDX_LOOKUP_KEY);
            String name = cursor.getString(CONTACTS_SUMMARY_IDX_DISPLAY_NAME);
            String photoUri = cursor.getString(CONTACTS_SUMMARY_IDX_PHOTO_URI);
            int displayNameSource = cursor.getInt(CONTACTS_DISPLAY_NAME_SOURCE);
            Uri contactLookupUri = ContactsContract.Contacts.getLookupUri(id, lookupKey);

            BaseContactInfo contactInfo = new BaseContactInfo();
            contactInfo.mContactId = id;
            contactInfo.mLookupKey = lookupKey;
            contactInfo.mContactLookupUri = contactLookupUri;
            contactInfo.mDisplayName = name;
            contactInfo.mDisplayNameSource = displayNameSource;
            contactInfo.mPhotoUri = photoUri;

            result.add(contactInfo);
        }
        return result;
    }
}
