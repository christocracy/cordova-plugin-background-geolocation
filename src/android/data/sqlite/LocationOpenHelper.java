package com.tenforwardconsulting.cordova.backgroundgeolocation;


import com.geocrowd.android.core.GeoCrowdApplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LocationOpenHelper extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 1;
    public static final String LOCATION_TABLE_NAME = "location";
    private static final String LOCATION_TABLE_COLUMNS = 
		" id INTEGER PRIMARY KEY AUTOINCREMENT," +
        " recordedAt TEXT," +
        " latitude TEXT," +
        " longitude TEXT";
    private static final String LOCATION_TABLE_CREATE =
	    "CREATE TABLE " + LOCATION_TABLE_NAME + " (" +
	    LOCATION_TABLE_COLUMNS +
	    ");";

    LocationOpenHelper(Context context) {
        super(context, GeoCrowdApplication.SQLITE_DATABASE_NAME, null, DATABASE_VERSION);
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