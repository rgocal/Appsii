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

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Nick on 09/10/14.
 */
public final class AssetUtils {

    private AssetUtils() {
    }

    public static String readAssetToString(AssetManager assetManager, String fileName,
            StringBuilder stringBuilder) {

        try {
            return readAssetToStringImpl(assetManager, fileName, stringBuilder);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading file", e);
        }

    }

    private static String readAssetToStringImpl(AssetManager assetManager, String fileName,
            StringBuilder stringBuilder) throws IOException {

        stringBuilder.setLength(0);

        InputStream in = assetManager.open(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }

        return stringBuilder.toString();

    }


}
