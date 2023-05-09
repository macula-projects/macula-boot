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

package dev.macula.boot.starter.oss.aspect;

import dev.macula.boot.starter.oss.platform.FileStorage;
import dev.macula.boot.starter.oss.recorder.FileRecorder;
import dev.macula.boot.starter.oss.support.FileInfo;
import dev.macula.boot.starter.oss.support.UploadPretreatment;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 文件服务切面接口，用来干预文件上传，删除等
 */
public interface FileStorageAspect {

    /**
     * 上传，成功返回文件信息，失败返回 null
     */
    default FileInfo uploadAround(UploadAspectChain chain, FileInfo fileInfo, UploadPretreatment pre,
        FileStorage fileStorage, FileRecorder fileRecorder) {
        return chain.next(fileInfo, pre, fileStorage, fileRecorder);
    }

    /**
     * 删除文件，成功返回 true
     */
    default boolean deleteAround(DeleteAspectChain chain, FileInfo fileInfo, FileStorage fileStorage,
        FileRecorder fileRecorder) {
        return chain.next(fileInfo, fileStorage, fileRecorder);
    }

    /**
     * 文件是否存在，成功返回文件内容
     */
    default boolean existsAround(ExistsAspectChain chain, FileInfo fileInfo, FileStorage fileStorage) {
        return chain.next(fileInfo, fileStorage);
    }

    /**
     * 下载文件，成功返回文件内容
     */
    default void downloadAround(DownloadAspectChain chain, FileInfo fileInfo, FileStorage fileStorage,
        Consumer<InputStream> consumer) {
        chain.next(fileInfo, fileStorage, consumer);
    }

    /**
     * 下载缩略图文件，成功返回文件内容
     */
    default void downloadThAround(DownloadThAspectChain chain, FileInfo fileInfo, FileStorage fileStorage,
        Consumer<InputStream> consumer) {
        chain.next(fileInfo, fileStorage, consumer);
    }
}
