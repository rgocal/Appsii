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

package com.appsimobile.appsii.module.calls;

import android.annotation.SuppressLint;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.appsimobile.appsii.module.BaseContactInfo;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.provider.ContactsContract.CommonDataKinds;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class CallLogLoader extends AsyncTaskLoader<List<CallLogEntry>> {

    public static final String UNKNOWN_NUMBER = "-1";

    public static final String PRIVATE_NUMBER = "-2";

    public static final String PAYPHONE_NUMBER = "-3";

    static final boolean LOGD = false;

    List<CallLogEntry> mCallLogEntries;

    ContentObserver mCallLogObserver;

    PhoneNumberUtil mPhoneNumberUtil = PhoneNumberUtil.getInstance();

    public CallLogLoader(Context context) {
        super(context);
    }

    public static String getCountry(Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getSimState() == TelephonyManager.SIM_STATE_ABSENT) {
            return Locale.getDefault().getCountry();
        }
        return tm.getSimCountryIso();
    }

    public static int getPresentationTypeCompat(Cursor cursor) {
        String phone = cursor.getString(CallLogQuery.NUMBER);
        if (phone == null) phone = PRIVATE_NUMBER;
        return toPresentationTypeCompat(phone);

    }

    // In this method we suppress the inlined api as we just return a constant we use
    // later on. Be careful with adding stuff here
    @SuppressLint("InlinedApi")
    private static int toPresentationTypeCompat(@NonNull String phone) {
        switch (phone) {
            case PRIVATE_NUMBER:
                return CallLog.Calls.PRESENTATION_RESTRICTED;
            case UNKNOWN_NUMBER:
                return CallLog.Calls.PRESENTATION_UNKNOWN;
            case PAYPHONE_NUMBER:
                return CallLog.Calls.PRESENTATION_PAYPHONE;
            default:
                return CallLog.Calls.PRESENTATION_ALLOWED;
        }
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<CallLogEntry> apps) {
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(apps);
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override
    public List<CallLogEntry> loadInBackground() {
        // Retrieve all known applications.

        final Context context = getContext();

        SimpleArrayMap<String, BaseContactInfo> contactsByNumber = loadContactsByNumber(context);


        Uri baseUri;
//        baseUri = CallLog.Calls.CONTENT_URI.buildUpon().
//                appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, "50").
//                build();
        baseUri = CallLog.Calls.CONTENT_URI;

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        Cursor cursor = context.getContentResolver().query(baseUri,
                CallLogQuery.PROJECTION, null, null,
                CallLog.Calls.DEFAULT_SORT_ORDER);

        if (cursor == null) {
            return new ArrayList<>();
        }

        Phonenumber.PhoneNumber recycle = new Phonenumber.PhoneNumber();
        StringBuilder reuse = new StringBuilder();

        try {
            // Create corresponding array of entries and load their labels.
            List<CallLogEntry> entries = new ArrayList<>(9);

            String country = getCountry(context).toUpperCase();

            // We need to remember three things, to be able to group the calls
            // 1. the last number, repetitive calls to and from the same number
            //    are grouped to one item
            // 2. We only group events on a single day
            // 3. If the same number is encountered on the same day, we simply add
            //    that occurrence to the entry
            String lastNumber = null;
            CallLogEntry lastEntry = null;
            int lastDay = 0;

            while (cursor.moveToNext()) {

                // first get the values with which we can determine if we can
                // group with the the previous entry
                String phoneNumber = cursor.getString(CallLogQuery.NUMBER);
                long timeMillis = cursor.getLong(CallLogQuery.DATE);
                int julianDay = Time.getJulianDay(timeMillis, 0);

                String formatted = formatNumber(recycle, reuse, phoneNumber, country);
                int callType = cursor.getInt(CallLogQuery.CALL_TYPE);

                // If we can group, simply add the occurrence to the previous entry
                if (formatted != null && formatted.equals(lastNumber) && lastDay == julianDay) {
                    lastEntry.addCallType(callType);
                    continue;
                }

                // If we reach this point create the entry and populate it.
                CallLogEntry entry = new CallLogEntry();


                // remember the last values
                lastEntry = entry;
                lastNumber = formatted;
                lastDay = julianDay;

                // populate the entity
                entry.addCallType(callType);
                entry.mNumber = phoneNumber;
                entry.mFormattedNumber = formatted;
                entry.mMillis = timeMillis;
                entry.mJulianDay = julianDay;

                entry.mBaseContactInfo = contactsByNumber.get(entry.mFormattedNumber);
                if (entry.mBaseContactInfo == null) {
                    entry.mBaseContactInfo = contactsByNumber.get(entry.mNumber);
                }


                populateEntry(entry, cursor, recycle);

                addFormattedNumbers(entry, recycle, reuse, entry.mNumber, country);

                entries.add(entry);

//                if (entries.size() >= 9) break;
            }
            return entries;
        } finally {
            cursor.close();
        }
    }

    private SimpleArrayMap<String, BaseContactInfo> loadContactsByNumber(Context context) {
        SimpleArrayMap<String, BaseContactInfo> result = new SimpleArrayMap<>();
        Cursor c = context.getContentResolver().query(
                CommonDataKinds.Phone.CONTENT_URI,
                ContactsByNumberQuery.PROJECTION,
                null,
                null,
                null);

        while (c.moveToNext()) {
            String normalizedNumber = c.getString(ContactsByNumberQuery.NORMALIZED_NUMBER);
            String plainNumber = c.getString(ContactsByNumberQuery.NUMBER);

            if (normalizedNumber == null && plainNumber == null) continue;
            if (normalizedNumber != null && result.containsKey(normalizedNumber)) continue;
            if (plainNumber != null && result.containsKey(plainNumber)) continue;

            BaseContactInfo info = new BaseContactInfo();
            info.mContactId = c.getLong(ContactsByNumberQuery.CONTACT_ID);
            info.mLookupKey = c.getString(ContactsByNumberQuery.LOOKUP_KEY);
            info.mContactLookupUri =
                    ContactsContract.Contacts.getLookupUri(info.mContactId, info.mLookupKey);

            info.mDisplayName = c.getString(ContactsByNumberQuery.DISPLAY_NAME);
            info.mDisplayNameSource = c.getInt(ContactsByNumberQuery.DISPLAY_NAME_SOURCE);
            info.mPhotoUri = c.getString(ContactsByNumberQuery.PHOTO_URI);
            info.mStarred = c.getInt(ContactsByNumberQuery.STARRED) == 1;

            result.put(normalizedNumber, info);
            if (!TextUtils.equals(plainNumber, normalizedNumber)) {
                result.put(plainNumber, info);
            }

        }
        c.close();
        return result;
    }

    private String formatNumber(Phonenumber.PhoneNumber recycle, StringBuilder reuse, String number,
            String country) {
        if (TextUtils.isEmpty(number)) return null;
        recycle.clear();
        reuse.setLength(0);

        try {
            mPhoneNumberUtil.parse(number, country, recycle);
            mPhoneNumberUtil.format(recycle, PhoneNumberUtil.PhoneNumberFormat.E164, reuse);
        } catch (NumberParseException e) {
            Log.w("CallLogLoader", "error formatting nr", e);
            return number;
        }
        return reuse.toString();

    }

    private void populateEntry(CallLogEntry entry, Cursor cursor,
            Phonenumber.PhoneNumber number) {

        Context context = getContext();

        entry.mId = cursor.getLong(CallLogQuery.ID);
        entry.mIsRead = cursor.getInt(CallLogQuery.IS_READ) == 1;
        entry.mCachedName = cursor.getString(CallLogQuery.CACHED_NAME);
        PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
        entry.mGeoCodedLocation = geocoder.getDescriptionForNumber(number, Locale.getDefault());

        int presentationType = getPresentationType(cursor);
        entry.mPrivateNumner = isPrivateNumber(presentationType);

        int numberType = cursor.getInt(CallLogQuery.CACHED_NUMBER_TYPE);
        String numberLabel = cursor.getString(CallLogQuery.CACHED_NUMBER_LABEL);

        entry.mNumberTypeLabel = CommonDataKinds.Phone.getTypeLabel(
                context.getResources(), numberType, numberLabel);

        // TODO: we could add the time of the call to the existing entry
        if (LOGD) Log.d("CallLogLoader", "Checking nr: " + entry.mNumber);

    }

    private void addFormattedNumbers(CallLogEntry entry, Phonenumber.PhoneNumber recycle,
            StringBuilder reuse, String number, String country) {

        if (TextUtils.isEmpty(number)) return;

        try {
            int localCountry = mPhoneNumberUtil.getCountryCodeForRegion(country);

            recycle.clear();
            mPhoneNumberUtil.parse(number, country, recycle);
            reuse.setLength(0);
            mPhoneNumberUtil.format(recycle, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL,
                    reuse);
            entry.mNumberInternational = reuse.toString();
            entry.mCanRenderAsNational = recycle.getCountryCode() == localCountry;

            recycle.clear();
            mPhoneNumberUtil.parse(number, country, recycle);
            reuse.setLength(0);
            mPhoneNumberUtil.format(recycle, PhoneNumberUtil.PhoneNumberFormat.NATIONAL, reuse);
            entry.mNumberNational = reuse.toString();

            recycle.clear();
            mPhoneNumberUtil.parse(number, country, recycle);
            reuse.setLength(0);
            mPhoneNumberUtil.format(recycle, PhoneNumberUtil.PhoneNumberFormat.RFC3966, reuse);
            entry.mNumberRfc3966 = reuse.toString();
        } catch (NumberParseException e) {
            Log.w("CallLogLoader", "error formatting nr", e);
        }

    }

    private int getPresentationType(Cursor cursor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return getPresentationTypeV19(cursor);
        }
        return getPresentationTypeCompat(cursor);
    }

    boolean isPrivateNumber(int presentationType) {
        return presentationType != CallLog.Calls.PRESENTATION_ALLOWED;
    }

    private int getPresentationTypeV19(Cursor cursor) {
        return cursor.getInt(CallLogQuery.NUMBER_PRESENTATION);
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<CallLogEntry> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(List<CallLogEntry> apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        List<CallLogEntry> oldApps = mCallLogEntries;
        mCallLogEntries = apps;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(apps);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mCallLogEntries != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mCallLogEntries);
        }

        // Start watching for changes in the app data.
        if (mCallLogObserver == null) {
            mCallLogObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    onChange(selfChange, null);
                }

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    onContentChanged();
                }
            };
            getContext().getContentResolver()
                    .registerContentObserver(CallLog.Calls.CONTENT_URI, true, mCallLogObserver);
        }

        // Has something interesting in the configuration changed since we
        // last built the app list?

        if (takeContentChanged() || mCallLogEntries == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (mCallLogEntries != null) {
            //onReleaseResources(mCallLogEntries);
            //mCallLogEntries = null;
        }

        // Stop monitoring for changes.
        if (mCallLogObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mCallLogObserver);
            mCallLogObserver = null;
        }
    }

    static class ContactsByNumberQuery {

        final static String[] PROJECTION = new String[]{
                CommonDataKinds.Phone.CONTACT_ID,
                CommonDataKinds.Phone.NUMBER,
                CommonDataKinds.Phone.NORMALIZED_NUMBER,

                CommonDataKinds.Phone.DISPLAY_NAME,
                CommonDataKinds.Phone.PHONETIC_NAME,
                CommonDataKinds.Phone.CONTACT_PRESENCE,
                CommonDataKinds.Phone.PHOTO_ID,
                CommonDataKinds.Phone.LOOKUP_KEY,
                CommonDataKinds.Phone.PHOTO_URI,
                CommonDataKinds.Phone.DISPLAY_NAME_SOURCE,
                CommonDataKinds.Phone.STARRED,
        };

        final static int CONTACT_ID = 0;

        final static int NUMBER = 1;

        final static int NORMALIZED_NUMBER = 2;

        final static int DISPLAY_NAME = 3;

        final static int PHONETIC_NAME = 4;

        final static int CONTACT_PRESENCE = 5;

        final static int PHOTO_ID = 6;

        final static int LOOKUP_KEY = 7;

        final static int PHOTO_URI = 8;

        final static int DISPLAY_NAME_SOURCE = 9;

        final static int STARRED = 10;
    }


}