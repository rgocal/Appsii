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

import android.app.Activity;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by nick on 11/05/15.
 */
public final class ActivityUtils {

    private ActivityUtils() {
    }

    public static void setContentView(Activity activity, @LayoutRes int layoutResId) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        boolean large = activity.getResources().getBoolean(R.bool.isLarge);
        if (large) {
            activity.setContentView(R.layout.tablet_activity_wrapper);
            ViewGroup container = (ViewGroup) activity.findViewById(R.id.content_view);
            layoutInflater.inflate(layoutResId, container, true);
        } else {
            activity.setContentView(layoutResId);
        }
    }

    public static void setContentViewWithFab(Activity activity, @LayoutRes int layoutResId) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        boolean large = activity.getResources().getBoolean(R.bool.isLarge);
        if (large) {
            activity.setContentView(R.layout.tablet_activity_wrapper_fab);
            ViewGroup container = (ViewGroup) activity.findViewById(R.id.content_view);
            layoutInflater.inflate(layoutResId, container, true);
        } else {
            activity.setContentView(layoutResId);
        }
    }

    public static View setupFab(Activity activity, @IdRes int fabResId) {

        View normalFab = activity.findViewById(fabResId);
        View wrapperFab = activity.findViewById(R.id.wrapper_fab);

        if (wrapperFab != null) {
            normalFab.setVisibility(View.GONE);
            return wrapperFab;
        }

        return normalFab;
    }

    public static Toolbar setupToolbar(AppCompatActivity activity, @IdRes int toolbarResId) {
        ActionBar ab = activity.getSupportActionBar();
        if (ab != null) throw new IllegalStateException("Activity already has a toolbar");

        Toolbar layoutToolbar = (Toolbar) activity.findViewById(toolbarResId);
        Toolbar result = (Toolbar) activity.findViewById(R.id.tablet_toolbar);
        if (result != null) {
            if (layoutToolbar != null) {
                layoutToolbar.setVisibility(View.GONE);
            }
            activity.setSupportActionBar(result);
            ((TextView) activity.findViewById(R.id.toolbar_title)).setText(activity.getTitle());
            activity.getSupportActionBar().setTitle(null);
            return result;
        }
        activity.setSupportActionBar(layoutToolbar);
        activity.getSupportActionBar().setTitle(activity.getTitle());
        return layoutToolbar;
    }

    public static Toolbar getToolbarPlain(Activity activity, @IdRes int toolbarResId) {
        Toolbar layoutToolbar = (Toolbar) activity.findViewById(toolbarResId);
        Toolbar result = (Toolbar) activity.findViewById(R.id.tablet_toolbar);
        if (result != null) {
            if (layoutToolbar != null) {
                layoutToolbar.setVisibility(View.GONE);
            }
            ((TextView) activity.findViewById(R.id.toolbar_title)).setText(activity.getTitle());
            return result;
        }
        layoutToolbar.setTitle(activity.getTitle());
        return layoutToolbar;
    }

}
