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

import android.net.Uri;

/**
 * Created by Nick Martens on 9/22/13.
 */
public class IconPackFactory {

    public static IconPack createIconPack(Uri uri) {
        if (uri == null) {
            return null;
        }
        String packType = AbstractIconPack.getIconPackTypeIdFromUri(uri);
        String packageName = AbstractIconPack.getIconPackPackageFromUri(uri);

        if (packType.equals(NovaIconPack.NOVA_PACK_ID)) {
            return new NovaIconPack(packageName);
        } else if (packType.equals(ApexIconPack.APEX_PACK_ID)) {
            return new ApexIconPack(packageName, null);
        } else if (packType.equals(AdwIconPack.ADW_PACK_ID)) {
            return new AdwIconPack(uri, packageName);
        }
        return null;
    }

}
