/***
 * Custom Cordova Background GeoLocation plugin.  Uses iOS native API 
 * @author <chris@transistorsoft.com>
 * Largely based upon http://www.mindsizzlers.com/2011/07/ios-background-location/
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
cordova.define("org.transistorsoft.cordova.background-geolocation", function(require, exports, module) {
    var exec = require('cordova/exec');

    /**
     * Provides access to the vibration mechanism on the device.
     */

    module.exports = {
        /**
        * Configure the native API with our authentication-token and url to POST locations to
        * TODO Native plugin assumes the json-structure as required by our rails server
        * options: 
        *  auth_token: authentication token 
        *  url: endpoint that we post the locations to, including hostname
        */
        configure: function(success, fail, options) {
            success = (typeof(success) === 'function') ? success : function() {};
            fail = (typeof(fail) === 'function') ? fail : function() {};
            if (!options.auth_token || !options.url) {
                var msg = "BackgroundGeoLocation requires an auth_token and url to report to the server";
                console.log(msg);
                fail(msg);
                return;
            }
            return Cordova.exec(success, fail, "BackgroundGeoLocation", "configure", [options.auth_token, options.url]);
        },
        /**
        * Enable background GeoLocation
        */
        start: function(success, fail, options) {
            options = options || {};
            success = (typeof(success) === 'function') ? success : function() {};
            fail = (typeof(fail) === 'function') ? fail : function() {};
            return Cordova.exec(success, fail, "BackgroundGeoLocation", "start", [options]);
        },
        /**
        * disable background GeoLocation
        */
        stop: function(success, fail, options) {
            options = options || {};
            success = (typeof(success) === 'function') ? success : function() {};
            fail = (typeof(fail) === 'function') ? fail : function() {};
            return Cordova.exec(success, fail, "BackgroundGeoLocation", "stop", [options]);
        }
    }
});
