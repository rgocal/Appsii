/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.paintjob;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by nick on 27/04/15.
 */
public class PaintJob {

    public static final int SWATCH_VIBRANT = 0;

    public static final int SWATCH_DARK_VIBRANT = 1;

    public static final int SWATCH_LIGHT_VIBRANT = 2;

    public static final int SWATCH_MUTED = 3;

    public static final int SWATCH_DARK_MUTED = 4;

    public static final int SWATCH_LIGHT_MUTED = 5;

    private static ArgbEvaluator sArgbEvaluator;

    final View mRootView;

    final List<ViewPainter> mViewPainters;

    final BitmapCallback mBitmapCallback;

    final CountDownLatch mCountDownLatch;

    final SparseArray<View> mViews = new SparseArray<>();

    private final BitmapSource mBitmapSource;

    FallBackColors mFallBackColors;

    // volatile to ensure reference is visible after countdown.
    volatile Bitmap mBitmap;

    boolean mCancelled;

    boolean mExecuted;

    AsyncTask<BitmapSource, ?, ?> mLoadTask;

    AsyncTask<?, ?, ?> mPaletteTask;

    int mDuration;

    Palette mPalette;

    PaintJob(
            @NonNull View rootView,
            @NonNull BitmapSource bitmapSource,
            @Nullable FallBackColors fallBackColors,
            @NonNull List<ViewPainter> viewPainters,
            @Nullable BitmapCallback bitmapCallback) {

        mRootView = rootView;
        mFallBackColors = fallBackColors;
        mBitmapSource = bitmapSource;
        mViewPainters = viewPainters;
        mBitmapCallback = bitmapCallback;

        // provide a simple future by implementing call using a countdown-latch.
        mCountDownLatch = new CountDownLatch(1);

    }

    public static Builder newBuilder(View view, BitmapSource bitmapSource) {
        return new ViewBuilderImpl(view, bitmapSource);

    }

    public static Builder newBuilder(View view, Bitmap bitmap) {
        return new ViewBuilderImpl(view, new PlainBitmapSource(bitmap));

    }

    public void execute(int duration) {
        if (mExecuted) throw new IllegalStateException("PaintJob can only execute once");

        mDuration = duration;

        if (mBitmapSource instanceof PlainBitmapSource) {
            Bitmap bitmap = ((PlainBitmapSource) mBitmapSource).mBitmap;
            onBitmapLoaded(bitmap, true /* immediate */);
        } else {
            loadBitmap(mBitmapSource);
        }
    }

    public DerivedBuilder derive(View view) {

        BitmapSource source = new BitmapSource() {
            @Override
            public Bitmap loadBitmapAsync() {
                try {
                    mCountDownLatch.await();
                } catch (InterruptedException e) {
                    return null;
                }
                return mBitmap;
            }
        };
        return new ViewBuilderImpl(view, source);
    }

