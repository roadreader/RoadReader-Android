package roadreader.roadreader_android;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class GPSPoint {
        final String API_KEY = "YK9ZNUS6";

    HashMap<String, ArrayList<Float>> sensor_data;
    long time;
    double lat, lng;

    public GPSPoint (HashMap<String, ArrayList<Float>> sensorData, double latitude, double longitude, long timestamp) {

        sensor_data = new HashMap<>(sensorData);
        lat = latitude;
        lng = longitude;
        time = timestamp;
    }

    /**
     * Constructor for testing
     * @param latitude
     * @param longitude
     */
    public GPSPoint(double latitude, double longitude) {
        lat = latitude;
        lng = longitude;
    }

    public HashMap<String, ArrayList<Float>> getSensorData() {
        return sensor_data;
    }

    public long getTime() {
        return time;
    }

    public double getLat() {
        return lat;
    }

    public double getLong() {
        return lng;
    }


}
