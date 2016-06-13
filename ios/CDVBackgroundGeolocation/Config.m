//
//  Config.m
//  CDVBackgroundGeolocation
//
//  Created by Marian Hello on 11/06/16.
//

#import <Foundation/Foundation.h>
#import "Config.h"

@implementation Config

@synthesize stationaryRadius, distanceFilter, desiredAccuracy, isDebugging, activityType, stopOnTerminate, url, httpHeaders;

-(id) init {
    self = [super init];
    
    if (self == nil) {
        return self;
    }

    stationaryRadius = 50;
    distanceFilter = 500;
    desiredAccuracy = 100;
    isDebugging = NO;
    activityType = @"OTHER";
    stopOnTerminate = NO;
    
    return self;
}

+(instancetype) fromDictionary:(NSDictionary*)config
{
    Config *instance = [[Config alloc] init];

    if (config[@"stationaryRadius"]) {
        instance.stationaryRadius = [config[@"stationaryRadius"] intValue];
    }
    if (config[@"distanceFilter"]) {
        instance.distanceFilter = [config[@"distanceFilter"] intValue];
    }
    if (config[@"desiredAccuracy"]) {
        instance.desiredAccuracy = [config[@"desiredAccuracy"] intValue];
    }
    if (config[@"debug"]) {
        instance.isDebugging = [config[@"debug"] boolValue];
    }
    if (config[@"activityType"]) {
        instance.activityType = config[@"activityType"];
    }
    if (config[@"stopOnTerminate"]) {
        instance.stopOnTerminate = [config[@"stopOnTerminate"] boolValue];
    }
    if (config[@"url"]) {
        instance.url = config[@"url"];
    }
    if (config[@"httpHeaders"]) {
        instance.httpHeaders = config[@"httpHeaders"];
    }

    return instance;
}

- (CLActivityType) decodeActivityType;
{
    if ([activityType caseInsensitiveCompare:@"AutomotiveNavigation"]) {
        return CLActivityTypeAutomotiveNavigation;
    }
    if ([activityType caseInsensitiveCompare:@"OtherNavigation"]) {
        return CLActivityTypeOtherNavigation;
    }
    if ([activityType caseInsensitiveCompare:@"Fitness"]) {
        return CLActivityTypeFitness;
    }

    return CLActivityTypeOther;
}

- (NSInteger) decodeDesiredAccuracy
{
    if (desiredAccuracy >= 1000) {
        return kCLLocationAccuracyKilometer;
    }
    if (desiredAccuracy >= 100) {
        return kCLLocationAccuracyHundredMeters;
    }
    if (desiredAccuracy >= 10) {
        return kCLLocationAccuracyNearestTenMeters;
    }
    if (desiredAccuracy >= 0) {
        return kCLLocationAccuracyBest;
    }
    
    return kCLLocationAccuracyHundredMeters;
}

- (NSString *) description
{
    return [NSString stringWithFormat:@"Config: distanceFilter=%ld stationaryRadius=%ld desiredAccuracy=%ld activityType=%@ isDebugging=%d stopOnTerminate=%d url=%@ httpHeaders=%@", (long)distanceFilter, (long)stationaryRadius, (long)desiredAccuracy, activityType, isDebugging, stopOnTerminate, url, httpHeaders];
}


@end