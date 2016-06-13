////
//  BackgroundGeolocationDelegate
//
//  Created by Marian Hello on 04/06/16.
//  Version 2.0.0
//
//  According to apache license
//
//  This is class is using code from christocracy cordova-plugin-background-geolocation plugin
//  https://github.com/christocracy/cordova-plugin-background-geolocation
//

#import <UIKit/UIKit.h>
#import "BackgroundGeolocationDelegate.h"
#import "SQLiteLocationDAO.h"

// Debug sounds for bg-geolocation life-cycle events.
// http://iphonedevwiki.net/index.php/AudioServices
#define exitRegionSound         1005
#define locationSyncSound       1004
#define paceChangeYesSound      1110
#define paceChangeNoSound       1112
#define acquiringLocationSound  1103
#define acquiredLocationSound   1052
#define locationErrorSound      1073

#define SYSTEM_VERSION_EQUAL_TO(v)                  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] == NSOrderedSame)
#define SYSTEM_VERSION_GREATER_THAN(v)              ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] == NSOrderedDescending)
#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v)  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)
#define SYSTEM_VERSION_LESS_THAN(v)                 ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] == NSOrderedAscending)
#define SYSTEM_VERSION_LESS_THAN_OR_EQUAL_TO(v)     ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedDescending)

#define LOCATION_DENIED         "User denied use of location services."
#define LOCATION_RESTRICTED     "Application's use of location services is restricted."
#define LOCATION_NOT_DETERMINED "User undecided on application's use of location services."

NSString * const ErrorDomain = @"com.marianhello";

enum {
    maxLocationWaitTimeInSeconds = 15,
    maxLocationAgeInSeconds = 30
};

@interface BackgroundGeolocationDelegate () <CLLocationManagerDelegate>
@end

@implementation BackgroundGeolocationDelegate {
    BOOL isStarted;
    BOOL isUpdatingLocation;
    BOOL isAcquiringStationaryLocation;
    BOOL isAcquiringSpeed;
    BGOperationMode operationMode;
    NSDate *aquireStartTime;
    //    BOOL shouldStart; //indicating intent to start service, but we're waiting for user permission
    
    CLLocationManager *locationManager;
    BackgroundLocation *lastLocation;
    CLCircularRegion *stationaryRegion;
    NSDate *stationarySince;
    
    NSMutableArray *locationQueue;
    NSError* locationError;
    
    UILocalNotification *localNotification;
    
    NSNumber *maxBackgroundHours;
    UIBackgroundTaskIdentifier bgTask;
    NSDate *lastBgTaskAt;
    
    // configurable options
    Config* _config;
}

- (id) init
{
    self = [super init];
    
    if (self == nil) {
        return self;
    }
    
    // background location cache, for when no network is detected.
    locationManager = [[CLLocationManager alloc] init];
    
    if (SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"9.0")) {
        NSLog(@"BackgroundGeolocationDelegate iOS9 detected");
        locationManager.allowsBackgroundLocationUpdates = YES;
    }
    
    locationManager.delegate = self;
    
    localNotification = [[UILocalNotification alloc] init];
    localNotification.timeZone = [NSTimeZone defaultTimeZone];
    
    locationQueue = [[NSMutableArray alloc] init];
    
    bgTask = UIBackgroundTaskInvalid;
    
    isStarted = NO;
    isUpdatingLocation = NO;
    isAcquiringStationaryLocation = NO;
    isAcquiringSpeed = NO;
    //    shouldStart = NO;
    stationaryRegion = nil;
    
    return self;
}

/**
 * configure plugin
 * @param {NSInteger} stationaryRadius
 * @param {NSInteger} distanceFilter
 * @param {NSInteger} desiredAccuracy
 * @param {BOOL} debug
 * @param {NSString*} activityType
 * @param {BOOL} stopOnTerminate
 * @param {NSString*} url
 * @param {NSMutableDictionary*} httpHeaders
 */
