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
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import static android.telephony.PhoneStateListener.*;

import com.marianhello.cordova.bgloc.Constant;

public class FusedLocationService extends AbstractLocationService implements GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "FusedLocationService";

    private PowerManager.WakeLock wakeLock;

    private GoogleApiClient locationClientAPI;

    private long lastUpdateTime = 0l;
    private boolean running = false;
    private boolean enabled = false;
    private boolean startRecordingOnConnect = true;

    private BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getExtras();
            switch (data.getInt(Constant.ACTION)) {
                case Constant.ACTION_START_RECORDING:
                    startRecording();
                    break;
                case Constant.ACTION_STOP_RECORDING:
                    stopRecording();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "OnCreate");
        Log.d(TAG, "RUNNING JOSHUA'S MOD!!!!!!!!!!!!!!!");

        // Receiver for actions
        registerReceiver(actionReceiver, new IntentFilter(Constant.ACTION_FILTER));

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        wakeLock.acquire();

        startRecording();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "- onLocationChanged" + location.toString());

        if (config.isDebugging()) {
            Toast.makeText(FusedLocationService.this, "acy:" + location.getAccuracy() + ",v:" + location.getSpeed() + ",df:" + config.getDistanceFilter(), Toast.LENGTH_LONG).show();
        }

        // if (lastLocation != null && location.distanceTo(lastLocation) < config.getDistanceFilter()) {
        //     return;
        // }

        if (config.isDebugging()) {
            startTone("beep");
        }

        lastLocation = location;
        handleLocation(location);
    }

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
            Integer priority = translateDesiredAccuracy(config.getDesiredAccuracy());
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(priority) // this.accuracy
                    .setFastestInterval(config.getFastestInterval())
                    .setInterval(config.getInterval());
                    // .setSmallestDisplacement(config.getStationaryRadius());
            LocationServices.FusedLocationApi.requestLocationUpdates(locationClientAPI, locationRequest, this);
            this.running = true;
            Log.d(TAG, "- locationUpdateReceiver NOW RECORDING!!!!!!!!!! with priority: "
                + priority + ", fastestInterval: " + config.getFastestInterval() + ", interval: " + config.getInterval() + ", smallestDisplacement: " + config.getStationaryRadius());
        } else {
            locationClientAPI.connect();
        }
    }

    private void detachRecorder() {
        if (locationClientAPI == null) {
            connectToPlayAPI();
        } else if (locationClientAPI.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(locationClientAPI, this);
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
        Log.i(TAG, "connection to Google Play Services suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "connection to Google Play Services failed");
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
        unregisterReceiver(actionReceiver);
        locationClientAPI.disconnect();
        stopForeground(true);
        wakeLock.release();
    }

    //@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        this.stopRecording();
        this.stopSelf();
        super.onTaskRemoved(rootIntent);
    }
}
