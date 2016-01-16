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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.appsimobile.appsii.dagger.AppInjector;

import javax.inject.Inject;

/**
 * Created by nick on 23/04/15.
 * adb shell am startservice com.appsimobile.appsii/.ForceSyncService
 */
public class ForceSyncService extends Service {

    @Inject
    AccountHelper mAccountHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        AppInjector.inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mAccountHelper.configureAutoSyncAndSync();
//        accountHelper.requestSync();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
