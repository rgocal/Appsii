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
import android.support.annotation.Nullable;

/**
 * A helper around the iab-purchase process. This class needs to
 * Created by nick on 04/02/15.
 */
public class PurchaseHelper implements BaseIabHelper.OnIabSetupFinishedListener {

    public static final String TEST_PURCHASE = "android.test.purchased";

    final Activity mActivity;

    final IabPurchaseHelper mIabPurchaseHelper;

    boolean mInitializing;

    boolean mConnectedToGooglePlay;

    final PurchaseHelperListener mPurchaseHelperListener;

    /**
     * Creates an instance of the PurchaseHelper. This will automatically attempt
     * to connect to the Google play store. You must call dispose when you are done
     * with this class to prevent leaks
     */
    public PurchaseHelper(Activity activity, PurchaseHelperListener purchaseHelperListener) {
        mPurchaseHelperListener = purchaseHelperListener;
        mActivity = activity;

        mIabPurchaseHelper = new IabPurchaseHelper(activity);
        mInitializing = true;
        mIabPurchaseHelper.startSetup(this);
    }

    public boolean isConnectedToGooglePlay() {
        return mConnectedToGooglePlay;
    }

    /**
     * Creates a helper to purchase a product. Will return null when the iab-helper
     * is not connected.
     */
    @Nullable
    public ProductPurchaseHelper createProductPurchaseHelper(String sku) {
        if (!mConnectedToGooglePlay) return null;

        return new ProductPurchaseHelper(sku);
    }

    @Override
    public void onIabSetupFinished(IabResult result) {
        mInitializing = false;
        mConnectedToGooglePlay = result.isSuccess();
        if (mConnectedToGooglePlay) {
            mPurchaseHelperListener.onIabSetupSuccess();
        } else {
            mPurchaseHelperListener.onIabSetupFailed();
        }
    }


    public void dispose() {
        mIabPurchaseHelper.dispose();
    }

    public int consumeTestPurchase(final Purchase purchase) {
        if (mConnectedToGooglePlay) {
            return mIabPurchaseHelper.consume(TEST_PURCHASE, purchase.mToken);
        }
        return BaseIabHelper.IABHELPER_UNKNOWN_ERROR;
    }

    /**
     * A listener that will receive connection status callbacks from
     */
    public interface PurchaseHelperListener {

        void onIabSetupSuccess();

        void onIabSetupFailed();
    }
}
