package com.tenforwardconsulting.cordova.bgloc.data;

import com.tenforwardconsulting.cordova.bgloc.data.Location;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Helper class to translate Locations into JSON objects.  The JSON objects aim to mimick the https://developer.mozilla.org/en-US/docs/Web/API/Position API.
 */
public class LocationSerializer {

    /**
     * Translate a Location to its JSON representation that mimicks https://developer.mozilla.org/en-US/docs/Web/API/Position.
     * @param location to translate
     * @return A JSON representation of the provided Location
     */
    public static JSONObject toJSON(Location location) throws JSONException {
        JSONObject obj = new JSONObject();
        JSONObject coords = new JSONObject();
        coords.put("latitude", Double.parseDouble(location.getLatitude()));
        coords.put("longitude", Double.parseDouble(location.getLongitude()));
        coords.put("accuracy", Double.parseDouble(location.getAccuracy()));
        coords.put("speed", Double.parseDouble(location.getSpeed()));
        coords.put("bearing", Double.parseDouble(location.getBearing()));
        coords.put("altitude", Double.parseDouble(location.getAltitude()));
        obj.put("coords", coords);
        obj.put("timestamp", location.getRecordedAt());
        return obj;
    }


    /**
     * Translate an array of Locations to a JSON array where each location mimicks a https://developer.mozilla.org/en-US/docs/Web/API/Position.
     * @param locations to translate
     * @return A JSON representation of the provided locations
     */
    public static JSONArray toJSON(Location[] locations) throws JSONException {
        JSONArray jsonLocations = new JSONArray();
        for (Location location : locations) {
            JSONObject jsonLocation = LocationSerializer.toJSON(location);
            jsonLocations.put(jsonLocation);
        }
        return jsonLocations;
    }

}