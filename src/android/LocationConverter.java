/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

Differences to original version:

1. new toJSONObject method
*/

package com.tenforwardconsulting.cordova.bgloc;

import java.util.Date;
import android.util.Log;
import android.os.SystemClock;
import org.json.JSONObject;
import org.json.JSONException;

public class LocationConverter {

	public static JSONObject toJSONObject(android.location.Location location) throws JSONException {
		JSONObject json = new JSONObject();
		json.put("time", location.getTime());
		json.put("latitude", location.getLatitude());
		json.put("longitude", location.getLongitude());
		json.put("accuracy", location.getAccuracy());
		json.put("speed", location.getSpeed());
		json.put("altitude", location.getAltitude());
		json.put("bearing", location.getBearing());
		return json;
	}
}
