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

package com.appsimobile.appsii;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.DrawableRes;
import android.util.Log;

import com.appsimobile.appsii.permissions.PermissionUtils;
import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

/**
 * Created by nick on 14/04/15.
 */
public class BitmapUtils {

    PermissionUtils mPermissionUtils;

    Resources mResources;

    Context mContext;

    ContentResolver mContentResolver;

    @Inject
    BitmapUtils(Context context, PermissionUtils permissionUtils, Resources res,
            ContentResolver resolver) {
        mPermissionUtils = permissionUtils;
        mResources = res;
        mContext = context;
        mContentResolver = resolver;
    }

    public Bitmap decodeSampledBitmapFromResource(@DrawableRes int resId,
            int reqWidth, int reqHeight) {

        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(mResources, resId, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inScaled = false;

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeResource(mResources, resId, options);
            return ThumbnailUtils.extractThumbnail(bitmap, reqWidth, reqHeight);
        } catch (OutOfMemoryError e) {
            Crashlytics.logException(e);
            Log.w("Helper", "Out of memory while loading parallax image");
            return null;
        }
    }

    public int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public Bitmap decodeSampledBitmapFromFile(File path,
            int reqWidth, int reqHeight) {

        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream in = new FileInputStream(path);
            BitmapFactory.decodeStream(in, null, options);
            in.close();

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inScaled = false;

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            in = new FileInputStream(path);
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
            in.close();

            if (bitmap == null) return null;
            return ThumbnailUtils.extractThumbnail(bitmap, reqWidth, reqHeight);
        } catch (OutOfMemoryError e) {
            Crashlytics.logException(e);
            Log.w("Helper", "Out of memory loading custom image");
            return null;
        } catch (FileNotFoundException e) {
            Log.w("Helper", "error loading image", e);
            return null;
        } catch (IOException e) {
            Log.w("Helper", "error loading image", e);
            return null;
        }
    }

    public Bitmap decodeContactImage(Uri contactUri, int reqWidth,
            int reqHeight) throws PermissionDeniedException {

        try {
            mPermissionUtils.throwIfNotPermitted(mContext, Manifest.permission.READ_CONTACTS);

            InputStream avatarDataStream =
                    ContactsContract.Contacts.openContactPhotoInputStream(
                            mContentResolver,
                            contactUri, true);

            if (avatarDataStream == null) return null;

            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(avatarDataStream, null, options);
            avatarDataStream.close();

            avatarDataStream =
                    ContactsContract.Contacts.openContactPhotoInputStream(
                            mContentResolver,
                            contactUri, true);
            if (avatarDataStream == null) return null;

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inScaled = false;

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(avatarDataStream, null, options);
//            return Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, true);
        } catch (OutOfMemoryError e) {
            Log.wtf("Helper", "Out of memory while loading contact image; returning null", e);
            Crashlytics.logException(e);
            return null;
        } catch (SecurityException e) {
            throw new PermissionDeniedException(e);
        } catch (IOException e) {
            Log.wtf("Helper", "error loading contact image", e);
            return null;
        }
    }

    public File userImageFile(String customDrawableFileName) {
        File parentFolder = externalFilesFolder();
        return new File(parentFolder, customDrawableFileName);
    }

    public File externalFilesFolder() {
        File result = new File(Environment.getExternalStorageDirectory(), "appsii");
//        File result = new File("/sdcard/appsii");
        if (!result.exists()) {
            result.mkdirs();
        }
        return result;
    }
}
