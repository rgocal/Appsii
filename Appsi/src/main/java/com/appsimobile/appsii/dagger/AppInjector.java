package com.appsimobile.appsii.dagger;

import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.appsimobile.appsii.AboutActivity;
import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.AppsiServiceStatusView;
import com.appsimobile.appsii.BitmapUtils;
import com.appsimobile.appsii.BootCompleteReceiver;
import com.appsimobile.appsii.ForceSyncService;
import com.appsimobile.appsii.LookAndFeelActivity;
import com.appsimobile.appsii.MainActivity;
import com.appsimobile.appsii.PatternRelativeLayout;
import com.appsimobile.appsii.PopupLayer;
import com.appsimobile.appsii.RequestPermissionActivity;
import com.appsimobile.appsii.SidebarContext;
import com.appsimobile.appsii.SidebarHotspot;
import com.appsimobile.appsii.compat.AppWidgetManagerCompat;
import com.appsimobile.appsii.firstrun.FirstRunDoneFragment;
import com.appsimobile.appsii.firstrun.FirstRunLocationFragment;
import com.appsimobile.appsii.firstrun.FirstRunSettingsFragment;
import com.appsimobile.appsii.firstrun.FirstRunWelcomeFragment;
import com.appsimobile.appsii.hotspotmanager.ManageHotspotsActivity;
import com.appsimobile.appsii.hotspotmanager.ReorderController;
import com.appsimobile.appsii.module.ParallaxListViewHeader;
import com.appsimobile.appsii.module.apps.ReorderAppsFragment;
import com.appsimobile.appsii.module.appsiagenda.AgendaDaysLoader;
import com.appsimobile.appsii.module.appsiagenda.AgendaLoader;
import com.appsimobile.appsii.module.calls.CallLogLoader;
import com.appsimobile.appsii.module.home.AbsWeatherViewHolder;
import com.appsimobile.appsii.module.home.ClockFragment;
import com.appsimobile.appsii.module.home.HomeAdapter;
import com.appsimobile.appsii.module.home.HomeEditorActivity;
import com.appsimobile.appsii.module.home.HomePageController;
import com.appsimobile.appsii.module.home.IntentEditorFragment;
import com.appsimobile.appsii.module.home.IntentViewHolder;
import com.appsimobile.appsii.module.home.ProfileImageFragment;
import com.appsimobile.appsii.module.home.WeatherFragment;
import com.appsimobile.appsii.module.home.YahooLocationChooserDialogFragment;
import com.appsimobile.appsii.module.home.appwidget.WidgetChooserActivity;
import com.appsimobile.appsii.module.home.homepagesmanager.ManageHomePagesActivity;
import com.appsimobile.appsii.module.home.provider.HomeContentProvider;
import com.appsimobile.appsii.module.weather.WeatherActivity;
import com.appsimobile.appsii.module.weather.WeatherLoadingService;
import com.appsimobile.appsii.module.weather.WeatherSyncAdapter;
import com.appsimobile.appsii.module.weather.WeatherUtils;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.appsii.processmon.ProcessMonitorFragment;
import com.appsimobile.appsii.theme.CustomThemeActivity;
import com.appsimobile.view.ParallaxListView;

/**
 * Created by nmartens on 25/11/15.
 */
public class AppInjector {

    static ApplicationComponent sApplicationComponent;

    public static void inject(MainActivity activity) {
        sApplicationComponent.inject(activity);
    }

    public static void inject(AboutActivity.AboutPreferenceFragment f) {
        sApplicationComponent.inject(f);
    }

    public static void inject(SidebarContext f) {
        sApplicationComponent.inject(f);
    }

    public static void inject(AbsWeatherViewHolder absWeatherViewHolder) {
        sApplicationComponent.inject(absWeatherViewHolder);
    }

    public static void setComponent(ApplicationComponent applicationComponent) {
        sApplicationComponent = applicationComponent;
    }

    public static void inject(HomeAdapter homeAdapter) {
        sApplicationComponent.inject(homeAdapter);
    }

    public static void inject(HomeEditorActivity.ActionModeCallbackImpl actionModeCallback) {
        sApplicationComponent.inject(actionModeCallback);
    }

    public static void inject(HomeEditorActivity homeEditorActivity) {
        sApplicationComponent.inject(homeEditorActivity);
    }

    public static void inject(WidgetChooserActivity widgetChooserActivity) {
        sApplicationComponent.inject(widgetChooserActivity);
    }

    public static void inject(HomeContentProvider.HomeDatabaseHelper homeDatabaseHelper) {
        sApplicationComponent.inject(homeDatabaseHelper);
    }

    public static void inject(AppWidgetManagerCompat appWidgetManagerCompat) {
        sApplicationComponent.inject(appWidgetManagerCompat);
    }

    public static void inject(AppsiServiceStatusView appsiServiceStatusView) {
        sApplicationComponent.inject(appsiServiceStatusView);
    }

    public static void inject(BootCompleteReceiver bootCompleteReceiver) {
        sApplicationComponent.inject(bootCompleteReceiver);
    }

    public static void inject(FirstRunWelcomeFragment firstRunWelcomeFragment) {
        sApplicationComponent.inject(firstRunWelcomeFragment);
    }

    public static void inject(FirstRunSettingsFragment firstRunSettingsFragment) {
        sApplicationComponent.inject(firstRunSettingsFragment);
    }

    public static void inject(FirstRunLocationFragment firstRunLocationFragment) {
        sApplicationComponent.inject(firstRunLocationFragment);
    }

