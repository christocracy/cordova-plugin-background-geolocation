namespace Cordova.Extension.Commands
{
    public interface IBackgroundGeoLocation
    {
        void configure(string optionsString);
        void start(string asd);
        void stop();
        void finish();
        void onPaceChange(bool isMoving);
        void setConfig(string config);
        void getStationaryLocation();
    }
}