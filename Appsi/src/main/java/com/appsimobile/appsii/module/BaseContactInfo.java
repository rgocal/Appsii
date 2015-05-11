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

import android.net.Uri;
import android.provider.ContactsContract;

import com.appsimobile.appsii.module.avatar.AvatarBuilder;

import java.util.List;

/**
 * Created by nick on 07/06/14.
 */
public class BaseContactInfo {

    public String mDisplayName;

    public String mLookupKey;

    public int mDisplayNameSource;

    public long mContactId;

    public Uri mContactLookupUri;

    public boolean mStarred;

    public String mPhotoUri;

    public List<TypedPhoneNumber> mPhoneNumbers;

    private AvatarBuilder.DefaultImageRequest mDefaultImageRequest;

    private String mFirstLetter;

    public AvatarBuilder.DefaultImageRequest getDefaultImageRequest() {
        if (mDefaultImageRequest == null) {
            boolean org = mDisplayNameSource == ContactsContract.DisplayNameSources.ORGANIZATION;
            mDefaultImageRequest =
                    AvatarBuilder.createDefaultImageRequest(org, mLookupKey, mDisplayName);
        }
        return mDefaultImageRequest;
    }

    public String getFirstLetter() {
        if (mDisplayName == null || mDisplayName.length() == 0) return null;
        if (mFirstLetter == null) {
            mFirstLetter = mDisplayName.substring(0, 1).toUpperCase();
        }
        return mFirstLetter;
    }

    public static class TypedPhoneNumber {

        public String mNumber;

        public CharSequence mTypeLabel;
    }

}
