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

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.ViewConfiguration;

import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.dagger.ApplicationComponent;
import com.appsimobile.appsii.dagger.ApplicationModule;
import com.appsimobile.appsii.dagger.DaggerApplicationComponent;
import com.appsimobile.appsii.iab.FeatureManager;
import com.appsimobile.appsii.iab.FeatureManagerFactory;
import com.appsimobile.appsii.iab.FeatureManagerHelper;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.crashlytics.android.Crashlytics;

import java.lang.reflect.Field;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;

public class AppsiApplication extends Application {

    public static final boolean DEBUG = false;

    public static final boolean API19 =
            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    public static final int APPWIDGET_HOST_ID = 0x0BADBABE;

    private static float density = -1;

    private static Bitmap mDefaultAppIcon;
    @Inject
    FeatureManagerHelper mFeatureManagerHelper;

    public static float getDensity(Context context) {
        if (density == -1) {
            density = context.getResources().getDisplayMetrics().density;
        }
        return density;

    }

    public static synchronized Bitmap getDefaultAppIcon(Context context) {
        if (mDefaultAppIcon == null) {
            Resources res = context.getResources();
            mDefaultAppIcon =
                    BitmapFactory.decodeResource(res, android.R.drawable.sym_def_app_icon);
        }
        return mDefaultAppIcon;
    }

    public static void initializeStrictMode(Context context) {

        if (BuildConfig.DEBUG) {
            if (BuildConfig.DEBUG) {
                Log.i("LauncherApplication", "Application is debuggable, enabling strictmode");
            }
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                            //.penaltyDeath()
                    .build());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initializeDagger();
        AppInjector.inject(this);

        Fabric.with(this, new Crashlytics.Builder().disabled(BuildConfig.DEBUG).build());

        verifyPurchases();

        unlockOverflowButton();
    }

    public void verifyPurchases() {
        final FeatureManager featureManager = FeatureManagerFactory.getFeatureManager(this);
        featureManager.registerFeatureManagerListener(new FeatureManager.FeatureManagerListener() {
            @Override
            public void onIabSetupFailed() {
                Log.wtf("Appsii", "Iab setup failed :-(");
                featureManager.unregisterFeatureManagerListener(this);
            }

            @Override
            public void onInventoryReady() {
                updateInventory(featureManager);
                featureManager.unregisterFeatureManagerListener(this);

            }
        });
        featureManager.load(true);

    }

    private void unlockOverflowButton() {

        // Force menu overflow. Google decided to make this the default on kitkat anyway
        // https://android.googlesource.com/platform/frameworks/base.git/+/ea04f3cfc6e245
        // fb415fd352ed0048cd940a46fe
        // We want this as it increases usability
        Log.i("Appsii", "patching menu key..");

        // if the device has a permanent menu key, 'patch' this class using reflexion
        if (ViewConfiguration.get(this).hasPermanentMenuKey()) {
            try {
                ViewConfiguration config = ViewConfiguration.get(this);
                Field menuKeyField =
                        ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
                if (menuKeyField != null) {
                    menuKeyField.setAccessible(true);
                    menuKeyField.setBoolean(config, false);
                }
                Log.i("Appsii", "patching menu key success!");
            } catch (Exception ex) {
                Log.i("Appsii", "error patching menu key", ex);
                // Ignore can't really do anything if this fails.
            }
        }
    }

    void updateInventory(FeatureManager featureManager) {
        {
            boolean agendaAccess = mFeatureManagerHelper.hasAgendaAccess(this, featureManager);
            updatePageEnabledState(HomeContract.Pages.PAGE_AGENDA, agendaAccess);
        }

        {
            boolean peopleAccess = mFeatureManagerHelper.hasPeopleAccess(this, featureManager);
            updatePageEnabledState(HomeContract.Pages.PAGE_PEOPLE, peopleAccess);
        }

        {
            boolean callsAccess = mFeatureManagerHelper.hasCallsAccess(this, featureManager);
            updatePageEnabledState(HomeContract.Pages.PAGE_CALLS, callsAccess);
        }

        {
            boolean settingsAccess = mFeatureManagerHelper.hasSettingsAccess(this, featureManager);
            updatePageEnabledState(HomeContract.Pages.PAGE_SETTINGS, settingsAccess);
        }

        {
            boolean smsAccess = mFeatureManagerHelper.hasSmsAccess(this, featureManager);
            updatePageEnabledState(HomeContract.Pages.PAGE_SMS, smsAccess);
        }
    }

    private void updatePageEnabledState(int pageType, boolean enabled) {
        if (enabled) {
            PageHelper pageHelper = PageHelper.getInstance(this);
            pageHelper.enablePageAccess(pageType, false /* force */);
        }
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask,
            int flagsValues, int extraFlags, Bundle options)
            throws IntentSender.SendIntentException {
        super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            super.unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.w("AppsiiApplication", "error unregistering receiver", e);
        }
    }

    protected void initializeDagger() {
        ApplicationComponent applicationComponent = DaggerApplicationComponent
                .builder()
                .applicationModule(new ApplicationModule(this))
                .build();
        AppInjector.setComponent(applicationComponent);
    }

}
