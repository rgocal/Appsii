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

package com.appsimobile.appsii.iab;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.appsimobile.appsii.BuildConfig;

import java.util.UUID;

import static com.appsimobile.appsii.iab.BaseIabHelper.ITEM_TYPE_INAPP;

/**
 * An ongoing purchase, obtained through the purchase helper. This class
 * should be saved in onSaveInstanceState while the purchase is in
 * progress to get the result reported back properly, even on orientation
 * changes.
 * <p/>
 * Created by nick on 04/02/15.
 */
public class ProductPurchaseHelper implements Parcelable {

    public static final Creator<ProductPurchaseHelper> CREATOR
            = new Creator<ProductPurchaseHelper>() {
        @Override
        public ProductPurchaseHelper createFromParcel(Parcel in) {
            return new ProductPurchaseHelper(in);
        }

        @Override
        public ProductPurchaseHelper[] newArray(int size) {
            return new ProductPurchaseHelper[size];
        }
    };

    /**
     * The sku this purchase is started for
     */
    final String mSku;

    /**
     * The provided developer payload
     */
    final String mDeveloperPayload;

    /**
     * The error-code received from launching the purchase flow, if any
     */
    int mLastResult;

    ProductPurchaseHelper(String sku) {
        mSku = sku;
        mDeveloperPayload = UUID.randomUUID().toString();
    }

    ProductPurchaseHelper(Parcel in) {
        mSku = in.readString();
        mDeveloperPayload = in.readString();
    }

    public int getLastResult() {
        return mLastResult;
    }

    /**
     * Starts the purchase flow of the given sku. The requestCode is used in
     * the call to onActivityResult, when the purchase completes.
     * The provided purchase helper has to be connected to the iab-service,
     * otherwise an IllegalStateException will be thrown.
     * Returns true if the purchase flow is correctly initiated. False otherwise.
     * <p/>
     * In case there is an error use getLastResult to get the actual reason for
     * the error. For additional details on the possible error codes see {@link
     * IabPurchaseHelper#launchInAppItemPurchaseFlow(android.app.Activity, String, int, String)}
     */
    public boolean startPurchaseFlow(
            Activity activity, int requestCode, PurchaseHelper purchaseHelper) {

        IabPurchaseHelper iabPurchaseHelper = purchaseHelper.mIabPurchaseHelper;

        String realSku;
        if (BuildConfig.TEST_PURCHASES) {
            realSku = PurchaseHelper.TEST_PURCHASE;
        } else {
            realSku = mSku;
        }

        int result = iabPurchaseHelper.
                launchInAppItemPurchaseFlow(activity, realSku, requestCode, mDeveloperPayload);
        mLastResult = result;
        return result == BaseIabHelper.BILLING_RESPONSE_RESULT_OK;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mSku);
        out.writeString(mDeveloperPayload);
    }

    /**
     * Handles the result callback of the purchase. Calls into the provided listener when
     * the purchase was successful.
     */
    public int onActivityResult(
            int resultCode, Intent data,
            final IabPurchaseHelper.OnIabPurchaseFinishedListener purchaseListener) {

        // Depending on the configuration, the listener may need to be wrapped.
        // Concretely, in googleDebug mode, we need to replace the test-purchase
        // id with the one the user actually clicked.
        // This method calls into a build specific class to provide this behavior
        IabPurchaseHelper.OnIabPurchaseFinishedListener listener =
                FeatureManagerFactory.wrapListener(mSku, purchaseListener);

        return IabPurchaseHelper.handleActivityResult(
                resultCode, data, listener, ITEM_TYPE_INAPP, mDeveloperPayload);
    }

}
