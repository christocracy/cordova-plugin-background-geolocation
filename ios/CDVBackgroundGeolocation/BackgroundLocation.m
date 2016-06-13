//
//  BackgroundLocation.m
//  CDVBackgroundGeolocation
//
//  Created by Marian Hello on 10/06/16.
//

#import <Foundation/Foundation.h>
#import "BackgroundLocation.h"

enum {
    TWO_MINUTES = 120
};

@implementation BackgroundLocation

@synthesize id, time, accuracy, altitudeAccuracy, speed, heading, altitude, latitude, longitude, provider, service_provider, type, debug;

+ (instancetype) fromCLLocation:(CLLocation*)location;
{
    BackgroundLocation *instance = [[BackgroundLocation alloc] init];
    
    instance.time = location.timestamp;
    instance.accuracy = [NSNumber numberWithDouble:location.horizontalAccuracy];
    instance.altitudeAccuracy = [NSNumber numberWithDouble:location.verticalAccuracy];
    instance.speed = [NSNumber numberWithDouble:location.speed];
    instance.heading = [NSNumber numberWithDouble:location.course];
    instance.altitude = [NSNumber numberWithDouble:location.altitude];
    instance.latitude = [NSNumber numberWithDouble:location.coordinate.latitude];
    instance.longitude = [NSNumber numberWithDouble:location.coordinate.longitude];
    
    return instance;
}

+ (NSTimeInterval) locationAge:(CLLocation*)location
{
    return -[location.timestamp timeIntervalSinceNow];    
}

+ (NSMutableDictionary*) toDictionary:(CLLocation*)location;
{
    NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithCapacity:10];
    
    NSNumber* timestamp = [NSNumber numberWithDouble:([location.timestamp timeIntervalSince1970] * 1000)];
    [dict setObject:timestamp forKey:@"time"];
    [dict setObject:[NSNumber numberWithDouble:location.horizontalAccuracy] forKey:@"accuracy"];
    [dict setObject:[NSNumber numberWithDouble:location.verticalAccuracy] forKey:@"altitudeAccuracy"];
    [dict setObject:[NSNumber numberWithDouble:location.speed] forKey:@"speed"];
    [dict setObject:[NSNumber numberWithDouble:location.course] forKey:@"heading"];
    [dict setObject:[NSNumber numberWithDouble:location.altitude] forKey:@"altitude"];
    [dict setObject:[NSNumber numberWithDouble:location.coordinate.latitude] forKey:@"latitude"];
    [dict setObject:[NSNumber numberWithDouble:location.coordinate.longitude] forKey:@"longitude"];
    
    return dict;
}

/*
 * Age of location measured from now in seconds
 *
 */
- (NSTimeInterval) locationAge
{
    return -[time timeIntervalSinceNow];
}

- (NSMutableDictionary*) toDictionary
{
    NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithCapacity:10];

    if (id != nil) [dict setObject:id forKey:@"id"];
    if (time != nil) [dict setObject:[NSNumber numberWithDouble:([time timeIntervalSince1970] * 1000)] forKey:@"time"];
    if (accuracy != nil) [dict setObject:accuracy forKey:@"accuracy"];
    if (altitudeAccuracy != nil) [dict setObject:altitudeAccuracy forKey:@"altitudeAccuracy"];
    if (speed != nil) [dict setObject:speed forKey:@"speed"];
    if (heading != nil) [dict setObject:heading forKey:@"heading"];
    if (altitude != nil) [dict setObject:altitude forKey:@"altitude"];
    if (latitude != nil) [dict setObject:latitude forKey:@"latitude"];
    if (longitude != nil) [dict setObject:longitude forKey:@"longitude"];
    if (provider != nil) [dict setObject:provider forKey:@"provider"];
    if (service_provider != nil) [dict setObject:service_provider forKey:@"service_provider"];
    if (debug) [dict setObject:[NSNumber numberWithBool:debug] forKey:@"debug"];
    if (type != nil) [dict setObject:type forKey:@"location_type"];
    
    return dict;
}

