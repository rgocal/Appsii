package com.appsimobile.appsii;

import com.appsimobile.appsii.dagger.AppsiComponent;
import com.appsimobile.appsii.module.apps.AppsControllerTest;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by nmartens on 12/01/16.
 */
@Singleton
@Component(modules = {MockAppsiModule.class})
public interface MockAppsiComponent extends AppsiComponent {

    void inject(MainActivityTest mainActivityTest);

    void inject(AppsControllerTest appsControllerTest);

}
