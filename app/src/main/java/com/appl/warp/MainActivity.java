package com.appl.warp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    WarpView mWarpView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWarpView = findViewById(R.id.warp_view);
        mWarpView.startWarpAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWarpView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWarpView.onPause();
    }
}
