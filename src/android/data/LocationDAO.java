package com.tenforwardconsulting.cordova.bgloc.data;

import java.util.Date;
import java.util.Collection;

public interface LocationDAO {
    public Collection<LocationProxy> getAllLocations();
    public boolean persistLocation(LocationProxy l);
    public void deleteLocation(Integer locationId);
    public void deleteAllLocations();
}
