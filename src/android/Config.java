/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

import org.json.JSONArray;
import org.json.JSONException;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Config class
 */
public class Config implements Parcelable
{
    private static final Integer SECONDS_PER_MINUTE      = 60;
    private static final Integer MILLISECONDS_PER_SECOND = 60;

    private float stationaryRadius = 30;
    private Integer desiredAccuracy = 100;
    private Integer distanceFilter = 30;
    private Integer locationTimeout = 60;
    private Boolean debugging = false;
    private String notificationIconColor = "#4CAF50";
    private String notificationIcon  = "notification_icon";
    private String notificationTitle = "Background tracking";
    private String notificationText = "ENABLED";
    private Boolean stopOnTerminate = false;
    private Integer interval = SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND * 5;
    private Integer fastestInterval = SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;
    private Integer locationService = 0;

    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(getStationaryRadius());
        out.writeInt(getDesiredAccuracy());
        out.writeInt(getDistanceFilter());
        out.writeInt(getLocationTimeout());
        out.writeValue(isDebugging());
        out.writeString(getNotificationIconColor());
        out.writeString(getNotificationIcon());
        out.writeString(getNotificationTitle());
        out.writeString(getNotificationText());
        out.writeValue(getStopOnTerminate());
        out.writeInt(getInterval());
        out.writeInt(getFastestInterval());
    }

    public static final Parcelable.Creator<Config> CREATOR
            = new Parcelable.Creator<Config>() {
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public Config () {

    }

    private Config(Parcel in) {
        setStationaryRadius(in.readFloat());
        setDesiredAccuracy(in.readInt());
        setDistanceFilter(in.readInt());
        setLocationTimeout(in.readInt());
        setDebugging((Boolean) in.readValue(null));
        setNotificationIconColor(in.readString());
        setNotificationIcon(in.readString());
        setNotificationTitle(in.readString());
        setNotificationText(in.readString());
        setStopOnTerminate((Boolean) in.readValue(null));
        setInterval(in.readInt());
        setFastestInterval(in.readInt());
    }


    public static Config fromJSONArray (JSONArray data) throws JSONException {
        Config config = new Config();
        config.setStationaryRadius((float) data.getDouble(0));
        config.setDistanceFilter(data.getInt(1));
        config.setLocationTimeout(data.getInt(2));
        config.setDesiredAccuracy(data.getInt(3));
        config.setDebugging(data.getBoolean(4));
        config.setNotificationTitle(data.getString(5));
        config.setNotificationText(data.getString(6));
        config.setStopOnTerminate(data.getBoolean(8));
        config.setNotificationIcon(data.getString(9));
        config.setNotificationIconColor(data.getString(10));
        config.setLocationService(data.getInt(11));
        config.setInterval(data.getInt(12));
        config.setFastestInterval(data.getInt(13));

        return config;
    }

    public float getStationaryRadius() {
        return stationaryRadius;
    }

    public void setStationaryRadius(float stationaryRadius) {
        this.stationaryRadius = stationaryRadius;
    }

    public Integer getDesiredAccuracy() {
        return desiredAccuracy;
    }

    public void setDesiredAccuracy(Integer desiredAccuracy) {
        this.desiredAccuracy = desiredAccuracy;
    }

    public Integer getDistanceFilter() {
        return distanceFilter;
    }

    public void setDistanceFilter(Integer distanceFilter) {
        this.distanceFilter = distanceFilter;
    }

    public Integer getLocationTimeout() {
        return locationTimeout;
    }

    public void setLocationTimeout(Integer locationTimeout) {
        this.locationTimeout = locationTimeout;
    }

    public Boolean isDebugging() {
        return debugging;
    }

    public void setDebugging(Boolean debugging) {
        this.debugging = debugging;
    }

    public String getNotificationIconColor() {
        return notificationIconColor;
    }

    public void setNotificationIconColor(String notificationIconColor) {
        this.notificationIconColor = notificationIconColor;
    }

    public String getNotificationIcon() {
        return notificationIcon;
    }

    public void setNotificationIcon(String notificationIcon) {
        this.notificationIcon = notificationIcon;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String getNotificationText() {
        return notificationText;
    }

    public void setNotificationText(String notificationText) {
        this.notificationText = notificationText;
    }

    public Boolean getStopOnTerminate() {
        return stopOnTerminate;
    }

    public void setStopOnTerminate(Boolean stopOnTerminate) {
        this.stopOnTerminate = stopOnTerminate;
    }

    // TODO: refactor as enum
    public String getLocationService() {
        switch (locationService) {
            case 0:
                return "com.tenforwardconsulting.cordova.bgloc.DistanceFilterLocationService";
            case 1:
                return "com.tenforwardconsulting.cordova.bgloc.FusedLocationService";
            default:
                return "com.tenforwardconsulting.cordova.bgloc.DistanceFilterLocationService";
        }
    }

    public void setLocationService(Integer locationService) {
        this.locationService = locationService;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getFastestInterval() {
        return fastestInterval;
    }

    public void setFastestInterval(Integer fastestInterval) {
        this.fastestInterval = fastestInterval;
    }

    public String getLargeNotificationIcon () {
        return this.getNotificationIcon() + "_large";
    }

    public String getSmallNotificationIcon () {
        return this.getNotificationIcon() + "_small";
    }

    @Override
    public String toString () {
        return new StringBuffer()
                .append("- stationaryRadius: "     + getStationaryRadius())
                .append("- desiredAccuracy: "      + getDesiredAccuracy())
                .append("- distanceFilter: "       + getDistanceFilter())
                .append("- locationTimeout: "      + getLocationTimeout())
                .append("- isDebugging: "          + isDebugging())
                .append("- notificationIcon: "     + getNotificationIcon())
                .append("- notificationTitle: "    + getNotificationTitle())
                .append("- notificationText: "     + getNotificationText())
                .append("- stopOnTerminate: "      + getStopOnTerminate())
                .append("- locationService: "      + getLocationService())
                .append("- interval: "             + getInterval())
                .append("- fastestInterval: "      + getFastestInterval())
                .toString();
    }
}
