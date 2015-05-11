/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.appsii.module.home;

import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;

import junit.framework.TestCase;

import net.jcip.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by nick on 08/03/15.
 */
public class HomeItemConfigurationHelperTest extends TestCase {

    HomeItemConfigurationHelper mHomeItemConfigurationHelper;

    ArrayList<Runnable> mRunnables;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRunnables = new ArrayList<>();
        mHomeItemConfigurationHelper = new HomeItemConfigurationHelper(new TestMockContext()) {

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
        };
    }

    void runPendingRunnables() {
        final List<Runnable> runnableList = new ArrayList<>(mRunnables);
        mRunnables.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Runnable r : runnableList) {
                    r.run();
                }
            }
        }).start();
    }

    public void testGetValue() {
        assertEquals("0a", mHomeItemConfigurationHelper.getProperty(0, "keya", null));
        assertEquals("0b", mHomeItemConfigurationHelper.getProperty(0, "keyb", null));
        assertNull(mHomeItemConfigurationHelper.getProperty(0, "keyc", null));

        assertEquals("1a", mHomeItemConfigurationHelper.getProperty(1, "keya", null));
        assertNull(mHomeItemConfigurationHelper.getProperty(1, "keyb", null));
        assertNull(mHomeItemConfigurationHelper.getProperty(1, "keyc", null));

        assertEquals("2a", mHomeItemConfigurationHelper.getProperty(2, "keya", null));
        assertNull(mHomeItemConfigurationHelper.getProperty(2, "keyb", null));
        assertEquals("2c", mHomeItemConfigurationHelper.getProperty(2, "keyc", null));

        assertNull(mHomeItemConfigurationHelper.getProperty(3, "keya", null));
        assertNull(mHomeItemConfigurationHelper.getProperty(3, "keyb", null));
        assertNull(mHomeItemConfigurationHelper.getProperty(3, "keyc", null));

        assertNull(mHomeItemConfigurationHelper.getProperty(4, "keya", null));
        assertNull(mHomeItemConfigurationHelper.getProperty(4, "keyb", null));
        assertNull(mHomeItemConfigurationHelper.getProperty(4, "keyc", null));
    }

    public void testUpdate() throws InterruptedException {
        TestConfigurationListener l = new TestConfigurationListener(1, 1);
        mHomeItemConfigurationHelper.addConfigurationListener(l);
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "u0a");

        l.awaitUpdate();

        assertEquals(0L, l.updateCellIdAt(0));
        assertEquals("keya", l.updateCellKeyAt(0));
        assertEquals("u0a", l.updateCellValueAt(0));
        assertEquals("u0a", mHomeItemConfigurationHelper.getProperty(0, "keya", null));
        mHomeItemConfigurationHelper.removeConfigurationListener(l);


        l = new TestConfigurationListener(2, 1);
        mHomeItemConfigurationHelper.addConfigurationListener(l);
        mHomeItemConfigurationHelper.updateProperty(0, "keyb", "u0b");
        mHomeItemConfigurationHelper.updateProperty(0, "keyc", "u0c");

        l.awaitUpdate();

        assertEquals(0L, l.updateCellIdAt(0));
        assertEquals("keyb", l.updateCellKeyAt(0));
        assertEquals("u0b", l.updateCellValueAt(0));
        assertEquals(0L, l.updateCellIdAt(1));
        assertEquals("keyc", l.updateCellKeyAt(1));
        assertEquals("u0c", l.updateCellValueAt(1));

        assertEquals("u0b", mHomeItemConfigurationHelper.getProperty(0, "keyb", null));
        assertEquals("u0c", mHomeItemConfigurationHelper.getProperty(0, "keyc", null));
        mHomeItemConfigurationHelper.removeConfigurationListener(l);


        l = new TestConfigurationListener(8, 1);
        mHomeItemConfigurationHelper.addConfigurationListener(l);
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_a");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_b");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_c");
        mHomeItemConfigurationHelper.updateProperty(0, "keyb", "b");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_d");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_e");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_f");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "test");
        l.awaitUpdate();

        assertEquals("test", mHomeItemConfigurationHelper.getProperty(0, "keya", null));
        assertEquals("b", mHomeItemConfigurationHelper.getProperty(0, "keyb", null));
        mHomeItemConfigurationHelper.removeConfigurationListener(l);


    }

    public void testUpdateWithIntermediateDelete() throws InterruptedException {
        TestConfigurationListener l;
        final AtomicInteger clearCount = new AtomicInteger();
        final AtomicReference<String> lastValue = new AtomicReference<>();
        mHomeItemConfigurationHelper.addConfigurationListener(
                new HomeItemConfigurationHelper.ConfigurationListener() {

                    @Override
                    public void onConfigurationOptionUpdated(long cellId, String key,
                            String value) {
                        if ("keya".equals(key)) {
                            lastValue.set(value);
                        }
                    }

                    @Override
                    public void onConfigurationOptionDeleted(long cellId, String key) {
                        if ("keya".equals(key)) {
                            lastValue.set(null);
                            clearCount.incrementAndGet();
                        }
                    }
                });

        l = new TestConfigurationListener(8, 2);
        mHomeItemConfigurationHelper.addConfigurationListener(l);
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_a");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_b");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_c");
        mHomeItemConfigurationHelper.removeProperty(0, "keya");
        mHomeItemConfigurationHelper.updateProperty(0, "keyb", "a");
        mHomeItemConfigurationHelper.removeProperty(0, "keyb");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_d");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_e");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "_f");
        mHomeItemConfigurationHelper.updateProperty(0, "keya", "test");
        l.awaitUpdate();

        assertEquals("test", mHomeItemConfigurationHelper.getProperty(0, "keya", null));
        assertNull(mHomeItemConfigurationHelper.getProperty(0, "keyb", null));
        assertEquals("test", lastValue.get());
        assertEquals(1, clearCount.get());
        mHomeItemConfigurationHelper.removeConfigurationListener(l);
    }

    private class TestConfigurationListener
            implements HomeItemConfigurationHelper.ConfigurationListener {

        long mLastDeleteCellId;

        String mLastDeleteKey;

        @GuardedBy("this")
        long[] mUpdateCellIds;

        @GuardedBy("this")
        String[] mUpdateCellKeys;

        @GuardedBy("this")
        String[] mUpdateCellValues;

        @GuardedBy("this")
        int mUpdatePosition;

        CountDownLatch mUpdateLatch;

        CountDownLatch mDeleteLatch;

        TestConfigurationListener(int updateCount, int deleteCount) {
            mUpdateLatch = new CountDownLatch(updateCount);
            mDeleteLatch = new CountDownLatch(deleteCount);

            mUpdateCellIds = new long[updateCount];
            mUpdateCellKeys = new String[updateCount];
            mUpdateCellValues = new String[updateCount];
        }

        synchronized long updateCellIdAt(int position) {
            return mUpdateCellIds[position];
        }

        synchronized String updateCellKeyAt(int position) {
            return mUpdateCellKeys[position];
        }

        synchronized String updateCellValueAt(int position) {
            return mUpdateCellValues[position];
        }

        void awaitUpdate() throws InterruptedException {
            runPendingRunnables();
            mUpdateLatch.await();
        }

        @Override
        public void onConfigurationOptionUpdated(long cellId, String key, String value) {
            synchronized (this) {
                mUpdateCellIds[mUpdatePosition] = cellId;
                mUpdateCellKeys[mUpdatePosition] = key;
                mUpdateCellValues[mUpdatePosition] = value;
                mUpdatePosition++;
            }
            mUpdateLatch.countDown();
        }

        void awaitDelete() throws InterruptedException {
            runPendingRunnables();
            mDeleteLatch.await();
        }


        @Override

        public void onConfigurationOptionDeleted(long cellId, String key) {
            synchronized (this) {
                mLastDeleteCellId = cellId;
                mLastDeleteKey = key;
            }
            mDeleteLatch.countDown();
        }
    }

    class TestMockContext extends MockContext {

        @Override
        public ContentResolver getContentResolver() {
            return new MockContentResolver();
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }
    }


}
