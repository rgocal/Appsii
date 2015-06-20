/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.appsimobile.appsii.promo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.unlock.IAppsiPlugin;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * A helper class that can be used to connect to a Appsi Plugin.
 * use {@link #connectToService()} for this. The returned Object
 * can query the plugin's licensing status. However, before that
 * can be done it must first be connected to the Plugin.
 * <p/>
 * You can use {@link AppsiPluginChecker#waitForConnection()}
 * to wait for this. Querying the plugin can be done with
 * {@link AppsiPluginChecker#checkLicense()} this call will block
 * while the plugin performs the verification.
 */
public class PluginConnectionHelper {

    static final String TAG = "RpcConnectionHelper";

    private static final SecureRandom RANDOM = new SecureRandom();

    final String mSalt;

    final int mNonce;

    final ServiceConnectionImpl mServiceConnection;

    private final Context mContext;

    private final String mPackageName;

    boolean mConnected;

    IAppsiPlugin mService;

    /**
     * @param context a Context
     *
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    public PluginConnectionHelper(Context context, String packageName) {
        mContext = context;
        mPackageName = packageName;
        mNonce = generateNonce();
        mSalt = UUID.randomUUID().toString();
        mServiceConnection = new ServiceConnectionImpl();
    }

    /**
     * Generates a nonce (number used once).
     */
    private int generateNonce() {
        return RANDOM.nextInt();
    }

    public boolean isInstalledAndSignedCorrectly() {
        // Besides checking that is is installed, also check that
        // the signatures match

        return mContext.getPackageManager().checkSignatures(
                BuildConfig.APPLICATION_ID, mPackageName) == PackageManager.SIGNATURE_MATCH;
    }

    /**
     * Start the process of connecting to the plugin. This just tries to bind to the service.
     * It returns null if the package is not installed.
     * <p/>
     * It will throw an IllegalStateException if the service is already bound.
     */
    @Nullable
    public synchronized AppsiPluginChecker connectToService() {
        if (mConnected) throw new IllegalStateException("Connect can only be used once");
        mConnected = true;
        if (!isInstalled()) return null;

        Intent intent = new Intent();
        intent.setClassName(mPackageName, mPackageName + ".Appsii");
        try {
            boolean bindResult = mContext
                    .bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

            if (!bindResult) {
                Log.e(TAG, "Could not bind to service.");
                return null;
            }
        } catch (SecurityException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Could not bind to service (2).", e);
            }
            Log.e(TAG, "Could not bind to service (2).");
            return null;
        }
        return mServiceConnection;
    }

    public boolean isInstalled() {
        try {
            mContext.getPackageManager().getApplicationInfo(mPackageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Inform the library that the context is about to be destroyed, so that any
     * open connections can be cleaned up.
     * <p/>
     * Failure to call this method can result in a crash under certain
     * circumstances, such as during screen rotation if an Activity requests the
     * license check or when the user exits the application.
     */
    public synchronized void onDestroy() {
        cleanupService();
    }

    /**
     * Unbinds service if necessary and removes reference to it.
     */
    void cleanupService() {
        if (mService != null) {
            try {
                mContext.unbindService(mServiceConnection);
            } catch (IllegalArgumentException e) {
                // Somehow we've already been unbound. This is a non-fatal
                // error.
                Log.e(TAG, "Unable to unbind from licensing service (already unbound)");
            }
            mService = null;
        }
    }

    public interface AppsiPluginChecker {

        boolean waitForConnection() throws InterruptedException;

        Bundle checkLicense() throws RemoteException;
    }

    private final class ServiceConnectionImpl implements ServiceConnection, AppsiPluginChecker {

        final CountDownLatch mCountDownLatch = new CountDownLatch(1);

        volatile boolean mConnected;

        ServiceConnectionImpl() {
        }

        public boolean waitForConnection() throws InterruptedException {
            mCountDownLatch.await();
            return mConnected;
        }

        public Bundle checkLicense() throws RemoteException {
            try {
                Bundle bundle = new Bundle();
                bundle.putInt("nonce", mNonce);
                boolean valid = isInstalledAndSignedCorrectly();
                // even if the package is invalid, always perform the validation
                // this is confusion to say the least.
                Bundle result = mService.verifyLicense(bundle, mSalt);
                if (valid) return result;
                return null;
            } finally {
                cleanupService();
            }
        }

        public synchronized void onServiceConnected(ComponentName name, IBinder service) {
            mService = IAppsiPlugin.Stub.asInterface(service);
            mConnected = true;
            mCountDownLatch.countDown();
        }

        public synchronized void onServiceDisconnected(ComponentName name) {
            mCountDownLatch.countDown();
            mConnected = false;
            // Called when the connection with the service has been
            // unexpectedly disconnected. That is, Market crashed.
            // If there are any checks in progress, the timeouts will handle them.
            Log.w(TAG, "Service unexpectedly disconnected.");
            mService = null;
        }
    }
}
