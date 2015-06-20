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

import android.Manifest;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.ArrayMap;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Advanceable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.AppsiApplication;
import com.appsimobile.appsii.BitmapUtils;
import com.appsimobile.appsii.DrawableCompat;
import com.appsimobile.appsii.DrawableStartTintPainter;
import com.appsimobile.appsii.PermissionDeniedException;
import com.appsimobile.appsii.PopupMenuHelper;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.annotation.VisibleForTesting;
import com.appsimobile.appsii.appwidget.AppWidgetUtils;
import com.appsimobile.appsii.appwidget.AppsiiAppWidgetHost;
import com.appsimobile.appsii.module.home.appwidget.WidgetChooserActivity;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.module.weather.WeatherUtils;
import com.appsimobile.appsii.module.weather.loader.WeatherData;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.paintjob.PaintJob;
import com.appsimobile.paintjob.ViewPainters;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

/**
 * The adapter used on the Home-Page and in the HomeEditorActivity.
 * Created by nick on 20/01/15.
 */
public class HomeAdapter extends RecyclerView.Adapter<AbsHomeViewHolder> {

    static final String PERMISSION_ERROR_LOCATION_WEATHER = "_location_weather_";

    static final String PERMISSION_ERROR_CONTACTS_PROFILE = "_contact_profile_image_";

    /**
     * A place to support very basic caching of the host-views. The cache is automatically
     * cleared when the host stops listening.
     */
    static final SparseArray<AppWidgetHostView> sAppWidgetViewCache = new SparseArray<>();

    /**
     * The list of home items in the adapter.
     */
    final List<HomeItem> mHomeItems = new ArrayList<>(24);

    /**
     * A cached layout-inflater to prevent having to get it all the time
     */
    final LayoutInflater mLayoutInflater;

    /**
     * The context to which we are bound
     */
    final Context mContext;

    /**
     * The factory to create the view-wrappers. Can be overridden to create
     * a custom view-wrapper.
     */
    final ViewWrapperFactory mViewWrapperFactory;

    /**
     * The internal lookup for the home-spans
     */
    final HomeSpanSizeLookup mHomeSpanSizeLookup;

    /**
     * The id of the home-page we are bound to (or editing for)
     */
    final long mHomeId;

    /**
     * The app-widget-host. Can be null in case mAppWidgetsEnabled is false.
     */
    @Nullable
    final AppsiiAppWidgetHost mAppWidgetHost;

    /**
     * The app-widget manager. Used by widget view-holders to load the
     * AppWidgetInfo for the bound app-widget. In the disable variant it
     * is used to load the image.
     */
    final AppWidgetManager mAppWidgetManager;

    /**
     * A helper that auto-advances the views that need to be auto-advanced
     */
    final AutoAdvanceHelper mAutoAdvanceHelper = new AutoAdvanceHelper();

    /**
     * True in case app widgets must be enabled. false otherwise. Setting this
     * to false, will use a different view-holder that only loads the preview
     * image instead of the normal widget view.
     */
    final boolean mAppWidgetsEnabled;

    /**
     * The base height of the widgets. This is set to 72dp in the constructor
     */
    final int mBaseHeight;

    /**
     * The height to increase the widget height with, for each step.
     */
    final int mHeightPerStep;

    long mUnsetId = -1L;

    /**
     * True if the home-adapter is started. This specifically impacts the widgets
     * which are started and stopped when this state changes.
     */
    boolean mStarted;

    PermissionErrorListener mPermissionErrorListener;

    public HomeAdapter(Context context, long homeId) {
        this(context, new DefaultViewWrapperFactory(), homeId, true);
    }

    public HomeAdapter(Context context, ViewWrapperFactory viewWrapperFactory, long homeId,
            boolean appWidgetsEnabled) {
        mContext = context;
        mHomeId = homeId;
        mAppWidgetsEnabled = appWidgetsEnabled;

        mViewWrapperFactory = viewWrapperFactory;
        mBaseHeight = (int) (context.getResources().getDisplayMetrics().density * 72);
        mHeightPerStep = (int) (context.getResources().getDisplayMetrics().density * 24);
        mLayoutInflater = LayoutInflater.from(context);
        mHomeSpanSizeLookup = new HomeSpanSizeLookup(context);

        setHasStableIds(true);

        if (mAppWidgetsEnabled) {
            mAppWidgetHost = new AppsiiAppWidgetHost(context, AppsiApplication.APPWIDGET_HOST_ID);
        } else {
            mAppWidgetHost = null;
        }
        mAppWidgetManager = AppWidgetManager.getInstance(context);
    }

    long nextUnsetId() {
        return mUnsetId--;
    }

    public HomeItem getItemAt(int position) {
        return mHomeItems.get(position);
    }

    public void getItemsInRow(long rowId, List<HomeItem> homeItems) {
        for (HomeItem homeItem : mHomeItems) {
            if (homeItem.mRowId == rowId) {
                homeItems.add(homeItem);
            }
        }
    }

    @Override
    public AbsHomeViewHolder onCreateViewHolder(ViewGroup parent, int encodedViewType) {
        int viewType = encodedViewType & 0x0000FFFF;
        int rowHeight = (encodedViewType & 0xFFFF0000) >> 16;

        int heightPx = heightForRowHeight(rowHeight);

        switch (viewType) {
            case HomeContract.Cells.DISPLAY_TYPE_UNSET:
                return createEmptyViewHolder(heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP:
                return createPlainTemperatureViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP_WALLPAPER:
                return createWallpaperTemperatureViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND_WALLPAPER:
//                return createWallpaperWindViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND:
                return createPlainWindViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE_WALLPAPER:
//                return createWallpaperSunViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE:
                return createPlainSunViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_CLOCK:
                return createClockViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_BLUETOOTH_TOGGLE:
                return createBluetoothViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_INTENT:
                return createIntentTypeViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_PROFILE_IMAGE:
                return createProfileImageViewHolder(parent, heightPx);
            case HomeContract.Cells.DISPLAY_TYPE_APP_WIDGET:
                // for widgets, when they are not enabled, use a disabled
                // version of the viewholder.
                if (mAppWidgetsEnabled) {
                    return createAppWidgetViewHolder(parent, heightPx);
                } else {
                    return createDisabledAppWidgetViewHolder(parent, heightPx);

                }
        }
        return createDummyViewHolder(heightPx);
    }

    int heightForRowHeight(int rowHeight) {
        return mBaseHeight + (mHeightPerStep * rowHeight);
    }

    private AbsHomeViewHolder createEmptyViewHolder(int height) {
        View result = new View(mContext);
        HomeViewWrapper wrapped = wrapView(mContext, result, height);
        return new EmptyViewViewHolder(wrapped);
    }

    private AbsHomeViewHolder createPlainTemperatureViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_weather, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new WeatherTemperaturePlainViewHolder(wrapped);
    }