- (CLLocationCoordinate2D) coordinate
{
    CLLocationCoordinate2D coordinate;
    coordinate.latitude = [latitude doubleValue];
    coordinate.longitude = [longitude doubleValue];
    return coordinate;
}

- (double) distanceFromLocation:(BackgroundLocation*)location
{
    const float EarthRadius = 6378137.0f;
    double a_lat = [self.latitude doubleValue];
    double a_lon = [self.longitude doubleValue];
    double b_lat = [location.latitude doubleValue];
    double b_lon = [location.longitude doubleValue];
    double dtheta = (a_lat - b_lat) * (M_PI / 180.0);
    double dlambda = (a_lon - b_lon) * (M_PI / 180.0);
    double mean_t = (a_lat + b_lat) * (M_PI / 180.0) / 2.0;
    double cos_meant = cosf(mean_t);
    
    return sqrtf((EarthRadius * EarthRadius) * (dtheta * dtheta + cos_meant * cos_meant * dlambda * dlambda));
}

/** 
 * Determines whether instance is better then BackgroundLocation reading
 * @param location  The new Location that you want to evaluate
 * Note: code taken from https://developer.android.com/guide/topics/location/strategies.html
 */
- (BOOL) isBetterLocation:(BackgroundLocation*)location
{
    if (location == nil) {
        // A instance location is always better than no location
        return NO;
    }

    // Check whether the new location fix is newer or older
    NSTimeInterval timeDelta = [self.time timeIntervalSinceDate:location.time];
    BOOL isSignificantlyNewer = timeDelta > TWO_MINUTES;
    BOOL isSignificantlyOlder = timeDelta < -TWO_MINUTES;
    BOOL isNewer = timeDelta > 0;
    
    // If it's been more than two minutes since the current location, use the new location
    // because the user has likely moved
    if (isSignificantlyNewer) {
        return YES;
        // If the new location is more than two minutes older, it must be worse
    } else if (isSignificantlyOlder) {
        return NO;
    }
    
    // Check whether the new location fix is more or less accurate
    NSInteger accuracyDelta = [self.accuracy integerValue] - [location.accuracy integerValue];
    BOOL isLessAccurate = accuracyDelta > 0;
    BOOL isMoreAccurate = accuracyDelta < 0;
    BOOL isSignificantlyLessAccurate = accuracyDelta > 200;
    
    // Check if the old and new location are from the same provider
    BOOL isFromSameProvider = YES; //TODO: check

    // Determine location quality using a combination of timeliness and accuracy
    if (isMoreAccurate) {
        return YES;
    } else if (isNewer && !isLessAccurate) {
        return YES;
    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
        return YES;
    }

    return NO;
}

- (BOOL) isBeyond:(BackgroundLocation*)location radius:(NSInteger)radius
{
    double pointDistance = [self distanceFromLocation:location];
    return (pointDistance - [self.accuracy doubleValue] - [location.accuracy doubleValue]) > radius;
}

- (BOOL) isValid
{
    if (!accuracy || accuracy < 0) return NO;

    return YES;
}

- (NSString *) description
{
    return [NSString stringWithFormat:@"BackgroundLocation: id=%ld time=%ld lat=%@ lon=%@ accu=%@ aaccu=%@ speed=%@ head=%@ alt=%@ type=%@ debug=%d", (long)id, (long)time, latitude, longitude, accuracy, altitudeAccuracy, speed, heading, altitude, type, debug];
}

-(id) copyWithZone: (NSZone *) zone
{
    BackgroundLocation *copy = [[[self class] allocWithZone: zone] init];
    if (copy) {
        copy.time = time;
        copy.accuracy = accuracy;
        copy.altitudeAccuracy = altitudeAccuracy;
        copy.speed = speed;
        copy.heading = heading;
        copy.altitude = altitude;
        copy.latitude = latitude;
        copy.longitude = longitude;
        copy.provider = provider;
        copy.service_provider = service_provider;
        copy.type = type;
        copy.debug = debug;        
    }
    
    return copy;
}


@end