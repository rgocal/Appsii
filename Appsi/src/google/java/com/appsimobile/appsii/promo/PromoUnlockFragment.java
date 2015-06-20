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

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.PageHelper;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.iab.FeatureManager;
import com.appsimobile.appsii.iab.FeatureManagerHelper;
import com.appsimobile.appsii.module.home.provider.HomeContract;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by nick on 01/02/15.
 */
public class PromoUnlockFragment extends DialogFragment implements View.OnClickListener {

    final AnalyticsManager mAnalyticsManager = AnalyticsManager.getInstance();

//    AsyncQueryHandlerImpl mAsyncQueryHandler;

    ProgressBar mProgressBar;

    TextView mAgendaView;

    TextView mSettingsView;

    TextView mCallsView;

    TextView mPeopleView;

    TextView mSmsView;

    TextView mPowerPackView;

    Button mDownloadLink;

    /**
     * This fragment is used to retain the active license checker, even on orientation
     * changes.
     */
    LicenseCheckerFragment mLicenseCheckerFragment;

    UnlockListener mUnlockListener;

    public PromoUnlockFragment() {
        setStyle(STYLE_NO_TITLE, 0);
    }

    static String getCertificateFingerPrint(Context context, String packageName)
            throws PackageManager.NameNotFoundException {

        PackageManager pm = context.getPackageManager();
        Signature sig = pm.getPackageInfo(packageName,
                PackageManager.GET_SIGNATURES).signatures[0];

        try {
            return doFingerprint(sig.toByteArray(), "SHA1");
        } catch (NoSuchAlgorithmException e) {
            // can't happen on an android device.
            return null;
        }
    }

    protected static String doFingerprint(byte[] certificateBytes, String algorithm)
            throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(certificateBytes);
        byte[] digest = md.digest();

