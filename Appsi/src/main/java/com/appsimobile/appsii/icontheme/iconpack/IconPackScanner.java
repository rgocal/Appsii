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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick Martens on 9/14/13.
 */
public class IconPackScanner {

    private static List<Class<? extends IconPackFormatScanner>> sScannerList =
            new ArrayList<Class<? extends IconPackFormatScanner>>();

    static {
        sScannerList.add(AdwIconPackScanner.class);
        sScannerList.add(ApexIconPackScanner.class);
        sScannerList.add(NovaIconPackScanner.class);
    }

    private Context mContext;

    private IconPackScanner(Context context) {
        mContext = context;
    }

    public static IconPackScanner iconPackScanner(Context context) {
        return new IconPackScanner(context);
    }

    public List<IconPack> getIconPacks() {
        int count = sScannerList.size();
        List<IconPack> result = new ArrayList<IconPack>();
        for (int i = 0; i < count; i++) {
            Class<? extends IconPackFormatScanner> scannerClass = sScannerList.get(i);
            try {
                IconPackFormatScanner scanner = scannerClass.newInstance();
                List<IconPack> iconPacks = scanner.scan(mContext);
                if (iconPacks != null) {
                    result.addAll(iconPacks);
                }
            } catch (IllegalAccessException e) {
                Log.w("IconPackScanner", "Error instantiating scanner", e);
            } catch (InstantiationException e) {
                Log.w("IconPackScanner", "Error instantiating scanner", e);
            }
        }
        return result;
    }

}
