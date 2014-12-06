using Windows.Devices.Geolocation;

namespace Cordova.Extension.Commands
{
    public class GeolocatorWrapperPositionChangedEventArgs
    {
        public Geoposition Position { get; set; }
        public bool EnteredStationary { get; set; }
        public PositionStatus GeolocatorLocationStatus { get; set; }
        public PostionUpdateDebugData PositionUpdateDebugData { get; set; }
    }
}