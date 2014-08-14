using System;
using Windows.Devices.Geolocation; 
using WPCordovaClassLib.Cordova;
using WPCordovaClassLib.Cordova.Commands;
using WPCordovaClassLib.Cordova.JSON;
using System.Diagnostics;

namespace Cordova.Extension.Commands
{
    public class BackgroundGeoLocation : BaseCommand, IBackgroundGeoLocation
    {
        private string ConfigureCallbackToken { get; set; }
        private BackgroundGeoLocationOptions BackgroundGeoLocationOptions { get; set; }

        /// <summary>
        /// Geolocator and RunningInBackground are required properties to run in background
        /// For more information read http://msdn.microsoft.com/library/windows/apps/jj662935(v=vs.105).aspx
        /// </summary>
        public static Geolocator Geolocator { get; set; }
        public static bool RunningInBackground { get; set; }

        /// <summary>
        /// When start() is fired immediate after configure() in javascript, configure may not be finished yet, IsConfigured and IsConfiguring are used to keep track of this
        /// </summary>
        private bool IsConfigured { get; set; }
        private bool IsConfiguring { get; set; }

        public BackgroundGeoLocation()
        {
            IsConfiguring = false;
            IsConfigured = false;

        }

        public void configure(string args)
        {
            IsConfiguring = true;
            ConfigureCallbackToken = CurrentCommandCallbackId;
            RunningInBackground = false;

            BackgroundGeoLocationOptions = this.ParseBackgroundGeoLocationOptions(args);

            IsConfigured = BackgroundGeoLocationOptions.ParsingSucceeded;
            IsConfiguring = false;
        }

