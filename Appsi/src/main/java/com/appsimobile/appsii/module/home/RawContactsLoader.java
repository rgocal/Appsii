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

package com.appsimobile.appsii.module.home;

import android.content.AsyncTaskLoader;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * A loader that can load very basic contact info.
 */
class RawContactsLoader extends AsyncTaskLoader<Contact> {

    static final Set<Long> sNotifiedRawContactIds = new HashSet<>();

    Contact mContact;

    ForceLoadContentObserver mObserver;

    long mId;

    String mLookupKey;


    public RawContactsLoader(Context context, String lookupKey, long id) {
        super(context);
        mId = id;
        mLookupKey = lookupKey;
    }

    public static Contact loadContact(Context context, long id, String lookupKey) {

        if (lookupKey == null) return null;

        Uri uri = ContactsContract.Contacts.getLookupUri(id, lookupKey);

        Cursor c = context.getContentResolver().query(
                uri,
                new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.PHOTO_FILE_ID,
                        ContactsContract.Contacts.PHOTO_URI,
                        ContactsContract.Contacts.LOOKUP_KEY,
                },
                null,
                null,
                null);

        if (c != null && c.moveToNext()) {
            Contact contact = new Contact();
            contact.mId = c.getLong(0);
            contact.mPhotoId = c.getLong(1);
            contact.mPhotoUri = c.getString(2);
            contact.mLookupKey = c.getString(3);
            loadPhotoBinaryData(context, contact);

            c.close();
            loadRawContacts(context, contact);

            postViewNotificationToSyncAdapter(context, contact);

            return contact;
        }
        return null;

    }

    /**
     * Looks for the photo data item in entities. If found, creates a new Bitmap instance. If
     * not found, returns null
     */
    private static void loadPhotoBinaryData(Context context, Contact contactData) {
        // If we have a photo URI, try loading that first.
        String photoUri = contactData.mPhotoUri;
        if (photoUri != null) {
            try {
                final InputStream inputStream;
                final AssetFileDescriptor fd;
                final Uri uri = Uri.parse(photoUri);
                final String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    // Support HTTP urls that might come from extended directories
                    inputStream = new URL(photoUri).openStream();
                    fd = null;
                } else {
                    fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                    inputStream = fd.createInputStream();
                }
                byte[] buffer = new byte[16 * 1024];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    int size;
                    while ((size = inputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, size);
                    }
                    byte[] data = baos.toByteArray();
                    contactData.mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                } finally {
                    inputStream.close();
                    if (fd != null) {
                        fd.close();
                    }
                }
                return;
            } catch (IOException ioe) {
                // Just fall back to the case below.
            }
        }

        // If we couldn't load from a file, fall back to the data blob.
        final long photoId = contactData.mPhotoId;
        if (photoId <= 0) {
            // No photo ID
            return;
        }

        try {
            AssetFileDescriptor assetFileDescriptor = openDisplayPhoto(context, photoId);
            if (assetFileDescriptor != null) {
                InputStream in = assetFileDescriptor.createInputStream();
                try {
                    contactData.mBitmap = BitmapFactory.decodeStream(in);
                } finally {
                    in.close();
                    assetFileDescriptor.close();
                }
            }
        } catch (IOException e) {
            Log.wtf("ProfileImage", "Error loading image", e);
        }
    }

    private static void loadRawContacts(Context context, Contact contact) {
        Cursor c = context.getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts.ACCOUNT_TYPE,
                        ContactsContract.RawContacts._ID,
                        ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
                },
                ContactsContract.RawContacts.CONTACT_ID + "=?",
                new String[]{String.valueOf(contact.mId)},
                null);
        while (c.moveToNext()) {

            RawContact rawContact = new RawContact();
            rawContact.mAccountType = c.getString(0);
            rawContact.mId = c.getLong(1);
            contact.mRawContacts.add(rawContact);
        }
        c.close();
    }

    /**
     * Posts a message to the contributing sync adapters that have opted-in, notifying them
     * that the contact has just been loaded
     */
    private static void postViewNotificationToSyncAdapter(Context context, Contact contact) {
        for (RawContact rawContact : contact.mRawContacts) {
            final long rawContactId = rawContact.mId;
            if (sNotifiedRawContactIds.contains(rawContactId)) {
                continue; // Already notified for this raw contact.
            }
            sNotifiedRawContactIds.add(rawContactId);

            if (!rawContact.isGoogleAccount()) continue;

            final String serviceName = contact.getViewContactNotifyServiceClassName();
            final String servicePackageName = contact.getViewContactNotifyServicePackageName();
            final String serviceName2 = contact.getViewContactNotifyServiceClassName2();
            final String servicePackageName2 = contact.getViewContactNotifyServicePackageName2();

            sendViewIntent(context, rawContactId, serviceName, servicePackageName);

            sendViewIntent(context, rawContactId, serviceName2, servicePackageName2);
        }
    }

    public static AssetFileDescriptor openDisplayPhoto(Context context, long photoFileId) {
        Uri displayPhotoUri =
                ContentUris.withAppendedId(ContactsContract.DisplayPhoto.CONTENT_URI, photoFileId);
        try {
            AssetFileDescriptor fd = context.getContentResolver().openAssetFileDescriptor(
                    displayPhotoUri, "r");
            return fd;
        } catch (IOException e) {
            return null;
        }
    }

    private static void sendViewIntent(Context context, long rawContactId, String serviceName,
            String servicePackageName) {
        if (!TextUtils.isEmpty(serviceName) && !TextUtils.isEmpty(servicePackageName)) {
            final Uri uri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI,
                    rawContactId);
            final Intent intent = new Intent();
            intent.setClassName(servicePackageName, serviceName);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, ContactsContract.RawContacts.CONTENT_ITEM_TYPE);
            try {
                context.startService(intent);
            } catch (Exception e) {
                Log.e("ProfileImageFragment", "Error sending message to source-app", e);
            }
        }
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(Contact apps) {
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(apps);
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override
    public Contact loadInBackground() {
        return loadContact(getContext(), mId, mLookupKey);
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(Contact apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(Contact apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        Contact oldApps = mContact;
        mContact = apps;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(apps);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }


    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mContact != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mContact);
        }

        // Start watching for changes in the app data.
        if (mObserver == null) {
            mObserver = new ForceLoadContentObserver();
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, mId);
            getContext().getContentResolver().registerContentObserver(uri, true, mObserver);
        }

        if (takeContentChanged() || mContact == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }


    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }


    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (mContact != null) {
            onReleaseResources(mContact);
            mContact = null;
        }

        // Stop monitoring for changes.
        if (mObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }
    }


}
