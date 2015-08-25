[Premium Version](http://transistorsoft.github.io/cordova-background-geolocation/)
==============================

A new [Premium Version](http://transistorsoft.github.io/cordova-background-geolocation/) is available.

- Implements the latest Android APIs for **much-improved** battery performance.
- Auto-start Android & iOS on device-boot.
- Extra meta-data for each recorded location (eg: battery-state, moving/stationary)
- Cordova plugin `cordova-plugin-geolocation` is no longer required.
- Android now executes your `callback` just like **iOS**!
- Both iOS & Android can optionally **HTTP** POST locations to your server in the native code.  Optional **batch-mode**.
- Native **SQLite persistence** layer (for when no network is available).  Plugin provides a javascript API to fetch and/or sync the locations to your server.
- **Geofencing**.  Both iOS and Android allow you to add arbitrary circular geofences which execute your javascript-callback when a crossing-event occurs.


[Premium iOS Now Available Free!](https://github.com/transistorsoft/cordova-background-geolocation-lt)
===============================

The **[Premium iOS](https://github.com/transistorsoft/cordova-background-geolocation-lt)** version is now available free!

- Try out the [SampleApp](https://github.com/transistorsoft/cordova-background-geolocation-SampleApp) now!
- In-plugin SQLite storage of each recorded location
- Optional native HTTP layer
- Geofencing features
- Many more configuration options
- Extra meta-data added to each recorded location (eg: battery-state information)

![Android Battery Performance](/images/Screenshot-android-battery-framed.png "Android battery performance")
![SampleApp](/images/Screenshot-android-map1-framed.png "SampleApp")

[Now Available for React Native](https://github.com/transistorsoft/react-native-background-geolocation)
================================

Background Geolocation (Premium Version) has been [ported](http://www.reactnative.com/cross-platform-background-geolocation-module-for-react-native/?utm_content=buffer4e559&utm_medium=social&utm_source=twitter.com&utm_campaign=buffer) to [React Native](https://facebook.github.io/react-native/)!

Cordova Background Geolocation
==============================

Cross-platform background geolocation for Cordova / PhoneGap with battery-saving "circular region monitoring" and "stop detection".

![distanceFilter at highway speed](/images/distance-filter-highway.png "distanceFilter at highway speed")

Follows the [Cordova Plugin spec](https://github.com/apache/cordova-plugman/blob/master/plugin_spec.md), so that it works with [Plugman](https://github.com/apache/cordova-plugman).

This plugin leverages Cordova/PhoneGap's [require/define functionality used for plugins](http://simonmacdonald.blogspot.ca/2012/08/so-you-wanna-write-phonegap-200-android.html).

## Using the plugin ##
The plugin creates the object `window.BackgroundGeolocation` with the methods

  `configure(success, fail, option)`,

  `start(success, fail)`

  `stop(success, fail)`.

## Installing the plugin ##

```

   cordova plugin add https://github.com/christocracy/cordova-plugin-background-geolocation.git
```

**ACHTUNG!** If you're not using **Cordova 5.x**, you must use the tagged branch ```#cordova-4.x``` to install the plugin, like this (Because Cordova has [migrated to npm](https://cordova.apache.org/announcements/2015/04/21/plugins-release-and-move-to-npm.html))

```
  cordova plugin add https://github.com/christocracy/cordova-plugin-background-geolocation.git#cordova-4.x
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

    var bgGeo = window.BackgroundGeolocation;

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
        console.log('[js] BackgroundGeoLocation callback:  ' + location.latitude + ',' + location.longitude);
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
        url: 'http://only.for.android.com/update_location.json', // <-- Android ONLY:  your server url to send locations to
        params: {
            auth_token: 'user_secret_auth_token',    //  <-- Android ONLY:  HTTP POST params sent to your server when persisting locations.
            foo: 'bar'                              //  <-- Android ONLY:  HTTP POST params sent to your server when persisting locations.
        },
        headers: {                                   // <-- Android ONLY:  Optional HTTP headers sent to your configured #url when persisting locations
            "X-Foo": "BAR"
        },
        desiredAccuracy: 10,
        stationaryRadius: 20,
        distanceFilter: 30,
        notificationTitle: 'Background tracking', // <-- android only, customize the title of the notification
        notificationText: 'ENABLED', // <-- android only, customize the text of the notification
        activityType: 'AutomotiveNavigation',
        debug: true, // <-- enable this hear sounds for background-geolocation life-cycle.
        stopOnTerminate: false // <-- enable this to clear background location settings when the app terminates
    });

    // Turn ON the background-geolocation system.  The user will be tracked whenever they suspend the app.
    bgGeo.start();

    // If you wish to turn OFF background-tracking, call the #stop method.
    // bgGeo.stop()


```

NOTE: The plugin includes `org.apache.cordova.geolocation` as a dependency.  You must enable Cordova's GeoLocation in the foreground and have the user accept Location services by executing `#watchPosition` or `#getCurrentPosition`.

## Example Application

This plugin hosts a SampleApp in ```example/SampleApp``` folder.  This SampleApp contains no plugins so you must first start by adding this plugin.  You must copy the the `SampleApp` **out of** the repo folder to use it.

![SampleApp](/images/android-sample-app.png "SampleApp")

First, copy the `SampleApp` out of the repo and into any folder you wish (eg: `tmp`).
```
$ cp -R cordova-plugin-background-geolocation/example/SampleApp ./tmp
$ cd tmp/SampleApp
$ cordova plugin add https://github.com/christocracy/cordova-plugin-background-geolocation.git
$ cordova plugin add cordova-plugin-whitelist
$ cordova platform add ios
$ cordova build ios

```

If you're using XCode, boot the SampleApp in the iOS Simulator and enable ```Debug->Location->City Drive```.


## Behaviour

The plugin has features allowing you to control the behaviour of background-tracking, striking a balance between accuracy and battery-usage.  In stationary-mode, the plugin attempts to descrease its power usage and accuracy by setting up a circular stationary-region of configurable #stationaryRadius.  iOS has a nice system  [Significant Changes API](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instm/CLLocationManager/startMonitoringSignificantLocationChanges), which allows the os to suspend your app until a cell-tower change is detected (typically 2-3 city-block change) Android uses [LocationManager#addProximityAlert](http://developer.android.com/reference/android/location/LocationManager.html). Windows Phone does not have such a API.

When the plugin detects your user has moved beyond his stationary-region, it engages the native platform's geolocation system for aggressive monitoring according to the configured `#desiredAccuracy`, `#distanceFilter` and `#locationTimeout`.  The plugin attempts to intelligently scale `#distanceFilter` based upon the current reported speed.  Each time `#distanceFilter` is determined to have changed by 5m/s, it recalculates it by squaring the speed rounded-to-nearest-five and adding #distanceFilter (I arbitrarily came up with that formula.  Better ideas?).

  `(round(speed, 5))^2 + distanceFilter`

## iOS

On iOS the plugin will execute your configured ```callbackFn```. You may manually POST the received ```GeoLocation``` to your server using standard XHR. iOS ignores the @config params ```url```, ```params``` and ```headers```. The plugin uses iOS Significant Changes API, and starts triggering ```callbackFn``` only when a cell-tower switch is detected (i.e. the device exits stationary radius). The function ```changePace(isMoving, success, failure)``` is provided to force the plugin to enter "moving" or "stationary" state.


### Android

Android **WILL NOT** execute your configured ```callbackFn```.  The plugin manages sync-ing GeoLocations to your server automatically, using the configured ```url```, ```params``` and ```headers```.  Since the Android plugin must run as an autonomous Background Service, disconnected from your the main Android Activity (your foreground application), the background-geolocation plugin will continue to run, even if the foreground Activity is killed due to memory constraints.  This is why the Android plugin cannot execute the Javascript ```callbackFn```, since your app is not guaranteed to keep running -- syncing locations to the server must be handled by the plugin.

The Android plugin sends an HTTP POST to your configured ```url``` with ```Content-Type: application/json```.  The JSON location-data is encoded into the Request Body.  PHP people have [trouble with this](https://github.com/christocracy/cordova-plugin-background-geolocation/issues/50).  In PHP, find the raw JSON body with:

```$data = file_get_contents('php://input');```.

```
{
    "location": {
        "latitude": "<data>",
        "longitude": "<data>",
        "speed": "<data>",
        "bearing" "<data>",
        "altitude": "<data>",
        "recorded_at": "<data>"
    }
}
```

### WP8

WP8 uses ```callbackFn``` the way iOS do. On WP8, however, the plugin does not support the Stationary location and does not implement ```getStationaryLocation()``` and ```onPaceChange()```.
Keep in mind that it is **not** possible to use ```start()``` at the ```pause``` event of Cordova/PhoneGap. WP8 suspend your app immediately and ```start()``` will not be executed. So make sure you fire ```start()``` before the app is closed/minimized.

### Config

Use the following config-parameters with the #configure method:

#####`@param {Integer} desiredAccuracy [0, 10, 100, 1000] in meters`

The lower the number, the more power devoted to GeoLocation resulting in higher accuracy readings.  1000 results in lowest power drain and least accurate readings.  @see [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/desiredAccuracy)

#####`@param {Integer} stationaryRadius (meters)`

When stopped, the minimum distance the device must move beyond the stationary location for aggressive background-tracking to engage.  Note, since the plugin uses iOS significant-changes API, the plugin cannot detect the exact moment the device moves out of the stationary-radius.  In normal conditions, it can take as much as 3 city-blocks to 1/2 km before staionary-region exit is detected.

#####`@param {Boolean} debug`

When enabled, the plugin will emit sounds for life-cycle events of background-geolocation!  **NOTE iOS**:  In addition, you must manually enable the *Audio and Airplay* background mode in *Background Capabilities* to hear these debugging sounds.

- Exit stationary region:  *[ios]* Calendar event notification sound *[android]* dialtone beep-beep-beep
- GeoLocation recorded:  *[ios]* SMS sent sound, *[android]* tt short beep, *[WP8]* High beep, 1 sec.
- Aggressive geolocation engaged:  *[ios]* SIRI listening sound, *[android]* none
- Passive geolocation engaged:  *[ios]* SIRI stop listening sound, *[android]* none
- Acquiring stationary location sound: *[ios]* "tick,tick,tick" sound, *[android]* none
- Stationary location acquired sound:  *[ios]* "bloom" sound, *[android]* long tt beep.

![Enable Background Audio](/images/enable-background-audio.png "Enable Background Audio")

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

![distanceFilter at highway speed](/images/distance-filter-highway.png "distanceFilter at highway speed")

Compare now background-geolocation in the scope of a city.  In this image, the left-hand track is from a cab-ride, while the right-hand track is walking speed.

![distanceFilter at city scale](/images/distance-filter-city.png "distanceFilter at city scale")

#####`@param {Boolean} stopOnTerminate`
Enable this in order to force a stop() when the application terminated (e.g. on iOS, double-tap home button, swipe away the app)


### Android Config

#####`@param {String} url`

The url which the Android plugin will persist background geolocation to

#####`@param {Object} params`

Optional HTTP params POSTed to your server when persisting locations (eg:  auth_token)

#####`@param {Object} headers`

Optional HTTP headers POSTed to your server when persisting locations

#####`@param {String} notificationText/Title`

On Android devices it is required to have a notification in the drawer because it's a "foreground service".  This gives it high priority, decreasing probability of OS killing it.  To customize the title and text of the notification, set these options.

####`@param {String} notificationIcon`

Optional: the filename of a custom notification icon. The icon must be located in the plugin directory. You should include a small and large icon; append "\_small" and "\_large" end of your image filenames. Omit the small and large when passing notificationIcon to configure. This will default to "notification_icon".

```
notificationIcon = "notification_icon"

src/android/notification_icon_small.png
src/android/notification_icon_large.png
```

In order to include any new images, you must add them to the plugin.xml source-files.

```
notificationIcon = "new_icon"

src/android/new_icon_small.png
src/android/new_icon_large.png

<source-file src="src/android/new_icon_small.png" target-dir="res/drawable" />
<source-file src="src/android/new_icon_large.png" target-dir="res/drawable" />
```

#####`@param {Integer} locationTimeout`

The minimum time interval between location updates, in seconds.  See [Android docs](http://developer.android.com/reference/android/location/LocationManager.html#requestLocationUpdates(long,%20float,%20android.location.Criteria,%20android.app.PendingIntent)) for more information.

### iOS Config

#####`@param {String} activityType [AutomotiveNavigation, OtherNavigation, Fitness, Other]`

Presumably, this affects ios GPS algorithm.  See [Apple docs](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instp/CLLocationManager/activityType) for more information

### WP8 Config

#####`{String} desiredAccuracy`

In Windows Phone, the underlying GeoLocator you can choose to use 'DesiredAccuracy' or 'DesiredAccuracyInMeters'. Since this plugins default configuration accepts meters, the default desiredAccuracy is mapped to the Windows Phone DesiredAccuracyInMeters leaving the DesiredAccuracy enum empty. For more info see the [MS docs](http://msdn.microsoft.com/en-us/library/windows/apps/windows.devices.geolocation.geolocator.desiredaccuracyinmeters) for more information.

## Licence ##

[Apache License](https://github.com/christocracy/cordova-plugin-background-geolocation/blob/master/LICENSE)

Copyright (c) 2013 Christopher Scott, Transistor Software
