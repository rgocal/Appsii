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

package com.appsimobile.appsii.promo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.preference.ObfuscatedPreferences;
import com.appsimobile.appsii.preference.PreferencesFactory;
import com.google.android.vending.licensing.LicenseValidator;
import com.google.android.vending.licensing.util.Base64;
import com.google.android.vending.licensing.util.Base64DecoderException;

import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by nick on 06/02/15.
 */
abstract class LicenseChecker {

    public static final String RESPONSE_CODE = "0";

    public static final String SIGNED_DATA = "1";

    public static final String SIGNATURE = "2";

    public static final boolean DEBUG = BuildConfig.TEST_PURCHASES;

    final String mFeatureKey;

    final String mFeature;

    private final String mKey;

    private final String mPackageName;

    private final int mNonce;

    PluginConnectionHelper mPluginConnectionHelper;

    Activity mContext;

    String mVersionCode;

    AsyncTask<Void, Void, Bundle> mTask;

    LicenseChecker(Activity context, String packageName, String key, String feature) {

        mFeature = feature;
        mFeatureKey = "unlocked" + mFeature;
        // the key is already suffixed with 0
        mKey = key;
        mContext = context;
        mPackageName = packageName;
        mPluginConnectionHelper = new PluginConnectionHelper(
                context,
                mPackageName);
        mNonce = mPluginConnectionHelper.mNonce;
        mVersionCode = getVersionCode(context, packageName);
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
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Appsii", "Package not found. could not get version code.");
            return "";
        }
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
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (NoSuchAlgorithmException e) {
            // This won't happen in an Android-compatible environment.
            throw new RuntimeException(e);
        } catch (Base64DecoderException e) {
            Log.e("Appsii", "Could not decode from Base64.");
            throw new IllegalArgumentException(e);
        } catch (InvalidKeySpecException e) {
            Log.e("Appsii", "Invalid key specification.");
            throw new IllegalArgumentException(e);
        }
    }

    // generate a hash
    public static String sha256(String in) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(in.getBytes("UTF-8"));

            return bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
            return null;
        }
    }

    // utility function
    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Starts the check. Returns null if the plugin is not installed, the worker
     * in case it is.
     */
    public AsyncTask<?, ?, ?> checkAccess() {
        try {
            mContext.getPackageManager().getPackageInfo(mPackageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(mContext, R.string.plugin_not_installed, Toast.LENGTH_SHORT).show();
            return null;
        }


        mTask = new AsyncTask<Void, Void, Bundle>() {
            @Override
            protected Bundle doInBackground(Void... params) {
                if (DEBUG) Log.d("LicenseChecker", "in background, connecting to service");
                final PluginConnectionHelper.AppsiPluginChecker checker =
                        mPluginConnectionHelper.connectToService();
                if (DEBUG) Log.d("LicenseChecker", "got checker: " + checker);

                if (checker == null) {
                    return null;
                }

                try {
                    if (DEBUG) Log.d("LicenseChecker", "await connection");
                    if (checker.waitForConnection()) {
                        if (DEBUG) Log.d("LicenseChecker", "connected, calling remote method");
                        return checker.checkLicense();
                    }
                } catch (InterruptedException e) {
                    return null;
                } catch (RemoteException e) {
                    if (DEBUG) Log.wtf("Promo", "error in rpc", e);
                    return null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bundle bundle) {
                if (DEBUG) Log.d("LicenseChecker", "post exec. result: " + bundle);
                checkResult(bundle);
            }
        };
        // Do not block the main executor; run in a different pool
        return mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Verifies that the Bundle contains a string "s" that is equal to
     * salt + key + validation result (0 <- valid) + sha1 + mPackageName
     * <p/>
     * in which salt: is a random generated string (uuid)
     * key is the google play license key
     * sha1 is the locally calculated cache of the certificate key
     * package-name is the target package-name
     */
    void checkResult(Bundle bundle) {
        try {
            if (bundle == null) {
                return;
            }

            int responseCode = bundle.getInt(RESPONSE_CODE);
            String signedData = bundle.getString(SIGNED_DATA);
            String signature = bundle.getString(SIGNATURE);

            LicenseValidator validator = new LicenseValidator(mNonce, mPackageName, mVersionCode);
            PublicKey publicKey = generatePublicKey(mKey);
            int response = validator.verify(publicKey, responseCode, signedData, signature);

            boolean verified = response ==
                    LicenseValidator.LICENSED_OLD_KEY || response == LicenseValidator.LICENSED;

            if (mContext.getPackageManager().checkSignatures(
                    mPackageName, BuildConfig.APPLICATION_ID)
                    != PackageManager.SIGNATURE_MATCH) {
                throw new IllegalStateException("Unknown error");
            }

            String salt = mPluginConnectionHelper.mSalt;
            if (salt == null) {
                return;
            }

            String sha1;
            try {
                sha1 = PromoUnlockFragment.getCertificateFingerPrint(mContext, mPackageName);
            } catch (PackageManager.NameNotFoundException e) {
                return;
            }

            String keyWithResult = sha256(mKey + signedData + mNonce);

            String key = salt + keyWithResult + sha1 + mPackageName;
            if (DEBUG) Log.d("Appsii", "salt: " + salt);
            if (DEBUG) Log.d("Appsii", "keyWithResult: " + keyWithResult);
            if (DEBUG) Log.d("Appsii", "sha1: " + sha1);
            if (DEBUG) Log.d("Appsii", "package: " + mPackageName);
            String expectedSha1 = PromoUnlockFragment.sha1(key);

            String bundleSha1 = bundle.getString("s");
            if (bundleSha1 == null) {
                return;
            }
            if (DEBUG) Log.d("Appsii", "expected: " + expectedSha1);
            if (DEBUG) Log.d("Appsii", "received: " + bundleSha1);

            if (TextUtils.equals(expectedSha1, bundleSha1) && verified) {
                ObfuscatedPreferences preferences =
                        PreferencesFactory.getObfuscatedPreferences(mContext);
                preferences.edit().putString(mFeature, mFeatureKey).apply();
            }

        } finally {
            mPluginConnectionHelper.onDestroy();
            mPluginConnectionHelper = null;
            onCheckComplete(mPackageName);
        }
    }

    protected abstract void onCheckComplete(String packageName);


}
