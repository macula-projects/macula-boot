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

package dev.macula.boot.starter.crypto.core.encryptor;

import cn.hutool.core.codec.Base64;
import dev.macula.boot.starter.crypto.core.CryptoContext;
import dev.macula.boot.starter.crypto.enums.AlgorithmType;
import dev.macula.boot.starter.crypto.enums.EncodeType;

/**
 * Base64算法实现
 *
 * @author 老马
 * @version 4.6.0
 */
public class Base64Encryptor extends AbstractEncryptor {

    /**
     * 构造函数
     *
     * @param context 加密上下文
     */
    public Base64Encryptor(CryptoContext context) {
        super(context);
    }

    /**
     * 获得当前算法
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.BASE64;
    }

    /**
     * 加密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     */
    @Override
    public String encrypt(String value, EncodeType encodeType) {
        return Base64.encode(value);
    }

    /**
     * 解密
     *
     * @param value 待加密字符串
     */
    @Override
    public String decrypt(String value) {
        return Base64.decodeStr(value);
    }
}
