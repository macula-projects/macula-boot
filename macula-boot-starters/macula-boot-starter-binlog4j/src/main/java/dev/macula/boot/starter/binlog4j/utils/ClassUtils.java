/*
 * Copyright (c) 2023-2026 Macula
 * macula.dev, China
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

package dev.macula.boot.starter.binlog4j.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 解析接口泛型参数类型的反射工具类。
 *
 * @author rain
 * @since 5.0.0
 */
public class ClassUtils {

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getGenericType(Class<?> cls) {
        Type type = cls.getGenericInterfaces()[0];
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType)type;
            Type[] argTypes = paramType.getActualTypeArguments();
            if (argTypes.length > 0) {
                return (Class<T>)argTypes[0];
            }
        }
        return null;
    }
}
