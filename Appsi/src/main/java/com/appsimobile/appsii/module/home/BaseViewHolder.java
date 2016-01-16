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

package com.appsimobile.appsii.module.home;

import android.view.MotionEvent;
import android.view.View;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppsiInjector;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;

import javax.inject.Inject;

/**
 * Created by nick on 29/01/15.
 */
public abstract class BaseViewHolder extends AbsHomeViewHolder implements
        HomeItemConfigurationHelper.ConfigurationListener {

    final View mOverflow;
    HomeItem mHomeItem;
    @Inject
    HomeItemConfiguration mConfigurationHelper;

    public BaseViewHolder(HomeViewWrapper view) {
        super(view);
        AppsiInjector.inject(this);
        mOverflow = view.findViewById(R.id.overflow);
    }

    @Override
    public void onConfigurationOptionUpdated(long cellId, String key, String value) {
        if (cellId == mHomeItem.mId) {
            updateConfiguration();
        }
    }

    abstract void updateConfiguration();

    @Override
    public void onConfigurationOptionDeleted(long cellId, String key) {
        if (cellId == mHomeItem.mId) {
            updateConfiguration();
        }
    }

    @Override
    void bind(HomeItem item, int heightPx) {
        mHomeItem = item;
        applySuggestedHeight(heightPx);
    }

    @Override
    public void onAllowLoads() {
        mConfigurationHelper.addConfigurationListener(this);
        updateConfiguration();
    }

    @Override
    public void onDisallowLoads() {
        mConfigurationHelper.removeConfigurationListener(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return false;
    }
}
