package com.appsimobile.appsii.dagger;

import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.Application;
import android.app.KeyguardManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.UserManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.AppsiiUtils;
import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.LoaderManagerImpl;
import com.appsimobile.appsii.appwidget.AppWidgetIconCache;
import com.appsimobile.appsii.appwidget.AppsiiAppWidgetHost;
import com.appsimobile.appsii.appwidget.Utilities;
import com.appsimobile.appsii.compat.AppWidgetManagerCompat;
import com.appsimobile.appsii.compat.AppWidgetManagerCompatV16;
import com.appsimobile.appsii.compat.AppWidgetManagerCompatVL;
import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.compat.LauncherAppsCompatV16;
import com.appsimobile.appsii.compat.LauncherAppsCompatVL;
import com.appsimobile.appsii.compat.UserManagerCompat;
import com.appsimobile.appsii.compat.UserManagerCompatV16;
import com.appsimobile.appsii.compat.UserManagerCompatV17;
import com.appsimobile.appsii.compat.UserManagerCompatVL;
import com.appsimobile.appsii.iab.FeatureManager;
import com.appsimobile.appsii.iab.FeatureManagerFactory;
import com.appsimobile.appsii.iab.FeatureManagerHelper;
import com.appsimobile.appsii.module.home.YahooLocationChooserDialogFragment;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper
        .HomeItemConfigurationLoader;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.appsii.plugins.IconCache;
import com.appsimobile.appsii.preference.ObfuscatedPreferences;
import com.appsimobile.appsii.preference.PreferenceHelper;
import com.google.android.vending.licensing.AESObfuscator;

import java.util.TimeZone;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by nmartens on 24/11/15.
 */
@Module
public class ApplicationModule {

    private static final byte[] SALT =
            "http://developer.android.com/google/play/billing/billing_reference.html".getBytes();


    private final Application mApplication;

    public ApplicationModule(Application app) {
        this.mApplication = app;
    }

    @Singleton
    @Provides
    Context provideApplicationContext() {
        return mApplication;
    }

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mApplication);
    }

    @Provides
    @Singleton
    ObfuscatedPreferences provideObfuscatedPreferencess(SharedPreferences preferences) {
        AESObfuscator obfuscator = new AESObfuscator(SALT,
                BuildConfig.APPLICATION_ID, Settings.Secure.ANDROID_ID);

        return new ObfuscatedPreferences(preferences, obfuscator);
    }

    @Provides
    TimeZone provideDefaultTimeZone() {
        return TimeZone.getDefault();
    }

    @Provides
    @Singleton
    AnalyticsManager provideAnalyticsManager() {
        return new AnalyticsManager(mApplication);
    }

    @Provides
    @Singleton
    LoaderManagerImpl provideLoaderManagerImpl() {
        return new LoaderManagerImpl("Appsi", mApplication, false);
    }

    @Provides
    @Singleton
    PreferenceHelper providePreferenceHelper(Context context, SharedPreferences preferences) {
        return new PreferenceHelper(context, preferences);
    }

    @Provides
    @Singleton
    AppsiiAppWidgetHost provideAppsiiAppWidgetHost(Context context) {
        return new AppsiiAppWidgetHost(context, AppsiApplication.APPWIDGET_HOST_ID);
    }

    @Provides
    @Singleton
    AppWidgetManager provideAppWidgetManager(Context context) {
        return AppWidgetManager.getInstance(context);
    }

    @Provides
    @Singleton
    AppWidgetHost provideAppWidgetHost(AppsiiAppWidgetHost host) {
        return host;
    }

    @Provides
    @Singleton
    HomeItemConfiguration provideHomeItemConfiguration(
            Context context, HomeItemConfigurationLoader l) {

        return new HomeItemConfigurationHelper(context, l);
    }

    @Provides
    PackageManager providePackageManager(Context context) {
        return context.getPackageManager();
    }

    @Provides
    @Singleton
    HomeItemConfigurationHelper provideHomeItemConfigurationHelper(HomeItemConfiguration c) {
        return (HomeItemConfigurationHelper) c;
    }

    @Provides
    @Singleton
    FeatureManagerHelper provideFeatureManagerHelper(ObfuscatedPreferences prefs) {
        return new FeatureManagerHelper(prefs);
    }

    @Provides
    ConnectivityManager provideConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    WindowManager provideWindowManager(Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    @Provides
    InputMethodManager provideInputMethodManager(Context context) {
        return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Provides
    TelephonyManager provideTelephonyManager(Context context) {
        return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Provides
    WifiManager provideWifiManager(Context context) {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @Provides
    AudioManager provideAudioManager(Context context) {
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Provides
    AccountManager provideAccountManager(Context context) {
        return (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
    }

    @Provides
    KeyguardManager provideKeyguardManager(Context context) {
        return (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Provides
    UserManager provideUserManager(Context context) {
        return (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Provides
    ActivityManager provideActivityManager(Context context) {
        return (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Provides
    Vibrator provideVibrator(Context context) {
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Provides
    LauncherApps provideLauncherApps(Context context) {
        return (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
    }

    @Provides
    LocationManager provideLocationManager(Context context) {
        return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Provides
    ContentResolver provideContentResolver(Context context) {
        return context.getContentResolver();
    }

    @Provides
    Resources provideResources(Context context) {
        return context.getResources();
    }

    @Provides
    @Singleton
    UserManagerCompat provideUserManagerCompat(Context context) {
        if (Utilities.isLmpOrAbove()) {
            return new UserManagerCompatVL(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return new UserManagerCompatV17();
        } else {
            return new UserManagerCompatV16();
        }
    }

    @Provides
    @Singleton
    LauncherAppsCompat provideLauncherAppsCompat(Context context, AppsiiUtils appsiiUtils) {
        if (Utilities.isLmpOrAbove()) {
            return new LauncherAppsCompatVL(appsiiUtils);
        } else {
            return new LauncherAppsCompatV16(context);
        }
    }

    @Provides
    @Singleton
    FeatureManager provideFeatureManager(Context context) {
        return FeatureManagerFactory.getFeatureManager(context);
    }

    @Provides
    @Singleton
    AppWidgetManagerCompat provideAppWidgetManagerCompat(Context context, AppWidgetManager awm,
            PackageManager pm, UserManager um) {
        if (Utilities.isLmpOrAbove()) {
            return new AppWidgetManagerCompatVL(context, awm, pm, um);
        } else {
            return new AppWidgetManagerCompatV16(context.getApplicationContext(), awm);
        }
    }



    @Provides
    @Singleton
    IconCache provideIconCache(ActivityManager activityManager) {
        return new IconCache(activityManager);
    }

    @Provides
    @Singleton
    AppWidgetIconCache provideAppWidgetIconCache(Context context, UserManagerCompat umc,
            LauncherAppsCompat lac) {
        return new AppWidgetIconCache(context, umc, lac);
    }

    @Provides
    YahooLocationChooserDialogFragment.LocationUpdateHelper provideLocationUpdateHelper(
            SharedPreferences p, PermissionUtils pu, LocationManager lm) {
        return new YahooLocationChooserDialogFragment.DefaultLocationUpdate(p, pu, lm);
    }

}
