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

package com.appsimobile.appsii.appwidget;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.appsimobile.appsii.R;
import com.appsimobile.appsii.compat.AppWidgetManagerCompat;

import java.lang.ref.SoftReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class WidgetPreviewLoader {

    private static final float WIDGET_PREVIEW_ICON_PADDING_PERCENTAGE = 0.25f;

    final Context mContext;

    final Canvas mCanvas = new Canvas();

    private final AppWidgetManagerCompat mManager;

    private final IconCache mIconCache;

    private final RectCache mCachedAppWidgetPreviewSrcRect = new RectCache();

    private final RectCache mCachedAppWidgetPreviewDestRect = new RectCache();

    private final PaintCache mCachedAppWidgetPreviewPaint = new PaintCache();

    private final PaintCache mDefaultAppWidgetPreviewPaint = new PaintCache();

    private final MainThreadExecutor mMainThreadExecutor = new MainThreadExecutor();

    final int mAppIconSize;

    public WidgetPreviewLoader(Context context, IconCache iconCache) {
        mContext = context.getApplicationContext();
        mIconCache = iconCache;
        mManager = AppWidgetManagerCompat.getInstance(context);
        mAppIconSize = (int) (80 * context.getResources().getDisplayMetrics().density);
    }

    private static void renderDrawableToBitmap(
            Drawable d, Bitmap bitmap, int x, int y, int w, int h) {
        if (bitmap != null) {
            Canvas c = new Canvas(bitmap);
            Rect oldBounds = d.copyBounds();
            d.setBounds(x, y, x + w, y + h);
            d.draw(c);
            d.setBounds(oldBounds); // Restore the bounds
            c.setBitmap(null);
        }
    }

    public Bitmap generateWidgetPreview(AppWidgetProviderInfo info, Bitmap preview) {
        int maxWidth = maxWidthForWidgetPreview(2);
        int maxHeight = maxHeightForWidgetPreview(2);
        return generateWidgetPreview(info, 2, 2,
                maxWidth, maxHeight, preview, null);
    }

    public int maxWidthForWidgetPreview(int spanX) {
        return (int) (60 * spanX * mContext.getResources().getDisplayMetrics().density);
    }

    public int maxHeightForWidgetPreview(int spanY) {
        return (int) (60 * spanY * mContext.getResources().getDisplayMetrics().density);
    }

    public Bitmap generateWidgetPreview(AppWidgetProviderInfo info, int cellHSpan, int cellVSpan,
            int maxPreviewWidth, int maxPreviewHeight, Bitmap preview, int[] preScaledWidthOut) {

        if (info == null) return null;

        // Load the preview image if possible
        if (maxPreviewWidth < 0) maxPreviewWidth = Integer.MAX_VALUE;
        if (maxPreviewHeight < 0) maxPreviewHeight = Integer.MAX_VALUE;
        Drawable drawable = null;
        if (info.previewImage != 0) {
            drawable = mManager.loadPreview(info);
            if (drawable != null) {
                drawable = mutateOnMainThread(drawable);
            } else {
                Log.w("WidgetPreview", "Can't load widget preview drawable 0x" +
                        Integer.toHexString(info.previewImage) + " for provider: " + info.provider);
            }
        }
        int previewWidth;
        int previewHeight;
        Bitmap defaultPreview = null;
        boolean widgetPreviewExists = (drawable != null);
        if (widgetPreviewExists) {
            previewWidth = drawable.getIntrinsicWidth();
            previewHeight = drawable.getIntrinsicHeight();
        } else {
            // Generate a preview image if we couldn't load one
            if (cellHSpan < 1) cellHSpan = 1;
            if (cellVSpan < 1) cellVSpan = 1;
            // This Drawable is not directly drawn, so there's no need to mutate it.
            BitmapDrawable previewDrawable = (BitmapDrawable) mContext.getResources()
                    .getDrawable(R.drawable.widget_tile);
            final int previewDrawableWidth = previewDrawable
                    .getIntrinsicWidth();
            final int previewDrawableHeight = previewDrawable
                    .getIntrinsicHeight();
            previewWidth = previewDrawableWidth * cellHSpan;
            previewHeight = previewDrawableHeight * cellVSpan;
            defaultPreview = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
            final Canvas c = mCanvas;
            int canvasState = c.save();

            c.setBitmap(defaultPreview);
            Paint p = mDefaultAppWidgetPreviewPaint.get();
            if (p == null) {
                p = new Paint();
                p.setShader(new BitmapShader(previewDrawable.getBitmap(),
                        Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
                mDefaultAppWidgetPreviewPaint.set(p);
            }
            final Rect dest = mCachedAppWidgetPreviewDestRect.get();
            dest.set(0, 0, previewWidth, previewHeight);
            c.drawRect(dest, p);
            c.setBitmap(null);
            c.restoreToCount(canvasState);
            // Draw the icon in the top left corner
            int minOffset = (int) (mAppIconSize * WIDGET_PREVIEW_ICON_PADDING_PERCENTAGE);
            int smallestSide = Math.min(previewWidth, previewHeight);
            float iconScale = Math.min((float) smallestSide
                    / (mAppIconSize + 2 * minOffset), 1f);
            try {
                Drawable icon = mManager.loadIcon(info, mIconCache);
                if (icon != null) {
                    int hoffset = (int) ((previewDrawableWidth - mAppIconSize * iconScale) / 2);
                    int yoffset = (int) ((previewDrawableHeight - mAppIconSize * iconScale) / 2);
                    icon = mutateOnMainThread(icon);
                    renderDrawableToBitmap(icon, defaultPreview, hoffset,
                            yoffset, (int) (mAppIconSize * iconScale),
                            (int) (mAppIconSize * iconScale));
                }
            } catch (Resources.NotFoundException ignore) {
            }
        }
        // Scale to fit width only - let the widget preview be clipped in the
        // vertical dimension
        float scale = 1f;
        if (preScaledWidthOut != null) {
            preScaledWidthOut[0] = previewWidth;
        }
        if (previewWidth > maxPreviewWidth) {
            scale = maxPreviewWidth / (float) previewWidth;
        }
        if (scale != 1f) {
            previewWidth = (int) (scale * previewWidth);
            previewHeight = (int) (scale * previewHeight);
        }
        // If a bitmap is passed in, we use it; otherwise, we create a bitmap of the right size
        if (preview == null) {
            preview = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        }
        // Draw the scaled preview into the final bitmap
        int x = (preview.getWidth() - previewWidth) / 2;
        if (widgetPreviewExists) {
            renderDrawableToBitmap(drawable, preview, x, 0, previewWidth,
                    previewHeight);
        } else {
            final Canvas c = mCanvas;
            int canvasState = c.save();
            final Rect src = mCachedAppWidgetPreviewSrcRect.get();
            final Rect dest = mCachedAppWidgetPreviewDestRect.get();
            c.setBitmap(preview);
            src.set(0, 0, defaultPreview.getWidth(), defaultPreview.getHeight());
            dest.set(x, 0, x + previewWidth, previewHeight);
            Paint p = mCachedAppWidgetPreviewPaint.get();
            if (p == null) {
                p = new Paint();
                p.setFilterBitmap(true);
                mCachedAppWidgetPreviewPaint.set(p);
            }
            c.drawBitmap(defaultPreview, src, dest, p);
            c.setBitmap(null);
            c.restoreToCount(canvasState);
        }
        return mManager.getBadgeBitmap(info, preview);
    }

    private Drawable mutateOnMainThread(final Drawable drawable) {
        try {
            return mMainThreadExecutor.submit(new Callable<Drawable>() {
                @Override
                public Drawable call() throws Exception {
                    return drawable.mutate();
                }
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static abstract class SoftReferenceThreadLocal<T> {

        private final ThreadLocal<SoftReference<T>> mThreadLocal;

        public SoftReferenceThreadLocal() {
            mThreadLocal = new ThreadLocal<>();
        }

        public void set(T t) {
            mThreadLocal.set(new SoftReference<>(t));
        }

        public T get() {
            SoftReference<T> reference = mThreadLocal.get();
            T obj;
            if (reference == null) {
                obj = initialValue();
                mThreadLocal.set(new SoftReference<>(obj));
                return obj;
            } else {
                obj = reference.get();
                if (obj == null) {
                    obj = initialValue();
                    mThreadLocal.set(new SoftReference<>(obj));
                }
                return obj;
            }
        }

        abstract T initialValue();
    }

    private static class PaintCache extends SoftReferenceThreadLocal<Paint> {

        PaintCache() {
        }

        @Override
        protected Paint initialValue() {
            return null;
        }
    }

    private static class RectCache extends SoftReferenceThreadLocal<Rect> {

        RectCache() {
        }

        @Override
        protected Rect initialValue() {
            return new Rect();
        }
    }
}