/***
 * Custom Cordova Background GeoLocation plugin.  Uses iOS native API 
 * @author <chris@transistorsoft.com>
 * @author <brian@briansamson.com>
 * iOS native-side is largely based upon http://www.mindsizzlers.com/2011/07/ios-background-location/
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/
var exec = require("cordova/exec");
module.exports = {
    configure: function(success, failure, config) {
        var authToken           = config.auth_token || 'BackgroundGeoLocation_auth_token',
            url                 = config.url || 'BackgroundGeoLocation_url',
            stationaryRadius    = config.stationaryRadius   || 50,    // meters
            distanceFilter      = config.distanceFilter     || 500,   // meters
            locationTimeout     = config.locationTimeout    || 60,    // seconds
            desiredAccuracy     = config.desiredAccuracy    || 100;   // meters
               
        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'configure',
             [authToken, url, stationaryRadius, distanceFilter, locationTimeout, desiredAccuracy]);
    },
    start: function(success, failure, config) {
        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'start',
             []);
    },
    stop: function(success, failure, config) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'stop',
            []);
    },
    finish: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'finish',
            []);  
    },
    changePace: function(isMoving, success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'onPaceChange',
            [isMoving]);   
    },
    /**
    * @param {Integer} stationaryRadius
    * @param {Integer} desiredAccuracy
    * @param {Integer} distanceFilter
    * @param {Integer} timeout
    */
    setConfig: function(success, failure, config) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'setConfig',
            [config]);
    },
    setStationaryRadius: function(value, success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'setStationaryRadius',
            [value]);
    },
    setDesiredAccuracy: function(value, success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'setDesiredAccuracy',
            [value]);
    },
    setDistanceFilter: function(value, success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'setDistanceFilter',
            [value]);
    }
};



