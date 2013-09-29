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
   cordova plugin add <path_download_plugin>
```

A full example could be:
```
   var bgGeo = window.plugins.backgroundGeoLocation;
   bgGeo.configure(successFn, failFn, {
       auth_token: 'asdf23423kjslkfjasdf',
       url: '/users/location.json'
   });

   // Enable background geolocation
   bgGeo.start();

   // Disable it
   bgGeo.stop();

```

## iOS

The iOS implementation of background geolocation uses [CLLocationManager#startMonitoringSignificantLocationChanges](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html#//apple_ref/occ/instm/CLLocationManager/startMonitoringSignificantLocationChanges)

## Android

** TODO Brian ##

## Licence ##

The MIT License

Copyright (c) 2010 Matt Kane

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
