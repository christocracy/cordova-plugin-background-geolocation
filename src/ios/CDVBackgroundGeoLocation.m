////
//  CDVBackgroundGeoLocation
//
//  Created by Chris Scott <chris@transistorsoft.com> on 2013-06-15
//
#import "CDVLocation.h"
#import "CDVBackgroundGeoLocation.h"
#import <Cordova/CDVJSON.h>

@implementation CDVBackgroundGeoLocation {
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

    NSDate *suspendedAt;
    
    CLCircularRegion *myRegion;
    NSInteger stationaryRadius;
    NSInteger distanceFilter;
    NSInteger locationTimeout;
}

- (void)pluginInitialize
{
    // background location cache, for when no network is detected.
    locationManager = [[CLLocationManager alloc] init];
    locationManager.delegate = self;
    
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
    
    // Location filtering.
    stationaryRadius = [[command.arguments objectAtIndex: 2] intValue];
    distanceFilter = [[command.arguments objectAtIndex: 3] intValue];
    locationTimeout = [[command.arguments objectAtIndex: 4] intValue];
    syncCallbackId = command.callbackId;
    
    // Set a movement threshold for new events.
    locationManager.activityType = CLActivityTypeOther;
    locationManager.pausesLocationUpdatesAutomatically = YES;
    locationManager.desiredAccuracy = kCLLocationAccuracyKilometer;
    locationManager.distanceFilter = distanceFilter; // meters
    
    myRegion = nil;
    
    NSLog(@"CDVBackgroundGeoLocation configure");
    NSLog(@"  - token: %@", token);
    NSLog(@"  - url: %@", url);
    NSLog(@"  - distanceFilter: %ld", (long)distanceFilter);
    NSLog(@"  - stationaryRadius: %ld", (long)stationaryRadius);
    NSLog(@"  - locationTimeout: %ld", (long)locationTimeout);
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
 * Change stationary radius
 */
- (void) setStationaryRadius:(CDVInvokedUrlCommand *)command
{
    stationaryRadius = [[command.arguments objectAtIndex: 0] intValue];
    NSLog(@"- CDVBackgroundGeoLocation setStationaryRadius %d", stationaryRadius);
    
    CDVPluginResult* result = nil;
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}
/**
 * Change distance-filter
 */
- (void) setDistanceFilter:(CDVInvokedUrlCommand *)command
{
    distanceFilter = [[command.arguments objectAtIndex: 0] intValue];
    NSLog(@"- CDVBackgroundGeoLocation setDistanceFilter %d", distanceFilter);
    
    CDVPluginResult* result = nil;
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
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
    NSLog(@"- CDVBackgroundGeoLocation suspend");
    suspendedAt = [NSDate date];
    
    if (enabled) {
        [self setPace: isMoving];
    }
}
/**
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
    NSLog(@"- CDVBackgroundGeoLocation didUpdateLocations");
    
    UIApplication *app = [UIApplication sharedApplication];
    
    // Bail out if there's already a background-task in-effect.
    if (bgTask != UIBackgroundTaskInvalid) {
        NSLog(@" Abort:  found existing background-task");
        return;
    }

    bgTask = [app beginBackgroundTaskWithExpirationHandler:^{
        [self stopBackgroundTask];
    }];
    
    [self.commandDelegate runInBackground:^{
        [self sync:[locations lastObject]];
    }];
}
/**
 * We are running in the background if this is being executed.
 * We can't assume normal network access.
 * bgTask is defined as an instance variable of type UIBackgroundTaskIdentifier
 */
-(void) sync:(CLLocation *)location
{
    // Fetch last recorded location

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
    if (bgTask != UIBackgroundTaskInvalid)
    {
        NSLog(@"- CDVBackgroundGeoLocation stopBackgroundTask (remaining t: %f)", app.backgroundTimeRemaining);
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
 * 3. nullify myRegion
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
    NSLog(@"- CDVBackgroundGeoLocation setPace %d", value);
    isMoving = value;
    if (myRegion != nil) {
        [locationManager stopMonitoringForRegion:myRegion];
        myRegion = nil;
    }
    if (value == YES) {
        [locationManager stopMonitoringSignificantLocationChanges];
        locationManager.distanceFilter = distanceFilter;
        [locationManager startUpdatingLocation];
    } else {
        [locationManager stopUpdatingLocation];
        [self startMonitoringStationaryRegion];
        [locationManager startMonitoringSignificantLocationChanges];
    }
}
/**
 * Creates a new circle around user and region-monitors it for exit
 */
- (void) startMonitoringStationaryRegion {
    NSLog(@"- CDVBackgroundGeoLocation createStationaryRegion");
    if (myRegion != nil) {
        [locationManager stopMonitoringForRegion:myRegion];
    }
    myRegion = [[CLCircularRegion alloc] initWithCenter: [[locationManager location] coordinate] radius:stationaryRadius identifier:@"BackgroundGeoLocation stationary region"];
    myRegion.notifyOnExit = YES;
    [locationManager startMonitoringForRegion:myRegion];
}

// If you don't stopMonitorying when application terminates, the app will be awoken still when a
// new location arrives, essentially monitoring the user's location even when they've killed the app.
// Might be desirable in certain apps.
- (void)applicationWillTerminate:(UIApplication *)application {
    [locationManager stopMonitoringSignificantLocationChanges];
    [locationManager stopUpdatingLocation];
    if (myRegion != nil) {
        [locationManager stopMonitoringForRegion:myRegion];
    }
}

- (void)dealloc
{
    locationManager.delegate = nil;
}

@end
