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
 *
 */

package com.appsimobile.appsii;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.permissions.PermissionUtils;

import javax.inject.Inject;

/**
 * Created by nick on 17/06/15.
 */
public class RequestPermissionActivity extends Activity {

    @Inject
    PermissionUtils mPermissionUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);

        Intent intent = getIntent();
        String[] permissions = intent.getStringArrayExtra(PermissionUtils.EXTRA_PERMISSIONS);
        int requestCode = intent.getIntExtra(PermissionUtils.EXTRA_REQUEST_CODE, 0);
        mPermissionUtils.requestPermission(this, requestCode, permissions);
    }

    //@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        sendGrantedBroadcast(requestCode, permissions, grantResults);
    }

    private void sendGrantedBroadcast(int requestCode, String[] permission, int[] grantResults) {
        Intent intent = new Intent(PermissionUtils.ACTION_PERMISSION_RESULT);
        intent.putExtra(PermissionUtils.EXTRA_REQUEST_CODE, requestCode);
        intent.putExtra(PermissionUtils.EXTRA_PERMISSIONS, permission);
        intent.putExtra(PermissionUtils.EXTRA_GRANT_RESULTS, grantResults);
        sendBroadcast(intent);
        finish();
    }
}
