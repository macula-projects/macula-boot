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

package dev.macula.boot.starter.oss.support;

import cn.hutool.core.lang.Dict;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 文件id
     */
    private String id;
    /**
     * 文件访问地址
     */
    private String url;
    /**
     * 文件大小，单位字节
     */
    private Long size;
    /**
     * 文件名称
     */
    private String filename;
    /**
     * 原始文件名
     */
    private String originalFilename;
    /**
     * 基础存储路径
     */
    private String basePath;
    /**
     * 存储路径
     */
    private String path;
    /**
     * 文件扩展名
     */
    private String ext;
    /**
     * MIME 类型
     */
    private String contentType;
    /**
     * 存储平台
     */
    private String platform;
    /**
     * 缩略图访问路径
     */
    private String thUrl;
    /**
     * 缩略图名称
     */
    private String thFilename;
    /**
     * 缩略图大小，单位字节
     */
    private Long thSize;
    /**
     * 缩略图 MIME 类型
     */
    private String thContentType;
    /**
     * 文件所属对象id
     */
    private String objectId;
    /**
     * 文件所属对象类型，例如用户头像，评价图片
     */
    private String objectType;
    /**
     * 附加属性字典
     */
    private Dict attr;
    /**
     * 创建时间
     */
    private Date createTime;
}
