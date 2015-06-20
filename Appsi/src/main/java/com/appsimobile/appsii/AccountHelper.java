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
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.appsimobile.appsii.module.weather.WeatherContract;
import com.appsimobile.appsii.module.weather.WeatherLoadingService;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by nick on 14/04/15.
 */
public class AccountHelper {

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

    private static final Lock sAccountHelperLock = new ReentrantLock();

    private static volatile AccountHelper sInstance;

    Account mAccount;

    private final Context mContext;

    public AccountHelper(Context context) {
        mContext = context;
    }

    public static AccountHelper getInstance(Context context) {
        sAccountHelperLock.lock();
        try {
            if (sInstance == null) {
                sInstance = new AccountHelper(context.getApplicationContext());
            }
        } finally {
            sAccountHelperLock.unlock();
        }
        return sInstance;
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    private static Account createSyncAccount(Context context) {

        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);


        Account[] accounts = accountManager.getAccountsByType("com.appsimobile.appsii");
        if (accounts.length > 0) {
            return accounts[0];
        }

        // Create the account type and default account
        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            ContentResolver.setIsSyncable(newAccount, AUTHORITY, 1);
        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
            Log.i("Appsi", "local account already exists");
        }
        return newAccount;
    }

    public void createAccountIfNeeded() {
        mAccount = createSyncAccount(mContext);
        /*
         * Turn on periodic syncing
         */
        ContentResolver.addPeriodicSync(
                mAccount,
                AUTHORITY,
                Bundle.EMPTY,
                SYNC_INTERVAL);
        // start of as not syncable
        ContentResolver.setIsSyncable(mAccount, AUTHORITY, -1);

    }

    public void requestSync() {
        Log.w("AccountHelper",
                "request sync " + ContentResolver.getIsSyncable(mAccount, AUTHORITY));
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(mAccount, AUTHORITY, extras);
    }

    public void requestSync(String woeid) {
        Log.w("AccountHelper",
                "request sync " + ContentResolver.getIsSyncable(mAccount, AUTHORITY));
        Bundle bundle = new Bundle();
        bundle.putString(WeatherLoadingService.EXTRA_INCLUDE_WOEID, woeid);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(mAccount, AUTHORITY, bundle);
    }

    public void configureAutoSyncAndSync() {
        createAccountIfNeeded();

        ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
        if (WeatherLoadingService.hasTimeoutExpired(mContext)) {
            ContentResolver.requestSync(mAccount, AUTHORITY, Bundle.EMPTY);
        }

    }
}
