using System;
using System.Device.Location;
using Windows.Devices.Geolocation;
using Windows.Foundation;

namespace Cordova.Extension.Commands
{
    public interface IGeolocatorWrapper
    {
        /// <summary>
        /// Raises when location is updated after Report Interval and with a minimum movement of distance filter
        /// </summary>
        event TypedEventHandler<GeolocatorWrapper, GeolocatorWrapperPositionChangedEventArgs> PositionChanged;

        void Start();
        void Stop();
        bool IsActive { get; }
    }

    public class GeolocatorWrapper : IGeolocatorWrapper
    {
        /// <summary>
        /// Geolocator and RunningInBackground are required properties to run in background
        /// For more information read http://msdn.microsoft.com/library/windows/apps/jj662935(v=vs.105).aspx
        /// </summary>
        private static Geolocator Geolocator { get; set; }

        /// <summary>
        /// Desired accuracy in meters
        /// </summary>
        private readonly UInt32 _desiredAccuracy;

        /// <summary>
        /// Report interval in milliseconds
        /// </summary>
        private readonly uint _reportInterval;

        /// <summary>
        /// Base distance filter (set via constructor) in meters
        /// </summary>
        private readonly double _distanceFilter;

        /// <summary>
        /// Automatically scaled distance filter in meters
        /// </summary>
        private double? _scaledDistanceFilter;

        /// <summary>
        /// Current speed in Meter/Second
        /// </summary>
        private double? _currentSpeed;

        private GeoCoordinate _lastGeoCoordinate;
        private DateTime? _lastGeoCoordinateDateTime;
        private bool _skipNextPosition;

        public bool IsActive { get; private set; }

        public event TypedEventHandler<GeolocatorWrapper, GeolocatorWrapperPositionChangedEventArgs> PositionChanged;

        /// <param name="desiredAccuracy">In meters</param>
        /// <param name="reportInterval">In milliseconds</param>
        /// <param name="distanceFilter">In meters</param>
        public GeolocatorWrapper(UInt32 desiredAccuracy, UInt32 reportInterval, double distanceFilter)
        {
            _desiredAccuracy = desiredAccuracy;
            _reportInterval = reportInterval;
            _distanceFilter = distanceFilter;
        }

        public void Start()
        {
            if (Geolocator != null) Geolocator.PositionChanged -= OnGeolocatorOnPositionChanged;

            Geolocator = new Geolocator
            {
                // MovementThreshold 0 by purpose, is taken care of by this wrapper using current speed, ScaledDistanceFilter and ReportInterval
                MovementThreshold = default(double),

                ReportInterval = _reportInterval,

                DesiredAccuracyInMeters = _desiredAccuracy
            };

            Geolocator.PositionChanged += OnGeolocatorOnPositionChanged;
            IsActive = true;
        }

        public void Stop()
        {
            if (Geolocator == null) return;

            Geolocator.PositionChanged -= OnGeolocatorOnPositionChanged;
            Geolocator = null;
            IsActive = false;
        }

        private void OnGeolocatorOnPositionChanged(Geolocator sender, PositionChangedEventArgs positionChangesEventArgs)
        {
            if (_skipNextPosition)
            {
                _skipNextPosition = false;
                return;
            }

            var updateScaledDistanceFilter = UpdateScaledDistanceFilter(positionChangesEventArgs);
            if (updateScaledDistanceFilter.Skip) return;
            if (updateScaledDistanceFilter.ScaledDistanceFilterChanged) UpdateReportInterval();

            var debugNotifyMessage = string.Format("SPD:{0:0.0}m/s | ACY:{1:0.}m ", _currentSpeed, positionChangesEventArgs.Position.Coordinate.Accuracy);

            if (updateScaledDistanceFilter.ScaledDistanceFilterChanged)
                debugNotifyMessage += string.Format("| SDF:{0:0.} > {1:0.} | TI {2:0.}s", updateScaledDistanceFilter.InitialScaledDistanceFilter, updateScaledDistanceFilter.NewScaledDistanceFilter, Geolocator.ReportInterval / 1000);
            else
                debugNotifyMessage += string.Format("| SDF: {0:0.} | TI {1:0.}s", updateScaledDistanceFilter.NewScaledDistanceFilter, Geolocator.ReportInterval / 1000);

            PositionChanged(this, new GeolocatorWrapperPositionChangedEventArgs
            {
                GeolocatorLocationStatus = Geolocator.LocationStatus,
                Position = positionChangesEventArgs.Position,
                DebugMessage = debugNotifyMessage
            });
        }

