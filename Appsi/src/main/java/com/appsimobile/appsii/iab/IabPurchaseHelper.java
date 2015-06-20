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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONException;

/**
 * A specific iab-helper implementation. Gives access to the purchase function
 * if the iab-library
 */
public class IabPurchaseHelper extends BaseIabHelper {

    /**
     * Creates an instance. After creation, it will not yet be ready to use. You must perform
     * setup by calling {@link #startSetup} and wait for setup to complete. This constructor does
     * not
     * block and is safe to call from a UI thread.
     *
     * @param ctx Your application or Activity context. Needed to bind to the in-app billing
     * service.
     */
    public IabPurchaseHelper(Context ctx) {
        super(ctx);
    }

    /**
     * Handles an activity result that's part of the purchase flow in in-app billing. If you
     * are calling {@link #launchPurchaseFlow}, then you must call this method from your
     * Activity's {@link android.app.Activity@onActivityResult} method. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param resultCode The resultCode as you received it.
     * @param data The data (Intent) as you received it.
     *
     * @return Returns true if the result was related to a purchase flow and was handled;
     * false if the result was not related to a purchase, in which case you should
     * handle it normally.
     */
    public static int handleActivityResult(
            int resultCode, Intent data, OnIabPurchaseFinishedListener purchaseListener,
            String itemType, String devPayload) {

        if (data == null) {
            logError("Null data in IAB activity result.");
            return IABHELPER_BAD_RESPONSE;
        }

        int responseCode = getResponseCodeFromIntent(data);
        String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);

