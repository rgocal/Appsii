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

/**
 * Wraps the iab implementation to something that is usable in an activity
 * or fragment and abstracts away most of the iab-details (you only deal
 * with setup failed errors).
 * <p/>
 * Created by nick on 04/02/15.
 */
public interface FeatureManager {

    String AGENDA_FEATURE = "appsii_agenda_feature";
    String APPSI_PLUGIN_AGENDA_UNLOCKED = "unlocked" + AGENDA_FEATURE;
    String PEOPLE_FEATURE = "appsii_people_feature";
    String APPSI_PLUGIN_PEOPLE_UNLOCKED = "unlocked" + PEOPLE_FEATURE;
    String SETTINGS_FEATURE = "appsii_settings_feature";
    String APPSI_PLUGIN_SETTINGS_UNLOCKED = "unlocked" + SETTINGS_FEATURE;
    String CALLS_FEATURE = "appsii_calls_feature";
    String APPSI_PLUGIN_CALLS_UNLOCKED = "unlocked" + CALLS_FEATURE;
    String SMS_FEATURE = "appsii_sms_feature";
    String APPSI_PLUGIN_SMS_UNLOCKED = "unlocked" + SMS_FEATURE;

    String SETTINGS_AGENDA_FEATURE = "appsii_settings_and_agenda_feature";
    String APPSI_PLUGIN_POWERPACK_UNLOCKED = "unlocked" + SETTINGS_AGENDA_FEATURE;

    String SMS_CALLS_PEOPLE_FEATURE = "appsii_sms_calls_people_feature";
    String ALL_FEATURE = "appsii_all_feature";

    /**
     * Returns true in case the features have finished loading
     */
    boolean areFeaturesLoaded();

    /**
     * Returns true in case the features are currently loading
     */
    boolean areFeaturesLoading();

    /**
     * Starts a load of the features. May call into listeners before
     * the method returns if the data is already ready.
     */
    boolean load(boolean force);

    /**
     * Returns the sku-details for the given sku.
     * Only available when areFeaturesLoaded returns true
     */
    SkuDetails getSkuDetailForSku(String sku);

    /**
     * Returns the purchase-details for the given sku.
     * Only available when areFeaturesLoaded returns true
     */
    Purchase getPurchaseForSku(String sku);

    /**
     * Registers a listener for receiving feature manager callbacks
     * such as inventory ready and iab-setup failed
     */
    void registerFeatureManagerListener(FeatureManagerListener listener);

    /**
     * Removes a previously registered listener.
     */
    void unregisterFeatureManagerListener(FeatureManagerListener listener);

    /**
     * The listener that can be registered to the FeatureManager.
     */
    interface FeatureManagerListener {

        /**
         * Iab-setup failed
         */
        void onIabSetupFailed();

        /**
         * Called when the inventory was loaded. You can now call getSkuDetailForSku
         * and getPurchaseForSku in the FeatureManager.
         */
        void onInventoryReady();
    }
}
