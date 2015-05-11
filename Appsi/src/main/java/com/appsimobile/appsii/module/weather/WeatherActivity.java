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

package com.appsimobile.appsii.module.weather;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.appsimobile.appsii.BitmapUtils;
import com.appsimobile.appsii.BuildConfig;
import com.appsimobile.appsii.DrawableStartTintPainter;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.weather.loader.WeatherData;
import com.appsimobile.appsii.preference.PreferencesFactory;
import com.appsimobile.paintjob.PaintJob;
import com.appsimobile.paintjob.ViewPainters;
import com.appsimobile.util.TimeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Shows the detailed weather on a single location.
 * Created by nick on 12/04/15.
 */
public class WeatherActivity extends Activity {

    /**
     * Required extra for the activity. Specifies the woe-id to show the weather from
     */
    public static final String EXTRA_WOEID = BuildConfig.APPLICATION_ID + ".woe_id";

    /**
     * Required extra, the time-zone of the woe-id
     */
    public static final String EXTRA_TIME_ZONE = BuildConfig.APPLICATION_ID + ".timezone";

    /**
     * Required extra, the unit the user set to be used for this woe-id.
     */
    public static final String EXTRA_UNIT = BuildConfig.APPLICATION_ID + ".unit";

    String mDisplayUnit;

    String mTimezone;

    ImageView mBackgroundImage;

    ImageView mCurrentWeatherIcon;

    TextView mTemperatureView;

    TextView mLocationView;

    TextView mConditionView;

    TextView mMinTempView;

    TextView mMaxTempView;

    TextView mFeelsLikeView;

    TextView mWindView;

    TextView mForecastHeader;

    Drawable mMinTempDrawable;

    Drawable mMaxTempDrawable;

    SimpleRotateDrawable mWindDrawable;

    RecyclerView mRecyclerView;

    SharedPreferences mSharedPreferences;

    View mCurrentWeatherContainer;

    WeatherData mWeatherData;

    String mWoeid;

    ForecastAdapter mAdapter;

    boolean mIsDay;

