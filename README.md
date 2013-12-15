BackgroundGeoLocation
==============================

Cross-platform background geolocation for Cordova / PhoneGap.

Follows the [Cordova Plugin spec](https://github.com/apache/cordova-plugman/blob/master/plugin_spec.md), so that it works with [Plugman](https://github.com/apache/cordova-plugman).

This plugin leverages Cordova/PhoneGap's [require/define functionality used for plugins](http://simonmacdonald.blogspot.ca/2012/08/so-you-wanna-write-phonegap-200-android.html). 

## Using the plugin ##
The plugin creates the object `window.plugins.backgroundGeoLocation` with the methods `configure(success, fail, option)`, `start(success, fail)` and `stop(success, fail). 

## Installing the plugin ##

1. Download the repo using GIT or just a ZIP from Github.
2. Add the plugin to your project (from the root of your project):

```
   phonegap plugin add https://github.com/christocracy/cordova-plugin-background-geolocation.git
```

A full example could be:
```
    var bgGeo = window.plugins.backgroundGeoLocation;

    var callback = function(location) {
      // HTTP to your server.
      $.post({
        url: 'locations.json',
        callback: function() {
          // Must inform native plugin the task is complete so it can terminate background-thread and go back to sleep.
          bgGeo.finish();
        }
      })
    };

    bgGeo.configure(callback, failFn, {
      stationaryRadius: 50,  // meters
      distanceFilter: 50     // meters
    });

    // Enable background geolocation
    bgGeo.start();

    // Disable it
    bgGeo.stop();

```

## iOS

The iOS implementation of background geolocation uses [CLLocationManager#startMonitoringSignificantLocationChanges](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instm/CLLocationManager/startMonitoringSignificantLocationChanges)

When the app is suspended, the native plugin initiates [region-monitoring](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLRegion_class/Reference/Reference.html#//apple_ref/doc/c_ref/CLRegion), creating a circular-region of radius *#stationaryRadius* meters.  Once the monitored-region signals that the user has gone beyond this region, the native-plugin will initiate aggressive location-monitoring
using [standard location services](https://developer.apple.com/library/mac/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html).  At this time, *#distanceFilter* is in effect, recording a location each time the user travels that distance.

Both #distanceFilter and #stationaryRadius can be modified at run-time.  For example, a #distanceFilter of 50m works great for walking-speed, but is probably too low for a car at highway-speed (too many samples).  In the future, the native app could possibly intelligently monitor speed and vary #distanceFilter automatically.  For now, you must control this manually.

```
  
  bgGeo.setStationaryRadius(100);
  bgGeo.setDistanceFilter(500);

```

With aggressive location-monitoring enabled, if the user stops for exactly 15 minutes, iOS will automatically send a signal to the native-plugin which will turn-off standard location services and once again begin region-monitoring (#stationaryRadius) using the iOS significant-changes api.

## Android

** TODO Brian ##

## Licence ##

The MIT License

Copyright (c) 2013 Chris Scott and Brian Samson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
