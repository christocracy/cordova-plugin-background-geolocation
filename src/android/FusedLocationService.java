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
import java.util.Random;
import static android.telephony.PhoneStateListener.*;

public class FusedLocationService extends com.tenforwardconsulting.cordova.bgloc.AbstractLocationService implements GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "FusedLocationService";
    private static final String LOCATION_UPDATE = "com.tenforwardconsulting.cordova.bgloc.LOCATION_UPDATE";
    private static final String STOP_RECORDING  = "com.tenforwardconsulting.cordova.bgloc.STOP_RECORDING";
    private static final String START_RECORDING = "com.tenforwardconsulting.cordova.bgloc.START_RECORDING";

    private long lastUpdateTime = 0l;

    private PendingIntent locationUpdatePI;
    private GoogleApiClient locationClientAPI;
    private Criteria criteria;

    private boolean running = false;
    private boolean enabled = false;
    private boolean startRecordingOnConnect = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "OnCreate");
        Log.d(TAG, "RUNNING JOSHUA'S MOD!!!!!!!!!!!!!!!");

        toneGenerator           = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        // Location Update PI
        Intent locationUpdateIntent = new Intent(LOCATION_UPDATE);
        locationUpdatePI = PendingIntent.getBroadcast(this, 9001, locationUpdateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(locationUpdateReceiver, new IntentFilter(LOCATION_UPDATE));

        // Receivers for start/stop recording
        registerReceiver(startRecordingReceiver, new IntentFilter(START_RECORDING));
        registerReceiver(stopRecordingReceiver, new IntentFilter(STOP_RECORDING));

        // Location criteria

        criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(true);
    }

    /**
     * Adds an onclick handler to the notification
     */
    private Notification.Builder setClickEvent (Notification.Builder notification) {
        Context context     = getApplicationContext();
        String packageName  = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = new Random().nextInt();

        PendingIntent contentIntent = PendingIntent.getActivity(context, requestCode, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        return notification.setContentIntent(contentIntent);
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

                // Go ahead and cache, push to server
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

    @TargetApi(16)
    private Notification buildForegroundNotification(Notification.Builder builder) {
        return builder.build();
    }

    @SuppressWarnings("deprecation")
    @TargetApi(15)
    private Notification buildForegroundNotificationCompat(Notification.Builder builder) {
        return builder.getNotification();
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
