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

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;


/**
 * A simplified IabHelper that gives only access to the inventory requests
 */
public class IabInventoryHelper extends BaseIabHelper {

    /**
     * Creates an instance. After creation, it will not yet be ready to use. You must perform
     * setup by calling {@link #startSetup} and wait for setup to complete. This constructor does
     * not
     * block and is safe to call from a UI thread.
     *
     * @param ctx Your application or Activity context. Needed to bind to the in-app billing
     * service.
     */
    public IabInventoryHelper(Context ctx) {
        super(ctx);
    }

    public Inventory queryInventory(boolean querySkuDetails, List<String> moreSkus)
            throws IabException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     * Do not call from a UI thread.
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     * as purchase information.
     * @param moreItemSkus additional PRODUCT skus to query information on, regardless of ownership.
     * Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus additional SUBSCRIPTIONS skus to query information on, regardless of
     * ownership.
     * Ignored if null or if querySkuDetails is false.
     *
     * @throws IabException if a problem occurs while refreshing the inventory.
     */
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus,
            List<String> moreSubsSkus) throws IabException {
        checkSetupDone("queryInventory");
        try {
            Inventory inv = new Inventory();
            int r = queryPurchases(inv, ITEM_TYPE_INAPP);
            if (r != BILLING_RESPONSE_RESULT_OK) {
                throw new IabException(r, "Error refreshing inventory (querying owned items).");
            }

            if (querySkuDetails) {
                r = querySkuDetails(ITEM_TYPE_INAPP, inv, moreItemSkus);
                if (r != BILLING_RESPONSE_RESULT_OK) {
                    throw new IabException(r,
                            "Error refreshing inventory (querying prices of items).");
                }
            }

            // if subscriptions are supported, then also query for subscriptions
            if (mSubscriptionsSupported) {
                r = queryPurchases(inv, ITEM_TYPE_SUBS);
                if (r != BILLING_RESPONSE_RESULT_OK) {
                    throw new IabException(r,
                            "Error refreshing inventory (querying owned subscriptions).");
                }

                if (querySkuDetails) {
                    r = querySkuDetails(ITEM_TYPE_SUBS, inv, moreSubsSkus);
                    if (r != BILLING_RESPONSE_RESULT_OK) {
                        throw new IabException(r,
                                "Error refreshing inventory (querying prices of subscriptions).");
                    }
                }
            }

            return inv;
        } catch (RemoteException e) {
            throw new IabException(IABHELPER_REMOTE_EXCEPTION,
                    "Remote exception while refreshing inventory.", e);
        } catch (JSONException e) {
            throw new IabException(IABHELPER_BAD_RESPONSE,
                    "Error parsing JSON response while refreshing inventory.", e);
        }
    }

    private int queryPurchases(Inventory inv, String itemType)
            throws JSONException, RemoteException {
        // Query purchases
        logDebug("Querying owned items, item type: " + itemType);
        logDebug("Package name: " + mContext.getPackageName());
        boolean verificationFailed = false;
        String continueToken = null;

        do {
            logDebug("Calling getPurchases with continuation token: " + continueToken);
            Bundle ownedItems = mService.getPurchases(3, mContext.getPackageName(),
                    itemType, continueToken);

            int response = getResponseCodeFromBundle(ownedItems);
            logDebug("Owned items response: " + String.valueOf(response));
            if (response != BILLING_RESPONSE_RESULT_OK) {
                logDebug("getPurchases() failed: " + getResponseDesc(response));
                return response;
            }
            if (!ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST)
                    || !ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST)
                    || !ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST)) {
                logError("Bundle returned from getPurchases() doesn't contain required fields.");
                return IABHELPER_BAD_RESPONSE;
            }

            ArrayList<String> ownedSkus = ownedItems.getStringArrayList(
                    RESPONSE_INAPP_ITEM_LIST);
            ArrayList<String> purchaseDataList = ownedItems.getStringArrayList(
                    RESPONSE_INAPP_PURCHASE_DATA_LIST);
            ArrayList<String> signatureList = ownedItems.getStringArrayList(
                    RESPONSE_INAPP_SIGNATURE_LIST);

            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);
                if (Security.verifyPurchase(mSignatureBase64, purchaseData, signature)) {
                    logDebug("Sku is owned: " + sku);
                    Purchase purchase = new Purchase(itemType, purchaseData, signature);

                    if (TextUtils.isEmpty(purchase.getToken())) {
                        logWarn("BUG: empty/null token!");
                        logDebug("Purchase data: " + purchaseData);
                    }

                    // Record ownership and token
                    inv.addPurchase(purchase);
                } else {
                    logWarn("Purchase signature verification **FAILED**. Not adding item.");
                    logDebug("   Purchase data: " + purchaseData);
                    logDebug("   Signature: " + signature);
                    verificationFailed = true;
                }
            }

            continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN);
            logDebug("Continuation token: " + continueToken);
        } while (!TextUtils.isEmpty(continueToken));

        return verificationFailed ? IABHELPER_VERIFICATION_FAILED : BILLING_RESPONSE_RESULT_OK;
    }

    int querySkuDetails(String itemType, Inventory inv, List<String> moreSkus)
            throws RemoteException, JSONException {
        logDebug("Querying SKU details.");
        ArrayList<String> skuList = new ArrayList<>();
        skuList.addAll(inv.getAllOwnedSkus(itemType));
        if (moreSkus != null) skuList.addAll(moreSkus);

        if (skuList.size() == 0) {
            logDebug("queryPrices: nothing to do because there are no SKUs.");
            return BILLING_RESPONSE_RESULT_OK;
        }

        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(GET_SKU_DETAILS_ITEM_LIST, skuList);
        Bundle skuDetails = mService.getSkuDetails(3, mContext.getPackageName(),
                itemType, querySkus);

        if (!skuDetails.containsKey(RESPONSE_GET_SKU_DETAILS_LIST)) {
            int response = getResponseCodeFromBundle(skuDetails);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                logDebug("getSkuDetails() failed: " + getResponseDesc(response));
                return response;
            } else {
                logError(
                        "getSkuDetails() returned a bundle with neither an error nor a detail " +
                                "list.");
                return IABHELPER_BAD_RESPONSE;
            }
        }

        ArrayList<String> responseList = skuDetails.getStringArrayList(
                RESPONSE_GET_SKU_DETAILS_LIST);

        int N = responseList.size();
        for (int i = 0; i < N; i++) {
            String thisResponse = responseList.get(i);
            SkuDetails d = new SkuDetails(itemType, thisResponse);
            logDebug("Got sku details: " + d);
            inv.addSkuDetails(d);
        }
        return BILLING_RESPONSE_RESULT_OK;
    }
}