- (BOOL) configure:(Config*)config error:(NSError * __autoreleasing *)outError
{
    NSLog(@"BackgroundGeolocationDelegate configure");
    _config = config;
    
    NSLog(@"%@", config);
    
    locationManager.pausesLocationUpdatesAutomatically = YES;
    locationManager.activityType = [_config decodeActivityType];
    locationManager.distanceFilter = _config.distanceFilter; // meters
    locationManager.desiredAccuracy = [_config decodeDesiredAccuracy];
    
    // ios 8 requires permissions to send local-notifications
    if (_config.isDebugging) {
        UIApplication *app = [UIApplication sharedApplication];
        if ([app respondsToSelector:@selector(registerUserNotificationSettings:)]) {
            [app registerUserNotificationSettings:[UIUserNotificationSettings settingsForTypes:UIUserNotificationTypeAlert|UIUserNotificationTypeBadge|UIUserNotificationTypeSound categories:nil]];
        }
    }
    
    return YES;
}

/**
 * Turn on background geolocation
 * in case of failure it calls error callback from configure method
 * may fire two callback when location services are disabled and when authorization failed
 */
- (BOOL) start:(NSError * __autoreleasing *)outError
{
    NSLog(@"BackgroundGeolocationDelegate will start: %d", isStarted);
    
    if (isStarted) {
        return NO;
    }
    
    NSUInteger authStatus;
    
    if ([CLLocationManager respondsToSelector:@selector(authorizationStatus)]) { // iOS 4.2+
        authStatus = [CLLocationManager authorizationStatus];
#ifdef __IPHONE_8_0
        if (authStatus == kCLAuthorizationStatusDenied) {
            NSDictionary *errorDictionary = @{ @"code": [NSNumber numberWithInt:PERMISSIONDENIED], @"message" : @LOCATION_DENIED };
            if (outError != NULL) {
                *outError = [NSError errorWithDomain:ErrorDomain code:PERMISSIONDENIED userInfo:errorDictionary];
            }
            
            return NO;
        }
        
        if (authStatus == kCLAuthorizationStatusRestricted) {
            NSDictionary *errorDictionary = @{ @"code": [NSNumber numberWithInt:PERMISSIONDENIED], @"message" : @LOCATION_RESTRICTED };
            if (outError != NULL) {
                *outError = [NSError errorWithDomain:ErrorDomain code:PERMISSIONDENIED userInfo:errorDictionary];
            }
            
            return NO;
        }
        
        // we do startUpdatingLocation even though we might not get permissions granted
        // we can stop later on when recieved callback on user denial
        // it's neccessary to start call startUpdatingLocation in iOS < 8.0 to show user prompt!
        
        if (authStatus == kCLAuthorizationStatusNotDetermined) {
            if ([locationManager respondsToSelector:@selector(requestAlwaysAuthorization)]) {  //iOS 8.0+
                NSLog(@"BackgroundGeolocationDelegate requestAlwaysAuthorization");
                [locationManager requestAlwaysAuthorization];
            }
        }
#endif
    }
    
    [self switchMode:FOREGROUND];
    isStarted = YES;
    
    return YES;
}

/**
 * Turn it off
 */
- (BOOL) stop:(NSError * __autoreleasing *)outError
{
    NSLog(@"BackgroundGeolocationDelegate stop");
    
    if (!isStarted) {
        return YES;
    }
    
    isStarted = NO;
    
    [self stopUpdatingLocation];
    [self stopMonitoringSignificantLocationChanges];
    [self stopMonitoringForRegion];
    
    return YES;
}

/**
 * Called by js to signify the end of a background-geolocation event
 */
- (BOOL) finish
{
    NSLog(@"BackgroundGeolocationDelegate finish");
    [self stopBackgroundTask];
    return YES;
}

/**
 * toggle between foreground and background operation mode
 */
