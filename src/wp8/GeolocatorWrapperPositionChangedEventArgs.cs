using Windows.Devices.Geolocation;

namespace Cordova.Extension.Commands
{
    public class GeolocatorWrapperPositionChangedEventArgs
    {
        public Geoposition Position { get; set; } 
        public PositionStatus GeolocatorLocationStatus { get; set; }
        public string DebugMessage { get; set; }
    }
}