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

package com.appsimobile.appsii;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.util.CircularArray;
import android.test.InstrumentationTestCase;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.appsimobile.util.ArrayUtils;

/**
 * Created by nick on 30/03/15.
 */
public class SidebarPagerAdapterTest extends InstrumentationTestCase {

    MockSidebarContext mContext;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = new MockSidebarContext(getInstrumentation().getTargetContext());
    }

    public void testSetPrimaryItem() {

        Pair<CircularArray<HotspotPageEntry>, MockSidebarPagerAdapter> pair = createAdapter();

        CircularArray<HotspotPageEntry> entries = pair.first;
        MockSidebarPagerAdapter adapter = pair.second;

        FrameLayout container = new FrameLayout(mContext);

        adapter.setSidebarOpening(true);
        adapter.onAttachedToWindow();

        // instantiate the items 0 and 1. like the viewpager would
        adapter.instantiateItem(container, 0);
        adapter.instantiateItem(container, 1);

        {
            adapter.setPrimaryItem(container, 0, adapter.pageAt(0));

            MockPageController activePageController =
                    (MockPageController) adapter.mActivePageController;

            assertTrue(activePageController.mUserVisible);

            assertTrue(activePageController.mSidebarAttached);
            assertTrue(entries.get(0) == activePageController.mEntry);
            assertTrue(activePageController.mDeferLoads);

            adapter.setSidebarOpening(false);
            assertFalse(activePageController.mDeferLoads);
        }

        {

            // switch to page 2 and verify it's state
            adapter.destroyItem(container, 0, adapter.pageAt(0));
            adapter.setPrimaryItem(container, 1, adapter.pageAt(1));

            MockPageController activePageController =
                    (MockPageController) adapter.mActivePageController;

            assertTrue(entries.get(1) == activePageController.mEntry);

            assertFalse(activePageController.mDeferLoads);

        }

        adapter.onDetachedFromWindow();
        assertNotNull(adapter.mActivePageController);
        assertNotNull(adapter.pageAt(1));

        adapter.setSidebarOpening(true);
        assertNotNull(adapter.mActivePageController);
        assertNotNull(adapter.pageAt(1));

        adapter.onAttachedToWindow();
        assertNotNull(adapter.mActivePageController);
        assertNotNull(adapter.pageAt(1));

        {
            adapter.setPrimaryItem(container, 1, adapter.pageAt(1));

            MockPageController activePageController =
                    (MockPageController) adapter.mActivePageController;

            assertTrue(activePageController.mDeferLoads);

            adapter.setSidebarOpening(true);
            assertFalse(activePageController.mDeferLoads);
        }

    }

    Pair<CircularArray<HotspotPageEntry>, MockSidebarPagerAdapter> createAdapter() {
        MockSidebarPagerAdapter adapter = new MockSidebarPagerAdapter();

        CircularArray<HotspotPageEntry> entries = ArrayUtils.<HotspotPageEntry>asArray(
                entry(),
                entry(),
                entry(),
                entry(),
                entry());

        adapter.setPages(entries);

        return Pair.create(entries, adapter);
    }

    HotspotPageEntry entry() {
        HotspotPageEntry entry = new HotspotPageEntry();
        entry.mEnabled = true;
        return entry;
    }

    static class MockPageController extends PageController {

        final HotspotPageEntry mEntry;

        boolean mDeferLoads;

        public MockPageController(Context context, HotspotPageEntry entry, String title) {
            super(context, title);
            mEntry = entry;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                Bundle savedInstanceState) {
            return new FrameLayout(parent.getContext());
        }

        @Override
        protected void onViewCreated(View view, Bundle savedInstanceState) {

        }

        @Override
        protected void applyToolbarColor(int color) {

        }

        @Override
        public void setDeferLoads(boolean deferLoads) {
            super.setDeferLoads(deferLoads);
            mDeferLoads = deferLoads;
        }

    }

    static class MockSidebarContext extends SidebarContext {

        public MockSidebarContext(Context base) {
            super(base);
        }
    }

    private class MockSidebarPagerAdapter extends SidebarPagerAdapter {

        public MockSidebarPagerAdapter() {
            super(SidebarPagerAdapterTest.this.mContext);
        }

        MockPageController pageAt(int position) {
            HotspotPageEntry entry = mActivePageKeys.get(position);
            return (MockPageController) mCachedControllers.get(entry);
        }

        @Override
        protected PageController onCreatePageController(Context context,
                HotspotPageEntry page) {
            return new MockPageController(context, page, "title");
        }
    }
}