        private BackgroundGeoLocationOptions ParseBackgroundGeoLocationOptions(string configureArgs)
        {
            var parsingSucceeded = true;

            var options = JsonHelper.Deserialize<string[]>(configureArgs); 

            double stationaryRadius;
            double distanceFilter;
            UInt32 locationTimeout;
            UInt32 desiredAccuracy;
            bool debug;

            if (!double.TryParse(options[3], out stationaryRadius))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, string.Format("Invalid value for stationaryRadius:{0}", options[3])));
                parsingSucceeded = false;
            }
            if (!double.TryParse(options[4], out distanceFilter))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, string.Format("Invalid value for distanceFilter:{0}", options[4])));
                parsingSucceeded = false;
            }
            if (!UInt32.TryParse(options[5], out locationTimeout))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, string.Format("Invalid value for locationTimeout:{0}", options[5])));
                parsingSucceeded = false;
            }
            if (!UInt32.TryParse(options[6], out desiredAccuracy))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, string.Format("Invalid value for desiredAccuracy:{0}", options[6])));
                parsingSucceeded = false;
            }
            if (!bool.TryParse(options[7], out debug))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, string.Format("Invalid value for debug:{0}", options[7])));
                parsingSucceeded = false;
            }

            return new BackgroundGeoLocationOptions
            { 
                Url = options[1],
                StationaryRadius = stationaryRadius,
                DistanceFilterInMeters = distanceFilter,
                LocationTimeoutInMilliseconds = locationTimeout,
                DesiredAccuracyInMeters = desiredAccuracy,
                Debug = debug,
                ParsingSucceeded = parsingSucceeded
            };
        }

        public void start(string nothing)
        {
            while (!IsConfigured && IsConfiguring)
            {
                // Wait for configure() to complete...
            }

            if (!IsConfigured || !BackgroundGeoLocationOptions.ParsingSucceeded)
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.INVALID_ACTION, "Cannot start: Run configure() with proper values!"));
                stop();
                return;
            }
            StopGeolocatorIfActive();

            Geolocator = new Geolocator
            {
                // Default: 50 meters
                MovementThreshold = BackgroundGeoLocationOptions.DistanceFilterInMeters,

                // JS Interface takes seconds, MS takes miliseconds, default 60 seconds
                ReportInterval = BackgroundGeoLocationOptions.LocationTimeoutInMilliseconds * 1000,

                // In our case this property has always a value, if left empty or below zero the default will be 100 meter but can be overridden via parameter DesiredAccuracy
                DesiredAccuracyInMeters = BackgroundGeoLocationOptions.DesiredAccuracyInMeters
            }; 

            Geolocator.PositionChanged += OnGeolocatorOnPositionChanged;

            RunningInBackground = true; 
        }

        private void OnGeolocatorOnPositionChanged(Geolocator sender, PositionChangedEventArgs configureCallbackTokenargs)
        {
            if (Geolocator.LocationStatus == PositionStatus.Disabled || Geolocator.LocationStatus == PositionStatus.NotAvailable)
            {
                DispatchMessage(PluginResult.Status.ERROR, string.Format("Cannot start: LocationStatus/PositionStatus: {0}! {1}", Geolocator.LocationStatus, IsConfigured), true, ConfigureCallbackToken);
                return;
            }

            var callbackJsonResult = configureCallbackTokenargs.Position.Coordinate.ToJson();
            if (BackgroundGeoLocationOptions.Debug)
            {
                DebugAudioNotifier.GetDebugAudioNotifier().PlaySound(DebugAudioNotifier.Tone.High, TimeSpan.FromSeconds(3));
                Debug.WriteLine("PositionChanged token{0}, Coordinates: {1}", ConfigureCallbackToken, callbackJsonResult);
            }

            DispatchMessage(PluginResult.Status.OK, callbackJsonResult, true, ConfigureCallbackToken);
        } 

        public void stop()
        {
            RunningInBackground = false;
            StopGeolocatorIfActive();
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }

        private void StopGeolocatorIfActive()
        {
            if (Geolocator == null) return;

            Geolocator.PositionChanged -= OnGeolocatorOnPositionChanged;
            Geolocator = null;
        }

        public void finish()
        {
            DispatchCommandResult(new PluginResult(PluginResult.Status.NO_RESULT));
        }

        public void onPaceChange(bool isMoving)
        {
            throw new NotImplementedException();
        }

        public void setConfig(string setConfigArgs)
        {
            if (string.IsNullOrWhiteSpace(setConfigArgs))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.INVALID_ACTION, "Cannot set config because of an empty input"));
                return;
            }
            var parsingSucceeded = true;

            var options = JsonHelper.Deserialize<string[]>(setConfigArgs);

            double stationaryRadius;
            double distanceFilter;
            UInt32 locationTimeout;
            UInt32 desiredAccuracy;

            if (!double.TryParse(options[0], out stationaryRadius))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, string.Format("Invalid value for stationaryRadius:{0}", options[2])));
                parsingSucceeded = false;
            }
            if (!double.TryParse(options[1], out distanceFilter))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, string.Format("Invalid value for distanceFilter:{0}", options[3])));
                parsingSucceeded = false;
            }
            if (!UInt32.TryParse(options[2], out locationTimeout))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, string.Format("Invalid value for locationTimeout:{0}", options[4])));
                parsingSucceeded = false;
            }
            if (!UInt32.TryParse(options[3], out desiredAccuracy))
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, string.Format("Invalid value for desiredAccuracy:{0}", options[5])));
                parsingSucceeded = false;
            }
            if (!parsingSucceeded) return;

            BackgroundGeoLocationOptions.StationaryRadius = stationaryRadius;
            BackgroundGeoLocationOptions.DistanceFilterInMeters = distanceFilter;
            BackgroundGeoLocationOptions.LocationTimeoutInMilliseconds = locationTimeout * 1000;
            BackgroundGeoLocationOptions.DesiredAccuracyInMeters = desiredAccuracy;

            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }

        public void getStationaryLocation()
        {
            throw new NotImplementedException();

        }
        private void DispatchMessage(PluginResult.Status status, string message, bool keepCallback, string callBackId)
        {
            var pluginResult = new PluginResult(status, message) { KeepCallback = keepCallback };
            DispatchCommandResult(pluginResult, callBackId);
        }
    }
}
