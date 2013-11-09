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
- (void) finish:(CDVInvokedUrlCommand*)command;

- (void) sync;
- (void) onSuspend:(NSNotification *)notification;
- (void) onResume:(NSNotification *)notification;

@end

