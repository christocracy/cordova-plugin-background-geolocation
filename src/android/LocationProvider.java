/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

/**
 * LocationProvider
 */
public interface LocationProvider {

    public void onCreate();
    public void onDestroy();
    public void startRecording();
    public void stopRecording();
}
