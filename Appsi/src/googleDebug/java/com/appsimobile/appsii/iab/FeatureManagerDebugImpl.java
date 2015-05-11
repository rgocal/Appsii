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

import android.content.Context;
import android.support.v4.util.SimpleArrayMap;

/**
 * A special implementation of the feature-manager that can mark certain
 * purchases as purchased.
 * <p/>
 * Created by nick on 04/02/15.
 */
public class FeatureManagerDebugImpl extends FeatureManagerImpl {

    static final SimpleArrayMap<String, Purchase> sPurchasedSkus = new SimpleArrayMap<>();

    FeatureManagerDebugImpl(Context context) {
        super(context);
    }

    /**
     * Called when an item was purchased using the ProductPurchaseHelper when in
     * test_purchase mode. This adds the purchase to our own list as if the
     * product was really purchased. This won't be remembered on restart.
     */
    public static void addPurchase(Purchase info) {
        sPurchasedSkus.put(info.mSku, info);
    }

    /**
     * Simply change the purchases in the inventory to show up as purchased.
     *
     * @throws IabException
     */
    protected Inventory getInventoryFromHelper() throws IabException {
        Inventory result = super.getInventoryFromHelper();
//        for (String sku : sAllSkus) {
//            result.mPurchaseMap.put(sku, new Purchase(sku, BaseIabHelper.ITEM_TYPE_INAPP));
//        }

        int count = sPurchasedSkus.size();
        for (int i = 0; i < count; i++) {
            Purchase purchase = sPurchasedSkus.valueAt(i);
            result.mPurchaseMap.put(purchase.mSku, purchase);
        }

        return result;
    }
}
