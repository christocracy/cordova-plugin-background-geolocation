/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

/**
 * LocationProviderEnum
 */
public enum LocationProviderEnum
{
    ANDROID_DISTANCE_FILTER_PROVIDER(0),
    ANDROID_ACTIVITY_PROVIDER(1);

    public final int id;

    private LocationProviderEnum(int id) {
        this.id = id;
    }

    public int asInt() {
        return this.id;
    }

    public static LocationProviderEnum forInt (int id) {
        for (LocationProviderEnum provider : values()) {
            if (provider.id == id) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Invalid ServiceProvider id: " + id);
    }
}
