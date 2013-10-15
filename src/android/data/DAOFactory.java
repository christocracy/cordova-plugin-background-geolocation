package com.tenforwardconsulting.cordova.backgroundgeolocation.data;

import com.tenforwardconsulting.cordova.backgroundgeolocation.data.sqlite.SQLiteLocationDAO;

public abstract class DAOFactory {
	public static LocationDAO createLocationDAO() {
		//Very basic for now
		return new SQLiteLocationDAO();
	}
}
