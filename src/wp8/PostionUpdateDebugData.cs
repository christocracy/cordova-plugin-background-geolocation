using System;
using Windows.Devices.Geolocation;

namespace Cordova.Extension.Commands
{
    public class PostionUpdateDebugData
    {
        private readonly double? _currentAvgSpeed;
        private readonly double _accuracy;
        private readonly uint _reportInterval;

        private readonly bool _scaledDistanceFilterChanged;
        private readonly double _initialScaledDistanceFilter;
        private readonly double _newScaledDistanceFilter;

        private readonly double _distanceToExitStationary;

        public PositionUpdateType PositionUpdateType;
        private readonly double? _distance;
        private readonly double _distanceFilter;
        private readonly double _stationaryRadius;
         
        private PostionUpdateDebugData(PositionUpdateType positionUpdateType, double? distance, double distanceFilter, double stationaryRadius)
        {
            PositionUpdateType = positionUpdateType;
            _distance = distance;
            _distanceFilter = distanceFilter;
            _stationaryRadius = stationaryRadius;
        }

        private PostionUpdateDebugData(uint reportInterval, double distanceToExitStationary)
        {
            PositionUpdateType = PositionUpdateType.StationaryUpdate;
            _reportInterval = reportInterval;
            _distanceToExitStationary = distanceToExitStationary;
        }

        public static PostionUpdateDebugData ForStationaryUpdate(uint reportInterval, double distanceToExitStationary)
        {
            return new PostionUpdateDebugData(reportInterval, distanceToExitStationary);
        }

        public static PostionUpdateDebugData ForSkip(PositionUpdateType positionUpdateType, double? distance, double distanceFilter, double stationaryRadius)
        {
            return new PostionUpdateDebugData(positionUpdateType, distance, distanceFilter, stationaryRadius);
        }

        public static PostionUpdateDebugData ForNewPosition(PositionChangedEventArgs positionChangesEventArgs, double? currentAvgSpeed, UpdateScaledDistanceFilterResult updateScaledDistanceFilter, uint reportInterval,bool exitedFromStationary)
        {
            return new PostionUpdateDebugData(positionChangesEventArgs, currentAvgSpeed, updateScaledDistanceFilter, reportInterval, exitedFromStationary);
        }

        private PostionUpdateDebugData(PositionChangedEventArgs positionChangesEventArgs, double? currentAvgSpeed, UpdateScaledDistanceFilterResult updateScaledDistanceFilter, uint reportInterval, bool exitedFromStationary)
        {
            PositionUpdateType = exitedFromStationary ? PositionUpdateType.ExitStationary : PositionUpdateType.NewPosition;

            _currentAvgSpeed = currentAvgSpeed;
            _accuracy = positionChangesEventArgs.Position.Coordinate.Accuracy;
            _reportInterval = reportInterval;

            _scaledDistanceFilterChanged = updateScaledDistanceFilter.ScaledDistanceFilterChanged;
            _initialScaledDistanceFilter = updateScaledDistanceFilter.InitialScaledDistanceFilter;
            _newScaledDistanceFilter = updateScaledDistanceFilter.NewScaledDistanceFilter;
        }

        public string GetDebugNotifyMessage()
        {
            switch (PositionUpdateType)
            {
                case PositionUpdateType.SkippedBecauseOfDistance:
                    return string.Format("Distance filered: {0:0.}/{1:0.}m", _distance, _distanceFilter);

                case PositionUpdateType.EnteringStationary:
                    return string.Format("Entering stationary: {0:0.}/{1:0.}m", _distance, _stationaryRadius);

                case PositionUpdateType.StationaryUpdate:
                    return string.Format("Stat. Updt., TI: {0}s | EXIT IN {1:0.}m", TimeSpan.FromMilliseconds(_reportInterval).TotalSeconds, _distanceToExitStationary);

                case PositionUpdateType.ExitStationary:
                    return string.Format("Stat. Exit  TI {1:0.}s | SDF: {0:0.}", _newScaledDistanceFilter, _reportInterval / 1000);

                case PositionUpdateType.NewPosition:
                    var debugNotifyMessage = string.Format("SPD:{0:0.0}m/s | ACY:{1:0.}m ", _currentAvgSpeed, _accuracy);

                    if (_scaledDistanceFilterChanged)
                        debugNotifyMessage += string.Format("| TI {2:0.}s | SDF:{0:0.} > {1:0.}", _initialScaledDistanceFilter, _newScaledDistanceFilter, _reportInterval / 1000);
                    else
                        debugNotifyMessage += string.Format("| TI {1:0.}s | SDF: {0:0.}", _newScaledDistanceFilter, _reportInterval / 1000);

                    return debugNotifyMessage;
                default:
                    throw new Exception("Unknown PositionUpdateType");
            }
        }
    }
    public enum PositionUpdateType
    {
        EnteringStationary,
        StationaryUpdate,
        ExitStationary,
        NewPosition,
        SkippedBecauseOfDistance
    }
}