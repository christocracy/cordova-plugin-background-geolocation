/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

/**
 * ServiceProviderEnum
 */
public enum ServiceProviderEnum
{
    ANDROID_DISTANCE_FILTER(0),
    ANDROID_FUSED_LOCATION(1);

    public final int id;

    private ServiceProviderEnum(int id) {
        this.id = id;
    }

    public int asInt() {
        return this.id;
    }

    public static ServiceProviderEnum forInt (int id) {
        for (ServiceProviderEnum provider : values()) {
            if (provider.id == id) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Invalid ServiceProvider id: " + id);
    }
}
