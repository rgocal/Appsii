package com.appsimobile.appsii.dagger;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.WindowManager;

import com.appsimobile.appsii.AbstractHotspotHelper;
import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.Appsi;
import com.appsimobile.appsii.HotspotHelperImpl;
import com.appsimobile.appsii.LoaderManager;
import com.appsimobile.appsii.LoaderManagerImpl;
import com.appsimobile.appsii.PopupLayer;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.Sidebar;
import com.appsimobile.appsii.SidebarContext;
import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.module.apps.AppPageLoader;
import com.appsimobile.appsii.permissions.PermissionUtils;

import java.lang.ref.WeakReference;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by nmartens on 24/11/15.
 */
@Module
public class AppsiModule extends ApplicationModule {


    final Context mThemedContext;
    private WeakReference<Appsi> mAppsi;

    public AppsiModule(Appsi appsi, Context themedContext) {
        super(appsi.getApplication());
        mAppsi = new WeakReference<>(appsi);
        mThemedContext = themedContext;
    }

    @Singleton
    @Provides
    SidebarContext provideSidebarContext(AnalyticsManager analyticsManager,
            LoaderManager loaderManager) {
        return new SidebarContext(mThemedContext, analyticsManager, loaderManager);
    }

    @Singleton
    @Provides
    Appsi provideAppsi() {
        return mAppsi.get();
    }

    @Provides
    @Singleton
    AppPageLoader provideAppPageLoader(LauncherAppsCompat lac) {
        return new AppPageLoader(mThemedContext, lac);
    }

    @Provides
    @Singleton
    LoaderManager provideLoaderManager(LoaderManagerImpl loaderManager) {
        return loaderManager;
    }

    @Provides
    @Singleton
    PopupLayer providePopupLayer() {
        LayoutInflater inflater = LayoutInflater.from(mThemedContext);
        return (PopupLayer) inflater.inflate(R.layout.popup_layer, null);
    }

    @Provides
    @Singleton
    Sidebar provideSidebar() {
        LayoutInflater inflater = LayoutInflater.from(mThemedContext);
        return (Sidebar) inflater.inflate(R.layout.sidebar, null);
    }

    @Provides
    @Singleton
    AbstractHotspotHelper provideHotspotHelper(Context context, SharedPreferences sharedPreferences,
            PermissionUtils permissionUtils, WindowManager windowManager, PopupLayer popupLayer) {
        return new HotspotHelperImpl(
                context, sharedPreferences, permissionUtils, windowManager, popupLayer);
    }

}
