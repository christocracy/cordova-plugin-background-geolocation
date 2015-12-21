package com.tenforwardconsulting.cordova.bgloc.data;

import java.util.Date;
import java.util.Collection;

import com.marianhello.cordova.bgloc.Config;

public interface ConfigurationDAO {
    public boolean persistConfiguration(Config config);
    public Config retrieveConfiguration();
}
