/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.tenforwardconsulting.cordova.bgloc;

import android.os.HandlerThread;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;
import android.location.Location;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.media.ToneGenerator;
import com.marianhello.cordova.bgloc.Config;
import com.marianhello.cordova.bgloc.Constant;
import com.tenforwardconsulting.cordova.bgloc.data.LocationProxy;
import com.tenforwardconsulting.cordova.bgloc.data.LocationDAO;

import org.json.JSONException;

/**
 * AbstractLocationProvider
 */
public abstract class AbstractLocationProvider implements ServiceProvider {
    private static final String TAG = "AbstractLocationProvider";

    protected LocationDAO dao;
    protected Config config;
    protected Context context;
    protected Location lastLocation;

    protected ToneGenerator toneGenerator;
    protected HandlerThread handlerThread;

    protected AbstractLocationProvider(LocationDAO dao, Config config, Context context) {
        this.dao = dao;
        this.config = config;
        this.context = context.getApplicationContext();
    }

    public void onCreate() {
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

        handlerThread = new HandlerThread("LocationProviderThread");
        handlerThread.start();
    }

    public void onDestroy() {
        toneGenerator.release();
    }

    public Intent registerReceiver (BroadcastReceiver receiver, IntentFilter filter) {
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);

        return context.registerReceiver(receiver, filter, null, handler);
    }

    public void unregisterReceiver (BroadcastReceiver receiver) {
        context.unregisterReceiver(receiver);
    }

    public void persistLocation (Location location) {
        persistLocation(LocationProxy.fromAndroidLocation(location));
    }

    public void handleLocation (Location location) {
        broadcastLocation(location);
    }

    public void persistLocation (LocationProxy location) {
        if (dao.persistLocation(location)) {
            Log.d(TAG, "Persisted Location: " + location.toString());
        } else {
            Log.w(TAG, "Failed to persist location");
        }
    }

    public void broadcastLocation (Location location) {
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
            context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
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
}
