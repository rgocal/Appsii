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

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by nmartens on 25/11/15.
 */
@Singleton
@Component(modules = {AppsiModule.class})
public interface AppsiComponent {

    void inject(AgendaController controller);

    void inject(AppsController controller);

    void inject(PeopleController controller);

    void inject(CallLogController controller);

    void inject(HomePageController controller);

    void inject(SearchController controller);

    void inject(SidebarContext sidebarContext);

    void inject(BaseViewHolder holder);

    void inject(HotspotHelperImpl hotspotHelper);

    PeopleLoader providePeopleLoader();

    void inject(Sidebar sidebar);

    @Nullable
    Appsi provideAppsi();

    void inject(Appsi appsi);

    Sidebar provideSidebar();

    PopupLayer providePopupLayer();

    AbstractHotspotHelper provideHotspotHelper();
}
