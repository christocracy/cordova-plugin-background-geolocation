package com.tenforwardconsulting.cordova.bgloc.data.sqlite;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.List;
import java.util.Collection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.tenforwardconsulting.cordova.bgloc.data.LocationDAO;
import com.tenforwardconsulting.cordova.bgloc.data.LocationProxy;
import com.tenforwardconsulting.cordova.bgloc.data.sqlite.LocationContract.LocationEntry;

public class SQLiteLocationDAO implements LocationDAO {
  public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
  private static final String TAG = "SQLiteLocationDAO";
  private Context context;

  public SQLiteLocationDAO(Context context) {
    this.context = context;
  }

  public Collection<LocationProxy> getAllLocations() {
    SQLiteDatabase db = null;
    Cursor cursor = null;

    String[] columns = {
    	LocationEntry._ID,
      LocationEntry.COLUMN_NAME_TIME,
      LocationEntry.COLUMN_NAME_ACCURACY,
      LocationEntry.COLUMN_NAME_SPEED,
      LocationEntry.COLUMN_NAME_BEARING,
      LocationEntry.COLUMN_NAME_ALTITUDE,
      LocationEntry.COLUMN_NAME_LATITUDE,
      LocationEntry.COLUMN_NAME_LONGITUDE,
      LocationEntry.COLUMN_NAME_PROVIDER,
      LocationEntry.COLUMN_NAME_SERVICE_PROVIDER,
      LocationEntry.COLUMN_NAME_DEBUG
    };

    String selection = null;
    String[] selectionArgs = null;
    String groupBy = null;
    String having = null;

    String orderBy =
        LocationEntry.COLUMN_NAME_TIME + " ASC";

    Collection<LocationProxy> all = new ArrayList<LocationProxy>();
    try {
      db = new LocationOpenHelper(context).getReadableDatabase();
      cursor = db.query(
          LocationEntry.TABLE_NAME,  // The table to query
          columns,                   // The columns to return
          selection,                 // The columns for the WHERE clause
          selectionArgs,             // The values for the WHERE clause
          groupBy,                   // don't group the rows
          having,                    // don't filter by row groups
          orderBy                    // The sort order
      );
      cursor.moveToFirst();
      while (cursor.moveToNext()) {
        all.add(hydrate(cursor));
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      if (db != null) {
        db.close();
      }
    }
    return all;
  }

  public boolean persistLocation(LocationProxy location) {
    SQLiteDatabase db = new LocationOpenHelper(context).getWritableDatabase();
    db.beginTransaction();
    ContentValues values = getContentValues(location);
    long rowId = db.insert(LocationEntry.TABLE_NAME, LocationEntry.COLUMN_NAME_NULLABLE, values);
    Log.d(TAG, "After insert, rowId = " + rowId);
    db.setTransactionSuccessful();
    db.endTransaction();
    db.close();
    if (rowId > -1) {
      // location.setId(rowId);
      return true;
    } else {
      return false;
    }
  }

  public void deleteLocation(Integer locationId) {
    String selection = LocationEntry._ID + " = ?";
    String[] selectionArgs = { String.valueOf(locationId) };
    SQLiteDatabase db = new LocationOpenHelper(context).getWritableDatabase();
    db.beginTransaction();
    db.delete(LocationEntry.TABLE_NAME, selection, selectionArgs);
    db.setTransactionSuccessful();
    db.endTransaction();
    db.close();
  }

  public void deleteAllLocations() {
    SQLiteDatabase db = new LocationOpenHelper(context).getWritableDatabase();
    db.beginTransaction();
    db.execSQL("DELETE FROM " + LocationEntry.TABLE_NAME);
    db.setTransactionSuccessful();
    db.endTransaction();
    db.close();
  }

  private LocationProxy hydrate(Cursor c) {
    LocationProxy l = new LocationProxy(c.getString(c.getColumnIndex(LocationEntry.COLUMN_NAME_PROVIDER)));
    l.setLocationId(c.getLong(c.getColumnIndex(LocationEntry._ID)));
    l.setTime(c.getLong(c.getColumnIndex(LocationEntry.COLUMN_NAME_TIME)));
    l.setAccuracy(c.getFloat(c.getColumnIndex(LocationEntry.COLUMN_NAME_ACCURACY)));
    l.setSpeed(c.getFloat(c.getColumnIndex(LocationEntry.COLUMN_NAME_SPEED)));
    l.setBearing(c.getFloat(c.getColumnIndex(LocationEntry.COLUMN_NAME_BEARING)));
    l.setAltitude(c.getDouble(c.getColumnIndex(LocationEntry.COLUMN_NAME_ALTITUDE)));
    l.setLatitude(c.getDouble(c.getColumnIndex(LocationEntry.COLUMN_NAME_LATITUDE)));
    l.setLongitude(c.getDouble(c.getColumnIndex(LocationEntry.COLUMN_NAME_LONGITUDE)));
    l.setServiceProvider(c.getInt(c.getColumnIndex(LocationEntry.COLUMN_NAME_SERVICE_PROVIDER)));
    l.setDebug( (c.getInt(c.getColumnIndex(LocationEntry.COLUMN_NAME_DEBUG)) == 1) ? true : false);

    return l;
  }

  private ContentValues getContentValues(LocationProxy location) {
    ContentValues values = new ContentValues();
    values.put(LocationEntry.COLUMN_NAME_TIME, location.getTime());
    values.put(LocationEntry.COLUMN_NAME_ACCURACY, location.getAccuracy());
    values.put(LocationEntry.COLUMN_NAME_SPEED, location.getSpeed());
    values.put(LocationEntry.COLUMN_NAME_BEARING, location.getBearing());
    values.put(LocationEntry.COLUMN_NAME_ALTITUDE, location.getAltitude());
    values.put(LocationEntry.COLUMN_NAME_LATITUDE, location.getLatitude());
    values.put(LocationEntry.COLUMN_NAME_LONGITUDE, location.getLongitude());
    values.put(LocationEntry.COLUMN_NAME_PROVIDER, location.getProvider());
    values.put(LocationEntry.COLUMN_NAME_SERVICE_PROVIDER, location.getServiceProvider().asInt());
    values.put(LocationEntry.COLUMN_NAME_DEBUG, (location.getDebug() == true) ? 1 : 0);

    return values;
  }
}
