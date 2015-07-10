package com.zencity.cordova.bgloc.data;

import android.content.Context;

import com.zencity.cordova.bgloc.data.sqlite.SQLiteLocationDAO;

public abstract class DAOFactory {
	public static LocationDAO createLocationDAO(Context context) {
		//Very basic for now
		return new SQLiteLocationDAO(context);
	}
}