- (void) switchMode:(BGOperationMode)mode
{
    NSLog(@"BackgroundGeolocationDelegate switchMode %lu", (unsigned long)mode);
    
    operationMode = mode;
    aquireStartTime = [NSDate date];
    
    if (_config.isDebugging) {
        AudioServicesPlaySystemSound (operationMode  == FOREGROUND ? paceChangeYesSound : paceChangeNoSound);
    }
    
    if (operationMode == FOREGROUND) {
        isAcquiringSpeed = YES;
        isAcquiringStationaryLocation = NO;
        [self stopMonitoringForRegion];
        [self stopMonitoringSignificantLocationChanges];
    } else {
        isAcquiringSpeed = NO;
        isAcquiringStationaryLocation = YES;
        [self startMonitoringSignificantLocationChanges];
    }
    
    // Crank up the GPS power temporarily to get a good fix on our current location
    [self stopUpdatingLocation];
    locationManager.distanceFilter = kCLDistanceFilterNone;
    locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation;
    [self startUpdatingLocation];
}

- (BOOL) isLocationEnabled
{
    BOOL locationServicesEnabledInstancePropertyAvailable = [locationManager respondsToSelector:@selector(locationServicesEnabled)]; // iOS 3.x
    BOOL locationServicesEnabledClassPropertyAvailable = [CLLocationManager respondsToSelector:@selector(locationServicesEnabled)]; // iOS 4.x
    
    if (locationServicesEnabledClassPropertyAvailable) { // iOS 4.x
        return [CLLocationManager locationServicesEnabled];
    } else if (locationServicesEnabledInstancePropertyAvailable) { // iOS 2.x, iOS 3.x
        return [(id)locationManager locationServicesEnabled];
    } else {
        return NO;
    }
}

- (void) showAppSettings
{
    BOOL canGoToSettings = (UIApplicationOpenSettingsURLString != NULL);
    if (canGoToSettings) {
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString]];
    }
}

- (void) showLocationSettings
{
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:@"prefs:root=LOCATION_SERVICES"]];
}

//- (void) watchLocationMode
//{
//    // TODO: yet to be implemented
//}

- (void) stopWatchingLocationMode
{
    // TODO: yet to be implemented
}

- (NSMutableDictionary*) getStationaryLocation
{
    if (stationaryRegion != nil) {
        CLLocationDistance radius = [stationaryRegion radius];
        CLLocationCoordinate2D coordinate = [stationaryRegion center];
        double timestamp = [stationarySince timeIntervalSince1970] * 1000;

        NSMutableDictionary *data = [[NSMutableDictionary alloc] init];
        [data setObject:[NSNumber numberWithDouble:coordinate.latitude] forKey:@"latitude"];
        [data setObject:[NSNumber numberWithDouble:coordinate.longitude] forKey:@"longitude"];
        [data setObject:[NSNumber numberWithDouble:radius] forKey:@"radius"];
        [data setObject:[NSNumber numberWithDouble:timestamp] forKey:@"time"];
        return data;
    }
    return nil;
}

- (NSArray<BackgroundLocation*>*) getLocations
{
    SQLiteLocationDAO* locationDAO = [SQLiteLocationDAO sharedInstance];
    NSArray* locations = [locationDAO getAllLocations];
    NSMutableArray* dictionaryLocations = [[NSMutableArray alloc] initWithCapacity:[locations count]];
    for (BackgroundLocation* location in locations) {
        [dictionaryLocations addObject:[location toDictionary]];
    }
    return dictionaryLocations;
}

- (BOOL) deleteLocation:(NSNumber*) locationId
{
    SQLiteLocationDAO* locationDAO = [SQLiteLocationDAO sharedInstance];
    return [locationDAO deleteLocation:locationId];
}

- (BOOL) deleteAllLocations
{
    SQLiteLocationDAO* locationDAO = [SQLiteLocationDAO sharedInstance];
    return [locationDAO deleteAllLocations];
}

