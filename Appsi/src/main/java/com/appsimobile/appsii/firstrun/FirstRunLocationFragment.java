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
 *
 */

package com.appsimobile.appsii.firstrun;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.appsimobile.appsii.LocationLoader;
import com.appsimobile.appsii.LocationReceiver;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.home.YahooLocationChooserDialogFragment;
import com.appsimobile.appsii.module.weather.loader.YahooWeatherApiClient;
import com.appsimobile.appsii.permissions.PermissionUtils;
import com.appsimobile.appsii.preference.PreferenceHelper;

/**
 * Created by nick on 10/06/15.
 */
public final class FirstRunLocationFragment extends Fragment implements View.OnClickListener,
        YahooLocationChooserDialogFragment.LocationResultListener, LocationReceiver {

    View mChooseLocationButton;

    TextView mDefaultLocationName;

    Button mNextButton;

    LocationLoader mLocationLoader;

    @Nullable
    YahooWeatherApiClient.LocationSearchResult mLocationResult;

    boolean mSetToLocalSystem;

    RadioButton mMetricRadioButton;

    RadioButton mImperialRadioButton;

    private OnLocationCompletedListener mOnFirstRunCompletedListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getFragmentManager();
        YahooLocationChooserDialogFragment locationDialog =
                (YahooLocationChooserDialogFragment) fm.findFragmentByTag("location_dialog");
        if (locationDialog != null) {
            locationDialog.setLocationResultListener(this);
        }

        if (savedInstanceState != null) {
            mLocationResult = savedInstanceState.getParcelable("location");
            mSetToLocalSystem = savedInstanceState.getBoolean("set_to_local_system");
        } else {
            PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getActivity());
            mLocationResult = preferenceHelper.getDefaultUserLocation();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first_run_location, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mChooseLocationButton = view.findViewById(R.id.welcome_fallback_location);
        mNextButton = (Button) view.findViewById(R.id.next_button);
        mDefaultLocationName = (TextView) view.findViewById(R.id.welcome_fallback_location_name);
        mImperialRadioButton = (RadioButton) view.findViewById(R.id.default_imperial);
        mMetricRadioButton = (RadioButton) view.findViewById(R.id.default_metric);

        mChooseLocationButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);


        if (mLocationResult == null) {
            mNextButton.setEnabled(false);
            String unknown = getString(R.string.unknown);
            String location = getString(R.string.location_name, unknown);
            mDefaultLocationName.setText(location);
        } else {
            String location = getString(R.string.location_name, mLocationResult.displayName);
            mDefaultLocationName.setText(location);
        }

        if (!mSetToLocalSystem) {
            mSetToLocalSystem = true;
            String countryCode = getResources().getConfiguration().locale.getCountry();
            boolean useImperial = "US".equals(countryCode) ||
                    "LR".equals(countryCode) || "MM".equals(countryCode);

            if (useImperial) {
                mImperialRadioButton.setChecked(true);
            } else {
                mMetricRadioButton.setChecked(true);
            }

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mLocationResult == null && PermissionUtils.holdsPermission(
                getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {

            try {
                mLocationLoader = new LocationLoader(this);
                boolean success = mLocationLoader.requestLocationUpdate(getActivity());
                if (!success) {
                    Log.w("FirstRun", "Provider NETWORK does not exist");
                }
            } catch (SecurityException ignore) {
                // should not happen as we checked the permission before
                // also does not really matter
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("location", mLocationResult);
        outState.putBoolean("set_to_local_system", mSetToLocalSystem);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLocationLoader != null) {
            mLocationLoader.destroy();
        }
    }

    public void setOnLocationCompletedListener(
            OnLocationCompletedListener onLocationCompletedListener) {
        mOnFirstRunCompletedListener = onLocationCompletedListener;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.welcome_fallback_location:
                onChooseLocationPressed();
                break;
            case R.id.next_button:
                onNextButtonPressed();
                break;
        }
    }

    private void onChooseLocationPressed() {
        YahooLocationChooserDialogFragment locationDialog =
                YahooLocationChooserDialogFragment.newInstance(true /* no hints */);

        locationDialog.setLocationResultListener(this);
        locationDialog.show(getFragmentManager(), "location_dialog");
    }

    private void onNextButtonPressed() {
        // save imperial/metric
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance(getActivity());
        String unit = mMetricRadioButton.isChecked() ? "c" : "f";
        preferenceHelper.setDefaultWeatherTemperatureUnit(unit);

        mOnFirstRunCompletedListener.onLocationCompleted();
    }

    @Override
    public void onLocationSearchResult(YahooWeatherApiClient.LocationSearchResult result) {
        if (result != null) {
            saveLocationResult(result);
        }
    }

    private void saveLocationResult(YahooWeatherApiClient.LocationSearchResult result) {
        mLocationResult = result;
        // apply the label text
        String location = getString(R.string.location_name, result.displayName);
        mDefaultLocationName.setText(location);

        // we have a location, so enable the next button
        mNextButton.setEnabled(true);

        // Store the location somewhere in preferences
        // so we can use it again
        PreferenceHelper.getInstance(getActivity()).updateDefaultUserLocation(result);
    }

    @Override
    public void onCurrentLocationInfoReady(String woeid, String country, String town,
            String timezone) {

        YahooWeatherApiClient.LocationSearchResult locationResult =
                new YahooWeatherApiClient.LocationSearchResult();
        locationResult.woeid = woeid;
        locationResult.country = country;
        locationResult.timezone = timezone;
        locationResult.displayName = town;
        saveLocationResult(locationResult);
    }

    public interface OnLocationCompletedListener {

        void onLocationCompleted();
    }
}
