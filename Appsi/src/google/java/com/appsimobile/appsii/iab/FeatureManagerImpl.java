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
 * The normal implementation of the feature manager.
 * <p/>
 * Created by nick on 04/02/15.
 */
class FeatureManagerImpl implements FeatureManager, BaseIabHelper.OnIabSetupFinishedListener {


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

    /**
     * The result from connecting to the iab-helper
     */
    IabResult mIabResult;

    /**
     * The inventory helper that is in use.
     * When non-null, it is initializing in case mIabResult is null.
     * If mIabResult is not noll, initialization finished. Check the
     * status with mIabResult.isSuccessful()
     */
    IabInventoryHelper mInventoryHelper;

    /**
     * The loader used to load the sku-details and the purchased status
     */
    AsyncTask<Void, Void, Inventory> mLoadFeaturesTask;

    FeatureManagerImpl(Context context) {
        mContext = context;
    }

    @Nullable
    public Inventory getInventory() {
        return mInventory;
    }

    @Override
    public void onIabSetupFinished(IabResult result) {
        mIabResult = result;
        if (result.isSuccess()) {
            loadInventory();
        } else {
            mInventoryHelper.dispose();
            mInventoryHelper = null;
            notifyIabSetupFailed();
        }
    }

    public boolean areFeaturesLoaded() {
        return mInventory != null;
    }

    /**
     * Loads the inventory. This depends on a few things. If there is not yet a
     * result, but the helper is initialized, this means we are already loading
     * the inventory.
     * In case there is a result, but it is invalid, we return and do nothing
     * <p/>
     * In case there is no helper yet, it is created and it's setup is started
     */
    private void loadInventory() {

        // when the inventory-helper is not yet connected,
        // but connecting just wait until that callback is received.
        // this will auto-load the purchases anyway
        if (mIabResult == null && mInventoryHelper != null) return;

        // if the previous attempt failed, it is up to the client to
        // restart this process again.
        // The inventory-helper is only nullified if the load failed
        if (mIabResult != null && !mIabResult.isSuccess()) {
            mIabResult = null;
            return;
        }

        // if the inventoryHelper is not yet initialized, start
        // connecting to the iab-service. When is is successfully
        // connected, it will call this method again. In that
        // case, mIabResult is not null, and the task to query
        // the purchases will be started.
        if (mInventoryHelper == null) {
            mInventoryHelper = new IabInventoryHelper(mContext);
            mInventoryHelper.startSetup(this);
            return;
        }

        // In an existing loader exists, cancel it.
        if (mLoadFeaturesTask != null) {
            mLoadFeaturesTask.cancel(true);
        }

        // Create a new loader and start it.
        mLoadFeaturesTask = new AsyncTask<Void, Void, Inventory>() {
            @Override
            protected Inventory doInBackground(Void... params) {
                try {
                    return getInventoryFromHelper();
                } catch (IabException | RuntimeException e) {
                    Log.wtf("FeatureManager", "error loading inventory", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Inventory inventory) {
                onInventoryLoaded(inventory);
            }
        };
        // Do not execute on the main executor because in case of slow
        // network connections this may block the other short running
        // tasks as well; such as querying the database
        mLoadFeaturesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private void notifyIabSetupFailed() {
        int size = mFeatureManagerListeners.size();
        for (int i = size - 1; i >= 0; i--) {
            FeatureManagerListener listener = mFeatureManagerListeners.get(i);
            listener.onIabSetupFailed();
        }
    }

    public boolean areFeaturesLoading() {
        return mLoadFeaturesTask != null;
    }

    /**
     * Performs the actual query on the inventory-helper. This is executed on
     * a background thread.
     */
    protected Inventory getInventoryFromHelper() throws IabException {
        return mInventoryHelper.queryInventory(true, sAllSkus);
    }

    void onInventoryLoaded(Inventory inventory) {
        mInventory = inventory;
        mLoadFeaturesTask = null;
        mInventoryHelper.dispose();
        mInventoryHelper = null;
        notifyInventoryReady();
    }

    /**
     * Triggers a load. Returns true if a load was started
     */
    public boolean load(boolean force) {
        if (force) {
            loadInventory();
            return true;
        }

        if (!areFeaturesLoaded() && !areFeaturesLoading()) {
            loadInventory();
            return true;
        }
        return false;
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