- (void) queue:(BackgroundLocation*)location
{
    NSLog(@"BackgroundGeolocationDelegate queue %@", location);
    
    if (_config.url != nil && [location.type isEqual: @"current"]) {
        SQLiteLocationDAO* locationDAO = [SQLiteLocationDAO sharedInstance];
        location.id = [locationDAO persistLocation:location];
    }
    [locationQueue addObject:location];
    [self flushQueue];
}

- (void) flushQueue
{
    // Sanity-check the duration of last bgTask:  If greater than 30s, kill it.
    if (bgTask != UIBackgroundTaskInvalid) {
        if (-[lastBgTaskAt timeIntervalSinceNow] > 30.0) {
            NSLog(@"BackgroundGeolocationDelegate#flushQueue has to kill an out-standing background-task!");
            if (_config.isDebugging) {
                [self notify:@"Outstanding bg-task was force-killed"];
            }
            [self stopBackgroundTask];
        }
        return;
    }
    if ([locationQueue count] > 0) {
        // Create a background-task and delegate to Javascript for syncing location
        bgTask = [self createBackgroundTask];
        dispatch_async(dispatch_get_main_queue(), ^{
            BackgroundLocation *postLocation = [locationQueue lastObject];
            NSArray *locationsToPost = [[NSArray alloc] initWithObjects:[postLocation toDictionary], nil];
            if ([self postJSON:locationsToPost]) {
                // clear queue
                SQLiteLocationDAO* locationDAO = [SQLiteLocationDAO sharedInstance];
                [locationDAO deleteLocation:postLocation.id];
            }
            
            // send first queued location to client js (to mimic FIFO)
            BackgroundLocation *location = [locationQueue firstObject];
            [self sync:location];
            [locationQueue removeObject:location];
        });
    }
}

- (UIBackgroundTaskIdentifier) createBackgroundTask
{
    lastBgTaskAt = [NSDate date];
    return [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:^{
        [self stopBackgroundTask];
    }];
}

- (void) stopBackgroundTask
{
    UIApplication *app = [UIApplication sharedApplication];
    if (bgTask != UIBackgroundTaskInvalid) {
        [app endBackgroundTask:bgTask];
        bgTask = UIBackgroundTaskInvalid;
    }
    [self flushQueue];
}

/**
 * We are running in the background if this is being executed.
 * We can't assume normal network access.
 * bgTask is defined as an instance variable of type UIBackgroundTaskIdentifier
 */
- (void) sync:(BackgroundLocation*)location
{
    NSLog(@"BackgroundGeolocationDelegate#sync %@", location);
    if (_config.isDebugging) {
        [self notify:[NSString stringWithFormat:@"Location update: %s\nSPD: %0.0f | DF: %ld | ACY: %0.0f",
            ((operationMode == FOREGROUND) ? "FG" : "BG"),
            [location.speed doubleValue],
            (long) locationManager.distanceFilter,
            [location.accuracy doubleValue]
        ]];
        
        AudioServicesPlaySystemSound (locationSyncSound);
    }
    
    // Build a resultset for javascript callback.
    if ([location.type isEqualToString:@"stationary"]) {
        [self fireStationaryRegionListeners:[location toDictionary]];
    } else if ([location.type isEqualToString:@"current"]) {
        self.onLocationChanged([location toDictionary]);
    } else {
        NSLog(@"BackgroundGeolocationDelegate#sync could not determine location_type.");
        [self stopBackgroundTask];
    }
}

- (void) fireStationaryRegionListeners:(NSMutableDictionary*)data
{
    NSLog(@"BackgroundGeolocationDelegate#fireStationaryRegionListener");
    // Any javascript stationaryRegion event-listeners?
    [data setObject:[NSNumber numberWithDouble:_config.stationaryRadius] forKey:@"radius"];
    self.onStationaryChanged(data);
//    [self stopBackgroundTask];
}

