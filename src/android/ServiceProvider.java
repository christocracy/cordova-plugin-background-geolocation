/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

/**
 * ServiceProvider
 */
public enum ServiceProvider
{
    ANDROID_DISTANCE_FILTER(0), ANDROID_FUSED_LOCATION(1);

    public final int id;

    private ServiceProvider(int id) {
        this.id = id;
    }

    public int asInt() {
        return this.id;
    }

    public static ServiceProvider forInt (int id) {
        for (ServiceProvider provider : values()) {
            if (provider.id == id) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Invalid ServiceProvider id: " + id);
    }

    public static Class getClass (ServiceProvider provider) throws ClassNotFoundException {
        switch (provider) {
            case ANDROID_DISTANCE_FILTER:
                return com.tenforwardconsulting.cordova.bgloc.DistanceFilterLocationService.class;
            case ANDROID_FUSED_LOCATION:
                return com.tenforwardconsulting.cordova.bgloc.FusedLocationService.class;
            default:
                throw new ClassNotFoundException();
        }
    }
}
