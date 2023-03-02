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

package dev.macula.boot.starter.mp.utils;

import dev.macula.boot.starter.mp.annotation.CryptoField;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加密字段处理工具类
 *
 * @author guhong
 * @since 2021/6/12
 */
public class CryptoFieldUtils {

    private static final ConcurrentHashMap<String, Set<Field>> CRYPT_FIELDS = new ConcurrentHashMap<>();

    public static Map<String, String> getCryptoMap(String prefix, Object params) throws IllegalAccessException {
        if (params == null || params instanceof Collections) {
            return Collections.emptyMap();
        }
        if (prefix == null) {
            prefix = "";
        }
        if (prefix != "") {
            prefix = prefix + ".";
        }
        Map<String, String> cryptFieldMap = new HashMap<>();
        if (params instanceof Map) {
            Map<String, Object> paramsMap = (Map<String, Object>)params;
            for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
                cryptFieldMap.putAll(getCryptoMap(prefix + entry.getKey(), entry.getValue()));
            }
            return cryptFieldMap;
        }
        Set<Field> cryptFields = getCryptoFields(params);
        for (Field field : cryptFields) {
            Object data = field.get(params);
            if (data != null && !"".equals(data)) {
                cryptFieldMap.put(prefix + field.getName(), data.toString());
            }
        }
        return cryptFieldMap;
    }

    public static Set<Field> getCryptoFields(Object params) {
        if (params == null || params instanceof Map || params instanceof Collection) {
            return Collections.emptySet();
        }
        String className = params.getClass().getName();
        if (!CRYPT_FIELDS.contains(className)) {
            Field fields[] = params.getClass().getDeclaredFields();
            for (Field field : fields) {
                CryptoField cryptFieldAnnotation = field.getAnnotation(CryptoField.class);
                if (cryptFieldAnnotation != null) {
                    Set<Field> strings = CRYPT_FIELDS.computeIfAbsent(className, key -> new HashSet<>());
                    field.setAccessible(true);
                    strings.add(field);
                }
            }
            CRYPT_FIELDS.putIfAbsent(className, Collections.emptySet());
        }
        return CRYPT_FIELDS.get(className);
    }
}
