/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.tenforwardconsulting.cordova.bgloc;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.AlarmManager;
import android.support.v4.app.NotificationCompat;
import android.app.Service;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.marianhello.cordova.bgloc.Config;
import com.marianhello.cordova.bgloc.Constant;
import com.marianhello.cordova.bgloc.LocationProviderFactory;
import com.tenforwardconsulting.cordova.bgloc.data.LocationProxy;
import com.tenforwardconsulting.cordova.bgloc.data.LocationDAO;
import com.tenforwardconsulting.cordova.bgloc.data.DAOFactory;

import java.util.Random;


public class LocationService extends Service {
    private static final String TAG = "LocationService";

    private Config config;
    private Boolean isActionReceiverRegistered = false;
    private LocationProvider provider;

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
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        Log.i(TAG, "OnBind" + intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Receiver for actions
        registerActionReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        // config = Config.fromByteArray(intent.getByteArrayExtra("config"));
        if (intent.hasExtra("config")) {
            config = (Config) intent.getParcelableExtra("config");
        } else {
            config = new Config();
        }

        LocationProviderFactory spf = new LocationProviderFactory(this, config);
        provider = spf.getInstance(config.getLocationProvider());
        provider.onCreate();

        if (config.getStartForeground()) {
            // Build a Notification required for running service in foreground.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setContentTitle(config.getNotificationTitle());
            builder.setContentText(config.getNotificationText());
            if (config.getSmallNotificationIcon() != null) {
                builder.setSmallIcon(getPluginResource(config.getSmallNotificationIcon()));
            } else {
                builder.setSmallIcon(android.R.drawable.ic_menu_mylocation);
            }
            if (config.getLargeNotificationIcon() != null) {
                builder.setLargeIcon(BitmapFactory.decodeResource(getApplication().getResources(), getPluginResource(config.getLargeNotificationIcon())));
            }
            if (config.getNotificationIconColor() != null) {
                builder.setColor(this.parseNotificationIconColor(config.getNotificationIconColor()));
            }

            setClickEvent(builder);

            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
            startForeground(startId, notification);
        }

        provider.startRecording();

        //We want this service to continue running until it is explicitly stopped
        return START_REDELIVER_INTENT;
    }

    protected Integer getPluginResource(String resourceName) {
        return getApplication().getResources().getIdentifier(resourceName, "drawable", getApplication().getPackageName());
    }

    /**
     * Adds an onclick handler to the notification
     */
    protected NotificationCompat.Builder setClickEvent (NotificationCompat.Builder builder) {
        int requestCode = new Random().nextInt();
        Context context     = getApplicationContext();
        String packageName  = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestCode, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        return builder.setContentIntent(contentIntent);
    }

    private Integer parseNotificationIconColor(String color) {
        int iconColor = 0;
        if (color != null) {
            try {
                iconColor = Color.parseColor(color);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "couldn't parse color from android options");
            }
        }
        return iconColor;
    }

    public Intent registerActionReceiver () {
        if (isActionReceiverRegistered) { return null; }

        isActionReceiverRegistered = true;
        return registerReceiver(actionReceiver, new IntentFilter(Constant.ACTION_FILTER));
    }

    public void unregisterActionReceiver () {
        if (!isActionReceiverRegistered) { return; }

        unregisterReceiver(actionReceiver);
        isActionReceiverRegistered = false;
    }

    public void startRecording() {
        provider.startRecording();
    }

    public void stopRecording() {
        provider.stopRecording();
    }

    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "Stopping service: " + intent);
        provider.stopRecording();
        if (config.isDebugging()) {
            Toast.makeText(this, "Stopping Location service", Toast.LENGTH_SHORT).show();
        }
        return super.stopService(intent); // not needed???
    }

    /**
     * Forces the main activity to re-launch if it's unloaded.
     */
    private void forceMainActivityReload() {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        startActivity(launchIntent);
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "Destroying Location Service");
        unregisterActionReceiver();
        provider.onDestroy();
        stopForeground(true);
        super.onDestroy();
    }

    // @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task has been removed");
        unregisterActionReceiver();
        if (config.getStopOnTerminate()) {
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }
}
