/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

Differences to original version:

1. To avoid conflicts
package com.tenforwardconsulting.cordova.bgloc
was renamed to com.marianhello.cordova.bgloc
*/

package com.marianhello.cordova.bgloc.data.sqlite;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LocationOpenHelper extends SQLiteOpenHelper {
    private static final String SQLITE_DATABASE_NAME = "cordova_bg_locations";
    private static final int DATABASE_VERSION = 1;
    public static final String LOCATION_TABLE_NAME = "location";
    private static final String LOCATION_TABLE_COLUMNS = 
        " id INTEGER PRIMARY KEY AUTOINCREMENT," +
        " recordedAt TEXT," +
        " accuracy TEXT," +
        " speed TEXT," +
        " bearing TEXT," +
        " altitude TEXT," +
        " latitude TEXT," +
        " longitude TEXT";
    private static final String LOCATION_TABLE_CREATE =
        "CREATE TABLE " + LOCATION_TABLE_NAME + " (" +
        LOCATION_TABLE_COLUMNS +
        ");";

    LocationOpenHelper(Context context) {
        super(context, SQLITE_DATABASE_NAME, null, DATABASE_VERSION);
    }    

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(LOCATION_TABLE_CREATE);
        Log.d(this.getClass().getName(), LOCATION_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }
}