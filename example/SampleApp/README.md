Example Background GeoLocation app.
=============================================

Help to make this plugin better
==============================

Enable **Collect**, to send your position, battery level and basic device info to [background-geolocation-console](https://background-geolocation-console.herokuapp.com/). This data can be used to improve plugin in the future.

No ip address or device.uuid is stored on server. The device.uuid is anonymized before sent.
I will not provide any binary builds of SampleApp, so you can always check the source code, what the app is actually doing. [Source code](https://github.com/transistorsoft/background-geolocation-console) of the console is available on transistorsoft github.

## Description

Example app shows some possibilities how to use this plugin in real apps.
It is using IndexedDB to store locations when offline and resent them automatically when back online.

## How to build SampleApp

Replace platform with one of supported platforms: android, ios or wp8. In this example we will build for Android.

```
$ cordova platform add android
$ cordova build android
```

Run on device

```
$ cordova run --device
```

### iOS quirks

If you're using XCode, boot the SampleApp in the iOS Simulator and enable ```Debug->Location->City Drive```.


## Changelog

### [0.5.1] - Unreleased
#### Changed
- Removed foreground geo location. Now using only background plugin.
