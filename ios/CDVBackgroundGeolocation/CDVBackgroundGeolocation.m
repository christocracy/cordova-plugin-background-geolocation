//
//  BackgroundGeolocationDelegate.h
//
//  Created by Marian Hello on 04/06/16.
//  Version 2.0.0
//
//  According to apache license
//
//  This is class is using code from christocracy cordova-plugin-background-geolocation plugin
//  https://github.com/christocracy/cordova-plugin-background-geolocation


#import "CDVBackgroundGeolocation.h"
#import "Config.h"

@implementation CDVBackgroundGeolocation {
}

@synthesize syncCallbackId;
@synthesize bgDelegate;
@synthesize stationaryRegionListeners;

- (void)pluginInitialize
{
    bgDelegate = [[BackgroundGeolocationDelegate alloc] init];
    bgDelegate.onLocationChanged = [self createLocationChangedHandler];
    bgDelegate.onStationaryChanged = [self createStationaryChangedHandler];
    bgDelegate.onError = [self createErrorHandler];

    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onPause:) name:UIApplicationDidEnterBackgroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onResume:) name:UIApplicationWillEnterForegroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onFinishLaunching:) name:UIApplicationDidFinishLaunchingNotification object:nil];
}

/**
 * configure plugin
 * @param {Number} stationaryRadius
 * @param {Number} distanceFilter
 * @param {Number} locationTimeout
 */
- (void) configure:(CDVInvokedUrlCommand*)command
{
    Config* config = [Config fromDictionary:[command.arguments objectAtIndex:0]];
    self.syncCallbackId = command.callbackId;

    [self.commandDelegate runInBackground:^{
        NSError *error = nil;
        CDVPluginResult* result = nil;
        if (![bgDelegate configure:config error:&error]) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Configuration error"];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }
    }];
}

/**
 * Turn on background geolocation
 * in case of failure it calls error callback from configure method
 * may fire two callback when location services are disabled and when authorization failed
 */
- (void) start:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSError *error = nil;
        CDVPluginResult* result = nil;
        [result setKeepCallbackAsBool:YES];

        [bgDelegate start:&error];
        if (error == nil) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[error userInfo]];
        }
        
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

/**
 * Turn it off
 */
- (void) stop:(CDVInvokedUrlCommand*)command
{
    NSError *error = nil;
    CDVPluginResult* result = nil;
    if ([bgDelegate stop:&error]) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[error userInfo]];
    }
    
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * Change 
 * @param {Number} operation mode BACKGROUND/FOREGROUND
 */
- (void) switchMode:(CDVInvokedUrlCommand *)command
{
    BGOperationMode mode = [[command.arguments objectAtIndex: 0] intValue];
    [bgDelegate switchMode:mode];
}

- (void) addStationaryRegionListener:(CDVInvokedUrlCommand*)command
{
    if (self.stationaryRegionListeners == nil) {
        self.stationaryRegionListeners = [[NSMutableArray alloc] init];
    }
    [self.stationaryRegionListeners addObject:command.callbackId];
    NSMutableDictionary* stationaryLocation = [bgDelegate getStationaryLocation];
    if (stationaryLocation != nil) {
        // TODO: do this in background thread
        CDVPluginResult* result =[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:stationaryLocation];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

/**
 * Fetches current stationaryLocation
 */
- (void) getStationaryLocation:(CDVInvokedUrlCommand *)command
{
    CDVPluginResult* result = nil;

    NSMutableDictionary* stationaryLocation = [bgDelegate getStationaryLocation];
    if (stationaryLocation) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:stationaryLocation];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:NO];
    }

    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) isLocationEnabled:(CDVInvokedUrlCommand*)command
{
    BOOL isLocationEnabled = [bgDelegate isLocationEnabled];
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isLocationEnabled];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) showAppSettings:(CDVInvokedUrlCommand*)command
{
    [bgDelegate showAppSettings];
}

- (void) showLocationSettings:(CDVInvokedUrlCommand*)command
{
    [bgDelegate showLocationSettings];
}

- (void) watchLocationMode:(CDVInvokedUrlCommand*)command
{
    // TODO: yet to be implemented
}

- (void) stopWatchingLocationMode:(CDVInvokedUrlCommand*)command
{
     // TODO: yet to be implemented
}

- (void) getLocations:(CDVInvokedUrlCommand*)command
{
    NSArray *locations = [bgDelegate getLocations];
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:locations];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) deleteLocation:(CDVInvokedUrlCommand*)command
{
    int locationId = [[command.arguments objectAtIndex: 0] intValue];
    [bgDelegate deleteLocation:[[NSNumber alloc] initWithInt:locationId]];
}

- (void) deleteAllLocations:(CDVInvokedUrlCommand*)command
{
    [bgDelegate deleteAllLocations];
}

/**
 * Called by js to signify the end of a background-geolocation event
 */
-(void) finish:(CDVInvokedUrlCommand*)command
{
    [bgDelegate finish];
}

/**@
 * Resume.  Turn background off
 */
-(void) onResume:(NSNotification *)notification
{
    NSLog(@"CDVBackgroundGeoLocation resumed");
    [bgDelegate switchMode:FOREGROUND];
}

-(void) onPause:(NSNotification *)notification
{
    NSLog(@"CDVBackgroundGeoLocation paused");
    [bgDelegate switchMode:BACKGROUND];
}

/**@
 * on UIApplicationDidFinishLaunchingNotification
 */
-(void) onFinishLaunching:(NSNotification *)notification
{
    NSDictionary *dict = [notification userInfo];
    
    if ([dict objectForKey:UIApplicationLaunchOptionsLocationKey]) {
        NSLog(@"CDVBackgroundGeolocation started by system on location event.");
//        [bgDelegate switchOperationMode:BACKGROUND];
    }
}

-(void) onAppTerminate
{
    NSLog(@"CDVBackgroundGeoLocation appTerminate");
    [bgDelegate onAppTerminate];
}

-(void (^)(NSMutableDictionary *location)) createLocationChangedHandler {
    return ^(NSMutableDictionary *location) {
        NSLog(@"CDVBackgroundGeolocation onLocationChanged");
        CDVPluginResult* result = nil;
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:location];
        [result setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:result callbackId:self.syncCallbackId];
    };
}

-(void (^)(NSMutableDictionary *location)) createStationaryChangedHandler {
    return ^(NSMutableDictionary *location) {
        NSLog(@"CDVBackgroundGeolocation onStationaryChanged");

        if (![self.stationaryRegionListeners count]) {
            return;
        }
       
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:location];
        [result setKeepCallbackAsBool:YES];
        for (NSString *callbackId in self.stationaryRegionListeners) {
            [self.commandDelegate sendPluginResult:result callbackId:callbackId];
        }
    };
}

-(void (^)(NSError *error)) createErrorHandler {
    return ^(NSError *error) {
        NSLog(@"CDVBackgroundGeolocation onError");
        CDVPluginResult* result = nil;
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[error userInfo]];
        [result setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:result callbackId:self.syncCallbackId];
    };
}

@end
