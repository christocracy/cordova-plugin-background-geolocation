package com.tenforwardconsulting.cordova.bgloc.data.sqlite;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tenforwardconsulting.cordova.bgloc.data.sqlite.LocationContract.LocationEntry;

public class LocationOpenHelper extends SQLiteOpenHelper {
    private static final String SQLITE_DATABASE_NAME = "cordova_bg_geolocation.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
        "CREATE TABLE " + LocationEntry.TABLE_NAME + " (" +
        LocationEntry._ID + " INTEGER PRIMARY KEY," +
        LocationEntry.COLUMN_NAME_TIME + INTEGER_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_ACCURACY + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_SPEED + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_BEARING + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_ALTITUDE + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_LATITUDE + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_LONGITUDE + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_PROVIDER + INTEGER_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_SERVICE_PROVIDER + TEXT_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_DEBUG + INTEGER_TYPE +
        " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME;

    LocationOpenHelper(Context context) {
        super(context, SQLITE_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        Log.d(this.getClass().getName(), SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        Log.d(this.getClass().getName(), SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