        String toRet = "";
        for (int i = 0; i < digest.length; i++) {
            if (i != 0) {
                toRet += ":";
            }
            int b = digest[i] & 0xff;
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) {
                toRet += "0";
            }
            toRet += hex;
        }
        return toRet;
    }

    // generate a hash
    public static String sha256(String in) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(in.getBytes("UTF-8"));

            return bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
            return null;
        }
    }

    // utility function
    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    // generate a hash
    public static String sha1(String in) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(in.getBytes("UTF-8"));

            return bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
            return null;
        }
    }

    public void setUnlockListener(UnlockListener unlockListener) {
        mUnlockListener = unlockListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLicenseCheckerFragment =
                (LicenseCheckerFragment) getFragmentManager().findFragmentByTag("checker");

        if (mLicenseCheckerFragment == null) {
            mLicenseCheckerFragment = new LicenseCheckerFragment();
            getFragmentManager().beginTransaction()
                    .add(mLicenseCheckerFragment, "checker")
                    .commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity().isFinishing()) {
            getFragmentManager().beginTransaction().remove(mLicenseCheckerFragment).commit();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.download_apks:
                onDownloadApksClicked();
                break;
            case R.id.agenda_unlocked:
                startAgendaCheck();
                break;
            case R.id.calls_unlocked:
                startCallsCheck();
                break;
            case R.id.sms_unlocked:
                startSmsCheck();
                break;
            case R.id.settings_unlocked:
                startSettingsCheck();
                break;
            case R.id.contacts_unlocked:
                startPeopleCheck();
                break;
            case R.id.powerpack_unlocked:
                startPowerPackCheck();
                break;
        }
    }

    private void onDownloadApksClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String url = getString(R.string.url_unlock_apks);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    private void startAgendaCheck() {
        if (FeatureManagerHelper.legacyAgendaUnlocked(getActivity())) {
            Toast.makeText(getActivity(), R.string.already_unlocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mLicenseCheckerFragment.isCheckInProgress()) {
            if (mLicenseCheckerFragment.startCheck(
                    new AgendaLicenseCheckerImpl(getActivity()))) {

                setCheckInProgress(true);
            }
        }
    }

    private void startCallsCheck() {
        if (FeatureManagerHelper.legacyCallsUnlocked(getActivity())) {
            Toast.makeText(getActivity(), R.string.already_unlocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mLicenseCheckerFragment.isCheckInProgress()) {
            if (mLicenseCheckerFragment.startCheck(
                    new CallsLicenseCheckerImpl(getActivity()))) {

                setCheckInProgress(true);
            }
        }
    }

    private void startSmsCheck() {
        if (FeatureManagerHelper.legacySmsUnlocked(getActivity())) {
            Toast.makeText(getActivity(), R.string.already_unlocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mLicenseCheckerFragment.isCheckInProgress()) {
            if (mLicenseCheckerFragment.startCheck(
                    new SmsLicenseCheckerImpl(getActivity()))) {

                setCheckInProgress(true);
            }
        }
    }

    private void startSettingsCheck() {
        if (FeatureManagerHelper.legacySettingsUnlocked(getActivity())) {
            Toast.makeText(getActivity(), R.string.already_unlocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mLicenseCheckerFragment.isCheckInProgress()) {
            if (mLicenseCheckerFragment.startCheck(
                    new SettingsLicenseCheckerImpl(getActivity()))) {

                setCheckInProgress(true);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_promo_unlock, container, false);
    }

    private void startPeopleCheck() {
        if (FeatureManagerHelper.legacyPeopleUnlocked(getActivity())) {
            Toast.makeText(getActivity(), R.string.already_unlocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mLicenseCheckerFragment.isCheckInProgress()) {
            if (mLicenseCheckerFragment.startCheck(
                    new ContactsLicenseCheckerImpl(getActivity()))) {

                setCheckInProgress(true);
            }
        }
    }

    private void startPowerPackCheck() {
        if (FeatureManagerHelper.legacyPowerPackUnlocked(getActivity())) {
            Toast.makeText(getActivity(), R.string.already_unlocked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mLicenseCheckerFragment.isCheckInProgress()) {
            if (mLicenseCheckerFragment.startCheck(
                    new PowerPackLicenseCheckerImpl(getActivity()))) {

                setCheckInProgress(true);
            }
        }
    }

    void setCheckInProgress(boolean inProgress) {
        mProgressBar.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);
        mAgendaView.setEnabled(!inProgress);
        mCallsView.setEnabled(!inProgress);
        mSmsView.setEnabled(!inProgress);
        mSettingsView.setEnabled(!inProgress);
        mPeopleView.setEnabled(!inProgress);
        mPowerPackView.setEnabled(!inProgress);
    }

    void markAgendaUnlocked() {
        mAgendaView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mAgendaView = (TextView) view.findViewById(R.id.agenda_unlocked);
        mCallsView = (TextView) view.findViewById(R.id.calls_unlocked);
        mSmsView = (TextView) view.findViewById(R.id.sms_unlocked);
        mSettingsView = (TextView) view.findViewById(R.id.settings_unlocked);
        mPeopleView = (TextView) view.findViewById(R.id.contacts_unlocked);
        mPowerPackView = (TextView) view.findViewById(R.id.powerpack_unlocked);
        mDownloadLink = (Button) view.findViewById(R.id.download_apks);

        mAgendaView.setOnClickListener(this);
        mCallsView.setOnClickListener(this);
        mSmsView.setOnClickListener(this);
        mSettingsView.setOnClickListener(this);
        mPeopleView.setOnClickListener(this);
        mPowerPackView.setOnClickListener(this);
        mDownloadLink.setOnClickListener(this);

        if (FeatureManagerHelper.legacyAgendaUnlocked(getActivity())) {
            mAgendaView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
        }
        if (FeatureManagerHelper.legacyCallsUnlocked(getActivity())) {
            mCallsView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
        }
        if (FeatureManagerHelper.legacySmsUnlocked(getActivity())) {
            mSmsView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
        }
        if (FeatureManagerHelper.legacySettingsUnlocked(getActivity())) {
            mSettingsView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
        }
        if (FeatureManagerHelper.legacyPeopleUnlocked(getActivity())) {
            mPeopleView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
        }
        if (FeatureManagerHelper.legacyPowerPackUnlocked(getActivity())) {
            mPowerPackView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
        }
    }

    void markPowerPackUnlocked() {
        mPowerPackView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
    }

    void markSettingsUnlocked() {
        mSettingsView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
    }

    void markContactsUnlocked() {
        mPeopleView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
    }

    void markCallsUnlocked() {
        mCallsView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
    }

    void markSmsUnlocked() {
        mSmsView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_lock_open_black_24dp, 0, 0, 0);
    }

    public void onLicenseCheckComplete() {
        mLicenseCheckerFragment.clear();
        setCheckInProgress(false);
        if (mUnlockListener != null) {
            mUnlockListener.onAppsiPluginUnlocked();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLicenseCheckerFragment.isCheckInProgress()) {
            setCheckInProgress(true);
        }
    }

    interface UnlockListener {

        void onAppsiPluginUnlocked();
    }
//
//    static class AsyncQueryHandlerImpl extends AsyncQueryHandler {
//
//        final Context mContext;
//
//        public AsyncQueryHandlerImpl(Context context, ContentResolver cr) {
//            super(cr);
//            mContext = context;
//        }
//
//        public void ensurePageEnabled(int pageType) {
//            startQuery(1, pageType,
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
//            int pageType = (int) cookie;
//            int count = cursor.getCount();
//            cursor.close();
//            if (count == 0) {
//                enablePage(pageType);
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
//            startInsert(0, null, HomeContract.Pages.CONTENT_URI, values);
//        }
//
//        private String getTitleForPageType(int pageType) {
//            int resId;
//            switch (pageType) {
//                case HomeContract.Pages.PAGE_SETTINGS:
//                    resId = R.string.settings_page_name;
//                    break;
//                case HomeContract.Pages.PAGE_SMS:
//                    resId = R.string.sms_page_name;
//                    break;
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
//    }

    public static class LicenseCheckerFragment extends Fragment {

        LicenseChecker mActiveLicenseChecker;

        AsyncTask<?, ?, ?> mTask;

        boolean isCheckInProgress() {
            return mActiveLicenseChecker != null;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }


        /**
         * Starts the check. Returns false if the plugin is not installed
         */
        public boolean startCheck(LicenseChecker licenseChecker) {
            mActiveLicenseChecker = licenseChecker;
            mTask = licenseChecker.checkAccess();
            if (mTask == null) {
                mActiveLicenseChecker = null;
                return false;
            }
            return true;
        }

        public void clear() {
            mActiveLicenseChecker = null;
            mTask = null;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mTask != null) {
                mTask.cancel(true);
            }
        }


    }

    class AgendaLicenseCheckerImpl extends LicenseChecker {

        @SuppressWarnings("SpellCheckingInspection")
        static final String CALENDAR_KEY =
                // SHA-256 of the key
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApsqS2yG4ZcX7pEtfUhhT7pZf" +
                        "hDM2ABaa/24Z8moaqAkyPhxx143kvT/4BgIiucqN+4XUHFORKQkWqGcwf9VE" +
                        "gEnQf8JksH+xfNJrzGXcV8XLpjoE5uH5slVYnjs00cWQDuCDwwd/Qyw6mHes" +
                        "Yh7T4kE+Wyb7lellowQUNg4qxN2xfHEXu/Hv6ZYrYUmzsxjx50iJZn7l+q9s" +
                        "tRW4k5IxK8ytKIX5ocN5gzk4dLOvdZ+e299qayOebkub3tJaLJlZXXjE5evG" +
                        "tjZlBxDsqT3WK1L+gyIgAHrwOB/87peoZiLjlM5CNngykHVASx6QN16rf+ew" +
                        "KWZeohypIIu6mCJPPQIDAQAB";

        protected AgendaLicenseCheckerImpl(Activity context) {
            super(context, "com.appsimobile.appsicalendar",
                    CALENDAR_KEY, FeatureManager.AGENDA_FEATURE);
        }

        @Override
        protected void onCheckComplete(String packageName) {
            if (FeatureManagerHelper.legacyAgendaUnlocked(mContext)) {
                PageHelper pageHelper = PageHelper.getInstance(getActivity());
                pageHelper.enablePageAccess(
                        HomeContract.Pages.PAGE_AGENDA, false /* do not force add */);

                markAgendaUnlocked();
                Toast.makeText(mContext, R.string.unlock_success, Toast.LENGTH_SHORT).show();
                mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_APPSI_UNLOCK,
                        AnalyticsManager.CATEGORY_PAGES, packageName);

            } else {
                Toast.makeText(mContext, R.string.unlock_error, Toast.LENGTH_SHORT).show();
            }
            onLicenseCheckComplete();
        }
    }

    class PowerPackLicenseCheckerImpl extends LicenseChecker {

        @SuppressWarnings("SpellCheckingInspection")
        static final String POWER_PACK_KEY =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqaTU3L//VwvEU78IKXmCAJfcTw28E4nn" +
                        "AzY0tRG9x+aZKkBk6f4pkvcbN1WuT4y4hWrTfEJmaP/hfvRiK6J802SNcTwBe8/du6ZY" +
                        "fdsJ1d83EQ5Hu2Q/jhRqhD332np1/IvjXkCD3AU1NgvGiBRdMN5iPYmzWluIfTSEeGjS" +
                        "CiCDXEQdHiS+1TUOk0y6WBiVuhXJ+GINhQWJZY5V84yrnv7ooQc+MrE8ieT0KcHpHrQC" +
                        "8W/3JvXHxrmX2728Z7l7fsFiCCgbV3IIw9Lxo6AAhAUbwv8MqVSdXqUMqG3dhMHmXyW1" +
                        "/xiPt5BKrGBTTNPkwCC5jFka8tr3yAJQIHsy9QIDAQAB";

        protected PowerPackLicenseCheckerImpl(Activity context) {
            super(context, "com.appsimobile.appsipowerpack", POWER_PACK_KEY,
                    FeatureManager.SETTINGS_AGENDA_FEATURE);
        }

        @Override
        protected void onCheckComplete(String packageName) {
            if (FeatureManagerHelper.legacyPowerPackUnlocked(mContext)) {
                PageHelper pageHelper = PageHelper.getInstance(getActivity());

                pageHelper.enablePageAccess(HomeContract.Pages.PAGE_AGENDA, false /* force */);
                pageHelper.enablePageAccess(HomeContract.Pages.PAGE_SETTINGS, false /* force */);
                markPowerPackUnlocked();
                Toast.makeText(mContext, R.string.unlock_success, Toast.LENGTH_SHORT).show();
                mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_APPSI_UNLOCK,
                        AnalyticsManager.CATEGORY_PAGES, packageName);

            } else {
                Toast.makeText(mContext, R.string.unlock_error, Toast.LENGTH_SHORT).show();
            }
            onLicenseCheckComplete();
        }
    }

    class SettingsLicenseCheckerImpl extends LicenseChecker {

        @SuppressWarnings("SpellCheckingInspection")
        static final String SETTINGS_KEY =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjDUgBBwfK5C6FEjoq58Hrx6VJlq05X" +
                        "ssk8mfo24E9srsC39Pna0jl0146EisBoVFlvj79XGmsDB43ilXbKFb8an5pzhwU3pf" +
                        "G+d9OewIe75Fkg+bSn86eDXpHxNWs+NT0oAM9NMBbn0DYSDbuVg73yJryRJ/R7jSVS" +
                        "PxFEasBBtRmdPjtDDLZ6hKPgicQogrq6CRE1zmUkgUKze/OWL6sl6ip5RKjZS2sw7U" +
                        "HSUk09ZsTw1oGPPQRnvVUw+dDaC462GqcHwDIWyhRSRNE4iIHMUbBFG1eFKWeVtKDK" +
                        "WX4ZVkqmchZg7tAbWzjxg2eC1EofrmVRIu5eiw/vVBH1cVuQIDAQAB";


        protected SettingsLicenseCheckerImpl(Activity context) {
            super(context, "com.appsimobile.appsisettings", SETTINGS_KEY,
                    FeatureManager.SETTINGS_FEATURE);
        }

        @Override
        protected void onCheckComplete(String packageName) {
            if (FeatureManagerHelper.legacySettingsUnlocked(mContext)) {
                PageHelper pageHelper = PageHelper.getInstance(getActivity());
                pageHelper.enablePageAccess(HomeContract.Pages.PAGE_SETTINGS, false /* force */);
                markSettingsUnlocked();
                Toast.makeText(mContext, R.string.unlock_success, Toast.LENGTH_SHORT).show();
                mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_APPSI_UNLOCK,
                        AnalyticsManager.CATEGORY_PAGES, packageName);

            } else {
                Toast.makeText(mContext, R.string.unlock_error, Toast.LENGTH_SHORT).show();
            }
            onLicenseCheckComplete();
        }
    }

    class ContactsLicenseCheckerImpl extends LicenseChecker {

        @SuppressWarnings("SpellCheckingInspection")
        static final String CONTACTS_KEY =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuMwfPu2tw4ONCpWc3P9u0FQjrg8Y" +
                        "yx9sNQHWUtBDxyeYw2zv/8eAWBEUsaeIwTy6m75Xk/+11+1cItm5aIFQe4yhI8Cj" +
                        "tIfrtuiYHrcVCpf/FR60S1KileOP8H6BmFro+JH+j17hfbwks+ijuVxDCVMy/ZG/" +
                        "avRTGMnrL8+Zm6l+AYTUv3e9EH2UfXAQmo2KTk8KwoyFe9kGHQ19C/0Kgis0zltf" +
                        "aiEILy1NErm7XKrrUUnP4IGJT2LWyn66LW/20hTUefpIRV/dA1s77ZV79wkZ8Beb" +
                        "K7mPvF7IWvNUjNuz1t1jhj7rqzK6NebQKGJYtV0LqUU4ePyn9hnunYWWGwIDAQAB";


        protected ContactsLicenseCheckerImpl(Activity context) {
            super(context, "com.appsimobile.appsicontacts", CONTACTS_KEY,
                    FeatureManager.PEOPLE_FEATURE);
        }

        @Override
        protected void onCheckComplete(String packageName) {
            if (FeatureManagerHelper.legacyPeopleUnlocked(mContext)) {
                PageHelper pageHelper = PageHelper.getInstance(getActivity());
                pageHelper.enablePageAccess(HomeContract.Pages.PAGE_PEOPLE, false /* force */);
                markContactsUnlocked();
                Toast.makeText(mContext, R.string.unlock_success, Toast.LENGTH_SHORT).show();
                mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_APPSI_UNLOCK,
                        AnalyticsManager.CATEGORY_PAGES, packageName);
            } else {
                Toast.makeText(mContext, R.string.unlock_error, Toast.LENGTH_SHORT).show();
            }
            onLicenseCheckComplete();
        }
    }

    class CallsLicenseCheckerImpl extends LicenseChecker {

        @SuppressWarnings("SpellCheckingInspection")
        static final String CALLS_KEY =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhJeN9DkEssDXlhjce6tAsHufQMic" +
                        "VAKZg+7lW5WJuQ4dNyZnnJoUcPDLFgteqGJTurjrbcgXHt1izytDF5ZkSlITCN5U" +
                        "JV/sLw7X5nwLYohP0+faa4M4xLUVnKYdkDivIa3cvuPPNxQyWSUE7D8Of8p5j6Qm" +
                        "j+6RNpQeQ/9uYrjAm3OEpcvFn7EtfEaXHvZpVAW2GxA1BfcJRCU0R+8hMNVue7M8" +
                        "PjMv36MVNOlFR+ohdB1atb7kau89nT/PLSW+suGCMTVsdH0SoeNYjjnrJp2yXJYv" +
                        "kxp7W8zQ5Ix/aso6fjIR9zmX6KPW4HT6Oopb/cqERb56HMSNtFSc7idklwIDAQAB";

        protected CallsLicenseCheckerImpl(Activity context) {
            super(context, "com.appsimobile.appsicalls", CALLS_KEY, FeatureManager.CALLS_FEATURE);
        }

        @Override
        protected void onCheckComplete(String packageName) {
            if (FeatureManagerHelper.legacyCallsUnlocked(mContext)) {
                PageHelper pageHelper = PageHelper.getInstance(getActivity());
                pageHelper.enablePageAccess(HomeContract.Pages.PAGE_CALLS, false /* force */);
                markCallsUnlocked();
                Toast.makeText(mContext, R.string.unlock_success, Toast.LENGTH_SHORT).show();
                mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_APPSI_UNLOCK,
                        AnalyticsManager.CATEGORY_PAGES, packageName);

            } else {
                Toast.makeText(mContext, R.string.unlock_error, Toast.LENGTH_SHORT).show();
            }
            onLicenseCheckComplete();
        }
    }

    class SmsLicenseCheckerImpl extends LicenseChecker {

        @SuppressWarnings("SpellCheckingInspection")
        static final String SMS_KEY =
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgKMa1O3OA4Mc6JIkMrxGG4kq1xyR" +
                        "KFwG/8tLUjAvv1IRmTbxiEp+EkfgXM/rMxKdbfh9/F7Z/J0wmwhOZgJ0JR5g9aWi" +
                        "WoaJoYOaRBKi7bgMX+qALdJhiTLEvTcx982LajW1qWtq4GkbolU/eQUrOp0kyCV5" +
                        "CeWEGL3E2nty1xypZoaOAyotk/uAaStZYdDYjP47DzyBoeLyeQMfDLT5i0LMadvd" +
                        "y0sCjfUbO1yuuXOoCwPRGtDXGTzqfmNww2sYrz8Sc2bjcaJ0hPTYeOHasigb+FqI" +
                        "+InV6DLbGecDklq5lzK3DqSudBh+FiPIPrO05Y1BK5QAYSf9swvjl7pHZQIDAQAB";

        protected SmsLicenseCheckerImpl(Activity context) {
            super(context, "com.appsimobile.appsisms", SMS_KEY, FeatureManager.SMS_FEATURE);
        }

        @Override
        protected void onCheckComplete(String packageName) {
            if (FeatureManagerHelper.legacySmsUnlocked(mContext)) {
                PageHelper pageHelper = PageHelper.getInstance(getActivity());

                pageHelper.enablePageAccess(HomeContract.Pages.PAGE_SMS, false /* force */);
                markSmsUnlocked();
                Toast.makeText(mContext, R.string.unlock_success, Toast.LENGTH_SHORT).show();
                mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_APPSI_UNLOCK,
                        AnalyticsManager.CATEGORY_PAGES, packageName);

            } else {
                Toast.makeText(mContext, R.string.unlock_error, Toast.LENGTH_SHORT).show();
            }
            onLicenseCheckComplete();
        }
    }


}
