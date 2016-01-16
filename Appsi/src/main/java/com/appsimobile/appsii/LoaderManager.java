/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.appsimobile.appsii;

import android.app.Application;
import android.content.Loader;
import android.os.Bundle;

import com.appsimobile.appsii.annotation.VisibleForTesting;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static library support version of the framework's {@link android.app.LoaderManager}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework SDK
 * documentation for a class overview.
 * <p/>
 * <p>Your activity must derive from {@link SidebarContext} to use
 * this.
 */
public abstract class LoaderManager {

    private static final AtomicInteger sLoadingIdGenerator = new AtomicInteger();

    public static int loaderId() {
        return sLoadingIdGenerator.incrementAndGet();
    }

    /**
     * Control whether the framework's internal loader manager debugging
     * logs are turned on.  If enabled, you will see output in logcat as
     * the framework performs loader operations.
     */
    public static void enableDebugLogging(boolean enabled) {
        LoaderManagerImpl.DEBUG = enabled;
    }

    @VisibleForTesting
    public static LoaderManager createInstance(Application appsi, boolean started) {
        return new LoaderManagerImpl("xx", appsi, started);
    }

    /**
     * Ensures a loader is initialized and active.  If the loader doesn't
     * already exist, one is created and (if the activity/fragment is currently
     * started) starts the loader.  Otherwise the last created
     * loader is re-used.
     * <p/>
     * <p>In either case, the given callback is associated with the loader, and
     * will be called as the loader state changes.  If at the point of call
     * the caller is in its started state, and the requested loader
     * already exists and has generated its data, then
     * callback {@link LoaderCallbacks#onLoadFinished} will
     * be called immediately (inside of this function), so you must be prepared
     * for this to happen.
     *
     * @param id A unique identifier for this loader.  Can be whatever you want.
     * Identifiers are scoped to a particular LoaderManager instance.
     * @param args Optional arguments to supply to the loader at construction.
     * If a loader already exists (a new one does not need to be created), this
     * parameter will be ignored and the last arguments continue to be used.
     * @param callback Interface the LoaderManager will call to report about
     * changes in the state of the loader.  Required.
     */
    public abstract <D> Loader<D> initLoader(int id, Bundle args,
            LoaderCallbacks<D> callback);

    /**
     * Starts a new or restarts an existing {@link android.content.Loader} in
     * this manager, registers the callbacks to it,
     * and (if the activity/fragment is currently started) starts loading it.
     * If a loader with the same id has previously been
     * started it will automatically be destroyed when the new loader completes
     * its work. The callback will be delivered before the old loader
     * is destroyed.
     *
     * @param id A unique identifier for this loader.  Can be whatever you want.
     * Identifiers are scoped to a particular LoaderManager instance.
     * @param args Optional arguments to supply to the loader at construction.
     * @param callback Interface the LoaderManager will call to report about
     * changes in the state of the loader.  Required.
     */
    public abstract <D> Loader<D> restartLoader(int id, Bundle args,
            LoaderCallbacks<D> callback);

    /**
     * Stops and removes the loader with the given ID.  If this loader
     * had previously reported data to the client through
     * {@link LoaderCallbacks#onLoadFinished(android.content.Loader, Object)}, a call
     * will be made to {@link LoaderManager.LoaderCallbacks#onLoaderReset(android.content.Loader)}.
     */
    public abstract void destroyLoader(int id);

    /**
     * Return the Loader with the given id or null if no matching Loader
     * is found.
     */
    public abstract <D> Loader<D> getLoader(int id);

    /**
     * Print the LoaderManager's state into the given stream.
     *
     * @param prefix Text to print at the front of each line.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer A PrintWriter to which the dump is to be set.
     * @param args Additional arguments to the dump request.
     */
    public abstract void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args);

    /**
     * Returns true if any loaders managed are currently running and have not
     * returned data to the application yet.
     */
    public boolean hasRunningLoaders() {
        return false;
    }

    /**
     * Callback interface for a client to interact with the manager.
     */
    public interface LoaderCallbacks<D> {

        /**
         * Instantiate and return a new Loader for the given ID.
         *
         * @param id The ID whose loader is to be created.
         * @param args Any arguments supplied by the caller.
         *
         * @return Return a new Loader instance that is ready to start loading.
         */
        Loader<D> onCreateLoader(int id, Bundle args);

        /**
         * Called when a previously created loader has finished its load.  Note
         * that normally an application is <em>not</em> allowed to commit fragment
         * transactions while in this call, since it can happen after an
         * activity's state is saved.
         * <p/>
         * <p>This function is guaranteed to be called prior to the release of
         * the last data that was supplied for this Loader.  At this point
         * you should remove all use of the old data (since it will be released
         * soon), but should not do your own release of the data since its Loader
         * owns it and will take care of that.  The Loader will take care of
         * management of its data so you don't have to.  In particular:
         * <p/>
         * <ul>
         * <li> <p>The Loader will monitor for changes to the data, and report
         * them to you through new calls here.  You should not monitor the
         * data yourself.  For example, if the data is a {@link android.database.Cursor}
         * and you place it in a {@link android.widget.CursorAdapter}, use
         * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
         * android.database.Cursor, int)} constructor <em>without</em> passing
         * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
         * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
         * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
         * from doing its own observing of the Cursor, which is not needed since
         * when a change happens you will get a new Cursor throw another call
         * here.
         * <li> The Loader will release the data once it knows the application
         * is no longer using it.  For example, if the data is
         * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
         * you should not call close() on it yourself.  If the Cursor is being placed in a
         * {@link android.widget.CursorAdapter}, you should use the
         * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
         * method so that the old Cursor is not closed.
         * </ul>
         *
         * @param loader The Loader that has finished.
         * @param data The data generated by the Loader.
         */
        void onLoadFinished(Loader<D> loader, D data);

        /**
         * Called when a previously created loader is being reset, and thus
         * making its data unavailable.  The application should at this point
         * remove any references it has to the Loader's data.
         *
         * @param loader The Loader that is being reset.
         */
        void onLoaderReset(Loader<D> loader);
    }
}

