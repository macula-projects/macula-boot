/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import API from "@/api";

//上传配置

export default {
    apiObj: API.common_common.upload,			//上传请求API对象
    filename: "file",					//form请求时文件的key
    successCode: 200,					//请求完成代码
    maxSize: 10,						//最大文件大小 默认10MB
    parseData: function (res) {
        return {
            code: res.code,				//分析状态字段结构
            fileName: res.data.fileName,//分析文件名称
            src: res.data.src,			//分析图片远程地址结构
            msg: res.msg			//分析描述字段结构
        }
    },
    apiObjFile: API.common_common.uploadFile,	//附件上传请求API对象
    maxSizeFile: 10						//最大文件大小 默认10MB
}
