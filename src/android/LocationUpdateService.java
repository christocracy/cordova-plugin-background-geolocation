package com.tenforwardconsulting.cordova.bgloc;

import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import com.tenforwardconsulting.cordova.bgloc.data.DAOFactory;
import com.tenforwardconsulting.cordova.bgloc.data.LocationDAO;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.CellLocation;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.location.Location;
import android.location.Criteria;

import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import static android.telephony.PhoneStateListener.*;
import static java.lang.Math.*;

public class LocationUpdateService extends Service implements LocationListener {
    private static final String TAG = "LocationUpdateService";
    private static final String STATIONARY_REGION_ACTION  = "com.tenforwardconsulting.cordova.bgloc.STATIONARY_REGION_ACTION";
    private static final String STATIONARY_ALARM_ACTION  = "com.tenforwardconsulting.cordova.bgloc.STATIONARY_ALARM_ACTION";
    private static final String SINGLE_LOCATION_UPDATE_ACTION   = "com.tenforwardconsulting.cordova.bgloc.SINGLE_LOCATION_UPDATE_ACTION";
    private static long STATIONARY_TIMEOUT = 60 * 1000 * 1;
    private static final Integer MAX_STATIONARY_ACQUISITION_ATTEMPTS = 5;
    
    private PowerManager.WakeLock wakeLock;
    private Location lastLocation;
    private long lastUpdateTime = 0l;

    private String authToken = "HypVBMmDxbh76pHpwots";
    private String url = "http://192.168.2.15:3000/users/current_location.json";

    private float stationaryRadius;
    private Location stationaryLocation;
    private PendingIntent stationaryAlarmPI;
    private PendingIntent singleUpdatePI;
    private Integer stationaryLocationAttempts = 0;
    private Boolean isAcquiringStationaryLocation = false;
    
    private Integer desiredAccuracy;
    private Integer distanceFilter;
    private Integer scaledDistanceFilter;
    private Integer locationTimeout;
    private Boolean isDebugging;

    private ToneGenerator toneGenerator;

    private PendingIntent proximityPI;

    private LocationManager locationManager;
    private AlarmManager alarmManager;
    private ConnectivityManager connectivityManager;

    private Criteria criteria;

    private Boolean isMoving = false;

    public static TelephonyManager telephonyManager = null;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        Log.i(TAG, "OnBind" + intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "OnCreate");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        alarmManager    = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        
        // Stationary region PendingIntent
        stationaryAlarmPI = PendingIntent.getBroadcast(this, 0, new Intent(STATIONARY_ALARM_ACTION), 0);
        registerReceiver(stationaryAlarmReceiver, new IntentFilter(STATIONARY_ALARM_ACTION));
        