    private AbsHomeViewHolder createWallpaperTemperatureViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_weather_wp, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new WeatherTemperatureWallpaperViewHolder(wrapped);
    }

    private AbsHomeViewHolder createPlainWindViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_weather_wind, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new PlainWeatherWindViewHolder(wrapped);
    }

    private AbsHomeViewHolder createWallpaperWindViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_weather_wind, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new WallpaperWeatherWindViewHolder(wrapped);
    }

    private AbsHomeViewHolder createPlainSunViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_weather_sun, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new PlainWeatherSunViewHolder(wrapped);
    }

    private AbsHomeViewHolder createWallpaperSunViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_weather_sun_wp, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new WallpaperWeatherSunViewHolder(wrapped);
    }

    private AbsHomeViewHolder createClockViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_analog_clock, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new ClockViewHolder(wrapped);
    }

    private AbsHomeViewHolder createBluetoothViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_bluetooth_toggle, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new BluetoothViewHolder(wrapped);
    }

    private AbsHomeViewHolder createIntentTypeViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_app, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new IntentViewHolder(wrapped);
    }

    private AbsHomeViewHolder createProfileImageViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_image, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new ProfileImageViewHolder(wrapped);
    }

    private AbsHomeViewHolder createAppWidgetViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_appwidget, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        AppWidgetViewHolder holder =
                new AppWidgetViewHolder(wrapped, mAppWidgetHost, mAppWidgetManager,
                        mAutoAdvanceHelper);
        holder.postCreate();
        return holder;
    }

    private AbsHomeViewHolder createDisabledAppWidgetViewHolder(ViewGroup parent, int height) {
        View view = mLayoutInflater.inflate(R.layout.home_item_appwidget_disabled, parent, false);
        HomeViewWrapper wrapped = wrapView(mContext, view, height);
        return new DisabledAppWidgetViewHolder(wrapped, mAppWidgetManager);
    }

    private AbsHomeViewHolder createDummyViewHolder(int height) {
        TextView result = new TextView(mContext);
        HomeViewWrapper wrapped = wrapView(mContext, result, height);
        return new SimpleTextViewViewHolder(wrapped);
    }

    HomeViewWrapper wrapView(Context context, View child, int heightPx) {
        return mViewWrapperFactory.wrapView(context, child, heightPx);
    }

    @Override
    public void onBindViewHolder(AbsHomeViewHolder absHomeViewHolder, int i) {
        HomeItem item = mHomeItems.get(i);
        int heightPx = heightForRowHeight(item.mRowHeight);

        absHomeViewHolder.bind(item, heightPx);
    }

    @Override
    public int getItemViewType(int position) {
        HomeItem homeItem = mHomeItems.get(position);
        return (homeItem.mDisplayType & 0x0000FFFF) | (homeItem.mRowHeight << 16);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0) return RecyclerView.NO_ID;
        if (position >= mHomeItems.size()) return RecyclerView.NO_ID;

        return mHomeItems.get(position).mId;
    }

    @Override
    public int getItemCount() {
        return mHomeItems.size();
    }

    @Override
    public void onViewRecycled(AbsHomeViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof AbsWeatherWindViewHolder) {
            ((AbsWeatherWindViewHolder) holder).onRecycled();
        }
    }

    public void setHomeItems(List<HomeItem> data) {
        mHomeItems.clear();
        for (HomeItem item : data) {
            if (item.mPageId == mHomeId) {
                mHomeItems.add(item);
            }
        }
        try {
            mHomeSpanSizeLookup.setup(mHomeItems);
        } catch (InconsistentRowsException e) {
            fixHomeItemRows(mHomeItems);
        }
        notifyDataSetChanged();
    }

    private void fixHomeItemRows(List<HomeItem> homeItems) {
        int lastRowPosition = -1;
        int newRowIdx = 0;
        int N = homeItems.size();

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        boolean done;

        for (int i = 0; i < N; i++) {
            HomeItem item = homeItems.get(i);
            done = false;
            if (lastRowPosition == -1 || lastRowPosition != item.mRowPosition) {
                lastRowPosition = item.mRowPosition;

                Uri uri = ContentUris.withAppendedId(
                        HomeContract.Rows.CONTENT_URI, item.mRowId);

                ops.add(ContentProviderOperation.newUpdate(uri).
                        withValue(HomeContract.Rows.POSITION, newRowIdx).
                        build());
                newRowIdx++;
                done = true;
            }
            // handle the last row
            if (i == mHomeItems.size() - 1 && !done) {
                Uri uri = ContentUris.withAppendedId(
                        HomeContract.Rows.CONTENT_URI, item.mRowId);

                ops.add(ContentProviderOperation.newUpdate(uri).
                        withValue(HomeContract.Rows.POSITION, newRowIdx).
                        build());

            }
        }


        try {
            mContext.getContentResolver().applyBatch(HomeContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            Log.w("HomeAdapter", "error fixing bad row numbers", e);
        }

    }

    public HomeSpanSizeLookup getHomeSpanSizeLookup() {
        return mHomeSpanSizeLookup;
    }

    HomeAdapterEditor getEditor() {
        return new HomeAdapterEditor();
    }

    public int getPositionOfId(long lastId) {
        int position = 0;
        for (HomeItem item : mHomeItems) {
            if (item.mId == lastId) {
                return position;
            }
            position++;
        }
        return -1;
    }

    public void setStarted(boolean started) {
        if (mAppWidgetHost != null) {
            if (mStarted != started) {
                mStarted = started;

                if (started) {
                    mAppWidgetHost.startListening();
                    mAutoAdvanceHelper.start();
                } else {
                    mAppWidgetHost.stopListening();
                    mAutoAdvanceHelper.stop();
                }
            }
        }
        if (!started) {
            sAppWidgetViewCache.clear();
        }
    }

    public HomeItem getHomeItemForAppWidgetId(int appWidgetId) {
        HomeItemConfiguration helper = HomeItemConfigurationHelper.getInstance(mContext);
        long cellId = helper.
                findCellWithPropertyValue("app_widget_id", String.valueOf(appWidgetId));

        if (cellId != -1) {
            for (HomeItem homeItem : mHomeItems) {
                if (homeItem.mId == cellId) {
                    return homeItem;
                }
            }
        }
        return null;
    }

    public void onTrimMemory(int level) {
    }


    interface ViewWrapperFactory {

        /**
         * Wraps an home item view in a view that is used by the ViewHolder to manage the
         * height of the view in it's row.
         */
        HomeViewWrapper wrapView(Context context, View child, int heightPx);
    }

    interface OpsBuilder {

        Operation newInsert(Uri uri);

        Operation newDelete(Uri uri);

        Operation newUpdate(Uri uri);
    }

    interface Operation {

        Operation withValue(String position, int value);

        Operation withValue(String position, long value);

        Operation withValueBackReference(String rowId, int resultIdx);

        Operation withSelection(String selection, String[] selectionArgs);

        ContentProviderOperation build();

    }

    static class DefaultViewWrapperFactory implements ViewWrapperFactory {

        @Override
        public HomeViewWrapper wrapView(Context context, View child, int heightPx) {
            HomeViewWrapper result = new HomeViewWrapper(context);
            RecyclerView.LayoutParams params =
                    new RecyclerView.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, heightPx);

            result.addView(child, params);
            return result;
        }

    }

    static class ClockViewHolder extends BaseViewHolder implements View.OnClickListener,
            HomeItemConfigurationHelper.ConfigurationListener, PopupMenu.OnMenuItemClickListener {

        final AnalogClock mAnalogClock;

        final HomeItemConfiguration mConfigurationHelper;

        final TextView mTextView;

        public ClockViewHolder(HomeViewWrapper view) {
            super(view);
            mConfigurationHelper = HomeItemConfigurationHelper.getInstance(view.getContext());
            mAnalogClock = (AnalogClock) view.findViewById(R.id.analog_clock);
            mTextView = (TextView) view.findViewById(R.id.primary_text);
            mOverflow.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.overflow) {
                showOverflowMenu(v);
            }

        }

        private void showOverflowMenu(View v) {
            PopupMenuHelper.showPopupMenu(v, R.menu.home_item_clock, this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_cell_weather_prefs) {

                Context context = itemView.getContext();
                Intent intent = new Intent(context, CellClockActivity.class);
                intent.putExtra(CellClockActivity.EXTRA_CELL_ID, mHomeItem.mId);
                context.startActivity(intent);
                return true;
            }
            return false;
        }

        @Override
        void updateConfiguration() {
            long cellId = mHomeItem.mId;
            String timezone = mConfigurationHelper.getProperty(cellId, "timezone_id", null);
            String title = mConfigurationHelper.getProperty(cellId, "title", null);

            if (timezone == null) {
                mAnalogClock.clearTimezone();
            } else {
                mAnalogClock.setTimezone(timezone);
            }
            if (TextUtils.isEmpty(title)) {
                mTextView.setText(R.string.no_title);
            } else {
                mTextView.setText(title);
            }
        }


        @Override
        void bind(HomeItem item, int heightPx) {
            super.bind(item, heightPx);
            mChildView.setOnClickListener(this);
            updateConfiguration();
        }


    }

    static class BluetoothViewHolder extends BaseViewHolder implements View.OnClickListener,
            HomeItemConfigurationHelper.ConfigurationListener, PopupMenu.OnMenuItemClickListener {

        private final int mPrimaryColor;

        private final int mWidgetColor;

        final HomeItemConfiguration mConfigurationHelper;

        final TextView mTextView;

        final Drawable mBluetoothDrawable;

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            showOffStatus();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            mTextView.setText(R.string.turning_off);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            showOnStatus();
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            mTextView.setText(R.string.turning_on);
                            break;
                    }
                }
            }
        };

        final ImageView mImageView;

        private final BluetoothAdapter mBluetoothAdapter;

        public BluetoothViewHolder(HomeViewWrapper view) {
            super(view);
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mConfigurationHelper = HomeItemConfigurationHelper.getInstance(view.getContext());
            mImageView = (ImageView) view.findViewById(R.id.bluetooth_image);
            mBluetoothDrawable = mImageView.getDrawable();
            mTextView = (TextView) view.findViewById(R.id.primary_text);

            Context context = view.getContext();
            final TypedArray a = context.obtainStyledAttributes(
                    new int[]{R.attr.colorPrimary, R.attr.colorAccent,
                            R.attr.colorPrimaryDark,
                            R.attr.appsiHomeWidgetPrimaryColor,
                    });

            mPrimaryColor = a.getColor(0, Color.BLACK);
            mWidgetColor = a.getColor(3, Color.BLACK);
            a.recycle();

            mOverflow.setOnClickListener(this);
        }

        @Override
        public void onAllowLoads() {
            super.onAllowLoads();
            IntentFilter f = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            itemView.getContext().registerReceiver(mReceiver, f);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_bluetooth_settings) {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                try {
                    itemView.getContext().startActivity(intent);
                } catch (Exception e) {
                    // TODO: show toast
                }
                return true;
            }
            return false;
        }

        @Override
        public void onDisallowLoads() {
            super.onDisallowLoads();
            itemView.getContext().unregisterReceiver(mReceiver);
        }


        void showOnStatus() {
            DrawableCompat.setTintColorCompat(mBluetoothDrawable, mPrimaryColor);
            mTextView.setText(R.string.enabled);
        }

        void showOffStatus() {
            DrawableCompat.setTintColorCompat(mBluetoothDrawable, mWidgetColor);
            mTextView.setText(R.string.disabled);
        }


        @Override
        void updateConfiguration() {
            if (mBluetoothAdapter == null) {
                mBluetoothDrawable.setAlpha(128);
                mTextView.setText(R.string.no_bluetooth);
            } else {
                mBluetoothDrawable.setAlpha(255);
                if (mBluetoothAdapter.isEnabled()) {
                    showOnStatus();
                } else {
                    showOffStatus();
                }
            }

        }

        @Override
        void bind(HomeItem item, int heightPx) {
            super.bind(item, heightPx);
            mChildView.setOnClickListener(this);
            updateConfiguration();
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.overflow) {
                showOverflowMenu(v);
            } else {
                if (mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                    } else {
                        mBluetoothAdapter.enable();
                    }
                }
            }
        }

        private void showOverflowMenu(View v) {
            PopupMenuHelper.showPopupMenu(v, R.menu.home_item_bluetooth, this);
        }


    }

    static abstract class AbsWeatherTemperatureViewHolder extends AbsWeatherViewHolder
            implements View.OnClickListener,
            HomeItemConfigurationHelper.ConfigurationListener {

        @Nullable
        final
        ImageView mWeatherConditionView;

        final TextView mTextView;

        final TextView mTemperatureView;

        public AbsWeatherTemperatureViewHolder(HomeViewWrapper view, boolean showsWallpaper) {
            super(view, showsWallpaper);
            mWeatherConditionView = (ImageView) view.findViewById(R.id.weather_condition_image);
            mTextView = (TextView) view.findViewById(R.id.primary_text);
            mTemperatureView = (TextView) view.findViewById(R.id.temperature);
        }

        @Override
        void bind(HomeItem item, int heightPx) {
            mHomeItem = item;
            applySuggestedHeight(heightPx);
            mChildView.setOnClickListener(this);
            updateConfiguration();
        }

        abstract void bindWeatherData(long cellId, WeatherData weatherData, boolean isDay,
                String temperature);

        @Override
        void onWeatherDataReady(WeatherData weatherData) {
            long cellId = mHomeItem.mId;

            String timezone = mConfigurationHelper.getProperty(
                    cellId, WeatherFragment.PREFERENCE_WEATHER_TIMEZONE, null);

            if (timezone == null) {
                timezone = TimeZone.getDefault().getID();
            }


            boolean isDay = WeatherUtils.isDay(timezone, weatherData);

            String unit = weatherData.unit;
            String defaultUnit = mPreferenceHelper.getDefaultWeatherTemperatureUnit();
            String displayUnit = mConfigurationHelper.getProperty(cellId,
                    WeatherFragment.PREFERENCE_WEATHER_UNIT, defaultUnit);

            String temperature = WeatherUtils.formatTemperature(itemView.getContext(),
                    weatherData.nowTemperature, unit, displayUnit,
                    WeatherUtils.FLAG_TEMPERATURE_NO_UNIT);

            bindWeatherData(cellId, weatherData, isDay, temperature);

        }

        @Override
        void onNoWeatherDataAvailable() {
            if (mWeatherConditionView != null) {
                mWeatherConditionView.setImageResource(R.drawable.ic_weather_unknown);
            }
            mTextView.setText(R.string.unknown);
            mTemperatureView.setText("");
        }

        @Override
        public void onClick(View v) {

            super.onClick(v);
        }

    }

    static class WeatherTemperaturePlainViewHolder extends AbsWeatherTemperatureViewHolder
            implements View.OnClickListener,
            HomeItemConfigurationHelper.ConfigurationListener {

        public WeatherTemperaturePlainViewHolder(HomeViewWrapper view) {
            super(view, false /* showsWallpaper */);
        }

        @Override
        void bindWeatherData(long cellId, WeatherData weatherData, boolean isDay,
                String temperature) {

            int iconResId =
                    WeatherUtils.getConditionCodeIconResId(weatherData.nowConditionCode, isDay);

            mWeatherConditionView.setImageResource(iconResId);
            setupTitle(mTextView, weatherData, cellId);
            mTemperatureView.setText(temperature);
        }
    }

    static class WeatherTemperatureWallpaperViewHolder extends AbsWeatherTemperatureViewHolder
            implements View.OnClickListener,
            HomeItemConfigurationHelper.ConfigurationListener {

        public WeatherTemperatureWallpaperViewHolder(HomeViewWrapper view) {
            super(view, true /* showsWallpaper */);
        }

        @Override
        protected void configurePaintJob(PaintJob.Builder paintJob) {
            paintJob.paintWithSwatch(PaintJob.SWATCH_DARK_MUTED,
                    ViewPainters.text(R.id.primary_text, R.id.temperature),
                    ViewPainters.argb(128, R.id.primary_text, R.id.temperature),
                    DrawableStartTintPainter.forIds(R.id.primary_text));
        }

        @Override
        void bindWeatherData(long cellId, WeatherData weatherData, boolean isDay,
                String temperature) {
            // TODO: implement properly

            int iconResId =
                    WeatherUtils.getConditionCodeTinyIconResId(weatherData.nowConditionCode, isDay);

            mTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(iconResId, 0, 0, 0);
            setupTitle(mTextView, weatherData, cellId);
            mTemperatureView.setText(temperature);
        }
    }

    static class AutoAdvanceHelper implements Handler.Callback {

        private final int ADVANCE_MSG = 1;

        private final int mAdvanceInterval = 20000;

        private final int mAdvanceStagger = 250;

        private final Handler mHandler;

        boolean mVisible;

        boolean mAutoAdvanceRunning;

        final ArrayMap<View, AppWidgetProviderInfo> mWidgetsToAdvance = new ArrayMap<>();

        private long mAutoAdvanceSentTime;

        private long mAutoAdvanceTimeLeft = -1;

        AutoAdvanceHelper() {
            mHandler = new Handler(this);
        }

        void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
            if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1) return;
            View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
            if (v instanceof Advanceable) {
                mWidgetsToAdvance.put(hostView, appWidgetInfo);
                ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
                updateRunning();
            }
        }

        private void updateRunning() {
            boolean autoAdvanceRunning = mVisible && !mWidgetsToAdvance.isEmpty();
            if (autoAdvanceRunning != mAutoAdvanceRunning) {
                mAutoAdvanceRunning = autoAdvanceRunning;
                if (autoAdvanceRunning) {
                    long delay =
                            mAutoAdvanceTimeLeft == -1 ? mAdvanceInterval : mAutoAdvanceTimeLeft;
                    sendAdvanceMessage(delay);
                } else {
                    if (!mWidgetsToAdvance.isEmpty()) {
                        mAutoAdvanceTimeLeft = Math.max(0, mAdvanceInterval -
                                (System.currentTimeMillis() - mAutoAdvanceSentTime));
                    }
                    mHandler.removeMessages(ADVANCE_MSG);
                    mHandler.removeMessages(0); // Remove messages sent using postDelayed()
                }
            }
        }

        private void sendAdvanceMessage(long delay) {
            mHandler.removeMessages(ADVANCE_MSG);
            Message msg = mHandler.obtainMessage(ADVANCE_MSG);
            mHandler.sendMessageDelayed(msg, delay);
            mAutoAdvanceSentTime = System.currentTimeMillis();
        }

        void removeWidgetToAutoAdvance(View hostView) {
            if (mWidgetsToAdvance.containsKey(hostView)) {
                mWidgetsToAdvance.remove(hostView);
                updateRunning();
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == ADVANCE_MSG) {
                int i = 0;
                for (View key : mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(mWidgetsToAdvance.get(key).autoAdvanceViewId);
                    final int delay = mAdvanceStagger * i;
                    if (v instanceof Advanceable) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((Advanceable) v).advance();
                            }
                        }, delay);
                    }
                    i++;
                }
                sendAdvanceMessage(mAdvanceInterval);
                return true;
            }
            return false;
        }

        public void stop() {
            mVisible = false;
            updateRunning();
        }

        public void start() {
            mVisible = true;
            updateRunning();
        }
    }

    static class DisabledAppWidgetViewHolder extends BaseViewHolder {

        final AppWidgetManager mAppWidgetManager;

        int mAppWidgetId;

        final ImageView mWidgetPreview;

        final TextView mNoWidgetTextView;

        public DisabledAppWidgetViewHolder(HomeViewWrapper view,
                AppWidgetManager appWidgetManager) {
            super(view);
            mWidgetPreview = (ImageView) view.findViewById(R.id.widget_preview);
            mNoWidgetTextView = (TextView) view.findViewById(R.id.no_widget_selected_text);
            mAppWidgetManager = appWidgetManager;
        }

        @Override
        void updateConfiguration() {
            long cellId = mHomeItem.mId;
            String appWidgetId = mConfigurationHelper.getProperty(cellId, "app_widget_id", null);
            mAppWidgetId = appWidgetId == null ? -1 : Integer.parseInt(appWidgetId);

            if (mAppWidgetId == -1) {
                mWidgetPreview.setImageResource(0);
                mNoWidgetTextView.setVisibility(View.VISIBLE);
            } else {
                mNoWidgetTextView.setVisibility(View.GONE);
                AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(
                        mAppWidgetId);
                Bitmap image = AppWidgetUtils.
                        getWidgetPreviewBitmap(itemView.getContext(), appWidgetInfo, null);
                mWidgetPreview.setImageBitmap(image);
            }
        }
    }

    static class AppWidgetViewHolder extends BaseViewHolder
            implements View.OnClickListener, AppsiiAppWidgetHost.HostStatusListener {

        final AppsiiAppWidgetHost mAppWidgetHost;

        final AppWidgetManager mAppWidgetManager;

        final ViewGroup mContainer;

        final AutoAdvanceHelper mAutoAdvanceHelper;

        final int mTouchSlop;

        final View mChooseWidgetButton;

        final View mErrorLoadingWidgetButton;

        int mAppWidgetId;

        @Nullable
        AppWidgetHostView mAppWidgetHostView;

        AppWidgetProviderInfo mAppWidgetInfo;

        int mWidth;

        int mHeight;

        final CardView mCardView;

        final int mDefaultColor;

        boolean mInitializedSinceLastStop;

        public AppWidgetViewHolder(HomeViewWrapper view, AppsiiAppWidgetHost appWidgetHost,
                AppWidgetManager appWidgetManager, AutoAdvanceHelper autoAdvanceHelper) {
            super(view);
            mAutoAdvanceHelper = autoAdvanceHelper;
            mAppWidgetHost = appWidgetHost;
            mAppWidgetManager = appWidgetManager;
            mChooseWidgetButton = view.findViewById(R.id.choose_app_widget_button);
            mErrorLoadingWidgetButton = view.findViewById(R.id.error_loading_widget_button);
            mContainer = (ViewGroup) view.findViewById(R.id.widget_container);
            mCardView = (CardView) view.findViewById(R.id.widget_card);

            mErrorLoadingWidgetButton.setVisibility(View.GONE);

            TypedArray a = view.getContext().obtainStyledAttributes(
                    new int[]{R.attr.appsiAppWidgetCardBackground});
            mDefaultColor = a.getColor(0, Color.BLACK);
            a.recycle();


            mTouchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        }

        public void postCreate() {
            mAppWidgetHost.registerHostStatusListener(this);
            mChooseWidgetButton.setOnClickListener(this);
            mErrorLoadingWidgetButton.setOnClickListener(this);
        }


        @Override
        public void onStartedListening() {
            updateConfiguration();
        }

        /**
         * Adds the widget to the container, returning true when successful. May fail
         * in cases where there is a RemoteException. In that case no widget is added.
         */
        private boolean addWidgetToContainer(Context context) {
            // We have a simple cache for the app-widget host-views. This
            // cache is only valid at the time where the host is listening
            // The cache is automatically cleared when the host is stopped
            // so this is just an optimization, to improve performance
            AppWidgetHostView cached = sAppWidgetViewCache.get(mAppWidgetId);
            if (cached == null) {
                mAppWidgetHostView = safelyCreateView(context, mAppWidgetId, mAppWidgetInfo);
            } else {
                mAppWidgetHostView = cached;
            }
            if (mAppWidgetHostView == null) {
                return false;
            }


            mAppWidgetHostView.setPadding(0, 0, 0, 0);
            mContainer.removeAllViews();

            mContainer.addView(mAppWidgetHostView);
            if (cached == null) {
                mContainer.setAlpha(0);
                mContainer.animate().alpha(1).withEndAction(null);
            }
            return true;
        }

        void updateWidgetSizeRanges(AppWidgetHostView widgetView, int w, int h) {
            Context context = widgetView.getContext();
            float density = context.getResources().getDisplayMetrics().density;
            widgetView.updateAppWidgetSize(null,
                    (int) (w / density),
                    (int) (h / density),
                    (int) (w / density),
                    (int) (h / density));
        }

        private AppWidgetHostView safelyCreateView(
                Context context, int appWidgetId, AppWidgetProviderInfo info) {

            // This is an IPC call, and may cause a crash such as
            // java.lang.RuntimeException: system server dead?
            // Caused by: android.os.TransactionTooLargeException
            try {
                AppWidgetHostView result = mAppWidgetHost.createView(context, appWidgetId, info);
                // add it to the cache
                sAppWidgetViewCache.put(appWidgetId, result);
                return result;
            } catch (RuntimeException e) {
                return null;
            }
        }

        @Override
        public void onStoppedListening() {
            mInitializedSinceLastStop = false;
        }

        @Override
        public void onViewsCleared() {
            clearAppWidget();
        }

        void clearAppWidget() {
            mAppWidgetId = -1;
            mAppWidgetInfo = null;
            if (mAppWidgetHostView != null) {
                mAutoAdvanceHelper.removeWidgetToAutoAdvance(mAppWidgetHostView);
            }

            mContainer.animate().alpha(0).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mContainer.removeAllViews();
                }
            });
            mAppWidgetHostView = null;
        }

        @Override
        void bind(HomeItem item, int heightPx) {
            super.bind(item, heightPx);
        }


        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mWidth = w;
            mHeight = h;
            if (mAppWidgetHostView != null) {
                updateWidgetSizeRanges(mAppWidgetHostView, w, h);
            }
        }


        @Override
        void updateConfiguration() {
            int old = mAppWidgetId;
            long cellId = mHomeItem.mId;

            Context context = itemView.getContext();

            String appWidgetId = mConfigurationHelper.getProperty(cellId, "app_widget_id", null);

            mAppWidgetId = appWidgetId == null ? -1 : Integer.parseInt(appWidgetId);

            boolean widgetActive;

            if (mAppWidgetId == -1) {
                mChooseWidgetButton.setVisibility(View.VISIBLE);
                widgetActive = false;
            } else if (mAppWidgetId != old) {
                mChooseWidgetButton.setVisibility(View.GONE);

                if (mAppWidgetHostView != null) {
                    mAutoAdvanceHelper.removeWidgetToAutoAdvance(mAppWidgetHostView);
                }

                mAppWidgetInfo = mAppWidgetManager.getAppWidgetInfo(mAppWidgetId);
                widgetActive = addWidgetToContainer(context);
            } else {
                mChooseWidgetButton.setVisibility(View.GONE);
                // nothing changed, so we don't need to do anything
                // we only re-create the view to ensure it is up-to-date
                widgetActive = addWidgetToContainer(context);

            }

            if (mHomeItem.mEffectColor == Color.TRANSPARENT) {
                mCardView.setCardBackgroundColor(mDefaultColor);
            } else {
                mCardView.setCardBackgroundColor(mHomeItem.mEffectColor);
            }

            if (widgetActive) {
                updateWidgetSizeRanges(mAppWidgetHostView, mWidth, mHeight);
                mAutoAdvanceHelper.addWidgetToAutoAdvanceIfNeeded(mAppWidgetHostView,
                        mAppWidgetInfo);
            } else if (mAppWidgetId != -1) {
                mErrorLoadingWidgetButton.setVisibility(View.VISIBLE);
            }
        }


        @Override
        public void onAttached(boolean allowLoads) {
            super.onAttached(allowLoads);
            if (mAppWidgetHost.isListening() && !mInitializedSinceLastStop) {
                mInitializedSinceLastStop = true;
                updateConfiguration();
            }
            if (mAppWidgetHostView != null) {
                mAutoAdvanceHelper.addWidgetToAutoAdvanceIfNeeded(mAppWidgetHostView,
                        mAppWidgetInfo);
            }
        }


        @Override
        public void onDetached(boolean allowLoads) {
            super.onDetached(allowLoads);

            if (mAppWidgetHostView != null) {
                mAutoAdvanceHelper.removeWidgetToAutoAdvance(mAppWidgetHostView);
            }
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.error_loading_widget_button) {
                mErrorLoadingWidgetButton.setVisibility(View.GONE);
                updateConfiguration();
            } else {
                Context context = itemView.getContext();
                Intent intent = new Intent(context, WidgetChooserActivity.class);
                intent.putExtra(WidgetChooserActivity.EXTRA_CELL_ID, mHomeItem.mId);
                context.startActivity(intent);
            }
        }

    }

    static class PlainWeatherWindViewHolder extends AbsWeatherWindViewHolder {

        public PlainWeatherWindViewHolder(HomeViewWrapper view) {
            super(view, false /* showsWallpaper */);
        }

        @Override
        void applyWeatherData(String unit, String windSpeed, WeatherData weatherData) {
            // properly convert the speed if needed
            if (!"c".equals(unit)) {
                int speed = Math.round(WeatherUtils.toKph(weatherData.windSpeed));
                mLargeWindmillDrawable.setWindSpeedKmh(speed);
                mSmallWindmillDrawable.setWindSpeedKmh(speed);
            } else {
                mLargeWindmillDrawable.setWindSpeedKmh(weatherData.windSpeed);
                mSmallWindmillDrawable.setWindSpeedKmh(weatherData.windSpeed);
            }


            mWindSpeedView.setText(windSpeed);
            mLargeWindmillDrawable.start();
            mSmallWindmillDrawable.start();

        }
    }

    static class WallpaperWeatherWindViewHolder extends AbsWeatherWindViewHolder {

        public WallpaperWeatherWindViewHolder(HomeViewWrapper view) {
            super(view, true /* showsWallpaper */);
        }

        @Override
        void applyWeatherData(String unit, String windSpeed, WeatherData weatherData) {
            // properly convert the speed if needed
            if (!"c".equals(unit)) {
                int speed = Math.round(WeatherUtils.toKph(weatherData.windSpeed));
                mLargeWindmillDrawable.setWindSpeedKmh(speed);
                mSmallWindmillDrawable.setWindSpeedKmh(speed);
            } else {
                mLargeWindmillDrawable.setWindSpeedKmh(weatherData.windSpeed);
                mSmallWindmillDrawable.setWindSpeedKmh(weatherData.windSpeed);
            }


            mWindSpeedView.setText(windSpeed);
            mLargeWindmillDrawable.start();
            mSmallWindmillDrawable.start();

        }
    }

    static abstract class AbsWeatherWindViewHolder extends AbsWeatherViewHolder
            implements View.OnClickListener,
            HomeItemConfigurationHelper.ConfigurationListener {

        final WindmillDrawable mLargeWindmillDrawable;

        final WindmillDrawable mSmallWindmillDrawable;

        final ImageView mLargeWindmill;

        final ImageView mSmallWindmill;

        final TextView mTextView;

        final TextView mWindSpeedView;

        public AbsWeatherWindViewHolder(HomeViewWrapper view, boolean showsWallpaper) {
            super(view, showsWallpaper);
            Context context = view.getContext();
            mLargeWindmill = (ImageView) view.findViewById(R.id.weather_windmill_large);
            mSmallWindmill = (ImageView) view.findViewById(R.id.weather_windmill_medium);
            mTextView = (TextView) view.findViewById(R.id.primary_text);
            mWindSpeedView = (TextView) view.findViewById(R.id.wind_speed);

            mLargeWindmillDrawable = new WindmillDrawable(context);
            mSmallWindmillDrawable = new WindmillDrawable(context);
            mLargeWindmill.setImageDrawable(mLargeWindmillDrawable);

            mSmallWindmill.setImageDrawable(mSmallWindmillDrawable);

        }

        public void onRecycled() {
            mLargeWindmillDrawable.stop();
            mSmallWindmillDrawable.stop();
        }

        @Override
        void bind(HomeItem item, int heightPx) {
            mHomeItem = item;
            applySuggestedHeight(heightPx);
            mChildView.setOnClickListener(this);
            updateConfiguration();
        }


        @Override
        public void onAttached(boolean allowLoads) {
            super.onAttached(allowLoads);
            mLargeWindmillDrawable.start();
            mSmallWindmillDrawable.start();
        }

        @Override
        public void onDetached(boolean allowLoads) {
            super.onDetached(allowLoads);
            mLargeWindmillDrawable.stop();
            mSmallWindmillDrawable.stop();
        }

        @Override
        void onWeatherDataReady(WeatherData weatherData) {
            String defaultUnit = mPreferenceHelper.getDefaultWeatherTemperatureUnit();

            String displayUnit = mConfigurationHelper.getProperty(mHomeItem.mId,
                    WeatherFragment.PREFERENCE_WEATHER_UNIT, defaultUnit);

            long cellId = mHomeItem.mId;


            setupTitle(mTextView, weatherData, cellId);
            String unit = weatherData.unit;
            String windSpeed = WeatherUtils.formatWindSpeed(
                    itemView.getContext(), weatherData.windSpeed, unit, displayUnit);

            applyWeatherData(unit, windSpeed, weatherData);

        }

        abstract void applyWeatherData(String unit, String windSpeed, WeatherData weatherData);

        @Override
        void onNoWeatherDataAvailable() {
            mTextView.setText(R.string.unknown);
            mWindSpeedView.setText("-");
            mLargeWindmillDrawable.stop();
            mSmallWindmillDrawable.stop();
        }

        @Override
        public void onClick(View v) {
            super.onClick(v);
        }
    }

    static class PlainWeatherSunViewHolder extends AbsWeatherSunViewHolder {

        public PlainWeatherSunViewHolder(HomeViewWrapper view) {
            super(view, false /* showsWallpaper */);
        }

        @Override
        void applyWeatherData(Context context, long cellId, int minuteNow,
                int sunriseMinuteOfDay, int sunsetMinuteOfDay, WeatherData weatherData) {
            setupTitle(mTextView, weatherData, cellId);

            // set the values on the drawable
            mSunriseDrawable.setTime(sunriseMinuteOfDay, sunsetMinuteOfDay, minuteNow);

            String time = formatAsTime(context, sunriseMinuteOfDay);
            mSunriseView.setText(time);

            time = formatAsTime(context, sunsetMinuteOfDay);
            mSunsetView.setText(time);

        }
    }

    static class WallpaperWeatherSunViewHolder extends AbsWeatherSunViewHolder {

        public WallpaperWeatherSunViewHolder(HomeViewWrapper view) {
            super(view, true /* showsWallpaper */);
            setIsRecyclable(false);
        }

        @Override
        protected void configurePaintJob(PaintJob.Builder paintJob) {
            paintJob.paintWithSwatch(PaintJob.SWATCH_DARK_VIBRANT,
                    ViewPainters.argb(128, R.id.primary_text),
                    ViewPainters.text(R.id.primary_text, R.id.sunset, R.id.sunrise),
                    new DrawablePainter(R.id.weather_sun)
            );
        }

        static class DrawablePainter extends PaintJob.BaseViewPainter {

            protected DrawablePainter(int... viewIds) {
                super(255, viewIds);
            }

            @Override
            public boolean canAnimate() {
                return false;
            }

            @Override
            protected int getCurrentColorFromView(View view) {
                return 0;
            }

            @Override
            protected int getTargetColorFromSwatch(Palette.Swatch swatch) {
                return swatch.getBodyTextColor();
            }

            @Override
            protected boolean applyColorsToView(View view, Palette.Swatch swatch) {
                SunriseDrawable drawable = (SunriseDrawable) ((ImageView) view).getDrawable();
                drawable.customTheme(R.drawable.ic_small_clear_day,
                        swatch.getRgb(), swatch.getBodyTextColor(), swatch.getRgb());
                return true;
            }

            @Override
            protected void applyColorToView(View view, int color) {
            }
        }

        @Override
        void applyWeatherData(Context context, long cellId, int minuteNow,
                int sunriseMinuteOfDay, int sunsetMinuteOfDay, WeatherData weatherData) {
            setupTitle(mTextView, weatherData, cellId);

            // set the values on the drawable
            mSunriseDrawable.setTime(sunriseMinuteOfDay, sunsetMinuteOfDay, minuteNow);

            String time = formatAsTime(context, sunriseMinuteOfDay);
            mSunriseView.setText(time);

            time = formatAsTime(context, sunsetMinuteOfDay);
            mSunsetView.setText(time);

        }
    }

    static abstract class AbsWeatherSunViewHolder extends AbsWeatherViewHolder
            implements View.OnClickListener,
            HomeItemConfigurationHelper.ConfigurationListener {

        /**
         * The string builder used by the formatter to format the time
         */
        final StringBuilder mStringBuilder = new StringBuilder();

        /**
         * The formatter used to format the time
         */
        final Formatter mFormatter = new Formatter(mStringBuilder);

        /**
         * The view containing the drawable that will draw the info
         */
        final ImageView mSunView;

        /**
         * The main text view containing the location name
         */
        final TextView mTextView;

        /**
         * The drawable doing all the work of displaying the arc and the
         * dots
         */
        final SunriseDrawable mSunriseDrawable;

        /**
         * The text-view that displays the sunrise time
         */
        final TextView mSunriseView;

        /**
         * The text-view that displays the sunset time
         */
        final TextView mSunsetView;

        final Time sTempTime = new Time();

        public AbsWeatherSunViewHolder(HomeViewWrapper view, boolean showsWallpaper) {
            super(view, showsWallpaper);
            Context context = view.getContext();
            mSunView = (ImageView) view.findViewById(R.id.weather_sun);
            mTextView = (TextView) view.findViewById(R.id.primary_text);
            mSunriseView = (TextView) view.findViewById(R.id.sunrise);
            mSunsetView = (TextView) view.findViewById(R.id.sunset);

            mSunriseDrawable = new SunriseDrawable(context);
            mSunView.setImageDrawable(mSunriseDrawable);
        }

        @Override
        void bind(HomeItem item, int heightPx) {
            mHomeItem = item;
            applySuggestedHeight(heightPx);
            mChildView.setOnClickListener(this);
            updateConfiguration();
        }

        @Override
        void onWeatherDataReady(WeatherData weatherData) {
            long cellId = mHomeItem.mId;

            Context context = itemView.getContext();

            String timezone = mConfigurationHelper.getProperty(
                    cellId, WeatherFragment.PREFERENCE_WEATHER_TIMEZONE, null);


            // get the minutes now, sunrise and sunset minutes.
            if (timezone == null) {
                sTempTime.timezone = TimeZone.getDefault().getID();
            } else {
                sTempTime.timezone = timezone;
            }
            sTempTime.setToNow();

            int minuteNow = sTempTime.minute + 60 * sTempTime.hour;
            int sunriseMinuteOfDay = weatherData.getSunriseMinuteOfDay();
            int sunsetMinuteOfDay = weatherData.getSunsetMinuteOfDay();

            applyWeatherData(
                    context, cellId, minuteNow, sunriseMinuteOfDay, sunsetMinuteOfDay, weatherData);
        }

        abstract void applyWeatherData(Context context, long cellId, int minuteNow,
                int sunriseMinuteOfDay, int sunsetMinuteOfDay, WeatherData weatherData);

        /**
         * Uses the formatter to render the minuteOfDay into a string.
         */
        String formatAsTime(Context context, int minuteOfDay) {
            // calculate the sunrise time in millis
            sTime.hour = minuteOfDay / 60;
            sTime.minute = minuteOfDay % 60;
            sTime.second = 0;
            long millisSunrise = sTime.normalize(true);

            // format it
            mStringBuilder.setLength(0);
            DateUtils.formatDateRange(context, mFormatter, millisSunrise, millisSunrise,
                    DateUtils.FORMAT_SHOW_TIME);

            // return the resulting string
            return mStringBuilder.toString();
        }

        @Override
        void onNoWeatherDataAvailable() {
            mTextView.setText(R.string.unknown);
        }

        @Override
        public void onClick(View v) {
            super.onClick(v);
        }

    }

    /**
     * Created by nick on 24/01/15.
     */
    static class HomeSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

        final Context mContext;

        int[] mHomeItemSpans;

        HomeSpanSizeLookup(Context context) {
            mContext = context;
        }


        void setup(List<HomeItem> homeItems) throws InconsistentRowsException {
            int row = 0;
            int totalItems = 0;

            int rowStartIdx = 0;
            long rowId = -1;

            mHomeItemSpans = new int[homeItems.size()];
            int pos = 0;
            for (HomeItem homeItem : homeItems) {
                //if (homeItem.mPageId != pageId) continue;

                if (row != homeItem.mRowPosition) {
                    multiplyPositions(totalItems, rowStartIdx, pos);

                    totalItems = 0;
                    rowStartIdx = pos;

                    row = homeItem.mRowPosition;
                }
                mHomeItemSpans[pos] = homeItem.mColspan;
                totalItems += homeItem.mColspan;

                pos++;
            }
            multiplyPositions(totalItems, rowStartIdx, pos);

        }


        /**
         * Multiplies the values at the given range (a single row) with the correct multiplier.
         * This makes sure we have the proper row-spans set up.
         */
        private void multiplyPositions(int itemsInRow, int rangeStart, int rangeEnd)
                throws InconsistentRowsException {

            int multiplier;
            if (itemsInRow == 1) {
                multiplier = 12;
            } else if (itemsInRow == 2) {
                multiplier = 6;
            } else if (itemsInRow == 3) {
                multiplier = 4;
            } else if (itemsInRow == 4) {
                multiplier = 3;
            } else {
                throw new InconsistentRowsException("Invalid count in row: " + itemsInRow);
            }

            for (int i = rangeStart; i < rangeEnd; i++) {
                mHomeItemSpans[i] = mHomeItemSpans[i] * multiplier;
            }
        }

        @Override
        public int getSpanSize(int position) {

            // Something in the JB accessibility compat causes these
            // conditions so we need to check for them
            if (mHomeItemSpans == null) return 1;
            if (position < 0) return 1;
            if (position >= mHomeItemSpans.length) return 1;

            return mHomeItemSpans[position];
        }
    }

    static class OperationWrapper implements Operation {

        final ContentProviderOperation.Builder mBuilder;

        public OperationWrapper(ContentProviderOperation.Builder builder) {
            mBuilder = builder;
        }

        static Operation newInsert(Uri uri) {
            return new OperationWrapper(ContentProviderOperation.newInsert(uri));
        }

        static Operation newDelete(Uri uri) {
            return new OperationWrapper(ContentProviderOperation.newDelete(uri));
        }

        static Operation newUpdate(Uri uri) {
            return new OperationWrapper(ContentProviderOperation.newUpdate(uri));
        }

        @Override
        public Operation withValue(String position, int value) {
            mBuilder.withValue(position, value);
            return this;
        }

        @Override
        public Operation withValue(String position, long value) {
            mBuilder.withValue(position, value);
            return this;
        }

        @Override
        public Operation withValueBackReference(String rowId, int resultIdx) {
            mBuilder.withValueBackReference(rowId, resultIdx);
            return this;
        }

        @Override
        public Operation withSelection(String selection, String[] selectionArgs) {
            mBuilder.withSelection(selection, selectionArgs);
            return this;
        }

        @Override
        public ContentProviderOperation build() {
            return mBuilder.build();
        }
    }

    static class DefaultOpsBuilder implements OpsBuilder {

        @Override
        public Operation newInsert(Uri uri) {
            return OperationWrapper.newInsert(uri);
        }

        @Override
        public Operation newDelete(Uri uri) {
            return OperationWrapper.newDelete(uri);
        }

        @Override
        public Operation newUpdate(Uri uri) {
            return OperationWrapper.newUpdate(uri);
        }
    }

    static class InconsistentRowsException extends Exception {

        public InconsistentRowsException() {
        }

        public InconsistentRowsException(String detailMessage) {
            super(detailMessage);
        }

        public InconsistentRowsException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public InconsistentRowsException(Throwable throwable) {
            super(throwable);
        }
    }

    class ProfileImageViewHolder extends BaseViewHolder implements View.OnClickListener,
            PopupMenu.OnMenuItemClickListener {

        final ImageView mImageView;

        Bitmap mUserImage;

        volatile int mViewWidth;

        volatile int mViewHeight;

        ContactBitmapLoader mImageLoader;

        public ProfileImageViewHolder(HomeViewWrapper view) {
            super(view);
            mImageView = (ImageView) view.findViewById(R.id.image);
            mOverflow.setOnClickListener(this);
        }

        Uri getProfileContactUri() {
            Context context = itemView.getContext();
            Cursor c = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.LOOKUP_KEY,
                    },
                    ContactsContract.Contacts.IS_USER_PROFILE + "=?",
                    new String[]{
                            String.valueOf("1")
                    },
                    null);
            Uri lookupUri;
            if (c.moveToNext()) {
                long id = c.getLong(0);
                String lookupKey = c.getString(1);
                Uri u = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);

                lookupUri = Uri.withAppendedPath(u, String.valueOf(id));
            } else {
                lookupUri = null;
            }

            c.close();
            return lookupUri;
        }

        void applyUserImage(Bitmap bitmap) {
            mUserImage = bitmap;
            if (bitmap != null) {
                Log.i("Home", "Loader userImage: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            }
            if (mUserImage == null) {
                mUserImage = BitmapFactory.decodeResource(mImageView.getResources(),
                        R.drawable.fallback_profile_image);
            }
            mImageView.setImageBitmap(mUserImage);

        }

        @Override
        void updateConfiguration() {
            loadUserImageIfNeeded();
        }

        @Override
        void bind(HomeItem item, int heightPx) {
            super.bind(item, heightPx);
            mChildView.setOnClickListener(this);
            if (!HomeViewWrapper.areLoadsDeferred()) {
                loadUserImageIfNeeded();
            }
        }

        private void loadUserImageIfNeeded() {
            try {
                loadUserImageIfNeededImpl();
            } catch (PermissionDeniedException e) {
                onPermissionDenied(e, Manifest.permission.READ_CONTACTS,
                        PERMISSION_ERROR_CONTACTS_PROFILE);
            }

        }

        private void loadUserImageIfNeededImpl() throws PermissionDeniedException {
            PermissionUtils.throwIfNotPermitted(mContext, Manifest.permission.READ_CONTACTS);
            if (mImageLoader != null) {
                mImageLoader.cancel(true);
            }
            final String contactId =
                    mConfigurationHelper.getProperty(mHomeItem.mId, "contactId", null);
            final String lookupKey =
                    mConfigurationHelper.getProperty(mHomeItem.mId, "lookupKey", null);

            mImageLoader = new ContactBitmapLoader(lookupKey, contactId);
            mImageLoader.execute(itemView.getContext());
        }

        @Override
        public void onDisallowLoads() {
            super.onDisallowLoads();
            if (mImageLoader != null) {
                mImageLoader.cancel(true);
            }
        }

        @Override
        public void onAllowLoads() {
            super.onAllowLoads();
            loadUserImageIfNeeded();
        }

        @Override
        public void onAttached(boolean allowLoads) {
            super.onAttached(allowLoads);
            if (allowLoads) {
                loadUserImageIfNeeded();
            }
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mViewWidth = w;
            mViewHeight = h;
            // we wait with the load until we get the onAttached signal
            // in case the view is locked
            if (!HomeViewWrapper.areLoadsDeferred()) {
                loadUserImageIfNeeded();
            }
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.overflow) {
                PopupMenuHelper.showPopupMenu(v, R.menu.home_item_profile_image, this);
            } else {
                // clicked on the image.
                final String contactId =
                        mConfigurationHelper.getProperty(mHomeItem.mId, "contactId", null);
                final String lookupKey =
                        mConfigurationHelper.getProperty(mHomeItem.mId, "lookupKey", null);

                if (contactId != null && lookupKey != null) {
                    // TODO: track this event
                    Context context = v.getContext();
                    Long cid = Long.parseLong(contactId);
                    Uri contactLookupUri = ContactsContract.Contacts.getLookupUri(cid, lookupKey);
                    Intent intent = new Intent(Intent.ACTION_VIEW, contactLookupUri);
                    AnalyticsManager analyticsManager = AnalyticsManager.getInstance(context);
                    analyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_OPEN_HOME_ITEM,
                            AnalyticsManager.CATEGORY_PEOPLE);
                    context.startActivity(intent);

                }
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_cell_image_prefs) {

                Context context = itemView.getContext();
                Intent intent = new Intent(context, CellProfileImageActivity.class);
                intent.putExtra(CellProfileImageActivity.EXTRA_CELL_ID, mHomeItem.mId);
                context.startActivity(intent);
                return true;
            }
            return false;
        }

        class ContactBitmapLoader extends AsyncTask<Context, Void, Bitmap> {

            private final String mLookupKey;

            private final String mContactId;

            public ContactBitmapLoader(String lookupKey, String contactId) {
                mLookupKey = lookupKey;
                mContactId = contactId;
            }

            @Override
            protected Bitmap doInBackground(Context... params) {
                Context context = params[0];
                if (mLookupKey != null && mContactId != null) {
                    long id = Long.parseLong(mContactId);
                    Contact contact = RawContactsLoader.
                            loadContact(context, id, mLookupKey);

                    if (contact != null && contact.mBitmap != null) {
                        int w = contact.mBitmap.getWidth();
                        int h = contact.mBitmap.getWidth();

                        float wscale = mViewWidth / (float) w;
                        float hscale = mViewWidth / (float) h;

                        float scale = Math.max(wscale, hscale);

                        int destH = (int) (h * scale);
                        int destW = (int) (w * scale);

                        // can happen when the view was detached earlier
                        if (destH == 0 || destW == 0) return null;

                        return Bitmap.createScaledBitmap(contact.mBitmap, destW, destH, true);
                    }
                }

                Bitmap result;
                try {
                    Uri contactUri = ContactsContract.Profile.CONTENT_URI;
                    result = BitmapUtils.
                            decodeContactImage(mContext, contactUri, mViewWidth, mViewHeight);

                    if (result == null) {
                        Uri lookupUri = getProfileContactUri();
                        if (lookupUri != null) {
                            result = BitmapUtils.decodeContactImage(
                                    mContext, lookupUri, mViewWidth, mViewHeight);
                        }
                    }
                } catch (PermissionDeniedException e) {
                    // this is already checked before we actually
                    // start and create the loader. So if it happens
                    // just return null.
                    return null;
                }

                return result;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                applyUserImage(bitmap);
            }
        }
    }

    void onPermissionDenied(PermissionDeniedException e, String permission, String id) {
        // This allows the HomePage to implement something to handle this
        mPermissionErrorListener.onPermissionDenied(
                permission, id, R.string.permission_reason_contacts_profile_image);
    }

    public void setPermissionErrorListener(
            PermissionErrorListener permissionErrorListener) {
        mPermissionErrorListener = permissionErrorListener;
    }

    interface PermissionErrorListener {

        /**
         * Informs about a permission error.
         *
         * @param id The id of the error. This is unique for the combination
         * of the cell and the permission that was denied
         */
        void onPermissionDenied(String permission, String id, @StringRes int textResId);
    }

    class SimpleTextViewViewHolder extends AbsHomeViewHolder {

        public SimpleTextViewViewHolder(HomeViewWrapper view) {
            super(view);
        }

        @Override
        void bind(HomeItem item, int heightPx) {
            applySuggestedHeight(heightPx);
            TextView myView = (TextView) mChildView;
            myView.setText("r" + item.mRowPosition + ":p" + item.mPosition + ":s" +
                    item.mColspan + " - " + item.mDisplayType);

        }
    }

    class EmptyViewViewHolder extends AbsHomeViewHolder {

        public EmptyViewViewHolder(HomeViewWrapper view) {
            super(view);
        }

        @Override
        void bind(HomeItem item, int heightPx) {
            applySuggestedHeight(heightPx);
        }
    }

    class HomeAdapterEditor {

        /**
         * The builder used to create the operations. This allows tests to
         * override the builder and add additional checks to the created
         * operations.
         */
        @VisibleForTesting
        OpsBuilder mOpsBuilder = new DefaultOpsBuilder();

        final List<HomeItem> mTemp = new LinkedList<>();

        public void moveRowDown(int rowPosition, List<ContentProviderOperation> ops) {
            if (isLastRow(rowPosition)) return;

            boolean addedOpDown = false;
            boolean addedOpUp = false;

            for (HomeItem homeItem : mHomeItems) {
                if (homeItem.mRowPosition == rowPosition) {
                    homeItem.mRowPosition++;
                    if (!addedOpDown) {
                        addRowPositionOp(ops, homeItem);
                        addedOpDown = true;
                    }
                } else if (homeItem.mRowPosition == rowPosition + 1) {
                    homeItem.mRowPosition--;
                    if (!addedOpUp) {
                        addRowPositionOp(ops, homeItem);
                        addedOpUp = true;
                    }
                }
            }
        }

        boolean isLastRow(int rowPosition) {
            for (HomeItem homeItem : mHomeItems) {
                if (homeItem.mRowPosition > rowPosition) return false;
            }
            return true;
        }

        private void addRowPositionOp(List<ContentProviderOperation> ops, HomeItem homeItem) {
            Uri uri = rowUri(homeItem);
            ops.add(mOpsBuilder.newUpdate(uri).
                            withValue(HomeContract.Rows.POSITION, homeItem.mRowPosition).
                            build()
            );
        }

        private Uri rowUri(HomeItem homeItem) {
            return ContentUris.withAppendedId(HomeContract.Rows.CONTENT_URI, homeItem.mRowId);
        }

        public void moveRowUp(int rowPosition, List<ContentProviderOperation> ops) {
            if (rowPosition == 0) return;

            boolean addedOpDown = false;
            boolean addedOpUp = false;

            for (HomeItem homeItem : mHomeItems) {
                if (homeItem.mRowPosition == rowPosition) {
                    homeItem.mRowPosition--;
                    if (!addedOpUp) {
                        addRowPositionOp(ops, homeItem);
                        addedOpUp = true;
                    }
                } else if (homeItem.mRowPosition == rowPosition - 1) {
                    homeItem.mRowPosition++;
                    if (!addedOpDown) {
                        addRowPositionOp(ops, homeItem);
                        addedOpDown = true;
                    }
                }
            }
        }

        public int getItemPosition(long id) {
            int position = 0;
            for (HomeItem homeItem : mHomeItems) {
                if (homeItem.mId == id) return position;
                position++;
            }
            return -1;
        }

        public void moveItemRight(int position, List<ContentProviderOperation> ops) {
            if (isInvalidPosition(position)) return;
            boolean isLast = mHomeItems.size() - 1 == position;
            if (isLast) return;

            HomeItem item = mHomeItems.get(position);
            HomeItem next = mHomeItems.get(position + 1);
            if (next.mRowPosition != item.mRowPosition) return;

            item.mPosition++;
            next.mPosition--;

            ops.add(mOpsBuilder.newUpdate(homeItemUri(item)).
                            withValue(HomeContract.Cells.POSITION, item.mPosition).
                            build()
            );
            ops.add(mOpsBuilder.newUpdate(homeItemUri(next)).
                            withValue(HomeContract.Cells.POSITION, next.mPosition).
                            build()
            );
        }

        private boolean isInvalidPosition(int position) {
            return (position < 0 || position >= mHomeItems.size());
        }

        Uri homeItemUri(HomeItem homeItem) {
            return ContentUris.withAppendedId(HomeContract.Cells.CONTENT_URI, homeItem.mId);
        }

        boolean canMoveLeft(int position) {
            if (position == 0) return false;
            HomeItem item = mHomeItems.get(position);
            HomeItem prev = mHomeItems.get(position - 1);
            return prev.mRowPosition == item.mRowPosition;
        }

        boolean canMoveRight(int position) {
            if (position >= mHomeItems.size() - 1) return false;
            HomeItem item = mHomeItems.get(position);
            HomeItem next = mHomeItems.get(position + 1);
            return next.mRowPosition == item.mRowPosition;
        }

        public boolean canMoveUp(int position) {
            HomeItem item = mHomeItems.get(position);
            if (item.mRowPosition == 0) return false;
            return true;
        }

        public boolean canMoveDown(int position) {
            HomeItem item = mHomeItems.get(position);
            HomeItem last = mHomeItems.get(mHomeItems.size() - 1);
            if (item.mRowPosition == last.mRowPosition) return false;
            return true;
        }

        public boolean canIncreaseHeight(int position) {
            return true;
        }

        public boolean canDecreaseHeight(int position) {
            HomeItem item = mHomeItems.get(position);
            return item.mRowHeight > 1;
        }

        public boolean canIncreaseSpan(int position) {
            HomeItem homeItem = mHomeItems.get(position);

            int mRowPosition = homeItem.mRowPosition;
            int totalSpan = getTotalSpanForRow(mRowPosition);
            int itemCountInRow = getItemCountInRow(mRowPosition);

            if (itemCountInRow == 1) return false;
            if (totalSpan >= 4) return false;
            return true;
        }

        private int getTotalSpanForRow(int rowPosition) {
            int spanCount = 0;
            for (HomeItem homeItem : mHomeItems) {
                if (homeItem.mRowPosition == rowPosition) {
                    spanCount += homeItem.mColspan;
                }
            }
            return spanCount;
        }

        private int getItemCountInRow(int rowPosition) {
            int itemCount = 0;
            for (HomeItem homeItem : mHomeItems) {
                if (homeItem.mRowPosition == rowPosition) {
                    itemCount++;
                }
            }
            return itemCount;

        }

        public boolean canDecreaseSpan(int position) {
            HomeItem item = mHomeItems.get(position);
            if (item.mColspan == 1) return false;
            return item.mColspan > 1;
        }

        // walter klep 076 501 0002
        public void moveItemLeft(int position, List<ContentProviderOperation> ops) {
            boolean isFirst = position == 0;
            if (isFirst) return;
            if (isInvalidPosition(position)) return;


            HomeItem item = mHomeItems.get(position);
            HomeItem prev = mHomeItems.get(position - 1);
            if (prev.mRowPosition != item.mRowPosition) return;

            prev.mPosition = item.mPosition;
            item.mPosition--;

            ops.add(mOpsBuilder.newUpdate(homeItemUri(item)).
                            withValue(HomeContract.Cells.POSITION, item.mPosition).
                            build()
            );
            ops.add(mOpsBuilder.newUpdate(homeItemUri(prev)).
                            withValue(HomeContract.Cells.POSITION, prev.mPosition).
                            build()
            );
        }

        public void increaseSpan(int position, List<ContentProviderOperation> ops) {
            HomeItem homeItem = mHomeItems.get(position);

            int mRowPosition = homeItem.mRowPosition;
            int totalSpan = getTotalSpanForRow(mRowPosition);
            int itemCountInRow = getItemCountInRow(mRowPosition);

            if (itemCountInRow == 1) return;
            if (totalSpan >= 4) return;

            if (ops != null) {
                Uri uri = homeItemUri(homeItem);
                ops.add(mOpsBuilder.newUpdate(uri).
                        withValue(HomeContract.Cells.COLSPAN, homeItem.mColspan + 1).
                        build());
            }
        }

        public void decreaseSpan(int position, List<ContentProviderOperation> ops) {
            HomeItem homeItem = mHomeItems.get(position);

            if (homeItem.mColspan == 1) return;

            int mRowPosition = homeItem.mRowPosition;
            int itemCountInRow = getItemCountInRow(mRowPosition);
            if (itemCountInRow == 1) return;


            if (ops != null) {
                Uri uri = homeItemUri(homeItem);
                ops.add(mOpsBuilder.newUpdate(uri).
                        withValue(HomeContract.Cells.COLSPAN, homeItem.mColspan - 1).
                        build());
            }

        }

        public void switchItemPositions(int positionFrom, int positionTo,
                List<ContentProviderOperation> ops) {
            // check for < 0
            if (positionFrom < 0) return;
            if (positionTo < 0) return;

            // TODO: the selection seems not to have been updated in the ui. update it
            if (positionTo >= mHomeItems.size() || positionFrom >= mHomeItems.size()) return;

            HomeItem h1 = mHomeItems.get(positionTo);
            HomeItem h2 = mHomeItems.get(positionFrom);

            // first, before changing anything add the ops to update the cells.
            // All we need to do is switch row-ids, col-span and position
            ops.add(mOpsBuilder.newUpdate(homeItemUri(h1)).
                            withValue(HomeContract.Cells.COLSPAN, h2.mColspan).
                            withValue(HomeContract.Cells._ROW_ID, h2.mRowId).
                            withValue(HomeContract.Cells.POSITION, h2.mPosition).
                            build()
            );
            ops.add(mOpsBuilder.newUpdate(homeItemUri(h2)).
                            withValue(HomeContract.Cells.COLSPAN, h1.mColspan).
                            withValue(HomeContract.Cells._ROW_ID, h1.mRowId).
                            withValue(HomeContract.Cells.POSITION, h1.mPosition).
                            build()
            );


        }

        public boolean canAddCellsInItemRow(int position) {
            // TODO: the selection seems not to have been updated in the ui. update it
            if (position >= mHomeItems.size()) return false;

            HomeItem homeItem = mHomeItems.get(position);

            int rowPosition = homeItem.mRowPosition;

            if (getItemCountInRow(rowPosition) == 4) return false;
            if (getTotalSpanForRow(rowPosition) == 4) return false;

            return true;
        }

        public void removeCellAtPosition(int position, List<ContentProviderOperation> ops) {
            if (position < 0) return;
            if (position >= mHomeItems.size()) return;

            HomeItem homeItem = mHomeItems.get(position);
            int rowPosition = homeItem.mRowPosition;
            int itemsInRow = getItemCountInRow(rowPosition);

            if (itemsInRow == 1) {
                removeRowAtItemPosition(position, ops);
                return;
            }

            int size = mHomeItems.size();
            // update the next items in the same row by decreasing their position nr.
            for (int i = position + 1; i < size; i++) {
                HomeItem next = mHomeItems.get(i);
                if (next.mRowPosition == homeItem.mRowPosition &&
                        next.mPosition >= homeItem.mPosition) {

                    // add the db-op
                    ops.add(mOpsBuilder.newUpdate(homeItemUri(next)).
                                    withValue(HomeContract.Cells.POSITION, next.mPosition - 1).
                                    build()
                    );
                } else {
                    break;
                }
            }

            // add the delete operations
            addDeleteCellOp(ops, homeItem);


        }

        public void removeRowAtItemPosition(int position, List<ContentProviderOperation> ops) {
            HomeItem item = mHomeItems.get(position);
            int rowPosition = item.mRowPosition;

            int size = mHomeItems.size();

            int rangeCount = 0;
            int rangeStart = -1;

            int lastRowPosition = -1;
            for (int i = size - 1; i >= 0; i--) {
                HomeItem homeItem = mHomeItems.get(i);
                if (homeItem.mRowPosition == rowPosition) {
                    rangeCount++;
                    addDeleteCellOp(ops, homeItem);
                } else if (homeItem.mRowPosition > rowPosition) {
                    homeItem.mRowPosition--;
                    if (lastRowPosition != homeItem.mRowPosition) {
                        addRowPositionOp(ops, homeItem);
                        lastRowPosition = homeItem.mRowPosition;
                    }
                } else if (rangeStart == -1) {
                    rangeStart = i;
                }
            }
            addDeleteRowOp(ops, item);

        }

        public void insertCellLeftOfPosition(int position, List<ContentProviderOperation> ops) {
            insertCellAtPosition(position, false, ops);
        }

        private void insertCellAtPosition(int position, boolean right,
                List<ContentProviderOperation> ops) {
            int rowPosition;
            int rowHeight;
            int cellPosition;
            long rowId;
            int size = mHomeItems.size();


            {
                // perform some checks that got a valid index
                if (isInvalidPosition(position)) return;

                HomeItem hi = mHomeItems.get(position);
                int spanCount = getTotalSpanForRow(hi.mRowPosition);
                if (spanCount > 3) return;
            }

            boolean append = size <= position;
            // in case we add at the last position in the list
            if (append) {
                HomeItem item = mHomeItems.get(position - 1);
                rowId = item.mRowId;
                rowPosition = item.mRowPosition;
                cellPosition = item.mPosition + 1;
                rowHeight = item.mRowHeight;

            } else {

                HomeItem item = mHomeItems.get(position);
                rowId = item.mRowId;
                rowPosition = item.mRowPosition;
                rowHeight = item.mRowHeight;
                cellPosition = item.mPosition;
                if (right) {
                    cellPosition++;
                }

                for (int i = position; i < size; i++) {
                    // if we are adding to the right, skip the item at position
                    if (right && i == position) continue;


                    // for all items in the same row, move them one to the right
                    HomeItem h = mHomeItems.get(i);
                    if (h.mRowPosition == item.mRowPosition) {
                        // add op
                        ops.add(mOpsBuilder.newUpdate(homeItemUri(h)).
                                        withValue(HomeContract.Cells.POSITION, h.mPosition + 1).
                                        build()
                        );
                    }
                }
            }

            createEmptyCell(rowId, rowPosition, rowHeight, cellPosition, ops);
        }

        private HomeItem createEmptyCell(long rowId, int rowPosition, int rowHeight,
                int cellPosition, List<ContentProviderOperation> ops) {
            HomeItem toInsert = new HomeItem();
            toInsert.mDisplayType = HomeContract.Cells.DISPLAY_TYPE_UNSET;
            toInsert.mRowId = rowId;
            toInsert.mRowPosition = rowPosition;
            toInsert.mPosition = cellPosition;
            toInsert.mColspan = 1;
            toInsert.mRowHeight = rowHeight;
            toInsert.mId = nextUnsetId();

            ops.add(mOpsBuilder.newInsert(HomeContract.Cells.CONTENT_URI).
                            withValue(HomeContract.Cells.TYPE, toInsert.mDisplayType).
                            withValue(HomeContract.Cells._ROW_ID, toInsert.mRowId).
                            withValue(HomeContract.Cells.POSITION, toInsert.mPosition).
                            withValue(HomeContract.Cells.COLSPAN, toInsert.mColspan).
                            build()
            );


            return toInsert;
        }

        public void insertCellRightOfPosition(int position, List<ContentProviderOperation> ops) {
            insertCellAtPosition(position, true, ops);
        }

        public void insertRowAbovePosition(int position, List<ContentProviderOperation> ops) {
            if (position >= mHomeItems.size()) return;
            if (position < 0) return;

            position = getFirstItemPositionOfRowAtPosition(position);

            int count = mHomeItems.size();
            int lastRowPosition = -1;

            for (int i = position; i < count; i++) {
                HomeItem homeItem = mHomeItems.get(i);
                homeItem.mRowPosition++;
                // if we haven't seen this row before create an op to update it's position
                if (lastRowPosition != homeItem.mRowPosition) {
                    addRowPositionOp(ops, homeItem);
                    lastRowPosition = homeItem.mRowPosition;
                }
            }

            HomeItem homeItem = mHomeItems.get(position);
            int rowPosition = homeItem.mRowPosition - 1;

            // we need to know the location of the insert op to pass it as a backref
            int insertOpPosition = ops.size();
            ops.add(mOpsBuilder.newInsert(HomeContract.Rows.CONTENT_URI).
                            withValue(HomeContract.Rows.POSITION, rowPosition).
                            withValue(HomeContract.Rows._PAGE_ID, homeItem.mPageId).
                            withValue(HomeContract.Rows.HEIGHT, 1).
                            build()
            );

            // now add the op to create the cell in the column with a backref
            createEmptyCellWithRowBackRef(rowPosition,
                    1 /* row-height */, 0 /* cellPosition */, ops, insertOpPosition);

        }

        private int getFirstItemPositionOfRowAtPosition(int position) {
            HomeItem itemAtPosition = mHomeItems.get(position);

            for (int i = position; i >= 0; i--) {
                HomeItem homeItem = mHomeItems.get(i);
                // the first item of a row with a different position is
                // the one item before the position we are looking for.
                // return position + 1;
                if (homeItem.mRowPosition != itemAtPosition.mRowPosition) {
                    return i + 1;
                }
            }
            // we are at the beginning, return 0 to indicate it should
            // be inserted as the very first item
            return 0;
        }

        private HomeItem createEmptyCellWithRowBackRef(int rowPosition, int rowHeight,
                int cellPosition, List<ContentProviderOperation> ops, int resultIdx) {
            HomeItem toInsert = new HomeItem();
            toInsert.mDisplayType = HomeContract.Cells.DISPLAY_TYPE_UNSET;
            toInsert.mRowPosition = rowPosition;
            toInsert.mPosition = cellPosition;
            toInsert.mColspan = 1;
            toInsert.mRowHeight = rowHeight;
            toInsert.mId = nextUnsetId();

            ops.add(mOpsBuilder.newInsert(HomeContract.Cells.CONTENT_URI).
                            withValue(HomeContract.Cells.TYPE, toInsert.mDisplayType).
                            withValueBackReference(HomeContract.Cells._ROW_ID, resultIdx).
                            withValue(HomeContract.Cells.POSITION, toInsert.mPosition).
                            withValue(HomeContract.Cells.COLSPAN, toInsert.mColspan).
                            build()
            );


            return toInsert;
        }

        public void insertRowBelowPosition(int position, List<ContentProviderOperation> ops) {
            if (isInvalidPosition(position)) return;

            position = getLastItemPositionOfRowAtPosition(position);
            if (position >= mHomeItems.size()) {
                position = mHomeItems.size() - 1;
            }

            int lastRowPosition = -1;
            int count = mHomeItems.size();

            for (int i = position + 1; i < count; i++) {
                HomeItem homeItem = mHomeItems.get(i);
                homeItem.mRowPosition++;

                // if we haven't seen this row before create an op to update it's position
                if (lastRowPosition != homeItem.mRowPosition) {
                    addRowPositionOp(ops, homeItem);
                    lastRowPosition = homeItem.mRowPosition;
                }
            }

            HomeItem homeItem = mHomeItems.get(position);
            int rowPosition = homeItem.mRowPosition + 1;

            // we need to know the location of the insert op to pass it as a backref
            int insertOpPosition = ops.size();
            ops.add(mOpsBuilder.newInsert(HomeContract.Rows.CONTENT_URI).
                            withValue(HomeContract.Rows.POSITION, rowPosition).
                            withValue(HomeContract.Rows._PAGE_ID, homeItem.mPageId).
                            withValue(HomeContract.Rows.HEIGHT, 1).
                            build()
            );

            // now add the op to create the cell in the column with a backref
            createEmptyCellWithRowBackRef(rowPosition,
                    1 /* row-height */, 0 /* cellPosition */, ops, insertOpPosition);
        }

        private int getLastItemPositionOfRowAtPosition(int position) {
            int count = mHomeItems.size();
            HomeItem itemAtPosition = mHomeItems.get(position);

            for (int i = position; i < count; i++) {
                HomeItem homeItem = mHomeItems.get(i);
                // the first item of a row with a different position is
                // the one item after the position we are looking for.
                // return position + 1;
                if (homeItem.mRowPosition != itemAtPosition.mRowPosition) {
                    return i - 1;
                }
            }
            // return the last valid position in the list
            return count - 1;
        }

        public void changeCellType(int position, int displayType,
                List<ContentProviderOperation> ops) {
            HomeItem homeItem = mHomeItems.get(position);
            homeItem.mDisplayType = displayType;

            // Note that the deletion of the configuration is done elsewhere to
            // prevent problems with the ConfigurationHelper's internal state.
            ops.add(mOpsBuilder.newUpdate(homeItemUri(homeItem)).
                            withValue(HomeContract.Cells.TYPE, displayType).
                            build()
            );

        }

        public void increaseHeight(int position, ArrayList<ContentProviderOperation> ops) {
            if (position < 0) return;
            if (position >= mHomeItems.size()) return;

            HomeItem homeItem = mHomeItems.get(position);

            mTemp.clear();
            getItemsInRow(homeItem.mRowId, mTemp);

            if (ops != null) {
                Uri uri = ContentUris.
                        withAppendedId(HomeContract.Rows.CONTENT_URI, homeItem.mRowId);

                ops.add(mOpsBuilder.newUpdate(uri).
                                withValue(HomeContract.Rows.HEIGHT, homeItem.mRowHeight + 1).
                                build()
                );
            }
        }

        public void decreaseHeight(int position, ArrayList<ContentProviderOperation> ops) {
            if (position < 0) return;
            if (position >= mHomeItems.size()) return;

            HomeItem homeItem = getItemAt(position);

            if (homeItem.mRowHeight <= 1) return;

            mTemp.clear();
            getItemsInRow(homeItem.mRowId, mTemp);

            if (ops != null) {
                Uri uri = ContentUris.
                        withAppendedId(HomeContract.Rows.CONTENT_URI, homeItem.mRowId);

                ops.add(mOpsBuilder.newUpdate(uri).
                                withValue(HomeContract.Rows.HEIGHT, homeItem.mRowHeight - 1).
                                build()
                );
            }

        }

        public HomeItem getItemAt(int position) {
            return HomeAdapter.this.getItemAt(position);
        }

        private void addDeleteCellOp(List<ContentProviderOperation> ops, HomeItem homeItem) {
            ops.add(mOpsBuilder.newDelete(homeItemUri(homeItem)).build());
        }

        private void addDeleteRowOp(List<ContentProviderOperation> ops, HomeItem homeItem) {
            ops.add(mOpsBuilder.newDelete(rowUri(homeItem)).build());
        }

        /**
         * Returns true when there are at least two rows in the adapter. Needed to see if
         * we can remove a row without removing the last one.
         */
        public boolean hasAtLeastTwoRows() {
            long rowId = -1;
            for (HomeItem homeItem : mHomeItems) {
                if (rowId == -1) {
                    rowId = homeItem.mRowId;
                } else if (rowId != homeItem.mRowId) {
                    return true;
                }
            }
            return false;
        }

    }

}
