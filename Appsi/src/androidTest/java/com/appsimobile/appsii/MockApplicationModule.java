package com.appsimobile.appsii;

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
import android.telephony.TelephonyManager;
import android.test.mock.MockContentResolver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.appsimobile.appsii.appwidget.AppWidgetIconCache;
import com.appsimobile.appsii.appwidget.AppsiiAppWidgetHost;
import com.appsimobile.appsii.compat.AppWidgetManagerCompat;
import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.compat.UserManagerCompat;
import com.appsimobile.appsii.iab.FeatureManagerHelper;
import com.appsimobile.appsii.module.home.YahooLocationChooserDialogFragment;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.appsii.plugins.IconCache;
import com.appsimobile.appsii.preference.ObfuscatedPreferences;
import com.appsimobile.appsii.preference.PreferenceHelper;

import org.mockito.Mockito;

import java.util.TimeZone;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static com.appsimobile.appsii.ThemingUtils.DEFAULT_APPSII_THEME;
import static com.appsimobile.appsii.ThemingUtils.PREF_APPSII_THEME;

/**
 * Created by nmartens on 12/01/16.
 */
@Module
public class MockApplicationModule {

    private final Application mApplication;

    public MockApplicationModule(Application app) {
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
        SharedPreferences result = Mockito.mock(SharedPreferences.class);
        Mockito.when(result.getString(PREF_APPSII_THEME, DEFAULT_APPSII_THEME))
                .thenReturn(DEFAULT_APPSII_THEME);
        return result;
    }

    @Provides
    @Singleton
    ObfuscatedPreferences provideObfuscatedPreferencess() {
        return Mockito.mock(ObfuscatedPreferences.class);
    }

    @Provides
    TimeZone provideDefaultTimeZone() {
        return TimeZone.getTimeZone("UTC");
    }

    @Provides
    @Singleton
    AnalyticsManager provideAnalyticsManager() {
        return Mockito.mock(AnalyticsManager.class);
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
    HomeItemConfiguration provideHomeItemConfiguration() {
        return Mockito.mock(HomeItemConfigurationHelper.class);
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
    FeatureManagerHelper provideFeatureManagerHelper() {
        return Mockito.mock(FeatureManagerHelper.class);
    }

    @Singleton
    @Provides
    ConnectivityManager provideConnectivityManager() {
        return Mockito.mock(ConnectivityManager.class);
    }

    @Singleton
    @Provides
    WindowManager provideWindowManager() {
        return Mockito.mock(WindowManager.class);
    }

    @Singleton
    @Provides
    InputMethodManager provideInputMethodManager() {
        return Mockito.mock(InputMethodManager.class);
    }

    @Singleton
    @Provides
    TelephonyManager provideTelephonyManager() {
        return Mockito.mock(TelephonyManager.class);
    }

    @Singleton
    @Provides
    WifiManager provideWifiManager() {
        return Mockito.mock(WifiManager.class);
    }

    @Singleton
    @Provides
    AudioManager provideAudioManager() {
        return Mockito.mock(AudioManager.class);
    }

    @Singleton
    @Provides
    AccountManager provideAccountManager() {
        return Mockito.mock(AccountManager.class);
    }

    @Singleton
    @Provides
    KeyguardManager provideKeyguardManager() {
        return Mockito.mock(KeyguardManager.class);
    }

    @Singleton
    @Provides
    UserManager provideUserManager() {
        return Mockito.mock(UserManager.class);
    }

    @Singleton
    @Provides
    ActivityManager provideActivityManager() {
        return Mockito.mock(ActivityManager.class);
    }

    @Singleton
    @Provides
    Vibrator provideVibrator() {
        return Mockito.mock(Vibrator.class);
    }

    @Singleton
    @Provides
    LauncherApps provideLauncherApps() {
        return Mockito.mock(LauncherApps.class);
    }

    @Singleton
    @Provides
    LocationManager provideLocationManager() {
        return Mockito.mock(LocationManager.class);
    }

    @Singleton
    @Provides
    ContentResolver provideContentResolver(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return new MockContentResolver(context);
        }
        return Mockito.mock(ContentResolver.class);
    }

    @Provides
    Resources provideResources(Context context) {
        return context.getResources();
    }

    @Provides
    @Singleton
    UserManagerCompat provideUserManagerCompat() {
        return Mockito.mock(UserManagerCompat.class);
    }

    @Provides
    @Singleton
    LauncherAppsCompat provideLauncherAppsCompat() {
        return Mockito.mock(LauncherAppsCompat.class);
    }

    @Provides
    @Singleton
    AppWidgetManagerCompat provideAppWidgetManagerCompat() {
        return Mockito.mock(AppWidgetManagerCompat.class);
    }

    @Provides
    @Singleton
    IconCache provideIconCache() {
        return Mockito.mock(IconCache.class);
    }

    @Provides
    @Singleton
    AppWidgetIconCache provideAppWidgetIconCache() {
        return Mockito.mock(AppWidgetIconCache.class);
    }

    @Singleton
    @Provides
    PermissionUtils providePermissionUtils() {
        return Mockito.mock(PermissionUtils.class);
    }

    @Provides
    @Singleton
    HomeItemConfigurationHelper.HomeItemConfigurationLoader provideHomeItemConfigurationLoader() {
        return Mockito.mock(HomeItemConfigurationHelper.HomeItemConfigurationLoader.class);
    }

    @Singleton
    @Provides
    YahooLocationChooserDialogFragment.LocationUpdateHelper provideLocationUpdateHelper() {
        return Mockito.mock(YahooLocationChooserDialogFragment.LocationUpdateHelper.class);
    }


}
