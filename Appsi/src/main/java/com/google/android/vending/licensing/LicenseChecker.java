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

package com.google.android.vending.licensing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.util.Log;

import com.android.vending.licensing.ILicenseResultListener;
import com.android.vending.licensing.ILicensingService;
import com.google.android.vending.licensing.util.Base64;
import com.google.android.vending.licensing.util.Base64DecoderException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that should be copied into each of the Appsi-Plugins. This class
 * can perform the call to the LicenseChecker.
 * This is a very much simplified version of the license-checker that runs
 * synchronously.
 */
public class LicenseChecker {

    private static final String TAG = "LicenseChecker";

    private static final String KEY_FACTORY_ALGORITHM = "RSA";

    // Timeout value (in milliseconds) for calls to service.
    private static final int TIMEOUT_MS = 8 * 1000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final boolean DEBUG_LICENSE_ERROR = true;

    private final Context mContext;

    private final Policy mPolicy;

    private final String mPackageName;

    private final String mVersionCode;

    private PublicKey mPublicKey;

    /**
     * A handler for running tasks on a background thread. We don't want license
     * processing to block the UI thread.
     */
    private Handler mHandler;


    /**
     * @param context a Context
     * @param policy implementation of Policy
     * @param encodedPublicKey Base64-encoded RSA public key
     *
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    public LicenseChecker(Context context, String packageName, Policy policy,
            String encodedPublicKey) {
        mContext = context;
        mPolicy = policy;
        mPublicKey = generatePublicKey(encodedPublicKey);
        mPackageName = packageName;
        mVersionCode = getVersionCode(context, mPackageName);

        HandlerThread handlerThread = new HandlerThread("background thread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * Generates a PublicKey instance from a string containing the
     * Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     *
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    private static PublicKey generatePublicKey(String encodedPublicKey) {
        try {
            byte[] decodedKey = Base64.decode(encodedPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);

            return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (NoSuchAlgorithmException e) {
            // This won't happen in an Android-compatible environment.
            throw new RuntimeException(e);
        } catch (Base64DecoderException e) {
            Log.e(TAG, "Could not decode from Base64.");
            throw new IllegalArgumentException(e);
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "Invalid key specification.");
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Get version code for the application package name.
     *
     * @param packageName application package name
     *
     * @return the version code or empty string if package not found
     */
    private static String getVersionCode(Context context, String packageName) {
        try {
            return String.valueOf(context.getPackageManager().getPackageInfo(packageName, 0).
                    versionCode);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Package not found. could not get version code.");
            return "";
        }
    }

    public int checkLicense() throws InterruptedException, RemoteException {
        LicensingConnection service = connectSync();

        LicenseValidator validator =
                new LicenseValidator(generateNonce(), mPackageName, mVersionCode);


        Log.i(TAG, "Calling checkLicense on service for " + validator.getPackageName());
        ResultListener listener = new ResultListener(validator);
        service.mLicensingService.checkLicense(
                validator.getNonce(), validator.getPackageName(),
                listener);

        int result = listener.waitForResult();

        try {
            mContext.unbindService(service.mServiceConnection);
        } catch (IllegalArgumentException e) {
            // Somehow we've already been unbound. This is a non-fatal
            // error.
            Log.e(TAG, "Unable to unbind from licensing service (already unbound)");
        }

        mHandler.getLooper().quit();

        return result;
    }

