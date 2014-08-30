using System;

namespace Cordova.Extension.Commands
{
    public class BackgroundGeoLocationOptions
    {
        public string Url; 
        public double StationaryRadius;
        public double DistanceFilterInMeters;
        public UInt32 LocationTimeoutInMilliseconds;
        public UInt32 DesiredAccuracyInMeters;
        public bool Debug;
        public bool ParsingSucceeded { get; set; }

    }
}