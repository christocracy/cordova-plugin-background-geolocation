package com.tenforwardconsulting.cordova.bgloc;

import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import com.tenforwardconsulting.cordova.bgloc.data.DAOFactory;
import com.tenforwardconsulting.cordova.bgloc.data.LocationDAO;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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

public class LocationUpdateService extends Service implements LocationListener {
	private static final String TAG = "LocationService";
	public static final int NOTIFICATION_ID = 555;
	private PowerManager.WakeLock wakeLock;
	private Location lastLocation;
	private long lastUpdateTime = 0l;
	private BusyTask looper;
	private String authToken = "HypVBMmDxbh76pHpwots";
	private String url = "http://192.168.2.15:3000/users/current_location.json";
	private Notification notification;
	private NotificationManager notificationManager;
	private LocationManager locationManager;
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
		Log.i(TAG, "OnCreate");
		notificationManager = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1*60*1000, 0, this);
		connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wakeLock.acquire();
		
//		looper = new BusyTask();
//		looper.execute("go");
		//scheudlePostLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        if (intent != null) {
        	this.authToken = intent.getStringExtra("authToken");
	        this.url = intent.getStringExtra("url");
        }
        Toast.makeText(this, "Background location tracking started", Toast.LENGTH_SHORT).show();
        //We want this service to continue running until it is explicitly
        // stopped, so return sticky.
    	notification = buildNotification();
    	notificationManager.notify(NOTIFICATION_ID, notification);

        return START_STICKY;
    }
    
    @Override
    public boolean stopService(Intent intent) {
    	Log.i(TAG, "Received stop: " + intent);
    	cleanUp();
    	Toast.makeText(this, "Background location tracking stopped", Toast.LENGTH_SHORT).show();
    	return super.stopService(intent);
    }
    @Override
    public void onDestroy() {
    	Log.w(TAG, "Destroyed Location update Service");
    	cleanUp();
    	super.onDestroy();
    }
    private void cleanUp() {
    	locationManager.removeUpdates(this);
    	notificationManager.cancel(NOTIFICATION_ID);
    	wakeLock.release();
    	if (looper != null) {
    		looper.stop = true;
    	}
    }
    private Notification buildNotification() {
        PackageManager pm = this.getPackageManager();
        Intent notificationIntent = pm.getLaunchIntentForPackage(this.getPackageName());
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        Application application = this.getApplication();
        int backgroundIconId = 0;
        for (String s: Arrays.asList("ic_launcher", "icon", "notification") ) {
        	backgroundIconId = application.getResources().getIdentifier(s, "drawable", application.getPackageName());
        	if (backgroundIconId != 0) {
        		break;
        	}
        }
         
        int appNameId = application.getResources().getIdentifier("app_name", "string", application.getPackageName());
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
			.setSmallIcon(backgroundIconId)
			.setContentTitle(this.getString(appNameId))
			.setOngoing(true)
			.setContentIntent(contentIntent)
			.setWhen(System.currentTimeMillis());
       if (lastLocation != null) {
    	   builder.setContentText("Last location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
       } else {
    	   builder.setContentText("Tracking your GPS position");
       }
			
        return builder.build();
    }
    
    public void onLocationChanged(Location location) {
    	Log.d(TAG, "Location22Change: "  +location.getLatitude() + ", " + location.getLongitude());
    	lastLocation = location;
    	Log.d(TAG, "Location33Change:");
    	persistLocation(location);
    	
		if (this.isNetworkConnected()) {
			Log.d(TAG, "Scheduling location network post");
			schedulePostLocations();
		} else {
			Log.d(TAG, "Network unavailable, waiting for now");
		}
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
			Log.i(TAG, "Posting	 native location update: " + l);
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost request = new HttpPost(url);
			JSONObject params = new JSONObject();
			params.put("auth_token", authToken);
			
			JSONObject location = new JSONObject();
			location.put("latitude", l.getLatitude());
			location.put("longitude", l.getLongitude());
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
	public void onTaskRemoved(Intent rootIntent) {
		this.stopSelf();
		super.onTaskRemoved(rootIntent);
	}
	
	private class BusyTask extends AsyncTask<String, Integer, Boolean>{
		public boolean stop = false;
		
		@Override
		protected Boolean doInBackground(String...params) {
			while(!stop) {
				Log.d(TAG, "#timestamp " + System.currentTimeMillis());
				if (lastUpdateTime + 5*60*1000 < SystemClock.elapsedRealtime()) {
					Log.d(TAG, "5 minutes, forcing update with last location");
					postLocation(com.tenforwardconsulting.cordova.bgloc.data.Location.fromAndroidLocation(
							locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)));
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}
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
			notification = buildNotification();
			notificationManager.notify(NOTIFICATION_ID, notification);
		}
	}
}
