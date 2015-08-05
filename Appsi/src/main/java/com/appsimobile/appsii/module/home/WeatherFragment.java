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

import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.appsimobile.appsii.AccountHelper;
import com.appsimobile.appsii.ActivityUtils;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.module.weather.loader.YahooWeatherApiClient;
import com.appsimobile.appsii.preference.PreferenceHelper;

/**
 * Created by Nick on 20/02/14.
 */
public class WeatherFragment extends Fragment
        implements YahooLocationChooserDialogFragment.LocationResultListener,
        CompoundButton.OnCheckedChangeListener {

    public static final String PREFERENCE_WEATHER_LOCATION = "pref_weather_location";

    public static final String PREFERENCE_WEATHER_WOEID = "pref_weather_woeid";

    public static final String PREFERENCE_WEATHER_TIMEZONE = "pref_weather_timezone";

    public static final String PREFERENCE_WEATHER_TITLE_TYPE = "pref_weather_title_type";

    // c for celcius, f for fahrenheit
    public static final String PREFERENCE_WEATHER_UNIT = "pref_weather_unit";

    public static final int UNIT_INDEX_CELSIUS = 0;

    public static final int UNIT_INDEX_FAHRENHEIT = 1;

    public static final int TITLE_INDEX_CONDITION = 0;

    public static final int TITLE_INDEX_LOCATION = 1;

    HomeItemConfiguration mConfigurationHelper;

    long mCellId;

    PreferenceHelper mPreferenceHelper;

    int mCellType;

    CellTypeQueryHandler mCellTypeQueryHandler;

    private TextView mLocationSpinner;

    private TextView mUnitSpinner;

    private TextView mTitleSpinner;

    private SwitchCompat mLocationImageSwitch;

    public static WeatherFragment createInstance(long cellId, int cellType) {
        // check validity of the cellType
        switch (cellType) {
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE_WALLPAPER:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP_WALLPAPER:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND_WALLPAPER:
                break;
            default:
                throw new IllegalArgumentException("Invalid cell type");
        }

        WeatherFragment result = new WeatherFragment();
        Bundle args = new Bundle();
        args.putLong("cellId", cellId);
        args.putInt("cellType", cellType);
        result.setArguments(args);
        return result;
    }

    static boolean isPlainVariant(int cellType) {
        return getPlainVariantForWeatherCellType(cellType) == cellType;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferenceHelper = PreferenceHelper.getInstance(getActivity());
        mConfigurationHelper = HomeItemConfigurationHelper.getInstance(getActivity());
        Bundle arguments = getArguments();
        mCellId = arguments.getLong("cellId");
        mCellType = arguments.getInt("cellType");


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // find the views we need to do stuff with
        mLocationSpinner = (TextView) view.findViewById(R.id.weather_location);
        mUnitSpinner = (TextView) view.findViewById(R.id.weather_unit);
        mTitleSpinner = (TextView) view.findViewById(R.id.title_value);
        mLocationImageSwitch = (SwitchCompat) view.findViewById(R.id.location_image_switch);

        View locationSwitchContainer = view.findViewById(R.id.background_container);

        View unitContainer = view.findViewById(R.id.unit_container);
        View locationContainer = view.findViewById(R.id.location_container);
        View titleContainer = view.findViewById(R.id.title_container);

        // setup the toolbar
        Toolbar toolbar = ActivityUtils.getToolbarPlainNoTitle(getActivity(), R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onNavigateUp();
            }
        });

        String defaultUnit = mPreferenceHelper.getDefaultWeatherTemperatureUnit();

        // We need to see if the cell has the wallpaper variant enabled.
        // This is actually a different cell-type, but that does not matter
        // to the user
        boolean isWallpaperVariantEnabled = isWallpaperVariant(mCellType);

        // For now, the wallpaper variant is only enabled for the temperature
        // view. So in all other cases, hide the view to change that.
        boolean hasWallpaperVariant = hasWallpaperVariant(mCellType);
        if (!hasWallpaperVariant) {
            locationSwitchContainer.setVisibility(View.GONE);
        }

        // Get the cell's configuration
        String weatherUnit = mConfigurationHelper.
                getProperty(mCellId, PREFERENCE_WEATHER_UNIT, defaultUnit);

        String location = mConfigurationHelper.
                getProperty(mCellId, PREFERENCE_WEATHER_LOCATION, null);

        String woeid = mConfigurationHelper.getProperty(mCellId, PREFERENCE_WEATHER_WOEID, null);

        String defaultTitleType = mPreferenceHelper.getDefaultWeatherTitleType();
        String titleType = mConfigurationHelper.
                getProperty(mCellId, PREFERENCE_WEATHER_TITLE_TYPE, defaultTitleType);

        // When there is no woe-id, this means use local weather.
        if (woeid == null) {
            mLocationSpinner.setText(R.string.weather_auto_location);
        } else {
            mLocationSpinner.setText(location);
        }

        // setup the correct values for all components
        mLocationImageSwitch.setChecked(isWallpaperVariantEnabled);
        mLocationImageSwitch.setOnCheckedChangeListener(this);

        setTitleTypeSpinnerValueFromPreferenceValue(titleType);
        setUnitSpinnerValueFromPreferenceValue(weatherUnit);

        // add the listeners as needed
        locationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLocationChooser();
            }
        });
        unitContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUnitPicker();
            }
        });
        titleContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTitlePicker();
            }
        });

        // reconnect to the existing fragment.
        Fragment fragment = getFragmentManager().findFragmentByTag("weather_dialog");
        if (fragment != null) {
            ((YahooLocationChooserDialogFragment) fragment).setLocationResultListener(this);
        }
    }

    static boolean isWallpaperVariant(int cellType) {
        return getWallpaperVariantForWeatherCellType(cellType) == cellType;
    }

    static boolean hasWallpaperVariant(int cellType) {
        switch (cellType) {
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE_WALLPAPER:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND_WALLPAPER:
                return false;
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP_WALLPAPER:
                return true;
        }
        throw new IllegalStateException("Unknown cell type for weather settings: " + cellType);
    }

    void setTitleTypeSpinnerValueFromPreferenceValue(String preferenceValue) {
        int resId = "condition".equals(preferenceValue) ?
                R.string.weather_condition_title : R.string.weather_location_name;

        String value = getString(resId);
        mTitleSpinner.setText(value);
    }

    void setUnitSpinnerValueFromPreferenceValue(String preferenceValue) {
        int resId = !"c".equals(preferenceValue) ? R.string.imperial : R.string.metric;
        String value = getString(resId);
        mUnitSpinner.setText(value);
    }

    void showLocationChooser() {
        YahooLocationChooserDialogFragment fragment = new YahooLocationChooserDialogFragment();
        fragment.setLocationResultListener(this);
        fragment.show(getFragmentManager(), "weather_dialog");
    }

    void showUnitPicker() {
        String defaultUnit = mPreferenceHelper.getDefaultWeatherTemperatureUnit();
        String weatherUnit =
                mConfigurationHelper.getProperty(mCellId, PREFERENCE_WEATHER_UNIT, defaultUnit);
        int index = "c".equals(weatherUnit) ? UNIT_INDEX_CELSIUS : UNIT_INDEX_FAHRENHEIT;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] units = getResources().getStringArray(R.array.weather_units);
        builder.setSingleChoiceItems(units, index, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                onWeatherUnitPicked(index);
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(true);
        builder.show();
    }

    void showTitlePicker() {
        String defaultTitleType = mPreferenceHelper.getDefaultWeatherTitleType();

        String titleType =
                mConfigurationHelper.getProperty(mCellId, PREFERENCE_WEATHER_TITLE_TYPE,
                        defaultTitleType);
        int index = "condition".equals(titleType) ? TITLE_INDEX_CONDITION : TITLE_INDEX_LOCATION;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] units = getResources().getStringArray(R.array.weather_title_types);
        builder.setSingleChoiceItems(units, index, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                onTitleTypePicked(index);
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(true);
        builder.show();
    }

    static int getWallpaperVariantForWeatherCellType(int cellType) {
        switch (cellType) {
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE_WALLPAPER:
                return HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE_WALLPAPER;
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP_WALLPAPER:
                return HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP_WALLPAPER;
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND_WALLPAPER:
                return HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND_WALLPAPER;

        }
        throw new IllegalStateException("Unknown cell type for weather settings: " + cellType);
    }

    void onWeatherUnitPicked(int index) {
        boolean isCelsius = index == UNIT_INDEX_CELSIUS;
        String value = isCelsius ? "c" : "f";
        mConfigurationHelper.updateProperty(mCellId, PREFERENCE_WEATHER_UNIT, value);
        setUnitSpinnerValueFromPreferenceValue(value);
        forceReloadWeatherInfo(null);
    }

    void onTitleTypePicked(int index) {
        boolean isCondition = index == TITLE_INDEX_CONDITION;
        String value = isCondition ? "condition" : "location";
        mConfigurationHelper.updateProperty(mCellId, PREFERENCE_WEATHER_TITLE_TYPE, value);
        setTitleTypeSpinnerValueFromPreferenceValue(value);
    }

    private void forceReloadWeatherInfo(String woeid) {
        AccountHelper.getInstance(getActivity()).requestSync(woeid);
    }

    @Override
    public void onLocationSearchResult(YahooWeatherApiClient.LocationSearchResult value) {
        if (value == null) {
            mConfigurationHelper.removeProperty(mCellId, PREFERENCE_WEATHER_LOCATION);
            mConfigurationHelper.removeProperty(mCellId, PREFERENCE_WEATHER_WOEID);
            mConfigurationHelper.removeProperty(mCellId, PREFERENCE_WEATHER_TIMEZONE);
            mLocationSpinner.setText(R.string.weather_auto_location);
            return;
        }
        String woeid = value.woeid;
        String locationName = value.displayName;
        String timezone = value.timezone;
        mConfigurationHelper.updateProperty(mCellId, PREFERENCE_WEATHER_LOCATION, locationName);
        mConfigurationHelper.updateProperty(mCellId, PREFERENCE_WEATHER_WOEID, woeid);
        mConfigurationHelper.updateProperty(mCellId, PREFERENCE_WEATHER_TIMEZONE, timezone);
        mLocationSpinner.setText(locationName);

        // start the service to update the weatherinfo
        forceReloadWeatherInfo(woeid);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mCellType = getWallpaperVariantForWeatherCellType(mCellType);
        } else {
            mCellType = getPlainVariantForWeatherCellType(mCellType);
        }
        changeCellToType(mCellId, mCellType);
    }

    static int getPlainVariantForWeatherCellType(int cellType) {
        switch (cellType) {
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE_WALLPAPER:
                return HomeContract.Cells.DISPLAY_TYPE_WEATHER_SUNRISE;
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP_WALLPAPER:
                return HomeContract.Cells.DISPLAY_TYPE_WEATHER_TEMP;
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND:
            case HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND_WALLPAPER:
                return HomeContract.Cells.DISPLAY_TYPE_WEATHER_WIND;

        }
        throw new IllegalStateException("Unknown cell type for weather settings: " + cellType);
    }

    void changeCellToType(long cellId, int cellType) {
        if (mCellTypeQueryHandler == null) {
            mCellTypeQueryHandler = new CellTypeQueryHandler(getActivity().getContentResolver());
        }
        mCellTypeQueryHandler.setCellType(cellId, cellType);
    }

    static class CellTypeQueryHandler extends AsyncQueryHandler {

        public CellTypeQueryHandler(ContentResolver cr) {
            super(cr);
        }

        public void setCellType(long cellId, int cellType) {
            ContentValues values = new ContentValues(1);
            values.put(HomeContract.Cells.TYPE, cellType);
            Uri uri = ContentUris.withAppendedId(HomeContract.Cells.CONTENT_URI, cellId);
            startUpdate(0, null, uri, values, null, null);
        }
    }

}
