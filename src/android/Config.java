/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Config class
 */
public class Config implements Parcelable
{
    private float stationaryRadius = 50;
    private Integer distanceFilter = 500;
    private Integer locationTimeout = 60;
    private Integer desiredAccuracy = 100;
    private Boolean debugging = false;
    private String notificationTitle = "Background tracking";
    private String notificationText = "ENABLED";
    private String activityType; //not used
    private Boolean stopOnTerminate = false;
    private String notificationIcon;
    private String notificationIconColor;
    private ServiceProvider serviceProvider = ServiceProvider.ANDROID_DISTANCE_FILTER;
    private Integer interval = 600000; //milliseconds
    private Integer fastestInterval = 120000; //milliseconds
    private Integer activitiesInterval = 1000; //milliseconds

    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(getStationaryRadius());
        out.writeInt(getDistanceFilter());
        out.writeInt(getLocationTimeout());
        out.writeInt(getDesiredAccuracy());
        out.writeValue(isDebugging());
        out.writeString(getNotificationTitle());
        out.writeString(getNotificationText());
        out.writeString(getActivityType());
        out.writeValue(getStopOnTerminate());
        out.writeString(getNotificationIcon());
        out.writeString(getNotificationIconColor());
        out.writeInt(getServiceProvider().asInt());
        out.writeInt(getInterval());
        out.writeInt(getFastestInterval());
        out.writeInt(getActivitiesInterval());
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
        setDistanceFilter(in.readInt());
        setLocationTimeout(in.readInt());
        setDesiredAccuracy(in.readInt());
        setDebugging((Boolean) in.readValue(null));
        setNotificationTitle(in.readString());
        setNotificationText(in.readString());
        setActivityType(in.readString());
        setStopOnTerminate((Boolean) in.readValue(null));
        setNotificationIcon(in.readString());
        setNotificationIconColor(in.readString());
        setServiceProvider(in.readInt());
        setInterval(in.readInt());
        setFastestInterval(in.readInt());
        setActivitiesInterval(in.readInt());
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
        if (!"null".equals(notificationIconColor)) {
            this.notificationIconColor = notificationIconColor;
        }
    }

    public String getNotificationIcon() {
        return notificationIcon;
    }

    public void setNotificationIcon(String notificationIcon) {
        if (!"null".equals(notificationIcon)) {
            this.notificationIcon = notificationIcon;
        }
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

    public ServiceProvider getServiceProvider() {
        return this.serviceProvider;
    }

    public void setServiceProvider(Integer providerId) {
        this.serviceProvider = ServiceProvider.forInt(providerId);
    }

    public void setServiceProvider(ServiceProvider provider) {
        this.serviceProvider = provider;
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

    public Integer getActivitiesInterval() {
        return activitiesInterval;
    }

    public void setActivitiesInterval(Integer activitiesInterval) {
        this.activitiesInterval = activitiesInterval;
    }

    public String getLargeNotificationIcon () {
        String iconName = getNotificationIcon();
        if (iconName != null) {
            iconName = iconName + "_large";
        }
        return iconName;
    }

    public String getSmallNotificationIcon () {
        String iconName = getNotificationIcon();
        if (iconName != null) {
            iconName = iconName + "_small";
        }
        return iconName;
    }

    private void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    private String getActivityType() {
        return activityType;
    }

    @Override
    public String toString () {
        return new StringBuffer()
                .append("stationaryRadius: "       + getStationaryRadius())
                .append(" desiredAccuracy: "       + getDesiredAccuracy())
                .append(" distanceFilter: "        + getDistanceFilter())
                .append(" locationTimeout: "       + getLocationTimeout())
                .append(" debugging: "             + isDebugging())
                .append(" notificationIcon: "      + getNotificationIcon())
                .append(" notificationIconColor: " + getNotificationIconColor())
                .append(" notificationTitle: "     + getNotificationTitle())
                .append(" notificationText: "      + getNotificationText())
                .append(" stopOnTerminate: "       + getStopOnTerminate())
                .append(" serviceProvider: "       + getServiceProvider())
                .append(" interval: "              + getInterval())
                .append(" fastestInterval: "       + getFastestInterval())
                .append(" activitiesInterval: "    + getActivitiesInterval())
                .toString();
    }

    public Parcel toParcel () {
        Parcel parcel = Parcel.obtain();
        this.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return parcel;
    }

    public static Config fromByteArray (byte[] byteArray) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(byteArray, 0, byteArray.length);
        parcel.setDataPosition(0);
        return Config.CREATOR.createFromParcel(parcel);
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
        config.setActivityType(data.getString(7));
        config.setStopOnTerminate(data.getBoolean(8));
        config.setNotificationIcon(data.getString(9));
        config.setNotificationIconColor(data.getString(10));
        config.setServiceProvider(data.getInt(11));
        config.setInterval(data.getInt(12));
        config.setFastestInterval(data.getInt(13));
        config.setActivitiesInterval(data.getInt(14));

        return config;
    }
}
