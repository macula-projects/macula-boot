/*
 * Copyright (c) 2026 Macula
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
package dev.macula.boot.starter.crypto.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 与 CryptoPostProcessor 的 mpw 配置协议兼容的加解密工具。
 *
 * @author Rain
 */
public final class ConfigCryptoUtil {
    private static final String PREFIX = "mpw:";

    private ConfigCryptoUtil() {
    }

    /**
     * 使用 UTF-8 密钥加密，返回带 mpw: 前缀的 Base64 密文。
     *
     * @param plaintext 明文，可为空字符串
     * @param key 16、24 或 32 个 UTF-8 字节的非空白密钥
     * @return 完整配置密文
     */
    public static String encrypt(String plaintext, String key) {
        byte[] keyBytes = keyBytes(key);
        if (plaintext == null) {
            throw new IllegalArgumentException("明文不能为 null");
        }
        try {
            return PREFIX + SecureUtil.aes(keyBytes).encryptBase64(plaintext, StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("加密失败");
        }
    }

    /**
     * 解密完整的 mpw: 配置值，保留明文中的空白。
     *
     * @param ciphertext 带 mpw: 前缀的 Base64 密文
     * @param key 与加密时相同的密钥
     * @return UTF-8 明文
     */
    public static String decrypt(String ciphertext, String key) {
        byte[] keyBytes = keyBytes(key);
        if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
            throw new IllegalArgumentException("密文必须以 mpw: 开头");
        }
        byte[] encrypted;
        try {
            encrypted = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("密文必须为有效的 Base64");
        }
        if (encrypted.length == 0 || encrypted.length % 16 != 0) {
            throw new IllegalArgumentException("密文长度无效");
        }
        try {
            return new String(SecureUtil.aes(keyBytes).decrypt(encrypted), StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("解密失败，请检查密钥和密文");
        }
    }

    private static byte[] keyBytes(String key) {
        if (StrUtil.isBlank(key)) {
            throw new IllegalArgumentException("密钥不能为空或全为空白");
        }
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) {
            throw new IllegalArgumentException("密钥的 UTF-8 字节长度必须为 16、24 或 32");
        }
        return bytes;
    }
}
