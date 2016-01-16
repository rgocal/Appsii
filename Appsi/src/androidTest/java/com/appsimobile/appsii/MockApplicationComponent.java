package com.appsimobile.appsii;

import com.appsimobile.appsii.dagger.ApplicationComponent;
import com.appsimobile.appsii.module.home.WeatherActivityTest;
import com.appsimobile.appsii.permissions.PermissionUtils;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by nmartens on 12/01/16.
 */
@Singleton
@Component(modules = {MockApplicationModule.class})
public interface MockApplicationComponent extends ApplicationComponent {

    void inject(MainActivityTest mainActivityTest);

    void inject(WeatherActivityTest weatherActivityTest);

    PermissionUtils providePermissionUtils();

}
