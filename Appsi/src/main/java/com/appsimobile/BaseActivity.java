package com.appsimobile;

import android.app.Activity;
import android.os.Bundle;

import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.dagger.AppComponent;

/**
 * Created by nmartens on 25/04/16.
 */
public class BaseActivity extends Activity {

    AppComponent mAppComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppComponent = ((AppsiApplication) getApplication()).getComponent();
    }

    public AppComponent component() {
        return mAppComponent;
    }
}
