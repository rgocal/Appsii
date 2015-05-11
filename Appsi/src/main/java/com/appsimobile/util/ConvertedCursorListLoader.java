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

package com.appsimobile.util;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.support.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Implements a AsyncCursorLoader that uses a query (like the CursorLoader)
 * that is converted into a result-list instead of returning a plain cursor.
 * <p/>
 * Most of the contents of this class is copied from CursorLoader.java
 * <p/>
 * Created by nick on 22/09/14.
 */
public abstract class ConvertedCursorListLoader<T> extends AsyncTaskLoader<List<T>> {

    final ForceLoadContentObserver mObserver;

    Uri mUri;

    String[] mProjection;

    String mSelection;

    String[] mSelectionArgs;

    String mSortOrder;

    List<T> mCursor;

    CancellationSignal mCancellationSignal;

    /**
     * Creates an empty unspecified CursorLoader.  You must follow this with
     * calls to {@link #setUri(Uri)}, {@link #setSelection(String)}, etc
     * to specify the query to perform.
     */
    public ConvertedCursorListLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    @Override
    public void onCanceled(List<T> cursor) {
        cleanup(cursor);
    }

    /* Runs on a worker thread */
    @Override
    public List<T> loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mCancellationSignal = new CancellationSignal();
        }
        try {
            Cursor cursor = getContext().getContentResolver().query(mUri, mProjection, mSelection,
                    mSelectionArgs, mSortOrder, mCancellationSignal);

            if (cursor != null) {
                try {
                    // Ensure the cursor window is filled.
                    cursor.getCount();
                } catch (RuntimeException ex) {
                    cursor.close();
                    throw ex;
                }
                List<T> result = convertCursor(cursor);
                cursor.close();
                return result;
            }
            return null;
        } finally {
            synchronized (this) {
                mCancellationSignal = null;
            }
        }
    }

    protected abstract List<T> convertCursor(@NonNull Cursor cursor);    /* Runs on the UI thread */

    @Override
    public void deliverResult(List<T> cursor) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            return;
        }
        List<T> oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != cursor) {
            cleanup(oldCursor);
        }
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();

        synchronized (this) {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }
    }

    protected abstract void cleanup(List<T> old);

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix);
        writer.print("mUri=");
        writer.println(mUri);
        writer.print(prefix);
        writer.print("mProjection=");
        writer.println(Arrays.toString(mProjection));
        writer.print(prefix);
        writer.print("mSelection=");
        writer.println(mSelection);
        writer.print(prefix);
        writer.print("mSelectionArgs=");
        writer.println(Arrays.toString(mSelectionArgs));
        writer.print(prefix);
        writer.print("mSortOrder=");
        writer.println(mSortOrder);
        writer.print(prefix);
        writer.print("mCursor=");
        writer.println(mCursor);
    }

    public Uri getUri() {
        return mUri;
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     * <p/>
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    public void setUri(Uri uri) {
        unregisterReceiver();
        mUri = uri;
        registerReceiver();
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    private void unregisterReceiver() {
        if (mUri != null) {
            getContext().getContentResolver().unregisterContentObserver(mObserver);
        }
    }

    private void registerReceiver() {
        if (mUri != null) {
            getContext().getContentResolver().registerContentObserver(mUri, true, mObserver);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        cleanup(mCursor);
        mCursor = null;
    }

    public String[] getProjection() {
        return mProjection;
    }

    public void setProjection(String[] projection) {
        mProjection = projection;
    }

    public String getSelection() {
        return mSelection;
    }

    public void setSelection(String selection) {
        mSelection = selection;
    }

    public String[] getSelectionArgs() {
        return mSelectionArgs;
    }

    public void setSelectionArgs(String[] selectionArgs) {
        mSelectionArgs = selectionArgs;
    }

    public String getSortOrder() {
        return mSortOrder;
    }

    public void setSortOrder(String sortOrder) {
        mSortOrder = sortOrder;
    }


}
