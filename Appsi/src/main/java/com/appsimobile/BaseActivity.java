package com.appsimobile;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;

import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.dagger.AppComponent;

/**
 * Created by nmartens on 25/04/16.
 */
public class BaseActivity extends Activity {

    AppComponent mAppComponent;

    public static AppComponent componentFrom(Fragment fragment) {
        Activity activity = fragment.getActivity();
        return ((AppsiApplication) activity.getApplication()).getComponent();
    }

    public static AppComponent componentFrom(Context context) {
        return ((AppsiApplication) context.getApplicationContext()).getComponent();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppComponent = ((AppsiApplication) getApplication()).getComponent();
    }

    public AppComponent component() {
        return mAppComponent;
    }
}
