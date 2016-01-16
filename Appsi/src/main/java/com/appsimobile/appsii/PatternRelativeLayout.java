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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.appsimobile.appsii.dagger.AppInjector;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

/**
 * Created by Nick Martens on 6/20/13.
 */
public class PatternRelativeLayout extends RelativeLayout
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    // watch the properties:
    // pref_sidebar_background_image
    // pref_sidebar_size
    // pref_sidebar_background_horizontal_repeat
    // pref_sidebar_background_vertical_repeat

    int mAlpha;

    float mXposition;

    float mYposition;

    AsyncTask<Void, Void, Bitmap> mBackgroundLoader;

    int mOldWidth;

    int mOldHeight;

    boolean mShouldMeasureChildren = true;

    //private ViewPager mViewPager;

    boolean mShouldLayoutChildren = true;

    boolean mBlockLayouts;

    Configuration mOldConfiguration;

    Bitmap mActivePattern;
    @Inject
    SharedPreferences mSharedPreferences;
    private BitmapDrawable mBitmapDrawable;
    private boolean mTileX;
    private boolean mTileY;

    {
        AppInjector.inject(this);
    }

    public PatternRelativeLayout(Context context) {
        super(context);
    }

    public PatternRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PatternRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    /*
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mBlockLayouts && !mShouldMeasureChildren) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(w, h);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            mShouldMeasureChildren = false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mBlockLayouts && !mShouldLayoutChildren) {
            mViewPager.layout(mViewPager.getLeft(), mViewPager.getTop(), mViewPager.getRight(),
            mViewPager.getBottom());
            return;
        }
        mShouldLayoutChildren = false;
        super.onLayout(changed, l, t, r, b);
    }
    */

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mBlockLayouts) {
            setBlockLayouts(false);
        }
        mShouldLayoutChildren = true;
        mShouldMeasureChildren = true;
        return super.onInterceptTouchEvent(ev);
    }

    public void setBlockLayouts(boolean blockLayouts) {
        if (blockLayouts != mBlockLayouts) {
            mBlockLayouts = blockLayouts;
            if (blockLayouts) {
                requestLayout();
                invalidate();
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        try {
            super.dispatchDraw(canvas);
        } catch (NullPointerException e) {
            Log.e("PatternRelativeLayout",
                    "prevented framework level crash, this may cause flickering", e);
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        try {
            return super.drawChild(canvas, child, drawingTime);
        } catch (Exception e) {
            Log.e("PatternRelativeLayout",
                    "prevented framework level crash, this may cause flickering", e);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        boolean needsUpdate = mOldConfiguration == null;
        if (!needsUpdate) {
            needsUpdate = newConfig.orientation != mOldConfiguration.orientation;
        }
        if (needsUpdate) {
            updateBitmap();
        }
        super.onConfigurationChanged(newConfig);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (oldw == 0) {
            oldw = mOldWidth;
        }
        if (oldh == 0) {
            oldh = mOldHeight;
        }
        mOldWidth = w;
        mOldHeight = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        onDrawImpl(canvas); // <-- NOTE THIS CALL, WHEN REMOVING TO THIS PROPERLY!!!
        super.onDraw(canvas);
    }

    //Paint mPaint;
    protected void onDrawImpl(Canvas canvas) {
        if (mBitmapDrawable != null) {
            //mActivePattern = BitmapFactory.decodeResource(getResources(),
            // R.drawable.colorful_bubbles);

            int xOffset = 0;
            int yOffset = 0;

            int totalWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            int totalHeight = getHeight() - getPaddingTop() - getPaddingBottom();

            int patternWidth = mActivePattern.getWidth();
            int patternHeight = mActivePattern.getHeight();

            int w = mTileX ? totalWidth : Math.min(totalWidth, patternWidth);
            int h = mTileY ? totalHeight : Math.min(totalHeight, patternHeight);

            if (!mTileX) {
                int spaceLeft = totalWidth;
                spaceLeft -= patternWidth;
                if (spaceLeft > 0) {
                    xOffset = (int) ((spaceLeft / 100f) * mXposition);
                }
            }
            if (!mTileY) {
                int spaceLeft = totalHeight;

                spaceLeft -= patternHeight;
                if (spaceLeft > 0) {
                    yOffset = (int) ((spaceLeft / 100f) * mYposition);
                }
            }

            canvas.translate(xOffset, yOffset);
            mBitmapDrawable.setBounds(getPaddingLeft(), getPaddingTop(), w + getPaddingLeft(), h);
            mBitmapDrawable.setAlpha(mAlpha);
            mBitmapDrawable.draw(canvas);
            canvas.translate(-xOffset, -yOffset);
            /*
            Paint p = new Paint();
            p.setColor(Color.WHITE);
            canvas.drawRect(0, 0, totalWidth - 1, totalHeight - 1, p);
            p.setColor(Color.GREEN);
            canvas.drawRect(0, 0, w - 1, h - 1, p);
            */
            //canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
            // getHeight() - getPaddingBottom(), mPaint);
        }
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setWillNotDraw(false);
        if (!isInEditMode()) {
            updateBitmap();
            SharedPreferences preferences = mSharedPreferences;
            preferences.registerOnSharedPreferenceChangeListener(this);
            mAlpha = getAlpha(preferences.getInt("pref_sidebar_background_transparency", 0));
            mXposition = preferences.getInt("pref_sidebar_background_x_pos", 0);
            mYposition = preferences.getInt("pref_sidebar_background_y_pos", 0);
        }
        //mViewPager = (ViewPager) findViewById(R.id.sidebar_pager);
    }

    private void updateBitmap() {
        SharedPreferences preferences = mSharedPreferences;
        int height = getContext().getResources().getDisplayMetrics().heightPixels;
        int widthTmp = getContext().getResources().getDisplayMetrics().widthPixels;

        int width = Math.min(height, widthTmp);
        height = Math.max(height, widthTmp);


        float sidebarPercentage = preferences.getInt("pref_sidebar_size", 80);
        width = (int) ((width * sidebarPercentage) / 100f);
        loadBackground(getContext(), width, height);
    }

    int getAlpha(int preferenceValue) {
        preferenceValue = 100 - Math.min(preferenceValue, 100);
        return (preferenceValue * 255) / 100;
    }

    // preventing the system for crashing on 4.0.3 / 4.0.4 devices with hw accelleration enabled

    void loadBackground(final Context context, final int width, final int height) {
        if (mBackgroundLoader != null) {
            mBackgroundLoader.cancel(true);
        }
        final WeakReference<Context> contextReference = new WeakReference<Context>(context);
        mBackgroundLoader = new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                Context context = contextReference.get();
                if (context == null) return null;
                SharedPreferences preferences = mSharedPreferences;
                return ThemingUtils.getSidebarWallpaper(context, preferences, width, height);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mActivePattern = bitmap;
                updatePaint();
            }
        };
        mBackgroundLoader.execute();
    }

    void updatePaint() {
        if (mActivePattern == null) {
            //mPaint = null;
            mBitmapDrawable = null;
            return;
        }
        mBitmapDrawable = new BitmapDrawable(getResources(), mActivePattern);
        //mPaint = new Paint();

        SharedPreferences preferences = mSharedPreferences;

        String hType = preferences.getString("pref_sidebar_background_horizontal_repeat", "none");
        String vType = preferences.getString("pref_sidebar_background_vertical_repeat", "none");


        Shader.TileMode vertical = getTileMode(vType);
        Shader.TileMode horizontal = getTileMode(hType);

        if (horizontal != null) {
            mBitmapDrawable.setTileModeX(horizontal);
            mTileX = true;
        } else {
            mTileX = false;
        }

        if (vertical != null) {
            mBitmapDrawable.setTileModeY(vertical);
            mTileY = true;
        } else {
            mTileY = false;
        }

    }

    Shader.TileMode getTileMode(String prefValue) {
        if (prefValue.equals("repeat")) {
            return Shader.TileMode.REPEAT;
        } else if (prefValue.equals("mirror")) {
            return Shader.TileMode.MIRROR;
        } else if (prefValue.equals("repeat_edges")) {
            return Shader.TileMode.CLAMP;
        }
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_sidebar_custom_background") ||
                key.equals("pref_sidebar_background_image") || key.equals("pref_sidebar_size")) {
            updateBitmap();

        } else if (key.equals("pref_sidebar_background_horizontal_repeat") ||
                key.equals("pref_sidebar_background_vertical_repeat")) {
            updatePaint();

        } else if (key.equals("pref_sidebar_background_transparency")) {
            mAlpha = getAlpha(sharedPreferences.getInt(key, 100));
            invalidate();

        } else if (key.equals("pref_sidebar_background_x_pos") ||
                key.equals("pref_sidebar_background_y_pos")) {
            mXposition = sharedPreferences.getInt("pref_sidebar_background_x_pos", 0);
            mYposition = sharedPreferences.getInt("pref_sidebar_background_y_pos", 0);
            invalidate();

        }
    }

}
