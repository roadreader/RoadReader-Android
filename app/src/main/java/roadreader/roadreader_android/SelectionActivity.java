package roadreader.roadreader_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SelectionActivity extends AppCompatActivity implements View.OnClickListener {

    private Button visionBtn, sensorBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        visionBtn = findViewById(R.id.visionPlusBtn);
        sensorBtn = findViewById(R.id.sensorOnlyBtn);

        visionBtn.setOnClickListener(this);
        sensorBtn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {

            case R.id.visionPlusBtn:
                startActivity(new Intent(SelectionActivity.this, CameraActivity.class));
                break;

            case R.id.sensorOnlyBtn:
                startActivity(new Intent(SelectionActivity.this, SensorActivity.class));
                break;

        }
    }

    @Override
    public void onBackPressed() {
        Intent myIntent = new Intent(this, SelectionActivity.class);
        startActivity(myIntent);
    }
}
