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
#include "sample_timer.h"
#include <cmath>

namespace TNN_NS {
    StyleOutput::~StyleOutput() {}

    Style::~Style() {
    }

std::shared_ptr<Mat> Style::ProcessSDKInputMat(std::shared_ptr<Mat> input_mat,
                                                                   std::string name) {
    auto target_dims = GetInputShape(name);
    this->orig_dims = input_mat->GetDims();
    auto input_height = input_mat->GetHeight();
    auto input_width = input_mat->GetWidth();
    auto input_channel = input_mat->GetChannel();
    if (target_dims.size() >= 4 &&  input_channel == target_dims[1] &&
        (input_height != target_dims[2] || input_width != target_dims[3])) {
        auto target_mat = std::make_shared<TNN_NS::Mat>(input_mat->GetDeviceType(),
                                                        input_mat->GetMatType(), target_dims);
        auto status = Resize(input_mat, target_mat, TNNInterpLinear);
        if (status == TNN_OK) {
            return target_mat;
        } else {
            LOGE("%s\n", status.description().c_str());
            return nullptr;
        }
    }
    return input_mat;
}

std::shared_ptr<Mat> Style::MergeImage(std::shared_ptr<Mat> image) {
    auto new_dim = image->GetDims();
    new_dim[1] = 4;
    auto merged_image = std::make_shared<Mat>(image->GetDeviceType(), N8UC4, new_dim);
    auto alpha_data = static_cast<float *>(image->GetData());
    auto merged_image_data = static_cast<uint8_t *>(merged_image->GetData());

    auto hw = new_dim[2] * new_dim[3];
    auto channel = 3;
    for(int s=0; s<hw; ++s) {
        float c0 = alpha_data[hw * 0 + s];
        float c1 = alpha_data[hw * 1 + s];
        float c2 = alpha_data[hw * 2 + s];
        float c3 = 255;

        merged_image_data[s*4 + 0] = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, c0)));
        merged_image_data[s*4 + 1] = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, c1)));
        merged_image_data[s*4 + 2] = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, c2)));
        merged_image_data[s*4 + 3] = static_cast<uint8_t>(std::min(255.0f, std::max(0.0f, c3)));
    }
    return merged_image;
}

MatConvertParam Style::GetConvertParamForInput(std::string tag) {
    MatConvertParam input_cvt_param;
    input_cvt_param.scale = {1.0, 1.0, 1.0, 0.0};
    input_cvt_param.bias  = {0, 0,   0,   0.0};
    return input_cvt_param;
}

std::shared_ptr<TNNSDKOutput> Style::CreateSDKOutput() {
    return std::make_shared<StyleOutput>();
}

Status Style::ProcessSDKOutput(std::shared_ptr<TNNSDKOutput> output_) {
    Status status = TNN_OK;
    auto output = dynamic_cast<StyleOutput *>(output_.get());
    RETURN_VALUE_ON_NEQ(!output, false,
                        Status(TNNERR_PARAM_ERR, "TNNSDKOutput is invalid"));
    output->image = ImageInfo(MergeImage(output->GetMat("output")));

    return status;
}

}
