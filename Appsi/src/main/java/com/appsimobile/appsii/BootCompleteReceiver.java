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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.preference.PreferenceHelper;

import javax.inject.Inject;

/**
 * A receiver that handles the boot complete and user present events.
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    @Inject
    PreferenceHelper mPreferenceHelper;

    public BootCompleteReceiver() {
        AppInjector.inject(this);
    }

    public static void autoStartAppsi(Context context, PreferenceHelper preferenceHelper) {
        if (preferenceHelper.getAutoStart()) {
            AppsiiUtils.startAppsi(context);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        autoStartAppsi(context, mPreferenceHelper);
    }

}
