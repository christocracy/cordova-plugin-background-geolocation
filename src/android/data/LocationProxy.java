package com.tenforwardconsulting.cordova.bgloc.data;

import android.location.Location;
import org.json.JSONObject;
import org.json.JSONException;

import com.marianhello.cordova.bgloc.ServiceProvider;

public class LocationProxy {
    private Location location;
    private Long locationId;
    private Boolean debug = false;
    private ServiceProvider serviceProvider;

    public LocationProxy (String provider) {
        location = new Location(provider);
    }

    public LocationProxy (Location location) {
        this.location = location;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public double getLatitude() {
        return location.getLatitude();
    }

    public void setLatitude(double latitude) {
        location.setLatitude(latitude);
    }

    public long getTime() {
        return location.getTime();
    }

    public void setTime(long time) {
        location.setTime(time);
    }

    public double getLongitude() {
        return location.getLongitude();
    }

    public void setLongitude(double longitude) {
        location.setLongitude(longitude);
    }

    public float getAccuracy() {
        return location.getAccuracy();
    }

    public void setAccuracy(float accuracy) {
        location.setAccuracy(accuracy);
    }

    public float getSpeed() {
        return location.getSpeed();
    }

    public void setSpeed(float speed) {
        location.setSpeed(speed);
    }

    public float getBearing() {
        return location.getBearing();
    }

    public void setBearing(float bearing) {
        location.setBearing(bearing);
    }

    public double getAltitude() {
        return location.getAltitude();
    }

    public void setAltitude(double altitude) {
        location.setAltitude(altitude);
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public String getProvider() {
        return location.getProvider();
    }

    public void setProvider(String provider) {
        location.setProvider(provider);
    }

    public void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public void setServiceProvider(Integer providerId) {
        this.serviceProvider = ServiceProvider.forInt(providerId);
    }

    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    public static LocationProxy fromAndroidLocation(Location location) {
        return new LocationProxy(location);
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("locationId", getLocationId());
        json.put("time", getTime());
        json.put("latitude", getLatitude());
        json.put("longitude", getLongitude());
        json.put("accuracy", getAccuracy());
        json.put("speed", getSpeed());
        json.put("altitude", getAltitude());
        json.put("bearing", getBearing());
        json.put("serviceProvider", getServiceProvider());
        json.put("debug", getDebug());

        return json;
  	}
}
