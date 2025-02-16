package com.tencent.tnn.demo.SegmentationAndStylization;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.tencent.tnn.demo.FileUtils;
import com.tencent.tnn.demo.HairSegmentation;
import com.tencent.tnn.demo.ImageClassify;
import com.tencent.tnn.demo.ImageInfo;
import com.tencent.tnn.demo.R;
import com.tencent.tnn.demo.common.fragment.BaseFragment;

import java.nio.ByteBuffer;


public class SegmentationAndStylizationFragment extends BaseFragment {

    private final static String TAG = SegmentationAndStylizationFragment.class.getSimpleName();
    private ImageClassify mImageClassify = new ImageClassify();
    private HairSegmentation mHairSegmentation = new HairSegmentation();
    private static final String IMAGE = "girl.jpg";
    private static final String STYLE_IMAGE = "candy.jpg";
    private static final String RESULT_LIST = "synset.txt";
    private static final int NET_INPUT = 720;
    private ToggleButton mGPUSwitch;
    private Button mRunButton;
    private boolean mUseGPU = false;
    //add for npu
    private ToggleButton mHuaweiNPUswitch;
    private boolean mUseHuaweiNpu = false;
    private TextView HuaweiNpuTextView;

    /**********************************     Get Preview Advised    **********************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        System.loadLibrary("tnn_wrapper");
        String segmentationModelPath = initSegmentationModel();
        String stylizationModelPath = initStylizationModel();
        NpuEnable = mImageClassify.checkNpu(stylizationModelPath);
    }

    private String initSegmentationModel() {
        String targetDir = getActivity().getFilesDir().getAbsolutePath();

        // copy segmentation model to sdcard
        String[] modelPathsSegmentation = {
                "segmentation.tnnmodel",
                "segmentation.tnnproto",
        };

        for (int i = 0; i < modelPathsSegmentation.length; i++) {
            String modelFilePath = modelPathsSegmentation[i];
            String interModelFilePath = targetDir + "/" + modelFilePath;
            FileUtils.copyAsset(getActivity().getAssets(), "hair_segmentation/" + modelFilePath, interModelFilePath);
        }
        return targetDir;
    }

    private String initStylizationModel() {
        String targetDir = getActivity().getFilesDir().getAbsolutePath();

        //copy detect model to sdcard
        String[] modelPathsDetector = {
                "style.tnnmodel",
                "style.tnnproto",
        };

        for (int i = 0; i < modelPathsDetector.length; i++) {
            String modelFilePath = modelPathsDetector[i];
            String interModelFilePath = targetDir + "/" + modelFilePath;
            FileUtils.copyAsset(getActivity().getAssets(), "SqueezeNet/" + modelFilePath, interModelFilePath);
        }
        return targetDir;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.back_rl) {
            clickBack();
        }
    }

    private void onSwichGPU(boolean b) {
        if (b && mHuaweiNPUswitch.isChecked()) {
            mHuaweiNPUswitch.setChecked(false);
            mUseHuaweiNpu = false;
        }
        mUseGPU = b;
        TextView result_view = (TextView) $(R.id.result);
        result_view.setText("");
    }

    private void onSwichNPU(boolean b) {
        if (b && mGPUSwitch.isChecked()) {
            mGPUSwitch.setChecked(false);
            mUseGPU = false;
        }
        mUseHuaweiNpu = b;
        TextView result_view = (TextView) $(R.id.result);
        result_view.setText("");
    }

    private void clickBack() {
        if (getActivity() != null) {
            (getActivity()).finish();
        }
    }

    @Override
    public void setFragmentView() {
        Log.d(TAG, "setFragmentView");
        setView(R.layout.triple_image_show);
        setTitleGone();
        $$(R.id.back_rl);
        $$(R.id.gpu_switch);
        mGPUSwitch = $(R.id.gpu_switch);
        mGPUSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                onSwichGPU(b);
            }
        });
        $$(R.id.npu_switch);
        mHuaweiNPUswitch = $(R.id.npu_switch);
        mHuaweiNPUswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                onSwichNPU(b);
            }
        });

        HuaweiNpuTextView = $(R.id.npu_text);
        if (!NpuEnable) {
            HuaweiNpuTextView.setVisibility(View.INVISIBLE);
            mHuaweiNPUswitch.setVisibility(View.INVISIBLE);
        }

        mRunButton = $(R.id.run_button);
        mRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDetect();
            }
        });

        final Bitmap originBitmap = FileUtils.readBitmapFromFile(getActivity().getAssets(), IMAGE);
        ImageView contentImage = (ImageView) $(R.id.contentPicture);
        contentImage.setImageBitmap(originBitmap);

        final Bitmap styleBitmap = FileUtils.readBitmapFromFile(getActivity().getAssets(), STYLE_IMAGE);
        ImageView styleImage = (ImageView) $(R.id.stylePicture);
        styleImage.setImageBitmap(styleBitmap);
    }

    @Override
    public void openCamera() {

    }

    @Override
    public void startPreview(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void closeCamera() {

    }


    private void startDetect() {
        long startTime = System.currentTimeMillis();
        final Bitmap originBitmap = FileUtils.readBitmapFromFile(getActivity().getAssets(), IMAGE);

        final Bitmap scaleBitmap = Bitmap.createScaledBitmap(originBitmap, NET_INPUT, NET_INPUT, false);
        ImageView source = (ImageView) $(R.id.contentPicture);
        source.setImageBitmap(originBitmap);
        String modelPath = initStylizationModel();
        ImageView resultPicture = (ImageView) $(R.id.resultPicture);
        Log.d(TAG, "Init classify " + modelPath);
        int device = 0;
        if (mUseHuaweiNpu) {
            device = 2;
        } else if (mUseGPU) {
            device = 1;
        }
        int result = mImageClassify.init(modelPath, NET_INPUT, NET_INPUT, device);
        Bitmap styleBitmap = null;
        if (result == 0) {
            ImageInfo[] indexArray = mImageClassify.detectFromImage(scaleBitmap, NET_INPUT, NET_INPUT);
            ImageInfo imageInfo = indexArray[0];
            Bitmap bitmap = Bitmap.createBitmap(imageInfo.image_width, imageInfo.image_height, Bitmap.Config.ARGB_8888);
            ByteBuffer buffer = ByteBuffer.wrap(imageInfo.data);
            bitmap.copyPixelsFromBuffer(buffer);
            styleBitmap = Bitmap.createScaledBitmap(bitmap, originBitmap.getWidth(), originBitmap.getHeight(), false);
            mImageClassify.deinit();
        } else {
            Log.e(TAG, "failed to init stylization model " + result);
        }
        result = mHairSegmentation.init(modelPath, NET_INPUT, NET_INPUT, "segmentation", device);
        if (result == 0) {
            ImageInfo[] indexArray = mHairSegmentation.predictFromPicture(originBitmap, originBitmap.getWidth(), originBitmap.getHeight());
            ImageInfo imageInfo = indexArray[0];
            byte[] mask = imageInfo.data;
            int index = 0;
            assert styleBitmap != null;
            int height = styleBitmap.getHeight();
            int width = styleBitmap.getWidth();
            for (int x = 0; x < height; x++) {
                for (int y = 0; y < width; y++) {
                    if (mask[index*4] == 0) {
                        styleBitmap.setPixel(y, x, originBitmap.getPixel(y, x));
                    } else {
                        Log.d(TAG, "styled");
                    }
                    index++;
                }
            }
            resultPicture.setImageBitmap(styleBitmap);
            mHairSegmentation.deinit();
        } else {
            Log.e(TAG, "failed to init segmentation model " + result);
        }
        TextView result_view = (TextView)$(R.id.timeCost);
        long timeCost = System.currentTimeMillis()-startTime;
        String s = "time cost : "+timeCost/1000.0+"s";
        result_view.setText(s);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        getFocus();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    private void preview() {
        Log.i(TAG, "preview");

    }

    private void getFocus() {
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    clickBack();
                    return true;
                }
                return false;
            }
        });
    }

}
