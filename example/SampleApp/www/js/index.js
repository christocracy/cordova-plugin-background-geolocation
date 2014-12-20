/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var ENV = (function() {
    
    var localStorage = window.localStorage;

    return {
        settings: {
            /**
            * state-mgmt
            */
            enabled:    localStorage.getItem('enabled')     || 'true',
            aggressive: localStorage.getItem('aggressive')  || 'false'
        },
        toggle: function(key) {
            var value       = localStorage.getItem(key)
                newValue    = ((new String(value)) == 'true') ? 'false' : 'true';

            localStorage.setItem(key, newValue);
            return newValue;
        }
    }
})()

var app = {
    /**
    * @property {google.maps.Map} map
    */
    map: undefined,
    /**
    * @property {google.maps.Marker} location The current location
    */
    location: undefined,
    /**
    * @property {google.map.PolyLine} path The list of background geolocations
    */
    path: undefined,
    /**
    * @property {Boolean} aggressiveEnabled
    */
    aggressiveEnabled: false,
    /**
    * @property {Array} locations List of rendered map markers of prev locations
    */
    locations: [],
    /**
    * @private
    */
    btnEnabled: undefined,
    btnPace: undefined,
    btnHome: undefined,
    btnReset: undefined,

    // Application Constructor  
    initialize: function() {
        this.bindEvents();
        google.maps.event.addDomListener(window, 'load', app.initializeMap);
    },
    initializeMap: function() {
        
        var mapOptions = {
          center: { lat: -34.397, lng: 150.644},
          zoom: 8,
          zoomControl: false
        };

        var header = $('#header'),
            footer = $('#footer'),
            canvas = $('#map-canvas'),
            canvasHeight = window.innerHeight - header[0].clientHeight - footer[0].clientHeight;

        canvas.height(canvasHeight);
        canvas.width(window.clientWidth);

        app.map = new google.maps.Map(canvas[0], mapOptions);
    },
    // Bind Event Listeners
    //
    // Bind any events that are required on startup. Common events are:
    // 'load', 'deviceready', 'offline', and 'online'.
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
        document.addEventListener('pause', this.onPause, false);
        document.addEventListener('resume', this.onResume, false);

        // Init UI buttons
        this.btnHome        = $('button#btn-home');
        this.btnReset       = $('button#btn-reset');
        this.btnPace        = $('button#btn-pace');
        this.btnEnabled     = $('button#btn-enabled');

        if (ENV.settings.aggressive == 'true') {
            this.btnPace.addClass('btn-danger');
        } else {
            this.btnPace.addClass('btn-success');
        }
        if (ENV.settings.enabled == 'true') {
            this.btnEnabled.addClass('btn-danger');
            this.btnEnabled[0].innerHTML = 'Stop';
        } else {
            this.btnEnabled.addClass('btn-success');
            this.btnEnabled[0].innerHTML = 'Start';
        }
        
        this.btnHome.on('click', this.onClickHome);
        this.btnReset.on('click', this.onClickReset);
        this.btnPace.on('click', this.onClickChangePace);
        this.btnEnabled.on('click', this.onClickToggleEnabled);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicitly call 'app.receivedEvent(...);'
    onDeviceReady: function() {
        app.receivedEvent('deviceready');
        app.configureBackgroundGeoLocation();
        app.watchPosition();
    },
    configureBackgroundGeoLocation: function() {
        var fgGeo = window.navigator.geolocation,
            bgGeo = window.plugins.backgroundGeoLocation;

        app.onClickHome();

        /**
        * This would be your own callback for Ajax-requests after POSTing background geolocation to your server.
        */
        var yourAjaxCallback = function(response) {
            bgGeo.finish();
        };

        /**
        * This callback will be executed every time a geolocation is recorded in the background.
        */
        var callbackFn = function(location) {
            console.log('[js] BackgroundGeoLocation callback:  ' + location.latitude + ',' + location.longitude);
            
            // Update our current-position marker.
            app.setCurrentLocation(location);

            // After you Ajax callback is complete, you MUST signal to the native code, which is running a background-thread, that you're done and it can gracefully kill that thread.
            yourAjaxCallback.call(this);
        };

        var failureFn = function(error) {
            console.log('BackgroundGeoLocation error');
        };

        // Only ios emits this stationary event
        bgGeo.onStationary(function(location) {
            if (!app.stationaryRadius) {
                app.stationaryRadius = new google.maps.Circle({
                    fillColor: '#cc0000',
                    fillOpacity: 0.4,
                    strokeOpacity: 0,
                    map: app.map
                });
            }
            var radius = (location.accuracy < location.radius) ? location.radius : location.accuracy;
            var center = new google.maps.LatLng(location.latitude, location.longitude);
            app.stationaryRadius.setRadius(radius);
            app.stationaryRadius.setCenter(center);

        });

        // BackgroundGeoLocation is highly configurable.
        bgGeo.configure(callbackFn, failureFn, {
            url: 'http://only.for.android.com/update_location.json', // <-- Android ONLY:  your server url to send locations to
            params: {
                auth_token: 'user_secret_auth_token',    //  <-- Android ONLY:  HTTP POST params sent to your server when persisting locations.
                foo: 'bar'                              //  <-- Android ONLY:  HTTP POST params sent to your server when persisting locations.
            },
            desiredAccuracy: 0,
            stationaryRadius: 50,
            distanceFilter: 50,
            notificationTitle: 'Background tracking', // <-- android only, customize the title of the notification
            notificationText: 'ENABLED', // <-- android only, customize the text of the notification
            activityType: 'AutomotiveNavigation',
            debug: true, // <-- enable this hear sounds for background-geolocation life-cycle.
            stopOnTerminate: false // <-- enable this to clear background location settings when the app terminates
        });
        
        // Turn ON the background-geolocation system.  The user will be tracked whenever they suspend the app.
        var settings = ENV.settings;

        if (settings.enabled == 'true') {
            bgGeo.start();
        
            if (settings.aggressive == 'true') {
                bgGeo.changePace(true);
            }
        }
    },
    onClickHome: function() {
        var fgGeo = window.navigator.geolocation;

        // Your app must execute AT LEAST ONE call for the current position via standard Cordova geolocation,
        //  in order to prompt the user for Location permission.
        fgGeo.getCurrentPosition(function(location) {
            var map     = app.map,
                coords  = location.coords,
                ll      = new google.maps.LatLng(coords.latitude, coords.longitude),
                zoom    = map.getZoom();

            map.setCenter(ll);
            if (zoom < 15) {
                map.setZoom(15);
            }
            app.setCurrentLocation(coords);
        });
    },
    onClickChangePace: function(value) {
        var bgGeo   = window.plugins.backgroundGeoLocation,
            btnPace = app.btnPace;

        btnPace.removeClass('btn-success');
        btnPace.removeClass('btn-danger');

        var isAggressive = ENV.toggle('aggressive');
        if (isAggressive == 'true') {
            btnPace.addClass('btn-danger');
            bgGeo.changePace(true);
        } else {
            btnPace.addClass('btn-success');
            bgGeo.changePace(false);
        }
    },
    onClickReset: function() {
        // Clear prev location markers.
        var locations = app.locations;
        for (var n=0,len=locations.length;n<len;n++) {
            locations[n].setMap(null);
        }
        app.locations = [];

        // Clear Polyline.
        app.path.setMap(null);
        app.path = undefined;
    },
    onClickToggleEnabled: function(value) {
        var bgGeo       = window.plugins.backgroundGeoLocation,
            btnEnabled  = app.btnEnabled,
            isEnabled   = ENV.toggle('enabled');
        
        btnEnabled.removeClass('btn-danger');
        btnEnabled.removeClass('btn-success');

        if (isEnabled == 'true') {
            btnEnabled.addClass('btn-danger');
            btnEnabled[0].innerHTML = 'Stop';
            bgGeo.start();
        } else {
            btnEnabled.addClass('btn-success');
            btnEnabled[0].innerHTML = 'Start';
            bgGeo.stop();
        }
    },
    watchPosition: function() {
        var fgGeo = window.navigator.geolocation;
        if (app.watchId) {
            app.stopPositionWatch();
        }
        // Watch foreground location
        app.watchId = fgGeo.watchPosition(function(location) {
            app.setCurrentLocation(location.coords);
        }, function() {}, {
            enableHighAccuracy: true,
            maximumAge: 5000,
            frequency: 10000,
            timeout: 10000
        });
    },
    stopPositionWatch: function() {
        var fgGeo = window.navigator.geolocation;
        if (app.watchId) {
            fgGeo.clearWatch(app.watchId);
            app.watchId = undefined;
        }
    },
    /**
    * Cordova foreground geolocation watch has no stop/start detection or scaled distance-filtering to conserve HTTP requests based upon speed.  
    * You can't leave Cordova's GeoLocation running in background or it'll kill your battery.  This is the purpose of BackgroundGeoLocation:  to intelligently 
    * determine start/stop of device.
    */
    onPause: function() {
        console.log('- onPause');
        app.stopPositionWatch();
    },
    /**
    * Once in foreground, re-engage foreground geolocation watch with standard Cordova GeoLocation api
    */
    onResume: function() {
        console.log('- onResume');
        app.watchPosition();
    },
    // Update DOM on a Received Event
    receivedEvent: function(id) {
        console.log('Received Event: ' + id);
    },
    setCurrentLocation: function(location) {
        if (!app.location) {
            app.location = new google.maps.Marker({
                map: app.map,
                icon: {
                    path: google.maps.SymbolPath.CIRCLE,
                    scale: 3,
                    fillColor: 'blue',
                    strokeColor: 'blue',
                    strokeWeight: 5
                }
            });
            app.locationAccuracy = new google.maps.Circle({
                fillColor: '#3366cc',
                fillOpacity: 0.4,
                strokeOpacity: 0,
                map: app.map
            });
        }
        if (!app.path) {
            app.path = new google.maps.Polyline({
                map: app.map,
                strokeColor: '#3366cc',
                fillOpacity: 0.4
            });
        }
        var latlng = new google.maps.LatLng(location.latitude, location.longitude);
        
        if (app.previousLocation) {
            var prevLocation = app.previousLocation;
            // Drop a breadcrumb of where we've been.
            app.locations.push(new google.maps.Marker({
                icon: {
                    path: google.maps.SymbolPath.CIRCLE,
                    scale: 3,
                    fillColor: 'green',
                    strokeColor: 'green',
                    strokeWeight: 5
                },
                map: app.map,
                position: new google.maps.LatLng(prevLocation.latitude, prevLocation.longitude)
            }));
        }

        // Update our current position marker and accuracy bubble.
        app.location.setPosition(latlng);
        app.locationAccuracy.setCenter(latlng);
        app.locationAccuracy.setRadius(location.accuracy);

        // Add breadcrumb to current Polyline path.
        app.path.getPath().push(latlng);
        app.previousLocation = location;
    }
};

app.initialize();