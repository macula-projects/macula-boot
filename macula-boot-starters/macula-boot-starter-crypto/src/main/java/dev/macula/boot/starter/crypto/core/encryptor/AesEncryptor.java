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

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import dev.macula.boot.starter.crypto.core.CryptoContext;
import dev.macula.boot.starter.crypto.enums.AlgorithmType;
import dev.macula.boot.starter.crypto.enums.EncodeType;

import java.nio.charset.StandardCharsets;

/**
 * AES算法实现
 *
 * @author 老马
 * @version 4.6.0
 */
public class AesEncryptor extends AbstractEncryptor {

    private final AES aes;

    /**
     * 构造函数
     *
     * @param context 加密上下文
     */
    public AesEncryptor(CryptoContext context) {
        super(context);
        String password = context.getPassword();
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("AES没有获得秘钥信息");
        }
        // aes算法的秘钥要求是16位、24位、32位
        int[] array = {16, 24, 32};
        if (!ArrayUtil.contains(array, password.length())) {
            throw new IllegalArgumentException("AES秘钥长度应该为16位、24位、32位，实际为" + password.length() + "位");
        }
        aes = SecureUtil.aes(context.getPassword().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获得当前算法
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.AES;
    }

    /**
     * 加密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     */
    @Override
    public String encrypt(String value, EncodeType encodeType) {
        if (encodeType == EncodeType.HEX) {
            return aes.encryptHex(value);
        } else {
            return aes.encryptBase64(value);
        }
    }

    /**
     * 解密
     *
     * @param value 待加密字符串
     */
    @Override
    public String decrypt(String value) {
        return this.aes.decryptStr(value);
    }
}
