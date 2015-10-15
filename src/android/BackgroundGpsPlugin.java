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
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.marianhello.cordova.bgloc.Config;
import com.marianhello.cordova.bgloc.Constant;
import com.marianhello.cordova.bgloc.ServiceProvider;

public class BackgroundGpsPlugin extends CordovaPlugin {
    private static final String TAG = "BackgroundGpsPlugin";

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_SET_CONFIG = "setConfig";
    public static final String ACTION_LOCATION_ENABLED_CHECK = "isLocationEnabled";
    public static final String ACTION_SHOW_LOCATION_SETTINGS = "showLocationSettings";

    private Config config = new Config();
    private Boolean isEnabled = false;
    private Intent updateServiceIntent;
    private CallbackContext callbackContext;

    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        Activity activity = this.cordova.getActivity();
        Context context = activity.getApplicationContext();

        if (ACTION_START.equalsIgnoreCase(action) && !isEnabled) {
            try {
                updateServiceIntent = new Intent(activity, ServiceProvider.getClass(config.getLocationServiceProvider()));
            } catch (ClassNotFoundException e) {
                callbackContext.error("Configuration error: provider not found");
                return false;
            }

            IntentFilter intentFilter = new IntentFilter(Constant.FILTER);
            context.registerReceiver(mMessageReceiver, intentFilter);
            String canonicalName = activity.getClass().getCanonicalName();

            updateServiceIntent.putExtra("config", config);
            updateServiceIntent.putExtra("activity", canonicalName);
            Log.d( TAG, "Put activity " + canonicalName);

            activity.startService(updateServiceIntent);
            isEnabled = true;
            Log.d(TAG, "bg service has been started");

        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            context.unregisterReceiver(mMessageReceiver);
            isEnabled = false;
            activity.stopService(updateServiceIntent);
            callbackContext.success();
            Log.d(TAG, "bg service has been stopped");
        } else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
            try {
                this.callbackContext = callbackContext;
                this.config = Config.fromJSONArray(data);
                Log.d(TAG, "bg service configured");
            } catch (JSONException e) {
                callbackContext.error("Configuration error: " + e.getMessage());
                return false;
            }
        } else if (ACTION_SET_CONFIG.equalsIgnoreCase(action)) {
            // TODO reconfigure Service
            callbackContext.success();
            Log.d(TAG, "bg service reconfigured");
        } else if (ACTION_LOCATION_ENABLED_CHECK.equalsIgnoreCase(action)) {
            Log.d(TAG, "location services enabled check");
            try {
                int isLocationEnabled = BackgroundGpsPlugin.isLocationEnabled(context) ? 1 : 0;
                callbackContext.success(isLocationEnabled);
            } catch (SettingNotFoundException e) {
                callbackContext.error("Location setting not found on this platform");
                return false;
            }
        } else if (ACTION_SHOW_LOCATION_SETTINGS.equalsIgnoreCase(action)) {
            showLocationSettings();
        }

        return true;
    }

    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    public void onDestroy() {
        Activity activity = this.cordova.getActivity();

        if (isEnabled && config.getStopOnTerminate()) {
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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received location from bg service");
            handleMessage(intent);
        }
    };

    private void handleMessage(Intent msg) {
        Bundle data = msg.getExtras();
        switch (data.getInt(Constant.COMMAND, 0))
        {
            case Constant.UPDATE_PROGRESS:
                try {
                    JSONObject location = new JSONObject(data.getString(Constant.DATA));
                    PluginResult result = new PluginResult(PluginResult.Status.OK, location);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                    Log.d(TAG, "Sending plugin result");
                } catch (JSONException e) {
                    Log.w(TAG, "Error converting message to json");
                }
                break;
            default:
                break;
        }
    }
}
