package com.tencent.tnn.demo.StreamHairSegmentation;

import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.tnn.demo.R;
import com.tencent.tnn.demo.common.activity.DemoBaseActivity;

import java.io.File;

public class StreamHairSegmentationActivity extends DemoBaseActivity {
    private static final String TAG = StreamHairSegmentationActivity.class.getSimpleName();
    private static final int FACE_PERMISSION_QUEST_CAMERA = 1024;
    private int height = 480, width = 640;
    private String modelname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.base_activity_layout);
        Intent intent = getIntent();
        height = intent.getIntExtra("height", 480);
        width = intent.getIntExtra("width", 640);
        modelname = intent.getStringExtra("modelname");

        if (Build.VERSION.SDK_INT >= 23) {
            Log.d(TAG, "begin askForPermission the sdk version is" + Build.VERSION.SDK_INT);
            askForPermission();
        } else {
            Log.d(TAG, "no need to askForPermission the sdk version is" + Build.VERSION.SDK_INT);
            updateUI();
        }
    }

    public void updateUI() {
        Fragment fragment = StreamHairSegmentationFragment.newInstance(height, width, modelname);
        getFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Activity onResume");
        super.onResume();

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Activity onPause");
        super.onPause();

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Activity onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Activity onDestroy");
        super.onDestroy();
    }
}
