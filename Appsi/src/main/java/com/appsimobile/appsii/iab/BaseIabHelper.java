/* Copyright (c) 2012 Google Inc.
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

package com.appsimobile.appsii.iab;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

/**
 * A base class for iab-features. Provides connecting to the iab-service.
 * Subclasses can implement features like purchases or inventory checks
 */
public abstract class BaseIabHelper {

    // Billing response codes
    public static final int BILLING_RESPONSE_RESULT_OK = 0;

    public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;

    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;

    public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;

    public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;

    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;

    public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;

    public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;

    // IAB Helper error codes
    public static final int IABHELPER_ERROR_BASE = -1000;

    public static final int IABHELPER_REMOTE_EXCEPTION = -1001;

    public static final int IABHELPER_BAD_RESPONSE = -1002;

    public static final int IABHELPER_VERIFICATION_FAILED = -1003;

    public static final int IABHELPER_SEND_INTENT_FAILED = -1004;

    public static final int IABHELPER_USER_CANCELLED = -1005;

    public static final int IABHELPER_UNKNOWN_PURCHASE_RESPONSE = -1006;

    public static final int IABHELPER_MISSING_TOKEN = -1007;

    public static final int IABHELPER_UNKNOWN_ERROR = -1008;

    public static final int IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE = -1009;

    public static final int IABHELPER_INVALID_CONSUMPTION = -1010;

    public static final int IABHELPER_DEVELOPER_PAYLOAD_FAILED = -2001;

    // Keys for the responses from InAppBillingService
    public static final String RESPONSE_CODE = "RESPONSE_CODE";

    public static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";

    public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";

    public static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";

    public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";

    public static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";

    public static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";

    public static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";

    public static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";

    // Item types
    public static final String ITEM_TYPE_INAPP = "inapp";

    public static final String ITEM_TYPE_SUBS = "subs";

    // some fields on the getSkuDetails response bundle
    public static final String GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST";

    public static final String GET_SKU_DETAILS_ITEM_TYPE_LIST = "ITEM_TYPE_LIST";

    static final String RSA_CODE = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmd6LF44ZpxvxO3j3" +
            "LTrwNqAL4HRgIdHhKwzLuF4rGiF4M/5ktv26gP/7toUonspr9LYDkk9xSi9JuyxVmpWvAyysCAVBnfr6CXx" +
            "DYB6aVophrMxiQkXDZ+onwFGc/T0igA0PLe1Hq4sA1Raa8NJeu08CzAJ/UuZLAHDqWVxt+myCMQJqTpm3Z9" +
            "wO10y+ftQuhnA3h2Hhi0aGH3OgYzkRv5VtAFZx0i/vacmjlIclvafazoGX77VwzUnH5Py4omG2GpsM0pItn" +
            "BLRAkLTDhDch/e7XgzoWUrJp3JXOJF7iKxMkknZa68Z4W63oaa/2I1WgLJHRV/cx0rNrMVgYSUy7wIDAQAB";

    // Is debug logging enabled?
    static boolean sDebugLog = true;

    static String sDebugTag = "IabBaseHelper";

    // Context we were passed during initialization
    final Context mContext;

    // Is setup done?
    boolean mSetupDone = false;

    // Are subscriptions supported?
    boolean mSubscriptionsSupported = false;

    // Connection to the service
    IInAppBillingService mService;

    ServiceConnection mServiceConn;

    // Public key for verifying signature, in base64 encoding
    String mSignatureBase64 = null;

    /**
     * Creates an instance. After creation, it will not yet be ready to use. You must perform
     * setup by calling {@link #startSetup} and wait for setup to complete. This constructor does
     * not
     * block and is safe to call from a UI thread.
     *
     * @param ctx Your application or Activity context. Needed to bind to the in-app billing
     * service.
     */
    public BaseIabHelper(Context ctx) {
        mContext = ctx.getApplicationContext();
        mSignatureBase64 = RSA_CODE;
        logDebug("IAB helper created.");
    }

    static void logDebug(String msg) {
        if (sDebugLog) Log.d(sDebugTag, msg);
    }

    /**
     * Returns a human-readable description for the given response code.
     *
     * @param code The response code
     *
     * @return A human-readable string explaining the result code.
     * It also includes the result code numerically.
     */
    public static String getResponseDesc(int code) {
        String[] iab_msgs = ("0:OK/1:User Canceled/2:Unknown/" +
                "3:Billing Unavailable/4:Item unavailable/" +
                "5:Developer Error/6:Error/7:Item Already Owned/" +
                "8:Item not owned").split("/");
        String[] iabhelper_msgs = ("0:OK/-1001:Remote exception during initialization/" +
                "-1002:Bad response received/" +
                "-1003:Purchase signature verification failed/" +
                "-1004:Send intent failed/" +
                "-1005:User cancelled/" +
                "-1006:Unknown purchase response/" +
                "-1007:Missing token/" +
                "-1008:Unknown error/" +
                "-1009:Subscriptions not available/" +
                "-1010:Invalid consumption attempt").split("/");

        if (code <= IABHELPER_ERROR_BASE) {
            int index = IABHELPER_ERROR_BASE - code;
            if (index >= 0 && index < iabhelper_msgs.length) {
                return iabhelper_msgs[index];
            } else {
                return String.valueOf(code) + ":Unknown IAB Helper Error";
            }
        } else if (code < 0 || code >= iab_msgs.length) {
            return String.valueOf(code) + ":Unknown";
        } else {
            return iab_msgs[code];
        }
    }

