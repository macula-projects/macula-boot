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
package dev.macula.boot.starter.oss.core;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import dev.macula.boot.starter.oss.config.OssProperty;
import dev.macula.boot.starter.oss.exception.MediaTypeException;
import dev.macula.boot.starter.oss.utils.ThreadLocalUtils;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * aizuda 抽象文件存储类
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品
 *
 * @author 青苗
 * @since 2022-03-22
 */
public abstract class AbstractFileStorage implements IFileStorage {
    /**
     * 配置属性
     */
    protected OssProperty ossProperty;

    /**
     * 存储桶名称
     */
    protected String getBucketName() {
        String tempBucketName = this.tempBucketName();
        String _bucketName = ThreadLocalUtils.get(tempBucketName);
        if (null == _bucketName) {
            _bucketName = ossProperty.getBucketName();
        } else {
            // 释放临时桶名称
            ThreadLocalUtils.remove(tempBucketName);
        }
        return _bucketName;
    }

    /**
     * 临时桶名称
     */
    protected String tempBucketName() {
        return this.getClass().getName() + "OssBucket";
    }

    @Override
    public IFileStorage bucket(String bucketName) {
        if (StrUtil.isNotEmpty(bucketName)) {
            ThreadLocalUtils.put(this.tempBucketName(), bucketName);
        }
        return this;
    }

    /**
     * 存储对象名称，默认生成日期文件路径，按年月目录存储
     *
     * @param suffix     文件后缀
     * @param objectName 文件对象名
     * @return 文件名，包含存储路径
     */
    protected String getObjectName(String suffix, String objectName) {
        if (null != objectName) {
            return objectName;
        }
        StringBuffer ojn = new StringBuffer();
        ojn.append(DateUtil.format(new Date(), "yyyyMM")).append("/");
        ojn.append(UUID.randomUUID()).append(".").append(suffix);
        return ojn.toString();
    }

    @Override
    public IFileStorage allowMediaType(InputStream is, Function<String, Boolean> function) throws Exception {
        boolean legal = false;
        is.mark(is.available() + 1);
        String mediaType = MediaType.detect(is);
        if (null == function) {
            List<String> allowMediaType = this.ossProperty.getAllowMediaType();
            if (null != allowMediaType) {
                legal = allowMediaType.stream().anyMatch(t -> mediaType.startsWith(t));
            }
        } else {
            legal = function.apply(mediaType);
        }
        // 不合法媒体类型抛出异常
        if (!legal) {
            throw new MediaTypeException("Illegal file type");
        }
        is.reset();
        return this;
    }
}
