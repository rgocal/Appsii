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

package com.appsimobile.appsii.promo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.appsimobile.appsii.ActivityUtils;
import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.AppsiiUtils;
import com.appsimobile.appsii.PageHelper;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.iab.BaseIabHelper;
import com.appsimobile.appsii.iab.FeatureManager;
import com.appsimobile.appsii.iab.FeatureManagerHelper;
import com.appsimobile.appsii.iab.IabPurchaseHelper;
import com.appsimobile.appsii.iab.ProductPurchaseHelper;
import com.appsimobile.appsii.iab.Purchase;
import com.appsimobile.appsii.iab.PurchaseHelper;
import com.appsimobile.appsii.iab.SkuDetails;
import com.appsimobile.appsii.module.home.provider.HomeContract;

import javax.inject.Inject;

/**
 * Created by nick on 16/11/14.
 */
public class PromoActivity extends AppCompatActivity
        implements View.OnClickListener, IabPurchaseHelper.OnIabPurchaseFinishedListener,
        PurchaseHelper.PurchaseHelperListener, FeatureManager.FeatureManagerListener,
        PromoUnlockFragment.UnlockListener {

    /**
     * The request-code used to send with the purchases
     */
    final static int PURCHASE_REQUEST_CODE = 0x0BADBABE;

    @Inject
    AnalyticsManager mAnalyticsManager;

    /**
     * The button to manage the home pages
     */
    View mGotIt;

    Button mGotItButton;

    Button mPeopleUnlockButton;

    Button mAgendaUnlockButton;

    Button mCallsUnlockButton;

    Button mAllUnlockButton;

    Button mSettingsAgendaUnlockButton;

    Button mCallsPeopleSmsUnlockButton;

    /**
     * The button to try the agenda page
     */
    Button mTryAgendaButton;

    /**
     * The button to try the call log page
     */
    Button mTryCallsButton;

    /**
     * The button to try the people page
     */
    Button mTryPeopleButton;

    /**
     * An ongoing purchase, saved in the instance state
     */
    ProductPurchaseHelper mActivePurchase;

    /**
     * The purchase helper to create purchases on
     */
    PurchaseHelper mPurchaseHelper;

    /**
     * The feature manager which we can use to get info about purchased items
     */
    @Inject
    FeatureManager mFeatureManager;

    @Inject
    FeatureManagerHelper mFeatureManagerHelper;


    boolean mIabHelperConnected;

    @Inject
    SharedPreferences mPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);
        ActivityUtils.setContentView(this, R.layout.activity_promo);

        mGotIt = findViewById(R.id.appsi_plugins_got_it);
        mGotItButton = (Button) findViewById(R.id.appsi_plugins_got_it_button);
        mCallsUnlockButton = (Button) findViewById(R.id.call_log_unlock);
        mTryCallsButton = (Button) findViewById(R.id.calls_try);
        mPeopleUnlockButton = (Button) findViewById(R.id.people_unlock);
        mTryPeopleButton = (Button) findViewById(R.id.people_try);
        mAgendaUnlockButton = (Button) findViewById(R.id.agenda_unlock);
        mTryAgendaButton = (Button) findViewById(R.id.agenda_try);
        mAllUnlockButton = (Button) findViewById(R.id.all_unlock);
        mSettingsAgendaUnlockButton = (Button) findViewById(R.id.setting_agenda_unlock);
        mCallsPeopleSmsUnlockButton = (Button) findViewById(R.id.calls_people_sms_unlock);
        ActivityUtils.setupToolbar(this, R.id.toolbar);

        if (!mPreferences.getBoolean("appsi_plugins_got_it_dismissed", false)) {
            mGotIt.setVisibility(View.VISIBLE);
        }

        mGotItButton.setOnClickListener(this);
        mCallsUnlockButton.setOnClickListener(this);
        mTryCallsButton.setOnClickListener(this);
        mPeopleUnlockButton.setOnClickListener(this);
        mTryPeopleButton.setOnClickListener(this);
        mAgendaUnlockButton.setOnClickListener(this);
        mTryAgendaButton.setOnClickListener(this);
        mAllUnlockButton.setOnClickListener(this);
        mSettingsAgendaUnlockButton.setOnClickListener(this);
        mCallsPeopleSmsUnlockButton.setOnClickListener(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPurchaseHelper = new PurchaseHelper(this, this);

        // make sure the purchases are loaded, but don't force.
        mFeatureManager.load(true);
        mFeatureManager.registerFeatureManagerListener(this);

        if (savedInstanceState != null) {
            mActivePurchase = savedInstanceState.getParcelable("active_purchase");
        }

        PromoUnlockFragment unlockFragment =
                (PromoUnlockFragment) getFragmentManager().findFragmentByTag("unlock");

        if (unlockFragment != null) {
            unlockFragment.setUnlockListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPurchaseHelper.dispose();
        mPurchaseHelper = null;
        mFeatureManager.unregisterFeatureManagerListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mActivePurchase != null && requestCode == PURCHASE_REQUEST_CODE) {
            mActivePurchase.onActivityResult(resultCode, data, this);
            mActivePurchase = null;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("active_purchase", mActivePurchase);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateButtonStatusFromInventory();
        if (mFeatureManager.areFeaturesLoaded()) {
            onInventoryLoaded();
        }
    }

    private void updateButtonStatusFromInventory() {
        if (mFeatureManager.areFeaturesLoaded()) {
            boolean agendaAccess = mFeatureManagerHelper.hasAgendaAccess(this, mFeatureManager);
            boolean settingsAccess = mFeatureManagerHelper.hasSettingsAccess(this, mFeatureManager);
            boolean peopleAccess = mFeatureManagerHelper.hasPeopleAccess(this, mFeatureManager);
            boolean callsAccess = mFeatureManagerHelper.hasCallsAccess(this, mFeatureManager);
            boolean smsAccess = mFeatureManagerHelper.hasSmsAccess(this, mFeatureManager);

            if (agendaAccess) {
                mAgendaUnlockButton.setText(R.string.unlocked);
                mAgendaUnlockButton.setEnabled(false);
            } else {
                String price = getPriceForFeature(FeatureManager.AGENDA_FEATURE);
                mAgendaUnlockButton.setText(price);
                mAgendaUnlockButton.setEnabled(true);
            }
            if (settingsAccess) {
                // TODO: add button
            }
            if (smsAccess) {
                // TODO: add button
            }

            if (peopleAccess) {
                mPeopleUnlockButton.setText(R.string.unlocked);
                mPeopleUnlockButton.setEnabled(false);
            } else {
                String price = getPriceForFeature(FeatureManager.PEOPLE_FEATURE);
                mPeopleUnlockButton.setText(price);
                mPeopleUnlockButton.setEnabled(true);
            }


            if (callsAccess) {
                mCallsUnlockButton.setText(R.string.unlocked);
                mCallsUnlockButton.setEnabled(false);
            } else {
                String price = getPriceForFeature(FeatureManager.CALLS_FEATURE);
                mCallsUnlockButton.setText(price);
                mCallsUnlockButton.setEnabled(true);
            }


            if (agendaAccess && settingsAccess) {
                mSettingsAgendaUnlockButton.setText(R.string.unlocked);
                mSettingsAgendaUnlockButton.setEnabled(false);
            } else if ((!agendaAccess && settingsAccess) || (!settingsAccess && agendaAccess)) {
                mSettingsAgendaUnlockButton.setText(R.string.partially_unlocked);
                mSettingsAgendaUnlockButton.setEnabled(false);
            } else {
                String price = getPriceForFeature(FeatureManager.SETTINGS_AGENDA_FEATURE);
                mSettingsAgendaUnlockButton.setText(price);
                mSettingsAgendaUnlockButton.setEnabled(true);
            }

            if (peopleAccess && callsAccess && smsAccess) {
                mCallsPeopleSmsUnlockButton.setText(R.string.unlocked);
                mCallsPeopleSmsUnlockButton.setEnabled(false);
            } else if (peopleAccess || callsAccess || smsAccess) {
                mCallsPeopleSmsUnlockButton.setText(R.string.partially_unlocked);
                mCallsPeopleSmsUnlockButton.setEnabled(false);
            } else {
                String price = getPriceForFeature(FeatureManager.SMS_CALLS_PEOPLE_FEATURE);
                mCallsPeopleSmsUnlockButton.setText(price);
                mCallsPeopleSmsUnlockButton.setEnabled(true);
            }

            if (agendaAccess && settingsAccess && callsAccess && peopleAccess && smsAccess) {
                mAllUnlockButton.setText(R.string.unlocked);
                mAllUnlockButton.setEnabled(false);
            } else if (agendaAccess || settingsAccess || callsAccess || peopleAccess || smsAccess) {
                mAllUnlockButton.setText(R.string.partially_unlocked);
                mAllUnlockButton.setEnabled(false);
            } else {
                String price = getPriceForFeature(FeatureManager.ALL_FEATURE);
                mAllUnlockButton.setText(price);
                mAllUnlockButton.setEnabled(true);
            }
        }
    }

    private void onInventoryLoaded() {
        Purchase purchase = mFeatureManager.getPurchaseForSku(PurchaseHelper.TEST_PURCHASE);
        if (purchase != null) {
            Log.i("Appsi", "Consuming test purchase");
            consumePurchase(purchase);
        }
        updateButtonStatusFromInventory();

    }

    String getPriceForFeature(String feature) {
        if (mFeatureManager.areFeaturesLoaded()) {
            SkuDetails details = mFeatureManager.getSkuDetailForSku(feature);
            return details.getPrice();
        }
        return getString(R.string.unlock);
    }

    void consumePurchase(Purchase purchase) {
        if (!PurchaseHelper.TEST_PURCHASE.equals(purchase.getSku())) return;

        AsyncTask<Purchase, Void, Integer> task = new AsyncTask<Purchase, Void, Integer>() {
            @Override
            protected Integer doInBackground(Purchase... params) {
                try {
                    Purchase purchase = params[0];
                    return mPurchaseHelper.consumeTestPurchase(purchase);
                } catch (Throwable e) {
                    return BaseIabHelper.IABHELPER_UNKNOWN_ERROR;
                }
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != BaseIabHelper.BILLING_RESPONSE_RESULT_OK) {
                    Log.wtf("Appsii", "error consuming purchase");
                }
            }
        };
        // Do not block the main executor thread.
        // run in a different pool
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.promo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_unlock_appsi_plugins) {
            showUnlockFragment();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void showUnlockFragment() {
        PromoUnlockFragment fragment = new PromoUnlockFragment();
        fragment.setUnlockListener(this);
        fragment.show(getFragmentManager(), "unlock");
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.appsi_plugins_got_it_button:
                onAppsiPluginsGotItClicked();
                break;
            case R.id.call_log_unlock:
                purchaseCallLogPage();
                break;
            case R.id.calls_try:
                tryCallLogPage();
                break;
            case R.id.people_unlock:
                purchasePeoplePage();
                break;
            case R.id.people_try:
                tryPeoplePage();
                break;
            case R.id.agenda_unlock:
                purchaseAgendaPage();
                break;
            case R.id.agenda_try:
                tryAgendaPage();
                break;
            case R.id.all_unlock:
                purchaseAllPages();
                break;
            case R.id.setting_agenda_unlock:
                purchaseSettingsAndAgendaPages();
                break;
            case R.id.calls_people_sms_unlock:
                purchaseCallsPeopleAndSmsPages();
                break;
        }
    }

    private void onAppsiPluginsGotItClicked() {
        mPreferences.edit().putBoolean("appsi_plugins_got_it_dismissed", true).apply();
        mGotIt.setVisibility(View.GONE);
    }

    private void purchaseCallLogPage() {
        performPurchase(FeatureManager.CALLS_FEATURE);
    }

    private void tryCallLogPage() {
        Intent intent = AppsiiUtils.createTryOpenIntent(this, HomeContract.Pages.PAGE_CALLS);
        mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_PREVIEW,
                AnalyticsManager.CATEGORY_PAGES, FeatureManager.CALLS_FEATURE);

        startService(intent);
    }

    private void purchasePeoplePage() {
        performPurchase(FeatureManager.PEOPLE_FEATURE);
    }

    private void tryPeoplePage() {
        Intent intent = AppsiiUtils.createTryOpenIntent(this, HomeContract.Pages.PAGE_PEOPLE);
        mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_PREVIEW,
                AnalyticsManager.CATEGORY_PAGES, FeatureManager.PEOPLE_FEATURE);

        startService(intent);
    }

    private void purchaseAgendaPage() {
        performPurchase(FeatureManager.AGENDA_FEATURE);
    }

    private void tryAgendaPage() {
        Intent intent = AppsiiUtils.createTryOpenIntent(this, HomeContract.Pages.PAGE_AGENDA);
        mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_PREVIEW,
                AnalyticsManager.CATEGORY_PAGES, FeatureManager.AGENDA_FEATURE);
        startService(intent);
    }

    private void purchaseAllPages() {
        performPurchase(FeatureManager.ALL_FEATURE);
    }

    private void purchaseSettingsAndAgendaPages() {
        performPurchase(FeatureManager.SETTINGS_AGENDA_FEATURE);
    }

    private void purchaseCallsPeopleAndSmsPages() {
        performPurchase(FeatureManager.SMS_CALLS_PEOPLE_FEATURE);
    }

    private void performPurchase(String feature) {
        if (mPurchaseHelper.isConnectedToGooglePlay()) {
            if (mActivePurchase == null) {
                mActivePurchase = mPurchaseHelper.createProductPurchaseHelper(feature);
                if (mActivePurchase != null) {
                    if (!mActivePurchase.startPurchaseFlow(
                            this, PURCHASE_REQUEST_CODE, mPurchaseHelper)) {

                        int result = mActivePurchase.getLastResult();
                        if (result != BaseIabHelper.IABHELPER_REMOTE_EXCEPTION) {
                            Toast.makeText(this, R.string.error_launching_purchase_flow,
                                    Toast.LENGTH_SHORT).show();
                        }

                        mActivePurchase = null;
                    }
                }
            } else {
                Toast.makeText(this, R.string.purchase_in_progress, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onIabPurchaseSuccess(Purchase info) {
        SkuDetails details = mFeatureManager.getSkuDetailForSku(info.getSku());
        unlockPurchase(info);
        mActivePurchase = null;
        mAnalyticsManager.trackPurchase(info, details);
        // make the FeatureManager update it's item list
        // this will also result in a call to onInventoryReady.
        // there we check if the test-purchase is in the
        // inventory and clear it again
        mFeatureManager.load(true);

    }

    private void unlockPurchase(Purchase info) {
        String sku = info.getSku();
        PageHelper pageHelper = PageHelper.getInstance(this);
        // unlock the page and add it to the existing hotspots
        // if the page was not already enabled
        pageHelper.enablePageAccess(sku, false);

        mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_PURCHASE,
                AnalyticsManager.CATEGORY_PAGES, sku);
    }

    @Override
    public void onIabSetupSuccess() {
        mIabHelperConnected = true;
    }

    @Override
    public void onIabSetupFailed() {
        Toast.makeText(this, R.string.connection_google_play_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInventoryReady() {
        onInventoryLoaded();
    }

    @Override
    public void onAppsiPluginUnlocked() {
        updateButtonStatusFromInventory();
    }

//
//    static class AsyncQueryHandlerImpl extends AsyncQueryHandler {
//
//        static final int QUERY_PAGE_INSERTED = 1;
//
//        static final int QUERY_HOTSPOTS = 2;
//
//        static final int INSERT_ENABLE_PAGE = 3;
//
//        static final int INSERT_HOTSPOT_PAGE = 4;
//
//        final Context mContext;
//
//        public AsyncQueryHandlerImpl(Context context, ContentResolver cr) {
//            super(cr);
//            mContext = context;
//        }
//
//        public void ensurePageEnabled(String sku) {
//            switch (sku) {
//                case FeatureManager.AGENDA_FEATURE:
//                    ensurePageEnabled(HomeContract.Pages.PAGE_AGENDA);
//                    break;
//                case FeatureManager.SETTINGS_AGENDA_FEATURE:
//                    ensurePageEnabled(HomeContract.Pages.PAGE_AGENDA);
//                    ensurePageEnabled(HomeContract.Pages.PAGE_SETTINGS);
//                    break;
//                case FeatureManager.SMS_CALLS_PEOPLE_FEATURE:
//                    ensurePageEnabled(HomeContract.Pages.PAGE_SMS);
//                    ensurePageEnabled(HomeContract.Pages.PAGE_CALLS);
//                    ensurePageEnabled(HomeContract.Pages.PAGE_PEOPLE);
//                    break;
//                case FeatureManager.SETTINGS_FEATURE:
//                    ensurePageEnabled(HomeContract.Pages.PAGE_SETTINGS);
//                    break;
//                case FeatureManager.ALL_FEATURE:
//                    ensurePageEnabled(HomeContract.Pages.PAGE_AGENDA);
//                    ensurePageEnabled(HomeContract.Pages.PAGE_CALLS);
//                    ensurePageEnabled(HomeContract.Pages.PAGE_PEOPLE);
//                    break;
//                case FeatureManager.CALLS_FEATURE:
//                    ensurePageEnabled(HomeContract.Pages.PAGE_CALLS);
//                    break;
//                case FeatureManager.PEOPLE_FEATURE:
//                    ensurePageEnabled(HomeContract.Pages.PAGE_PEOPLE);
//                    break;
//                case FeatureManager.SMS_FEATURE:
//                    ensurePageEnabled(HomeContract.Pages.PAGE_SMS);
//                    break;
//            }
//        }
//
//        private void ensurePageEnabled(int pageType) {
//            startQuery(QUERY_PAGE_INSERTED, pageType,
//                    HomeContract.Pages.CONTENT_URI,
//                    new String[]{HomeContract.Pages._ID},
//                    HomeContract.Pages.TYPE + "=?",
//                    new String[]{String.valueOf(pageType)},
//                    null
//            );
//        }
//
//        @Override
//        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
//            if (token == QUERY_PAGE_INSERTED) {
//                int pageType = (int) cookie;
//                int count = cursor.getCount();
//                cursor.close();
//                if (count == 0) {
//                    enablePage(pageType);
//                }
//            } else if (token == QUERY_HOTSPOTS) {
//                Uri pageUri = (Uri) cookie;
//                long pageId = ContentUris.parseId(pageUri);
//                while (cursor.moveToNext()) {
//                    long hotspotId = cursor.getLong(0);
//                    ContentValues values = new ContentValues(3);
//                    values.put(HomeContract.HotspotPages._PAGE_ID, pageId);
//                    values.put(HomeContract.HotspotPages._HOTPSOT_ID, hotspotId);
//                    values.put(HomeContract.HotspotPages.POSITION, 12);
//                    startInsert(INSERT_HOTSPOT_PAGE, null,
//                            HomeContract.HotspotPages.CONTENT_URI, values);
//                }
//                cursor.close();
//
//            }
//        }
//
//        public void enablePage(int pageType) {
//            ContentValues values = new ContentValues();
//            String displayName = getTitleForPageType(pageType);
//
//            values.put(HomeContract.Pages.TYPE, pageType);
//            values.put(HomeContract.Pages.DISPLAY_NAME, displayName);
//
//            startInsert(INSERT_ENABLE_PAGE, null, HomeContract.Pages.CONTENT_URI, values);
//        }
//
//        private String getTitleForPageType(int pageType) {
//            int resId;
//            switch (pageType) {
//                case HomeContract.Pages.PAGE_AGENDA:
//                    resId = R.string.agenda_page_name;
//                    break;
//                case HomeContract.Pages.PAGE_CALLS:
//                    resId = R.string.calls_page_name;
//                    break;
//                case HomeContract.Pages.PAGE_PEOPLE:
//                    resId = R.string.people_page_name;
//                    break;
//                default:
//                    return null;
//            }
//            return mContext.getString(resId);
//        }
//
//        @Override
//        protected void onInsertComplete(int token, Object cookie, Uri uri) {
//            if (token == INSERT_ENABLE_PAGE) {
//                startQuery(QUERY_HOTSPOTS, uri,
//                        HomeContract.Hotspots.CONTENT_URI,
//                        new String[]{HomeContract.Hotspots._ID},
//                        null,
//                        null,
//                        null);
//            }
//        }
//    }
}

