package com.appsimobile.appsii.dagger;

import com.appsimobile.appsii.dagger.agera.ContentProviderObservable;
import com.appsimobile.appsii.dagger.repository.AppsRepository;
import com.appsimobile.appsii.module.apps.AddTagActivity;
import com.appsimobile.appsii.module.apps.AppView;
import com.appsimobile.appsii.module.apps.AppsController;
import com.appsimobile.appsii.module.apps.EditTagActivity;
import com.appsimobile.appsii.module.apps.ReorderAppsActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by nmartens on 23/04/16.
 */
@Singleton
@Component(modules = {AppsModule.class, CalendarModule.class, MainModule.class})
public interface AppComponent {


    void inject(AppsRepository appsRepository);

    void inject(ContentProviderObservable contentProviderObservable);

    void inject(AppsModule.LauncherAppsProvider launcherAppsProvider);

    void inject(AppsModule.HistoryItemSupplier historyItemSupplier);

    void inject(AppsModule.AppTagCursorSupplier appTagCursorSupplier);

    void inject(AppsModule.TaggedAppsIntoDataMerger taggedAppsMerger);

    void inject(AppsModule.TaggedAppCursorSupplier taggedAppCursorSupplier);

    void inject(AddTagActivity addTagActivity);

    void inject(AppView appView);

    void inject(EditTagActivity editTagActivity);

    void inject(ReorderAppsActivity reorderAppsActivity);

    void inject(AppsController appsController);
}
