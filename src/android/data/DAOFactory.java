package com.marianhello.cordova.bgloc.data;

import android.content.Context;

import com.marianhello.cordova.bgloc.data.sqlite.SQLiteLocationDAO;
import com.marianhello.cordova.bgloc.data.sqlite.SQLiteConfigurationDAO;

public abstract class DAOFactory {
    public static LocationDAO createLocationDAO(Context context) {
        return new SQLiteLocationDAO(context);
    }

    public static ConfigurationDAO createConfigurationDAO(Context context) {
        return new SQLiteConfigurationDAO(context);
    }
}
