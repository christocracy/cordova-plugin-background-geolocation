var exec = require("cordova/exec");

module.exports = {
    /**
    * @property {Object} stationaryRegion
    */
    stationaryRegion: null,

    /**
    * @property {Object} config
    */
    config: {},

    /**
     * Configure plugin (i.e. geolocation, notification, debugging etc.)
     * Configure must be called before start or stop.
     * @param {Function} success [optional]
     * @param {Function} failure [optional]
     * @param {Object} config [optional]
     */
    configure: function(success, failure, config) {
        // Reconfigure arguments for flexibility
        if      (arguments.length === 1) {
            config  = success;
            success = function() {};
        } else if (arguments.length === 2) {
            config  = failure;
            failure = function() {};
        }

        this.config = config || {};

        var params              = JSON.stringify(this.config.params  || {}),
            headers             = JSON.stringify(this.config.headers || {}),

            url                 = this.config.url || 'BackgroundGeoLocation_url',

            // Geolocation Configuration
            stationaryRadius    = (this.config.stationaryRadius >= 0) ? this.config.stationaryRadius : 50,    // meters
            distanceFilter      = (this.config.distanceFilter   >= 0) ? this.config.distanceFilter   : 500,   // meters
            locationTimeout     = (this.config.locationTimeout  >= 0) ? this.config.locationTimeout  : 60,    // seconds
            desiredAccuracy     = (this.config.desiredAccuracy  >= 0) ? this.config.desiredAccuracy  : 100,   // meters

            debug               = this.config.debug || false,

            // Notification Configuration
            notificationIcon    = this.config.notificationIcon  || "notification_icon",
            notificationTitle   = this.config.notificationTitle || "Background tracking",
            notificationText    = this.config.notificationText  || "ENABLED",

            activityType        = this.config.activityType    || "OTHER",
            stopOnTerminate     = this.config.stopOnTerminate || false;

        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'configure',
             [
                params,
                headers,
                url,
                stationaryRadius,
                distanceFilter,
                locationTimeout,
                desiredAccuracy,
                debug,
                notificationIcon,
                notificationTitle,
                notificationText,
                activityType,
                stopOnTerminate
             ]
        );
    },

    /**
     * Start background geolocation tracking based on configuration
     * and add notification drawer to taskbar.
     * @param {Function} success [optional]
     * @param {Function} failure [optional]
     */
    start: function(success, failure) {
        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'start',
             []);
    },

    /**
     * Stop background geolocation tracking and remove notification drawer.
     * @param {Function} success [optional]
     * @param {Function} failure [optional]
     */
    stop: function(success, failure) {
        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'stop',
             []);
    },

    /**
     * Called to signify the end of the plugin tracking.
     * @param {Function} success [optional]
     * @param {Function} failure [optional]
     */
    finish: function(success, failure) {
        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'finish',
             []);
    },

    /**
     * @param {Boolean} isMoving
     * @param {Function} success [optional]
     * @param {Function} failure [optional]
     */
    changePace: function(isMoving, success, failure) {
        exec(success || function() {},
             failure || function() {},
            'BackgroundGeoLocation',
            'onPaceChange',
            [isMoving]);
    },

    /**
     * Update the plugin configuration.
     * @param {Function} success [optional]
     * @param {Function} failure [optional]
     * @param {Object} config
     */
    setConfig: function(success, failure, config) {
        var self = this;

        // Reconfigure arguments for flexibility
        if      (arguments.length === 0) return;
        else if (arguments.length === 1) {
            config = success;
            success = function() {};
        } else if (arguments.length === 2) {
            config = failure;
            failure = function() {};
        }

        this.apply(this.config, config);

        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'setConfig',
             [config]);
    },

    /**
    * @returns current stationaryLocation if available.  null if not
    */
    getStationaryLocation: function(success, failure) {
        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'getStationaryLocation',
             []);
    },

    /**
    * Add a stationary-region listener.  Whenever the devices enters "stationary-mode",
    * your #success callback will be executed with #location param containing #radius of region
    * @param {Function} success
    * @param {Function} failure [optional] NOT IMPLEMENTED
    */
    onStationary: function(success, failure) {
        var self = this;

        success = success || function() {};

        var callback = function(region) {
            self.stationaryRegion = region;
            success.apply(self, arguments);
        };

        exec(callback,
             failure || function(e) { console.log(e) },
             'BackgroundGeoLocation',
             'addStationaryRegionListener',
             []);
    },

    /**
     * Apply and object to another.
     * Updates destination with values from source.
     * @param {Object} desitination
     * @param {Object} source
     * @returns {Object} destination updated from source
     */
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
