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

import dev.macula.boot.starter.crypto.config.CryptoPostProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置加解密与现有处理器兼容性验证。
 *
 * @author Rain
 */
class ConfigCryptoUtilTest {
    private static final String KEY = "abcdef1234567890";
    private static final String KNOWN = "mpw:Or0wNwIcctnekUyDfeN8cw==";

    static Stream<String> keys() {
        return Stream.of(KEY, "123456789012345678901234", "12345678901234567890123456789012",
            " 12345678901234 ", "中文密钥1234");
    }

    @ParameterizedTest
    @MethodSource("keys")
    void roundTripAndActualProcessor(String key) {
        for (String text : Arrays.asList("hello1", "中文配置", "", "  text \t\n")) {
            String encrypted = ConfigCryptoUtil.encrypt(text, key);
            assertTrue(encrypted.startsWith("mpw:"));
            assertEquals(text, ConfigCryptoUtil.decrypt(encrypted, key));
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(new OriginTrackedMapPropertySource("key",
                Collections.<String, Object>singletonMap("macula.crypto.key", key)));
            env.getPropertySources().addFirst(new OriginTrackedMapPropertySource("config",
                Collections.<String, Object>singletonMap("test.secret", encrypted)));
            new CryptoPostProcessor().postProcessEnvironment(env, null);
            assertEquals(text, env.getProperty("test.secret"));
        }
    }

    @Test
    void knownVector() {
        assertEquals(KNOWN, ConfigCryptoUtil.encrypt("hello1", KEY));
        assertEquals("hello1", ConfigCryptoUtil.decrypt(KNOWN, KEY));
    }

    @Test
    void invalidKeysAreRejectedWithoutExposingInput() {
        for (String key : Arrays.asList(null, "", "short", "                ",
            "\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000")) {
            assertThrows(IllegalArgumentException.class, () -> ConfigCryptoUtil.encrypt("secret", key));
            assertThrows(IllegalArgumentException.class, () -> ConfigCryptoUtil.decrypt(KNOWN, key));
        }
    }

    @Test
    void invalidContentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ConfigCryptoUtil.encrypt(null, KEY));
        for (String value : Arrays.asList(null, "", "hello", "mpw:", "mpw:???", "mpw:YQ==")) {
            assertThrows(IllegalArgumentException.class, () -> ConfigCryptoUtil.decrypt(value, KEY));
        }
    }

    @Test
    void detectedCryptoFailureDoesNotExposeInput() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> ConfigCryptoUtil.decrypt(KNOWN, "1234567890abcdef"));
        assertFalse(ex.getMessage().contains(KNOWN));
        assertFalse(ex.getMessage().contains("1234567890abcdef"));
    }
}