- (void) locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations
{
    NSLog(@"BackgroundGeolocationDelegate didUpdateLocations (operationMode: %lu)", (unsigned long)operationMode);
    
    locationError = nil;
    
    if (operationMode == FOREGROUND && !isUpdatingLocation) {
        [self startUpdatingLocation];
    }
    
    if (operationMode == BACKGROUND && !isAcquiringStationaryLocation && !stationaryRegion) {
        // Perhaps our GPS signal was interupted, re-acquire a stationaryLocation now.
        [self switchMode:operationMode];
    }
    
    for (CLLocation *location in locations) {
        BackgroundLocation *bgloc = [BackgroundLocation fromCLLocation:location];
        bgloc.type = @"current";

        // test the age of the location measurement to determine if the measurement is cached
        // in most cases you will not want to rely on cached measurements
        NSLog(@"Location age %f", [bgloc locationAge]);
        if ([bgloc locationAge] > maxLocationAgeInSeconds || ![bgloc isValid]) {
            continue;
        }
        
        if (lastLocation == nil) {
            lastLocation = bgloc;
            continue;
        }
        
        if ([bgloc isBetterLocation:lastLocation]) {
            NSLog(@"Better location found: %@", bgloc);
            lastLocation = bgloc;
        }
    }
    
    if (lastLocation == nil) {
        return;
    }
    
    // test the measurement to see if it is more accurate than the previous measurement
    if (isAcquiringStationaryLocation) {
        NSLog(@"Acquiring stationary location, accuracy: %@", lastLocation.accuracy);
        if (_config.isDebugging) {
            AudioServicesPlaySystemSound (acquiringLocationSound);
        }

        if ([lastLocation.accuracy doubleValue] <= [[NSNumber numberWithInteger:_config.desiredAccuracy] doubleValue]) {
            NSLog(@"BackgroundGeolocationDelegate found most accurate stationary before timeout");
        } else if (-[aquireStartTime timeIntervalSinceNow] < maxLocationWaitTimeInSeconds) {
            // we still have time to aquire better location
            return;
        }

        isAcquiringStationaryLocation = NO;
        [self stopUpdatingLocation]; //saving power while monitoring region

        BackgroundLocation *stationaryLocation = [lastLocation copy];
        stationaryLocation.type = @"stationary";
        [self startMonitoringStationaryRegion:stationaryLocation];
        // fire onStationary @event for Javascript.
        [self queue:stationaryLocation];
    } else if (isAcquiringSpeed) {
        if (_config.isDebugging) {
            AudioServicesPlaySystemSound (acquiringLocationSound);
        }

        if ([lastLocation.accuracy doubleValue] <= [[NSNumber numberWithInteger:_config.desiredAccuracy] doubleValue]) {
            NSLog(@"BackgroundGeolocationDelegate found most accurate location before timeout");
        } else if (-[aquireStartTime timeIntervalSinceNow] < maxLocationWaitTimeInSeconds) {
            // we still have time to aquire better location
            return;
        }

        if (_config.isDebugging) {
            [self notify:@"Aggressive monitoring engaged"];
        }

        // We should have a good sample for speed now, power down our GPS as configured by user.
        isAcquiringSpeed = NO;
        locationManager.desiredAccuracy = _config.desiredAccuracy;
        locationManager.distanceFilter = [self calculateDistanceFilter:[lastLocation.speed floatValue]];
        [self startUpdatingLocation];
        
    } else if (operationMode == FOREGROUND) {
        // Adjust distanceFilter incrementally based upon current speed
        float newDistanceFilter = [self calculateDistanceFilter:[lastLocation.speed floatValue]];
        if (newDistanceFilter != locationManager.distanceFilter) {
            NSLog(@"BackgroundGeolocationDelegate updated distanceFilter, new: %f, old: %f", newDistanceFilter, locationManager.distanceFilter);
            locationManager.distanceFilter = newDistanceFilter;
            [self startUpdatingLocation];
        }
    } else if ([self locationIsBeyondStationaryRegion:lastLocation]) {
        if (_config.isDebugging) {
            [self notify:@"Manual stationary exit-detection"];
        }
        [self switchMode:operationMode];
    }
    
    [self queue:lastLocation];
}

