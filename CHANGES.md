## Changelog

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
