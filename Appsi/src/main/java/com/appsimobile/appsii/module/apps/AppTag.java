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

package com.appsimobile.appsii.module.apps;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Represents a tag to which an app can be added
 * Created by nick on 24/08/14.
 */
public class AppTag implements Comparable<AppTag>, Parcelable {

    public static final Parcelable.Creator<AppTag> CREATOR
            = new Parcelable.Creator<AppTag>() {
        public AppTag createFromParcel(Parcel in) {
            return new AppTag(in);
        }

        public AppTag[] newArray(int size) {
            return new AppTag[size];
        }
    };

    /**
     * The id of the tag
     */
    public final long id;

    /**
     * The name of the tag
     */
    public final String title;

    /**
     * Should this be expanded by default?
     */
    public final boolean defaultExpanded;

    /**
     * True if this tag is visible
     */
    public final boolean visible;

    public final int columnCount;

    public final int tagType;

    /**
     * The position of the tag when compared to other tags
     */
    private final int position;

    public AppTag(long id, String title, int position, boolean defaultExpanded, boolean visible,
            int columnCount, int tagType) {
        this.id = id;
        this.title = title;
        this.position = position;
        this.defaultExpanded = defaultExpanded;
        this.visible = visible;
        this.columnCount = columnCount;
        this.tagType = tagType;
    }

    private AppTag(Parcel in) {
        id = in.readLong();
        title = in.readString();
        position = in.readInt();
        defaultExpanded = in.readInt() == 1;
        visible = in.readInt() == 1;
        columnCount = in.readInt();
        tagType = in.readInt();
    }

    public static int intCompare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    @Override
    public int compareTo(@NonNull AppTag another) {
        return intCompare(position, another.position);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppTag appTag = (AppTag) o;

        if (columnCount != appTag.columnCount) return false;
        if (defaultExpanded != appTag.defaultExpanded) return false;
        if (id != appTag.id) return false;
        if (position != appTag.position) return false;
        if (tagType != appTag.tagType) return false;
        if (visible != appTag.visible) return false;
        if (!title.equals(appTag.title)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + title.hashCode();
        result = 31 * result + position;
        result = 31 * result + (defaultExpanded ? 1 : 0);
        result = 31 * result + (visible ? 1 : 0);
        result = 31 * result + columnCount;
        result = 31 * result + tagType;
        return result;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(title);
        out.writeInt(position);
        out.writeInt(defaultExpanded ? 1 : 0);
        out.writeInt(visible ? 1 : 0);
        out.writeInt(columnCount);
        out.writeInt(tagType);
    }

}
