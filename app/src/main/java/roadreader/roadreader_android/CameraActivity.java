package roadreader.roadreader_android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import roadreader.roadreader_android.media.CameraHelper;
import roadreader.roadreader_android.media.CameraPreview;

public class CameraActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int MEDIA_RECORDER_REQUEST = 0;

    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private CamcorderProfile profile;
    private FrameLayout layout;
    private CameraPreview cameraPreview;
    private TextureView mPreview;
    private MediaRecorder mMediaRecorder;
    private File mOutputFile;
    private OutputStream outputStream;

    private boolean isRecording = false;
    private static final String TAG = "Recorder";
    private Button captureButton;
    protected ImageView redDot;
    protected Animation redDotAnimation;
    private boolean canRecord = false;
    FirebaseUser user;

    String timeStamp;

    private GPS gps;

    private final String[] requiredPermissions = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mPreview = (TextureView) findViewById(R.id.textureView);
        captureButton = (Button) findViewById(R.id.button_capture);
        layout = (FrameLayout) findViewById(R.id.camera_preview);
        redDot = findViewById(R.id.redDot);

        redDot.setVisibility(View.INVISIBLE);

        if (areCameraPermissionGranted()) {
            initCamera();
        } else {
            requestCameraPermissions();
        }

    }

    private void initCamera() {

        mCamera = CameraHelper.getDefaultBackFacingCameraInstance();
        mCameraInfo = CameraHelper.getDefaultCameraInfo();

        int defaultRotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int correctRotation = CameraHelper.getCorrectCameraOrientation(defaultRotation,mCamera,mCameraInfo);

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();

        parameters.setRotation(correctRotation);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        mCamera.setParameters(parameters);
        mCamera = CameraHelper.getDefaultBackFacingCameraInstance();
        cameraPreview = new CameraPreview(this, mCamera);
        layout.addView(cameraPreview);
    }


    /**
     * The capture button controls all user interaction. When recording, the button click
     * stops recording, releases {@link android.media.MediaRecorder} and {@link android.hardware.Camera}. When not recording,
     * it prepares the {@link android.media.MediaRecorder} and starts recording.
     *
     * @param view the view generating the event.
     */
    public void onCaptureClick(View view) {

        if (areCameraPermissionGranted()) {
            Log.d("camera","record button pressed");
            startCapture();
        } else {
            requestCameraPermissions();
        }
    }

    private void startRedDotAnimation() {
        redDot.setVisibility(View.VISIBLE);
        redDotAnimation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        redDotAnimation.setDuration(1000); // duration - half a second
        redDotAnimation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        redDotAnimation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        redDotAnimation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
        redDot.startAnimation(redDotAnimation);
    }

    private void cancelRedDotAnimation() {
        redDotAnimation.cancel();
        redDot.setVisibility(View.INVISIBLE);
    }

    private void startCapture() {

        if (isRecording) {
            cancelRedDotAnimation();
            Log.d("camera", "isrecording");
            // BEGIN_INCLUDE(stop_release_media_recorder)

            // stop recording and release camera
            try {
                mMediaRecorder.stop();  // stop the recording
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                //mOutputFile.delete();
            }

            Trip trip;
            try {
                trip = (Trip) gps.getTrip().clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                trip = gps.getTrip();
                Log.d("RoadReader", "failed to clone trip");
            }

            gps.stop();

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


            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            // inform the user that recording has stopped
            setCaptureButtonText("Capture");
            isRecording = false;
            releaseCamera();
            //transition to listView
            startActivity(new Intent(CameraActivity.this,ListActivity.class));
        } else {

            startRedDotAnimation();

            timeStamp = String.valueOf((System.currentTimeMillis() / 1000L));
            setCaptureButtonText("Stop");
            // BEGIN_INCLUDE(prepare_start_media_recorder)

            //new MediaPrepareTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            //new MediaPrepareTask().execute(null, null, null);
            gps = new GPS(this, user.getUid());
            //gps.setListener(this);
            gps.startListening();
            isRecording = true;

            if(prepareVideoRecorder()){
                mMediaRecorder.start();
            }else {
                releaseMediaRecorder();
            }
            // END_INCLUDE(prepare_start_media_recorder)

        }
    }

    private void setCaptureButtonText(String title) {
        captureButton.setText(title);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(gps != null)
            gps.resume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(gps != null)
            gps.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        layout.removeAllViews();
        // if we are using MediaRecorder, release it first
        releaseMediaRecorder();
        // release the camera immediately on pause event
        releaseCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(gps != null)
            gps.stop();
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean prepareVideoRecorder() {

        mCamera = CameraHelper.getDefaultBackFacingCameraInstance();
        mCameraInfo = CameraHelper.getDefaultCameraInfo();

        int defaultRotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int correctRotation = CameraHelper.getCorrectCameraOrientation(defaultRotation,mCamera,mCameraInfo);

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                mSupportedPreviewSizes, mPreview.getWidth(), mPreview.getHeight());

        parameters.setRotation(correctRotation);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);


        // Use the same size for recording profile.
        profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;

        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(parameters);

        // BEGIN_INCLUDE (configure_preview)
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }
        // END_INCLUDE (configure_preview)


        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);
        mMediaRecorder.setCaptureRate(5);

        // Step 4: Set output file
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO, CameraHelper.EXTERNAL_SAVE, timeStamp);

        if (mOutputFile == null) {
            return false;
        }
        mMediaRecorder.setOutputFile(mOutputFile.getPath());
        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private boolean areCameraPermissionGranted() {

        for (String permission : requiredPermissions) {
            if (!(ActivityCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }
        return true;
    }

    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(
                this,
                requiredPermissions,
                MEDIA_RECORDER_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (MEDIA_RECORDER_REQUEST != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        boolean areAllPermissionsGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                areAllPermissionsGranted = false;
                break;
            }
        }

        if (areAllPermissionsGranted) {
            initCamera();
        } else {
            // User denied one or more of the permissions, without these we cannot record
            // Show a toast to inform the user.
            Toast.makeText(getApplicationContext(),
                    getString(R.string.need_camera_permissions),
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                isRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                CameraActivity.this.finish();
            }
            // inform the user that recording has started
            setCaptureButtonText("Stop");

        }
    }

}


