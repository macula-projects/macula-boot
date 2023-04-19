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

/*
 * 爱组搭 http://aizuda.com 低代码组件化开发平台
 * ------------------------------------------
 * 受知识产权保护，请勿删除版权申明
 */
package dev.macula.boot.starter.oss.platform;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import dev.macula.boot.starter.oss.config.OssProperty;
import dev.macula.boot.starter.oss.core.AbstractFileStorage;
import dev.macula.boot.starter.oss.core.MultipartUploadResponse;
import dev.macula.boot.starter.oss.model.OssResult;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 本地存储
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品
 *
 * @author 青苗
 * @since 2022-06-30
 */
public class Local extends AbstractFileStorage {

    public Local(OssProperty ossProperty) {
        this.ossProperty = ossProperty;
    }

    @Override
    public OssResult upload(InputStream is, String filename, String objectName) throws Exception {
        String suffix = this.getFileSuffix(filename);
        String _objectName = this.getObjectName(suffix, objectName);
        File file = new File(this.getLocalFilePath(_objectName));
        if (!file.exists()) {
            // 文件不存在则创建文件，先创建目录
            File dir = new File(file.getParent());
            dir.mkdirs();
            file.createNewFile();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IoUtil.copy(is, fos);
        }
        return OssResult.builder().bucketName(this.ossProperty.getLocalFileUrl()).objectName(_objectName)
            .filename(filename).suffix(suffix).build();
    }

    protected String getLocalFilePath(String objectName) throws FileNotFoundException {
        return this.ossProperty.getLocalFilePath(objectName);
    }

    @Override
    public InputStream download(String objectName) throws Exception {
        try (FileInputStream fis = new FileInputStream(this.getObjectFile(objectName))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IoUtil.copy(fis, out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Override
    public boolean delete(List<String> objectNameList) throws Exception {
        if (ObjectUtil.isEmpty(objectNameList)) {
            return false;
        }
        for (String objectName : objectNameList) {
            this.delete(objectName);
        }
        return true;
    }

    @Override
    public boolean delete(String objectName) throws Exception {
        File file = this.getObjectFile(objectName);
        if (null == file || !file.exists()) {
            return false;
        }
        return file.delete();
    }

    protected File getObjectFile(String objectName) throws FileNotFoundException {
        return new File(this.getLocalFilePath(objectName));
    }

    @Override
    public String getUrl(String objectName, int duration, TimeUnit unit) throws Exception {
        return objectName;
    }

    @Override
    public MultipartUploadResponse getUploadSignedUrl(String filename) {
        return null;
    }
}
