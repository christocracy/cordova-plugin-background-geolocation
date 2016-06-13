//
//  BackgroundLocation.h
//  CDVBackgroundGeolocation
//
//  Created by Marian Hello on 10/06/16.
//

#ifndef BackgroundLocation_h
#define BackgroundLocation_h

#import <CoreLocation/CoreLocation.h>

@interface BackgroundLocation : NSObject <NSCopying>

@property (nonatomic, retain) NSNumber *id;
@property (nonatomic, retain) NSDate *time;
@property (nonatomic, retain) NSNumber *accuracy;
@property (nonatomic, retain) NSNumber *altitudeAccuracy;
@property (nonatomic, retain) NSNumber *speed;
@property (nonatomic, retain) NSNumber *heading;
@property (nonatomic, retain) NSNumber *altitude;
@property (nonatomic, retain) NSNumber *latitude;
@property (nonatomic, retain) NSNumber *longitude;
@property (nonatomic, retain) NSString *provider;
@property (nonatomic, retain) NSNumber *service_provider;
@property (nonatomic, retain) NSString *type;
@property BOOL debug;

+ (instancetype) fromCLLocation:(CLLocation*)location;
+ (NSTimeInterval) locationAge:(CLLocation*)location;
+ (NSMutableDictionary*) toDictionary:(CLLocation*)location;;
- (NSTimeInterval) locationAge;
- (NSMutableDictionary*) toDictionary;
- (CLLocationCoordinate2D) coordinate;
- (double) distanceFromLocation:(BackgroundLocation*)location;
- (BOOL) isBetterLocation:(BackgroundLocation*)location;
- (BOOL) isBeyond:(BackgroundLocation*)location radius:(NSInteger)radius;
- (BOOL) isValid;
- (id) copyWithZone: (NSZone *)zone;

@end

#endif /* BackgroundLocation_h */