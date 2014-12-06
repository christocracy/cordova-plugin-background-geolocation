using System;
using Windows.Devices.Geolocation;
using WPCordovaClassLib.Cordova.Commands;

namespace Cordova.Extension.Commands
{
    public class StationaryManager
    {
        private readonly double _stationaryRadius;
        private Position _stationaryPosition;
        private Geocoordinate _stationaryGeocoordinate;
        public bool InStationary { get { return _stationaryPosition != null; } }

        private Position _previousPosition;

        private static TimeSpan _stationaryPollingIntervalLazy = TimeSpan.FromMinutes(3);
        private static TimeSpan _stationaryPollingIntervalAggresive = TimeSpan.FromMinutes(1);

        /// <param name="stationaryRadius">Stationary Radius in meters</param>
        public StationaryManager(double stationaryRadius)
        {
            _stationaryRadius = stationaryRadius;
        }

        public void StartStationary(Position stationaryPosition, Geocoordinate geocoordinate)
        {
            _stationaryPosition = stationaryPosition;
            _stationaryGeocoordinate = geocoordinate;
        }

        /// <returns>Returns null if exited Stationary modus, else returns Report Interval in milliseconds (lazy or aggressive)</returns>
        public double? GetNewReportInterval(Position newPosition)
        {
            if (_previousPosition == null) // first position-update in stationary, always aggresive
            {
                _previousPosition = newPosition;
                return _stationaryPollingIntervalAggresive.TotalMilliseconds;
            }

            var distance = GetDistanceToStationary(newPosition);
            if (distance > _stationaryRadius)
            {
                // exit stationary
                _stationaryPosition = null;
                _stationaryGeocoordinate = null;
                return null;
            }
            var percentage = distance / _stationaryRadius;

            return percentage < 0.5 ? _stationaryPollingIntervalLazy.TotalMilliseconds : _stationaryPollingIntervalAggresive.TotalMilliseconds;
        }

        public double GetDistanceToStationary(Position position)
        {
            return _stationaryPosition.GeoCoordinate.GetDistanceTo(position.GeoCoordinate);
        }

        public Geocoordinate GetStationaryGeocoordinate()
        {
            return _stationaryGeocoordinate;
        }

        public void ExitStationary()
        {
            _stationaryGeocoordinate = null;
        }
    }
}