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

import android.support.annotation.MenuRes;
import android.view.MenuInflater;
import android.view.View;
import android.widget.PopupMenu;

/**
 * Created by nick on 31/01/15.
 */
public class PopupMenuHelper {

    public static PopupMenu showPopupMenu(View v, @MenuRes int menuResId,
            PopupMenu.OnMenuItemClickListener listener) {

        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

        MenuInflater menuInflater = popupMenu.getMenuInflater();
        menuInflater.inflate(menuResId, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(listener);

        popupMenu.show();

        return popupMenu;
    }
}
