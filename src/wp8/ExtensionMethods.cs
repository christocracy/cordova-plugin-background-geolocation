using System;
using System.Globalization;
using Windows.Devices.Geolocation;

namespace Cordova.Extension.Commands
{
    public static class ExtensionMethods
    {
        public static string ToJson(this Geocoordinate geocoordinate)
        {
            var numberFormatInfo = (NumberFormatInfo)NumberFormatInfo.CurrentInfo.Clone();
            numberFormatInfo.NaNSymbol = "0";
            numberFormatInfo.NumberDecimalSeparator = ".";

            return string.Format("{{ " +
                                 "\"accuracy\": {0}," +
                                 "\"latitude\": {1}," +
                                 "\"longitude\": {2}," +
                                 "\"altitude\": {3}," +
                                 "\"altitudeAccuracy\": {4}," +
                                 "\"heading\": {5}," +
                                 "\"velocity\": {6}," +
                                 "\"timestamp\": {7}" +
                                 "}}"
                , geocoordinate.Accuracy.ToString(numberFormatInfo)
                , geocoordinate.Latitude.ToString(numberFormatInfo)
                , geocoordinate.Longitude.ToString(numberFormatInfo)
                , geocoordinate.Altitude.HasValue ? geocoordinate.Altitude.Value.ToString(numberFormatInfo) : "0"
                , geocoordinate.AltitudeAccuracy.HasValue ? geocoordinate.AltitudeAccuracy.Value.ToString(numberFormatInfo) : "0"
                , geocoordinate.Heading.HasValue ? geocoordinate.Heading.Value.ToString(numberFormatInfo) : "0"
                , geocoordinate.Speed.HasValue ? geocoordinate.Speed.Value.ToString(numberFormatInfo) : "0"
                , geocoordinate.Timestamp.DateTime.ToJavaScriptMilliseconds()); 
        }

        public static long ToJavaScriptMilliseconds(this DateTime dt)
        {
            return (long)dt
                .ToUniversalTime()
                .Subtract(new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc))
                .TotalMilliseconds; 
        }
    }
}