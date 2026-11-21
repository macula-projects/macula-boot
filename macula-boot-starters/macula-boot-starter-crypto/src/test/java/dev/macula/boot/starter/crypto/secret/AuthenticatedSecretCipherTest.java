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

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证认证信封的用途隔离、随机nonce、密钥轮换读取和错误脱敏。
 *
 * @author Rain
 * @since 6.1.0
 */
class AuthenticatedSecretCipherTest {
    private static final String CONTEXT = "HUMAN/TENCENT/config-id/2";

    /**
     * 相同秘密每次加密不同，但均只在原用途可解密。
     */
    @Test
    void roundTripUsesIndependentNoncesAndAuthenticatesContext() {
        var cipher = new AuthenticatedSecretCipher(Map.of("current", new byte[32]), "current");
        String first = cipher.encrypt(CONTEXT, "合成测试秘密");
        String second = cipher.encrypt(CONTEXT, "合成测试秘密");
        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(CONTEXT, first)).isEqualTo("合成测试秘密");
        assertThatThrownBy(() -> cipher.decrypt("GEOLOCATION/IPDATA/config-id/2", first))
                .isInstanceOf(IllegalStateException.class).hasMessage("SECRET_ENVELOPE_REJECTED");
        assertThat(cipher.toString()).doesNotContain("合成测试秘密", first);
    }

    /**
     * nonce、密文、版本和keyId任意篡改都失败，不能把未知密钥当默认密钥。
     */
    @Test
    void rejectsTamperingAndUnknownKeys() {
        var cipher = new AuthenticatedSecretCipher(Map.of("current", new byte[32]), "current");
        String value = cipher.encrypt(CONTEXT, "synthetic-secret");
        String[] parts = value.split("\\.");
        for (int index = 0; index < parts.length; index++) {
            String[] changed = parts.clone();
            changed[index] = index < 2 ? "unknown" : "A" + changed[index].substring(1);
            if (changed[index].equals(parts[index])) {
                changed[index] = "B" + changed[index].substring(1);
            }
            String corrupted = String.join(".", changed);
            assertThatThrownBy(() -> cipher.decrypt(CONTEXT, corrupted))
                    .isInstanceOf(IllegalStateException.class).hasMessage("SECRET_ENVELOPE_REJECTED");
        }
    }

    /**
     * 密钥材料复制后不能被调用者意外更改；读取旧信封需要显式保留旧keyId。
     */
    @Test
    void copiesKeysAndSupportsExplicitReadKeyring() {
        byte[] old = new byte[32];
        var previous = new AuthenticatedSecretCipher(Map.of("old", old), "old");
        String envelope = previous.encrypt(CONTEXT, "fixture");
        Arrays.fill(old, (byte) 17);
        assertThat(previous.decrypt(CONTEXT, envelope)).isEqualTo("fixture");
        var rotated = new AuthenticatedSecretCipher(Map.of("old", new byte[32], "new", old), "new");
        assertThat(rotated.decrypt(CONTEXT, envelope)).isEqualTo("fixture");
        assertThat(rotated.encrypt(CONTEXT, "fixture")).startsWith("v1.new.");
    }

    /**
     * 配置错误和无界输入在进入密码学操作前即拒绝。
     */
    @Test
    void rejectsInvalidConfigurationAndOversizedValues() {
        assertThatThrownBy(() -> new AuthenticatedSecretCipher(Map.of(), "missing"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthenticatedSecretCipher(Map.of("key", new byte[16]), "key"))
                .isInstanceOf(IllegalArgumentException.class);
        var cipher = new AuthenticatedSecretCipher(Map.of("key", new byte[32]), "key");
        assertThatThrownBy(() -> cipher.encrypt("", "value")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.encrypt(CONTEXT, "汉".repeat(12000)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decrypt(CONTEXT, "x".repeat(45001)))
                .isInstanceOf(IllegalStateException.class);
    }
}
