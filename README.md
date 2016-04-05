# cordova-plugin-mauron85-background-geolocation

## Fork notice

This is fork of [christocracy cordova-background-geolocation](https://github.com/christocracy/cordova-plugin-background-geolocation). The main change is in Android version. Posting positions to url was replaced by callbacks, so now it works same as in iOS. Plugin is now battery and data efficient **foreground** and background geolocation provider.

On Android you can choose from two location service providers:
* ANDROID_DISTANCE_FILTER (original christocracy's)
* ANDROID_FUSED_LOCATION (experimental contributed by [huttj](https://github.com/huttj/cordova-plugin-background-geolocation))

See wiki [Which provider should I use?](https://github.com/mauron85/cordova-plugin-background-geolocation/wiki/Android-providers) for more information about providers.

Warning: You probably have to set your cordova app to keep running by **keepRunning** property to true (this is the default now).

## Description

Cross-platform foreground and background geolocation for Cordova / PhoneGap with battery-saving "circular region monitoring" and "stop detection".

It is far more battery and data efficient then html5 geolocation or cordova-geolocation plugin. But you can still use it together with other geolocation providers (eg. html5 navigator.geolocation).

## Installing the plugin

As Cordova is [shifting towards npm](http://cordova.apache.org/announcements/2015/04/21/plugins-release-and-move-to-npm.html), this plugin can be installed from npm:

```
cordova plugin add cordova-plugin-mauron85-background-geolocation
```
## Registering plugin for Adobe® PhoneGap™ Build

There is separate project [cordova-plugin-mauron85-background-geolocation-phonegapbuild](https://github.com/mauron85/cordova-plugin-mauron85-background-geolocation-phonegapbuild) to support [Adobe® PhoneGap™ Build](http://build.phonegap.com).

The reason is that PhoneGap™ Build doesn't support ```<framework src="com.google.android.gms:play-services-location:+" />``` config option, so instead [cordova-plugin-googleplayservices](https://github.com/floatinghotpot/google-play-services) is used as dependency.

To register plugin add following line into your config.xml:

```
<gap:plugin name="cordova-plugin-mauron85-background-geolocation-phonegapbuild" source="npm"/>
```

NOTE: If you're using *hydration*, you have to download and reinstall your app with every new version of the plugin, as plugins are not updated.

## Compilation

### Android
You will need to ensure that you have installed the following items through the Android SDK Manager:

Name                       | Version
-------------------------- | -------
Android SDK Tools          | 24.4.1
Android SDK Platform-tools | 23.1
Android SDK Build-tools    | 23.0.1
Android Support Repository | 25
Android Support Library    | 23.1.1
Google Play Services       | 29
Google Repository          | 24

## Quick Example

```javascript
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady () {

    /**
    * This callback will be executed every time a geolocation is recorded in the background.
    */
    var callbackFn = function(location) {
        console.log('[js] BackgroundGeoLocation callback:  ' + location.latitude + ',' + location.longitude);

        // Do your HTTP request here to POST location to your server.
        // jQuery.post(url, JSON.stringify(location));

        /*
        IMPORTANT:  You must execute the finish method here to inform the native plugin that you're finished,
        and the background-task may be completed.  You must do this regardless if your HTTP request is successful or not.
        IF YOU DON'T, ios will CRASH YOUR APP for spending too much time in the background.
        */
        backgroundGeoLocation.finish();
    };

    var failureFn = function(error) {
        console.log('BackgroundGeoLocation error');
    };

    // BackgroundGeoLocation is highly configurable. See platform specific configuration options
    backgroundGeoLocation.configure(callbackFn, failureFn, {
        desiredAccuracy: 10,
        stationaryRadius: 20,
        distanceFilter: 30,
        debug: true, // <-- enable this hear sounds for background-geolocation life-cycle.
        stopOnTerminate: false, // <-- enable this to clear background location settings when the app terminates
    });

    // Turn ON the background-geolocation system.  The user will be tracked whenever they suspend the app.
    backgroundGeoLocation.start();

    // If you wish to turn OFF background-tracking, call the #stop method.
    // backgroundGeoLocation.stop();
}
```

## Example Application

This plugin hosts a SampleApp in [example/SampleApp](/example/SampleApp) folder. SampleApp can be also used to improve plugin in the future. Read instructions in [README.md](/example/SampleApp/README.md).

## Behaviour

The plugin has features allowing you to control the behaviour of background-tracking, striking a balance between accuracy and battery-usage.  In stationary-mode, the plugin attempts to decrease its power usage and accuracy by setting up a circular stationary-region of configurable `stationaryRadius`. iOS has a nice system [Significant Changes API](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instm/CLLocationManager/startMonitoringSignificantLocationChanges), which allows the os to suspend your app until a cell-tower change is detected (typically 2-3 city-block change) Android uses [LocationManager#addProximityAlert](http://developer.android.com/reference/android/location/LocationManager.html). Windows Phone does not have such a API.

When the plugin detects your user has moved beyond his stationary-region, it engages the native platform's geolocation system for aggressive monitoring according to the configured `desiredAccuracy`, `distanceFilter` and `locationTimeout`.  The plugin attempts to intelligently scale `distanceFilter` based upon the current reported speed.  Each time `distanceFilter` is determined to have changed by 5m/s, it recalculates it by squaring the speed rounded-to-nearest-five and adding `distanceFilter` (I arbitrarily came up with that formula.  Better ideas?).

`(round(speed, 5))^2 + distanceFilter`

### distanceFilter
is calculated as the square of speed-rounded-to-nearest-5 and adding configured #distanceFilter.

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

**NOTE:** `distanceFilter` is elastically auto-calculated by the plugin:  When speed increases, distanceFilter increases;  when speed decreases, so does distanceFilter.

## API

### backgroundGeoLocation.configure(success, fail, option)

Parameter | Type | Platform     | Description
--------- | ---- | ------------ | -----------
`success` | `Function` | all | Callback to be executed every time a geolocation is recorded in the background.
`fail` | `Function` | all | Callback to be executed every time a geolocation error occurs.
`option` | `JSON Object` | all |
`option.desiredAccuracy` | `Number` | all | Desired accuracy in meters. Possible values [0, 10, 100, 1000]. The lower the number, the more power devoted to GeoLocation resulting in higher accuracy readings.  1000 results in lowest power drain and least accurate readings. **@see** [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/desiredAccuracy)
`option.stationaryRadius` | `Number` | all | Stationary radius in meters. When stopped, the minimum distance the device must move beyond the stationary location for aggressive background-tracking to engage.
`option.debug` | `Boolean` | all | When enabled, the plugin will emit sounds for life-cycle events of background-geolocation! See debugging sounds table.
`option.distanceFilter` | `Number` | all | The minimum distance (measured in meters) a device must move horizontally before an update event is generated. **@see** [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/distanceFilter).
`option.stopOnTerminate` | `Boolean` | iOS, Android | Enable this in order to force a stop() when the application terminated (e.g. on iOS, double-tap home button, swipe away the app).
`option.locationTimeout` | `Number` | Android, WP8 | The minimum time interval between location updates in seconds. **@see** [Android docs](http://developer.android.com/reference/android/location/LocationManager.html#requestLocationUpdates(long,%20float,%20android.location.Criteria,%20android.app.PendingIntent)) and the [MS doc](http://msdn.microsoft.com/en-us/library/windows/apps/windows.devices.geolocation.geolocator.reportinterval) for more information.
`option.notificationTitle` | `String` optional | Android | Custom notification title in the drawer.
`option.notificationText` | `String` optional | Android | Custom notification text in the drawer.
`option.notificationIconColor` | `String` optional| Android | The accent color to use for notification. Eg. **#4CAF50**.
`option.notificationIcon` | `String` optional | Android | The filename of a custom notification icon. See android quirks. **NOTE:** Only available for API Level >=21.
`option.locationService` | `Number` | Android | Set location service provider **@see** [wiki](https://github.com/mauron85/cordova-plugin-background-geolocation/wiki/Android-providers)
`option.activityType` | `String` | iOS | [AutomotiveNavigation, OtherNavigation, Fitness, Other] Presumably, this affects iOS GPS algorithm. **@see** [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/activityType) for more information

Following options are specific to provider as defined by locationService option
### ANDROID_FUSED_LOCATION provider options

Parameter | Type | Platform     | Description
--------- | ---- | ------------ | -----------
`option.interval` | `Number` | Android | Rate in milliseconds at which your app prefers to receive location updates. @see [android docs](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest.html#getInterval())
`option.fastestInterval` | `Number` | Android | Fastest rate in milliseconds at which your app can handle location updates. **@see** [android  docs](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest.html#getFastestInterval()).
`option.activitiesInterval` | `Number` | Android | Rate in milliseconds at which activity recognition occurs. Larger values will result in fewer activity detections while improving battery life.

Success callback will be called with one argument - location object, which tries to mimic w3c [Coordinates interface](http://dev.w3.org/geo/api/spec-source.html#coordinates_interface).

Callback parameter | Type | Description
------------------ | ---- | -----------
`locationId` | `Number` | ID of location as stored in DB (or null)
`serviceProvider` | `String` | Service provider
`debug` | `Boolean` | true if location recorded as part of debug
`time` | `Number` |UTC time of this fix, in milliseconds since January 1, 1970.
`latitude` | `Number` | latitude, in degrees.
`longitude` | `Number` | longitude, in degrees.
`accuracy` | `Number` | estimated accuracy of this location, in meters.
`speed` | `Number` | speed if it is available, in meters/second over ground.
`altitude` | `Number` | altitude if available, in meters above the WGS 84 reference ellipsoid.
`bearing` | `Number` | bearing, in degrees.


### backgroundGeoLocation.start()

Start background geolocation.

### backgroundGeoLocation.stop()

Stop background geolocation.

### backgroundGeoLocation.isLocationEnabled(success, fail)
NOTE: Android only

One time check for status of location services. In case of error, fail callback will be executed.

Success callback parameter | Type | Description
-------------------------- | ---- | -----------
`enabled` | `Boolean` | true/false (true when location services are enabled)

### backgroundGeoLocation.showLocationSettings()
NOTE: Android only

Show system settings to allow configuration of current location sources.

### backgroundGeoLocation.watchLocationMode(success, fail)
NOTE: Android only

Method can be used to detect user changes in location services settings.
If user enable or disable location services then success callback will be executed.
In case or error (SettingNotFoundException) fail callback will be executed.

Success callback parameter | Type | Description
-------------------------- | ---- | -----------
`enabled` | `Boolean` | true/false (true when location services are enabled)

### backgroundGeoLocation.stopWatchingLocationMode()
NOTE: Android only

Stop watching for location mode changes.

### backgroundGeoLocation.getLocations(success, fail)
NOTE: Android only

Method will return all stored locations.

Success callback parameter | Type | Description
-------------------------- | ---- | -----------
`locations` | `Array` | collection of stored locations

Locations are stored when:

1. ```config.stopOnTerminate``` is false and main activity was killed by the system
or
2. ```option.debug``` is true

Debug locations can be filtered:

```javascript
[].filter.call(locations, function(location) {
    return location.debug === false;
});
```

### backgroundGeoLocation.deleteLocation(locationId, success, fail)
NOTE: Android only

Delete stored location by given locationId.

### backgroundGeoLocation.deleteAllLocations(success, fail)
NOTE: Android only

Delete all stored locations.

### Example config

#### Android:

```javascript
backgroundGeoLocation.configure(callbackFn, failureFn, {
    desiredAccuracy: 10,
    notificationIconColor: '#4CAF50',
    notificationTitle: 'Background tracking',
    notificationText: 'ENABLED',
    notificationIcon: 'notification_icon',
    debug: true, // <-- enable this hear sounds for background-geolocation life-cycle.
    stopOnTerminate: false, // <-- enable this to clear background location settings when the app terminates
    locationService: backgroundGeoLocation.service.ANDROID_FUSED_LOCATION,
    interval: 60000, // <!-- poll for position every minute
    fastestInterval: 120000
});
```

#### iOS:

```javascript
backgroundGeoLocation.configure(callbackFn, failureFn, {
    desiredAccuracy: 10,
    stationaryRadius: 20,
    distanceFilter: 30,
    activityType: 'AutomotiveNavigation',
    debug: true, // <-- enable this hear sounds for background-geolocation life-cycle.
    stopOnTerminate: false // <-- enable this to clear background location settings when the app terminates
});
```

## Quirks

### iOS

On iOS the plugin will execute your configured ```callbackFn```. You may manually POST the received ```GeoLocation``` to your server using standard XHR. The plugin uses iOS Significant Changes API, and starts triggering ```callbackFn``` only when a cell-tower switch is detected (i.e. the device exits stationary radius). The function ```changePace(isMoving, success, failure)``` is provided to force the plugin to enter "moving" or "stationary" state.

#### `stationaryRadius`

Since the plugin uses **iOS** significant-changes API, the plugin cannot detect the exact moment the device moves out of the stationary-radius.  In normal conditions, it can take as much as 3 city-blocks to 1/2 km before stationary-region exit is detected.

### WP8

Keep in mind that it is **not** possible to use ```start()``` during the ```pause``` event of Cordova/PhoneGap. WP8 suspend your app immediately and our ```start()``` will not be executed. So make sure you fire ```start()``` before the app is closed/minimized.

#### `stationaryRadius`
In **WP8** the frequency of position polling (while in stationary mode) is slowed down to once every three minutes.

#### `desiredAccuracy`

In Windows Phone, the underlying GeoLocator you can choose to use 'DesiredAccuracy' or 'DesiredAccuracyInMeters'. Since this plugins default configuration accepts meters, the default desiredAccuracy is mapped to the Windows Phone DesiredAccuracyInMeters leaving the DesiredAccuracy enum empty. For more info see the [MS docs](http://msdn.microsoft.com/en-us/library/windows/apps/windows.devices.geolocation.geolocator.desiredaccuracyinmeters) for more information.

### Android

Android **WILL** execute your configured ```callbackFn```. This is the main difference from original christocracy plugin. Android is using intents to do so.

On Android devices it is required to have a notification in the drawer because it's a "foreground service".  This gives it high priority, decreasing probability of OS killing it. Check [wiki](https://github.com/mauron85/cordova-plugin-background-geolocation/wiki/Android-implementation) for explanation.

If main activity is killed by the system and ```stopOnTerminate``` option is false, plugin will store locations into database. Stored locations can be retrieved later with ```getAllLocations``` method. Locations are also stored, when ```debug``` option is **true**. However in this case all stored locations, are flagged with ```debug: true``` and can be easily filtered.

#### Custom ROMs

Plugin should work with custom ROMS at least ANDROID_DISTANCE_FILTER. But ANDROID_FUSED_LOCATION provider depends on Google Play Services.
Usually ROMs don't include Google Play Services libraries. Strange bugs may occur, like no GPS locations (only from network and passive) and other. When posting issue report, please mention that you're using custom ROM.


#### Multidex
Note: Following section was kindly copied from [phonegap-plugin-push](https://github.com/phonegap/phonegap-plugin-push/blob/master/docs/INSTALLATION.md#multidex). Visit link for resolving issue with facebook plugin.

If you have an issue compiling the app and you're getting an error similar to this (`com.android.dex.DexException: Multiple dex files define`):

```
UNEXPECTED TOP-LEVEL EXCEPTION:
com.android.dex.DexException: Multiple dex files define Landroid/support/annotation/AnimRes;
	at com.android.dx.merge.DexMerger.readSortableTypes(DexMerger.java:596)
	at com.android.dx.merge.DexMerger.getSortedTypes(DexMerger.java:554)
	at com.android.dx.merge.DexMerger.mergeClassDefs(DexMerger.java:535)
	at com.android.dx.merge.DexMerger.mergeDexes(DexMerger.java:171)
	at com.android.dx.merge.DexMerger.merge(DexMerger.java:189)
	at com.android.dx.command.dexer.Main.mergeLibraryDexBuffers(Main.java:502)
	at com.android.dx.command.dexer.Main.runMonoDex(Main.java:334)
	at com.android.dx.command.dexer.Main.run(Main.java:277)
	at com.android.dx.command.dexer.Main.main(Main.java:245)
	at com.android.dx.command.Main.main(Main.java:106)
```

Then at least one other plugin you have installed is using an outdated way to declare dependencies such as `android-support` or `play-services-gcm`.
This causes gradle to fail, and you'll need to identify which plugin is causing it and request an update to the plugin author, so that it uses the proper way to declare dependencies for cordova.
See [this for the reference on the cordova plugin specification](https://cordova.apache.org/docs/en/5.4.0/plugin_ref/spec.html#link-18), it'll be usefull to mention it when creating an issue or requesting that plugin to be updated.

Common plugins to suffer from this outdated dependency management are plugins related to *facebook*, *google+*, *notifications*, *crosswalk* and *google maps*.

#### Android Permissions

Android 6.0 "Marshmallow" introduced a new permissions model where the user can turn on and off permissions as necessary. When user disallow location access permissions, error configure callback will be called with error code: 20.


#### `notificationIcon`
**NOTE:** Only available for API Level >=21.

To use custom notification icon eg. **new_icon**, you need to put icons **new_icon_small.png** and **new_icon_large.png** into *res/drawable* directory **of your app**. You can automate the process  as part of **after_platform_add** hook configured via [config.xml](/example/SampleApp/config.xml). Check SampleApp [config.xml](/example/SampleApp/config.xml) and [scripts/resource_files.js](/example/SampleApp/scripts/resource_files.js) for reference.

With Adobe® PhoneGap™ Build icons must be placed into ```locales/android/drawable``` dir at the root of your project. For more information go to [how-to-add-native-image-with-phonegap-build](http://stackoverflow.com/questions/30802589/how-to-add-native-image-with-phonegap-build/33221780#33221780).

### Intel XDK

Plugin will not work in XDK emulator ('Unimplemented API Emulation: BackgroundGeoLocation.start' in emulator). But will work on real device.

## Debugging sounds
|    | *ios* | *android* | *WP8* |
| ------------- | ------------- | ------------- | ------------- |
| Exit stationary region  | Calendar event notification sound  | dialtone beep-beep-beep  | triple short high tone |
| GeoLocation recorded  | SMS sent sound  | tt short beep | single long high tone |
| Aggressive geolocation engaged | SIRI listening sound |  | |
| Passive geolocation engaged | SIRI stop listening sound |  |  |
| Acquiring stationary location sound | "tick,tick,tick" sound |  | double long low tone |
| Stationary location acquired sound | "bloom" sound | long tt beep | double short high tone |  

**NOTE:** For iOS  in addition, you must manually enable the *Audio and Airplay* background mode in *Background Capabilities* to hear these debugging sounds.

## Geofencing
There is nice cordova plugin [cordova-plugin-geofence](https://github.com/cowbell/cordova-plugin-geofence), which does exactly that. Let's keep this plugin lightweight as much as possible.

## Changelog

See [CHANGES.md](/CHANGES.md)

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
