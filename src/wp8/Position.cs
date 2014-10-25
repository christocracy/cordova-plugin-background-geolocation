using System;
using System.Device.Location;

namespace Cordova.Extension.Commands
{
    public class Position
    {
        public DateTime DateTime { get; private set; }
        public GeoCoordinate GeoCoordinate { get; private set; }

        /// <summary>
        /// Distance to previous position in meters
        /// </summary>
        public double? DinstanceToPrevious { get; set; }

        /// <summary>
        /// Speed to previous position in meter/seconde
        /// </summary>
        public double? Speed { get; set; }


        public double Accuracy { get; set; }

        public Position(GeoCoordinate geoCoordinate, DateTime dateTime, double accuracy)
        {
            GeoCoordinate = geoCoordinate;
            DateTime = dateTime;
            Accuracy = accuracy;
        }
    }
}