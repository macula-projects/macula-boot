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

package dev.macula.boot.starter.oss.platform;

import dev.macula.boot.starter.oss.support.FileInfo;
import dev.macula.boot.starter.oss.support.UploadPretreatment;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 文件存储接口，对应各个平台
 */
public interface FileStorage extends AutoCloseable {

    /**
     * 获取平台
     */
    String getPlatform();

    /**
     * 设置平台
     */
    void setPlatform(String platform);

    /**
     * 保存文件
     */
    boolean save(FileInfo fileInfo, UploadPretreatment pre);

    /**
     * 删除文件
     */
    boolean delete(FileInfo fileInfo);

    /**
     * 文件是否存在
     */
    boolean exists(FileInfo fileInfo);

    /**
     * 下载文件
     */
    void download(FileInfo fileInfo, Consumer<InputStream> consumer);

    /**
     * 下载缩略图文件
     */
    void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer);

    /**
     * 释放相关资源
     */
    void close();

}
