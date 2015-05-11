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

package com.appsimobile.appsii.hotspotmanager;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.appsimobile.appsii.HotspotItem;

/**
 * A simple viewholder for the hotspots. This shows the title of the hotspot and
 * an overflow button with additional options
 * Created by nick on 31/01/15.
 */
public abstract class AbsHotspotViewHolder extends RecyclerView.ViewHolder {

    public AbsHotspotViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void bind(HotspotItem hotspotItem);


}
