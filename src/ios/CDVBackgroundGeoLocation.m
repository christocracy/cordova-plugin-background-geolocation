////
//  CDVBackgroundGeoLocation
//
//  Created by Chris Scott <chris@transistorsoft.com> on 2013-06-15
//
#import "CDVLocation.h"
#import "CDVBackgroundGeoLocation.h"
#import <Cordova/CDVJSON.h>

// Debug sounds for bg-geolocation life-cycle events.
// http://iphonedevwiki.net/index.php/AudioServices
#define exitRegionSound         1005
#define locationSyncSound       1004
#define paceChangeYesSound      1110
#define paceChangeNoSound       1112
#define acquiringLocationSound  1103
#define acquiredLocationSound   1052

@implementation CDVBackgroundGeoLocation {
    BOOL isDebugging;
    BOOL enabled;
    NSString *token;
    NSString *url;
    NSString *syncCallbackId;
    UIBackgroundTaskIdentifier bgTask;
    NSTimer *backgroundTimer;
    
    BOOL isMoving;
    
    NSNumber *maxBackgroundHours;
    CLLocationManager *locationManager;
    CDVLocationData *locationData;
    NSMutableArray *locationCache;
    NSDate *suspendedAt;
    
    BOOL isAcquiringStationaryLocation;
    CLLocation *stationaryLocation;
    NSInteger stationaryLocationAttempts;
    NSInteger maxStationaryLocationAttempts;
    CLCircularRegion *stationaryRegion;
    
    BOOL isAcquiringSpeed;
    NSInteger speedAcquisitionAttempts;
    NSInteger maxSpeedAcquistionAttempts;
    
    NSInteger stationaryRadius;
    NSInteger distanceFilter;
    NSInteger locationTimeout;
    NSInteger desiredAccuracy;
}

- (void)pluginInitialize
{
    // background location cache, for when no network is detected.
    locationCache = [NSMutableArray array];
    locationManager = [[CLLocationManager alloc] init];
    locationManager.delegate = self;
    
    stationaryLocation = nil;
    stationaryRegion = nil;
    isDebugging = NO;
    
    maxStationaryLocationAttempts   = 4;
    maxSpeedAcquistionAttempts      = 3;
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onSuspend:) name:UIApplicationDidEnterBackgroundNotification object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onResume:) name:UIApplicationWillEnterForegroundNotification object:nil];
}
/**
 * configure plugin
 * @param {String} token
 * @param {String} url
 * @param {Number} stationaryRadius
 * @param {Number} distanceFilter
 * @param {Number} locationTimeout
 */
