/**
 * Custom Cordova Background GeoLocation plugin.  Uses iOS native API 
 * @author <chris@transistorsoft.com>
 * Largely based upon http://www.mindsizzlers.com/2011/07/ios-background-location/
 */

;(function(){
    var BackgroundGeoLocation = function() {}

    BackgroundGeoLocation.prototype = {
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
    };

    // remove Cordova.addConstructor since it was not supported on PhoneGap 2.0
    if (!window.plugins) window.plugins = {};

    if (!window.plugins.BackgroundGeoLocation) {
        window.plugins.BackgroundGeoLocation = new BackgroundGeoLocation();
    }
})();
