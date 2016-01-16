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

package com.appsimobile.appsii.module.appsisettings;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SettingsHelper {

    public static final boolean API19 =
            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    public static final boolean API18 =
            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;

    public static final boolean API17 =
            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;


    public static final String DATA_USAGE_ACTION = "_DATA_USAGE_";

    static final int bv = Build.VERSION.SDK_INT;
    final int mAirplaneToggleDrawableResourceId;
    final int mBluetoothToggleDrawableResourceId;
    final int mDataToggleDrawableResourceId;
    final int mWirelessToggleDrawableResourceId;
    final int mSoundToggleDrawableResourceId;
    private final Section[] mSections;
    private final List<SectionItem> mPickerItems;
    private final Context mContext;
    List<SectionItem> mAdditionalPickerItems;
    ConnectivityManager mConnectivityManager;

    TelephonyManager mTelephonyManager;

    AudioManager mAudioManager;

    WifiManager mWifiManager;

    public SettingsHelper(Context context, Section[] sections,
            int airplaneToggleDrawableResourceId,
            int bluetoothToggleDrawableResourceId,
            int dataToggleDrawableResourceId,
            int wirelessToggleDrawableResourceId,
            int soundToggleDrawableResourceId,
            ConnectivityManager connectivityManager,
            TelephonyManager telephonyManager,
            AudioManager audioManager,
            WifiManager wifiManager) {
        mConnectivityManager = connectivityManager;
        mTelephonyManager = telephonyManager;
        mAudioManager = audioManager;
        mWifiManager = wifiManager;
        mContext = context;
        boolean devEnabled = isDevEnabled();
        mPickerItems = new ArrayList<>();

        int count = sections.length;
        for (int i = 0; i < count; i++) {

            SectionItem[] sectionItems = sections[i].mItems;

            int itemCount = sectionItems.length;
            for (int j = 0; j < itemCount; j++) {
                SectionItem sectionItem = sectionItems[j];
                updateSectionItemsAvailability(context, sectionItem, devEnabled);
            }
        }
        mSections = sections;
        mAirplaneToggleDrawableResourceId = airplaneToggleDrawableResourceId;
        mBluetoothToggleDrawableResourceId = bluetoothToggleDrawableResourceId;
        mDataToggleDrawableResourceId = dataToggleDrawableResourceId;
        mWirelessToggleDrawableResourceId = wirelessToggleDrawableResourceId;
        mSoundToggleDrawableResourceId = soundToggleDrawableResourceId;

        addAdditionalPickerItems(context, mPickerItems);
    }

    public boolean isDevEnabled() {
        if (API17) {
            return Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
        }
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
    }

    private void updateSectionItemsAvailability(Context context, SectionItem sectionItem,
            boolean devEnabled) {
        String id = sectionItem.mActivityAction;

        if (id.equals(Settings.ACTION_NFCSHARING_SETTINGS)) {
            PackageManager packageManager = context.getPackageManager();
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                sectionItem.mShown = false;
            }
        }
        if (id.equals(Settings.ACTION_NFC_SETTINGS)) {
            PackageManager packageManager = context.getPackageManager();
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                sectionItem.mShown = false;
            }
        }
        if (id.equals(Settings.ACTION_DATA_ROAMING_SETTINGS)) {
            if (!hasData()) {
                sectionItem.mShown = false;
            }
        }
        if (id.equals(Settings.ACTION_NFC_PAYMENT_SETTINGS)) {
            if (!API19) {
                sectionItem.mShown = false;
            } else {
                PackageManager packageManager = context.getPackageManager();
                if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                    sectionItem.mShown = false;
                } else {
                    // Only show if we have the HCE feature
                    if (!packageManager.hasSystemFeature(
                            PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
                        sectionItem.mShown = false;
                    }
                }

            }
        }
        if (id.equals(Settings.ACTION_CAPTIONING_SETTINGS)) {
            if (!API19) {
                sectionItem.mShown = false;
            }
        }
        if (id.equals(Settings.ACTION_DREAM_SETTINGS)) {
            if (!API18) {
                sectionItem.mShown = false;
            }
        }
        if (id.equals(Settings.ACTION_PRINT_SETTINGS)) {
            if (!API19) {
                sectionItem.mShown = false;
            }
        }
        if (id.equals(
                Settings.ACTION_WIFI_SETTINGS)) {// Remove WiFi Settings if WiFi service is not
            // available.
            if (!hasWifi()) {
                sectionItem.mShown = false;
            }

        } else if (id.equals(Settings.ACTION_BLUETOOTH_SETTINGS)) {
            if (!hasBluetooth()) {
                sectionItem.mShown = false;
            }

        } else if (id.equals(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) {
            if (!devEnabled) {
                sectionItem.mShown = false;
            }

        }
    }

    private void addAdditionalPickerItems(Context context, List<SectionItem> pickerItems) {
        int count = mAdditionalPickerItems.size();
        for (int i = 0; i < count; i++) {
            SectionItem sectionItem = mAdditionalPickerItems.get(i);
            updateSectionItemsAvailability(context, sectionItem, false);
            mPickerItems.add(sectionItem);
        }
    }

    public boolean hasData() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    public boolean hasWifi() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    public boolean hasBluetooth() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    public int getIconResourceIdForToggle(String toggleName) {
        if (toggleName.equals("AIRPLANE_TOGGLE")) {
            return mAirplaneToggleDrawableResourceId;
        } else if (toggleName.equals("BT_TOGGLE")) {
            return mBluetoothToggleDrawableResourceId;
        } else if (toggleName.equals("DATA_TOGGLE")) {
            return mDataToggleDrawableResourceId;
        } else if (toggleName.equals("WIFI_TOGGLE")) {
            return mWirelessToggleDrawableResourceId;
        } else if (toggleName.equals("AUDIO_TOGGLE")) {
            return mSoundToggleDrawableResourceId;
        }
        return 0;
    }

    public SectionItem findSectionItem(String activityAction) {
        int count = mSections.length;
        for (int i = 0; i < count; i++) {
            Section section = mSections[i];
            SectionItem[] items = section.mItems;
            int itemCount = items.length;
            for (int j = 0; j < itemCount; j++) {
                SectionItem sectionItem = items[j];
                if (sectionItem.mActivityAction.equals(activityAction)) {
                    return sectionItem;
                }
            }
        }
        count = mPickerItems.size();
        for (int i = 0; i < count; i++) {
            SectionItem sectionItem = mPickerItems.get(i);
            if (sectionItem.mActivityAction.equals(activityAction)) {
                return sectionItem;
            }
        }
        return null;
    }

    public synchronized Section[] getSections() {
        return mSections;
    }

    public synchronized List<SectionItem> getPickerSectionItems() {
        return mPickerItems;
    }

    public boolean isAirplaneModeEnabled() {
        if (API17) {
            return Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        }
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

    public void setAirplaneModeEnabled(boolean enable) {
        if (API17) {
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                    enable ? 1 : 0);
        } else {
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON,
                    enable ? 1 : 0);
        }
    }

    public boolean isSoundEnabled() {
        AudioManager manager = mAudioManager;

        int ringermode = manager.getRingerMode();
        if (ringermode == AudioManager.RINGER_MODE_SILENT ||
                ringermode == AudioManager.RINGER_MODE_VIBRATE) {
            return false;
        }
        return true;
    }

    public void setSoundEnabled(boolean enable) {
        AudioManager manager = mAudioManager;
        manager.setRingerMode(
                enable ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_VIBRATE);

    }

    public boolean isWiFiEnabled() {
        WifiManager wifiManager = mWifiManager;
        return wifiManager.isWifiEnabled();
    }

    public void setWiFiEnabled(boolean enable) {
        WifiManager wifiManager = mWifiManager;
        wifiManager.setWifiEnabled(enable);
    }

    public boolean isBluetoothEnabled() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
        /*
        if (LauncherApplication.API17) {
            return Settings.Global.getInt(context.getContentResolver(),
            Settings.Global.BLUETOOTH_ON, 0) == 1;
        }
        return Settings.System.getInt(context.getContentResolver(), Settings.System.BLUETOOTH_ON,
         0) == 1;
        */
    }

    public void setBluetoothEnabled(boolean enable) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (enable) {
            adapter.enable();
        } else {
            adapter.disable();
        }
    }

    public boolean setDataEnabled(Context context, boolean ON) {

        try {
            if (bv == Build.VERSION_CODES.FROYO) {
                Method dataConnSwitchmethod;
                Class<?> telephonyManagerClass;
                Object iTelephonyStub;
                Class<?> iTelephonyClass;

                TelephonyManager telephonyManager = mTelephonyManager;

                telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
                Method getITelephonyMethod =
                        telephonyManagerClass.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                iTelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                iTelephonyClass = Class.forName(iTelephonyStub.getClass().getName());

                if (ON) {
                    dataConnSwitchmethod =
                            iTelephonyClass.getDeclaredMethod("enableDataConnectivity");
                } else {
                    dataConnSwitchmethod =
                            iTelephonyClass.getDeclaredMethod("disableDataConnectivity");
                }
                dataConnSwitchmethod.setAccessible(true);
                dataConnSwitchmethod.invoke(iTelephonyStub);

            } else {
                //log.i("App running on Ginger bread+");
                final ConnectivityManager conman = mConnectivityManager;
                final Class<?> conmanClass = Class.forName(conman.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(conman);
                final Class<?> iConnectivityManagerClass =
                        Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod =
                        iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled",
                                Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(iConnectivityManager, ON);
            }
            return true;
        } catch (Exception e) {
            Log.e("SettingsHelper", "error turning on/off data", e);
            return false;
        }

    }

    public boolean isDataEnabled() {

        try {
            if (bv == Build.VERSION_CODES.FROYO) {
                Method getDataConnSwitchmethod;
                Class<?> class_telephonyManager;
                Object iTelephonyStub;
                Class<?> class_ITelephony;

                TelephonyManager telephonyManager = mTelephonyManager;

                class_telephonyManager = telephonyManager.getClass();
                Method getITelephonyMethod =
                        class_telephonyManager.getDeclaredMethod("getITelephony");
                getITelephonyMethod.setAccessible(true);
                iTelephonyStub = getITelephonyMethod.invoke(telephonyManager);
                class_ITelephony = iTelephonyStub.getClass();

                getDataConnSwitchmethod = class_ITelephony
                        .getDeclaredMethod("isDataConnectivityPossible()");
                getDataConnSwitchmethod.setAccessible(true);
                return Boolean.TRUE.equals(getDataConnSwitchmethod.invoke(iTelephonyStub));

            } else {
                //log.i("App running on Ginger bread+");
                final ConnectivityManager conman = mConnectivityManager;
                final Class<?> conmanClass = Class.forName(conman.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(conman);
                final Class<?> iConnectivityManagerClass =
                        Class.forName(iConnectivityManager.getClass().getName());
                final Method getMobileDataEnabledMethod =
                        iConnectivityManagerClass.getDeclaredMethod("getMobileDataEnabled");
                getMobileDataEnabledMethod.setAccessible(true);
                Boolean result = (Boolean) getMobileDataEnabledMethod.invoke(iConnectivityManager);
                return result.booleanValue();
            }
        } catch (Exception e) {
            Log.w("SettingsHelper", "error getting data status", e);
            return false;
        }

    }

    public static class Section {

        final int mNameResourceId;

        final SectionItem[] mItems;

        public Section(int resourceId, SectionItem[] items) {
            mItems = items;
            mNameResourceId = resourceId;
        }

    }

    public static class SectionItem {

        final String mActivityAction;
        final int mTitleResourceId;
        final int mIconResourceId;
        volatile boolean mShown = true;

        public SectionItem(String action, int title, int icon) {
            mActivityAction = action;
            mTitleResourceId = title;
            mIconResourceId = icon;
        }
    }

    public static class Builder {


        final int mAirplaneToggleDrawableResourceId;
        final int mBluetoothToggleDrawableResourceId;
        final int mDataToggleDrawableResourceId;
        final int mWirelessToggleDrawableResourceId;
        final int mSoundToggleDrawableResourceId;
        final ConnectivityManager mConnectivityManager;
        final TelephonyManager mTelephonyManager;
        final AudioManager mAudioManager;
        final WifiManager mWifiManager;
        private final Section[] mSections;

        public Builder(Section[] sections,
                int airplaneToggleDrawableResourceId,
                int bluetoothToggleDrawableResourceId,
                int dataToggleDrawableResourceId,
                int wirelessToggleDrawableResourceId,
                int soundToggleDrawableResourceId,
                ConnectivityManager connectivityManager,
                TelephonyManager telephonyManager,
                AudioManager audioManager,
                WifiManager wifiManager) {
            mSections = sections;
            mAirplaneToggleDrawableResourceId = airplaneToggleDrawableResourceId;
            mBluetoothToggleDrawableResourceId = bluetoothToggleDrawableResourceId;
            mDataToggleDrawableResourceId = dataToggleDrawableResourceId;
            mWirelessToggleDrawableResourceId = wirelessToggleDrawableResourceId;
            mSoundToggleDrawableResourceId = soundToggleDrawableResourceId;
            mConnectivityManager = connectivityManager;
            mTelephonyManager = telephonyManager;
            mAudioManager = audioManager;
            mWifiManager = wifiManager;
        }

        public SettingsHelper build(Context context) {
            return new SettingsHelper(context,
                    mSections,
                    mAirplaneToggleDrawableResourceId,
                    mBluetoothToggleDrawableResourceId,
                    mDataToggleDrawableResourceId,
                    mWirelessToggleDrawableResourceId,
                    mSoundToggleDrawableResourceId,
                    mConnectivityManager,
                    mTelephonyManager,
                    mAudioManager,
                    mWifiManager);
        }
    }

}