- (void) configure:(CDVInvokedUrlCommand*)command
{
    // in iOS, we call to javascript for HTTP now so token and url should be @deprecated until Android calls out to javascript.
    token = [command.arguments objectAtIndex: 0];
    url = [command.arguments objectAtIndex: 1];
    
    // Params.
    stationaryRadius    = [[command.arguments objectAtIndex: 2] intValue];
    distanceFilter      = [[command.arguments objectAtIndex: 3] intValue];
    locationTimeout     = [[command.arguments objectAtIndex: 4] intValue];
    desiredAccuracy     = [self translateDesiredAccuracy:[[command.arguments objectAtIndex: 5] intValue]];
    isDebugging         = [[command.arguments objectAtIndex: 6] boolValue];
    
    syncCallbackId = command.callbackId;
    
    // Set a movement threshold for new events.
    locationManager.activityType = CLActivityTypeOther;
    locationManager.pausesLocationUpdatesAutomatically = YES;
    locationManager.distanceFilter = distanceFilter; // meters
    locationManager.desiredAccuracy = desiredAccuracy;
    
    NSLog(@"CDVBackgroundGeoLocation configure");
    NSLog(@"  - token: %@", token);
    NSLog(@"  - url: %@", url);
    NSLog(@"  - distanceFilter: %ld", (long)distanceFilter);
    NSLog(@"  - stationaryRadius: %ld", (long)stationaryRadius);
    NSLog(@"  - locationTimeout: %ld", (long)locationTimeout);
    NSLog(@"  - desiredAccuracy: %ld", (long)desiredAccuracy);
    NSLog(@"  - debug: %hhd", isDebugging);
}
- (void) setConfig:(CDVInvokedUrlCommand*)command
{
    NSLog(@"- CDVBackgroundGeoLocation setConfig");
    NSDictionary *config = [command.arguments objectAtIndex:0];
    
    if (config[@"desiredAccuracy"]) {
        desiredAccuracy = [self translateDesiredAccuracy:[config[@"desiredAccuracy"] floatValue]];
        NSLog(@"    desiredAccuracy: %@", config[@"desiredAccuracy"]);
    }
    if (config[@"stationaryRadius"]) {
        stationaryRadius = [config[@"stationaryRadius"] intValue];
        NSLog(@"    stationaryRadius: %@", config[@"stationaryRadius"]);
    }
    if (config[@"distanceFilter"]) {
        distanceFilter = [config[@"distanceFilter"] intValue];
        NSLog(@"    distanceFilter: %@", config[@"distanceFilter"]);
    }
    if (config[@"timeout"]) {
        locationTimeout = [config[@"timeout"] intValue];
        NSLog(@"    locationTimeout: %@", config[@"timeout"]);
    }
    
    CDVPluginResult* result = nil;
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

-(NSInteger)translateDesiredAccuracy:(NSInteger)accuracy
{
    switch (accuracy) {
        case 1000:
            accuracy = kCLLocationAccuracyKilometer;
            break;
        case 100:
            accuracy = kCLLocationAccuracyHundredMeters;
            break;
        case 10:
            accuracy = kCLLocationAccuracyNearestTenMeters;
            break;
        case 0:
            accuracy = kCLLocationAccuracyBest;
            break;
        default:
            accuracy = kCLLocationAccuracyHundredMeters;
    }
    return accuracy;
}

/**
 * Turn on background geolocation
 */
- (void) start:(CDVInvokedUrlCommand*)command
{
    enabled = YES;
    UIApplicationState state = [[UIApplication sharedApplication] applicationState];
    
    NSLog(@"- CDVBackgroundGeoLocation start (background? %d)", state);
    
    if (state == UIApplicationStateBackground) {
        [self setPace:isMoving];
    }
    CDVPluginResult* result = nil;
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}
/**
 * Turn it off
 */
- (void) stop:(CDVInvokedUrlCommand*)command
{
    NSLog(@"- CDVBackgroundGeoLocation stop");
    enabled = NO;
    [locationManager stopUpdatingLocation];
    [locationManager stopMonitoringSignificantLocationChanges];
    if (stationaryRegion != nil) {
        [locationManager stopMonitoringForRegion:stationaryRegion];
        stationaryRegion = nil;
    }
    CDVPluginResult* result = nil;
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    
}
/**
 * Change pace to moving/stopped
 * @param {Boolean} isMoving
 */
- (void) onPaceChange:(CDVInvokedUrlCommand *)command
{
    isMoving = [[command.arguments objectAtIndex: 0] boolValue];
    NSLog(@"- CDVBackgroundGeoLocation onPaceChange %hhd", isMoving);
    UIApplicationState state = [[UIApplication sharedApplication] applicationState];
    if (state == UIApplicationStateBackground) {
        [self setPace:isMoving];
    }
}
/**
 * Fetches current stationaryLocation
 */
- (void) getStationaryLocation:(CDVInvokedUrlCommand *)command
{
    NSLog(@"- CDVBackgroundGeoLocation getStationaryLocation");
    
    NSMutableDictionary* returnInfo;
    // Build a resultset for javascript callback.
    CDVPluginResult* result = nil;
    
    if (stationaryLocation) {
        returnInfo = [NSMutableDictionary dictionaryWithCapacity:9];
        
        NSNumber* timestamp = [NSNumber numberWithDouble:([stationaryLocation.timestamp timeIntervalSince1970] * 1000)];
        [returnInfo setObject:timestamp forKey:@"timestamp"];
        [returnInfo setObject:[NSNumber numberWithDouble:stationaryLocation.speed] forKey:@"velocity"];
        [returnInfo setObject:[NSNumber numberWithDouble:stationaryLocation.verticalAccuracy] forKey:@"altitudeAccuracy"];
        [returnInfo setObject:[NSNumber numberWithDouble:stationaryLocation.horizontalAccuracy] forKey:@"accuracy"];
        [returnInfo setObject:[NSNumber numberWithDouble:stationaryLocation.course] forKey:@"heading"];
        [returnInfo setObject:[NSNumber numberWithDouble:stationaryLocation.altitude] forKey:@"altitude"];
        [returnInfo setObject:[NSNumber numberWithDouble:stationaryLocation.coordinate.latitude] forKey:@"latitude"];
        [returnInfo setObject:[NSNumber numberWithDouble:stationaryLocation.coordinate.longitude] forKey:@"longitude"];
        
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnInfo];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:NO];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}
/**
 * Called by js to signify the end of a background-geolocation event
 */
