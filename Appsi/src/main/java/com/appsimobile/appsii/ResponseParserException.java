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

import android.support.annotation.Nullable;

/**
 * An exception throws when something is not as expected in the returned response
 * <p/>
 * Created by Nick on 08/10/14.
 */
public class ResponseParserException extends Exception {

    public ResponseParserException(String detailMessage) {
        super(detailMessage);
    }

    public ResponseParserException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ResponseParserException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Throws a ResponseParserException when the given object is null.
     *
     * @throws com.appsimobile.appsii.ResponseParserException
     */
    static void throwIfNull(@Nullable Object o, String path) throws ResponseParserException {
        if (o == null) {
            throw new ResponseParserException("Missing [" + path + "] in json response");
        }
    }

    /**
     * Creates, but does not throw the exception for a path that is missing
     */
    public static ResponseParserException forPath(String path) {
        return new ResponseParserException("Missing [" + path + "] in json response");
    }

}
