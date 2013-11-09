////
//  CDVBackgroundGeoLocation
//
//  Created by Chris Scott <chris@transistorsoft.com> on 2013-06-15
//  Largely based upon http://www.mindsizzlers.com/2011/07/ios-background-location/
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
    
    NSNumber *maxBackgroundHours;
    CLLocationManager *locationManager;
    CDVLocationData *locationData;
    NSMutableArray *locationCache;
    NSDate *suspendedAt;
    
    CLCircularRegion *myRegion;
    NSInteger stationaryRadius;
    NSInteger distanceFilter;
    NSInteger locationTimeout;
}

- (CDVPlugin*) initWithWebView:(UIWebView*) theWebView
{
    // background location cache, for when no network is detected.
    locationCache = [NSMutableArray array];
    locationManager = [[CLLocationManager alloc] init];
    locationManager.delegate = self;
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onSuspend:) name:UIApplicationDidEnterBackgroundNotification object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onResume:) name:UIApplicationWillEnterForegroundNotification object:nil];
    return self;
}
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
    
    NSLog(@"CDVBackgroundGeoLocation configure");
    NSLog(@"  - token: %@", token);
    NSLog(@"  - url: %@", url);
    NSLog(@"  - distanceFilter: %ld", (long)distanceFilter);
    NSLog(@"  - stationaryRadius: %ld", (long)stationaryRadius);
    NSLog(@"  - locationTimeout: %ld", (long)locationTimeout);
}

- (void) start:(CDVInvokedUrlCommand*)command
{
    NSLog(@"CDVBackgroundGeoLocation start");
    enabled = YES;
}

- (void) stop:(CDVInvokedUrlCommand*)command
{
    NSLog(@"CDVBackgroundGeoLocation stop");
    enabled = NO;
    [locationManager stopUpdatingLocation];
    [locationManager stopMonitoringSignificantLocationChanges];
}
- (void) test:(CDVInvokedUrlCommand*)command
{
    NSLog(@"CDVBackgroundGeoLocation test");
    [locationManager startMonitoringSignificantLocationChanges];
    if ([locationCache count] > 0){
        [self sync];
    } else {
        NSLog(@"CDVBackgroundGeoLocation could not find a location to sync");
    }
}
-(void) onSuspend:(NSNotification *) notification
{
    NSLog(@"CDVBackgroundGeoLocation suspend");
    suspendedAt = [NSDate date];
    
    if (enabled) {
        if (myRegion == nil) {
            myRegion = [self createStationaryRegion];
            [locationManager startMonitoringForRegion:myRegion];
        }
        [locationManager startMonitoringSignificantLocationChanges];
    }
}
-(void) onResume:(NSNotification *) notification
{
    NSLog(@"CDVBackgroundGeoLocation resume");
    if (enabled) {
        [locationManager stopMonitoringSignificantLocationChanges];
        [locationManager stopUpdatingLocation];
    }
    // When coming back-to-life, flush the background queue.
    if ([locationCache count] > 0){
        [self sync];
    }
}

-(void) locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations
{
    NSLog(@"CDVBackgroundGeoLocation didUpdateLocations");

    // Handle location updates as normal, code omitted for brevity.
    // The omitted code should determine whether to reject the location update for being too
    // old, too close to the previous one, too inaccurate and so forth according to your own
    // application design.
    [locationCache addObjectsFromArray:locations];
    
    [self sync];
    
}
/**
 * We are running in the background if this is being executed.
 * We can't assume normal network access.
 * bgTask is defined as an instance variable of type UIBackgroundTaskIdentifier
 */
-(void) sync
{
    NSLog(@"- CDVBackgroundGeoLocation sync");
    // Some voodoo.
    // Note that the expiration handler block simply ends the task. It is important that we always
    // end tasks that we have started.
    UIApplication *app = [UIApplication sharedApplication];
    
    bgTask = [app beginBackgroundTaskWithExpirationHandler:^{
        [app endBackgroundTask:bgTask];
    }];
    
    // Fetch last recorded location
    CLLocation *location = [locationCache lastObject];
    
    // Build a resultset for javascript callback.
    CDVPluginResult* result = nil;
    
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
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnInfo];
    [result setKeepCallbackAsBool:YES];
    
    // Inform javascript a background-fetch event has occurred.
    [self.commandDelegate sendPluginResult:result callbackId:syncCallbackId];
}
-(void) finish:(CDVInvokedUrlCommand*)command
{
    NSLog(@"CDVBackgroundGeoLocation finish");
    //_completionHandler(UIBackgroundFetchResultNewData);
    // Finish the voodoo.
    if (bgTask != UIBackgroundTaskInvalid)
    {
        [[UIApplication sharedApplication] endBackgroundTask:bgTask];
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
    if (myRegion != nil) {
        [locationManager stopMonitoringSignificantLocationChanges];
        [locationManager stopMonitoringForRegion:myRegion];
        [locationManager startUpdatingLocation];
        myRegion = nil;
    }
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
    [locationManager stopUpdatingLocation];
    [locationManager startMonitoringSignificantLocationChanges];
    
    myRegion = [self createStationaryRegion];
    [manager startMonitoringForRegion:myRegion];
}
/**
 * 1. Turn off significantChanges ApI
 * 2. turn on std. location services
 * 3. nullify myRegion
 */
- (void)locationManagerDidResumeLocationUpdates:(CLLocationManager *)manager
{
    NSLog(@"- CDVBackgroundGeoLocation resume location updates");
    [locationManager stopMonitoringSignificantLocationChanges];
    [locationManager startUpdatingLocation];
}

-(CLCircularRegion*) createStationaryRegion {
    CLCircularRegion *region = [[CLCircularRegion alloc] initWithCenter: [[locationManager location] coordinate] radius:stationaryRadius identifier:@"BackgroundGeoLocation stationary region"];
    region.notifyOnExit = YES;
    return region;
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
