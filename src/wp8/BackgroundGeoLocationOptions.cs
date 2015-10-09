using System;

namespace Cordova.Extension.Commands
{
    public class BackgroundGeoLocationOptions
    {
        public double StationaryRadius;
        public double DistanceFilterInMeters;
        public UInt32 LocationTimeoutInSeconds;
        public UInt32 DesiredAccuracyInMeters;
        public bool Debug;
        public bool StopOnTerminate;
        public bool ParsingSucceeded { get; set; }

    }
}
