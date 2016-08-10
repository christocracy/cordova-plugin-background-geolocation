package com.marianhello.cdvbackgroundgeolocation;

import android.location.Location;
import android.test.suitebuilder.annotation.SmallTest;

import com.marianhello.bgloc.data.BackgroundLocation;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Created by finch on 10/08/16.
 */
@SmallTest
public class BackgroundLocationTest {

    @Test
    public void gpsLocationShouldBeBetterThanNetwork() {

        BackgroundLocation netLocation = new BackgroundLocation();
        netLocation.setProvider("network");
        netLocation.setLatitude(49);
        netLocation.setLongitude(5);
        netLocation.setAccuracy(38);
        netLocation.setTime(1000 * 60 * 2);

        BackgroundLocation gpsLocation = new BackgroundLocation();
        gpsLocation.setProvider("gps");
        gpsLocation.setLatitude(49);
        gpsLocation.setLongitude(5);
        gpsLocation.setAccuracy(5);
        gpsLocation.setTime(0);

        Assert.assertTrue(gpsLocation.isBetterLocationThan(netLocation));
    }
}
