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

package com.appsimobile.appsii.module.home;

import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.appsii.module.PeopleQuery;
import com.appsimobile.appsii.module.people.AbstractPeopleViewHolder;
import com.appsimobile.appsii.module.people.ContactView;
import com.appsimobile.appsii.module.people.PeopleViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 28/03/15.
 */
public class ContactPickerDialog extends DialogFragment
        implements PeopleViewHolder.OnItemClickListener {

    Button mOkButton;

    Button mCancelButton;

    RecyclerView mRecyclerView;

    ProfileImageAdapter mProfileImageAdapter;

    ContactPickedListener mContactPickedListener;

    public ContactPickerDialog() {
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProfileImageAdapter = new ProfileImageAdapter(getActivity());
        mProfileImageAdapter.setOnItemClickListener(this);
        getLoaderManager().initLoader(1, null, new CursorLoaderCallbacks());
    }

    public void setContactPickedListener(
            ContactPickedListener contactPickedListener) {
        mContactPickedListener = contactPickedListener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_contact_picker, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mOkButton = (Button) view.findViewById(R.id.ok_button);
        mCancelButton = (Button) view.findViewById(R.id.cancel);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.contact_recycler);

        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        mRecyclerView.setAdapter(mProfileImageAdapter);
    }

    void onContactListLoaded(List<? extends BaseContactInfo> contactInfos) {
        mProfileImageAdapter.setData(contactInfos);
    }


    @Override
    public void onItemClick(AbstractPeopleViewHolder viewHolder) {
        onContactSelected(((PeopleViewHolder) viewHolder).getContactInfo());
    }

    private void onContactSelected(BaseContactInfo contactInfo) {
        if (mContactPickedListener != null) {
            mContactPickedListener.onContactPicked(contactInfo);
        }
        dismiss();
    }

    interface ContactPickedListener {

        void onContactPicked(BaseContactInfo contactInfo);
    }

    /**
     * Created by nick on 02/11/14.
     */
    static class ProfileImageAdapter extends RecyclerView.Adapter<AbstractPeopleViewHolder> {

        private final List<BaseContactInfo> mContactInfos = new ArrayList<>();

        PeopleViewHolder.OnItemClickListener mOnItemClickListener;

        private Context mContext;

        public ProfileImageAdapter(Context context) {
            mContext = context;
            setHasStableIds(true);
        }

        public BaseContactInfo getItem(int position) {
            return mContactInfos.get(position);
        }

        @Override
        public AbstractPeopleViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ContactView view =
                    (ContactView) inflater.inflate(R.layout.grid_item_people_tile, viewGroup,
                            false);
            view.setShowAsTiles(true);
//            View view = inflater.inflate(R.layout.list_item_people_entry, viewGroup, false);
            return new PeopleViewHolder(view, mOnItemClickListener,
                    null /* peopleActionListener */);
        }

        @Override
        public void onBindViewHolder(AbstractPeopleViewHolder viewHolder,
                int i) {
            BaseContactInfo info = mContactInfos.get(i);
            // if there is no contact info, bind to null
            // and no need to get any previous items
            if (info == null) {
                viewHolder.bind(null, null, false);
                return;
            }

            String firstLetter = info.getFirstLetter();

            boolean isFirstWithLetter;
            if (i == 0) {
                isFirstWithLetter = true;
            } else {
                BaseContactInfo prev = mContactInfos.get(i - 1);
                if (prev != null) {
                    String prevLetter = prev.getFirstLetter();
                    isFirstWithLetter =
                            !TextUtils.equals(prevLetter.toLowerCase(), firstLetter.toLowerCase());
                } else {
                    isFirstWithLetter = true;
                }
            }
            viewHolder.bind(info, firstLetter, isFirstWithLetter);

        }

        @Override
        public long getItemId(int position) {
            BaseContactInfo contactInfo = mContactInfos.get(position);
            return contactInfo.mContactId;
        }


        @Override
        public int getItemCount() {
            return mContactInfos.size();
        }

        public void setData(List<? extends BaseContactInfo> contactInfos) {
            mContactInfos.clear();

            for (int i = 0; i < contactInfos.size(); i++) {
                BaseContactInfo contactInfo = contactInfos.get(i);
                mContactInfos.add(contactInfo);
            }
            notifyDataSetChanged();
        }

        public void clear() {
            mContactInfos.clear();
            notifyDataSetChanged();
        }

        public void onTrimMemory(int level) {
        }

        public void setOnItemClickListener(
                PeopleViewHolder.OnItemClickListener onItemClickListener) {
            mOnItemClickListener = onItemClickListener;
        }

    }

    class CursorLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Uri baseUri;
            baseUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                    .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                            String.valueOf(ContactsContract.Directory.DEFAULT)).build();

            String select =
                    "((" + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL) AND ("
                            + ContactsContract.Contacts.DISPLAY_NAME + " != '' ))";

            return new CursorLoader(getActivity(), baseUri,
                    PeopleQuery.CONTACTS_SUMMARY_PROJECTION, select, null,
                    ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            List<? extends BaseContactInfo> contactInfos =
                    PeopleQuery.cursorToContactInfos(data);
            onContactListLoaded(contactInfos);
        }


        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }

    }

}
