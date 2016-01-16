package com.appsimobile.appsii.dagger;

import android.support.annotation.Nullable;

import com.appsimobile.appsii.AbstractHotspotHelper;
import com.appsimobile.appsii.Appsi;
import com.appsimobile.appsii.HotspotHelperImpl;
import com.appsimobile.appsii.PopupLayer;
import com.appsimobile.appsii.Sidebar;
import com.appsimobile.appsii.SidebarContext;
import com.appsimobile.appsii.module.apps.AppsController;
import com.appsimobile.appsii.module.appsiagenda.AgendaController;
import com.appsimobile.appsii.module.calls.CallLogController;
import com.appsimobile.appsii.module.home.BaseViewHolder;
import com.appsimobile.appsii.module.home.HomePageController;
import com.appsimobile.appsii.module.people.PeopleController;
import com.appsimobile.appsii.module.people.PeopleLoader;
import com.appsimobile.appsii.module.search.SearchController;

/**
 * Created by nmartens on 25/11/15.
 */
public class AppsiInjector {

    static AppsiComponent sAppsiComponent;

    public static void inject(SidebarContext sidebarContext) {
        sAppsiComponent.inject(sidebarContext);
    }

    public static void inject(AppsController appsController) {
        sAppsiComponent.inject(appsController);
    }

    public static void inject(AgendaController controller) {
        sAppsiComponent.inject(controller);
    }

    public static void inject(SearchController controller) {
        sAppsiComponent.inject(controller);
    }

    public static void inject(HomePageController controller) {
        sAppsiComponent.inject(controller);
    }

    public static void inject(BaseViewHolder holder) {
        sAppsiComponent.inject(holder);
    }

    public static void inject(PeopleController controller) {
        sAppsiComponent.inject(controller);
    }

    public static void inject(CallLogController callLogController) {
        sAppsiComponent.inject(callLogController);
    }

    public static void inject(HotspotHelperImpl hotspotHelper) {
        sAppsiComponent.inject(hotspotHelper);
    }

    public static PeopleLoader providePeopleLoader() {
        return sAppsiComponent.providePeopleLoader();
    }

    public static void inject(Sidebar sidebar) {
        sAppsiComponent.inject(sidebar);
    }

    @Nullable
    public static Appsi provideAppsi() {
        return sAppsiComponent.provideAppsi();
    }

    public static AppsiComponent getAppsiComponent() {
        return sAppsiComponent;
    }

    public static void setAppsiComponent(AppsiComponent component) {
        sAppsiComponent = component;
    }

    public static void inject(Appsi appsi) {
        sAppsiComponent.inject(appsi);
    }

    public static Sidebar provideSidebar() {
        return sAppsiComponent.provideSidebar();
    }

    public static PopupLayer providePopupLayer() {
        return sAppsiComponent.providePopupLayer();
    }

    public static AbstractHotspotHelper provideHotspotHelper() {
        return sAppsiComponent.provideHotspotHelper();
    }
}
