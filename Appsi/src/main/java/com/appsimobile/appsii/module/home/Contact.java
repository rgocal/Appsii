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

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 28/03/15.
 */
class Contact {

    final List<RawContact> mRawContacts = new ArrayList<>();

    long mId;

    String mLookupKey;

    long mPhotoId;

    String mPhotoUri;

    Bitmap mBitmap;

    public String getViewContactNotifyServiceClassName() {
        return "com.google.android.syncadapters.contacts." +
                "SyncHighResPhotoIntentService";
    }

    public String getViewContactNotifyServicePackageName() {
        return "com.google.android.syncadapters.contacts";
    }

    public String getViewContactNotifyServiceClassName2() {
        return "com.google.android.apps.plus.service.AndroidContactsNotificationService";
    }

    public String getViewContactNotifyServicePackageName2() {
        return "com.google.android.apps.plus";
    }

    /*
    .method protected onHandleIntent(Landroid/content/Intent;)V
    .locals 3

            .prologue
            .line 24
            new-instance v0, Landroid/content/Intent;

    invoke-direct {v0}, Landroid/content/Intent;-><init>()V

            .line 25
            const-string v1, "com.google.android.gms"

            const-string v2, "com.google.android.gms.people.pub.PeopleSyncRawContactService"

    invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->setClassName(Ljava/lang/String;
    Ljava/lang/String;)Landroid/content/Intent;

    .line 27
    invoke-virtual {p1}, Landroid/content/Intent;->getData()Landroid/net/Uri;

    move-result-object v1

    invoke-virtual {v0, v1}, Landroid/content/Intent;->setData(Landroid/net/Uri;)
    Landroid/content/Intent;

    .line 28
    invoke-virtual {p0, v0},
    Lcom/google/android/apps/plus/service/AndroidContactsNotificationService;->startService
    (Landroid/content/Intent;)Landroid/content/ComponentName;

    .line 29
            return-void
    .end method
*/

}
