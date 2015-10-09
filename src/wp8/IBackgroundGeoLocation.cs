namespace Cordova.Extension.Commands
{
    public interface IBackgroundGeoLocation
    {
        void configure(string optionsString);
        void start(string args);
        void stop(string args);
        void finish(string args);
        void onPaceChange(bool isMoving);
        void setConfig(string config);
        void getStationaryLocation(string args);
        void addStationaryRegionListener(string args);
    }
}