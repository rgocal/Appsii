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

package com.appsimobile.appsii.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;

import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.AppsiiUtils;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.icontheme.iconpack.ActiveIconPackInfo;

public class AppIconHelper {

    private static final Canvas sScaleCanvas = new Canvas();

    private static Paint sPaint = new Paint();

    public static Bitmap getIcon(Context context, Uri path, Bitmap defaultBitmap,
            boolean largeIcon) {
        //Intent i = mPackageManager.getLaunchIntentForPackage(name.getPackageName());
        //Resources resources = mPackageManager.getResourcesForApplication(name.getPackageName());
        Bitmap result = loadBitmap(context, path, largeIcon);
        if (result == null) {
            result = defaultBitmap != null ? defaultBitmap :
                    AppsiApplication.getDefaultAppIcon(context);
            result = ActiveIconPackInfo.getInstance(context).decorateDefaultIcon(context, result);
        }
        return result;
    }

    private static Bitmap loadBitmap(Context context, Uri path, boolean largeIcon) {
        int key = largeIcon ? R.dimen.large_icon : R.dimen.small_icon;
        int dimen = (int) context.getResources().getDimension(key);
        return loadBitmap(context, path, dimen, dimen);
    }

    private static Bitmap loadBitmap(Context c, Uri path, int w, int h) {
        Bitmap result = loadBitmapImpl(c, path, w, h);
        if (result != null) {
            result = createScaledBitmap(result, w, h, 0);
        }
        return result;
    }

    private static Bitmap loadBitmapImpl(Context c, Uri path, int w, int h) {
        if (path != null) {
            path = path.buildUpon().
                    appendQueryParameter(AppsiiUtils.PARAM_APPSI_ICON_WIDTH,
                            String.valueOf(w)).
                    appendQueryParameter(AppsiiUtils.PARAM_APPSI_ICON_HEIGHT,
                            String.valueOf(h)).
                    build();

            return ActiveIconPackInfo.getInstance(c).loadIconFromUri(c, path);
        }

        return null;
    }

    private static synchronized Bitmap createScaledBitmap(Bitmap bitmap, final int width,
            final int height, float cornerRadius) {

        if (bitmap == null) {
            return null;
        }

        int adjustedWidth = width;
        int adjustedHeight = height;

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        //int inBytes = bitmap.getByteCount();
        if (width >= bitmapWidth && height >= bitmapHeight) return bitmap;

        if (width > 0 && height > 0) {
            //if (width < bitmapWidth || height < bitmapHeight) {
            final float ratio = (float) bitmapWidth / bitmapHeight;

            if (bitmapWidth > bitmapHeight) {
                adjustedHeight = (int) (width / ratio);
            } else if (bitmapHeight > bitmapWidth) {
                adjustedWidth = (int) (height * ratio);
            }

            final Bitmap.Config c = Bitmap.Config.ARGB_8888;
            final Bitmap thumb = Bitmap.createBitmap(width, height, c);
            final Canvas canvas = sScaleCanvas;
            final Paint paint = sPaint;
            canvas.setBitmap(thumb);
            paint.setDither(false);
            paint.setFilterBitmap(true);

            Rect sBounds = new Rect();
            Rect sOldBounds = new Rect();

            sBounds.set((width - adjustedWidth) >> 1, (height - adjustedHeight) >> 1, adjustedWidth,
                    adjustedHeight);
            sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);

            if (cornerRadius != 0) {
                //Path p = new Path();
                RectF rect = new RectF(sBounds);
                canvas.drawARGB(0, 0, 0, 0);
                paint.setColor(Color.WHITE);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
                paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
                //p.addRoundRect(rect, cornerRadius, cornerRadius, Direction.CCW);
                //canvas.clipPath(p, Op.REPLACE);
            } else {
                paint.setXfermode(null);
                //canvas.clipRect(0, 0, thumb.getWidth(), thumb.getHeight());
            }

            canvas.drawBitmap(bitmap, sOldBounds, sBounds, paint);

            canvas.setBitmap(Bitmap.createBitmap(1, 1, Config.ALPHA_8));

            return thumb;

        }
        return bitmap;
    }

    public static Bitmap loadAppIcon(Context c, Uri path, int w, int h) {
        if (path != null) {
            path = path.buildUpon().
                    appendQueryParameter(AppsiiUtils.PARAM_APPSI_ICON_WIDTH,
                            String.valueOf(w)).
                    appendQueryParameter(AppsiiUtils.PARAM_APPSI_ICON_HEIGHT,
                            String.valueOf(h)).
                    build();

            return ActiveIconPackInfo.getInstance(c).loadThemedAppIconFromUri(c, path);
        }

        return null;
    }


    public static Bitmap loadAppIcon(Context c, ComponentName componentName, int w, int h) {
        if (componentName != null) {

            return ActiveIconPackInfo.getInstance(c).loadThemedAppIconFromComponentName(c,
                    componentName);
        }

        return null;
    }


}
