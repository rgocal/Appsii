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

import android.support.v4.util.SimpleArrayMap;

import com.appsimobile.util.CollectionUtils;

import java.util.ArrayList;

/**
 * Represents a block of information about in-app items.
 * An Inventory is returned by such methods as {@link IabHelper#queryInventory}.
 */
public class Inventory {

    final SimpleArrayMap<String, SkuDetails> mSkuMap = new SimpleArrayMap<>();

    final SimpleArrayMap<String, Purchase> mPurchaseMap = new SimpleArrayMap<>();

    Inventory() {
    }

    /**
     * Returns the listing details for an in-app product.
     */
    public SkuDetails getSkuDetails(String sku) {
        return mSkuMap.get(sku);
    }

    /**
     * Returns purchase information for a given product, or null if there is no purchase.
     */
    public Purchase getPurchase(String sku) {
        return mPurchaseMap.get(sku);
    }

    /**
     * Returns whether or not there exists a purchase of the given product.
     */
    public boolean hasPurchase(String sku) {
        return mPurchaseMap.containsKey(sku);
    }

    /**
     * Return whether or not details about the given product are available.
     */
    public boolean hasDetails(String sku) {
        return mSkuMap.containsKey(sku);
    }

    /**
     * Erase a purchase (locally) from the inventory, given its product ID. This just
     * modifies the Inventory object locally and has no effect on the server! This is
     * useful when you have an existing Inventory object which you know to be up to date,
     * and you have just consumed an item successfully, which means that erasing its
     * purchase data from the Inventory you already have is quicker than querying for
     * a new Inventory.
     */
    public void erasePurchase(String sku) {
        if (mPurchaseMap.containsKey(sku)) mPurchaseMap.remove(sku);
    }

    /**
     * Returns a list of all owned product IDs.
     */
    ArrayList<String> getAllOwnedSkus() {
        int N = mPurchaseMap.size();
        ArrayList<String> result = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            result.add(mPurchaseMap.keyAt(i));
        }
        return result;
    }

    /**
     * Returns a list of all owned product IDs of a given type
     */
    ArrayList<String> getAllOwnedSkus(String itemType) {
        ArrayList<String> result = new ArrayList<>(mPurchaseMap.size());
        int N = mPurchaseMap.size();
        for (int i = 0; i < N; i++) {
            Purchase p = mPurchaseMap.valueAt(i);
            if (p.getItemType().equals(itemType)) result.add(p.getSku());
        }
        return result;
    }

    /**
     * Returns a list of all purchases.
     */
    ArrayList<Purchase> getAllPurchases() {
        ArrayList<Purchase> result = new ArrayList<>(mPurchaseMap.size());
        CollectionUtils.addValues(mPurchaseMap, result);
        return result;
    }

    void addSkuDetails(SkuDetails d) {
        mSkuMap.put(d.getSku(), d);
    }

    void addPurchase(Purchase p) {
        mPurchaseMap.put(p.getSku(), p);
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "mSkuMap=" + mSkuMap +
                ", mPurchaseMap=" + mPurchaseMap +
                '}';
    }
}