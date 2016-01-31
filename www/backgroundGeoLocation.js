/*
 According to apache license

 This is fork of christocracy cordova-plugin-background-geolocation plugin
 https://github.com/christocracy/cordova-plugin-background-geolocation

 Differences to original version:

 1. new method isLocationEnabled
 */

var exec = require('cordova/exec');

var backgroundGeoLocation = {
    /**
     * @property {Object} stationaryRegion
     */
    stationaryRegion: null,

    /**
     * @property {Object} provider
     */
    provider: {
        ANDROID_DISTANCE_FILTER_PROVIDER: 0,
        ANDROID_ACTIVITY_PROVIDER: 1
    },

    accuracy: {
        HIGH: 0,
        MEDIUM: 100,
        LOW: 1000,
        PASSIVE: 10000
    },

    /**
     * @property {Object} config
     */
    config: {},

    configure: function(success, failure, config) {
        this.config = config || {};
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'configure',
            [config]
        );
    },

    start: function(success, failure) {
        exec(success || function() {},
            failure || function(err) { console.log(err); },
            'BackgroundGeoLocation',
            'start', []);
    },

    stop: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'stop', []);
    },

    finish: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'finish', []);
    },

    changePace: function(isMoving, success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'onPaceChange', [isMoving]);
    },

    /**
     * @param {Integer} stationaryRadius
     * @param {Integer} desiredAccuracy
     * @param {Integer} distanceFilter
     * @param {Integer} timeout
     */
    setConfig: function(success, failure, config) {
        this.apply(this.config, config);
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'setConfig', [config]);
    },

    getConfig: function(success, failure) {
      if (typeof(success) !== 'function') {
           throw 'BackgroundGeolocation#getConfig requires a success callback';
      }
      exec(success,
          failure || function() {},
          'BackgroundGeoLocation',
          'getConfig', []);
    },

    /**
     * Returns current stationaryLocation if available.  null if not
     */
    getStationaryLocation: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'getStationaryLocation', []);
    },

    /**
     * Add a stationary-region listener.  Whenever the devices enters "stationary-mode", your #success callback will be executed with #location param containing #radius of region
     * @param {Function} success
     * @param {Function} failure [optional] NOT IMPLEMENTED
     */
    onStationary: function(success, failure) {
        var me = this;
        success = success || function() {};
        var callback = function(region) {
            me.stationaryRegion = region;
            success.apply(me, arguments);
        };
        exec(callback,
            failure || function() {},
            'BackgroundGeoLocation',
            'addStationaryRegionListener', []);
    },

    isLocationEnabled: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'isLocationEnabled', []);
    },

    showLocationSettings: function() {
        exec(function() {},
            function() {},
            'BackgroundGeoLocation',
            'showLocationSettings', []);
    },

    watchLocationMode: function(success, failure) {
        if (typeof(success) !== 'function') {
             throw 'BackgroundGeolocation#watchLocationMode requires a success callback';
        }
        exec(success,
            failure || function() {},
            'BackgroundGeoLocation',
            'watchLocationMode', []);
    },

    stopWatchingLocationMode: function() {
        exec(function() {},
            function() {},
            'BackgroundGeoLocation',
            'stopWatchingLocationMode', []);
    },

    getLocations: function(success, failure) {
        if (typeof(success) !== 'function') {
             throw 'BackgroundGeolocation#getLocations requires a success callback';
        }
        exec(success,
            failure || function() {},
            'BackgroundGeoLocation',
            'getLocations', []);
    },

    deleteLocation: function(locationId, success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'deleteLocation', [locationId]);
    },

    deleteAllLocations: function(success, failure) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'deleteAllLocations', []);
    },

    apply: function(destination, source) {
        source = source || {};
        for (var property in source) {
            if (source.hasOwnProperty(property)) {
                destination[property] = source[property];
            }
        }
        return destination;
    }
};

module.exports = backgroundGeoLocation;
