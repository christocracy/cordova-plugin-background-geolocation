//
//  SQLiteLocationDAO.h
//  CDVBackgroundGeolocation
//
//  Created by Marian Hello on 10/06/16.
//

#ifndef LocationDAO_h
#define LocationDAO_h

#import <Foundation/Foundation.h>
#import "BackgroundLocation.h"

typedef struct sqlite3 sqlite3;

@interface SQLiteLocationDAO : NSObject

+ (instancetype) sharedInstance;
- (id) init NS_UNAVAILABLE;
- (NSArray<BackgroundLocation*>*) getAllLocations;
- (NSNumber*) persistLocation:(BackgroundLocation*)location;
- (BOOL) deleteLocation:(NSNumber*)locationId;
- (BOOL) deleteAllLocations;
- (BOOL) clearDatabase;

@end

#endif /* SQLiteLocationDAO_h */
