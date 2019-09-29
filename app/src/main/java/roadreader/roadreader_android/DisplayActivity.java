package roadreader.roadreader_android;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

public class DisplayActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button uploadBtn, deleteBtn;
    private TextView videoTitle;
    private ImageButton playBtn;
    private File video;

    private BroadcastReceiver mBroadcastReceiver;
    private ProgressDialog mProgressDialog;

    private static final String KEY_FILE_URI = "key_file_uri";
    private static final String TAG = "DisplayActivity";

    private Uri mFileUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        videoView = findViewById(R.id.videoView);
        videoTitle = findViewById(R.id.videoTitle);
        playBtn = findViewById(R.id.playBtn);

        uploadBtn = findViewById(R.id.uploadBtn);
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    upload();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        deleteBtn = findViewById(R.id.deleteBtn);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delete(false);
            }
        });

        // Restore instance state
        if (savedInstanceState != null) {
            mFileUri = savedInstanceState.getParcelable(KEY_FILE_URI);
        }
        onNewIntent(getIntent());

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive:" + intent);
                hideProgressDialog();

                switch (intent.getAction()) {
                    case MyUploadService.UPLOAD_COMPLETED:
                        onUploadResultIntent(intent,"Success");
                        break;
                    case MyUploadService.UPLOAD_ERROR:
                        onUploadResultIntent(intent,"Error");
                        break;
                }
            }
        };

        initPlayer();

    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check if this Activity was launched by clicking on an upload notification
        if (intent.hasExtra(MyUploadService.EXTRA_DOWNLOAD_URL)) {
            onUploadResultIntent(intent,"Success");
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        // Register receiver for uploads and downloads
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mBroadcastReceiver, MyUploadService.getIntentFilter());
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unregister download receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable(KEY_FILE_URI, mFileUri);
    }

    private void uploadTrip() throws FileNotFoundException {
        String tripName = getTimestamp(video.getName());
        File tripFile = new File(getFilesDir(), "Trips/" + tripName + ".json");
        //read trip.json file and convert it to trip class
        BufferedReader br = new BufferedReader(new FileReader(tripFile));
        final Trip trip =  new Gson().fromJson(br, Trip.class);
        List<GPSPoint> pts = trip.getGpsPoints();

        Log.d("database", "User ID: " + trip.getUserId());

        FirebaseFirestore db = FirebaseFirestore.getInstance();


        db.collection("trips").add(trip)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("database", "DocumentSnapshot written with ID: " + documentReference.getId());
                        String tripId = documentReference.getId();
                        Log.d("database", "tripId: " + tripId);
                        Toast.makeText(DisplayActivity.this,tripId,Toast.LENGTH_SHORT).show();

                        //now upload video using uploadTask
                        uploadFromUri(Uri.fromFile(new File(video.getAbsolutePath())), trip.getUserId() + "/" + tripId);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("database", "Error adding document", e);
                    }
                });
    }

    private void uploadFromUri(Uri fileUri, String videoPath) {
        Log.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        // Save the File URI
        mFileUri = fileUri;

        // Start MyUploadService to upload the file, so that the file is uploaded
        // even if this Activity is killed or put in the background
        startService(new Intent(this, MyUploadService.class)
                .putExtra(MyUploadService.EXTRA_FILE_URI, fileUri)
                .putExtra(MyUploadService.EXTRA_FILE_PATH, videoPath)
                .setAction(MyUploadService.ACTION_UPLOAD));

        // Show loading spinner
        showProgressDialog(getString(R.string.progress_uploading));
    }

    private void onUploadResultIntent(Intent intent, String message) {
        // Got a new intent from MyUploadService with a success or failure
        mFileUri = intent.getParcelableExtra(MyUploadService.EXTRA_FILE_URI);
        hideProgressDialog();
        showMessageDialog(message, mFileUri.toString());
    }

    private void showMessageDialog(String title, String message) {
        AlertDialog ad = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .create();
        ad.show();
    }

    private void showProgressDialog(String caption) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.setMessage(caption);
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }


    private boolean getVideoInfo() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if(bundle != null) {
            video = (File)bundle.get("file");
            return true;
        }
        return false;
    }

    private void initPlayer() {
        if (getVideoInfo()) {
            videoTitle.setText(video.getName());
            videoView.setVideoPath(video.getAbsolutePath());
            //videoView.start();
            //playBtn.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    private void upload() throws FileNotFoundException {
        //String tripName = getTimestamp(video.getName());
        //File tripFile = new File(getFilesDir(), "Trips/" + tripName + ".json");
        //Request request = new Request(this);
        //request.sendTripWithVideo(tripFile, video.getAbsolutePath());
        uploadTrip();
    }

    public void delete(Boolean isSent) {
        videoView.stopPlayback();
        if(isSent)
            Toast.makeText(this, "Video Sent!", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Video Deleted!", Toast.LENGTH_SHORT).show();
        video.delete();
        startActivity(new Intent(DisplayActivity.this, ListActivity.class));
    }

    private String getTimestamp(String videoName) {
        int startIndex = 4;
        int endIndex = videoName.indexOf(".mp4");
        videoName = videoName.substring(startIndex,endIndex);
        return videoName;
    }

    @Override
    public void onBackPressed() {
        Intent myIntent = new Intent(this, ListActivity.class);
        startActivity(myIntent);
    }
}
