/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

Differences to original version:

1. To avoid conflicts
package com.tenforwardconsulting.cordova.bgloc
was renamed to com.marianhello.cordova.bgloc
*/

package com.marianhello.cordova.bgloc.data;

import android.content.Context;

import com.marianhello.cordova.bgloc.data.sqlite.SQLiteLocationDAO;

public abstract class DAOFactory {
	public static LocationDAO createLocationDAO(Context context) {
		//Very basic for now
		return new SQLiteLocationDAO(context);
	}
}
