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

package com.appsimobile.appsii.module.calls;

import com.appsimobile.appsii.PermissionDeniedException;

import java.util.List;

/**
 * Created by nick on 17/06/15.
 */
public class CallLogResult {

    final List<CallLogEntry> mCallLog;

    final PermissionDeniedException mPermissionDeniedException;

    public CallLogResult(PermissionDeniedException permissionDeniedException) {
        mPermissionDeniedException = permissionDeniedException;
        mCallLog = null;
    }


    public CallLogResult(List<CallLogEntry> callLog) {
        mCallLog = callLog;
        mPermissionDeniedException = null;
    }
}
