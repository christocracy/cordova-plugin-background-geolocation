//
//  CDVBackgroundGeoLocation.hs
//
//  Created by Chris Scott <chris@transistorsoft.com>
//

#import <Cordova/CDVPlugin.h>
#import "CDVLocation.h"

@interface CDVBackgroundGeoLocation : CDVPlugin <CLLocationManagerDelegate>
- (void) configure:(CDVInvokedUrlCommand*)command;
- (void) start:(CDVInvokedUrlCommand*)command;
- (void) stop:(CDVInvokedUrlCommand*)command;
- (void) test:(CDVInvokedUrlCommand*)command;
- (void) setHighAccuracy:(CDVInvokedUrlCommand*)command;
- (void) finish:(CDVInvokedUrlCommand*)command;

- (void) sync;
- (void) onSuspend:(NSNotification *)notification;
- (void) onResume:(NSNotification *)notification;


@property(nonatomic,retain) NSString *token;
@property(nonatomic,retain) NSString *url;
@property(nonatomic,retain) NSString *callbackId;
@property UIBackgroundTaskIdentifier bgTask;

@property(nonatomic,assign) BOOL enabled;
@property(nonatomic,retain) NSNumber *maxBackgroundHours;
@property (nonatomic, strong) CLLocationManager* locationManager;
@property (nonatomic, strong) CDVLocationData* locationData;
@property (strong) NSMutableArray *locationCache;
@property (nonatomic, retain) NSDate *suspendedAt;
@end

