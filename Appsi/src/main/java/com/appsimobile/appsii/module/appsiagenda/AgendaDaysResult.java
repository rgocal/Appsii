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

package com.appsimobile.appsii.module.appsiagenda;

import android.util.SparseBooleanArray;

import com.appsimobile.appsii.PermissionDeniedException;

/**
 * Created by nick on 17/06/15.
 */
public class AgendaDaysResult {

    final SparseBooleanArray mResult;

    final PermissionDeniedException mException;

    public AgendaDaysResult(PermissionDeniedException exception) {
        mException = exception;
        mResult = null;
    }

    public AgendaDaysResult(SparseBooleanArray result) {
        mResult = result;
        mException = null;
    }
}
