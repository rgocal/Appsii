package com.appsimobile.appsii;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.WindowManager;

import com.appsimobile.appsii.compat.LauncherAppsCompat;
import com.appsimobile.appsii.module.apps.AppPageLoader;
import com.appsimobile.appsii.permissions.PermissionUtils;

import org.mockito.Mockito;

import java.lang.ref.WeakReference;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by nmartens on 13/01/16.
 */
@Module
public class MockAppsiModule extends MockApplicationModule {


    final Context mThemedContext;
    private final WeakReference<Appsi> mAppsi;

    public MockAppsiModule(Appsi appsi, Context themedContext) {
        super(appsi.getApplication());
        this.mAppsi = new WeakReference<>(appsi);
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
        return Mockito.mock(AbstractHotspotHelper.class);
    }


}
