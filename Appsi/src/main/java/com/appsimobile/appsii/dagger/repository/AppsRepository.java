package com.appsimobile.appsii.dagger.repository;

import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.dagger.AppsModule;
import com.appsimobile.appsii.dagger.agera.BroadcastObservable;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by nmartens on 23/04/16.
 */
public class AppsRepository {

    @Named(AppsModule.NAME_APPS)
    @Inject
    BroadcastObservable mPackageObserver;

    public AppsRepository(AppsiApplication app) {
        app.getComponent().inject(this);
    }

}
