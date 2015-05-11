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

package com.appsimobile.appsii.module.avatar;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

/**
 * Created by nick on 07/06/14.
 */
public class AvatarBuilder {

    /**
     * Contact type constants used for default letter images
     */
    public static final int TYPE_PERSON = LetterTileDrawable.TYPE_PERSON;

    public static final int TYPE_BUSINESS = LetterTileDrawable.TYPE_BUSINESS;

    public static final int TYPE_VOICEMAIL = LetterTileDrawable.TYPE_VOICEMAIL;

    public static final int TYPE_DEFAULT = LetterTileDrawable.TYPE_DEFAULT;

    /**
     * Scale and offset default constants used for default letter images
     */
    public static final float SCALE_DEFAULT = 1.0f;

    public static final float OFFSET_DEFAULT = 0.0f;

    /**
     * Uri-related constants used for default letter images
     */
    private static final String DISPLAY_NAME_PARAM_KEY = "display_name";

    private static final String IDENTIFIER_PARAM_KEY = "identifier";

    private static final String CONTACT_TYPE_PARAM_KEY = "contact_type";

    private static final String SCALE_PARAM_KEY = "scale";

    private static final String OFFSET_PARAM_KEY = "offset";

    private static final String DEFAULT_IMAGE_URI_SCHEME = "defaultimage";

    private static final Uri DEFAULT_IMAGE_URI = Uri.parse(DEFAULT_IMAGE_URI_SCHEME + "://");

    static Drawable sDefaultLetterAvatar;

    /**
     * Obtain the default drawable for a contact when no photo is available. If this is a local
     * contact, then use the contact's display name and lookup key (as a unique identifier) to
     * retrieve a default drawable for this contact. If not, then use the name as the contact
     * identifier instead.
     */
    public static DefaultImageRequest createDefaultImageRequest(boolean isOrganization,
            String lookupKey,
            String displayName) {
        DefaultImageRequest request;
        int contactType = isOrganization ? TYPE_BUSINESS : TYPE_DEFAULT;

        if (TextUtils.isEmpty(lookupKey)) {
            return new DefaultImageRequest(null, displayName, contactType);
        } else {
            return new DefaultImageRequest(displayName, lookupKey, contactType);
        }
    }


    /**
     * Given a {@link DefaultImageRequest}, returns a {@link android.graphics.drawable.Drawable},
     * that when drawn, will
     * draw a letter tile avatar based on the request parameters defined in the
     * {@link DefaultImageRequest}.
     */
    public static Drawable getDefaultAvatarDrawableForContact(Resources resources, boolean hires,
            DefaultImageRequest defaultImageRequest) {
        return getDefaultAvatarDrawableForContact(resources, hires, defaultImageRequest, false);
    }

    /**
     * Given a {@link DefaultImageRequest}, returns a {@link android.graphics.drawable.Drawable},
     * that when drawn, will
     * draw a letter tile avatar based on the request parameters defined in the
     * {@link DefaultImageRequest}.
     */
    public static Drawable getDefaultAvatarDrawableForContact(Resources resources, boolean hires,
            DefaultImageRequest defaultImageRequest, boolean square) {
        if (defaultImageRequest == null) {
            if (sDefaultLetterAvatar == null) {
                // Cache and return the letter tile drawable that is created by a null request,
                // so that it doesn't have to be recreated every time it is requested again.
                sDefaultLetterAvatar = LetterTileDefaultImageProvider.getDefaultImageForContact(
                        resources, null);
            }
            return sDefaultLetterAvatar;
        }
        LetterTileDrawable d = LetterTileDefaultImageProvider.getDefaultImageForContact(resources,
                defaultImageRequest);
        d.setSquared(square);
        return d;
    }

    /**
     * A default image provider that applies a letter tile consisting of a colored background
     * and a letter in the foreground as the default image for a contact. The color of the
     * background and the type of letter is decided based on the contact's details.
     */
    private static class LetterTileDefaultImageProvider extends DefaultImageProvider {

