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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.appsimobile.appsii.AccountHelper;
import com.appsimobile.appsii.BitmapUtils;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.module.weather.ImageDownloadHelper;
import com.appsimobile.appsii.module.weather.WeatherActivity;
import com.appsimobile.appsii.module.weather.WeatherLoadingService;
import com.appsimobile.appsii.module.weather.WeatherUtils;
import com.appsimobile.appsii.module.weather.loader.WeatherData;
import com.appsimobile.appsii.preference.PreferenceHelper;
import com.appsimobile.appsii.preference.PreferencesFactory;
import com.appsimobile.paintjob.PaintJob;

import java.io.File;
import java.util.TimeZone;

/**
 * Created by nick on 22/01/15.
 */
abstract class AbsWeatherViewHolder extends AbsHomeViewHolder implements
        HomeItemConfigurationHelper.ConfigurationListener, View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    static final Time sTime = new Time();

    final HomeItemConfiguration mConfigurationHelper;

    final SharedPreferences mSharedPreferences;

    final boolean mShowsWallpaper;

    HomeItem mHomeItem;

    AsyncTask<Void, Void, WeatherData> mLoaderTask;

    BroadcastReceiver mReceiver;

    final View mOverflow;

    OnWeatherCellClickListener mOnWeatherCellClickListener;

    final PreferenceHelper mPreferenceHelper;

    /**
     * This is null when {@link #mShowsWallpaper} is false
     */
    @Nullable
    final
    ImageView mCellBackground;

    String mBackgroundLoadedForWoeid;

    WeatherData mWeatherData;

    PaintJob mPaintJob;

    public AbsWeatherViewHolder(HomeViewWrapper view, boolean showsWallpaper) {
        super(view);
        mShowsWallpaper = showsWallpaper;
        Context context = view.getContext();
        mPreferenceHelper = PreferenceHelper.getInstance(context);

        mConfigurationHelper = HomeItemConfigurationHelper.getInstance(context);
        mSharedPreferences = PreferencesFactory.getPreferences(context);
        mOverflow = view.findViewById(R.id.overflow);
        mCellBackground = (ImageView) view.findViewById(R.id.weather_location_background);
        mOverflow.setOnClickListener(this);

        if (showsWallpaper && mCellBackground == null) {
            throw new IllegalStateException("Expected wallpaper view");
        }

        view.setOnClickListener(this);
    }

    public void setOnWeatherCellClickListener(
            OnWeatherCellClickListener onWeatherCellClickListener) {
        mOnWeatherCellClickListener = onWeatherCellClickListener;
    }

    @Override
    void bind(HomeItem item, int heightPx) {
        mHomeItem = item;
        applySuggestedHeight(heightPx);
    }

    @Override
    public void onAllowLoads() {
        mConfigurationHelper.addConfigurationListener(this);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateConfiguration();
            }
        };
        IntentFilter intentFilter =
                new IntentFilter(WeatherLoadingService.ACTION_WEATHER_UPDATED);
        itemView.getContext().registerReceiver(mReceiver, intentFilter);

        updateConfiguration();
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mWeatherData != null) {
            applyBackgroundImageIfNeeded(mWeatherData.woeid);
        }
    }

    @Override
    public void onDisallowLoads() {
        mConfigurationHelper.removeConfigurationListener(this);
        itemView.getContext().unregisterReceiver(mReceiver);
        if (mLoaderTask != null) {
            mLoaderTask.cancel(true);
        }
    }

    @Override
    public void onDetached(boolean allowLoads) {
        super.onDetached(allowLoads);
        if (mCellBackground != null) {
            mCellBackground.setImageDrawable(null);
        }
        if (mPaintJob != null) {
            mPaintJob.cancel();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return false;
    }

    void updateConfiguration() {
        long cellId = mHomeItem.mId;
        String woeid = mConfigurationHelper.getProperty(
                cellId, WeatherFragment.PREFERENCE_WEATHER_WOEID, null);

        if (woeid == null) {
            long lastUpdateMillis =
                    mSharedPreferences.getLong(WeatherLoadingService.PREFERENCE_LAST_UPDATED_MILLIS,
                            0);
            long elapsed = System.currentTimeMillis() - lastUpdateMillis;
            if (elapsed < DateUtils.DAY_IN_MILLIS) {
                woeid = mSharedPreferences.getString(
                        WeatherLoadingService.PREFERENCE_LAST_KNOWN_WOEID, null);
            }
        }
        if (woeid == null) {
            PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(itemView.getContext());
            woeid = preferenceHelper.getDefaultLocationWoeId();
        }

        if (woeid != null) {
            startLoadWeatherData(woeid);
        } else {
            onNoWeatherDataAvailable();
        }

        Context context = itemView.getContext();

        if (WeatherLoadingService.hasTimeoutExpired(context)) {
            AccountHelper.getInstance(context).requestSync();
        }
    }

    void startLoadWeatherData(final String woeid) {
        final Context context = itemView.getContext();
        if (mLoaderTask != null) {
            mLoaderTask.cancel(true);
        }

        mLoaderTask = new AsyncTask<Void, Void, WeatherData>() {
            @Override
            protected WeatherData doInBackground(Void... params) {
                return WeatherUtils.getWeatherData(context, woeid);
            }

            @Override
            protected void onPostExecute(WeatherData weatherData) {
                mWeatherData = weatherData;
                if (weatherData == null) {
                    onNoWeatherDataAvailable();
                } else {
                    onWeatherDataReady(weatherData);
                    applyBackgroundImageIfNeeded(weatherData.woeid);
                }
            }
        };
        mLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    abstract void onNoWeatherDataAvailable();

    abstract void onWeatherDataReady(WeatherData weatherData);

    public void applyBackgroundImageIfNeeded(String woeid) {
        if (!mShowsWallpaper) return;
        if (mCellBackground == null) return;

        if (TextUtils.equals(woeid, mBackgroundLoadedForWoeid)) {
            return;
        }

        if (mPaintJob != null) {
            mPaintJob.cancel();
        }

        mCellBackground.setImageDrawable(null);

        int w = (int) (itemView.getWidth() * 1.5f);
        int h = (int) (itemView.getHeight() * 1.5f);

        PaintJob.Builder builder = PaintJob.newBuilder(itemView, new BackgroundLoader(
                itemView.getContext(), woeid, w, h, 10, true));

        builder.setBitmapCallback(new PaintJob.BitmapCallback() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, boolean immediate) {
                if (bitmap == null) {
                    mCellBackground.setImageDrawable(null);
                    return;
                }

                Drawable tmp = new ColorDrawable(Color.TRANSPARENT);
                Drawable drawable = new BitmapDrawable(itemView.getResources(), bitmap);
                TransitionDrawable d = new TransitionDrawable(new Drawable[]{
                        tmp,
                        drawable
                });
                mCellBackground.setImageDrawable(d);
                d.startTransition(150);
                d.setCrossFadeEnabled(true);
            }
        });
        configurePaintJob(builder);
        PaintJob paintJob = builder.build();
        paintJob.execute(150);
        mPaintJob = paintJob;
    }

    protected void configurePaintJob(PaintJob.Builder paintJob) {

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_cell_weather_prefs) {

            Context context = itemView.getContext();
            Intent intent = new Intent(context, CellWeatherActivity.class);
            intent.putExtra(CellWeatherActivity.EXTRA_CELL_ID, mHomeItem.mId);
            intent.putExtra(CellWeatherActivity.EXTRA_CELL_TYPE, mHomeItem.mDisplayType);
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public void onConfigurationOptionUpdated(long cellId, String key, String value) {
        if (cellId == mHomeItem.mId) {
            updateConfiguration();
        }
    }

    @Override
    public void onConfigurationOptionDeleted(long cellId, String key) {
        if (cellId == mHomeItem.mId) {
            updateConfiguration();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mOverflow) {
            onOverflowClicked(v);
        } else {
            String woeid = mConfigurationHelper.getProperty(
                    mHomeItem.mId, WeatherFragment.PREFERENCE_WEATHER_WOEID, null);
            onBackgroundClicked(woeid);
        }
    }

    private void onOverflowClicked(View v) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.home_item_weather, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);

        popupMenu.show();
    }

    public void onBackgroundClicked(String woeid) {
        if (mOnWeatherCellClickListener != null) {
            mOnWeatherCellClickListener.onWeatherCellClicked(woeid);
        } else {
            Context context = itemView.getContext();
            Intent intent = new Intent(context, WeatherActivity.class);
            String timezone = mConfigurationHelper.getProperty(
                    mHomeItem.mId, WeatherFragment.PREFERENCE_WEATHER_TIMEZONE, null);
            String defaultUnit = mPreferenceHelper.getDefaultWeatherTemperatureUnit();
            String unit = mConfigurationHelper.getProperty(
                    mHomeItem.mId, WeatherFragment.PREFERENCE_WEATHER_UNIT, defaultUnit);
            if (timezone == null) timezone = TimeZone.getDefault().getID();


            intent.putExtra(WeatherActivity.EXTRA_WOEID, woeid);
            intent.putExtra(WeatherActivity.EXTRA_UNIT, unit);
            intent.putExtra(WeatherActivity.EXTRA_TIME_ZONE, timezone);
            context.startActivity(intent);
        }
    }

    void setupTitle(TextView textView, WeatherData weatherData, long cellId) {
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(
                itemView.getContext());
        String defaultTitleType = preferenceHelper.getDefaultWeatherTitleType();

        String titleType = mConfigurationHelper.getProperty(cellId,
                WeatherFragment.PREFERENCE_WEATHER_TITLE_TYPE, defaultTitleType);

        if ("condition".equals(titleType)) {
            String title = WeatherUtils.formatConditionCode(weatherData.nowConditionCode);
            textView.setText(title);
        } else {
            textView.setText(weatherData.location);
        }
    }


    public interface OnWeatherCellClickListener {

        void onWeatherCellClicked(String woeid);
    }

    static class BackgroundLoader implements PaintJob.BitmapSource {

        final Context mContext;

        final String mWoeid;

        final int mWidth;

        final int mHeight;

        final boolean mIsDay;

        final int mConditionCode;

        BackgroundLoader(Context context, String woeid, int w, int h, int conditionCode,
                boolean isDay) {
            mContext = context;
            mWoeid = woeid;
            mWidth = w;
            mHeight = h;
            mConditionCode = conditionCode;
            mIsDay = isDay;
        }


        @Override
        public Bitmap loadBitmapAsync() {
            File[] files = WeatherUtils.getCityPhotos(mContext, mWoeid);
            Bitmap bitmap;

            if (files != null) {
                int idx = 0;
                File file = files[idx];
                bitmap = BitmapUtils.decodeSampledBitmapFromFile(file, mWidth, mHeight);
                return bitmap;
            }


            int resId = ImageDownloadHelper.getFallbackDrawableForConditionCode(
                    mIsDay, mConditionCode);
            return BitmapFactory.decodeResource(mContext.getResources(), resId);
        }
    }
}
