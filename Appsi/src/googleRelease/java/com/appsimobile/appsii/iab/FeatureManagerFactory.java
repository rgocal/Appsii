/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.appsii.iab;

import android.content.Context;

import com.appsimobile.appsii.annotation.VisibleForTesting;

/**
 * Created by nick on 04/02/15.
 */
public class FeatureManagerFactory {

    private static FeatureManager sFeatureManager;

    @VisibleForTesting
    public static void setFeatureManager(FeatureManager featureManager) {
        sFeatureManager = featureManager;
    }

    public static FeatureManager getFeatureManager(Context context) {
        if (sFeatureManager == null) {
            sFeatureManager = new FeatureManagerImpl(context);
        }
        return sFeatureManager;
    }

    public static IabPurchaseHelper.OnIabPurchaseFinishedListener wrapListener(
            String sku, IabPurchaseHelper.OnIabPurchaseFinishedListener purchaseListener) {
        return purchaseListener;
    }
}
