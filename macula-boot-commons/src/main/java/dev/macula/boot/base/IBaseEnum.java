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

package dev.macula.boot.base;

import cn.hutool.core.util.ObjectUtil;

import java.util.EnumSet;
import java.util.Objects;

/**
 * 枚举通用接口
 *
 * @author haoxr
 * @since 2022/3/27 12:06
 */
public interface IBaseEnum<T> {

    /**
     * 根据值获取枚举
     *
     * @param value 枚举值
     * @param clazz 枚举类型
     * @param <E>   枚举
     * @return 枚举
     */
    static <E extends Enum<E> & IBaseEnum> E getEnumByValue(Object value, Class<E> clazz) {
        Objects.requireNonNull(value);
        EnumSet<E> allEnums = EnumSet.allOf(clazz); // 获取类型下的所有枚举
        E matchEnum = allEnums.stream().filter(e -> ObjectUtil.equal(e.getValue(), value)).findFirst().orElse(null);
        return matchEnum;
    }

    /**
     * 根据文本标签获取值
     *
     * @param value 枚举值
     * @param clazz 枚举类型
     * @param <E>   枚举
     * @return 枚举
     */
    static <E extends Enum<E> & IBaseEnum> String getLabelByValue(Object value, Class<E> clazz) {
        Objects.requireNonNull(value);
        EnumSet<E> allEnums = EnumSet.allOf(clazz); // 获取类型下的所有枚举
        E matchEnum = allEnums.stream().filter(e -> ObjectUtil.equal(e.getValue(), value)).findFirst().orElse(null);

        String label = null;
        if (matchEnum != null) {
            label = matchEnum.getLabel();
        }
        return label;
    }

    /**
     * 根据文本标签获取值
     *
     * @param label 标签
     * @param clazz 类型
     * @param <E>   枚举
     * @return 枚举
     */
    static <E extends Enum<E> & IBaseEnum> Object getValueByLabel(String label, Class<E> clazz) {
        Objects.requireNonNull(label);
        EnumSet<E> allEnums = EnumSet.allOf(clazz); // 获取类型下的所有枚举
        String finalLabel = label;
        E matchEnum =
            allEnums.stream().filter(e -> ObjectUtil.equal(e.getLabel(), finalLabel)).findFirst().orElse(null);

        Object value = null;
        if (matchEnum != null) {
            value = matchEnum.getValue();
        }
        return value;
    }

    T getValue();

    String getLabel();

}
