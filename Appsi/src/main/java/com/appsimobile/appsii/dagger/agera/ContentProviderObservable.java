package com.appsimobile.appsii.dagger.agera;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.appsimobile.appsii.AppsiApplication;
import com.google.android.agera.ActivationHandler;
import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;
import com.google.android.agera.UpdateDispatcher;

import javax.inject.Inject;

import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Preconditions.checkNotNull;

/**
 * Created by nmartens on 23/04/16.
 */
public final class ContentProviderObservable implements ActivationHandler, Observable {

    @NonNull
    private final UpdateDispatcher updateDispatcher;

    @NonNull
    private final Context context;

    @Inject
    ContentResolver mContentResolver;

    ContentObserver mContentObserver;

    Uri[] mUris;

    public ContentProviderObservable(@NonNull final Context applicationContext,
            @NonNull final Uri... uris) {
        this.context = checkNotNull(applicationContext);
        this.updateDispatcher = updateDispatcher(this);
        mContentResolver = applicationContext.getContentResolver();

        AppsiApplication app = (AppsiApplication) applicationContext.getApplicationContext();
        app.getComponent().inject(this);

        mUris = uris;

        mContentObserver = new ContentObserver(new Handler(Looper.myLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                updateDispatcher.update();
            }
        };

    }

    @Override
    public void observableActivated(@NonNull final UpdateDispatcher caller) {
        for (Uri uri : mUris) {
            mContentResolver.registerContentObserver(uri, true, mContentObserver);
        }

    }

    @Override
    public void observableDeactivated(@NonNull final UpdateDispatcher caller) {
        for (Uri uri : mUris) {
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }

    @Override
    public void addUpdatable(@NonNull final Updatable updatable) {
        updateDispatcher.addUpdatable(updatable);
    }

    @Override
    public void removeUpdatable(@NonNull final Updatable updatable) {
        updateDispatcher.removeUpdatable(updatable);
    }
}