    public static void inject(
            YahooLocationChooserDialogFragment yahooLocationChooserDialogFragment) {
        sApplicationComponent.inject(yahooLocationChooserDialogFragment);
    }

    public static void inject(FirstRunDoneFragment firstRunDoneFragment) {
        sApplicationComponent.inject(firstRunDoneFragment);
    }

    public static void inject(WeatherFragment weatherFragment) {
        sApplicationComponent.inject(weatherFragment);
    }

    public static void inject(HomePageController controller) {
        sApplicationComponent.inject(controller);
    }

    public static void inject(ManageHotspotsActivity manageHotspotsActivity) {
        sApplicationComponent.inject(manageHotspotsActivity);
    }

    public static void inject(ClockFragment clockFragment) {
        sApplicationComponent.inject(clockFragment);
    }

    public static void inject(IntentEditorFragment intentEditorFragment) {
        sApplicationComponent.inject(intentEditorFragment);
    }

    public static void inject(IntentViewHolder intentViewHolder) {
        sApplicationComponent.inject(intentViewHolder);
    }

    public static void inject(ProfileImageFragment profileImageFragment) {
        sApplicationComponent.inject(profileImageFragment);
    }

    public static void inject(ManageHomePagesActivity manageHomePagesActivity) {
        sApplicationComponent.inject(manageHomePagesActivity);
    }

    public static void inject(WeatherSyncAdapter weatherSyncAdapter) {
        sApplicationComponent.inject(weatherSyncAdapter);
    }

    public static void inject(WeatherActivity weatherActivity) {
        sApplicationComponent.inject(weatherActivity);
    }

    public static void inject(WeatherLoadingService weatherLoadingService) {
        sApplicationComponent.inject(weatherLoadingService);
    }

    public static void inject(ProcessMonitorFragment processMonitorFragment) {
        sApplicationComponent.inject(processMonitorFragment);
    }

    public static void inject(ProcessMonitorFragment.GotItFragment gotItFragment) {
        sApplicationComponent.inject(gotItFragment);
    }

    public static void inject(CustomThemeActivity customThemeActivity) {
        sApplicationComponent.inject(customThemeActivity);
    }

    public static void inject(ForceSyncService forceSyncService) {
        sApplicationComponent.inject(forceSyncService);
    }

    public static void inject(RequestPermissionActivity requestPermissionActivity) {
        sApplicationComponent.inject(requestPermissionActivity);
    }

    public static void inject(ReorderController reorderController) {
        sApplicationComponent.inject(reorderController);
    }

    public static void inject(AgendaDaysLoader agendaDaysLoader) {
        sApplicationComponent.inject(agendaDaysLoader);
    }

    public static void inject(AgendaLoader agendaLoader) {
        sApplicationComponent.inject(agendaLoader);
    }

    public static void inject(ParallaxListViewHeader parallaxListViewHeader) {
        sApplicationComponent.inject(parallaxListViewHeader);
    }

    public static void inject(ParallaxListView parallaxListView) {
        sApplicationComponent.inject(parallaxListView);
    }

    public static BitmapUtils provideBitmapUtils() {
        return sApplicationComponent.provideBitmapUtils();
    }

    public static WeatherUtils provideWeatherUtils() {
        return sApplicationComponent.provideWeatherUtils();
    }

    public static SharedPreferences provideSharedPreferences() {
        return sApplicationComponent.provideSharedPreferences();
    }

    public static void inject(
            LookAndFeelActivity.LookAndFeelPreferencesFragment lookAndFeelPreferencesFragment) {
        sApplicationComponent.inject(lookAndFeelPreferencesFragment);
    }

    public static void inject(PatternRelativeLayout patternRelativeLayout) {
        sApplicationComponent.inject(patternRelativeLayout);
    }

    public static void inject(SidebarHotspot sidebarHotspot) {
        sApplicationComponent.inject(sidebarHotspot);
    }

    public static LocationManager provideLocationManager() {
        return sApplicationComponent.provideLocationManager();
    }

    public static void inject(PopupLayer popupLayer) {
        sApplicationComponent.inject(popupLayer);
    }

    public static ActivityManager provideActivityManager() {
        return sApplicationComponent.provideActivityManager();
    }

    public static TelephonyManager provideTelephonyManager() {
        return sApplicationComponent.provideTelephonyManager();
    }

    public static InputMethodManager provideInputMethodManager() {
        return sApplicationComponent.provideInputMethodManager();
    }

    public static WindowManager provideWindowManager() {
        return sApplicationComponent.provideWindowManager();
    }

    public static UserManager provideUserManager() {
        return sApplicationComponent.provideUserManager();
    }

    public static PackageManager providePackageManager() {
        return sApplicationComponent.providePackageManager();
    }

    public static LauncherApps provideLauncherApps() {
        return sApplicationComponent.provideLauncherApps();
    }

    public static void inject(CallLogLoader callLogLoader) {
        sApplicationComponent.inject(callLogLoader);
    }

    public static void inject(ReorderAppsFragment reorderAppsFragment) {
        sApplicationComponent.inject(reorderAppsFragment);
    }

    public static void inject(AppsiApplication appsiApplication) {
        sApplicationComponent.inject(appsiApplication);
    }

    public static PermissionUtils providePermissionUtils() {
        return sApplicationComponent.providePermissionUtils();
    }

    public static ApplicationComponent getApplicationComponent() {
        return sApplicationComponent;
    }
}
