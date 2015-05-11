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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by nick on 31/01/15.
 */
public class HotspotPageEntry implements Parcelable {

    public static final Parcelable.Creator<HotspotPageEntry> CREATOR
            = new Parcelable.Creator<HotspotPageEntry>() {
        @SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
        @Override
        public HotspotPageEntry createFromParcel(Parcel in) {
            return new HotspotPageEntry(in);
        }

        @Override
        public HotspotPageEntry[] newArray(int size) {
            return new HotspotPageEntry[size];
        }
    };

    public long mPageId;

    public int mPosition;

    public boolean mEnabled;

    public String mPageName;

    public int mPageType;

    public String mHotspotName;

    public long mHotspotId;

    public HotspotPageEntry() {

    }

    private HotspotPageEntry(Parcel in) {
        mPageId = in.readLong();
        mPosition = in.readInt();
        mEnabled = in.readInt() == 1;
        mPageName = in.readString();
        mPageType = in.readInt();
        mHotspotName = in.readString();
        mHotspotId = in.readLong();
    }

    public long genId() {
        return mHotspotId << 32 | mPageId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mPageId);
        out.writeInt(mPosition);
        out.writeInt(mEnabled ? 1 : 0);
        out.writeString(mPageName);
        out.writeInt(mPageType);
        out.writeString(mHotspotName);
        out.writeLong(mHotspotId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HotspotPageEntry that = (HotspotPageEntry) o;

        return mPageId == that.mPageId && mPageType == that.mPageType;

    }

    @Override
    public int hashCode() {
        int result = (int) (mPageId ^ (mPageId >>> 32));
        result = 31 * result + mPageType;
        return result;
    }
}
