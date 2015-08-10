package com.appsimobile.appsii.module.home.config;

import android.content.Context;
import android.support.v4.util.LongSparseArray;

import java.util.ArrayList;

/**
 * Created by nmartens on 07/08/15.
 */
public class MockHomeItemConfigurationHelper extends HomeItemConfigurationHelper {

    ArrayList<Runnable> mRunnables;


    public MockHomeItemConfigurationHelper(Context context, ArrayList<Runnable> runnables) {
        super(context);
        mRunnables = runnables;
    }

    @Override
    LongSparseArray<ConfigurationProperty> loadConfigurations(Context context) {
        LongSparseArray<ConfigurationProperty> result = new LongSparseArray<>();

        ConfigurationProperty p0 = createProperty(result, 0);
        ConfigurationProperty p1 = createProperty(result, 1);
        ConfigurationProperty p2 = createProperty(result, 2);
        ConfigurationProperty p3 = createProperty(result, 3);

        p0.put("keya", "0a");
        p0.put("keyb", "0b");
        p1.put("keya", "1a");
        p2.put("keya", "2a");
        p2.put("keyc", "2c");


        return result;
    }


    @Override
    public void updateProperty(final long cellId, final String key, final String value) {
        mRunnables.add(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                onPropertyUpdated(cellId, key, value);
            }
        });
    }


    @Override
    public void removeProperty(final long cellId, final String key) {
        mRunnables.add(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                onPropertyDeleted(cellId, key);
            }
        });
    }
}
