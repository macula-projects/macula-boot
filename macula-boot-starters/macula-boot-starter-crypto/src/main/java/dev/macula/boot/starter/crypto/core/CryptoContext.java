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

package dev.macula.boot.starter.crypto.core;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.starter.crypto.annotation.CryptoField;
import dev.macula.boot.starter.crypto.config.CryptoProperties;
import dev.macula.boot.starter.crypto.enums.AlgorithmType;
import dev.macula.boot.starter.crypto.enums.EncodeType;
import lombok.Data;

/**
 * 加密上下文 用于encryptor传递必要的参数。
 *
 * @author 老马
 * @version 4.6.0
 */
@Data
public class CryptoContext {

    /**
     * 默认算法
     */
    private AlgorithmType algorithm;

    /**
     * 安全秘钥
     */
    private String password;

    /**
     * 公钥
     */
    private String publicKey;

    /**
     * 私钥
     */
    private String privateKey;

    /**
     * 编码方式，base64/hex
     */
    private EncodeType encode;

    /**
     * 构造函数
     *
     * @param cryptoField       加密字段注解
     * @param defaultProperties 默认加密配置属性
     */
    public CryptoContext(CryptoField cryptoField, CryptoProperties defaultProperties) {
        this.setAlgorithm(cryptoField.algorithm() == AlgorithmType.DEFAULT ? defaultProperties.getAlgorithm()
            : cryptoField.algorithm());
        this.setEncode(
            cryptoField.encode() == EncodeType.DEFAULT ? defaultProperties.getEncode() : cryptoField.encode());
        this.setPassword(
            StrUtil.isBlank(cryptoField.password()) ? defaultProperties.getPassword() : cryptoField.password());
        this.setPrivateKey(
            StrUtil.isBlank(cryptoField.privateKey()) ? defaultProperties.getPrivateKey() : cryptoField.privateKey());
        this.setPublicKey(
            StrUtil.isBlank(cryptoField.publicKey()) ? defaultProperties.getPublicKey() : cryptoField.publicKey());
    }

    /**
     * 构造函数
     *
     * @param p 加密配置属性
     */
    public CryptoContext(CryptoProperties p) {
        this.setPassword(p.getPassword());
        this.setAlgorithm(p.getAlgorithm());
        this.setPublicKey(p.getPublicKey());
        this.setPrivateKey(p.getPrivateKey());
        this.setEncode(p.getEncode());
    }
}
