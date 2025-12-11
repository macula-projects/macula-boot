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

package dev.macula.boot.starter.crypto.annotation;

import dev.macula.boot.starter.crypto.enums.AlgorithmType;
import dev.macula.boot.starter.crypto.enums.EncodeType;

import java.lang.annotation.*;

/**
 * {@code CryptoField} 秘密字段注解
 *
 * @author rain
 * @since 2022/8/22 11:44
 */
@Documented
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CryptoField {

    /**
     * 加密算法
     *
     * @return 加密算法类型
     */
    AlgorithmType algorithm() default AlgorithmType.DEFAULT;

    /**
     * 秘钥。AES、SM4需要
     *
     * @return 秘钥字符串
     */
    String password() default "";

    /**
     * 公钥。RSA、SM2需要
     *
     * @return 公钥字符串
     */
    String publicKey() default "";

    /**
     * 私钥。RSA、SM2需要
     *
     * @return 私钥字符串
     */
    String privateKey() default "";

    /**
     * 编码方式。对加密算法为BASE64的不起作用
     *
     * @return 编码类型
     */
    EncodeType encode() default EncodeType.DEFAULT;

}
