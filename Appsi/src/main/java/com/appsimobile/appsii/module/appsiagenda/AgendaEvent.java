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

/**
 * Represents an event from the calendar provider
 * Created by nick on 22/09/14.
 */
public class AgendaEvent {

    /**
     * True when this is an all-day event
     */
    public boolean allDay;

    /**
     * The julian start day of the event
     */
    public int startDay;

    /**
     * The start time in millis in UTC
     */
    public long startMillis;

    /**
     * The id of the calendar this event belongs to
     */
    public long calendarId;

    /**
     * The Julian day on which this event ends
     */
    public int endDay;

    /**
     * The end time of the event in UTC millis
     */
    public long endMillis;

    /**
     * The color of the event
     */
    public int color;

    /**
     * The id of the event
     */
    public long id;

    /**
     * The title of the event
     */
    public String title;

    /**
     * The name of the calendar this event belongs to
     */
    public String calendarName;
}
