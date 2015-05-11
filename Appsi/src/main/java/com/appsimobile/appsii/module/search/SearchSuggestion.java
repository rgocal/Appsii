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

package com.appsimobile.appsii.module.search;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a search suggestion from the recent queries table.
 * Created by nick on 18/02/15.
 */
public class SearchSuggestion implements Parcelable {

    public static final Parcelable.Creator<SearchSuggestion> CREATOR
            = new Parcelable.Creator<SearchSuggestion>() {
        @Override
        public SearchSuggestion createFromParcel(Parcel in) {
            return new SearchSuggestion(in);
        }

        @Override
        public SearchSuggestion[] newArray(int size) {
            return new SearchSuggestion[size];
        }
    };

    public String query;

    public long id;

    public long lastUsed;

    public SearchSuggestion() {

    }

    private SearchSuggestion(Parcel in) {
        query = in.readString();
        id = in.readLong();
        lastUsed = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(query);
        out.writeLong(id);
        out.writeLong(lastUsed);
    }

    @Override
    public String toString() {
        return "SearchSuggestion{" +
                "query='" + query + '\'' +
                ", id=" + id +
                ", lastUsed=" + lastUsed +
                '}';
    }
}
