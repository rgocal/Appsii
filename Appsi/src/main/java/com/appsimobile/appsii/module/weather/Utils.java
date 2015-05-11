/*
 * Copyright 2013 Google Inc.
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

package com.appsimobile.appsii.module.weather;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Because every project needs a Utils class.
 */
public class Utils {

    public static final int SECONDS_MILLIS = 1000; // 1 second is 1000 ms

    public static final int MINUTES_MILLIS = 60 * SECONDS_MILLIS; // 1 minute = 60 sec

    private static final String TAG = "Utils";

    // TODO: Let's use a *real* HTTP library, eh?
    public static HttpURLConnection openUrlConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setUseCaches(false);
        conn.setChunkedStreamingMode(0);
        //conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.connect();
        return conn;
    }


}