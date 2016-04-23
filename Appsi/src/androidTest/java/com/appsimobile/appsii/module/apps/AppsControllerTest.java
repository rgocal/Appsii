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

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.appsimobile.appsii.Appsi;
import com.appsimobile.appsii.MockAppsiApplication;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by nick on 09/03/15.
 */
@RunWith(AndroidJUnit4.class)
public class AppsControllerTest {

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    Appsi mAppsi;

    AppsAdapter mAppsAdapter;

    @Before
    public void setup() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        MockAppsiApplication app =
                (MockAppsiApplication) instrumentation.getTargetContext().getApplicationContext();

//        mPermissionUtils = AppInjector.getApplicationComponent().providePermissionUtils();

//        Mockito.when(mPermissionUtils.canDrawOverlays(any(Context.class))).thenReturn(true);

//        mServiceRule.startService(new Intent(app, Appsi.class));

//        MockAppsiComponent ac = (MockAppsiComponent) AppsiInjector.getAppsiComponent();
//        ac.inject(this);

//        mAppsi = AppsiInjector.provideAppsi();

        mAppsAdapter = new AppsAdapter();

    }

    @Test
    public void testNullApps() throws InterruptedException {

        AppTag allAppsTag = new AppTag(
                0, "all", 0, false, true, 3, AppsContract.TagColumns.TAG_TYPE_ALL);
        AppTag recentAppsTag = new AppTag(
                1, "recent", 1, false, true, 3, AppsContract.TagColumns.TAG_TYPE_RECENT);

        AppPageData data = new AppPageData(null, new ArrayList<HistoryItem>(),
                Arrays.asList(allAppsTag, recentAppsTag));

        mAppsAdapter.setAppPageData(data);

        Assert.assertEquals(3, mAppsAdapter.getItemCount());
    }


}
