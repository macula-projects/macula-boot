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
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import dev.macula.boot.starter.crypto.core.CryptoContext;
import dev.macula.boot.starter.crypto.enums.AlgorithmType;
import dev.macula.boot.starter.crypto.enums.EncodeType;

/**
 * RSA算法实现
 *
 * @author 老马
 * @version 4.6.0
 */
public class RsaEncryptor extends AbstractEncryptor {

    private final RSA rsa;

    /**
     * 构造函数
     *
     * @param context 加密上下文
     */
    public RsaEncryptor(CryptoContext context) {
        super(context);
        String privateKey = context.getPrivateKey();
        String publicKey = context.getPublicKey();
        if (!StrUtil.isAllNotEmpty(privateKey, publicKey)) {
            throw new IllegalArgumentException("RSA公私钥均需要提供，公钥加密，私钥解密。");
        }
        this.rsa = SecureUtil.rsa(Base64.decode(privateKey), Base64.decode(publicKey));
    }

    /**
     * 获得当前算法
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.RSA;
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
            return rsa.encryptHex(value, KeyType.PublicKey);
        } else {
            return rsa.encryptBase64(value, KeyType.PublicKey);
        }
    }

    /**
     * 解密
     *
     * @param value 待加密字符串
     */
    @Override
    public String decrypt(String value) {
        return this.rsa.decryptStr(value, KeyType.PrivateKey);
    }
}
