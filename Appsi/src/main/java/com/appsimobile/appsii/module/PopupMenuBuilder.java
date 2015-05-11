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

package com.appsimobile.appsii.module;

import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

/**
 * Created by nick on 30/05/14.
 */
public class PopupMenuBuilder {

    final View mView;

    final Context mContext;

    PopupMenu.OnMenuItemClickListener mListener;

    PopupMenu mPopupMenu;

    public PopupMenuBuilder(View view) {
        mView = view;
        mContext = view.getContext();
    }

    public PopupMenuBuilder listener(PopupMenu.OnMenuItemClickListener l) {
        mListener = l;
        if (mPopupMenu != null) {
            mPopupMenu.setOnMenuItemClickListener(l);
        }
        return this;
    }

    public PopupMenuBuilder addAction(int id, CharSequence title) {
        ensureCreated();
        mPopupMenu.getMenu().add(0, id, Menu.NONE, title);
        return this;
    }

    private void ensureCreated() {
        if (mPopupMenu == null) {
            mPopupMenu = new PopupMenu(mContext, mView);
            if (mListener != null) {
                mPopupMenu.setOnMenuItemClickListener(mListener);
            }
        }
    }

    public PopupMenuBuilder addAction(int id, int titleResId) {
        ensureCreated();
        mPopupMenu.getMenu().add(0, id, Menu.NONE, titleResId);
        return this;
    }


    public PopupMenuBuilder addAction(int id, int titleResId, Object... formatArgs) {
        ensureCreated();
        CharSequence title = mContext.getString(titleResId, formatArgs);
        mPopupMenu.getMenu().add(0, id, Menu.NONE, title);
        return this;
    }

    public PopupMenuBuilder inflate(int menuResId) {
        ensureCreated();
        mPopupMenu.inflate(menuResId);
        return this;
    }

    public void show() {
        ensureCreated();
        mPopupMenu.show();
    }

}
