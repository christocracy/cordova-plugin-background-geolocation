BackgroundGeoLocation
==============================

Cross-platform background geolocation for Cordova / PhoneGap.

Follows the [Cordova Plugin spec](https://github.com/apache/cordova-plugman/blob/master/plugin_spec.md), so that it works with [Plugman](https://github.com/apache/cordova-plugman).

This plugin leverages Cordova/PhoneGap's [require/define functionality used for plugins](http://simonmacdonald.blogspot.ca/2012/08/so-you-wanna-write-phonegap-200-android.html). 

## Using the plugin ##
The plugin creates the object `window.plugins.backgroundGeoLocation` with the methods 

  `configure(success, fail, option)`, 

  `start(success, fail)`

  `stop(success, fail)`. 

## Installing the plugin ##

1. Download the repo using GIT or just a ZIP from Github.
2. Add the plugin to your project (from the root of your project):

```
   phonegap plugin add https://github.com/christocracy/cordova-plugin-background-geolocation.git
```

A full example could be:
```
    //
    //
    // after deviceready
    //
    //

    // Your app must execute AT LEAST ONE call for the current position via standard Cordova geolocation,
    //  in order to prompt the user for Location permission.
    window.navigator.geolocation.getCurrentPosition(function(location) {
        console.log('Location from Phonegap');
    });

    var bgGeo = window.plugins.backgroundGeoLocation;

    /**
    * This would be your own callback for Ajax-requests after POSTing background geolocation to your server.
    */
    var yourAjaxCallback = function(response) {
        ////
        // IMPORTANT:  You must execute the #finish method here to inform the native plugin that you're finished,
        //  and the background-task may be completed.  You must do this regardless if your HTTP request is successful or not.
        // IF YOU DON'T, ios will CRASH YOUR APP for spending too much time in the background.
        //
        //
        bgGeo.finish();
    };

    /**
    * This callback will be executed every time a geolocation is recorded in the background.
    */
    var callbackFn = function(location) {
        console.log('[js] BackgroundGeoLocation callback:  ' + location.latitudue + ',' + location.longitude);
        // Do your HTTP request here to POST location to your server.
        //
        //
        yourAjaxCallback.call(this);
    };

    var failureFn = function(error) {
        console.log('BackgroundGeoLocation error');
    }
    
    // BackgroundGeoLocation is highly configurable.
    bgGeo.configure(callbackFn, failureFn, {
        desiredAccuracy: 10,
        stationaryRadius: 20,
        distanceFilter: 30,
        debug: true // <-- enable this hear sounds for background-geolocation life-cycle.
    });

    // Turn ON the background-geolocation system.  The user will be tracked whenever they suspend the app.
    bgGeo.start();

    // If you wish to turn OFF background-tracking, call the #stop method.
    // bgGeo.stop()


```

NOTE: The plugin includes `org.apache.cordova.geolocation` as a dependency.  You must enable Cordova's GeoLocation in the foreground and have the user accept Location services by executing `#watchPosition` or `#getCurrentPosition`.

## iOS

Unfortunately, this plugin is really geared towards use with iOS;  The Android implementation is only very basic.  The iOS implementation of background geolocation uses [CLLocationManager#startMonitoringSignificantLocationChanges](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instm/CLLocationManager/startMonitoringSignificantLocationChanges)

When the app is suspended, the native plugin initiates [region-monitoring](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLRegion_class/Reference/Reference.html#//apple_ref/doc/c_ref/CLRegion), creating a circular-region of radius *#stationaryRadius* meters.  Once the monitored-region signals that the user has gone beyond this region, the native-plugin will initiate aggressive location-monitoring
using [standard location services](https://developer.apple.com/library/mac/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html).  At this time, *#distanceFilter* is in effect, recording a location each time the user travels that distance.

Both #distanceFilter and #stationaryRadius can be modified at run-time.  For example, a #distanceFilter of 50m works great for walking-speed, but is probably too low for a car at highway-speed (too many samples).  In the future, the native app could possibly intelligently monitor speed and vary #distanceFilter automatically.  For now, you must control this manually.

With aggressive location-monitoring enabled, if the user stops for exactly 15 minutes, iOS will automatically send a signal to the native-plugin which will turn-off standard location services and once again begin region-monitoring (#stationaryRadius) using the iOS significant-changes api.

### iOS Config

Use the following config-parameters with the #configure method:

  *`@param {Integer} [0, 10, 100, 1000] desiredAccuracy in meters`*

The lower the number, the more power devoted to GeoLocation resulting in higher accuracy readings.  1000 results in lowest power drain and least accurate readings.  @see [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/desiredAccuracy)

  `@param {Integer} distanceFilter`

  The minimum distance (measured in meters) a device must move horizontally before an update event is generated.  @see [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/distanceFilter)

  `@param {Integer} stationaryRadius (meters)`

When stopped, the minimum distance the device must move beyond the stationary location for aggressive background-tracking to engage.  Note, since the plugin uses iOS significant-changes API, the plugin cannot detect the exact moment the device moves out of the stationary-radius.  In normal conditions, it can take as much as 3 city-blocks to 1/2 km before staionary-region exit is detected.

  `@param {Boolean} debug`

When enabled, the plugin will emit sounds for life-cycle events of background-geolocation.  NOTE:  In addition, you must manually enable the *Audio and Airplay* background mode in *Background Capabilities* to hear these debugging sounds.

- Exit stationary region:  Calendar event notification sound
- GeoLocation recorded:  SMS sent sound
- Aggressive geolocation engaged:  SIRI listening sound
- Passive geolocation engaged:  SIRI stop listening sound
- Acquiring stationary location sound:  "tick" sound
- Stationary location acquired sound:  "bloom" sound

![Alt text](/enable-background-audio.png "Optional title")

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
