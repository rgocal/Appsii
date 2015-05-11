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

import android.text.TextUtils;
import android.util.Log;

import com.google.android.vending.licensing.util.Base64;
import com.google.android.vending.licensing.util.Base64DecoderException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Contains data related to a licensing request and methods to verify
 * and process the response.
 */
public class LicenseValidator {

    // Server response codes.
    public static final int LICENSED = 0x0;

    public static final int NOT_LICENSED = 0x1;

    public static final int LICENSED_OLD_KEY = 0x2;

    public static final int ERROR_NOT_MARKET_MANAGED = 0x3;

    public static final int ERROR_SERVER_FAILURE = 0x4;

    public static final int ERROR_OVER_QUOTA = 0x5;

    public static final int ERROR_CONTACTING_SERVER = 0x101;

    public static final int ERROR_INVALID_PACKAGE_NAME = 0x102;

    public static final int ERROR_NON_MATCHING_UID = 0x103;

    public static final int ERROR_SIGNATURE_FAILED = 0x104;

    public static final int ERROR_INVALID_PUBLIC_KEY = 5;

    public static final int ERROR_INVALID_RESPONSE = 7;

    private static final String TAG = "LicenseValidator";

    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    private final int mNonce;

    private final String mPackageName;

    private final String mVersionCode;

    public LicenseValidator(int nonce, String packageName, String versionCode) {
        mNonce = nonce;
        mPackageName = packageName;
        mVersionCode = versionCode;
    }

    public int getNonce() {
        return mNonce;
    }

    public String getPackageName() {
        return mPackageName;
    }


    /**
     * Verifies the response from server and calls appropriate callback method.
     *
     * @param publicKey public key associated with the developer account
     * @param responseCode server response code
     * @param signedData signed data from server
     * @param signature server signature
     */
    public int verify(PublicKey publicKey, int responseCode, String signedData, String signature) {
        String userId;
        // Skip signature check for unsuccessful requests
        ResponseData data;
        if (responseCode == LICENSED || responseCode == NOT_LICENSED ||
                responseCode == LICENSED_OLD_KEY) {
            // Verify signature.
            try {
                Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
                sig.initVerify(publicKey);
                sig.update(signedData.getBytes());

                if (!sig.verify(Base64.decode(signature))) {
                    Log.e(TAG, "Signature verification failed.");
                    return ERROR_SIGNATURE_FAILED;
                }
            } catch (NoSuchAlgorithmException e) {
                // This can't happen on an Android compatible device.
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                return ERROR_INVALID_PUBLIC_KEY;
            } catch (SignatureException e) {
                throw new RuntimeException(e);
            } catch (Base64DecoderException e) {
                Log.e(TAG, "Could not Base64-decode signature.");
                return ERROR_INVALID_RESPONSE;
            }

            // Parse and validate response.
            try {
                data = ResponseData.parse(signedData);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Could not parse response.");
                return ERROR_INVALID_RESPONSE;
            }

            if (data.responseCode != responseCode) {
                Log.e(TAG, "Response codes don't match.");
                return ERROR_INVALID_RESPONSE;
            }

            if (data.nonce != mNonce) {
                Log.e(TAG, "Nonce doesn't match.");
                return ERROR_INVALID_RESPONSE;
            }

            if (!data.packageName.equals(mPackageName)) {
                Log.e(TAG, "Package name doesn't match.");
                return ERROR_INVALID_RESPONSE;
            }

            if (!data.versionCode.equals(mVersionCode)) {
                Log.e(TAG, "Version codes don't match.");
                return ERROR_INVALID_RESPONSE;
            }

            // Application-specific user identifier.
            userId = data.userId;
            if (TextUtils.isEmpty(userId)) {
                Log.e(TAG, "User identifier is empty.");
                return ERROR_INVALID_RESPONSE;
            }
        }

        return responseCode;
    }
}