-(void) finish:(CDVInvokedUrlCommand*)command
{
    NSLog(@"- CDVBackgroundGeoLocation finish");
    [self stopBackgroundTask];
}

/**
 * Suspend.  Turn on passive location services
 */
-(void) onSuspend:(NSNotification *) notification
{
    NSLog(@"- CDVBackgroundGeoLocation suspend (enabled? %hhd)", enabled);
    suspendedAt = [NSDate date];
    
    if (enabled) {
        [self setPace: isMoving];
    }
}
/**@
 * Resume.  Turn background off
 */
-(void) onResume:(NSNotification *) notification
{
    NSLog(@"- CDVBackgroundGeoLocation resume");
    if (enabled) {
        [locationManager stopMonitoringSignificantLocationChanges];
        [locationManager stopUpdatingLocation];
    }
}

-(void) locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations
{
    NSLog(@"- CDVBackgroundGeoLocation didUpdateLocations (isMoving: %hhd)", isMoving);
    CLLocation *newLocation = [locations lastObject];
    
    if (!isMoving && !isAcquiringStationaryLocation && !stationaryLocation) {
        // Perhaps our GPS signal was interupted, re-acquire a stationaryLocation now.
        [self setPace: NO];
    }
    
    // Handle location updates as normal, code omitted for brevity.
    // The omitted code should determine whether to reject the location update for being too
    // old, too close to the previous one, too inaccurate and so forth according to your own
    // application design.
    [locationCache addObjectsFromArray:locations];
    
    UIApplication *app = [UIApplication sharedApplication];
    
    // test the age of the location measurement to determine if the measurement is cached
    // in most cases you will not want to rely on cached measurements
    NSTimeInterval locationAge = -[newLocation.timestamp timeIntervalSinceNow];
    
    if (locationAge > 5.0) return;
    
    // test that the horizontal accuracy does not indicate an invalid measurement
    if (newLocation.horizontalAccuracy < 0) return;
    
    // test the measurement to see if it is more accurate than the previous measurement
    if (isAcquiringStationaryLocation) {
        NSLog(@"- Acquiring stationary location, accuracy: %f", newLocation.horizontalAccuracy);
        if (![self isBestStationaryLocation:newLocation]) {
            if (isDebugging) {
                AudioServicesPlaySystemSound (acquiringLocationSound);
            }
            return;
        }
        if (isDebugging) {
            AudioServicesPlaySystemSound (acquiredLocationSound);
        }
        [self startMonitoringStationaryRegion:stationaryLocation];
    }
    
    // Bail out if there's already a background-task in-effect.
    if (bgTask != UIBackgroundTaskInvalid) {
        NSLog(@" Abort:  found existing background-task");
        return;
    }

    bgTask = [app beginBackgroundTaskWithExpirationHandler:^{
        [self stopBackgroundTask];
    }];
    
    [self.commandDelegate runInBackground:^{
        [self sync:newLocation];
    }];
    
    // Adjust distanceFilter incrementally based upon current speed
    if (isMoving)
    {
        float newDistanceFilter = [self calculateDistanceFilter:newLocation.speed];
        if (newDistanceFilter != locationManager.distanceFilter) {
            NSLog(@"- CDVBackgroundGeoLocation updated distanceFilter, new: %f, old: %f", newDistanceFilter, locationManager.distanceFilter);
            [locationManager stopUpdatingLocation];
            locationManager.distanceFilter = newDistanceFilter;
            [locationManager startUpdatingLocation];
        }
    }
}
- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error
{
    NSLog(@"- CDVBackgroundGeoLocation locationManager failed:  %@", error);
    [locationManager stopUpdatingLocation];
    
    isMoving = NO;
    isAcquiringStationaryLocation = NO;
    stationaryLocation = nil;
    
    [locationManager startMonitoringSignificantLocationChanges];
}
-(BOOL) isBestStationaryLocation:(CLLocation*)location {
    stationaryLocationAttempts++;
    if (stationaryLocationAttempts == maxStationaryLocationAttempts) {
        return true;
    }
    if (stationaryLocation == nil || stationaryLocation.horizontalAccuracy > location.horizontalAccuracy) {
        // store the location as the "best effort"
        stationaryLocation = location;
        if (location.horizontalAccuracy <= 5.0) {
            return true;
        }
    }
    return false;
}

