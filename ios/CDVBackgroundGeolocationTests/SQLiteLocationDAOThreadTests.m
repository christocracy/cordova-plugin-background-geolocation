//
//  CDVBackgroundGeolocationTests.m
//  CDVBackgroundGeolocationTests
//
//  Created by Marian Hello on 10/06/16.
//  Copyright © 2016 mauron85. All rights reserved.
//

#import <XCTest/XCTest.h>
#import "SQLiteLocationDAO.h"

@interface PersistLocationInThread : NSObject
{
    
}
// class methods go here
- (void) noop;
- (void) persistLocation:(NSNumber *)value;
@end

@interface SQLiteLocationDAOThreadTests : XCTestCase

@end

@implementation SQLiteLocationDAOThreadTests

- (void)setUp {
    [super setUp];
    SQLiteLocationDAO *locationDAO = [SQLiteLocationDAO sharedInstance];
    [locationDAO clearDatabase];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
    SQLiteLocationDAO *locationDAO = [SQLiteLocationDAO sharedInstance];
    [locationDAO clearDatabase];
}

- (void)testGetAllLocationsMultiThread {
    SQLiteLocationDAO *locationDAO = [SQLiteLocationDAO sharedInstance];
    long unsigned waitForThreads = 100;

    dispatch_queue_t queue = dispatch_queue_create("com.marianhello.SQLiteLocationDAOThreadTests", DISPATCH_QUEUE_CONCURRENT);

    for (int i = 0; i < waitForThreads; i++) {
        dispatch_async(queue, ^{
            BackgroundLocation *location = [[BackgroundLocation alloc] init];
            location.time = [NSDate dateWithTimeIntervalSince1970:100+i];
            location.accuracy = [NSNumber numberWithDouble:i];
            location.speed = [NSNumber numberWithDouble:32+i];
            location.heading = [NSNumber numberWithDouble:200+i];
            location.altitude = [NSNumber numberWithDouble:940+i];
            location.latitude = [NSNumber numberWithDouble:37+i];
            location.longitude = [NSNumber numberWithDouble:-22+i];
            location.provider = @"TEST";
            location.service_provider = [NSNumber numberWithInt:-1];
            location.debug = YES;
            
            [locationDAO persistLocation:location];
        });
    }

    sleep(10); //very naive, help needed
    
    NSMutableArray *locations = [NSMutableArray arrayWithArray:[locationDAO getAllLocations]];
    [locations sortUsingDescriptors:@[[NSSortDescriptor sortDescriptorWithKey:@"time" ascending:YES]]];

    XCTAssertEqual([locations count], waitForThreads, @"Number of stored location is %lu expecting %lu", (unsigned long)[locations count], waitForThreads);
    
    for (int i = 0; i < waitForThreads; i++) {
        BackgroundLocation *result = [locations objectAtIndex:i];
        XCTAssertTrue([result.time isEqualToDate:[NSDate dateWithTimeIntervalSince1970:100+i]], "time is %@ expecting %@", result.time, [NSDate dateWithTimeIntervalSince1970:100+i]);
        XCTAssertTrue([result.accuracy isEqualToNumber:[NSNumber numberWithDouble:i]], "accuracy is %@ expecting %@", result.accuracy, [NSNumber numberWithDouble:i]);
        XCTAssertTrue([result.speed isEqualToNumber:[NSNumber numberWithDouble:32+i]], "speed is %@ expecting %@", result.speed, [NSNumber numberWithDouble:32+i]);
        XCTAssertTrue([result.heading isEqualToNumber:[NSNumber numberWithDouble:200+i]], "heading is %@ expecting %@", result.heading, [NSNumber numberWithDouble:200+i]);
        XCTAssertTrue([result.altitude isEqualToNumber:[NSNumber numberWithDouble:940+i]], "altitude is %@ expecting %@", result.altitude, [NSNumber numberWithDouble:940+i]);
        XCTAssertTrue([result.latitude isEqualToNumber:[NSNumber numberWithDouble:37+i]], "latitude is %@ expecting %@", result.latitude, [NSNumber numberWithDouble:37+i]);
        XCTAssertTrue([result.longitude isEqualToNumber:[NSNumber numberWithDouble:-22+i]], "longitude is %@ expecting %@", result.longitude, [NSNumber numberWithDouble:-22+i]);
        XCTAssertTrue([result.provider isEqualToString:@"TEST"], @"provider is expected to be TEST");
        XCTAssertTrue([result.service_provider isEqualToNumber:[NSNumber numberWithInt:-1]], "service_provider is %@ expecting %@", result.service_provider, [NSNumber numberWithInt:-1]);
        XCTAssertEqual([result debug], 1, @"debug is expected to be true");
    }
}

- (void)testPerformanceExample {
    // This is an example of a performance test case.
    [self measureBlock:^{
        // Put the code you want to measure the time of here.
    }];
}

@end
