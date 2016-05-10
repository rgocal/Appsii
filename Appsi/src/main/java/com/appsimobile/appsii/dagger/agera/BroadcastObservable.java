package com.appsimobile.appsii.dagger.agera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.google.android.agera.ActivationHandler;
import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;
import com.google.android.agera.UpdateDispatcher;

import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Preconditions.checkNotNull;

/**
 * Created by nmartens on 23/04/16.
 */
public final class BroadcastObservable extends BroadcastReceiver
        implements ActivationHandler, Observable {

    @NonNull
    private final UpdateDispatcher updateDispatcher;

    @NonNull
    private final Context context;

    @NonNull
    private final IntentFilter filter;

    public BroadcastObservable(@NonNull final Context applicationContext,
            @NonNull final IntentFilter intentFilter) {
        this.context = checkNotNull(applicationContext);
        this.updateDispatcher = updateDispatcher(this);
        this.filter = intentFilter;
    }

    @Override
    public void observableActivated(@NonNull final UpdateDispatcher caller) {
        context.registerReceiver(this, filter);
    }

    @Override
    public void observableDeactivated(@NonNull final UpdateDispatcher caller) {
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        updateDispatcher.update();
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
