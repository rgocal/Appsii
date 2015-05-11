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

package com.appsimobile.appsii.module.people;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.appsimobile.appsii.module.BaseContactInfo;

/**
 * Created by nick on 02/11/14.
 */
public abstract class AbstractPeopleViewHolder extends RecyclerView.ViewHolder {

    public AbstractPeopleViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void bind(BaseContactInfo info, String firstLetter, boolean isFirstWithLetter);
}
