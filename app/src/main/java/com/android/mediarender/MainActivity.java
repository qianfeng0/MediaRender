package com.android.mediarender;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private View surfaceVideoBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Init();
    }

    private void Init() {
        surfaceVideoBtn = findViewById(R.id.surface_video);
        surfaceVideoBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "onClick: surfaceVideoBtn");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(getApplicationContext(), SurfaceVideoActivity.class));
                Log.d("MainActivity", "intent:" + intent);
                startActivity(intent);
            }
        });

        findViewById(R.id.egl_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(getApplicationContext(), GLVideoActivity.class));
                startActivity(intent);
            }
        });
    }
}
