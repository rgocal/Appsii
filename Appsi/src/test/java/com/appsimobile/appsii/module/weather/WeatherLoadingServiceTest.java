package com.appsimobile.appsii.module.weather;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.MockLocationManager;
import android.os.Handler;
import android.os.Looper;

import com.appsimobile.appsii.module.SimplePreferences;
import com.appsimobile.appsii.module.home.config.HomeItemConfiguration;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationFactory;
import com.appsimobile.appsii.module.home.config.HomeItemConfigurationHelper;
import com.appsimobile.appsii.module.home.config.MockHomeItemConfigurationHelper;
import com.appsimobile.appsii.preference.PreferencesFactory;
import com.appsimobile.util.CollectionUtils;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

/**
 * Created by nmartens on 06/08/15.
 */
@SuppressWarnings("MissingPermission")
@RunWith(MockitoJUnitRunner.class)
public class WeatherLoadingServiceTest {

    @Mock
    Context mContext;

    @Mock
    MockLocationManager mLocationManager;

    Location mLocationResult;

    long mWaitTime;

    void initLocationManagerMocks(boolean networkEnabled, String... allProviders) {
        Mockito.when(mLocationManager.getAllProviders())
                .thenReturn(CollectionUtils.asList(allProviders));
        Mockito.when(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                .thenReturn(networkEnabled);

    }

    @Before
    public void initMocks() {

        Mockito.when(mContext.getSystemService(Context.LOCATION_SERVICE))
                .thenReturn(mLocationManager);
        Mockito.when(mContext.getApplicationContext()).thenReturn(mContext);


        final Location loc = new Location(LocationManager.NETWORK_PROVIDER);
        loc.setLatitude(4.5);
        loc.setLongitude(31.0);
        mLocationResult = loc;

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Looper looper = (Looper) invocation.getArguments()[2];
                final LocationListener l = (LocationListener) invocation.getArguments()[1];
                if (looper != null) {
                    Handler h = new Handler(looper);
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            l.onLocationChanged(loc);
                        }
                    }, mWaitTime);
                } else {
                    l.onLocationChanged(loc);
                }
                return null;
            }
        }).when(mLocationManager).requestSingleUpdate(eq(LocationManager.NETWORK_PROVIDER),
                any(LocationListener.class), any(Looper.class));


        SimplePreferences preferences = new SimplePreferences();
        preferences.put("default_user_country", "mock_country")
                .put("default_user_display_name", "Mock Country")
                .put("default_user_timezone", "mock")
                .put("default_user_woeid", "12345");

        PreferencesFactory.setPreferences(preferences);

        HomeItemConfigurationHelper.setFactory(
                new HomeItemConfigurationFactory() {
                    @Override
                    public HomeItemConfiguration createInstance(
                            Context context) {
                        return new MockHomeItemConfigurationHelper(mContext,
                                new ArrayList<Runnable>());
                    }
                });

        mWaitTime = 0;
    }

    @Test
    public void testLocation_available() throws InterruptedException {
        initLocationManagerMocks(true, LocationManager.NETWORK_PROVIDER);
        WeatherLoadingService weatherLoadingService = new WeatherLoadingService(mContext);
        Location result = weatherLoadingService.requestLocationInfoBlocking();
        Assert.assertEquals(mLocationResult, result);
    }
    @Test
    public void testLocation_providerDisabled() throws InterruptedException {
        initLocationManagerMocks(false, LocationManager.NETWORK_PROVIDER);
        WeatherLoadingService weatherLoadingService = new WeatherLoadingService(mContext);
        Location result = weatherLoadingService.requestLocationInfoBlocking();
        Assert.assertNull(result);
    }
    @Test
    public void testLocation_noNetworkProvider() throws InterruptedException {
        initLocationManagerMocks(true, LocationManager.PASSIVE_PROVIDER);
        WeatherLoadingService weatherLoadingService = new WeatherLoadingService(mContext);
        Location result = weatherLoadingService.requestLocationInfoBlocking();
        Assert.assertNull(result);
    }
}