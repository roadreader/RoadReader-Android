package roadreader.roadreader_android;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class DisplayActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button uploadBtn, deleteBtn;
    private TextView videoTitle;
    private ImageButton playBtn;
    private File video;


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

        initPlayer();

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
            videoView.start();
            playBtn.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    private void upload() throws FileNotFoundException {
        String tripName = getTimestamp(video.getName());
        File tripFile = new File(getFilesDir(), "Trips/" + tripName + ".json");
        Request request = new Request(this);
        request.sendTripWithVideo(tripFile, video.getAbsolutePath());
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
}
