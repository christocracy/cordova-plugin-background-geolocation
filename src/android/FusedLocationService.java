package com.tenforwardconsulting.cordova.bgloc;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import static android.telephony.PhoneStateListener.*;

import com.marianhello.cordova.bgloc.Constant;

public class FusedLocationService extends com.tenforwardconsulting.cordova.bgloc.AbstractLocationService implements GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = "FusedLocationService";

    private PendingIntent locationUpdatePI;
    private GoogleApiClient locationClientAPI;

    private long lastUpdateTime = 0l;
    private boolean running = false;
    private boolean enabled = false;
    private boolean startRecordingOnConnect = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "OnCreate");
        Log.d(TAG, "RUNNING JOSHUA'S MOD!!!!!!!!!!!!!!!");

        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        // Location Update PI
        Intent locationUpdateIntent = new Intent(Constant.LOCATION_UPDATE);
        locationUpdatePI = PendingIntent.getBroadcast(this, 9001, locationUpdateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(locationUpdateReceiver, new IntentFilter(Constant.LOCATION_UPDATE));

        // Receivers for start/stop recording
        registerReceiver(startRecordingReceiver, new IntentFilter(Constant.START_RECORDING));
        registerReceiver(stopRecordingReceiver, new IntentFilter(Constant.STOP_RECORDING));

        startRecording();
    }

    /**
     * Broadcast receiver for receiving a single-update from LocationManager.
     */
    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "- locationUpdateReceiver TRIGGERED!!!!!!!!!!");
            String key = FusedLocationProviderApi.KEY_LOCATION_CHANGED;
            Location location = (Location)intent.getExtras().get(key);

            if (location != null) {
                Log.d(TAG, "- locationUpdateReceiver" + location.toString());

                lastLocation = location;
                broadcastLocation(location);
            }
        }
    };

    private BroadcastReceiver startRecordingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "- START_RECORDING RECEIVER");
            startRecording();
        }
    };

    private BroadcastReceiver stopRecordingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "- STOP_RECORDING RECEIVER");
            stopRecording();
        }
    };

    private void enable() {
        this.enabled = true;
    }

    private void disable() {
        this.enabled = false;
    }

    public void startRecording() {
        Log.d(TAG, "- locationUpdateReceiver STARTING RECORDING!!!!!!!!!!");
        this.startRecordingOnConnect = true;
        attachRecorder();
    }

    public void stopRecording() {
        Log.d(TAG, "- locationUpdateReceiver STOPPING RECORDING!!!!!!!!!!");
        this.startRecordingOnConnect = false;
        detachRecorder();
    }

    private void connectToPlayAPI() {
        Log.d(TAG, "- CONNECTING TO GOOGLE PLAY SERVICES API!!!!!!!!!!");
        locationClientAPI =  new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                //.addOnConnectionFailedListener(this)
                .build();
        locationClientAPI.connect();
    }

    private void attachRecorder() {
        if (locationClientAPI == null) {
            connectToPlayAPI();
        } else if (locationClientAPI.isConnected()) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(translateDesiredAccuracy(config.getDesiredAccuracy())) // this.accuracy
                    .setFastestInterval(config.getFastestInterval())
                    .setInterval(config.getInterval())
                    .setSmallestDisplacement(config.getStationaryRadius());
            LocationServices.FusedLocationApi.requestLocationUpdates(locationClientAPI, locationRequest, locationUpdatePI);
            this.running = true;
            Log.d(TAG, "- locationUpdateReceiver NOW RECORDING!!!!!!!!!!");
        } else {
            locationClientAPI.connect();
        }
    }

    private void detachRecorder() {
        if (locationClientAPI == null) {
            connectToPlayAPI();
        } else if (locationClientAPI.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(locationClientAPI, locationUpdatePI);
            this.running = false;
            Log.d(TAG, "- locationUpdateReceiver NO LONGER RECORDING!!!!!!!!!!");
        } else {
            locationClientAPI.connect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "- CONNECTED TO GOOGLE PLAY SERVICES API!!!!!!!!!!");
        if (this.startRecordingOnConnect) {
            attachRecorder();
        } else {
            detachRecorder();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // locationClientAPI.connect();
    }

    /**
    * Translates a number representing desired accuracy of GeoLocation system from set [0, 10, 100, 1000].
    * 0:  most aggressive, most accurate, worst battery drain
    * 1000:  least aggressive, least accurate, best for battery.
    */
    private Integer translateDesiredAccuracy(Integer accuracy) {
        switch (accuracy) {
            case 10000:
                accuracy = LocationRequest.PRIORITY_NO_POWER;
                break;
            case 1000:
                accuracy = LocationRequest.PRIORITY_LOW_POWER;
                break;
            case 100:
                accuracy = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                break;
            case 10:
                accuracy = LocationRequest.PRIORITY_HIGH_ACCURACY;
                break;
            case 0:
                accuracy = LocationRequest.PRIORITY_HIGH_ACCURACY;
                break;
            default:
                accuracy = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
        }
        return accuracy;
    }

    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "- Received stop: " + intent);
        this.stopRecording();
        this.cleanUp();
        if (config.isDebugging()) {
            Toast.makeText(this, "Background location tracking stopped", Toast.LENGTH_SHORT).show();
        }
        return super.stopService(intent);
    }

    protected void cleanUp() {
        // this.disable();
        toneGenerator.release();
        unregisterReceiver(locationUpdateReceiver);
        unregisterReceiver(startRecordingReceiver);
        unregisterReceiver(stopRecordingReceiver);
        locationClientAPI.disconnect();
        stopForeground(true);
    }

    //@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        this.stopRecording();
        this.stopSelf();
        super.onTaskRemoved(rootIntent);
    }
}
