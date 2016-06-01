package com.marianhello.bgloc.data;

import java.util.Collection;

public interface LocationDAO {
    public Collection<BackgroundLocation> getAllLocations();
    public boolean persistLocation(BackgroundLocation l);
    public void deleteLocation(Integer locationId);
    public void deleteAllLocations();
}