/**
 * Calculates distanceFilter by rounding speed to nearest 5 and multiplying by 10.
 */
-(float) calculateDistanceFilter:(float)speed
{
    float newDistanceFilter = distanceFilter;
    if (isAcquiringSpeed) {
        if (++speedAcquisitionAttempts == maxSpeedAcquistionAttempts) {
            // We should have a good sample for speed now, power down our GPS as configured by user.
            isAcquiringSpeed = NO;
            [locationManager stopUpdatingLocation];
            locationManager.desiredAccuracy = desiredAccuracy;
            [locationManager startUpdatingLocation];
        } else {
            return newDistanceFilter;
        }
    }
    if (speed > 3.0 && speed < 100) {
        // (rounded-speed-to-nearest-5) / 2)^2
        // eg 5.2 becomes (5/2)^2
        newDistanceFilter = pow((5.0 * floorf(speed / 5.0 + 0.5f)), 2) + distanceFilter;
    }
    return (newDistanceFilter < 1000) ? newDistanceFilter : 1000;
}


/**
 * We are running in the background if this is being executed.
 * We can't assume normal network access.
 * bgTask is defined as an instance variable of type UIBackgroundTaskIdentifier
 */
-(void) sync:(CLLocation*)location
{
    NSLog(@" position: %f,%f, speed: %f", location.coordinate.latitude, location.coordinate.longitude, location.speed);
    if (isDebugging) {
        AudioServicesPlaySystemSound (locationSyncSound);
    }
    
    NSMutableDictionary* returnInfo = [NSMutableDictionary dictionaryWithCapacity:8];
    NSNumber* timestamp = [NSNumber numberWithDouble:([location.timestamp timeIntervalSince1970] * 1000)];
    [returnInfo setObject:timestamp forKey:@"timestamp"];
    [returnInfo setObject:[NSNumber numberWithDouble:location.speed] forKey:@"velocity"];
    [returnInfo setObject:[NSNumber numberWithDouble:location.verticalAccuracy] forKey:@"altitudeAccuracy"];
    [returnInfo setObject:[NSNumber numberWithDouble:location.horizontalAccuracy] forKey:@"accuracy"];
    [returnInfo setObject:[NSNumber numberWithDouble:location.course] forKey:@"heading"];
    [returnInfo setObject:[NSNumber numberWithDouble:location.altitude] forKey:@"altitude"];
    [returnInfo setObject:[NSNumber numberWithDouble:location.coordinate.latitude] forKey:@"latitude"];
    [returnInfo setObject:[NSNumber numberWithDouble:location.coordinate.longitude] forKey:@"longitude"];
    
    // Build a resultset for javascript callback.
    CDVPluginResult* result = nil;
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnInfo];
    [result setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:result callbackId:syncCallbackId];
}

- (void) stopBackgroundTask
{
    UIApplication *app = [UIApplication sharedApplication];
    NSLog(@"- CDVBackgroundGeoLocation stopBackgroundTask (remaining t: %f)", app.backgroundTimeRemaining);
    if (bgTask != UIBackgroundTaskInvalid)
    {
        [app endBackgroundTask:bgTask];
        bgTask = UIBackgroundTaskInvalid;
    }
}
/**
 * Called when user exits their stationary radius (ie: they walked ~50m away from their last recorded location.
 * - turn on more aggressive location monitoring.
 */
