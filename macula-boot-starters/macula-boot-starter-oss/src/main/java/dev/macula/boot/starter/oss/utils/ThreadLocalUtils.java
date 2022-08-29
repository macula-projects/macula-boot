/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.oss.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地线程请求数据处理工具类
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品
 *
 * @author 青苗
 * @since 2022-07-13
 */
public class ThreadLocalUtils {
    /**
     * 请求参数存取
     */
    private static final ThreadLocal<Map<String, Object>> REQUEST_DATA = new ThreadLocal<>();

    /**
     * 设置请求参数
     *
     * @param key   请求参数 key
     * @param value 请求参数 value
     */
    public static void put(String key, Object value) {
        Map<String, Object> dataMap = get();
        if (null == dataMap) {
            dataMap = new HashMap<>();
        }
        dataMap.put(key, value);
        REQUEST_DATA.set(dataMap);
    }

    /**
     * 设置请求参数
     *
     * @param dataMap 请求参数 MAP 对象
     */
    public static void put(Map<String, Object> dataMap) {
        REQUEST_DATA.set(dataMap);
    }

    /**
     * 获取请求参数
     *
     * @param key 请求参数 key
     * @return 请求参数 MAP 对象
     */
    public static <T> T get(String key) {
        Map<String, Object> dataMap = get();
        return null == dataMap ? null : (T) dataMap.get(key);
    }

    /**
     * 获取请求参数
     *
     * @return 请求参数 MAP 对象
     */
    public static Map<String, Object> get() {
        return REQUEST_DATA.get();
    }

    /**
     * 删除指定请求参数
     *
     * @param key 请求参数key
     */
    public static void remove(String key) {
        Map<String, Object> dataMap = get();
        if (null != dataMap) {
            dataMap.remove(key);
            REQUEST_DATA.set(dataMap);
        }
    }

    /**
     * 删除所有请求参数
     */
    public static void removeAll() {
        REQUEST_DATA.remove();
    }
}
