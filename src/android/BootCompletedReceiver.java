/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.marianhello.cordova.bgloc.Config;
import com.tenforwardconsulting.cordova.bgloc.LocationService;
import com.tenforwardconsulting.cordova.bgloc.data.DAOFactory;
import com.tenforwardconsulting.cordova.bgloc.data.ConfigurationDAO;
/**
 * BootCompletedReceiver class
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
     public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received boot completed");
        ConfigurationDAO dao = DAOFactory.createConfigurationDAO(context);
        Config config = dao.retrieveConfiguration();
        if (config == null) { return; }

        Log.i(TAG, "Config: " + config.toString());

        if (config.getStartOnBoot()) {
            Intent locationServiceIntent = new Intent(context, LocationService.class);
            locationServiceIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            locationServiceIntent.putExtra("config", config);

            context.startService(locationServiceIntent);
        }
     }
}
