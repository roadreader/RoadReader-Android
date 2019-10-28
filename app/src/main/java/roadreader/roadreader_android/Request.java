package roadreader.roadreader_android;

import android.content.Context;
import android.hardware.Sensor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class Request {

    private static final String USER_AGENT = "Mozilla/5.0";
    String tripId;
    String path;
    Context display;

    public Request(Context d) {
        display = d;
    }

    public Request() {

    }

    /**
     * Sends a GET request to specified URL
     * @param s The URL that the request is sent to
     * @return The response of the request. Returns an empty string if request fails
     * @throws IOException
     */
    public String sendGET(String s) throws IOException {
        URL url = new URL(s);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();

        Log.d( "GET request","GET Response Code :: " + responseCode);
        System.out.println("GET Response Code :: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // log result
            Log.d("GET request" , response.toString());
            System.out.println(response.toString());
            con.disconnect();
            return response.toString();

        } else {
            Log.d( "GET request","GET request did not work");
            System.out.println("GET request did not work");
            con.disconnect();
            return "";
        }
    }

    public String sendPOST() throws IOException {

        return "";
    }

    public void sendTrip (File file) throws FileNotFoundException {

        //read trip.json file and convert it to trip class
        BufferedReader br = new BufferedReader(new FileReader(file));
        final Trip trip =  new Gson().fromJson(br, Trip.class);
        List<GPSPoint> pts = trip.getGpsPoints();

        Log.d("database", "User ID: " + trip.getUserId());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("trips").add(trip)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("database", "DocumentSnapshot written with ID: " + documentReference.getId());
                        tripId = documentReference.getId();
                        Log.d("database", "tripId: " + tripId);
                        Toast.makeText(display,tripId,Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("database", "Error adding document", e);
                    }
                });
    }

    /**
     * Sends Trip file to firebase. On success, calls the sendVideo method to upload the
     * corresponding video.
     * @param file The Trip file to be uploaded
     * @param videoPath Absolute path of video corresponding to the Trip file
     * @throws FileNotFoundException If Trip file cannot be found
     */
    public void sendTripWithVideo (File file, final String videoPath) throws FileNotFoundException {

        //read trip.json file and convert it to trip class
        BufferedReader br = new BufferedReader(new FileReader(file));
        final Trip trip =  new Gson().fromJson(br, Trip.class);
        List<GPSPoint> pts = trip.getGpsPoints();

        Log.d("database", "User ID: " + trip.getUserId());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("trips").add(trip)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("database", "DocumentSnapshot written with ID: " + documentReference.getId());
                        tripId = documentReference.getId();
                        Log.d("database", "tripId: " + tripId);
                        Toast.makeText(display,tripId,Toast.LENGTH_SHORT).show();
                        sendVideo(videoPath, trip.getUserId() + "/" + tripId); //send video if trip uploaded
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("database", "Error adding document", e);
                    }
                });
    }

    /**
     * Sends the video corresponding to the trip file that was uploaded
     * @param filePath Absolute path of video file
     * @param ref Determines path that the video will be stored on Firebase.
     *            Path is "[userId]/[tripId]"
     */
    private void sendVideo(String filePath, String ref) {

        Uri file = Uri.fromFile(new File(filePath));

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference vidRef = storageRef.child(ref);
        StorageMetadata metadata = new StorageMetadata.Builder().build();

        UploadTask uploadTask = vidRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.d("database", "Failed to upload video");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                Log.d("database", "Successfully uploaded video");
                //display.delete(true);

            }
        });

    }
}
