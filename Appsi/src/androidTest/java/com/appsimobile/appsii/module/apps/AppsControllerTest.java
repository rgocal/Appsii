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

package com.appsimobile.appsii.module.apps;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.test.InstrumentationTestCase;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.appsimobile.appsii.Appsi;
import com.appsimobile.appsii.LoaderManager;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.SidebarContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by nick on 09/03/15.
 */
public class AppsControllerTest extends InstrumentationTestCase {

    Appsi mAppsi;

    AppsController mAppsController;

    View mAppsControllerView;

    Context mContext;

    private Handler mHandler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();


        Looper looper = Looper.getMainLooper();

        final CountDownLatch latch = new CountDownLatch(1);

        mHandler = new Handler(looper);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Context context = getInstrumentation().getTargetContext();
                context = new ContextThemeWrapper(context,
                        R.style.Appsi_Sidebar_Material_Teal);
                mContext = context;

                LayoutInflater inflater = LayoutInflater.from(context);
                SidebarContext sb = new SidebarContext(context) {
                    @Override
                    public void track(String action, String category, String label) {
                    }

                    @Override
                    public void track(String action, String category) {
                    }

                    @Override
                    public void trackPageView(String page) {
                    }
                };
                sb.setLoaderManager(LoaderManager.createInstance(mAppsi, false));


                FrameLayout parent = new FrameLayout(sb);

                mAppsController = new AppsController(sb, "Apps");
                mAppsController.performCreate(null);
                mAppsControllerView = mAppsController.performCreateView(inflater, parent);
                mAppsController.performViewCreated(mAppsControllerView);
                mAppsController.onSidebarAttached();

                mAppsController.performOnAttach();

                parent.measure(
                        View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
                parent.layout(0, 0, 1200, 2400);

                mAppsController.performStart();
                mAppsController.performRestoreInstanceState();
                mAppsController.performResume();

                mAppsController.onFirstLayout();
                mAppsController.onUserVisible();

                latch.countDown();
            }
        });

        latch.await();

    }

    public void testNullApps() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<AppsAdapter> adapterRef = new AtomicReference<>();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                AppTag allAppsTag = new AppTag(
                        0, "all", 0, false, true, 3, AppsContract.TagColumns.TAG_TYPE_ALL);
                AppTag recentAppsTag = new AppTag(
                        1, "recent", 1, false, true, 3, AppsContract.TagColumns.TAG_TYPE_RECENT);

                AppPageData data = new AppPageData(null, new ArrayList<HistoryItem>(),
                        Arrays.asList(allAppsTag, recentAppsTag));

                AppPageLoader loader = new AppPageLoader(mContext);
                loader.registerListener(AppsController.APPS_LOADER_ID, null);
                mAppsController.onLoadFinished(loader, data);
                adapterRef.set(mAppsController.mAppsAdapter);
                latch.countDown();
            }
        });
        latch.await();
        AppsAdapter appsAdapter = adapterRef.get();
        assertEquals(3, appsAdapter.getItemCount());
    }


}
