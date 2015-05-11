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

import com.appsimobile.appsii.HotspotItem;

/**
 * A listener that can be added to the hotspot viewholder.
 * This informs the client of the selected user action
 * Created by nick on 31/01/15.
 */
public interface HotspotActionListener {

    /**
     * The user tapped the hotspot
     */
    void performMainAction(HotspotItem configuration);

    /**
     * The user selected the move action
     */
    void performMoveHotspotAction(HotspotItem configuration);

    /**
     * The user selected the delete action
     */
    void performDeleteHotspotAction(HotspotItem configuration);

    /**
     * The user selected the change height action
     */
    void performSetHeightHotspotAction(HotspotItem configuration);

}
