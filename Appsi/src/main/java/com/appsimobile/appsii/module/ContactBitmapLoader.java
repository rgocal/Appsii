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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by nick on 29/05/14.
 */
public class ContactBitmapLoader extends AsyncTask<Void, Void, Bitmap> {

    private final Uri mUri;

    private final WeakReference<Context> mContext;

    private final String mPhotoUri;

    private final long mContactId;

    private final int mMinDimen;

    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    public ContactBitmapLoader(Context context, long contactId, Uri lookupUri, String photoUri,
            int minDimen) {
        mUri = lookupUri;
        mContext = new WeakReference<>(context);
        mPhotoUri = photoUri;
        mContactId = contactId;
        mMinDimen = minDimen;
    }

    private static Bitmap loadContactBitmap(Context context, Uri lookupUri, long contactId,
            String photoUri, int minDimen) {

        Bitmap result = loadFromContentProvider(context, lookupUri);

        if (result != null && result.getWidth() > minDimen) {
            return result;
        }
        if (photoUri == null) return null;

        Uri uri = Uri.parse(photoUri);
        String scheme = uri.getScheme();
        InputStream in;
        AssetFileDescriptor fd;
        byte[] bytes = null;

        boolean loadedExternally = false;
        try {
            if ("http".equals(scheme) || "https".equals(scheme)) {
                // Support HTTP urls that might come from extended directories
                in = new URL(photoUri).openStream();
                fd = null;
                loadedExternally = true;
            } else {
                fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                in = fd.createInputStream();
            }
            byte[] buffer = new byte[16 * 1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                int size;
                while ((size = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, size);
                }
                bytes = baos.toByteArray();
            } finally {
                in.close();
                if (fd != null) {
                    fd.close();
                }
            }

        } catch (IOException e) {
            Log.e("ContactBitmapLoader", "error", e);
        }

        if (bytes != null && loadedExternally) {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            ops.add(ContentProviderOperation
                    .newUpdate(
                            ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                            ContactsContract.Data.CONTACT_ID
                                    + " = ? AND "
                                    + ContactsContract.Data.MIMETYPE
                                    + " = ?",
                            new String[]{
                                    String.valueOf(contactId),
                                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE}
                    )
                    .withValue(ContactsContract.Contacts.Photo.PHOTO, bytes).build());
            try {
                context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (RemoteException | OperationApplicationException e) {
                Log.e("ContactBitmapLoader", "error updating provider", e);
            }
        }
        if (bytes != null) {
            result = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

        //return BitmapFactory.decodeResource(context.getResources(),
        // R.drawable.ic_contact_picture_holo_dark);
        return result;
    }

    private static Bitmap loadFromContentProvider(Context context, Uri lookupUri) {
        InputStream in = openContactPhotoInputStream(context, lookupUri);
        if (in != null) {
            try {
                return BitmapFactory.decodeStream(in);
            } catch (OutOfMemoryError e) {
                FirebaseCrash.report(e);
                return null;
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static InputStream openContactPhotoInputStream(Context context, Uri lookupUri) {
        try {
            return ContactsContract.Contacts
                    .openContactPhotoInputStream(context.getContentResolver(), lookupUri, true);
            // There was a bug report #152 in crashlytics a crash in the method called
        } catch (NullPointerException e) {
            FirebaseCrash.report(e);
            return null;
        }
    }

    public void enqueue() {
        executeOnExecutor(mExecutorService);
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        Context context = mContext.get();
        if (context != null) {
            PeopleCache peopleCache = PeopleCache.getInstance();
            if (peopleCache.isKnownNoBitmap(mUri)) return null;

            Bitmap result = peopleCache.getBitmap(mUri);
            if (result == null) {
                result = loadContactBitmap(context, mUri, mContactId, mPhotoUri, mMinDimen);
                peopleCache.addToCache(mUri, result);
            }
            return result;
        }
        return null;
    }
}
