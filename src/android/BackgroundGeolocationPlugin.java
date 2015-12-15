/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

Differences to original version:

1. new methods isLocationEnabled, mMessageReciever, handleMessage
*/

package com.tenforwardconsulting.cordova.bgloc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.location.LocationManager;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.marianhello.cordova.bgloc.Config;
import com.marianhello.cordova.bgloc.Constant;
import com.marianhello.cordova.bgloc.ServiceProvider;
import com.tenforwardconsulting.cordova.bgloc.data.LocationDAO;
import com.tenforwardconsulting.cordova.bgloc.data.DAOFactory;
import com.tenforwardconsulting.cordova.bgloc.data.LocationProxy;

import java.util.Collection;

public class BackgroundGeolocationPlugin extends CordovaPlugin {
    private static final String TAG = "BackgroundGeolocationPlugin";

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_SET_CONFIG = "setConfig";
    public static final String ACTION_LOCATION_ENABLED_CHECK = "isLocationEnabled";
    public static final String ACTION_SHOW_LOCATION_SETTINGS = "showLocationSettings";
    public static final String REGISTER_MODE_CHANGED_RECEIVER = "watchLocationMode";
    public static final String UNREGISTER_MODE_CHANGED_RECEIVER = "stopWatchingLocationMode";
    public static final String ACTION_GET_ALL_LOCATIONS = "getLocations";
    public static final String ACTION_DELETE_LOCATION = "deleteLocation";
    public static final String ACTION_DELETE_ALL_LOCATIONS = "deleteAllLocations";

    private Config config = new Config();
    private Boolean isEnabled = false;
    private Intent updateServiceIntent;
    private CallbackContext callbackContext;
    private CallbackContext locationModeChangeCallbackContext;

