package com.marianhello.bgloc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import java.util.List;

/**
 * Created by finch on 20/07/16.
 */
public class LocationSampler implements LocationListener {

    public static final int MAX_LOCATION_WAIT_TIME_MILLIS = 15000;

    private Long sampleStartTime;
    private LocationManager locationManager;

    public void sample() throws SecurityException {
        sampleStartTime = System.currentTimeMillis();

        List<String> matchingProviders = locationManager.getAllProviders();
        for (String provider : matchingProviders) {
            if (provider != LocationManager.PASSIVE_PROVIDER) {
                locationManager.requestLocationUpdates(provider, 0, 0, this);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
