package com.appsimobile.appsii.dagger;

import android.content.ContentResolver;
import android.content.Context;

import com.appsimobile.appsii.AppsiApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by nmartens on 23/04/16.
 */
@Singleton
@Module
public class MainModule {

    AppsiApplication mApplication;

    public MainModule(AppsiApplication app) {
        mApplication = app;
    }

    @Singleton
    @Provides
    public AppsiApplication provideApplication() {
        return mApplication;
    }

    @Singleton
    @Provides
    public Context provideContext() {
        return mApplication;
    }

    @Provides
    public ContentResolver provideContentResolver() {
        return mApplication.getContentResolver();
    }


}
