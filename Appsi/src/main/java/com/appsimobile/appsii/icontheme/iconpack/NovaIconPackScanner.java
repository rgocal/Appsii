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

package com.appsimobile.appsii.icontheme.iconpack;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick Martens on 9/14/13.
 */
public class NovaIconPackScanner implements IconPackFormatScanner {

    public static final String NOVA_THEME_ACTION = "com.novalauncher.THEME";

    @Override
    public ArrayList<IconPack> scan(Context context) {
        Intent intent = new Intent(NOVA_THEME_ACTION);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        int count = resolveInfos.size();

        ArrayList<IconPack> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            String packageName = resolveInfo.activityInfo.packageName;
            NovaIconPack iconPack = new NovaIconPack(packageName);
            result.add(iconPack);
        }

        return result;
    }

}
