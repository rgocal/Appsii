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

package com.appsimobile.appsii.module;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.RoundedImageDrawable;
import com.appsimobile.appsii.module.avatar.AvatarBuilder;

/**
 * Created by nick on 29/05/14.
 */
public abstract class AbstractContactView extends FrameLayout {

    protected BaseContactInfo mContactInfo;

    //    private Drawable mNoImageDrawable;
    PeopleCache mPeopleCache;

    boolean mShowAsTiles;

    private ImageView mImageView;

    private TextView mPrimaryText;

    private String mDisplayName;

    private Drawable mDefaultDrawable;

    private ContactBitmapLoader mImageLoader;

    public AbstractContactView(Context context) {
        this(context, null);
    }

    public AbstractContactView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbstractContactView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setShowAsTiles(boolean showAsTiles) {
        mShowAsTiles = showAsTiles;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mImageView = (ImageView) findViewById(R.id.contact_image);
        mPrimaryText = (TextView) findViewById(R.id.primary_text);
        mPeopleCache = PeopleCache.getInstance();
    }

    public void bindToData(BaseContactInfo baseContactInfo) {
        bindToData(baseContactInfo, baseContactInfo.mDisplayName);
    }

    public void bindToData(BaseContactInfo baseContactInfo, String displayName) {
        long id = baseContactInfo == null ? 0 : baseContactInfo.mContactId;
        Uri contactLookupUri = baseContactInfo == null ? null : baseContactInfo.mContactLookupUri;
        String photoUri = baseContactInfo == null ? null : baseContactInfo.mPhotoUri;
        String name = baseContactInfo == null ? displayName : baseContactInfo.mDisplayName;

        mContactInfo = baseContactInfo;
        mDisplayName = displayName;

        if (mImageLoader != null) {
            mImageLoader.cancel(true);
        }
        boolean dontLoad =
                contactLookupUri == null || mPeopleCache.isKnownNoBitmap(contactLookupUri);

        mDefaultDrawable = loadDefaultDrawable();
        mImageView.setImageDrawable(mDefaultDrawable);

        if (!dontLoad) {
            loadBitmap(contactLookupUri, id, photoUri);
        }

//        mImageView.assignContactUri(contactLookupUri);
        mPrimaryText.setText(name);
    }

    private Drawable loadDefaultDrawable() {
        AvatarBuilder.DefaultImageRequest req = getDefaultImageRequest(mContactInfo, mDisplayName);
        return AvatarBuilder.getDefaultAvatarDrawableForContact(getResources(), true /* hi-res */,
                req, mShowAsTiles);
    }

    private void loadBitmap(Uri contactLookupUri, long id, String photoUri) {
        Bitmap bitmap = mPeopleCache.getBitmap(contactLookupUri);
        applyBitmap(bitmap, false);

        if (bitmap == null) {
            int minDimen = (int) (40 * getResources().getDisplayMetrics().density);
            mImageLoader = new ContactBitmapLoader(getContext(), id, contactLookupUri, photoUri,
                    minDimen) {
                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    if (!isCancelled()) {
                        applyBitmap(bitmap, true);
                    }
                }
            };
            mImageLoader.enqueue();
        }

    }

    private AvatarBuilder.DefaultImageRequest getDefaultImageRequest(BaseContactInfo info,
            String displayName) {
        if (info != null) {
            return info.getDefaultImageRequest();
        }
        return AvatarBuilder.createDefaultImageRequest(false, null, displayName);
    }

    void applyBitmap(Bitmap bitmap, boolean loaded) {
        BaseContactInfo info = mContactInfo;

        if (mDefaultDrawable == null) {
            mDefaultDrawable = loadDefaultDrawable();
        }

        if (bitmap != null) {
            Drawable from = mDefaultDrawable;
            Drawable to;
            if (mShowAsTiles) {
                to = new BitmapDrawable(getResources(), bitmap);
            } else {
                to = new RoundedImageDrawable(getResources(), bitmap);
            }
            TransitionDrawable drawable = new TransitionDrawable(
                    new Drawable[]{
                            from, to
                    }
            );
            mImageView.setImageDrawable(drawable);
            drawable.startTransition(150);
        } else {
            mImageView.setImageDrawable(mDefaultDrawable);
        }
    }

    public BaseContactInfo getContactInfo() {
        return mContactInfo;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mImageLoader != null) {
            mImageLoader.cancel(true);
        }
    }
}
