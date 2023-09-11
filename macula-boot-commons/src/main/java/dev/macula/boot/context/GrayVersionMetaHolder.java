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

package dev.macula.boot.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * {@code VersionMetaHolder} 注册中心Meta定义的gray_version信息
 *
 * @author rain
 * @since 2023/9/11 12:05
 */
public class GrayVersionMetaHolder {
    private final static ThreadLocal<String> THREAD_LOCAL_GRAY_VERSION = new TransmittableThreadLocal<>();

    /**
     * 获取TTL中的灰度版本
     *
     * @return 灰度版本号
     */
    public static String getGrayVersion() {
        return THREAD_LOCAL_GRAY_VERSION.get();
    }

    /**
     * TTL 设置灰度版本
     *
     * @param version 灰度版本号
     */
    public static void setGrayVersion(String version) {
        THREAD_LOCAL_GRAY_VERSION.set(version);
    }

    /**
     * 清除灰度版本号
     */
    public static void clear() {
        THREAD_LOCAL_GRAY_VERSION.remove();
    }
}
