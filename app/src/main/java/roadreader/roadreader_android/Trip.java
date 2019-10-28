package roadreader.roadreader_android;

import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Trip implements Cloneable{

    private List<GPSPoint> gpsPoints;
    private String userId;
    //private String tripID;
    //Queue<GPSPoint> gpsQueue;

    public Trip() {

    }

    public Trip(String uId) {
        gpsPoints = new ArrayList<>();
        userId = uId;
        //gpsQueue = new PriorityQueue<>();
    }

    public Object clone() throws
            CloneNotSupportedException
    {
        return super.clone();
    }

    /*
    public void setTripId(String ref) {
        tripID = ref;
    }

    public String getTripId() {
        return tripID;
    }
    */

    public String getUserId() {
        return userId;
    }

    public List<GPSPoint> getGpsPoints() {
        return gpsPoints;
    }

    /**
     * Creates new GPS instance and adds it to the trip.
     * @param sensor_data Data from accelerometer and gyroscope since last recorded GPS location
     * @param latitude
     * @param longitude
     */
    public void addGPSPoint(HashMap<String, ArrayList<Float>> sensor_data,
                       double latitude, double longitude, long time) {

        gpsPoints.add(new GPSPoint(sensor_data, latitude, longitude, time));

    }



}