    // Workaround to bug where sometimes response codes come as Long instead of Integer
    static int getResponseCodeFromBundle(Bundle b) {
        Object o = b.get(RESPONSE_CODE);
        if (o == null) {
            logDebug("Bundle with null response code, assuming OK (known issue)");
            return BILLING_RESPONSE_RESULT_OK;
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof Long) {
            return (int) ((Long) o).longValue();
        } else {
            logError("Unexpected type for bundle response code.");
            logError(o.getClass().getName());
            throw new RuntimeException(
                    "Unexpected type for bundle response code: " + o.getClass().getName());
        }
    }

    static void logError(String msg) {
        Log.e(sDebugTag, "In-app billing error: " + msg);
    }

    // Workaround to bug where sometimes response codes come as Long instead of Integer
    static int getResponseCodeFromIntent(Intent i) {
        Object o = i.getExtras().get(RESPONSE_CODE);
        if (o == null) {
            logError("Intent with no response code, assuming OK (known issue)");
            return BILLING_RESPONSE_RESULT_OK;
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof Long) {
            return (int) ((Long) o).longValue();
        } else {
            logError("Unexpected type for intent response code.");
            logError(o.getClass().getName());
            throw new RuntimeException(
                    "Unexpected type for intent response code: " + o.getClass().getName());
        }
    }

    static void logWarn(String msg) {
        Log.w(sDebugTag, "In-app billing warning: " + msg);
    }

    /**
     * Enables or disable debug logging through LogCat.
     */
    public void enableDebugLogging(boolean enable, String tag) {
        sDebugLog = enable;
        sDebugTag = tag;
    }

    public void enableDebugLogging(boolean enable) {
        sDebugLog = enable;
    }

    /**
     * Starts the setup process. This will start up the setup process asynchronously.
     * You will be notified through the listener when the setup process is complete.
     * This method is safe to call from a UI thread.
     *
     * @param listener The listener to notify when the setup process is complete.
     */
    public void startSetup(final OnIabSetupFinishedListener listener) {
        // If already set up, can't do it again.
        if (mSetupDone) throw new IllegalStateException("IAB helper is already set up.");

        // Connection to IAB service
        logDebug("Starting in-app billing setup.");
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                logDebug("Billing service connected.");
                mService = IInAppBillingService.Stub.asInterface(service);
                String packageName = mContext.getPackageName();
                try {
                    logDebug("Checking for in-app billing 3 support.");

                    // check for in-app billing v3 support
                    int response = mService.isBillingSupported(3, packageName, ITEM_TYPE_INAPP);
                    if (response != BILLING_RESPONSE_RESULT_OK) {
                        if (listener != null) {
                            listener.onIabSetupFinished(new IabResult(response,
                                    "Error checking for billing v3 support."));
                        }

                        // if in-app purchases aren't supported, neither are subscriptions.
                        mSubscriptionsSupported = false;
                        return;
                    }
                    logDebug("In-app billing version 3 supported for " + packageName);

                    // check for v3 subscriptions support
                    response = mService.isBillingSupported(3, packageName, ITEM_TYPE_SUBS);
                    if (response == BILLING_RESPONSE_RESULT_OK) {
                        logDebug("Subscriptions AVAILABLE.");
                        mSubscriptionsSupported = true;
                    } else {
                        logDebug("Subscriptions NOT AVAILABLE. Response: " + response);
                    }

                    mSetupDone = true;
                } catch (RemoteException e) {
                    if (listener != null) {
                        listener.onIabSetupFinished(new IabResult(IABHELPER_REMOTE_EXCEPTION,
                                "RemoteException while setting up in-app billing."));
                    }
                    e.printStackTrace();
                    return;
                }

                if (listener != null) {
                    listener.onIabSetupFinished(
                            new IabResult(BILLING_RESPONSE_RESULT_OK, "Setup successful."));
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                logDebug("Billing service disconnected.");
                mService = null;
            }
        };

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        if (!mContext.getPackageManager().queryIntentServices(serviceIntent, 0).isEmpty()) {
            // service available to handle that Intent
            serviceIntent.setPackage("com.android.vending");
            mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        } else {
            // no service available to handle that Intent
            if (listener != null) {
                listener.onIabSetupFinished(
                        new IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                                "Billing service unavailable on device."));
            }
        }
    }

    /**
     * Dispose of object, releasing resources. It's very important to call this
     * method when you are done with this object. It will release any resources
     * used by it such as service connections. Naturally, once the object is
     * disposed of, it can't be used again.
     */
    public void dispose() {
        logDebug("Disposing.");
        mSetupDone = false;
        if (mServiceConn != null) {
            logDebug("Unbinding from service.");
            try {
                if (mContext != null) mContext.unbindService(mServiceConn);
            } catch (IllegalArgumentException ignore) {

            }
            mServiceConn = null;
            mService = null;
        }
    }

    // Checks that setup was done; if not, throws an exception.
    void checkSetupDone(String operation) {
        if (!mSetupDone) {
            logError("Illegal state for operation (" + operation + "): IAB helper is not set up.");
            throw new IllegalStateException(
                    "IAB helper is not set up. Can't perform operation: " + operation);
        }
    }

    /**
     * Callback for setup process. This listener's {@link #onIabSetupFinished} method is called
     * when the setup process is complete.
     */
    public interface OnIabSetupFinishedListener {

        /**
         * Called to notify that setup is complete.
         *
         * @param result The result of the setup process.
         */
        public void onIabSetupFinished(IabResult result);
    }
}