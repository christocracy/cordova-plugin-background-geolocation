/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

/**
 * Constants
 */
public abstract class Constant
{
    private static final String P_NAME = "com.tenforwardconsulting.cordova.bgloc";

    public static final String DATA = "DATA";
    public static final String ACTION = "ACTION";
    public static final String ACTION_FILTER = P_NAME + ".cordova.bgloc.ACTION";
    public static final String LOCATION_UPDATE_FILTER = P_NAME + ".cordova.bgloc.LOCATION_UPDATE";
    public static final String DETECTED_ACTIVITY_UPDATE = P_NAME + ".DETECTED_ACTIVITY_UPDATE";
    public static final String LOCATION_SENT_INDICATOR = "LOCATION_SENT";
    public static final int ACTION_LOCATION_UPDATE = 0;
    public static final int ACTION_STOP_RECORDING = 1;
    public static final int ACTION_START_RECORDING = 2;
    public static final int ACTION_ACTIVITY_KILLED = 3;
}