    private LicensingConnection connectSync() throws InterruptedException {


        ensureNotOnMainThread(mContext);
        final BlockingQueue<ILicensingService> q = new LinkedBlockingQueue<>(1);
        ServiceConnection connection = new ServiceConnection() {
            volatile boolean mConnectedAtLeastOnce = false;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (!mConnectedAtLeastOnce) {
                    mConnectedAtLeastOnce = true;
                    try {
                        q.put(ILicensingService.Stub.asInterface(service));
                    } catch (InterruptedException e) {
                        // will never happen, since the queue starts with one available slot
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        try {
            Intent intent = new Intent(new String(Base64.decode(
                    "Y29tLmFuZHJvaWQudmVuZGluZy5saWNlbnNpbmcuSUxpY2Vuc2luZ1NlcnZpY2U=")));

            boolean isBound = mContext.bindService(intent,
                    connection,
                    Context.BIND_AUTO_CREATE);
            if (!isBound) {
                throw new AssertionError("could not bind to LicensingService");
            }
            return new LicensingConnection(connection, q.take());
        } catch (Base64DecoderException e) {
            return null;
        }
    }

    /**
     * Generates a nonce (number used once).
     */
    private int generateNonce() {
        return RANDOM.nextInt();
    }

    private static void ensureNotOnMainThread(Context context) {
        Looper looper = Looper.myLooper();
        if (looper != null && looper == context.getMainLooper()) {
            throw new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
        }
    }

    public boolean isLooperRunning() {
        return mHandler.sendEmptyMessage(444);
    }

    class LicensingConnection {

        ServiceConnection mServiceConnection;

        ILicensingService mLicensingService;

        public LicensingConnection(ServiceConnection connection,
                ILicensingService licensingService) {
            mServiceConnection = connection;
            mLicensingService = licensingService;
        }
    }


    private class ResultListener extends ILicenseResultListener.Stub {

        private static final int ERROR_CONTACTING_SERVER = 0x101;

        private static final int ERROR_INVALID_PACKAGE_NAME = 0x102;

        private static final int ERROR_NON_MATCHING_UID = 0x103;

        final CountDownLatch mCountDownLatch = new CountDownLatch(1);

        final AtomicBoolean mTimedOut = new AtomicBoolean();

        final AtomicInteger mResult = new AtomicInteger(Policy.NOT_LICENSED);

        private final LicenseValidator mValidator;

        private Runnable mOnTimeout;

        public ResultListener(LicenseValidator validator) {
            mValidator = validator;
            mOnTimeout = new Runnable() {
                public void run() {
                    Log.i(TAG, "Check timed out.");
                    mTimedOut.set(true);
                    mCountDownLatch.countDown();
                }
            };
            startTimeout();
        }

        private void startTimeout() {
            Log.i(TAG, "Start monitoring timeout.");
            mHandler.postDelayed(mOnTimeout, TIMEOUT_MS);
        }

        // Runs in IPC thread pool. Post it to the Handler, so we can guarantee
        // either this or the timeout runs.
        public void verifyLicense(final int responseCode, final String signedData,
                final String signature) {
            mHandler.post(new Runnable() {
                public void run() {
                    try {
                        Log.i(TAG, "Received response.");
                        // Make sure it hasn't already timed out.
                        if (!mTimedOut.get()) {
                            clearTimeout();
                            mValidator.verify(mPublicKey, responseCode, signedData, signature);
                        }
                        if (DEBUG_LICENSE_ERROR) {
                            boolean logResponse;
                            String stringError = null;
                            switch (responseCode) {
                                case ERROR_CONTACTING_SERVER:
                                    logResponse = true;
                                    stringError = "ERROR_CONTACTING_SERVER";
                                    break;
                                case ERROR_INVALID_PACKAGE_NAME:
                                    logResponse = true;
                                    stringError = "ERROR_INVALID_PACKAGE_NAME";
                                    break;
                                case ERROR_NON_MATCHING_UID:
                                    logResponse = true;
                                    stringError = "ERROR_NON_MATCHING_UID";
                                    break;
                                default:
                                    logResponse = false;
                            }

                            if (logResponse) {
                                String android_id = Secure.getString(mContext.getContentResolver(),
                                        Secure.ANDROID_ID);
                                Date date = new Date();
                                Log.d(TAG, "Server Failure: " + stringError);
                                Log.d(TAG, "Android ID: " + android_id);
                                Log.d(TAG, "Time: " + date.toGMTString());
                            }
                        }
                    } finally {
                        mCountDownLatch.countDown();
                    }
                }
            });
        }

        private void clearTimeout() {
            Log.i(TAG, "Clearing timeout.");
            mHandler.removeCallbacks(mOnTimeout);
        }

        public int waitForResult() throws InterruptedException {
            mCountDownLatch.await();
            return mResult.get();
        }
    }
}