        if (resultCode == Activity.RESULT_OK && responseCode == BILLING_RESPONSE_RESULT_OK) {
            logDebug("Successful resultcode from purchase activity.");
            logDebug("Purchase data: " + purchaseData);
            logDebug("Data signature: " + dataSignature);
            logDebug("Extras: " + data.getExtras());
            logDebug("Expected item type: " + itemType);

            if (purchaseData == null || dataSignature == null) {
                logError("BUG: either purchaseData or dataSignature is null.");
                logDebug("Extras: " + data.getExtras().toString());
                return IABHELPER_UNKNOWN_ERROR;
            }

            Purchase purchase;
            try {
                purchase = new Purchase(itemType, purchaseData, dataSignature);
                String sku = purchase.getSku();

                // Verify signature
                if (!Security.verifyPurchase(RSA_CODE, purchaseData, dataSignature)) {
                    logError("Purchase signature verification FAILED for sku " + sku);
                    return IABHELPER_VERIFICATION_FAILED;
                }
                logDebug("Purchase signature successfully verified.");
                if (!Security.verifyDeveloperPayload(purchase, devPayload)) {
                    logError("Purchase payload verification FAILED for sku " + sku);
                    return IABHELPER_DEVELOPER_PAYLOAD_FAILED;
                }
                logDebug("Developer payload successfully verified.");
            } catch (JSONException e) {
                logError("Failed to parse purchase data.");
                e.printStackTrace();
                return IABHELPER_BAD_RESPONSE;
            }

            if (purchaseListener != null) {
                purchaseListener.onIabPurchaseSuccess(purchase);
            }
            return BILLING_RESPONSE_RESULT_OK;

        } else if (resultCode == Activity.RESULT_OK) {
            // result code was OK, but in-app billing response was not OK.
            logDebug("Result code was OK but in-app billing response was not OK: " +
                    getResponseDesc(responseCode));
            return responseCode;
        } else if (resultCode == Activity.RESULT_CANCELED) {
            logDebug("Purchase canceled - Response: " + getResponseDesc(responseCode));
            return IABHELPER_USER_CANCELLED;
        } else {
            logError("Purchase failed. Result code: " + Integer.toString(resultCode)
                    + ". Response: " + getResponseDesc(responseCode));
            return IABHELPER_UNKNOWN_PURCHASE_RESPONSE;
        }
    }

    /**
     * Returns whether subscriptions are supported.
     */
    public boolean subscriptionsSupported() {
        return mSubscriptionsSupported;
    }

    public int launchInAppItemPurchaseFlow(Activity act, String sku, int requestCode,
            String developerPayload) {
        return launchPurchaseFlow(act, sku, ITEM_TYPE_INAPP, requestCode, developerPayload);
    }

    public int launchSubscriptionPurchaseFlow(Activity act, String sku, int requestCode,
            String extraData) {
        return launchPurchaseFlow(act, sku, ITEM_TYPE_SUBS, requestCode, extraData);
    }

    /**
     * Initiate the UI flow for an in-app purchase. Call this method to initiate an in-app purchase,
     * which will involve bringing up the Google Play screen. The calling activity will be paused
     * while
     * the user interacts with Google Play, and the result will be delivered via the activity's
     * {@link android.app.Activity#onActivityResult} method, at which point you must call
     * this object's {@link #handleActivityResult} method to continue the purchase flow. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param act The calling activity.
     * @param sku The sku of the item to purchase.
     * @param itemType indicates if it's a product or a subscription (ITEM_TYPE_INAPP or
     * ITEM_TYPE_SUBS)
     * @param requestCode A request code (to differentiate from other responses --
     * as in {@link android.app.Activity#startActivityForResult}).
     * @param developerPayload Extra data (developer payload), which will be returned with the
     * purchase data
     * when the purchase completes. This extra data will be permanently bound to that purchase
     * and will always be returned when the purchase is queried.
     */
    public int launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode,
            String developerPayload) {
        checkSetupDone("launchPurchaseFlow");

        if (itemType.equals(ITEM_TYPE_SUBS) && !mSubscriptionsSupported) {
            return IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE;
        }

        try {
            logDebug("Constructing buy intent for " + sku + ", item type: " + itemType);
            Bundle buyIntentBundle =
                    mService.getBuyIntent(3, mContext.getPackageName(), sku, itemType,
                            developerPayload);
            int response = getResponseCodeFromBundle(buyIntentBundle);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                logError("Unable to buy item, Error response: " + getResponseDesc(response));
                return response;
            }

            PendingIntent pendingIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
            logDebug("Launching buy intent for " + sku + ". Request code: " + requestCode);
            act.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    requestCode, new Intent(), 0, 0, 0);
            return BILLING_RESPONSE_RESULT_OK;
        } catch (SendIntentException e) {
            logError("SendIntentException while launching purchase flow for sku " + sku);
            e.printStackTrace();
            return IABHELPER_SEND_INTENT_FAILED;
        } catch (RemoteException e) {
            logError("RemoteException while launching purchase flow for sku " + sku);
            e.printStackTrace();
            return IABHELPER_REMOTE_EXCEPTION;
        }
    }

    /**
     * Consumes a given in-app product. Consuming can only be done on an item
     * that's owned, and as a result of consumption, the user will no longer own it.
     * This method may block or take long to return.
     *
     * @throws IabException if there is a problem during consumption.
     */
    int consume(String sku, String token) {
        checkSetupDone("consume");

        try {
            if (token == null || token.equals("")) {
                logError("Can't consume " + sku + ". No token.");
                return IABHELPER_MISSING_TOKEN;
            }

            logDebug("Consuming sku: " + sku + ", token: " + token);
            int response = mService.consumePurchase(3, mContext.getPackageName(), token);
            if (response == BILLING_RESPONSE_RESULT_OK) {
                logDebug("Successfully consumed sku: " + sku);
            } else {
                logDebug("Error consuming consuming sku " + sku + ". " + getResponseDesc(response));
            }
            return response;
        } catch (RemoteException e) {
            Log.wtf("Appsii", "Remote exception while consuming. Sku: " + sku, e);
            return IABHELPER_REMOTE_EXCEPTION;
        }
    }

    /**
     * Callback that notifies when a purchase is finished.
     */
    public interface OnIabPurchaseFinishedListener {

        /**
         * Called to notify that an in-app purchase finished. If the purchase was successful,
         * then the sku parameter specifies which item was purchased. If the purchase failed,
         * the sku and extraData parameters may or may not be null, depending on how far the
         * purchase
         * process went.
         *
         * @param info The purchase information (null if purchase failed)
         */
        void onIabPurchaseSuccess(Purchase info);
    }


}