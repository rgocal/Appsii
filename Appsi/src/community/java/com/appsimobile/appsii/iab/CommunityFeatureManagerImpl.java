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
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A special implementation of the feature manager which always returns that all features
 * have been unlocked.
 * <p/>
 * Created by nick on 04/05/15.
 */
class CommunityFeatureManagerImpl implements FeatureManager {


    /**
     * The list of all skus present in the play store
     */
    static final List<String> sAllSkus;

    static {
        List<String> result = new ArrayList<>();
        result.add(AGENDA_FEATURE);
        result.add(PEOPLE_FEATURE);
        result.add(SETTINGS_FEATURE);
        result.add(CALLS_FEATURE);
        result.add(SMS_FEATURE);
        result.add(SETTINGS_AGENDA_FEATURE);
        result.add(SMS_CALLS_PEOPLE_FEATURE);
        result.add(ALL_FEATURE);
        sAllSkus = Collections.unmodifiableList(result);
    }

    /**
     * The context we can use to connect to the iab-helper
     */
    final Context mContext;

    final List<FeatureManagerListener> mFeatureManagerListeners = new ArrayList<>(4);

    /**
     * The inventory that was loaded
     */
    Inventory mInventory;

    CommunityFeatureManagerImpl(Context context) {
        mContext = context;
        mInventory = new Inventory();
        mInventory.addPurchase(new Purchase(ALL_FEATURE, BaseIabHelper.ITEM_TYPE_INAPP));
    }

    @Nullable
    public Inventory getInventory() {
        return mInventory;
    }


    public boolean areFeaturesLoaded() {
        // just say they are loaded
        return true;
    }

    public boolean areFeaturesLoading() {
        // just say no; we are always ready and never need loading
        return false;
    }

    /**
     * Triggers a load. Returns true if a load was started
     */
    public boolean load(boolean force) {
        notifyInventoryReady();
        return true;
    }

    private void notifyInventoryReady() {
        int size = mFeatureManagerListeners.size();
        for (int i = size - 1; i >= 0; i--) {
            FeatureManagerListener listener = mFeatureManagerListeners.get(i);
            listener.onInventoryReady();
        }

    }


    public SkuDetails getSkuDetailForSku(String sku) {
        if (mInventory == null) return null;
        return mInventory.getSkuDetails(sku);
    }

    public Purchase getPurchaseForSku(String sku) {
        if (mInventory == null) return null;
        return mInventory.getPurchase(sku);
    }

    @Override
    public void registerFeatureManagerListener(FeatureManagerListener listener) {
        mFeatureManagerListeners.add(listener);
    }

    @Override
    public void unregisterFeatureManagerListener(FeatureManagerListener listener) {
        mFeatureManagerListeners.remove(listener);
    }
}