        private UpdateScaledDistanceFilterResult UpdateScaledDistanceFilter(PositionChangedEventArgs positionChangesEventArgs)
        {
            var result = new UpdateScaledDistanceFilterResult(_scaledDistanceFilter.HasValue ? _scaledDistanceFilter.Value : 0);
            var newGeoCoordinate = new GeoCoordinate(positionChangesEventArgs.Position.Coordinate.Latitude, positionChangesEventArgs.Position.Coordinate.Longitude);

            if (_lastGeoCoordinate == null || !_lastGeoCoordinateDateTime.HasValue)
            {
                _lastGeoCoordinate = newGeoCoordinate;
                _lastGeoCoordinateDateTime = DateTime.Now;
            }
            else if (positionChangesEventArgs.Position.Coordinate.Accuracy > _desiredAccuracy)
            {
                // Accuracy to low to measure the traveled distance and calculate the ScaledDistanceFilter (default distance filter is used)
                _lastGeoCoordinate = newGeoCoordinate;
                _lastGeoCoordinateDateTime = DateTime.Now;
                _scaledDistanceFilter = _distanceFilter;
                _currentSpeed = null;
                return result;
            }
            else
            {
                var distanceInMeters = newGeoCoordinate.GetDistanceTo(_lastGeoCoordinate);

                if (distanceInMeters < _distanceFilter)
                {
                    //Todo: implement Stationary Location ?!
                    result.Skip = true;
                    return result;
                }

                var secondsBetween = (DateTime.Now - _lastGeoCoordinateDateTime.Value).TotalSeconds;
                var currentSpeed = distanceInMeters / secondsBetween;

                _lastGeoCoordinate = newGeoCoordinate;
                _lastGeoCoordinateDateTime = DateTime.Now;

                result.NewScaledDistanceFilter = CalculateNewScaledDistanceFilter(currentSpeed);

                if (!result.ScaledDistanceFilterChanged) return result;

                _scaledDistanceFilter = result.NewScaledDistanceFilter;

                // Not using GeoCoordinate speed (following line) because it is empty when in Emulator or when accuracy is low  
                //_currentSpeed = positionChangesEventArgs.Position.Coordinate.Speed;
                _currentSpeed = currentSpeed;
            }

            return result;
        }

        private void UpdateReportInterval()
        {
            var reportInterval = CalculateNewReportInterval();

            // Changing the ReportInterval fires the Geolocator.OnPositionChanged event. 
            // Prevent a direct/second update of the DistanceFilter/ReportInterval directly after the current one 
            _skipNextPosition = true;

            // Windows Phone suspends the app when all eventhandlers of all GeoLocator objects are removed (only in background mode)
            // Wire up a temporary Geolocator to prevent the app from closing
            var tempGeolocator = new Geolocator
            {
                MovementThreshold = 1,
                ReportInterval = 1
            };
            TypedEventHandler<Geolocator, PositionChangedEventArgs> dummyHandler = (sender, positionChangesEventArgs2) => { };
            tempGeolocator.PositionChanged += dummyHandler;

            // It is not allowed to change properties of Geolocator when eventhandlers are attached 
            Geolocator.PositionChanged -= OnGeolocatorOnPositionChanged;
            Geolocator.ReportInterval = reportInterval;
            Geolocator.PositionChanged += OnGeolocatorOnPositionChanged;

            tempGeolocator.PositionChanged -= dummyHandler;
        }

        private double CalculateNewScaledDistanceFilter(double currentSpeed)
        {
            var newDistanceFilter = _distanceFilter;
            if (currentSpeed > 100) return newDistanceFilter;

            var speedRoundedToNearesFive = RoundToNearestFactor(currentSpeed, 5);
            var squareRouteOfSpeed = Math.Pow(speedRoundedToNearesFive, 2);
            newDistanceFilter = squareRouteOfSpeed + newDistanceFilter;

            if (newDistanceFilter > 1000) newDistanceFilter = 1000;

            return newDistanceFilter;
        }

        /// <returns>New report interval in milliseconds</returns>
        private uint CalculateNewReportInterval()
        {
            var defaultReportInterval = _reportInterval;
            if (!_currentSpeed.HasValue || Math.Abs(_currentSpeed.Value) < 0.1) return defaultReportInterval;

            var newReportInterval = (_scaledDistanceFilter / _currentSpeed.Value) * 1000;
            if (newReportInterval > UInt32.MaxValue) newReportInterval = UInt32.MaxValue;

            // Never longer than ten times the defaultReportInterval 
            if (newReportInterval > (10 * defaultReportInterval)) newReportInterval = (10 * _reportInterval);

            // Never longer than one hour 
            if (newReportInterval > TimeSpan.FromHours(1).TotalMilliseconds) newReportInterval = TimeSpan.FromHours(1).TotalMilliseconds;

            return newReportInterval > defaultReportInterval ? Convert.ToUInt32(newReportInterval) : defaultReportInterval;
        }

        private int RoundToNearestFactor(double value, int factor)
        {
            return (int)Math.Round((value / factor), MidpointRounding.AwayFromZero) * factor;
        }
    }
}