        public static LetterTileDrawable getDefaultImageForContact(Resources resources,
                DefaultImageRequest defaultImageRequest) {
            final LetterTileDrawable drawable = new LetterTileDrawable(resources);
            if (defaultImageRequest != null) {
                // If the contact identifier is null or empty, fallback to the
                // displayName. In that case, use {@code null} for the contact's
                // display name so that a default bitmap will be used instead of a
                // letter
                if (TextUtils.isEmpty(defaultImageRequest.identifier)) {
                    drawable.setContactDetails(null, defaultImageRequest.displayName);
                } else {
                    drawable.setContactDetails(defaultImageRequest.displayName,
                            defaultImageRequest.identifier);
                }
                drawable.setContactType(defaultImageRequest.contactType);
                drawable.setScale(defaultImageRequest.scale);
                drawable.setOffset(defaultImageRequest.offset);
            }
            return drawable;
        }

        @Override
        public void applyDefaultImage(ImageView view, int extent, boolean darkTheme,
                DefaultImageRequest defaultImageRequest) {
            final Drawable drawable = getDefaultImageForContact(view.getResources(),
                    defaultImageRequest);
            view.setImageDrawable(drawable);
        }
    }


    public static abstract class DefaultImageProvider {

        /**
         * Applies the default avatar to the ImageView. Extent is an indicator for the size (width
         * or height). If darkTheme is set, the avatar is one that looks better on dark background
         *
         * @param defaultImageRequest {@link DefaultImageRequest} object that specifies how a
         * default letter tile avatar should be drawn.
         */
        public abstract void applyDefaultImage(ImageView view, int extent, boolean darkTheme,
                DefaultImageRequest defaultImageRequest);
    }


    /**
     * Contains fields used to contain contact details and other user-defined settings that might
     * be used by the ContactPhotoManager to generate a default contact image. This contact image
     * takes the form of a letter or bitmap drawn on top of a colored tile.
     */
    public static class DefaultImageRequest {

        /**
         * The contact's display name. The display name is used to
         */
        public String displayName;

        /**
         * A unique and deterministic string that can be used to identify this contact. This is
         * usually the contact's lookup key, but other contact details can be used as well,
         * especially for non-local or temporary contacts that might not have a lookup key. This
         * is used to determine the color of the tile.
         */
        public String identifier;

        /**
         * The type of this contact. This contact type may be used to decide the kind of
         * image to use in the case where a unique letter cannot be generated from the contact's
         * display name and identifier. See:
         * {@link #TYPE_PERSON}
         * {@link #TYPE_BUSINESS}
         * {@link #TYPE_PERSON}
         * {@link #TYPE_DEFAULT}
         */
        public int contactType = TYPE_DEFAULT;

        /**
         * The amount to scale the letter or bitmap to, as a ratio of its default size (from a
         * range of 0.0f to 2.0f). The default value is 1.0f.
         */
        public float scale = SCALE_DEFAULT;

        /**
         * The amount to vertically offset the letter or image to within the tile.
         * The provided offset must be within the range of -0.5f to 0.5f.
         * If set to -0.5f, the letter will be shifted upwards by 0.5 times the height of the canvas
         * it is being drawn on, which means it will be drawn with the center of the letter starting
         * at the top edge of the canvas.
         * If set to 0.5f, the letter will be shifted downwards by 0.5 times the height of the
         * canvas it is being drawn on, which means it will be drawn with the center of the letter
         * starting at the bottom edge of the canvas.
         * The default is 0.0f, which means the letter is drawn in the exact vertical center of
         * the tile.
         */
        public float offset = OFFSET_DEFAULT;

        public DefaultImageRequest() {
        }

        public DefaultImageRequest(String displayName, String identifier) {
            this(displayName, identifier, TYPE_DEFAULT, SCALE_DEFAULT, OFFSET_DEFAULT);
        }

        public DefaultImageRequest(String displayName, String identifier, int contactType,
                float scale, float offset) {
            this.displayName = displayName;
            this.identifier = identifier;
            this.contactType = contactType;
            this.scale = scale;
            this.offset = offset;
        }

        public DefaultImageRequest(String displayName, String identifier, int contactType) {
            this(displayName, identifier, contactType, SCALE_DEFAULT, OFFSET_DEFAULT);
        }


    }


}
