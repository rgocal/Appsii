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

package com.appsimobile.appsii.compat;

import android.graphics.drawable.Drawable;

import java.util.List;

public abstract class UserManagerCompat {

    protected UserManagerCompat() {
    }

    public abstract List<UserHandleCompat> getUserProfiles();

    public abstract long getSerialNumberForUser(UserHandleCompat user);

    public abstract UserHandleCompat getUserForSerialNumber(long serialNumber);

    public abstract Drawable getBadgedDrawableForUser(Drawable unbadged, UserHandleCompat user);

    public abstract CharSequence getBadgedLabelForUser(CharSequence label, UserHandleCompat user);
}
