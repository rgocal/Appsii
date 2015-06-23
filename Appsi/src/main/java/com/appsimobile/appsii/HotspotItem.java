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

public class HotspotItem implements Parcelable {


    public static final long NO_ID = -1;

    public static final Parcelable.Creator<HotspotItem> CREATOR
            = new Parcelable.Creator<HotspotItem>() {
        @Override
        public HotspotItem createFromParcel(Parcel in) {
            // This call can't have more access.
            //noinspection PrivateMemberAccessBetweenOuterAndInnerClass
            return new HotspotItem(in);
        }

        @Override
        public HotspotItem[] newArray(int size) {
            return new HotspotItem[size];
        }
    };

    public long mId = NO_ID;

    public long mDefaultPageId;

    public float mHeightRelativeToViewHeight;

    public float mYPosRelativeToView;

    public boolean mLeft;

    public String mName;

    public boolean mNeedsConfiguration;

    HotspotItem.ConfigurationListener mConfigurationListener;

    public HotspotItem() {
    }

    private HotspotItem(Parcel in) {
        mId = in.readLong();
        mDefaultPageId = in.readLong();
        mHeightRelativeToViewHeight = in.readFloat();
        mYPosRelativeToView = in.readFloat();
        mLeft = in.readInt() == 1;
        mName = in.readString();
        mNeedsConfiguration = in.readInt() == 1;
    }

    public void setHeightRelativeToViewHeight(float heightRelativeToViewHeight) {
        float old = mHeightRelativeToViewHeight;
        mHeightRelativeToViewHeight = heightRelativeToViewHeight;
        if (mConfigurationListener != null) {
            mConfigurationListener.onHotspotConfigurationChanged(this, heightRelativeToViewHeight,
                    old);
        }
    }

    public void setConfigurationListener(
            HotspotItem.ConfigurationListener configurationListener) {
        mConfigurationListener = configurationListener;
    }

    public void init(long id, String name, float height, float ypos, boolean left,
            boolean needsConfiguration, long defaultPageId) {
        mId = id;
        mName = name;
        mLeft = left;
        mHeightRelativeToViewHeight = height;
        mYPosRelativeToView = ypos;
        mNeedsConfiguration = needsConfiguration;
        mDefaultPageId = defaultPageId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mId);
        out.writeLong(mDefaultPageId);
        out.writeFloat(mHeightRelativeToViewHeight);
        out.writeFloat(mYPosRelativeToView);
        out.writeInt(mLeft ? 1 : 0);
        out.writeString(mName);
        out.writeInt(mNeedsConfiguration ? 1 : 0);
    }


    public interface ConfigurationListener {

        void onHotspotConfigurationChanged(HotspotItem hotspotItem,
                float heightRelativeToViewHeight, float old);
    }


}