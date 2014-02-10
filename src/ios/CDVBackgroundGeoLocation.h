//
//  CDVBackgroundGeoLocation.h
//
//  Created by Chris Scott <chris@transistorsoft.com>
//

#import <Cordova/CDVPlugin.h>
#import "CDVLocation.h"
#import <AudioToolbox/AudioToolbox.h>

@interface CDVBackgroundGeoLocation : CDVPlugin <CLLocationManagerDelegate>
- (void) configure:(CDVInvokedUrlCommand*)command;
- (void) start:(CDVInvokedUrlCommand*)command;
- (void) stop:(CDVInvokedUrlCommand*)command;
- (void) finish:(CDVInvokedUrlCommand*)command;
- (void) onPaceChange:(CDVInvokedUrlCommand*)command;
- (void) setConfig:(CDVInvokedUrlCommand*)command;

- (void) sync:(CLLocation*)location;
- (void) onSuspend:(NSNotification *)notification;
- (void) onResume:(NSNotification *)notification;

@end

