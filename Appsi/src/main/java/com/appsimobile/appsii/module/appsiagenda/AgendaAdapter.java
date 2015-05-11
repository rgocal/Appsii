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

package com.appsimobile.appsii.module.appsiagenda;

import android.support.v7.widget.RecyclerView;
import android.text.format.Time;

import java.util.List;

/**
 * Created by nick on 07/01/15.
 */
public abstract class AgendaAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    public abstract void setOnAgendaItemClickListener(
            ItemAgendaViewHolder.OnItemClickListener onItemClickListener);

    public abstract float getHeaderScrollPercentage();

    /**
     * Sets the date in the given time to the date of the event at the given position.
     * If the method returns false, no valid time could be determined for the given position
     *
     * @param time The time to set the date into
     * @param position the position of the item to get the date from
     *
     * @return true when a position was available for the given time. False otherwise
     */
    public abstract boolean getTimeAtPosition(Time time, int position);

    public abstract int positionOfDate(int year, int month, int day);

    public abstract int positionOfJulianDay(int julianDay);

    public abstract int setAgendaEvents(List<AgendaEvent> data);

    /**
     * Returns true when a new week starts at the given position
     */
    public abstract boolean hasWeekNumberAtPosition(int position);

    /**
     * Returns the julian day at the given position
     */
    abstract int getJulianDayAtPosition(int position);

    public abstract void focusPosition(int agendaPosition);

    public abstract void clear();

}
