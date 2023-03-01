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

package dev.macula.boot.starter.mp.test.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * <p>
 * <b>SexEnum</b> 性别枚举
 * </p>
 *
 * @author Rain
 * @since 2022-01-24
 */
public enum SexEnum {
    MALE("M", "男"), FEMALE("F", "女");

    @EnumValue
    private final String code;
    private String desc;

    SexEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "SexEnum{" + "code='" + code + '\'' + ", desc='" + desc + '\'' + '}';
    }
}
