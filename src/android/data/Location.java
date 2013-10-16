package com.tenforwardconsulting.cordova.bgloc.data;

import java.util.Date;

import android.os.SystemClock;


public class Location {
	private String latitude;
	private String longitude;
	private Date recordedAt;
	private Long id;
	
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
	
	public static Location fromAndroidLocation(android.location.Location originalLocation) {
		Location location = new Location();
		location.setRecordedAt(new Date(originalLocation.getTime()));
		location.setLongitude(String.valueOf(originalLocation.getLongitude()));
		location.setLatitude(String.valueOf(originalLocation.getLatitude()));
		
		return location;
	}
}
