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

import java.util.Date;

public interface LocationDAO {
    public Location[] getAllLocations();
    public boolean persistLocation(Location l);
    public void deleteLocation(Location l);
    public String dateToString(Date date);
}
