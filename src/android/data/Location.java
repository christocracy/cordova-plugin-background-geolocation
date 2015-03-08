/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

Differences to original version:

1. To avoid conflicts
package com.tenforwardconsulting.cordova.bgloc
was renamed to com.marianhello.cordova.bgloc

2. location as android Parcel (this could be safely removed as not used anywhere)

3. new toJSONObject method
*/

package com.marianhello.cordova.bgloc.data;

import java.util.Date;
import android.util.Log;
import android.os.SystemClock;
import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONObject;
import org.json.JSONException;

public class Location {
	private String latitude;
	private String longitude;
	private Date recordedAt;
	private String accuracy;
	private String speed;
	private String altitude;
	private String bearing;

	private Long id;
	
    public Location() {
    	
    }
     
    private Location(Parcel in) {
		latitude = in.readString();
		longitude = in.readString();
		recordedAt = new Date(in.readLong());
		accuracy = in.readString();
		speed = in.readString();
		altitude = in.readString();
		bearing = in.readString();
    }	
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	public Date getRecordedAt() {
		return recordedAt;
	}
	public void setRecordedAt(Date recordedAt) {
		this.recordedAt = recordedAt;
	}
	public String getAccuracy() {
		return accuracy;
	}
	public void setAccuracy(String accuracy) {
		this.accuracy = accuracy;
	}
	public String getSpeed() {
		return speed;
	}
	public void setSpeed(String speed) {
		this.speed = speed;
	}
	public String getBearing() {
		return bearing;
	}
	public void setBearing(String bearing) {
		this.bearing = bearing;
	}
	public String getAltitude() {
		return altitude;
	}
	public void setAltitude(String altitude) {
		this.altitude = altitude;
	}

	public JSONObject toJSONObject() throws JSONException {
	    JSONObject json = new JSONObject();
	    json.put("latitude", latitude);
	    json.put("longitude", longitude);
	    // json.put("recordedAt", recordedAt);
	    json.put("accuracy", accuracy);
	    json.put("speed", speed);
	    json.put("altitude", altitude);
	    json.put("bearing", bearing);
		return json;
	}
	
	public static Location fromAndroidLocation(android.location.Location originalLocation) {
		Location location = new Location();
		location.setRecordedAt(new Date(originalLocation.getTime()));
		location.setLongitude(String.valueOf(originalLocation.getLongitude()));
		location.setLatitude(String.valueOf(originalLocation.getLatitude()));
		location.setAccuracy(String.valueOf(originalLocation.getAccuracy()));
		location.setSpeed(String.valueOf(originalLocation.getSpeed()));
		location.setBearing(String.valueOf(originalLocation.getBearing()));
		location.setAltitude(String.valueOf(originalLocation.getAltitude()));
		
		return location;
	}

	public int describeContents() {
         return 0;
    }

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(latitude);
		dest.writeString(longitude);
		dest.writeLong(recordedAt.getTime());
		dest.writeString(accuracy);
		dest.writeString(speed);
		dest.writeString(altitude);
		dest.writeString(bearing);
	}

    public static final Parcelable.Creator<Location> CREATOR
             = new Parcelable.Creator<Location>() {
         public Location createFromParcel(Parcel in) {
             return new Location(in);
         }

         public Location[] newArray(int size) {
             return new Location[size];
         }
    };
}
