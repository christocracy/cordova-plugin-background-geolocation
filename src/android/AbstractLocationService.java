/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.tenforwardconsulting.cordova.bgloc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.AlarmManager;
import android.support.v4.app.NotificationCompat;
import android.app.Service;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.marianhello.cordova.bgloc.Config;
import com.marianhello.cordova.bgloc.Constant;
import com.marianhello.cordova.bgloc.ServiceProvider;
import com.tenforwardconsulting.cordova.bgloc.data.LocationProxy;
import com.tenforwardconsulting.cordova.bgloc.data.LocationDAO;
import com.tenforwardconsulting.cordova.bgloc.data.DAOFactory;

import java.util.Random;
import org.json.JSONException;

public abstract class AbstractLocationService extends Service {
    private static final String TAG = "AbstractLocationService";

    protected Config config;
    private Boolean isActionReceiverRegistered = false;

    protected Location lastLocation;
    protected ToneGenerator toneGenerator;

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
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

        // Receiver for actions
        registerActionReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        if (intent != null) {
            // config = Config.fromByteArray(intent.getByteArrayExtra("config"));
            config = (Config) intent.getParcelableExtra("config");
            Log.i(TAG, "Config: " + config.toString());

            // Build a Notification required for running service in foreground.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setContentTitle(config.getNotificationTitle());
            builder.setContentText(config.getNotificationText());
            builder.setSmallIcon(android.R.drawable.ic_menu_mylocation);
            if (config.getNotificationIcon() != null) {
                builder.setSmallIcon(getPluginResource(config.getSmallNotificationIcon()));
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

        //We want this service to continue running until it is explicitly stopped
        return START_REDELIVER_INTENT;
    }

    public Integer getPluginResource(String resourceName) {
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

    protected void persistLocation (LocationProxy location) {
        LocationDAO dao = DAOFactory.createLocationDAO(this.getApplicationContext());

        if (dao.persistLocation(location)) {
            Log.d(TAG, "Persisted Location: " + location);
        } else {
            Log.w(TAG, "Failed to persist location");
        }
    }

    protected void handleLocation (Location location) {
        final LocationProxy bgLocation = LocationProxy.fromAndroidLocation(location);
        bgLocation.setServiceProvider(config.getServiceProvider());

        if (config.isDebugging()) {
            bgLocation.setDebug(true);
            persistLocation(bgLocation);
        }

        Log.d(TAG, "Broadcasting update message: " + bgLocation.toString());
        try {
            String locStr = bgLocation.toJSONObject().toString();
            Intent intent = new Intent(Constant.ACTION_FILTER);
            intent.putExtra(Constant.ACTION, Constant.ACTION_LOCATION_UPDATE);
            intent.putExtra(Constant.DATA, locStr);
            this.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
                // @SuppressLint("NewApi")
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Final Result Receiver");
                    Bundle results = getResultExtras(true);
                    if (results.getString(Constant.LOCATION_SENT_INDICATOR) == null) {
                        Log.w(TAG, "Main activity seems to be killed");
                        if (config.getStopOnTerminate() == false) {
                            bgLocation.setDebug(false);
                            persistLocation(bgLocation);
                            Log.d(TAG, "Persisting location. Reason: Main activity was killed.");
                        }
                    }
              }
            }, null, Activity.RESULT_OK, null, null);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to broadcast location");
        }
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

    public void startDelayed () {
        Class serviceProviderClass = null;
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        try {
            Intent serviceIntent = new Intent(this, ServiceProvider.getClass(config.getServiceProvider()));
            serviceIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            serviceIntent.putExtra("config", config.toParcel().marshall());
            PendingIntent pintent = PendingIntent.getService(this, 0, serviceIntent, 0);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 1000, pintent);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Service restart failed");
        }
    }

    protected abstract void cleanUp();

    protected abstract void startRecording();

    protected abstract void stopRecording();


    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "- Received stop: " + intent);
        cleanUp();
        if (config.isDebugging()) {
            Toast.makeText(this, "Background location tracking stopped", Toast.LENGTH_SHORT).show();
        }
        return super.stopService(intent); // not needed???
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "Destroyed Location update Service");
        toneGenerator.release();
        unregisterActionReceiver();
        cleanUp();
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
