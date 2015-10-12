cordova-plugin-mauron85-background-geolocation
==============================

Fork notice
==============================

This is fork of [christocracy cordova-backgroud-geolocation](https://github.com/christocracy/cordova-plugin-background-geolocation). The main change is in Android version. Posting positions to url was replaced by callbacks, so now it works same as in iOS. It was possible be using intents. Also it can (and should) be used as battery and data efficient **foreground** geolocation provider.

Warning: You probably have to set your cordova app to keep running by keepRunning property to true (this is the default now).

Description
==============================

Cross-platform background geolocation for Cordova / PhoneGap with battery-saving "circular region monitoring" and "stop detection".

Plugin is both foreground and background geolocation provider. It is far more battery and data efficient comparing to html5 geolocation or cordova-geolocation plugin. But you can still use it together with other geolocation providers (eg. html5 navigator.geolocation).

## Installing the plugin ##

As Cordova is [shifting towards npm](http://cordova.apache.org/announcements/2015/04/21/plugins-release-and-move-to-npm.html), this plugin can be installed from npm:

```
cordova plugin add cordova-plugin-mauron85-background-geolocation
```

## Registering plugin for Adobe® PhoneGap™ Build

[Adobe® PhoneGap™ Build](http://build.phonegap.com) supports plugins from npm as well. To register plugin add following line into your config.xml. If you're using *hydration*, you have to download and reinstall your app with every new version of the plugin, as plugins are not updated.

```
<gap:plugin name="cordova-plugin-mauron85-background-geolocation" source="npm"/>
```

## Using the plugin ##
The plugin creates the object `window.plugins.backgroundGeoLocation` with the methods

* `configure(success, fail, option)`
* `start(success, fail)`
* `stop(success, fail)`

A full example could be:
```
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady () {
    var bgGeo = window.plugins.backgroundGeoLocation;

    /**
    * This callback will be executed every time a geolocation is recorded in the background.
    */
    var callbackFn = function(location) {
        console.log('[js] BackgroundGeoLocation callback:  ' + location.latitude + ',' + location.longitude);

        // Do your HTTP request here to POST location to your server.
        // jQuery.post(url, JSON.stringify(location));

        /*
        IMPORTANT:  You must execute the #finish method here to inform the native plugin that you're finished, and the background-task may be completed.  You must do this regardless if your HTTP request is successful or not.
        IF YOU DON'T, ios will CRASH YOUR APP for spending too much time in the background.
        */
        bgGeo.finish();
    };

    var failureFn = function(error) {
        console.log('BackgroundGeoLocation error');
    };

    // BackgroundGeoLocation is highly configurable. See platform specific configuration options
    bgGeo.configure(callbackFn, failureFn, {
        desiredAccuracy: 10,
        stationaryRadius: 20,
        distanceFilter: 30,
        debug: true, // <-- enable this hear sounds for background-geolocation life-cycle.
        stopOnTerminate: false, // <-- enable this to clear background location settings when the app terminates
    });

    // Turn ON the background-geolocation system.  The user will be tracked whenever they suspend the app.
    bgGeo.start();

    // If you wish to turn OFF background-tracking, call the #stop method.
    // bgGeo.stop();
}
```

NOTE: On some platforms is required to enable Cordova's GeoLocation in the foreground and have the user accept Location services by executing `#watchPosition` or `#getCurrentPosition`. Not needed on Android.

## Example Application

This plugin hosts a SampleApp in [example/SampleApp](/example/SampleApp) folder. SampleApp can be also used to improve plugin in the future. Read instructions in [README.md](/example/SampleApp/README.md).

## Behaviour

The plugin has features allowing you to control the behaviour of background-tracking, striking a balance between accuracy and battery-usage.  In stationary-mode, the plugin attempts to descrease its power usage and accuracy by setting up a circular stationary-region of configurable #stationaryRadius.  iOS has a nice system  [Significant Changes API](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instm/CLLocationManager/startMonitoringSignificantLocationChanges), which allows the os to suspend your app until a cell-tower change is detected (typically 2-3 city-block change) Android uses [LocationManager#addProximityAlert](http://developer.android.com/reference/android/location/LocationManager.html). Windows Phone does not have such a API.

When the plugin detects your user has moved beyond his stationary-region, it engages the native platform's geolocation system for aggressive monitoring according to the configured `#desiredAccuracy`, `#distanceFilter` and `#locationTimeout`.  The plugin attempts to intelligently scale `#distanceFilter` based upon the current reported speed.  Each time `#distanceFilter` is determined to have changed by 5m/s, it recalculates it by squaring the speed rounded-to-nearest-five and adding #distanceFilter (I arbitrarily came up with that formula.  Better ideas?).

  `(round(speed, 5))^2 + distanceFilter`

## iOS

On iOS the plugin will execute your configured ```callbackFn```. You may manually POST the received ```GeoLocation``` to your server using standard XHR. iOS ignores the @config params ```url```, ```params``` and ```headers```. The plugin uses iOS Significant Changes API, and starts triggering ```callbackFn``` only when a cell-tower switch is detected (i.e. the device exits stationary radius). The function ```changePace(isMoving, success, failure)``` is provided to force the plugin to enter "moving" or "stationary" state.


### Android

Android **WILL** execute your configured ```callbackFn```. This is the main difference from original christocracy plugin. Android is using intents to do so. Since the Android plugin must run as an autonomous Background Service, disconnected from your the main Android Activity (your foreground application), the background-geolocation plugin will continue to run, even if the foreground Activity is killed due to memory constraints.

### WP8

Keep in mind that it is **not** possible to use ```start()``` during the ```pause``` event of Cordova/PhoneGap. WP8 suspend your app immediately and our ```start()``` will not be executed. So make sure you fire ```start()``` before the app is closed/minimized.

### Config

Use the following config-parameters with the #configure method:

#####`@param {Integer} desiredAccuracy [0, 10, 100, 1000] in meters`

The lower the number, the more power devoted to GeoLocation resulting in higher accuracy readings.  1000 results in lowest power drain and least accurate readings.  @see [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/desiredAccuracy)

#####`@param {Integer} stationaryRadius (meters)`

When stopped, the minimum distance the device must move beyond the stationary location for aggressive background-tracking to engage.  Note, since the plugin uses iOS significant-changes API, the plugin cannot detect the exact moment the device moves out of the stationary-radius.  In normal conditions, it can take as much as 3 city-blocks to 1/2 km before staionary-region exit is detected.
In WP8 the frequency  of position polling (while in stationary mode) is slowed down to once every three minutes.

#####`@param {Boolean} debug`

When enabled, the plugin will emit sounds for life-cycle events of background-geolocation!  **NOTE iOS**:  In addition, you must manually enable the *Audio and Airplay* background mode in *Background Capabilities* to hear these debugging sounds.

|    | *ios* | *android* | *WP8* |
| ------------- | ------------- | ------------- | ------------- |
| Exit stationary region  | Calendar event notification sound  | dialtone beep-beep-beep  | triple short high tone |
| GeoLocation recorded  | SMS sent sound  | tt short beep | single long high tone |
| Aggressive geolocation engaged | SIRI listening sound |  | |
| Passive geolocation engaged | SIRI stop listening sound |  |  |
| Acquiring stationary location sound | "tick,tick,tick" sound |  | double long low tone |
| Stationary location acquired sound | "bloom" sound | long tt beep | double short high tone |  

#####`@param {Integer} distanceFilter`

The minimum distance (measured in meters) a device must move horizontally before an update event is generated.  @see [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/distanceFilter).  However, #distanceFilter is elastically auto-calculated by the plugin:  When speed increases, #distanceFilter increases;  when speed decreases, so does distanceFilter.

distanceFilter is calculated as the square of speed-rounded-to-nearest-5 and adding configured #distanceFilter.

  `(round(speed, 5))^2 + distanceFilter`

For example, at biking speed of 7.7 m/s with a configured distanceFilter of 30m:

  `=> round(7.7, 5)^2 + 30`
  `=> (10)^2 + 30`
  `=> 100 + 30`
  `=> 130`

A gps location will be recorded each time the device moves 130m.

At highway speed of 30 m/s with distanceFilter: 30,

  `=> round(30, 5)^2 + 30`
  `=> (30)^2 + 30`
  `=> 900 + 30`
  `=> 930`

A gps location will be recorded every 930m

Note the following real example of background-geolocation on highway 101 towards San Francisco as the driver slows down as he runs into slower traffic (geolocations become compressed as distanceFilter decreases)

![distanceFilter at highway speed](/distance-filter-highway.png "distanceFilter at highway speed")

Compare now background-geolocation in the scope of a city.  In this image, the left-hand track is from a cab-ride, while the right-hand track is walking speed.

![distanceFilter at city scale](/distance-filter-city.png "distanceFilter at city scale")

#####`@param {Boolean} stopOnTerminate`
Enable this in order to force a stop() when the application terminated (e.g. on iOS, double-tap home button, swipe away the app). This does not work in WP8.

#####`@param {Integer} locationTimeout`

The minimum time interval between location updates for Android and WP8, in seconds.
See [Android docs](http://developer.android.com/reference/android/location/LocationManager.html#requestLocationUpdates(long,%20float,%20android.location.Criteria,%20android.app.PendingIntent)) and the [MS doc](http://msdn.microsoft.com/en-us/library/windows/apps/windows.devices.geolocation.geolocator.reportinterval) for more information.

### Android Config

Example:

```
bgGeo.configure(callbackFn, failureFn, {
    desiredAccuracy: 10,
    stationaryRadius: 20,
    distanceFilter: 30,
    notificationIconColor: '#4CAF50',
    notificationTitle: 'Background tracking',
    notificationText: 'ENABLED',
    notificationIcon: 'notification_icon',
    debug: true, // <-- enable this hear sounds for background-geolocation life-cycle.
    stopOnTerminate: false, // <-- enable this to clear background location settings when the app terminates
    locationService: bgGeo.service.ANDROID_FUSED_LOCATION
});

```

#####`@param {String} notificationText/Title`

On Android devices it is required to have a notification in the drawer because it's a "foreground service".  This gives it high priority, decreasing probability of OS killing it.  To customize the title and text of the notification, set these options.

#####`@param {String} notificationIconColor`

The accent color to use for notification. Defaults to **#4CAF50**

#####`@param {String} notificationIcon`

Optional: the filename of a custom notification icon. The icon must be located in the *res/drawable* directory. You should include a small and large icon (append "\_small" and "\_large" to the end of your image filenames). Omit the small and large when passing notificationIcon to configure. This will default to "notification_icon".

To use custom notification icon eg. new_icon, you need to:

##### 1. Configure plugin
```
bgGeo.configure(callbackFn, failureFn, {
    //... add other config options
    notificationIcon: 'new_icon'
    //... add other config options
});
```

##### 2. Copy icon files
Add your custom *new_icon_small.png* and *new_icon_large.png* to res/drawable directory.

##### 3. Register icons in plugin.xml source-file.
```
<source-file src="res/drawable/new_icon_small.png" target-dir="res/drawable" />
<source-file src="res/drawable/new_icon_large.png" target-dir="res/drawable" />
```

#####`@param {Integer} locationService`

You can choose from two location providers:

* ANDROID_DISTANCE_FILTER (default)
* ANDROID_FUSED_LOCATION

ANDROID_DISTANCE_FILTER is using christocracy's distance filter algorithm. This is the default service.
ANDROID_FUSED_LOCATION is using google FusedLocation API.

To configure plugin to use FusedLocation

```
bgGeo.configure(callbackFn, failureFn, {
    //... add other config options
    locationService: bgGeo.service.ANDROID_FUSED_LOCATION
    //... add other config options
});
```

#####`@param {Integer} interval`

Only used for ANDROID_FUSED_LOCATION.

#####`@param {Integer} fastestInterval`

Only used for ANDROID_FUSED_LOCATION.

### iOS Config

Example:

```
bgGeo.configure(callbackFn, failureFn, {
    desiredAccuracy: 10,
    stationaryRadius: 20,
    distanceFilter: 30,
    activityType: 'AutomotiveNavigation',
    debug: true, // <-- enable this hear sounds for background-geolocation life-cycle.
    stopOnTerminate: false // <-- enable this to clear background location settings when the app terminates
});

```

#####`@param {String} activityType [AutomotiveNavigation, OtherNavigation, Fitness, Other]`

Presumably, this affects ios GPS algorithm.  See [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/activityType) for more information

### WP8 Config

#####`{String} desiredAccuracy`

In Windows Phone, the underlying GeoLocator you can choose to use 'DesiredAccuracy' or 'DesiredAccuracyInMeters'. Since this plugins default configuration accepts meters, the default desiredAccuracy is mapped to the Windows Phone DesiredAccuracyInMeters leaving the DesiredAccuracy enum empty. For more info see the [MS docs](http://msdn.microsoft.com/en-us/library/windows/apps/windows.devices.geolocation.geolocator.desiredaccuracyinmeters) for more information.

## Development

There are many works of original christocracy's plugin. The most interesting repos I've found are:
* [huttj](https://github.com/huttj/cordova-plugin-background-geolocation)
* [erikkemperman](https://github.com/erikkemperman/cordova-plugin-background-geolocation/)
* [codebling](https://github.com/codebling/cordova-plugin-background-geolocation)

Lot of work has been done, but scattered all over the github. My intention is to maintain
this version and adopt all those cool changes. You're more then welcome to pull your request here.

## Changelog

### [0.5.2] - 2015-10-12
#### Fixed
- Android fixing FusedLocationService start and crash on stop

### [0.5.1] - 2015-10-12
#### Fixed
- Android fix return types
- Android fix #3 NotificationBuilder.setColor method not present in API Level <21

#### Changed
- Android replacing Notication.Builder for NotificationCompat.Builder
- SampleApp can send position to server.
- SampleApp offline mode (IndexedDB)

#### Removed
- Android unnecessary plugins
- Docs: removing instructions to enable cordova geolocation in foreground
 and user accept location services

### [0.5.0] - 2015-10-10
#### Changed
- Android FusedLocationService
- Android package names reverted
- Android configuration refactored
- WP8 merged improvements

#### Removed
- Android unused classes
- All removing deprecated url, params, headers

### [0.4.3] - 2015-10-09
#### Added
- Android Add icon color parameter

#### Changed
- Changed the plugin.xml dependencies to the new NPM-based plugin syntax
- updated SampleApp

### [0.4.2] - 2015-09-30
#### Added
- Android open activity when notification clicked [69989e79a8a67485fc88463eec8d69bb713c2dbe](https://github.com/erikkemperman/cordova-plugin-background-geolocation/commit/69989e79a8a67485fc88463eec8d69bb713c2dbe)

#### Fixed
- Android duplicate desiredAccuracy extra
- Android [compilation error](https://github.com/coletivoEITA/cordova-plugin-background-geolocation/commit/813f1695144823d2a61f9733ced5b9fdedf15ff3)

### [0.4.1] - 2015-09-21
- maintenance version

### [0.4.0] - 2015-03-08
#### Added
- Android using callbacks same as iOS

#### Removed
- Android storing position into sqlite

## Licence ##

[Apache License](http://www.apache.org/licenses/LICENSE-2.0)

Copyright (c) 2013 Christopher Scott, Transistor Software

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
