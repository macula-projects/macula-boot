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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import dev.macula.boot.starter.oss.exception.FileStorageRuntimeException;
import dev.macula.boot.starter.oss.support.FileInfo;
import dev.macula.boot.starter.oss.support.UploadPretreatment;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 本地文件存储升级版
 */
@Getter
@Setter
public class LocalPlusFileStorage implements FileStorage {

    /* 基础路径 */
    private String basePath;
    /* 本地存储路径*/
    private String storagePath;
    /* 存储平台 */
    private String platform;
    /* 访问域名 */
    private String domain;

    @Override
    public void close() {
    }

    /**
     * 获取本地绝对路径
     */
    public String getAbsolutePath(String path) {
        return storagePath + path;
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {

        String newFileKey = basePath + fileInfo.getPath() + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);

        try {
            File newFile = FileUtil.touch(getAbsolutePath(newFileKey));
            pre.getFileWrapper().transferTo(newFile);

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = basePath + fileInfo.getPath() + fileInfo.getThFilename();
                fileInfo.setThUrl(domain + newThFileKey);
                FileUtil.writeBytes(thumbnailBytes, getAbsolutePath(newThFileKey));
            }
            return true;
        } catch (IOException e) {
            FileUtil.del(getAbsolutePath(newFileKey));
            throw new FileStorageRuntimeException(
                "文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(), e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        if (fileInfo.getThFilename() != null) {   //删除缩略图
            FileUtil.del(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()));
        }
        return FileUtil.del(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()));
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        return new File(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename())).exists();
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (InputStream in = FileUtil.getInputStream(
            getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()))) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件下载失败！platform：" + fileInfo, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        try (InputStream in = FileUtil.getInputStream(
            getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()))) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo, e);
        }
    }
}
