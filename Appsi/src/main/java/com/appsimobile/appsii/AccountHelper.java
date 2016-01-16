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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.appsimobile.appsii.module.home.WeatherFragment;
import com.appsimobile.appsii.module.weather.WeatherContract;
import com.appsimobile.appsii.module.weather.WeatherLoadingService;
import com.appsimobile.appsii.preference.PreferenceHelper;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by nick on 14/04/15.
 */
@Singleton
public final class AccountHelper implements SharedPreferences.OnSharedPreferenceChangeListener {

    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = WeatherContract.AUTHORITY;

    // An account type, in the form of a domain name
    // Do not use the BuildConfig.APPLICATION_ID because we use two variants and
    // only need one account
    public static final String ACCOUNT_TYPE = "com.appsimobile.appsii";

    public static final long SECONDS_PER_MINUTE = 60L;

    public static final long SYNC_INTERVAL_IN_MINUTES = 60L;

    public static final long SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE;

    // The account name
    public static final String ACCOUNT = "Appsii";

    private final PreferenceHelper mPreferenceHelper;

    private final AccountManager mAccountManager;
    private final Context mContext;
    SharedPreferences mPrefs;
    Account mAccount;

    String mDefaultWeatherUnit;

    @Inject
    public AccountHelper(Context context, SharedPreferences preferences,
            PreferenceHelper preferenceHelper, AccountManager accountManager) {
        mContext = context;
        mPrefs = preferences;
        mPreferenceHelper = preferenceHelper;
        mAccountManager = accountManager;

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onWeatherUpdated();
            }
        };
        IntentFilter intentFilter =
                new IntentFilter(WeatherLoadingService.ACTION_WEATHER_UPDATED);
        mContext.registerReceiver(receiver, intentFilter);

        mDefaultWeatherUnit = preferenceHelper.getDefaultWeatherTemperatureUnit();
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Nullable
    public static Account getExistingAccount(AccountManager accountManager) {

        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        if (accounts.length > 0) {
            return accounts[0];
        }
        return null;
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    private static Account createSyncAccount(Context context, AccountManager accountManager) {

        // Create the account type and default account
        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (!accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
            Log.i("Appsi", "local account already exists");
        }
        return newAccount;
    }

    void onWeatherUpdated() {
        mPrefs.edit().putLong(
                WeatherLoadingService.PREFERENCE_LAST_UPDATED_MILLIS, System.currentTimeMillis()).
                apply();

    }

    public void requestSync(String woeid) {
        Log.w("AccountHelper",
                "request sync " + ContentResolver.getIsSyncable(mAccount, AUTHORITY));
        Bundle bundle = createManualSyncBundle(woeid);
        ContentResolver.requestSync(mAccount, AUTHORITY, bundle);
    }

    @NonNull
    private Bundle createManualSyncBundle(@Nullable String woeid) {
        Bundle bundle = createDefaultSyncBundle();

        if (woeid != null) {
            bundle.putString(WeatherLoadingService.EXTRA_INCLUDE_WOEID, woeid);
        }
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        return bundle;
    }

    @NonNull
    private Bundle createDefaultSyncBundle() {
        String defaultUnit = mPreferenceHelper.getDefaultWeatherTemperatureUnit();

        String unit = mPrefs.getString(WeatherFragment.PREFERENCE_WEATHER_UNIT, defaultUnit);

        Bundle bundle = new Bundle();
        bundle.putString(WeatherLoadingService.EXTRA_UNIT, unit);
        return bundle;
    }

    public void configureAutoSyncAndSync() {
        createAccountIfNeeded();

        updateSyncSettings();

        if (ContentResolver.getIsSyncable(mAccount, AUTHORITY) < 1) {
            Log.d("AccountHelper", "making account syncable...");
            ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
        }
        requestSync();
//        ContentResolver.requestSync(mAccount, AUTHORITY, Bundle.EMPTY);
    }

    public boolean createAccountIfNeeded() {
        // Get an instance of the Android account manager
        mAccount = getExistingAccount(mAccountManager);
        boolean created = mAccount == null;
        if (mAccount == null) {
            mAccount = createSyncAccount(mContext, mAccountManager);
        }

        // start of as not syncable
        ContentResolver.setIsSyncable(mAccount, AUTHORITY, -1);

        return created;
    }

    public void updateSyncSettings() {
        Bundle bundle = createDefaultSyncBundle();
        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(mAccount, AUTHORITY);
        int N = syncs.size();
        for (int i = 0; i < N; i++) {
            PeriodicSync sync = syncs.get(i);
            ContentResolver.removePeriodicSync(sync.account, sync.authority, sync.extras);
        }
        ContentResolver.addPeriodicSync(
                mAccount,
                AUTHORITY,
                bundle,
                SYNC_INTERVAL);
    }

    public void requestSync() {
        Log.w("AccountHelper",
                "request sync " + ContentResolver.getIsSyncable(mAccount, AUTHORITY));
        Bundle extras = createManualSyncBundle(null /* no woeid */);
        ContentResolver.requestSync(mAccount, AUTHORITY, extras);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getExistingAccount(mAccountManager) == null) {
            Log.d("AccountHelper", "Weather unit changed, but no account. Not updating settings…");
            return;
        }
        if (ContentResolver.getIsSyncable(mAccount, AUTHORITY) > 0) {

            if ("weather_default_unit".equals(key)) {
                String value = sharedPreferences.getString(key, "c");
                if (!TextUtils.equals(mDefaultWeatherUnit, value)) {
                    mDefaultWeatherUnit = value;
                    updateSyncSettings();
                    requestSync();
                }
            }
        } else {

            Log.d("AccountHelper", "Weather unit changed, but account not syncable…");
        }
    }
}