- (void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region
{
    NSLog(@"- CDVBackgroundGeoLocation exit region");
    if (isDebugging) {
        AudioServicesPlaySystemSound (exitRegionSound);
    }
    [self setPace:YES];
}
/**
 * 1. turn off std location services
 * 2. turn on significantChanges API
 * 3. create a region and start monitoring exits.
 * 4. clear locationCache
 */
- (void)locationManagerDidPauseLocationUpdates:(CLLocationManager *)manager
{
    NSLog(@"- CDVBackgroundGeoLocation paused location updates");
    [self setPace:NO];
}
/**
 * 1. Turn off significantChanges ApI
 * 2. turn on std. location services
 * 3. nullify stationaryRegion
 */
- (void)locationManagerDidResumeLocationUpdates:(CLLocationManager *)manager
{
    NSLog(@"- CDVBackgroundGeoLocation resume location updates");
    [self setPace:YES];
}
/**
 * toggle passive or aggressive location services
 */
- (void)setPace:(BOOL)value
{
    NSLog(@"- CDVBackgroundGeoLocation setPace %d, stationaryRegion? %d", value, stationaryRegion!=nil);
    isMoving = value;
    if (isDebugging) {
        AudioServicesPlaySystemSound (isMoving ? paceChangeYesSound : paceChangeNoSound);
    }
    if (isMoving) {
        if (stationaryRegion != nil) {
            [locationManager stopMonitoringForRegion:stationaryRegion];
            stationaryRegion = nil;
        }
        isAcquiringSpeed = YES;
        speedAcquisitionAttempts = 0;
        
        [locationManager stopMonitoringSignificantLocationChanges];
        locationManager.distanceFilter = distanceFilter;
        // Power-up the GPS temporarily until we get a good speed sample.
        locationManager.desiredAccuracy = kCLLocationAccuracyBest;
        [locationManager startUpdatingLocation];
    } else {
        // Crank up the GPS power temporarily to get a good fix on our current staionary location in order to set up region-monitoring.
        stationaryLocation = nil;
        isAcquiringStationaryLocation = YES;
        stationaryLocationAttempts = 0;
        
        locationManager.distanceFilter = kCLDistanceFilterNone;
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation;
        [locationManager startUpdatingLocation];
    }
}
/**
 * Creates a new circle around user and region-monitors it for exit
 */
- (void) startMonitoringStationaryRegion:(CLLocation*)location {
    CLLocationCoordinate2D coord = [location coordinate];
    
    NSLog(@"- CDVBackgroundGeoLocation createStationaryRegion (%f,%f)", coord.latitude, coord.longitude);
    
    if (stationaryRegion != nil) {
        [locationManager stopMonitoringForRegion:stationaryRegion];
    }

    isAcquiringStationaryLocation = NO;
    [locationManager stopUpdatingLocation];
    locationManager.distanceFilter = distanceFilter;
    locationManager.desiredAccuracy = desiredAccuracy;
    [locationManager startMonitoringSignificantLocationChanges];
    
    stationaryRegion = [[CLCircularRegion alloc] initWithCenter: coord radius:stationaryRadius identifier:@"BackgroundGeoLocation stationary region"];
    stationaryRegion.notifyOnExit = YES;
    [locationManager startMonitoringForRegion:stationaryRegion];
}

// If you don't stopMonitorying when application terminates, the app will be awoken still when a
// new location arrives, essentially monitoring the user's location even when they've killed the app.
// Might be desirable in certain apps.
- (void)applicationWillTerminate:(UIApplication *)application {
    [locationManager stopMonitoringSignificantLocationChanges];
    [locationManager stopUpdatingLocation];
    if (stationaryRegion != nil) {
        [locationManager stopMonitoringForRegion:stationaryRegion];
    }
}

- (void)dealloc
{
    locationManager.delegate = nil;
}

@end