/**
 * Called when user exits their stationary radius (ie: they walked ~50m away from their last recorded location.
 *
 */
- (void) locationManager:(CLLocationManager *)manager didExitRegion:(CLCircularRegion *)region
{
    CLLocationDistance radius = [region radius];
    CLLocationCoordinate2D coordinate = [region center];

    NSLog(@"BackgroundGeolocationDelegate didExitRegion {%f,%f,%f}", coordinate.latitude, coordinate.longitude, radius);
    if (_config.isDebugging) {
        AudioServicesPlaySystemSound (exitRegionSound);
        [self notify:@"Exit stationary region"];
    }
    [self switchMode:operationMode];
}

- (void) locationManagerDidPauseLocationUpdates:(CLLocationManager *)manager
{
    NSLog(@"BackgroundGeolocationDelegate location updates paused");
    if (_config.isDebugging) {
        [self notify:@"Location updates paused"];
    }
}

- (void) locationManagerDidResumeLocationUpdates:(CLLocationManager *)manager
{
    NSLog(@"BackgroundGeolocationDelegate location updates resumed");
    if (_config.isDebugging) {
        [self notify:@"Location updates resumed b"];
    }
}

- (void) locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error
{
    NSLog(@"BackgroundGeolocationDelegate didFailWithError: %@", error);
    if (_config.isDebugging) {
        AudioServicesPlaySystemSound (locationErrorSound);
        [self notify:[NSString stringWithFormat:@"Location error: %@", error.localizedDescription]];
    }
    
    locationError = error;
    
    switch(error.code) {
        case kCLErrorLocationUnknown:
        case kCLErrorNetwork:
        case kCLErrorRegionMonitoringDenied:
        case kCLErrorRegionMonitoringSetupDelayed:
        case kCLErrorRegionMonitoringResponseDelayed:
        case kCLErrorGeocodeFoundNoResult:
        case kCLErrorGeocodeFoundPartialResult:
        case kCLErrorGeocodeCanceled:
            break;
        case kCLErrorDenied:
            [self stop:nil];
            break;
    }
    
    self.onError(error);
    
}

- (void) locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status
{
    NSLog(@"BackgroundGeolocationDelegate didChangeAuthorizationStatus %u", status);
    if (_config.isDebugging) {
        [self notify:[NSString stringWithFormat:@"Authorization status changed %u", status]];
    }
    // TODO: add logic to start/stop service base on user authorization
}

- (void) stopUpdatingLocation
{
    if (isUpdatingLocation) {
        [locationManager stopUpdatingLocation];
        isUpdatingLocation = NO;
    }
}

- (void) startUpdatingLocation
{
    if (!isUpdatingLocation) {
        [locationManager startUpdatingLocation];
        isUpdatingLocation = YES;
    }
}

- (void) startMonitoringSignificantLocationChanges
{
    [locationManager startMonitoringSignificantLocationChanges];
}

- (void) stopMonitoringSignificantLocationChanges
{
    [locationManager stopMonitoringSignificantLocationChanges];
}

/**
 * Creates a new circle around user and region-monitors it for exit
 */
- (void) startMonitoringStationaryRegion:(BackgroundLocation*)location {
    CLLocationCoordinate2D coord = [location coordinate];
    NSLog(@"BackgroundGeolocationDelegate startMonitoringStationaryRegion {%f,%f,%ld}", coord.latitude, coord.longitude, (long)_config.stationaryRadius);
    
    if (_config.isDebugging) {
        AudioServicesPlaySystemSound (acquiredLocationSound);
        [self notify:[NSString stringWithFormat:@"Monitoring region {%f,%f,%ld}", location.coordinate.latitude, location.coordinate.longitude, (long)_config.stationaryRadius]];
    }
    
    [self stopMonitoringForRegion];
    stationaryRegion = [[CLCircularRegion alloc] initWithCenter: coord radius:_config.stationaryRadius identifier:@"BackgroundGeolocationDelegate stationary region"];
    stationaryRegion.notifyOnExit = YES;
    [locationManager startMonitoringForRegion:stationaryRegion];
    stationarySince = [NSDate date];
}

