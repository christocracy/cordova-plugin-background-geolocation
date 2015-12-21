/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

import android.content.Context;
import com.tenforwardconsulting.cordova.bgloc.data.DAOFactory;
import com.tenforwardconsulting.cordova.bgloc.ServiceProvider;
import com.tenforwardconsulting.cordova.bgloc.DistanceFilterLocationProvider;
import com.tenforwardconsulting.cordova.bgloc.FusedLocationProvider;
import java.lang.IllegalArgumentException;

/**
 * ServiceProviderFactory
 */
public class ServiceProviderFactory {

    private Context context;
    private Config config;

    public ServiceProviderFactory(Context context, Config config) {
        this.context = context;
        this.config = config;
    };

    public ServiceProvider getInstance (ServiceProviderEnum provider) {
        switch (provider) {
            case ANDROID_DISTANCE_FILTER:
                return new DistanceFilterLocationProvider(DAOFactory.createLocationDAO(context), config, context);
            case ANDROID_FUSED_LOCATION:
                return new FusedLocationProvider(DAOFactory.createLocationDAO(context), config, context);
            default:
                throw new IllegalArgumentException("Provider not found");
        }
    }
}
