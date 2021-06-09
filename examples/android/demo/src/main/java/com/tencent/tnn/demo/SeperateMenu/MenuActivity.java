package com.tencent.tnn.demo.SeperateMenu;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tencent.tnn.demo.ImageClassifyDetector.ImageClassifyDetectActivity;
import com.tencent.tnn.demo.ImageFaceDetector.ImageFaceDetectActivity;
import com.tencent.tnn.demo.R;
import com.tencent.tnn.demo.SegmentationAndStylization.SegmentationAndStylizationActivity;
import com.tencent.tnn.demo.StreamFaceDetector.StreamFaceDetectActivity;
import com.tencent.tnn.demo.StreamHairSegmentation.StreamHairSegmentationActivity;


public class MenuActivity extends Activity {

    private TextView lightLiveCheckBtn;

    private boolean isShowedActivity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
//       Debug.waitForDebugger();

        init();

    }

    private void init() {
        findViewById(R.id.tnn_menu_segmentation_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isShowedActivity) {
                    isShowedActivity = true;
                    Intent intent = new Intent();
                    Activity activity = MenuActivity.this;
                    intent.setClass(activity, StreamHairSegmentationActivity.class);
                    activity.startActivity(intent);
                }
            }
        });
        findViewById(R.id.tnn_menu_stylization_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isShowedActivity) {
                    isShowedActivity = true;
                    Intent intent = new Intent();
                    Activity activity = MenuActivity.this;
                    intent.setClass(activity, ImageClassifyDetectActivity.class);
                    activity.startActivity(intent);
                }
            }
        });
        findViewById(R.id.tnn_menu_segmentation_and_stylization_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isShowedActivity) {
                    isShowedActivity = true;
                    Intent intent = new Intent();
                    Activity activity = MenuActivity.this;
                    intent.setClass(activity, SegmentationAndStylizationActivity.class);
                    activity.startActivity(intent);
                }
            }
        });
        findViewById(R.id.back_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuActivity.this.finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isShowedActivity = false;
    }
}
