/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.tenforwardconsulting.cordova.bgloc;

import android.annotation.TargetApi;
import android.app.Notification;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.marianhello.cordova.bgloc.Config;
import com.marianhello.cordova.bgloc.Constant;

import org.json.JSONException;

public abstract class AbstractLocationService extends Service {
    private static String TAG;
    private static final String DEFAULT_NOTIFICATION_ICON_COLOR = "#4CAF50";

    protected Config config;
    protected String activity;

    protected Location lastLocation;
    protected ToneGenerator toneGenerator;

    private ConnectivityManager connectivityManager;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        Log.i(TAG, "OnBind" + intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        if (intent != null) {
            config = (Config) intent.getParcelableExtra("config");
            activity = intent.getStringExtra("activity");
            Log.d( TAG, "Got activity" + activity );
            Class<?> activityClass = null;
            try {
                activityClass = Class.forName( activity );
            } catch ( ClassNotFoundException e ) {
                e.printStackTrace();
            }

            // Build a Notification required for running service in foreground.
            Intent main = new Intent(this, BackgroundGpsPlugin.class);
            main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, main,  PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap largeIcon = BitmapFactory.decodeResource(getApplication().getResources(), getPluginResource(config.getLargeNotificationIcon()));

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setContentTitle(config.getNotificationTitle());
            builder.setContentText(config.getNotificationText());
            builder.setSmallIcon(getPluginResource(config.getSmallNotificationIcon()));
            builder.setLargeIcon(largeIcon);
            builder.setColor(this.parseNotificationIconColor(config.getNotificationIconColor()));
            builder.setContentIntent(pendingIntent);
            builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, activityClass), 0));
            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
            startForeground(startId, notification);
        }
        Log.i(TAG, config.toString());
        Log.i(TAG, "- activity: "   + activity);

        //We want this service to continue running until it is explicitly stopped
        return START_REDELIVER_INTENT;
    }

    public Integer getPluginResource(String resourceName) {
        return getApplication().getResources().getIdentifier(resourceName, "drawable", getApplication().getPackageName());
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
        if (iconColor != 0) {
            return iconColor;
        }
        else {
            return Color.parseColor(DEFAULT_NOTIFICATION_ICON_COLOR);
        }
    }

    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "- Received stop: " + intent);
        cleanUp();
        if (config.isDebugging()) {
            Toast.makeText(this, "Background location tracking stopped", Toast.LENGTH_SHORT).show();
        }
        return super.stopService(intent);
    }

    protected abstract void cleanUp();

    /**
     * Plays debug sound
     * @param name
     */
    protected void startTone(String name) {
        int tone = 0;
        int duration = 1000;

        if (name.equals("beep")) {
            tone = ToneGenerator.TONE_PROP_BEEP;
        } else if (name.equals("beep_beep_beep")) {
            tone = ToneGenerator.TONE_CDMA_CONFIRM;
        } else if (name.equals("long_beep")) {
            tone = ToneGenerator.TONE_CDMA_ABBR_ALERT;
        } else if (name.equals("doodly_doo")) {
            tone = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE;
        } else if (name.equals("chirp_chirp_chirp")) {
            tone = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
        } else if (name.equals("dialtone")) {
            tone = ToneGenerator.TONE_SUP_RINGTONE;
        }
        toneGenerator.startTone(tone, duration);
    }

    protected void broadcastLocation (Location location) {
        Log.d(TAG, "Broadcasting update message: " + location.toString());
        try {
            String locStr = com.tenforwardconsulting.cordova.bgloc.LocationConverter.toJSONObject(location).toString();
            Intent intent = new Intent(Constant.FILTER);
            intent.putExtra(Constant.COMMAND, Constant.UPDATE_PROGRESS);
            intent.putExtra(Constant.DATA, locStr);
            this.sendBroadcast(intent);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to broadcast location");
        }
    }

    public boolean isNetworkConnected() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            Log.d(TAG, "Network found, type = " + networkInfo.getTypeName());
            return networkInfo.isConnected();
        } else {
            Log.d(TAG, "No active network info");
            return false;
        }
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "------------------------------------------ Destroyed Location update Service");
        cleanUp();
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        this.stopSelf();
        super.onTaskRemoved(rootIntent);
    }
}
