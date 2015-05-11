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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;

/**
 * Created by Nick Martens on 8/28/13.
 */
public class ActionbarUtils {

    private static final ScaleAnimation sScaleAnimation =
            new ScaleAnimation(1, 1, 0, 1, ScaleAnimation.RELATIVE_TO_SELF, .5f,
                    ScaleAnimation.RELATIVE_TO_SELF, .5f);

    static {
        sScaleAnimation.setDuration(150);
        sScaleAnimation.setInterpolator(new DecelerateInterpolator());
    }

    public static void enableSaveButton(Activity activity,
            View.OnClickListener onClickListener) {
        enableSaveButton(activity.getActionBar(), onClickListener);
    }

    private static void enableSaveButton(ActionBar actionBar,
            View.OnClickListener onClickListener) {
        final LayoutInflater inflater = (LayoutInflater) actionBar.getThemedContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View customActionBarView =
                inflater.inflate(R.layout.actionbar_custom_view_done, null);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(onClickListener);

        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView);
        customActionBarView.startAnimation(sScaleAnimation);

    }

    public static void enableSaveButton(PreferenceActivity activity,
            View.OnClickListener onClickListener) {
        enableSaveButton(activity.getActionBar(), onClickListener);
    }


}
