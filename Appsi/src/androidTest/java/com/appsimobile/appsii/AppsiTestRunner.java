package com.appsimobile.appsii;

import android.app.Application;
import android.content.Context;
import android.support.test.runner.AndroidJUnitRunner;

/**
 * Created by nmartens on 12/01/16.
 */
public class AppsiTestRunner extends AndroidJUnitRunner {

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return newApplication(MockAppsiApplication.class, context);
    }
}