    private Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mBitmap = savedInstanceState.getParcelable("selected_image");
        }

        mSharedPreferences = PreferencesFactory.getPreferences(this);

        String woeid = getIntent().getStringExtra(EXTRA_WOEID);
        if (woeid == null) {
            woeid = mSharedPreferences.getString(
                    WeatherLoadingService.PREFERENCE_LAST_KNOWN_WOEID, null);
        }

        mDisplayUnit = getIntent().getStringExtra(EXTRA_UNIT);
        mTimezone = getIntent().getStringExtra(EXTRA_TIME_ZONE);

        mWoeid = woeid;

        setContentView(R.layout.activity_weather_details);

        mBackgroundImage = (ImageView) findViewById(R.id.weather_background);
        mCurrentWeatherIcon = (ImageView) findViewById(R.id.current_weather);
        mTemperatureView = (TextView) findViewById(R.id.temperature);
        mLocationView = (TextView) findViewById(R.id.location);
        mConditionView = (TextView) findViewById(R.id.condition);
        mCurrentWeatherContainer = findViewById(R.id.current_weather_container);
        mMinTempView = (TextView) findViewById(R.id.temp_min);
        mMaxTempView = (TextView) findViewById(R.id.temp_max);
        mWindView = (TextView) findViewById(R.id.wind);
        mFeelsLikeView = (TextView) findViewById(R.id.feels_like);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        mForecastHeader = (TextView) findViewById(R.id.forecast_header);

        mMinTempDrawable = mMinTempView.getCompoundDrawablesRelative()[0];
        mMaxTempDrawable = mMaxTempView.getCompoundDrawablesRelative()[0];
        BitmapDrawable windDrawable = (BitmapDrawable) mWindView.getCompoundDrawablesRelative()[0];
        mWindDrawable = new SimpleRotateDrawable(getResources(), windDrawable.getBitmap());
        mWindView.setCompoundDrawablesRelativeWithIntrinsicBounds(mWindDrawable, null, null, null);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ForecastAdapter(mDisplayUnit);
        mRecyclerView.setAdapter(mAdapter);

        getLoaderManager().initLoader(1, null, new WeatherDataLoaderCallbacks());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("selected_image", mBitmap);
    }

    void onWeatherDataReady(WeatherData weatherData,
            SparseArray<WeatherUtils.ForecastInfo> forecastForDays) {

        if (weatherData == null) {
            Toast.makeText(this, "No weather info available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mWeatherData = weatherData;
        showDefaultImage(weatherData.nowConditionCode);

        int today = TimeUtils.getJulianDay();
        WeatherUtils.ForecastInfo todaysForecast =
                forecastForDays == null ? null : forecastForDays.get(today);

        mAdapter.setForecast(forecastForDays);

        mIsDay = WeatherUtils.isDay(mTimezone, weatherData);

        int icon = WeatherUtils.getConditionCodeIconResId(weatherData.nowConditionCode, mIsDay);
        mCurrentWeatherIcon.setImageResource(icon);

        mLocationView.setText(weatherData.location);
        String feelsLikeTemp = WeatherUtils.formatTemperature(this, weatherData.windChill,
                weatherData.unit, mDisplayUnit, WeatherUtils.FLAG_TEMPERATURE_NO_UNIT);


        if (todaysForecast != null) {
            setTemperatureText(mMinTempView, todaysForecast.tempLow, todaysForecast.unit,
                    mDisplayUnit);
            setTemperatureText(mMaxTempView, todaysForecast.tempHigh, todaysForecast.unit,
                    mDisplayUnit);
        } else {
            mMinTempView.setText("-");
            mMaxTempView.setText("-");
        }

        String wind = WeatherUtils.formatWindSpeed(this, weatherData.windSpeed, weatherData.unit,
                mDisplayUnit);

        mWindDrawable.mAngle = weatherData.windDirection;

        setTemperatureText(
                mTemperatureView, weatherData.nowTemperature, weatherData.unit, mDisplayUnit);
        setTemperatureText(
                mTemperatureView, weatherData.nowTemperature, weatherData.unit, mDisplayUnit);
        setTemperatureText(
                mTemperatureView, weatherData.nowTemperature, weatherData.unit, mDisplayUnit);


        String feelsLike = getString(R.string.feels_like, feelsLikeTemp);
        mFeelsLikeView.setText(feelsLike);

        mWindView.setText(wind);

        mConditionView.setText(WeatherUtils.formatConditionCode(weatherData.nowConditionCode));
    }

    private void showDefaultImage(final int conditionCode) {
        final String woeid = mWoeid;
        final boolean isDay = mIsDay;

        View root = findViewById(android.R.id.content);
        PaintJob.Builder builder;

        if (mBitmap != null) {
            // FIXME: check why PaintJob.newBuilder(root, mBitmap) is not woring
            // as expected.
            builder = PaintJob.newBuilder(root, new PaintJob.BitmapSource() {
                @Override
                public Bitmap loadBitmapAsync() {
                    return mBitmap;
                }
            });
        } else {
            builder = PaintJob.newBuilder(root, new PaintJob.BitmapSource() {
                @Override
                public Bitmap loadBitmapAsync() {
                    return loadBitmapBlocking(woeid, isDay, conditionCode);
                }
            });
        }

        builder.paintWithSwatch(PaintJob.SWATCH_DARK_VIBRANT,
                ViewPainters.text(R.id.location)).
                paintWithSwatch(PaintJob.SWATCH_LIGHT_MUTED,
                        ViewPainters.title(R.id.forecast_header)).
                paintWithSwatch(PaintJob.SWATCH_VIBRANT,
                        ViewPainters.rgb(R.id.current_weather_container),
                        ViewPainters.text(R.id.temperature, R.id.temp_min, R.id.temp_max,
                                R.id.feels_like, R.id.wind, R.id.condition),
                        DrawableStartTintPainter.forIds(R.id.temp_min, R.id.temp_max, R.id.wind)
                ).setBitmapCallback(new PaintJob.BitmapCallback() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, boolean immediate) {
                WeatherActivity.this.onBitmapLoaded(bitmap, immediate);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.paintWithSwatch(PaintJob.SWATCH_DARK_VIBRANT, new StatusBarTintPainter());
        }

        PaintJob paintJob = builder.build();
        paintJob.execute(500);

        mAdapter.setPaintJob(paintJob);
    }

    static void setTemperatureText(TextView textView, int temperature, String unit,
            String displayUnit) {
        Context context = textView.getContext();
        String text = WeatherUtils.formatTemperature(context, temperature, unit, displayUnit,
                WeatherUtils.FLAG_TEMPERATURE_NO_UNIT);

        textView.setText(text);
    }

    Bitmap loadBitmapBlocking(String woeid, boolean isDay, int conditionCode) {
        File[] files = WeatherUtils.getCityPhotos(WeatherActivity.this, woeid);
        Bitmap bitmap;

        if (files != null) {
            int N = files.length;
            Random random = new Random();
            int idx = random.nextInt(N);
            File file = files[idx];
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            Display defaultDisplay = windowManager.getDefaultDisplay();
            int dimen = Math.max(defaultDisplay.getWidth(), defaultDisplay.getHeight());
            bitmap = BitmapUtils.decodeSampledBitmapFromFile(file, dimen, dimen);
            return bitmap;
        }

        int resId = ImageDownloadHelper.getFallbackDrawableForConditionCode(
                isDay, conditionCode);
        return BitmapFactory.decodeResource(getResources(), resId);
    }

    void onBitmapLoaded(Bitmap bitmap, boolean immediate) {
        mBitmap = bitmap;
        showBitmap(bitmap, immediate);
    }

    void showBitmap(Bitmap bitmap, boolean isImmediate) {
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
        showDrawable(drawable, isImmediate);
    }

    void showDrawable(Drawable drawable, boolean isImmediate) {
        mBackgroundImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        if (isImmediate) {
            int w = drawable.getIntrinsicWidth();
            int viewWidth = mBackgroundImage.getWidth();
            float factor = viewWidth / (float) w;
            int h = (int) (drawable.getIntrinsicHeight() * factor);
            drawable.setBounds(0, 0, w, h);
            mBackgroundImage.setImageDrawable(drawable);
        } else {
            Drawable current = mBackgroundImage.getDrawable();
            if (current == null) current = new ColorDrawable(Color.TRANSPARENT);
            TransitionDrawable transitionDrawable =
                    new TransitionDrawable(new Drawable[]{current, drawable});
            transitionDrawable.setCrossFadeEnabled(true);
            mBackgroundImage.setImageDrawable(transitionDrawable);
            transitionDrawable.startTransition(500);
        }

    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {

        static final Time sTime;

        static {
            sTime = new Time(Time.TIMEZONE_UTC);
        }

        final String mWeatherDisplayUnit;

        ImageView mForecastImage;

        TextView mTemperatureHigh;

        TextView mTemperatureLow;

        TextView mForecastDate;

        TextView mForecastCondition;

        Drawable mLowDrawable;

        Drawable mHighDrawable;

        PaintJob mPaintJob;

        public ForecastViewHolder(View itemView, String weatherDisplayUnit) {
            super(itemView);
            mWeatherDisplayUnit = weatherDisplayUnit;
            mForecastImage = (ImageView) itemView.findViewById(R.id.forecast_image);
            mTemperatureHigh = (TextView) itemView.findViewById(R.id.forecast_temperature_high);
            mTemperatureLow = (TextView) itemView.findViewById(R.id.forecast_temperature_low);
            mLowDrawable = mTemperatureLow.getCompoundDrawablesRelative()[0];
            mHighDrawable = mTemperatureHigh.getCompoundDrawablesRelative()[0];
            mForecastDate = (TextView) itemView.findViewById(R.id.forecast_date);
            mForecastCondition = (TextView) itemView.findViewById(R.id.forecast_condition);
        }

        public void setPaintJob(PaintJob paintJob) {
            mPaintJob = paintJob;
            mPaintJob.derive(itemView).
                    paintWithSwatch(PaintJob.SWATCH_LIGHT_VIBRANT,
                            ViewPainters.title(R.id.forecast_date),
                            ViewPainters.text(R.id.forecast_temperature_high,
                                    R.id.forecast_temperature_low,
                                    R.id.forecast_condition),
                            DrawableStartTintPainter.forIds(R.id.forecast_temperature_high,
                                    R.id.forecast_temperature_low)
                    ).build().execute(500);
        }

        public void bind(WeatherUtils.ForecastInfo info) {
            Context context = itemView.getContext();

            // for forecast icons always use the day icon.
            int iconResId =
                    WeatherUtils.getConditionCodeIconResId(info.conditionCode, true /* day */);
            mForecastImage.setImageResource(iconResId);

            setTemperatureText(mTemperatureHigh, info.tempHigh, info.unit, mWeatherDisplayUnit);
            setTemperatureText(mTemperatureLow, info.tempLow, info.unit, mWeatherDisplayUnit);
            mForecastCondition.setText(WeatherUtils.formatConditionCode(info.conditionCode));

            sTime.setJulianDay(info.julianDay);
            String time = DateUtils.formatDateTime(
                    context, sTime.toMillis(false), DateUtils.FORMAT_SHOW_WEEKDAY);

            mForecastDate.setText(time);
        }
    }

    static class ForecastAdapter extends RecyclerView.Adapter<ForecastViewHolder> {

        final List<WeatherUtils.ForecastInfo> mForecasts = new ArrayList<>();

        final String mWeatherDisplayUnit;

        private PaintJob mPaintJob;

//        Palette.Swatch mColorSwatch;

        ForecastAdapter(String weatherDisplayUnit) {
            mWeatherDisplayUnit = weatherDisplayUnit;
        }

        @Override
        public ForecastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.list_item_forecast, parent, false);
            ForecastViewHolder result = new ForecastViewHolder(view, mWeatherDisplayUnit);
            result.setPaintJob(mPaintJob);
            return result;
        }

        @Override
        public void onBindViewHolder(ForecastViewHolder holder, int position) {
            WeatherUtils.ForecastInfo info = mForecasts.get(position);
            holder.bind(info);
        }

        @Override
        public int getItemCount() {
            return mForecasts.size();
        }

        public void setForecast(SparseArray<WeatherUtils.ForecastInfo> forecastForDays) {
            mForecasts.clear();
            int N = forecastForDays.size();
            for (int i = 0; i < N; i++) {
                WeatherUtils.ForecastInfo forecastInfo = forecastForDays.valueAt(i);
                if (forecastInfo.julianDay != TimeUtils.getJulianDay()) {
                    mForecasts.add(forecastInfo);
                }
            }
            Collections.sort(mForecasts);
            notifyDataSetChanged();
        }

        public void setPaintJob(PaintJob paintJob) {
            mPaintJob = paintJob;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    class StatusBarTintPainter extends PaintJob.BaseViewPainter {

        StatusBarTintPainter() {
            super(android.R.id.content);
        }

        @Override
        protected int getCurrentColorFromView(View view) {
            return getWindow().getStatusBarColor();
        }

        @Override
        protected int getTargetColorFromSwatch(Palette.Swatch swatch) {
            return swatch.getRgb();
        }

        @Override
        protected void applyColorToView(View view, int color) {
            getWindow().setStatusBarColor(color);
            getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(color, 64));
        }
    }

    class SimpleRotateDrawable extends BitmapDrawable {

        int mAngle;

        public SimpleRotateDrawable(Resources resources, Bitmap bitmap) {
            super(resources, bitmap);
        }

        @Override
        public void draw(final Canvas canvas) {
            Rect bounds = getBounds();
            canvas.rotate(mAngle, bounds.centerX(), bounds.centerY());
            super.draw(canvas);
            canvas.rotate(mAngle, -bounds.centerX(), -bounds.centerY());
        }
    }

    class WeatherDataLoaderCallbacks implements LoaderManager.LoaderCallbacks<
            Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>>> {

        @Override
        public Loader<Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>>> onCreateLoader(
                int id, Bundle args) {
            return new WeatherDataLoader(WeatherActivity.this, mWoeid);
        }

        @Override
        public void onLoadFinished(
                Loader<Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>>> loader,
                Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>> data) {
            onWeatherDataReady(data.first, data.second);
        }

        @Override
        public void onLoaderReset(
                Loader<Pair<WeatherData, SparseArray<WeatherUtils.ForecastInfo>>> loader) {

        }
    }
}

