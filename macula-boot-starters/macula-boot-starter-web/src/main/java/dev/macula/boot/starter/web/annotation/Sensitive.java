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

package dev.macula.boot.starter.web.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dev.macula.boot.starter.web.json.SensitiveSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Sensitvie} 脱敏的JSON注解
 *
 * @author rain
 * @since 2022/7/28 23:48
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {
    /**
     * 脱敏的类型,默认手机号
     */
    Type value();

    /**
     * CUSTOM_HIDE/CUSTOM_OVERLAY 时生效
     *
     * @return 开始位置（包含）
     */
    int startInclude() default 0;

    /**
     * CUSTOM_HIDE/CUSTOM_OVERLAY 时生效
     *
     * @return 结束位置（不包含）
     */
    int endExclude() default 0;


    /**
     * CUSTOM_OVERLAY 时生效
     *
     * @return *重复的次数
     */
    int overlayRepeat() default 4;


    /**
     * Enumeration used with {@link Sensitive}
     */
    public enum Type {
        /**
         * 手机号
         */
        MOBILE,
        /**
         * 中文名
         */
        CHINESE_NAME,
        /**
         * 身份证号
         */
        ID_CARD,
        /**
         * 座机号
         */
        FIXED_PHONE,
        /**
         * 地址
         */
        ADDRESS,
        /**
         * 电子邮件
         */
        EMAIL,
        /**
         * 银行卡
         */
        BANK_CARD,
        /**
         * 自定义，有多少个字符替换成多少个*
         * e.g: startInclude=3,endExclude=7,隐藏第3个到第7个的字符
         */
        CUSTOM_HIDE,
        /**
         * 保留方式隐藏
         * e.g: startInclude=3,endExclude=4 ,保留前面3个和后面的4个
         */
        CUSTOM_RETAIN_HIDE,
        /**
         * 自定义,只替换成指定个*
         */
        CUSTOM_OVERLAY,
    }
}
