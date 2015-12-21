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

import org.json.JSONObject;
import org.json.JSONException;

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
    private String notificationIconLarge;
    private String notificationIconSmall;
    private String notificationIconColor;
    private String activityType; //not used
    private Boolean stopOnTerminate = false;
    private ServiceProviderEnum serviceProvider = ServiceProviderEnum.ANDROID_DISTANCE_FILTER;
    private Integer interval = 600000; //milliseconds
    private Integer fastestInterval = 120000; //milliseconds
    private Integer activitiesInterval = 1000; //milliseconds
    private Boolean startOnBoot = false;

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
        out.writeString(getLargeNotificationIcon());
        out.writeString(getSmallNotificationIcon());
        out.writeString(getNotificationIconColor());
        out.writeString(getActivityType());
        out.writeValue(getStopOnTerminate());
        out.writeValue(getStartOnBoot());
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
        setLargeNotificationIcon(in.readString());
        setSmallNotificationIcon(in.readString());
        setNotificationIconColor(in.readString());
        setActivityType(in.readString());
        setStopOnTerminate((Boolean) in.readValue(null));
        setStartOnBoot((Boolean) in.readValue(null));
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

    public String getLargeNotificationIcon () {
        return notificationIconLarge;
    }

    public void setLargeNotificationIcon (String icon) {
        this.notificationIconLarge = icon;
    }

    public String getSmallNotificationIcon () {
        return notificationIconSmall;
    }

    public void setSmallNotificationIcon (String icon) {
        this.notificationIconSmall = icon;
    }

    public Boolean getStopOnTerminate() {
        return stopOnTerminate;
    }

    public void setStopOnTerminate(Boolean stopOnTerminate) {
        this.stopOnTerminate = stopOnTerminate;
    }

    public Boolean getStartOnBoot() {
        return startOnBoot;
    }

    public void setStartOnBoot(Boolean startOnBoot) {
        this.startOnBoot = startOnBoot;
    }

    public ServiceProviderEnum getServiceProvider() {
        return this.serviceProvider;
    }

    public void setServiceProvider(Integer providerId) {
        this.serviceProvider = ServiceProviderEnum.forInt(providerId);
    }

    public void setServiceProvider(ServiceProviderEnum provider) {
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
                .append(" notificationTitle: "     + getNotificationTitle())
                .append(" notificationText: "      + getNotificationText())
                .append(" notificationIconLarge: " + getLargeNotificationIcon())
                .append(" notificationIconSmall: " + getSmallNotificationIcon())
                .append(" notificationIconColor: " + getNotificationIconColor())
                .append(" startOnBoot: "           + getStartOnBoot())
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
        config.setStartOnBoot(data.getBoolean(9));
        config.setServiceProvider(data.getInt(10));
        config.setInterval(data.getInt(11));
        config.setFastestInterval(data.getInt(12));
        config.setActivitiesInterval(data.getInt(13));
        config.setNotificationIconColor(data.getString(14));
        config.setLargeNotificationIcon(data.getString(15));
        config.setSmallNotificationIcon(data.getString(16));

        return config;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("stationaryRadius", getStationaryRadius());
        json.put("distanceFilter", getDistanceFilter());
        json.put("locationTimeout", getLocationTimeout());
        json.put("desiredAccuracy", getDesiredAccuracy());
        json.put("debugging", isDebugging());
        json.put("notificationTitle", getNotificationTitle());
        json.put("notificationText", getNotificationText());
        json.put("notificationIconLarge", getLargeNotificationIcon());
        json.put("notificationIconSmall", getSmallNotificationIcon());
        json.put("notificationIconColor", getNotificationIconColor());
        json.put("activityType", getActivityType());
        json.put("stopOnTerminate", getStopOnTerminate());
        json.put("startOnBoot", getStartOnBoot());
        json.put("serviceProvider", getServiceProvider());
        json.put("interval", getInterval());
        json.put("fastestInterval", getFastestInterval());
        json.put("activitiesInterval", getActivitiesInterval());

        return json;
  	}
}
