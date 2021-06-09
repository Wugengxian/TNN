// Tencent is pleased to support the open source community by making TNN available.
//
// Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include "style.h"
#include "image_classify_jni.h"
#include "helper_jni.h"
#include <android/bitmap.h>

static std::shared_ptr<TNN_NS::Style> gDetector;
static int gComputeUnitType = 0;
static jclass clsImageInfo;
static jmethodID midconstructorImageInfo;
static jfieldID fidimage_width;
static jfieldID fidimage_height;
static jfieldID fidimage_channel;
static jfieldID fiddata;

JNIEXPORT JNICALL jint TNN_CLASSIFY(init)(JNIEnv *env, jobject thiz, jstring modelPath, jint width, jint height, jint computeUnitType)
{
    // Reset bench description
    setBenchResult("");
    std::vector<int> nchw = {1, 3, height, width};
    gDetector = std::make_shared<TNN_NS::Style>();
    std::string protoContent, modelContent;
    std::string modelPathStr(jstring2string(env, modelPath));
    protoContent = fdLoadFile(modelPathStr + "/style.tnnproto");
    modelContent = fdLoadFile(modelPathStr + "/style.tnnmodel");
    LOGI("proto content size %d model content size %d", protoContent.length(), modelContent.length());
    TNN_NS::Status status = TNN_NS::TNN_OK;
    gComputeUnitType = computeUnitType;

    auto option = std::make_shared<TNN_NS::TNNSDKOption>();
    option->compute_units = TNN_NS::TNNComputeUnitsCPU;
    option->input_shapes = {};
    option->library_path="";
    option->proto_content = protoContent;
    option->model_content = modelContent;
    if (gComputeUnitType == 1) {
        option->compute_units = TNN_NS::TNNComputeUnitsGPU;
    } else if (gComputeUnitType == 2) {
        LOGI("the device type  %d device huawei_npu" ,gComputeUnitType);
        gDetector->setNpuModelPath(modelPathStr + "/");
        gDetector->setCheckNpuSwitch(false);
        option->compute_units = TNN_NS::TNNComputeUnitsHuaweiNPU;
    } else {
	    option->compute_units = TNN_NS::TNNComputeUnitsCPU;
    }
    status = gDetector->Init(option);

    if (status != TNN_NS::TNN_OK) {
        LOGE("detector init failed %d", (int)status);
        return -1;
    }

    if (clsImageInfo == NULL) {
        clsImageInfo = static_cast<jclass>(env->NewGlobalRef(env->FindClass("com/tencent/tnn/demo/ImageInfo")));
        midconstructorImageInfo = env->GetMethodID(clsImageInfo, "<init>", "()V");
        fidimage_width = env->GetFieldID(clsImageInfo, "image_width" , "I");
        fidimage_height = env->GetFieldID(clsImageInfo, "image_height" , "I");
        fidimage_channel = env->GetFieldID(clsImageInfo, "image_channel" , "I");
        fiddata = env->GetFieldID(clsImageInfo, "data" , "[B");
    }

    return 0;
}

JNIEXPORT jboolean TNN_CLASSIFY(checkNpu)(JNIEnv *env, jobject thiz, jstring modelPath) {
    TNN_NS::Style tmpDetector;
    std::string protoContent, modelContent;
    std::string modelPathStr(jstring2string(env, modelPath));
    protoContent = fdLoadFile(modelPathStr + "/style.tnnproto");
    modelContent = fdLoadFile(modelPathStr + "/style.tnnmodel");

    auto option = std::make_shared<TNN_NS::TNNSDKOption>();
    option->compute_units = TNN_NS::TNNComputeUnitsHuaweiNPU;
    option->input_shapes = {};
    option->library_path="";
    option->proto_content = protoContent;
    option->model_content = modelContent;

    tmpDetector.setNpuModelPath(modelPathStr + "/");
    tmpDetector.setCheckNpuSwitch(true);
    TNN_NS::Status ret = tmpDetector.Init(option);
    return ret == TNN_NS::TNN_OK;
}

JNIEXPORT JNICALL jint TNN_CLASSIFY(deinit)(JNIEnv *env, jobject thiz)
{

    gDetector = nullptr;
    return 0;
}
JNIEXPORT JNICALL jobjectArray TNN_CLASSIFY(detectFromImage)(JNIEnv *env, jobject thiz, jobject imageSource, jint width, jint height)
{
    jobjectArray imageInfoArray;
    int ret = -1;
    AndroidBitmapInfo  sourceInfocolor;
    void*              sourcePixelscolor;

    if (AndroidBitmap_getInfo(env, imageSource, &sourceInfocolor) < 0) {
        return 0;
    }

    if (sourceInfocolor.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return 0;
    }

    if ( AndroidBitmap_lockPixels(env, imageSource, &sourcePixelscolor) < 0) {
        return 0;
    }

    TNN_NS::BenchOption bench_option;
    bench_option.forward_count = 1;
    std::vector<TNN_NS::ImageInfo> imageInfoList;
    gDetector->SetBenchOption(bench_option);

    TNN_NS::DeviceType dt = TNN_NS::DEVICE_ARM;
    TNN_NS::DimsVector target_dims = {1, 3, height, width};
    std::shared_ptr<TNN_NS::Mat> input_mat = std::make_shared<TNN_NS::Mat>(dt, TNN_NS::N8UC4, target_dims, sourcePixelscolor);
    int resultList[1];

    std::shared_ptr<TNN_NS::TNNSDKInput> input = std::make_shared<TNN_NS::TNNSDKInput>(input_mat);
    std::shared_ptr<TNN_NS::TNNSDKOutput> output = gDetector->CreateSDKOutput();
    TNN_NS::Status status = gDetector->Predict(input, output);
    //get output map
    gDetector->ProcessSDKOutput(output);

    imageInfoList.push_back(dynamic_cast<TNN_NS::StyleOutput *>(output.get())->image);

    AndroidBitmap_unlockPixels(env, imageSource);

    if (status != TNN_NS::TNN_OK) {
        return 0;
    }
    if (imageInfoList.size() > 0) {
        imageInfoArray = env->NewObjectArray(imageInfoList.size(), clsImageInfo, NULL);
        for (int i = 0; i < imageInfoList.size(); i++) {
            jobject objImageInfo = env->NewObject(clsImageInfo, midconstructorImageInfo);
            int image_width = imageInfoList[i].image_width;
            int image_height = imageInfoList[i].image_height;
            int image_channel = imageInfoList[i].image_channel;
            int dataNum = image_channel * image_width * image_height;

            env->SetIntField(objImageInfo, fidimage_width, image_width);
            env->SetIntField(objImageInfo, fidimage_height, image_height);
            env->SetIntField(objImageInfo, fidimage_channel, image_channel);

            jbyteArray jarrayData = env->NewByteArray(dataNum);
            env->SetByteArrayRegion(jarrayData, 0, dataNum , (jbyte*)imageInfoList[i].data.get());
            env->SetObjectField(objImageInfo, fiddata, jarrayData);

            env->SetObjectArrayElement(imageInfoArray, i, objImageInfo);
            env->DeleteLocalRef(objImageInfo);
        }
        return imageInfoArray;
    } else {
        return 0;
    }

}
