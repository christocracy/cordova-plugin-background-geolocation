package com.tenforwardconsulting.cordova.backgroundgeolocation;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;


public class BackgroundGpsPlugin extends CordovaPlugin {
	public static final String ACTION_START = "start";
	public static final String ACTION_STOP = "stop";
	public static final String ACTION_CONFIGURE = "configure";
	
	private String authToken;
	private String url;
	
	@Override
	public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
		Activity activity = this.cordova.getActivity();
		Intent updateServiceIntent = new Intent(activity, LocationUpdateService.class);
		if (ACTION_START.equalsIgnoreCase(action)) {
			if (authToken == null || url == null) {
				callbackContext.error("Call configure before calling start");
				return false;
			}
			updateServiceIntent.putExtra("authToken", authToken);
			updateServiceIntent.putExtra("url", url);
			activity.startService(updateServiceIntent);
			
		} else if (ACTION_STOP.equalsIgnoreCase(action)) {
			activity.stopService(updateServiceIntent);
		} else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
			try {
				this.authToken = data.getString(0);
				this.url = data.getString(1);
			} catch (JSONException e) {
				callbackContext.error("authToken/url required as parameters: " + e.getMessage());
				return false;
			}
		}
		
		return true; 
	}
}
