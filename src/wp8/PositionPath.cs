using System;
using System.Collections.Generic;
using System.Device.Location;
using System.Linq;

namespace Cordova.Extension.Commands
{
    public class PositionPath
    {
        private readonly List<Position> _positions = new List<Position>();

        private const int MinimumAccuracyToCalculateSpeed = 50;
        private static readonly TimeSpan MaximumTimeframeForSpeedCalculation = TimeSpan.FromMinutes(5);

        public void AddPosition(GeoCoordinate geoCoordinate, DateTime dateTime, double accuracy)
        {
            AddPosition(new Position(geoCoordinate, dateTime, accuracy));
        }

        public void AddPosition(Position position)
        {
            if (_positions.Any())
            {
                var previousPosition = _positions.Last();

                position.DinstanceToPrevious = previousPosition.GeoCoordinate.GetDistanceTo(position.GeoCoordinate);

                if (position.Accuracy < MinimumAccuracyToCalculateSpeed)
                {
                    var secondsBetween = (position.DateTime - previousPosition.DateTime).TotalSeconds;
                    position.Speed = position.DinstanceToPrevious / secondsBetween;
                }
            }
            _positions.Add(position);
            CleanupPositions();
        }

        private void CleanupPositions()
        {
            var sinceSpeedCalculationMaximum = DateTime.Now.Subtract(MaximumTimeframeForSpeedCalculation);
            _positions.RemoveAll(x => x.DateTime < sinceSpeedCalculationMaximum);
        }

        public Position GetLastPosition()
        {
            if (!_positions.Any()) throw new KeyNotFoundException();
            return _positions.Last();
        }

        /// <summary>
        /// Gets the avg speed in a given timeframe, only of positions with a distance and a (valid) speed
        /// </summary>
        /// <param name="timeFrame"></param>
        /// <returns></returns>
        public double? GetCurrentSpeed(TimeSpan timeFrame)
        {
            var since = DateTime.Now.Subtract(timeFrame);
            var sinceMaximum = DateTime.Now.Subtract(MaximumTimeframeForSpeedCalculation);

            var positionsToMeasure = _positions
                .Where(x => x.DateTime > since)
                .Where(x => x.DateTime > sinceMaximum)
                .Where(x => x.DinstanceToPrevious.HasValue)
                .Where(x => x.Speed.HasValue)
                .Take(10)
                .ToList();

            if (!positionsToMeasure.Any()) return null;

            return positionsToMeasure.Average(x => x.Speed.Value);
        }
    }
}