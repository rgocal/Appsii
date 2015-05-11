/*
 * Copyright 2015. Appsi Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appsimobile.appsii;

import android.content.Context;

import com.appsimobile.appsii.module.apps.AppsController;
import com.appsimobile.appsii.module.appsiagenda.AgendaController;
import com.appsimobile.appsii.module.calls.CallLogController;
import com.appsimobile.appsii.module.home.HomePageController;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.module.people.PeopleController;
import com.appsimobile.appsii.module.search.SearchController;

/**
 * Created by nick on 10/08/14.
 */
public class SidebarPagerAdapter extends AbstractSidebarPagerAdapter {

    public SidebarPagerAdapter(Context context) {
        super(context);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        HotspotPageEntry key = getPageKey(position);
        return key.mPageName;
    }

    @Override
    protected PageController onCreatePageController(Context context, HotspotPageEntry page) {
        int type = page.mPageType;
        switch (type) {
            case HomeContract.Pages.PAGE_HOME:
                return new HomePageController(context, page.mPageId, page.mPageName);
            case HomeContract.Pages.PAGE_CALLS:
                return new CallLogController(context, page.mPageName);
            case HomeContract.Pages.PAGE_PEOPLE:
                return new PeopleController(context, page.mPageName);
            case HomeContract.Pages.PAGE_APPS:
                return new AppsController(context, page.mPageName);
            case HomeContract.Pages.PAGE_AGENDA:
                return new AgendaController(context, page.mPageName);
            case HomeContract.Pages.PAGE_SEARCH:
                return new SearchController(context, page.mPageName);
            default:
                throw new IllegalStateException("Unknown page: " + page);
        }
    }

}
