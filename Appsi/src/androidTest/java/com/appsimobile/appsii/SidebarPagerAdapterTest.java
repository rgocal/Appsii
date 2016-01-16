///*
// *
// *  * Copyright 2015. Appsi Mobile
// *  *
// *  * Licensed under the Apache License, Version 2.0 (the "License");
// *  * you may not use this file except in compliance with the License.
// *  * You may obtain a copy of the License at
// *  *
// *  *     http://www.apache.org/licenses/LICENSE-2.0
// *  *
// *  * Unless required by applicable law or agreed to in writing, software
// *  * distributed under the License is distributed on an "AS IS" BASIS,
// *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  * See the License for the specific language governing permissions and
// *  * limitations under the License.
// *
// */
//
//package com.appsimobile.appsii;
//
//import android.content.Context;
//import android.os.Bundle;
//import android.support.v4.util.CircularArray;
//import android.test.InstrumentationTestCase;
//import android.util.Pair;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.FrameLayout;
//
//import com.appsimobile.util.ArrayUtils;
//
//import org.junit.Before;
//import org.junit.Test;
//
///**
// * Created by nick on 30/03/15.
// */
//public class SidebarPagerAdapterTest {
//
//    SidebarContext mContext;
//
//    @Before
//    protected void setUp() throws Exception {
//        super.setUp();
//        mContext = new MockSidebarContext(getInstrumentation().getTargetContext());
//    }
//
//    @Test
//    public void testSetPrimaryItem() {
//
//        Pair<CircularArray<HotspotPageEntry>, MockSidebarPagerAdapter> pair = createAdapter();
//
//        CircularArray<HotspotPageEntry> entries = pair.first;
//        MockSidebarPagerAdapter adapter = pair.second;
//
//        FrameLayout container = new FrameLayout(mContext);
//
//        adapter.setSidebarOpening(true);
//        adapter.onAttachedToWindow();
//
//        // instantiate the items 0 and 1. like the viewpager would
//        adapter.instantiateItem(container, 0);
//        adapter.instantiateItem(container, 1);
//
//        {
//            adapter.setPrimaryItem(container, 0, adapter.pageAt(0));
//
//            MockPageController activePageController =
//                    (MockPageController) adapter.mActivePageController;
//
//            assertTrue(activePageController.mUserVisible);
//
//            assertTrue(activePageController.mSidebarAttached);
//            assertTrue(entries.get(0) == activePageController.mEntry);
//            assertTrue(activePageController.mDeferLoads);
//
//            adapter.setSidebarOpening(false);
//            assertFalse(activePageController.mDeferLoads);
//        }
//
//        {
//
//            // switch to page 2 and verify it's state
//            adapter.destroyItem(container, 0, adapter.pageAt(0));
//            adapter.setPrimaryItem(container, 1, adapter.pageAt(1));
//
//            MockPageController activePageController =
//                    (MockPageController) adapter.mActivePageController;
//
//            assertTrue(entries.get(1) == activePageController.mEntry);
//
//            assertFalse(activePageController.mDeferLoads);
//
//        }
//
//        adapter.onDetachedFromWindow();
//        assertNotNull(adapter.mActivePageController);
//        assertNotNull(adapter.pageAt(1));
//
//        adapter.setSidebarOpening(true);
//        assertNotNull(adapter.mActivePageController);
//        assertNotNull(adapter.pageAt(1));
//
//        adapter.onAttachedToWindow();
//        assertNotNull(adapter.mActivePageController);
//        assertNotNull(adapter.pageAt(1));
//
//        {
//            adapter.setPrimaryItem(container, 1, adapter.pageAt(1));
//
//            MockPageController activePageController =
//                    (MockPageController) adapter.mActivePageController;
//
//            assertTrue(activePageController.mDeferLoads);
//
//            adapter.setSidebarOpening(true);
//            assertFalse(activePageController.mDeferLoads);
//        }
//
//    }
//
//    Pair<CircularArray<HotspotPageEntry>, MockSidebarPagerAdapter> createAdapter() {
//        MockSidebarPagerAdapter adapter = new MockSidebarPagerAdapter();
//
//        CircularArray<HotspotPageEntry> entries = ArrayUtils.<HotspotPageEntry>asArray(
//                entry(),
//                entry(),
//                entry(),
//                entry(),
//                entry());
//
//        adapter.setPages(entries);
//
//        return Pair.create(entries, adapter);
//    }
//
//    HotspotPageEntry entry() {
//        HotspotPageEntry entry = new HotspotPageEntry();
//        entry.mEnabled = true;
//        return entry;
//    }
//
//
//}
