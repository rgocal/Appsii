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
import android.content.IntentFilter;
import android.net.Uri;

public class AppStatusReceiver extends BroadcastReceiver {

    public static BroadcastReceiver register(Context context) {
        IntentFilter appActionFilter = new IntentFilter();
        appActionFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appActionFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appActionFilter.addDataScheme("package");

        AppStatusReceiver receiver = new AppStatusReceiver();
        context.registerReceiver(receiver, appActionFilter);

        return receiver;
    }

    public static void broadcastRefreshAllApps(Context context) {
        // this is a special virtual uri, it is used to notify about app changes and update contents
        context.getContentResolver()
                .notifyChange(Uri.parse("content://com.appsimobile.appsii.appsplugin.all"), null);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // check if we should just refresh only
        if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action) ||
                Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
            broadcastRefreshAllApps(context);
            return;
        }

        boolean isDelete = Intent.ACTION_PACKAGE_REMOVED.equals(action);
        if (isDelete) {
            boolean updating =
                    intent.getBooleanExtra(android.content.Intent.EXTRA_REPLACING, false);
            if (updating) return;
        }


        broadcastRefreshAllApps(context);
    }

}
