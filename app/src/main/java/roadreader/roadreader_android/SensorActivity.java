package roadreader.roadreader_android;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SensorActivity extends AppCompatActivity implements SendSensorData, SendGPSData{

    private TextView ax, ay, az;
    private TextView lat, lng;
    private Button sensorBtn;
    private boolean isRecording = false;

    private GPS gps;
    String timeStamp;
    private File mOutputFile;
    private OutputStream outputStream;
    FirebaseUser user;

    private final String[] requiredPermissions = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        ax = findViewById(R.id.ax);
        ay = findViewById(R.id.ay);
        az = findViewById(R.id.az);
        lat = findViewById(R.id.lat);
        lng = findViewById(R.id.lng);

        sensorBtn = findViewById(R.id.sensorBtn);

    }

    @Override
    public void sendData(final float _ax, final float _ay, final float _az) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ax.setText(String.valueOf(_ax));
                ay.setText(String.valueOf(_ay));
                az.setText(String.valueOf(_az));
            }
        });


    }

    @Override
    public void sendGPSData(final double _lat, final double _lng) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lat.setText(String.valueOf(_lat));
                lng.setText(String.valueOf(_lng));
            }
        });


    }

    public void onSensorBtnClick(View view){
        startRecord();
    }

    private void setCaptureButtonText(String title) {
        sensorBtn.setText(title);
    }

    public void startRecord() {

        if (isRecording) {

            Trip trip;
            try {
                trip = (Trip) gps.getTrip().clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                trip = gps.getTrip();
                Log.d("RoadReader", "failed to clone trip");
            }

            gps.stopListening();

            Gson gson = new Gson();

            File tripInternalDir = new File(getFilesDir(), "Trips");
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!tripInternalDir.exists()) {
                if (!tripInternalDir.mkdirs()) {
                    Log.d("RoadReader", "failed to create directory");
                }
            }

            try {
                File tripFile = new File(tripInternalDir, timeStamp + ".json");
                //gson.toJson(trip, new FileWriter(tripFile));
                Log.d("trip","Writing trip to file");
                outputStream = new FileOutputStream(getFilesDir() + "/" + "Trips/" + tripFile.getName());
                //outputStream = openFileOutputtripFile.getAbsolutePath(), Context.MODE_PRIVATE);
                outputStream.write(gson.toJson(trip).getBytes());
                outputStream.close();
                Log.d("trip","Trip to String:");
                Log.d("trip", gson.toJson(trip));
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("trip", "failed to write trip to file");
            }

            setCaptureButtonText("Record");
            isRecording = false;

            //upload
            upload();

        }else {
            setCaptureButtonText("Upload");
            timeStamp = String.valueOf((System.currentTimeMillis() / 1000L));
            gps = new GPS(this, user.getUid());
            gps.setListener(this);
            gps.sensor.setListener(this);
            gps.sensor.setTimer();
            gps.startListening();
            isRecording = true;
        }

    }

    public void upload() {

        final File tripFile = new File(getFilesDir(), "Trips/" + timeStamp + ".json");
        final Request request = new Request(SensorActivity.this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    request.sendTrip(tripFile);
                }catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });


        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                //upload
                File tripFile = new File(getFilesDir(), "Trips/" + timeStamp + ".json");
                Request request = new Request(SensorActivity.this);

                try{
                    final String id = request.sendTrip(tripFile);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SensorActivity.this,id,Toast.LENGTH_SHORT).show();
                        }
                    });
                }catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        */

    }

    @Override
    protected void onResume() {
        Log.d("trip", "trip resumed");
        super.onResume();
        if(gps != null)
            gps.resume();
    }

    @Override
    protected void onStart() {
        Log.d("trip", "trip started");
        super.onStart();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(gps != null)
            gps.start();
    }

    @Override
    protected void onPause() {
        Log.d("trip", "trip paused");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("trip", "trip stopped");
        super.onStop();
        if(gps != null)
            gps.stop();
    }



}