        // Construct the Pending Intent that will be broadcast by the oneshot
        // location update.  
        singleUpdatePI = PendingIntent.getBroadcast(this, 0, new Intent(SINGLE_LOCATION_UPDATE_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(singleUpdateReceiver, new IntentFilter(SINGLE_LOCATION_UPDATE_ACTION));
        
        connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();

        criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        if (intent != null) {
            authToken = intent.getStringExtra("authToken");
            url = intent.getStringExtra("url");
            stationaryRadius = Float.parseFloat(intent.getStringExtra("stationaryRadius"));
            distanceFilter = Integer.parseInt(intent.getStringExtra("distanceFilter"));
            scaledDistanceFilter = distanceFilter;
            desiredAccuracy = Integer.parseInt(intent.getStringExtra("desiredAccuracy"));
            locationTimeout = Integer.parseInt(intent.getStringExtra("locationTimeout"));
            isDebugging = Boolean.parseBoolean(intent.getStringExtra("isDebugging"));
            
            Log.i(TAG, "- url: " + url);
            Log.i(TAG, "- token: " + authToken);
            Log.i(TAG, "- stationaryRadius: "   + stationaryRadius);
            Log.i(TAG, "- distanceFilter: "     + distanceFilter);
            Log.i(TAG, "- desiredAccuracy: "    + desiredAccuracy);
            Log.i(TAG, "- locationTimeout: "    + locationTimeout);
            Log.i(TAG, "- isDebugging: "        + isDebugging);
        }

        this.setPace(false);

        /**
         * Experimental cell-location-change handler
         *
         telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
         telephonyManager.listen(phoneStateListener, LISTEN_CELL_LOCATION);
         *
         */

        //We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    public void onCellLocationChanged(CellLocation cellLocation) {
        Log.i(TAG, "- onCellLocationChanged");
        Location location = getLastBestLocation((int) stationaryRadius, locationTimeout * 1000);
        if (location != null) {
            Log.i(TAG, "location: " + location.getLatitude() + "," + location.getLongitude() + ", accuracy: " + location.getAccuracy());
        }
    }
    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "Received stop: " + intent);
        cleanUp();
        Toast.makeText(this, "Background location tracking stopped", Toast.LENGTH_SHORT).show();
        return super.stopService(intent);
    }

    private Integer translateDesiredAccuracy(Integer accuracy) {
        switch (accuracy) {
            case 1000:
                accuracy = Criteria.ACCURACY_LOW;
                break;
            case 100:
                accuracy = Criteria.ACCURACY_MEDIUM;
                break;
            case 10:
                accuracy = Criteria.ACCURACY_MEDIUM;
                break;
            case 0:
                accuracy = Criteria.ACCURACY_HIGH;
                break;
            default:
                accuracy = Criteria.ACCURACY_MEDIUM;
        }
        return accuracy;
    }

    private void setPace(Boolean value) {
        Log.i(TAG, "setPace: " + value);
        isMoving = value;

        locationManager.removeUpdates(this);

        if (isMoving) {
            stationaryLocation = null;
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setHorizontalAccuracy(translateDesiredAccuracy(desiredAccuracy));
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), locationTimeout*1000, scaledDistanceFilter, this);

            resetStationaryAlarm();
        } else {
            stationaryLocation = null;
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_LOW);
            criteria.setPowerRequirement(Criteria.POWER_LOW);

            Location location = this.getLastBestLocation((int) stationaryRadius, locationTimeout * 1000);
            if (location != null) {
                this.startMonitoringStationaryRegion(location);
            } else {
                isAcquiringStationaryLocation = true;
                stationaryLocationAttempts = 0;
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setHorizontalAccuracy(translateDesiredAccuracy(desiredAccuracy));
                criteria.setPowerRequirement(Criteria.POWER_HIGH);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
            }
        }
    }

    public void resetStationaryAlarm() {
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + STATIONARY_TIMEOUT, stationaryAlarmPI); // Millisec * Second * Minute
    }

    /**
     * Returns the most accurate and timely previously detected location.
     * Where the last result is beyond the specified maximum distance or
     * latency a one-off location update is returned via the {@link LocationListener}
     * specified in {@link setChangedLocationListener}.
     * @param minDistance Minimum distance before we require a location update.
     * @param minTime Minimum time required between location updates.
     * @return The most accurate and / or timely previously detected location.
     */
    public Location getLastBestLocation(int minDistance, long minTime) {
        Log.i(TAG, "- fetching last best location");
        Location bestResult = null;
        float bestAccuracy = Float.MAX_VALUE;
        long bestTime = Long.MIN_VALUE;

        // Iterate through all the providers on the system, keeping
        // note of the most accurate result within the acceptable time limit.
        // If no result is found within maxTime, return the newest Location.
        List<String> matchingProviders = locationManager.getAllProviders();
        for (String provider: matchingProviders) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if ((time > minTime && accuracy < bestAccuracy)) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                }
                else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
                    bestResult = location;
                    bestTime = time;
                }
            }
        }
        // If the best result is beyond the allowed time limit, or the accuracy of the
        // best result is wider than the acceptable maximum distance, request a single update.
        // This check simply implements the same conditions we set when requesting regular
        // location updates every [minTime] and [minDistance]. 
        bestAccuracy = 1000;
        
        if ((bestTime < minTime || bestAccuracy > minDistance)) {
            bestResult = null;
        }
        return bestResult;
    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "- onLocationChanged: " + location.getLatitude() + "," + location.getLongitude() + ", accuracy: " + location.getAccuracy() + ", isMoving: " + isMoving + ", speed: " + location.getSpeed());

        if (isDebugging) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
        if (isMoving) {
            // If user hasn't moved beyond the stationaryRadius within time of STATIONARY_TIMEOUT
            //  assume they've stopped.
            if (lastLocation != null) {
                if (lastLocation.distanceTo(location) > stationaryRadius) {
                    resetStationaryAlarm();
                    Integer newDistanceFilter = calculateDistanceFilter(location.getSpeed());
                    if (newDistanceFilter != scaledDistanceFilter) {
                        Log.i(TAG, "- updated distanceFilter, new: " + newDistanceFilter + ", old: " + scaledDistanceFilter);
                        scaledDistanceFilter = newDistanceFilter;
                        setPace(true);
                    }
                }
            }
        }
        lastLocation = location;
        Toast.makeText(this, "mv:"+isMoving+",acy:"+location.getAccuracy()+",v:"+location.getSpeed()+",df:"+scaledDistanceFilter, Toast.LENGTH_LONG).show();
        
        if (isAcquiringStationaryLocation) {
            if (isBestStationaryLocation(location)) {
                locationManager.removeUpdates(this);
                startMonitoringStationaryRegion(stationaryLocation);
            } else {
                return;
            }
        }
        
        persistLocation(location);

        if (this.isNetworkConnected()) {
            Log.d(TAG, "Scheduling location network post");
            schedulePostLocations();
        } else {
            Log.d(TAG, "Network unavailable, waiting for now");
        }
    }
    
    private Boolean isBestStationaryLocation(Location location) {
        stationaryLocationAttempts++;
        if (stationaryLocationAttempts == MAX_STATIONARY_ACQUISITION_ATTEMPTS) {
            return true;
        }
        if (stationaryLocation == null || stationaryLocation.getAccuracy() > location.getAccuracy()) {
            // store the location as the "best effort"
            stationaryLocation = location;
            if (location.getAccuracy() <= stationaryRadius) {
                return true;
            }
        }
        return false;
    }

    
    private Integer calculateDistanceFilter(Float speed) {
        Double newDistanceFilter = (double) distanceFilter;
        if (speed > 3 && speed < 100) {
            float roundedDistanceFilter = (round(speed / 5) * 5);
            newDistanceFilter = pow(roundedDistanceFilter, 2) + (double) distanceFilter;
        }
        return (newDistanceFilter.intValue() < 1000) ? newDistanceFilter.intValue() : 1000;
    }


    private void startMonitoringStationaryRegion(Location location) {
        Log.i(TAG, "- startMonitoringStationaryRegion (" + location.getLatitude() + "," + location.getLongitude() + "), accuracy:" + location.getAccuracy());
        stationaryLocation = location;
        isAcquiringStationaryLocation = false;

        if (isDebugging) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT);
        }

        if (proximityPI != null) {
            locationManager.removeProximityAlert(proximityPI);
            proximityPI = null;
        }
        Intent intent = new Intent(STATIONARY_REGION_ACTION);
        proximityPI = PendingIntent.getBroadcast(this, 0, intent, 0);

        locationManager.addProximityAlert(
                location.getLatitude(),
                location.getLongitude(),
                (location.getAccuracy() < stationaryRadius) ? stationaryRadius : location.getAccuracy(),
                -1,
                proximityPI
        );

        IntentFilter filter = new IntentFilter(STATIONARY_REGION_ACTION);
        registerReceiver(stationaryRegionReceiver, filter);
    }
    
    private BroadcastReceiver singleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(singleUpdateReceiver);
            String key = LocationManager.KEY_LOCATION_CHANGED;
            Location location = (Location)intent.getExtras().get(key);
            if (location != null)
                onLocationChanged(location);
            locationManager.removeUpdates(singleUpdatePI);
        }
    };
    
    private BroadcastReceiver stationaryAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Put here YOUR code.
            Log.i(TAG, "- stationaryAlarm fired");
            setPace(false);
        }
    };

    private BroadcastReceiver stationaryRegionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "stationaryRegionReceiver");
            String key = LocationManager.KEY_PROXIMITY_ENTERING;

            Boolean entering = intent.getBooleanExtra(key, false);
            if (entering) {
                Log.d(TAG, "- ENTER");
                if (isMoving) {
                    setPace(false);
                }
            }
            else {
                Log.d(TAG, "- EXIT");
                onExitStationaryRegion();
            }
        }
    };

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCellLocationChanged(CellLocation location)
        {
            onCellLocationChanged(location);
        }
    };

    public void onExitStationaryRegion() {
        if (isDebugging) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_CONFIRM);
        }
        if (proximityPI != null) {
            Log.i(TAG, "- proximityPI: " + proximityPI.toString());
            locationManager.removeProximityAlert(proximityPI);
            proximityPI = null;
        }
        this.setPace(true);
    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    private void schedulePostLocations() {
        PostLocationTask task = new LocationUpdateService.PostLocationTask();
        Log.d(TAG, "beforeexecute " +  task.getStatus());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task.execute();
        Log.d(TAG, "afterexecute " +  task.getStatus());
    }

    private boolean postLocation(com.tenforwardconsulting.cordova.bgloc.data.Location l) {
        if (l == null) {
            Log.w(TAG, "postLocation: null location");
            return false;
        }
        try {
            lastUpdateTime = SystemClock.elapsedRealtime();
            Log.i(TAG, "Posting  native location update: " + l);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost request = new HttpPost(url);
            JSONObject params = new JSONObject();
            params.put("auth_token", authToken);

            JSONObject location = new JSONObject();
            location.put("latitude", l.getLatitude());
            location.put("longitude", l.getLongitude());
            location.put("accuracy", l.getAccuracy());
            location.put("speed", l.getSpeed());
            location.put("recorded_at", l.getRecordedAt());
            params.put("location", location);


            StringEntity se = new StringEntity(params.toString());
            request.setEntity(se);
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            Log.d(TAG, "Posting to " + request.getURI().toString());
            HttpResponse response = httpClient.execute(request);
            Log.i(TAG, "Response received: " + response.getStatusLine());
            if (response.getStatusLine().getStatusCode() == 200) {
                return true;
            } else {
                return false;
            }
        } catch (Throwable e) {
            Log.w(TAG, "Exception posting location: " + e);
            e.printStackTrace();
            return false;
        }
    }
    private void persistLocation(Location location) {
        LocationDAO dao = DAOFactory.createLocationDAO(this.getApplicationContext());
        com.tenforwardconsulting.cordova.bgloc.data.Location savedLocation = com.tenforwardconsulting.cordova.bgloc.data.Location.fromAndroidLocation(location);

        if (dao.persistLocation(savedLocation)) {
            Log.d(TAG, "Persisted Location: " + savedLocation);
        } else {
            Log.w(TAG, "Failed to persist location");
        }
    }

    private boolean isNetworkConnected() {
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
    private void cleanUp() {
        locationManager.removeUpdates(this);

        // Stationary-region proximity-detector.
        if (proximityPI != null) {
            locationManager.removeProximityAlert(proximityPI);
            proximityPI = null;
        }
        wakeLock.release();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        this.stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    private class PostLocationTask extends AsyncTask<Object, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Object...objects) {
            Log.d(TAG, "Executing PostLocationTask#doInBackground");
            LocationDAO locationDAO = DAOFactory.createLocationDAO(LocationUpdateService.this.getApplicationContext());
            for (com.tenforwardconsulting.cordova.bgloc.data.Location savedLocation : locationDAO.getAllLocations()) {
                Log.d(TAG, "Posting saved location");
                if (postLocation(savedLocation)) {
                    locationDAO.deleteLocation(savedLocation);
                }
            }
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "PostLocationTask#onPostExecture");
        }
    }
}