- (void) stopMonitoringForRegion
{
    if (stationaryRegion != nil) {
        [locationManager stopMonitoringForRegion:stationaryRegion];
        stationaryRegion = nil;
        stationarySince = nil;
    }
}

/**
 * Calculates distanceFilter by rounding speed to nearest 5 and multiplying by 10.  Clamped at 1km max.
 */
- (float) calculateDistanceFilter:(float)speed
{
    float newDistanceFilter = _config.distanceFilter;
    if (speed < 100) {
        // (rounded-speed-to-nearest-5) / 2)^2
        // eg 5.2 becomes (5/2)^2
        newDistanceFilter = pow((5.0 * floorf(fabsf(speed) / 5.0 + 0.5f)), 2) + _config.distanceFilter;
    }
    return (newDistanceFilter < 1000) ? newDistanceFilter : 1000;
}

/**
 * Manual stationary location his-testing.  This seems to help stationary-exit detection in some places where the automatic geo-fencing doesn't
 */
- (BOOL) locationIsBeyondStationaryRegion:(BackgroundLocation*)location
{
    CLLocationCoordinate2D regionCenter = [stationaryRegion center];
    BOOL containsCoordinate = [stationaryRegion containsCoordinate:[location coordinate]];

    NSLog(@"BackgroundGeolocationDelegate location {%@,%@} region {%f,%f,%f} contains: %d",
          location.latitude, location.longitude, regionCenter.latitude, regionCenter.longitude,
          [stationaryRegion radius], containsCoordinate);

    return !containsCoordinate;
}

- (void) notify:(NSString*)message
{
    localNotification.fireDate = [NSDate date];
    localNotification.alertBody = message;
    [[UIApplication sharedApplication] scheduleLocalNotification:localNotification];
}

- (BOOL) postJSON:(NSArray*)array
{
    NSError *e = nil;
    //    NSArray *jsonArray = [NSJSONSerialization JSONObjectWithData: data options: NSJSONReadingMutableContainers error: &e];
    NSData *data = [NSJSONSerialization dataWithJSONObject:array options:0 error:&e];
    if (!data) {
        return false;
    }
    
    NSString *jsonStr = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:_config.url]];
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPMethod:@"POST"];
    if (_config.httpHeaders) {
        for(id key in _config.httpHeaders) {
            id value = [_config.httpHeaders objectForKey:key];
            [request addValue:value forHTTPHeaderField:key];
        }
    }
    [request setHTTPBody:[jsonStr dataUsingEncoding:NSUTF8StringEncoding]];
    
    // Create url connection and fire request
    NSHTTPURLResponse* urlResponse = nil;
    NSError *error = nil;
    NSData *response = [NSURLConnection sendSynchronousRequest:request returningResponse:&urlResponse error:&error];
    
    if (error == nil && [urlResponse statusCode] == 200) {
        return YES;
    }
    
    return NO;
}

/**@
 * If you don't stopMonitoring when application terminates, the app will be awoken still when a
 * new location arrives, essentially monitoring the user's location even when they've killed the app.
 * Might be desirable in certain apps.
 */
- (void) onAppTerminate
{
    if (_config.stopOnTerminate) {
        NSLog(@"BackgroundGeolocationDelegate is stopping on app terminate.");
        [self stop:nil];
    } else {
        [self switchMode:BACKGROUND];
    }
}

- (void) dealloc
{
    locationManager.delegate = nil;
    //    [super dealloc];
}

@end
