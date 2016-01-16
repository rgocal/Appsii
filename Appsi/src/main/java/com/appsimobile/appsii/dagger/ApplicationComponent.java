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

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by nmartens on 25/11/15.
 */
@Singleton
@Component(modules = {ApplicationModule.class})
public interface ApplicationComponent {

    void inject(MainActivity activity);

    void inject(AboutActivity.AboutPreferenceFragment controller);

    void inject(SidebarContext context);

    void inject(AbsWeatherViewHolder absWeatherViewHolder);

    void inject(HomeAdapter homeAdapter);

    void inject(HomeEditorActivity.ActionModeCallbackImpl actionModeCallback);

    void inject(HomeEditorActivity homeEditorActivity);

    void inject(WidgetChooserActivity widgetChooserActivity);

    void inject(HomeContentProvider.HomeDatabaseHelper homeDatabaseHelper);

    void inject(AppWidgetManagerCompat appWidgetManagerCompat);

    void inject(AppsiServiceStatusView appsiServiceStatusView);

    void inject(BootCompleteReceiver bootCompleteReceiver);

    void inject(FirstRunWelcomeFragment firstRunWelcomeFragment);

    void inject(FirstRunSettingsFragment firstRunSettingsFragment);

    void inject(FirstRunLocationFragment firstRunLocationFragment);

    void inject(YahooLocationChooserDialogFragment yahooLocationChooserDialogFragment);

    void inject(FirstRunDoneFragment firstRunDoneFragment);

    void inject(WeatherFragment weatherFragment);

    void inject(HomePageController controller);

    void inject(ManageHotspotsActivity manageHotspotsActivity);

    void inject(ClockFragment clockFragment);

    void inject(IntentEditorFragment intentEditorFragment);

    void inject(IntentViewHolder intentViewHolder);

    void inject(ProfileImageFragment profileImageFragment);

    void inject(ManageHomePagesActivity manageHomePagesActivity);

    void inject(WeatherSyncAdapter weatherSyncAdapter);

    void inject(WeatherActivity weatherActivity);

    void inject(WeatherLoadingService weatherLoadingService);

    void inject(ProcessMonitorFragment processMonitorFragment);

    void inject(ProcessMonitorFragment.GotItFragment gotItFragment);

    void inject(CustomThemeActivity customThemeActivity);

    void inject(ForceSyncService forceSyncService);

    void inject(RequestPermissionActivity requestPermissionActivity);

    void inject(ReorderController reorderController);

    void inject(AgendaDaysLoader agendaDaysLoader);

    void inject(AgendaLoader agendaLoader);

    void inject(ParallaxListViewHeader parallaxListViewHeader);

    void inject(ParallaxListView parallaxListView);

    WeatherUtils provideWeatherUtils();

    BitmapUtils provideBitmapUtils();

    void inject(LookAndFeelActivity.LookAndFeelPreferencesFragment lookAndFeelPreferencesFragment);

    void inject(PatternRelativeLayout patternRelativeLayout);

    void inject(SidebarHotspot sidebarHotspot);

    SharedPreferences provideSharedPreferences();

    LocationManager provideLocationManager();

    void inject(PopupLayer popupLayer);

    ActivityManager provideActivityManager();

    UserManager provideUserManager();

    PackageManager providePackageManager();

    LauncherApps provideLauncherApps();

    void inject(CallLogLoader callLogLoader);

    WindowManager provideWindowManager();

    InputMethodManager provideInputMethodManager();

    TelephonyManager provideTelephonyManager();

    void inject(ReorderAppsFragment reorderAppsFragment);

    void inject(AppsiApplication appsiApplication);

    PermissionUtils providePermissionUtils();
}