    private void loadBitmap(final BitmapSource bitmapSource) {
        mLoadTask = new AsyncTask<BitmapSource, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(BitmapSource... params) {
                BitmapSource bitmapSource = params[0];
                return bitmapSource.loadBitmapAsync();
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                onBitmapLoaded(bitmap, false /* immediate */);
            }
        };
        mLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                new BitmapSource[]{bitmapSource});

    }

    void onBitmapLoaded(Bitmap bitmap, final boolean immediate) {
        mBitmap = bitmap;
        mLoadTask = null;

        mCountDownLatch.countDown();

        if (mBitmap == null) {
            onPaletteGenerated(null, false);
        } else {

            mPaletteTask = Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    onPaletteGenerated(palette, immediate);
                }
            });
        }
        if (mBitmapCallback != null) {
            mBitmapCallback.onBitmapLoaded(bitmap, immediate);
        }
    }

    void onPaletteGenerated(@Nullable Palette palette, boolean immediate) {
        // when we are cancelled, don't do anything
        if (mCancelled) return;

        mPalette = palette;
        applyPalette(palette, immediate);
    }

    private void applyPalette(@Nullable Palette palette, boolean immediate) {
        for (ViewPainter viewPainter : mViewPainters) {
            viewPainter.apply(palette, mRootView, this, immediate, mDuration);
        }
    }

    public void cancel() {
        mCancelled = true;
        if (mLoadTask != null) {
            mLoadTask.cancel(true);
            mLoadTask = null;
        }
    }

    FallBackColors getFallbackColors() {
        if (mFallBackColors == null) {
            mFallBackColors = FallBackColors.fromContext(mRootView.getContext());
        }
        return mFallBackColors;
    }

    View findViewById(@IdRes int viewId) {
        View result = mViews.get(viewId);
        if (result == null) {
            result = mRootView.findViewById(viewId);
            mViews.put(viewId, result);
        }
        return result;
    }

    @Retention(RetentionPolicy.CLASS)
    @IntDef({SWATCH_VIBRANT, SWATCH_DARK_VIBRANT, SWATCH_LIGHT_VIBRANT,
            SWATCH_MUTED, SWATCH_DARK_MUTED, SWATCH_LIGHT_MUTED})
    public @interface Swatch {

    }

    public interface ViewPainter {

        void apply(@Nullable Palette palette, View rootView, PaintJob paintJob, boolean immediate,
                int duration);

        void setSwatch(@Swatch int swatch);

        boolean canAnimate();
    }

    public interface DerivedBuilder {

        Builder setFallBackColors(FallBackColors fallBackColors);

        Builder paintWithSwatch(@Swatch int swatch, ViewPainter... viewPainters);

        PaintJob build();

    }

    public interface Builder extends DerivedBuilder {

        Builder setBitmapCallback(BitmapCallback bitmapCallback);
    }

    public interface BitmapSource {

        Bitmap loadBitmapAsync();
    }

    public interface BitmapCallback {

        void onBitmapLoaded(Bitmap bitmap, boolean immediate);
    }

    static class RgbViewPainter extends BaseViewPainter {

        RgbViewPainter(int alpha, int[] viewIds) {
            super(alpha, viewIds);
        }

        @Override
        protected int getTargetColorFromSwatch(Palette.Swatch swatch) {
            return swatch.getRgb();
        }

        @Override
        protected int getCurrentColorFromView(View view) {
            Drawable drawable = view.getBackground();
            if (drawable == null) return Color.TRANSPARENT;
            if (drawable instanceof ColorDrawable) {
                return ((ColorDrawable) drawable).getColor();
            }
            return Color.TRANSPARENT;
        }



        @Override
        protected void applyColorToView(View view, int color) {
            view.setBackgroundColor(color);
        }

    }

    static class TitleViewPainter extends BaseViewPainter {

        TitleViewPainter(int alpha, int[] viewIds) {
            super(alpha, viewIds);
        }

        @Override
        protected int getCurrentColorFromView(View view) {
            return ((TextView) view).getCurrentTextColor();
        }

        @Override
        protected int getTargetColorFromSwatch(Palette.Swatch swatch) {
            return swatch.getTitleTextColor();
        }

        @Override
        protected void applyColorToView(View view, int color) {
            ((TextView) view).setTextColor(color);
        }

    }

    static class BodyTextViewPainter extends TitleViewPainter {

        BodyTextViewPainter(int alpha, int[] viewIds) {
            super(alpha, viewIds);
        }

        @Override
        protected int getTargetColorFromSwatch(Palette.Swatch swatch) {
            return swatch.getBodyTextColor();
        }
    }

    public static abstract class BaseViewPainter implements ViewPainter {

        final int[] mViewIds;

        final int mAlpha;

        @Swatch
        int mSwatch;

        protected BaseViewPainter(int alpha, int... viewIds) {
            mAlpha = alpha;
            mViewIds = viewIds;
        }

        @Override
        public void apply(@Nullable Palette palette, View rootView, PaintJob paintJob,
                boolean immediate,
                int duration) {
            if (mViewIds == null) return;
            if (mViewIds.length == 0) return;
            Palette.Swatch swatch = getSwatch(mSwatch, palette);

            if (swatch == null) {
                FallBackColors fallBackColors = paintJob.getFallbackColors();
                swatch = getFallbackSwatch(mSwatch, fallBackColors);
            }

            int N = mViewIds.length;
            for (int i = 0; i < N; i++) {
                int viewId = mViewIds[i];
                View view = paintJob.findViewById(viewId);
                animateViewToColor(view, swatch, immediate, duration);
            }
        }

        @Override
        public void setSwatch(int swatch) {
            mSwatch = swatch;
        }

        @Override
        public boolean canAnimate() {
            return true;
        }

        private static Palette.Swatch getSwatch(@Swatch int swatch, @Nullable Palette palette) {
            if (palette == null) return null;

            switch (swatch) {
                case SWATCH_MUTED:
                    return palette.getMutedSwatch();
                case SWATCH_DARK_MUTED:
                    return palette.getDarkMutedSwatch();
                case SWATCH_LIGHT_MUTED:
                    return palette.getLightMutedSwatch();
                default:
                case SWATCH_VIBRANT:
                    return palette.getVibrantSwatch();
                case SWATCH_DARK_VIBRANT:
                    return palette.getDarkVibrantSwatch();
                case SWATCH_LIGHT_VIBRANT:
                    return palette.getLightVibrantSwatch();
            }
        }

        private static Palette.Swatch getFallbackSwatch(@Swatch int swatch,
                FallBackColors fallbacks) {
            switch (swatch) {
                case SWATCH_MUTED:
                    return fallbacks.getMutedSwatch();
                case SWATCH_DARK_MUTED:
                    return fallbacks.getDarkMutedSwatch();
                case SWATCH_LIGHT_MUTED:
                    return fallbacks.getLightMutedSwatch();
                default:
                case SWATCH_VIBRANT:
                    return fallbacks.getVibrantSwatch();
                case SWATCH_DARK_VIBRANT:
                    return fallbacks.getDarkVibrantSwatch();
                case SWATCH_LIGHT_VIBRANT:
                    return fallbacks.getLightVibrantSwatch();
            }
        }

        private void animateViewToColor(final View view, Palette.Swatch swatch, boolean immediate,
                int duration) {

            int targetColor = getTargetColorFromSwatch(swatch);
            targetColor = ColorUtils.setAlphaComponent(targetColor, mAlpha);

            if (!canAnimate() || immediate || duration <= 0) {
                if (!canAnimate() && !applyColorsToView(view, swatch)) {
                    applyColorToView(view, targetColor);
                }
            } else {
                final int currentColor = getCurrentColorFromView(view);

                ValueAnimator colorAnimation = new ValueAnimator();
                if (sArgbEvaluator == null) {
                    sArgbEvaluator = new ArgbEvaluator();
                }
                colorAnimation.setIntValues(currentColor, targetColor);

                final int finalTargetColor = targetColor;
                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int color = evaluate(animation.getAnimatedFraction(), currentColor,
                                finalTargetColor);
                        applyColorToView(view, color);
                    }
                });
                colorAnimation.setDuration(duration);
                colorAnimation.start();
            }
        }

        protected abstract int getTargetColorFromSwatch(Palette.Swatch swatch);

        protected boolean applyColorsToView(View view, Palette.Swatch swatch) {
            return false;
        }

        protected abstract void applyColorToView(View view, int color);

        protected abstract int getCurrentColorFromView(View view);

        static int evaluate(float fraction, int startInt, int endInt) {
            int startA = (startInt >> 24) & 0xff;
            int startR = (startInt >> 16) & 0xff;
            int startG = (startInt >> 8) & 0xff;
            int startB = startInt & 0xff;

            int endA = (endInt >> 24) & 0xff;
            int endR = (endInt >> 16) & 0xff;
            int endG = (endInt >> 8) & 0xff;
            int endB = endInt & 0xff;

            return (int) ((startA + (int) (fraction * (endA - startA))) << 24) |
                    (int) ((startR + (int) (fraction * (endR - startR))) << 16) |
                    (int) ((startG + (int) (fraction * (endG - startG))) << 8) |
                    (int) ((startB + (int) (fraction * (endB - startB))));
        }

    }

    static class ViewBuilderImpl implements Builder {

        final View mView;

        final List<ViewPainter> mViewPainters = new ArrayList<>();

        boolean mBuilt;

        FallBackColors mFallBackColors;

        BitmapSource mBitmapSource;

        BitmapCallback mBitmapCallback;

        public ViewBuilderImpl(View view, BitmapSource bitmapSource) {
            mView = view;
            mBitmapSource = bitmapSource;
        }

        @Override
        public Builder setFallBackColors(FallBackColors fallBackColors) {
            mFallBackColors = fallBackColors;
            return this;
        }

        @Override
        public Builder paintWithSwatch(@Swatch int swatch, ViewPainter... viewPainters) {
            if (viewPainters != null) {
                int N = viewPainters.length;
                for (int i = 0; i < N; i++) {
                    ViewPainter viewPainter = viewPainters[i];
                    viewPainter.setSwatch(swatch);
                    mViewPainters.add(viewPainter);
                }
            }
            return this;
        }

        @Override
        public PaintJob build() {
            if (mBuilt) throw new IllegalStateException("Can only call build once");
            mBuilt = true;
            return new PaintJob(mView, mBitmapSource, mFallBackColors, mViewPainters,
                    mBitmapCallback);
        }

        @Override
        public Builder setBitmapCallback(BitmapCallback bitmapCallback) {
            mBitmapCallback = bitmapCallback;
            return this;
        }
    }

    static class PlainBitmapSource implements BitmapSource {

        final Bitmap mBitmap;

        PlainBitmapSource(Bitmap bitmap) {
            mBitmap = bitmap;
        }


        @Override
        public Bitmap loadBitmapAsync() {
            return mBitmap;
        }
    }

    public static class FallBackColors {

        final int mVibrantFallbackColor;

        final int mMutedFallbackColor;

        Palette.Swatch mVibrantSwatch;

        Palette.Swatch mDarkVibrantSwatch;

        Palette.Swatch mLightVibrantSwatch;

        Palette.Swatch mMutedSwatch;

        Palette.Swatch mDarkMutedSwatch;

        Palette.Swatch mLightMutedSwatch;

        public FallBackColors(int vibrantFallbackColor, int mutedFallbackColor) {
            mVibrantFallbackColor = vibrantFallbackColor;
            mMutedFallbackColor = mutedFallbackColor;
        }

        public static FallBackColors fromContext(Context context) {
            TypedArray a = context.obtainStyledAttributes(
                    new int[]{R.attr.colorPrimary, R.attr.colorAccent});

            int vibrantFallback = a.getColor(1, 0 /* TODO: define smart default */);
            int mutedFallback = a.getColor(0, 0 /* TODO: define smart default */);

            a.recycle();

            return new FallBackColors(vibrantFallback, mutedFallback);
        }

        public Palette.Swatch getVibrantSwatch() {
            if (mVibrantSwatch == null) {
                mVibrantSwatch = new Palette.Swatch(mVibrantFallbackColor, 0);
            }
            return mVibrantSwatch;
        }

        public Palette.Swatch getDarkVibrantSwatch() {
            if (mDarkVibrantSwatch == null) {
                int color = ColorUtils.compositeColors(0x20000000, mVibrantFallbackColor);
                mDarkVibrantSwatch = new Palette.Swatch(color, 0);
            }
            return mDarkVibrantSwatch;
        }

        public Palette.Swatch getLightVibrantSwatch() {
            if (mLightVibrantSwatch == null) {
                int color = ColorUtils.compositeColors(0x20FFFFFF, mVibrantFallbackColor);
                mLightVibrantSwatch = new Palette.Swatch(color, 0);
            }
            return mLightVibrantSwatch;
        }

        public Palette.Swatch getDarkMutedSwatch() {
            if (mDarkMutedSwatch == null) {
                int color = ColorUtils.compositeColors(0x20000000, mMutedFallbackColor);
                mDarkMutedSwatch = new Palette.Swatch(color, 0);
            }
            return mDarkMutedSwatch;
        }

        public Palette.Swatch getLightMutedSwatch() {
            if (mLightMutedSwatch == null) {
                int color = ColorUtils.compositeColors(0x20FFFFFF, mMutedFallbackColor);
                mLightMutedSwatch = new Palette.Swatch(color, 0);
            }
            return mLightMutedSwatch;
        }

        public Palette.Swatch getMutedSwatch() {
            if (mMutedSwatch == null) {
                mMutedSwatch = new Palette.Swatch(mMutedFallbackColor, 0);
            }
            return mMutedSwatch;
        }

    }

}
