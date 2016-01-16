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

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;
import com.appsimobile.appsii.module.BaseContactInfo;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;

import javax.inject.Inject;

/**
 * A fragment that allows the user to load a profile image to show in the fragment.
 * <p/>
 * Created by nick on 24/01/15.
 */
public class ProfileImageFragment extends Fragment implements View.OnClickListener,
        ContactPickerDialog.ContactPickedListener {

    @Inject
    HomeItemConfiguration mConfigurationHelper;

    long mCellId;

    String mLookupKey;

    long mContactId;

    ImageView mPreviewImage;

    Toolbar mToolbar;

    View mSelectContactButton;


    public static ProfileImageFragment createInstance(long cellId) {
        ProfileImageFragment result = new ProfileImageFragment();
        Bundle args = new Bundle();
        args.putLong("cellId", cellId);
        result.setArguments(args);
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);
        Bundle arguments = getArguments();
        mCellId = arguments.getLong("cellId");
        mLookupKey = mConfigurationHelper.getProperty(mCellId, "lookupKey", null);
        String contactId = mConfigurationHelper.getProperty(mCellId, "contactId", null);
        mContactId = contactId == null ? -1L : Long.parseLong(contactId);

        ContactPickerDialog dialog =
                (ContactPickerDialog) getFragmentManager().findFragmentByTag("contact_picker");
        if (dialog != null) {
            dialog.setContactPickedListener(this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_image, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreviewImage = (ImageView) view.findViewById(R.id.preview_image);

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);

        mSelectContactButton = view.findViewById(R.id.choose_contact_button);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            mSelectContactButton.setOutlineProvider(new ViewOutlineProvider() {

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
        }
        mSelectContactButton.setOnClickListener(this);

        // start the loader here, because the view now exists.
        getLoaderManager().initLoader(0, null, new ContactLoaderCallbacks());
    }

    void onContactLoaded(Contact contact) {
        Bitmap bitmap = contact == null ? null : contact.mBitmap;
        if (bitmap == null) {
            mPreviewImage.setImageResource(R.drawable.fallback_profile_image);
        } else {
            mPreviewImage.setImageBitmap(contact.mBitmap);
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.choose_contact_button) {
            showContactPicker();
        }
    }

    private void showContactPicker() {
        ContactPickerDialog dialog = new ContactPickerDialog();
        dialog.setContactPickedListener(this);
        dialog.show(getFragmentManager(), "contact_picker");
    }

    @Override
    public void onContactPicked(BaseContactInfo contactInfo) {
        mContactId = contactInfo.mContactId;
        mLookupKey = contactInfo.mLookupKey;

        mConfigurationHelper.updateProperty(mCellId, "lookupKey", mLookupKey);
        mConfigurationHelper.updateProperty(mCellId, "contactId", String.valueOf(mContactId));

        getLoaderManager().restartLoader(0, null, new ContactLoaderCallbacks());
    }


    class ContactLoaderCallbacks implements LoaderManager.LoaderCallbacks<Contact> {

        @Override
        public Loader<Contact> onCreateLoader(int id, Bundle args) {
            return new RawContactsLoader(getActivity(), mLookupKey, mContactId);
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact contact) {
            onContactLoaded(contact);
        }

        @Override
        public void onLoaderReset(Loader<Contact> loader) {
        }
    }

}