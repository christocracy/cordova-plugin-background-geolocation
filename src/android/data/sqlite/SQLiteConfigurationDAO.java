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

import com.marianhello.cordova.bgloc.Config;
import com.tenforwardconsulting.cordova.bgloc.data.ConfigurationDAO;
import com.tenforwardconsulting.cordova.bgloc.data.sqlite.ConfigurationContract.ConfigurationEntry;

public class SQLiteConfigurationDAO implements ConfigurationDAO {
  public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
  private static final String TAG = "SQLiteConfigurationDAO";
  private Context context;

  public SQLiteConfigurationDAO(Context context) {
      this.context = context;
  }

  public Config retrieveConfiguration() {
    SQLiteDatabase db = null;
    Cursor cursor = null;

    String[] columns = {
    	ConfigurationEntry._ID,
      ConfigurationEntry.COLUMN_NAME_RADIUS,
      ConfigurationEntry.COLUMN_NAME_DISTANCE_FILTER,
      ConfigurationEntry.COLUMN_NAME_DESIRED_ACCURACY,
      ConfigurationEntry.COLUMN_NAME_DEBUGGING,
      ConfigurationEntry.COLUMN_NAME_NOTIF_TITLE,
      ConfigurationEntry.COLUMN_NAME_NOTIF_TEXT,
      ConfigurationEntry.COLUMN_NAME_NOTIF_ICON_LARGE,
      ConfigurationEntry.COLUMN_NAME_NOTIF_ICON_SMALL,
      ConfigurationEntry.COLUMN_NAME_NOTIF_COLOR,
      ConfigurationEntry.COLUMN_NAME_STOP_TERMINATE,
      ConfigurationEntry.COLUMN_NAME_START_BOOT,
      ConfigurationEntry.COLUMN_NAME_START_FOREGROUND,
      ConfigurationEntry.COLUMN_NAME_SERVICE_PROVIDER,
      ConfigurationEntry.COLUMN_NAME_INTERVAL,
      ConfigurationEntry.COLUMN_NAME_FASTEST_INTERVAL,
      ConfigurationEntry.COLUMN_NAME_ACTIVITIES_INTERVAL
    };

    String whereClause = null;
    String[] whereArgs = null;
    String groupBy = null;
    String having = null;
    String orderBy = null;

    Config config = null;
    try {
      db = new SQLiteOpenHelper(context).getReadableDatabase();
      cursor = db.query(
          ConfigurationEntry.TABLE_NAME,  // The table to query
          columns,                   // The columns to return
          whereClause,               // The columns for the WHERE clause
          whereArgs,                 // The values for the WHERE clause
          groupBy,                   // don't group the rows
          having,                    // don't filter by row groups
          orderBy                    // The sort order
      );
      if (cursor.moveToFirst()) {
        config = hydrate(cursor);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      if (db != null) {
        db.close();
      }
    }
    return config;
  }

  public boolean persistConfiguration(Config config) {
    SQLiteDatabase db = new SQLiteOpenHelper(context).getWritableDatabase();
    db.beginTransaction();
    db.delete(ConfigurationEntry.TABLE_NAME, null, null);
    long rowId = db.insert(ConfigurationEntry.TABLE_NAME, ConfigurationEntry.COLUMN_NAME_NULLABLE, getContentValues(config));
    Log.d(TAG, "After insert, rowId = " + rowId);
    db.setTransactionSuccessful();
    db.endTransaction();
    db.close();
    if (rowId > -1) {
      return true;
    } else {
      return false;
    }
  }

  private Config hydrate(Cursor c) {
    Config config = new Config();
    config.setStationaryRadius(c.getFloat(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_RADIUS)));
    config.setDistanceFilter(c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_DISTANCE_FILTER)));
    config.setDesiredAccuracy(c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_DESIRED_ACCURACY)));
    config.setDebugging( (c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_DEBUGGING)) == 1) ? true : false );
    config.setNotificationTitle(c.getString(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_NOTIF_TITLE)));
    config.setNotificationText(c.getString(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_NOTIF_TEXT)));
    config.setSmallNotificationIcon(c.getString(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_NOTIF_ICON_SMALL)));
    config.setLargeNotificationIcon(c.getString(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_NOTIF_ICON_LARGE)));
    config.setNotificationIconColor(c.getString(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_NOTIF_COLOR)));
    config.setStopOnTerminate( (c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_STOP_TERMINATE)) == 1) ? true : false );
    config.setStartOnBoot( (c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_START_BOOT)) == 1) ? true : false );
    config.setStartForeground( (c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_START_FOREGROUND)) == 1) ? true : false );
    config.setServiceProvider(c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_SERVICE_PROVIDER)));
    config.setInterval(c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_INTERVAL)));
    config.setFastestInterval(c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_FASTEST_INTERVAL)));
    config.setActivitiesInterval(c.getInt(c.getColumnIndex(ConfigurationEntry.COLUMN_NAME_ACTIVITIES_INTERVAL)));

    return config;
  }

  private ContentValues getContentValues(Config config) {
    ContentValues values = new ContentValues();
    values.put(ConfigurationEntry.COLUMN_NAME_RADIUS, config.getStationaryRadius());
    values.put(ConfigurationEntry.COLUMN_NAME_DISTANCE_FILTER, config.getDistanceFilter());
    values.put(ConfigurationEntry.COLUMN_NAME_DESIRED_ACCURACY, config.getDesiredAccuracy());
    values.put(ConfigurationEntry.COLUMN_NAME_DEBUGGING, (config.isDebugging() == true) ? 1 : 0);
    values.put(ConfigurationEntry.COLUMN_NAME_NOTIF_TITLE, config.getNotificationTitle());
    values.put(ConfigurationEntry.COLUMN_NAME_NOTIF_TEXT, config.getNotificationText());
    values.put(ConfigurationEntry.COLUMN_NAME_NOTIF_ICON_SMALL, config.getSmallNotificationIcon());
    values.put(ConfigurationEntry.COLUMN_NAME_NOTIF_ICON_LARGE, config.getLargeNotificationIcon());
    values.put(ConfigurationEntry.COLUMN_NAME_NOTIF_COLOR, config.getNotificationIconColor());
    values.put(ConfigurationEntry.COLUMN_NAME_STOP_TERMINATE, (config.getStopOnTerminate() == true) ? 1 : 0);
    values.put(ConfigurationEntry.COLUMN_NAME_START_BOOT, (config.getStartOnBoot() == true) ? 1 : 0);
    values.put(ConfigurationEntry.COLUMN_NAME_START_FOREGROUND, (config.getStartForeground() == true) ? 1 : 0);
    values.put(ConfigurationEntry.COLUMN_NAME_SERVICE_PROVIDER, config.getServiceProvider().asInt());
    values.put(ConfigurationEntry.COLUMN_NAME_INTERVAL, config.getInterval());
    values.put(ConfigurationEntry.COLUMN_NAME_FASTEST_INTERVAL, config.getFastestInterval());
    values.put(ConfigurationEntry.COLUMN_NAME_ACTIVITIES_INTERVAL, config.getActivitiesInterval());

    return values;
  }
}
