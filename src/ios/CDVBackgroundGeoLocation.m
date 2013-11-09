////
//  CDVBackgroundGeoLocation
//
//  Created by Chris Scott <chris@transistorsoft.com> on 2013-06-15
//  Largely based upon http://www.mindsizzlers.com/2011/07/ios-background-location/
//
#import "CDVLocation.h"
#import "CDVBackgroundGeoLocation.h"
#import <Cordova/CDVJSON.h>

@implementation CDVBackgroundGeoLocation

@synthesize enabled, token, url, locationManager, locationData, locationCache, suspendedAt;

- (CDVPlugin*) initWithWebView:(UIWebView*) theWebView
{
    // background location cache, for when no network is detected.
    self.locationCache = [NSMutableArray array];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onSuspend:)
                                                 name:UIApplicationDidEnterBackgroundNotification
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onResume:)
                                                 name:UIApplicationWillEnterForegroundNotification
                                               object:nil];
    return self;
}
- (void) configure:(CDVInvokedUrlCommand*)command
{
    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self;
    
    self.token = [command.arguments objectAtIndex: 0];
    self.url = [command.arguments objectAtIndex: 1];
    
    NSLog(@"CDVBackgroundGeoLocation configure");
    NSLog(@"  -token: %@", self.token);
    NSLog(@"  -url: %@", self.url);
}

- (void) start:(CDVInvokedUrlCommand*)command
{
    NSLog(@"CDVBackgroundGeoLocation start");
    self.enabled = YES;
}

- (void) stop:(CDVInvokedUrlCommand*)command
{
    NSLog(@"CDVBackgroundGeoLocation stop");
    self.enabled = NO;
    [self.locationManager stopMonitoringSignificantLocationChanges];
}

- (void) test:(CDVInvokedUrlCommand*)command
{
    NSLog(@"CDVBackgroundGeoLocation test");
    [self.locationManager startMonitoringSignificantLocationChanges];
    if ([self.locationCache count] > 0){
        [self sync];
    } else {
        NSLog(@"CDVBackgroundGeoLocation could not find a location to sync");
    }
}
-(void) onSuspend:(NSNotification *) notification
{
    NSLog(@"CDVBackgroundGeoLocation suspend");
    self.suspendedAt = [NSDate date];
    
    if (self.enabled) {
        [self.locationManager startMonitoringSignificantLocationChanges];
    }
}
-(void) onResume:(NSNotification *) notification
{
    NSLog(@"CDVBackgroundGeoLocation resume");
    if (self.enabled) {
        [self.locationManager stopMonitoringSignificantLocationChanges];
    }
    // When coming back-to-life, flush the background queue.
    if ([self.locationCache count] > 0){
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
    [self.locationCache addObjectsFromArray:locations];
    [self sync];
}

/**
 * We are running in the background if this is being executed.
 * We can't assume normal network access.
 * bgTask is defined as an instance variable of type UIBackgroundTaskIdentifier
 */
-(void) sync
{
    NSLog(@"BackgroundGeoLocation sync");
    // Note that the expiration handler block simply ends the task. It is important that we always
    // end tasks that we have started.
    UIBackgroundTaskIdentifier bgTask = 0;
    UIApplication *app = [UIApplication sharedApplication];
    
    // Some voodoo.
    bgTask = [app beginBackgroundTaskWithExpirationHandler:
              ^{
                  [app endBackgroundTask:bgTask];
              }];
    
    // Prepare a reusable Request instance.  We'll reuse it each time we iterate the queue below.
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString: self.url]
                                                           cachePolicy:NSURLRequestReloadIgnoringLocalCacheData
                                                       timeoutInterval:10.0];
    
    [request setHTTPMethod:@"POST"];
    [request setValue:@"application/json" forHTTPHeaderField:@"Accept"];
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    
    // The list of successfully recorded locations on server.
    NSMutableArray *recordedLocations = [NSMutableArray array];
    
    // Iterate the queue.
    CLLocation *location;
    for (location in self.locationCache) {
        // Build the json-data.
        NSString *lat = [NSString stringWithFormat: @"%f", location.coordinate.latitude];
        NSString *lng = [NSString stringWithFormat: @"%f", location.coordinate.longitude];
        
        NSMutableDictionary *params = [NSMutableDictionary dictionary];
        NSMutableDictionary *data = [NSMutableDictionary dictionary];
        [params setValue: self.token forKey: @"auth_token"];
        [params setValue: @"true" forKey: @"background_geolocation"];
        
        [data setValue: lat forKey: @"latitude"];
        [data setValue: lng forKey: @"longitude"];
        [data setValue: [location.timestamp descriptionWithLocale:[NSLocale systemLocale]] forKey: @"recorded_at"];
        [params setObject:data forKey:@"location"];
        NSString *json = [params JSONString];
        NSData *requestData = [NSData dataWithBytes:[json UTF8String] length:[json length]];
        
        [request setHTTPBody: requestData];
        
        // Synchronous HTTP request
        NSHTTPURLResponse* urlResponse = nil;
        NSError *error = [[NSError alloc] init];
        [NSURLConnection sendSynchronousRequest:request returningResponse:&urlResponse error:&error];
        
        if ([urlResponse statusCode] == 200) {
            // Yeehaw!
            NSLog(@"BackgroundGeoLocation HTTP SUCCESS");
            [recordedLocations addObject:location];
        } else {
            // If any HTTP request fails, break out of emptying queue and try again later.
            NSLog(@"BackgroundGeoLocation HTTP FAILED");
            break;
        }
    }
    // Remove our successfully recorded locations from cache.
    [self.locationCache removeObjectsInArray:recordedLocations];
    
    // Finish the voodoo.
    if (bgTask != UIBackgroundTaskInvalid)
    {
        [app endBackgroundTask:bgTask];
        bgTask = UIBackgroundTaskInvalid;
    }
}

// If you don't stopMonitorying when application terminates, the app will be awoken still when a
// new location arrives, essentially monitoring the user's location even when they've killed the app.
// Might be desirable in certain apps.
- (void)applicationWillTerminate:(UIApplication *)application {
    [self.locationManager stopMonitoringSignificantLocationChanges];
}

- (void)dealloc
{
    self.locationManager.delegate = nil;
}

@end
