package com.appsimobile.appsii;

import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.dagger.ApplicationComponent;

/**
 * Created by nmartens on 12/01/16.
 */
public class MockAppsiApplication extends AppsiApplication {

    private static volatile AppsiApplication sInstance;
    ApplicationComponent mApplicationComponent;

    public MockAppsiApplication() {
        sInstance = this;
    }

    public static AppsiApplication getInstance() {
        return sInstance;
    }

    protected void initializeDagger() {
        mApplicationComponent = DaggerMockApplicationComponent
                .builder()
                .mockApplicationModule(new MockApplicationModule(this))
                .build();
        AppInjector.setComponent(mApplicationComponent);
    }

    public ApplicationComponent getApplicationComponent() {
        return mApplicationComponent;
    }
}