    private BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received location from bg service");
            Bundle results = getResultExtras(true);
            Bundle data = intent.getExtras();
            switch (data.getInt(Constant.ACTION)) {
                case Constant.ACTION_LOCATION_UPDATE:
                    try {
                        Log.d(TAG, "Sending location update");
                        JSONObject location = new JSONObject(data.getString(Constant.DATA));
                        PluginResult result = new PluginResult(PluginResult.Status.OK, location);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    } catch (JSONException e) {
                        Log.w(TAG, "Error converting message to json");
                        PluginResult result = new PluginResult(PluginResult.Status.JSON_EXCEPTION);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }

                    results.putString(Constant.LOCATION_SENT_INDICATOR, "OK");
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver locationModeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received MODE_CHANGED_ACTION action");
            if (locationModeChangeCallbackContext != null) {
                PluginResult result;
                try {
                    int isLocationEnabled = BackgroundGeolocationPlugin.isLocationEnabled(context) ? 1 : 0;
                    result = new PluginResult(PluginResult.Status.OK, isLocationEnabled);
                    result.setKeepCallback(true);
                } catch (SettingNotFoundException e) {
                    result = new PluginResult(PluginResult.Status.ERROR, "Location setting error occured");
                }
                locationModeChangeCallbackContext.sendPluginResult(result);
            }
        }
    };

    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        Activity activity = this.cordova.getActivity();
        Context context = activity.getApplicationContext();

        if (ACTION_START.equals(action) && !isEnabled) {
            Class serviceProviderClass = null;

            try {
                serviceProviderClass = ServiceProvider.getClass(config.getServiceProvider());
            } catch (ClassNotFoundException e) {
                callbackContext.error("Configuration error: provider not found");
                return true;
            }

            updateServiceIntent = new Intent(activity, serviceProviderClass);
            updateServiceIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);

            context.registerReceiver(actionReceiver, new IntentFilter(Constant.ACTION_FILTER));
            String canonicalName = activity.getClass().getCanonicalName();

            updateServiceIntent.putExtra("config", config);
            updateServiceIntent.putExtra("activity", canonicalName);
            Log.d( TAG, "Put activity " + canonicalName);

            activity.startService(updateServiceIntent);
            // TODO: call success/fail callback

            isEnabled = true;
            Log.d(TAG, "bg service has been started");

            return true;
        } else if (ACTION_STOP.equals(action)) {
            context.unregisterReceiver(actionReceiver);
            isEnabled = false;
            activity.stopService(updateServiceIntent);
            callbackContext.success();
            Log.d(TAG, "bg service has been stopped");

            return true;
        } else if (ACTION_CONFIGURE.equals(action)) {
            try {
                this.callbackContext = callbackContext;
                this.config = Config.fromJSONArray(data);
                Log.d(TAG, "bg service configured");
                // callbackContext.success(); //we cannot do this
            } catch (JSONException e) {
                callbackContext.error("Configuration error: " + e.getMessage());
            }

            return true;
        } else if (ACTION_SET_CONFIG.equals(action)) {
            // TODO reconfigure Service
            callbackContext.success();
            Log.d(TAG, "bg service reconfigured");

            return true;
        } else if (ACTION_LOCATION_ENABLED_CHECK.equals(action)) {
            Log.d(TAG, "location services enabled check");
            try {
                int isLocationEnabled = BackgroundGeolocationPlugin.isLocationEnabled(context) ? 1 : 0;
                callbackContext.success(isLocationEnabled);
            } catch (SettingNotFoundException e) {
                callbackContext.error("Location setting error occured");
            }

            return true;
        } else if (ACTION_SHOW_LOCATION_SETTINGS.equals(action)) {
            showLocationSettings();
            // TODO: call success/fail callback

            return true;
        } else if (REGISTER_MODE_CHANGED_RECEIVER.equals(action)) {
            this.locationModeChangeCallbackContext = callbackContext;
            context.registerReceiver(locationModeChangeReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
            // TODO: call success/fail callback

            return true;
        } else if (UNREGISTER_MODE_CHANGED_RECEIVER.equals(action)) {
            context.unregisterReceiver(locationModeChangeReceiver);
            this.locationModeChangeCallbackContext = null;
            // TODO: call success/fail callback

            return true;
        } else if (ACTION_GET_ALL_LOCATIONS.equals(action)) {
            try {
                callbackContext.success(this.getAllLocations());
            } catch (JSONException e) {
                callbackContext.error("Converting locations to JSON failed.");
            }

            return true;
        } else if (ACTION_DELETE_LOCATION.equals(action)) {
            try {
                this.deleteLocation(data.getInt(0));
                callbackContext.success();
            } catch (JSONException e) {
                callbackContext.error("Configuration error: " + e.getMessage());
            }

            return true;
        } else if (ACTION_DELETE_ALL_LOCATIONS.equals(action)) {
            this.deleteAllLocations();
            callbackContext.success();

            return true;
        }

        return false;
    }

    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
     @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Main Activity destroyed!!!");
        Activity activity = this.cordova.getActivity();

        if (isEnabled && config.getStopOnTerminate()) {
            Log.d(TAG, "Stopping bg service");
            activity.stopService(updateServiceIntent);
        }
    }

    public void showLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        cordova.getActivity().startActivity(settingsIntent);
    }

    public static boolean isLocationEnabled(Context context) throws SettingNotFoundException {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    public JSONArray getAllLocations() throws JSONException {
        JSONArray jsonLocationsArray = new JSONArray();
        Context context = this.cordova.getActivity().getApplicationContext();
        LocationDAO dao = DAOFactory.createLocationDAO(context);
        Collection<LocationProxy> locations = dao.getAllLocations();
        for (LocationProxy location : locations) {
            jsonLocationsArray.put(location.toJSONObject());
        }
        return jsonLocationsArray;
    }

    public void deleteLocation(Integer locationId) {
        Context context = this.cordova.getActivity().getApplicationContext();
        LocationDAO dao = DAOFactory.createLocationDAO(context);
        dao.deleteLocation(locationId);
    }

    public void deleteAllLocations() {
        Context context = this.cordova.getActivity().getApplicationContext();
        LocationDAO dao = DAOFactory.createLocationDAO(context);
        dao.deleteAllLocations();
    }
}
