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

package dev.macula.boot.starter.crypto.secret;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 基于JCA AES-GCM的用途绑定秘密信封；不注册Bean，不影响既有字段加密配置。
 * 主密钥必须由部署秘密注入，信封中的keyId仅用于选择密钥，不代表已认证。
 *
 * @author Rain
 * @since 6.1.0
 */
public final class AuthenticatedSecretCipher {
    private static final String VERSION = "v1";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String REJECTED = "SECRET_ENVELOPE_REJECTED";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int MAXIMUM_BYTES = 32768;
    private static final int MAXIMUM_ENVELOPE = 45000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final Map<String, SecretKeySpec> keys;
    private final String activeKeyId;

    /**
     * 复制独立的256位密钥环，调用者后续修改原数组不能影响已装配实例。
     *
     * @param suppliedKeys keyId到32字节密钥的映射
     * @param activeKeyId 新信封使用的密钥，旧密钥只用于解密
     * @throws IllegalArgumentException 密钥或标识不符合约束
     */
    public AuthenticatedSecretCipher(Map<String, byte[]> suppliedKeys, String activeKeyId) {
        if (suppliedKeys == null || suppliedKeys.isEmpty() || suppliedKeys.size() > 16
                || !validId(activeKeyId) || !suppliedKeys.containsKey(activeKeyId)) {
            throw new IllegalArgumentException(REJECTED);
        }
        Map<String, SecretKeySpec> copy = new HashMap<>();
        suppliedKeys.forEach((id, key) -> {
            if (!validId(id) || key == null || key.length != 32) {
                throw new IllegalArgumentException(REJECTED);
            }
            copy.put(id, new SecretKeySpec(key, "AES"));
        });
        this.keys = Map.copyOf(copy);
        this.activeKeyId = activeKeyId;
    }

    /**
     * 每次生成新nonce，将信封版本、keyId和用途作为AAD；不得将输出或输入写入日志。
     *
     * @param context 服务器确定的kind/provider/configId/version等用途标识
     * @param plaintext 待保护秘密，UTF-8最多32768字节
     * @return 可持久化的版本化认证密文
     * @throws IllegalStateException 输入无效或JCA不可用
     */
    public String encrypt(String context, String plaintext) {
        requireContext(context);
        if (plaintext == null || plaintext.length() > MAXIMUM_BYTES) {
            throw rejected();
        }
        byte[] value = plaintext.getBytes(StandardCharsets.UTF_8);
        if (value.length > MAXIMUM_BYTES) {
            throw rejected();
        }
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, activeKeyId, nonce, context);
            return VERSION + "." + activeKeyId + "." + ENCODER.encodeToString(nonce) + "."
                    + ENCODER.encodeToString(cipher.doFinal(value));
        } catch (GeneralSecurityException ex) {
            throw rejected();
        }
    }

    /**
     * 完整认证通过后才返回明文；错误用途、未知keyId、格式损坏及篡改统一拒绝。
     *
     * @param context 读取方重新计算的服务器用途，不能直接采用外部请求值
     * @param envelope 加密信封
     * @return 原始UTF-8秘密
     * @throws IllegalStateException 信封不能通过认证，不附带密文或底层异常内容
     */
    public String decrypt(String context, String envelope) {
        requireContext(context);
        if (envelope == null || envelope.length() > MAXIMUM_ENVELOPE) {
            throw rejected();
        }
        String[] parts = envelope.split("\\.", -1);
        if (parts.length != 4 || !VERSION.equals(parts[0]) || !keys.containsKey(parts[1])) {
            throw rejected();
        }
        try {
            byte[] nonce = DECODER.decode(parts[2]);
            byte[] value = DECODER.decode(parts[3]);
            if (nonce.length != NONCE_BYTES || value.length < TAG_BITS / 8
                    || value.length > MAXIMUM_BYTES + TAG_BITS / 8) {
                throw rejected();
            }
            byte[] plaintext = cipher(Cipher.DECRYPT_MODE, parts[1], nonce, context).doFinal(value);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw rejected();
        }
    }

    /**
     * 用JCA认证加密，以NUL分隔版本、keyId和用途，并禁止用途包含NUL以避免拼接歧义。
     *
     * @param mode JCA加解密模式
     * @param keyId 已知密钥标识
     * @param nonce 唯一随机nonce
     * @param context 服务器用途
     * @return 已初始化的认证加密实例
     * @throws GeneralSecurityException JCA初始化失败
     */
    private Cipher cipher(int mode, String keyId, byte[] nonce, String context) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, keys.get(keyId), new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD((VERSION + "\0" + keyId + "\0" + context).getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    /**
     * 限制用途输入，防止不明确的分隔及无界分配。
     *
     * @param context 服务器用途
     */
    private static void requireContext(String context) {
        if (context == null || context.isBlank() || context.length() > 1024 || context.indexOf('\0') >= 0) {
            throw rejected();
        }
    }

    /**
     * keyId可公开但不能携带任意分隔符或秘密正文。
     *
     * @param value 标识
     * @return 是否符合公开标识约束
     */
    private static boolean validId(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{1,64}");
    }

    /**
     * 统一错误避免调用方记录秘密或密码学内部数据。
     *
     * @return 固定拒绝异常
     */
    private static IllegalStateException rejected() {
        return new IllegalStateException(REJECTED);
    }

    /**
     * 诊断信息永不包含密钥环。
     *
     * @return 固定脱敏说明
     */
    @Override
    public String toString() {
        return "AuthenticatedSecretCipher[keys=PROTECTED]";
    }
}
