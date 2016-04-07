## Changelog

### [0.9.6] - 2016-04-07
#### Fixed
- Android ANDROID_FUSED_LOCATION fixing crash on start
- Android ANDROID_FUSED_LOCATION unregisterReceiver on destroy

### [0.9.5] - 2016-04-05
#### Fixed
- Android ANDROID_FUSED_LOCATION startTracking when STILL after app has started

### [0.9.4] - 2016-01-31
#### Fixed
- Android 6.0 permissions issue #21

### [0.9.3] - 2016-01-29
#### Fixed
- iOS cordova 6 compilation error
- iOS fix for iOS 9

#### Changes
- iOS removing cordova-plugin-geolocation dependency
- iOS user prompt for using location services
- iOS error callback when location services are disabled
- iOS error callback when user denied location tracking
- iOS adding error callbacks to SampleApp

### [0.9.2] - 2016-01-29
#### Fixed
- iOS temporarily using cordova-plugin-geolocation-ios9-fix to fix issues with iOS9
- iOS fixing SampleApp indexedDB issues

### [0.9.1] - 2015-12-18
#### Fixed
- Android ANDROID_FUSED_LOCATION fix config setActivitiesInterval

### [0.9.0] - 2015-12-18
#### Changed
- Android ANDROID_FUSED_LOCATION using ActivityRecognition (saving battery)

### [0.8.3] - 2015-12-18
#### Fixed
- Android fixing crash on exit

### [0.8.2] - 2015-12-18
#### Fixed
- Android fixing #9 - immediate bg service crash

### [0.8.1] - 2015-12-15
#### Fixed
- Android fixing #9

### [0.8.0] - 2015-12-15 (Merry XMas Edition :-)
#### Fixed
- Android persist location when main activity was killed

#### Changed
- Android persisting position when debug is on

### [0.7.3] - 2015-11-06
#### Fixed
- Android issue #11

### [0.7.2] - 2015-10-21
#### Fixed
- iOS fixing plugin dependencies (build)
- iOS related fixes for SampleApp

### [0.7.1] - 2015-10-21
#### Changed
- Android ANDROID_FUSED_LOCATION ditching setSmallestDisplacement(stationaryRadius) (seems buggy)

### [0.7.0] - 2015-10-21
#### Changed
- Android deprecating config option.interval
- Android allow run in background for FusedLocationService (wakeLock)
- Android will try to persist locations when main activity is killed
- Android new methods: (getLocations, deleteLocation, deleteAllLocations)
- Android stop exporting implicit intents (security)
- SampleApp updates

### [0.6.0] - 2015-10-17
#### Changed
- deprecating window.plugins clobber
- SampleApp updates

#### Added
- Android showLocationSettings and watchLocationMode

### [0.5.4] - 2015-10-13
#### Changed
- Android only cosmetic changes, but we need stable base

### [0.5.3] - 2015-10-12
#### Changed
- Android not setting any default notificationIcon and notificationIconColor.
- Android refactoring
- Android updated SampleApp

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
