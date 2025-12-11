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

package dev.macula.boot.enums;

import dev.macula.boot.base.IBaseEnum;
import lombok.Getter;

/**
 * 状态枚举
 *
 * @author haoxr
 * @since 2022/10/14
 */
public enum StatusEnum implements IBaseEnum<Integer> {

    /**
     * 启用
     */
    ENABLE(1, "启用"),
    
    /**
     * 禁用
     */
    DISABLE(0, "禁用");

    @Getter
    private final Integer value;

    @Getter
    private final String label;

    /**
     * 构造函数
     *
     * @param value 状态值
     * @param label 状态标签
     */
    StatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
