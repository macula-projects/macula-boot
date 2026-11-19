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

package dev.macula.boot.starter.crypto.config;

import dev.macula.boot.starter.crypto.utils.ConfigCryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.cloud.bootstrap.config.BootstrapPropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 离线配置源解密、优先级与原类型兼容测试。
 * @author Rain
 * @since 6.1.0
 */
class CryptoPostProcessorTest {
    private static final String KEY = "abcdef1234567890";
    private static final String SECRET = "test.secret";
    private static final String NACOS = "bootstrapProperties-NACOS";

    private final CryptoPostProcessor processor = new CryptoPostProcessor();

    /**
     * 构造仅使用虚构测试密钥的本地环境。
     */
    private StandardEnvironment environment() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test-key",
            Collections.<String, Object>singletonMap("macula.crypto.key", KEY)));
        return environment;
    }

    /**
     * 构造嵌套的离线远程配置源及真实 bootstrap 包装，不连接 Nacos。
     */
    private CompositePropertySource nacos(Map<String, Object> values) {
        CompositePropertySource nested = new CompositePropertySource("NACOS");
        nested.addPropertySource(new BootstrapPropertySource<>(new MapPropertySource("test.yaml,DEFAULT_GROUP", values)));
        CompositePropertySource outer = new CompositePropertySource(NACOS);
        outer.addPropertySource(nested);
        return outer;
    }

    /**
     * 验证本地和嵌套远程源均可解密，源名顺序及原始密文保持不变。
     */
    @ParameterizedTest
    @ValueSource(strings = {"map", "origin", "nested", "bootstrap"})
    void decryptsLocalAndNestedRemoteSourcesWithoutChangingCiphertext(String type) {
        StandardEnvironment environment = environment();
        Map<String, Object> values = new HashMap<>();
        String encrypted = ConfigCryptoUtil.encrypt("initial", KEY);
        values.put(SECRET, encrypted);
        values.put("test.number", 42);
        values.put("test.plain", "plain");
        PropertySource<?> source = "nested".equals(type) ? nacos(values)
            : "bootstrap".equals(type) ? new BootstrapPropertySource<>(new MapPropertySource("remote", values))
            : "origin".equals(type) ? new OriginTrackedMapPropertySource("config", values)
            : new MapPropertySource("config", values);
        environment.getPropertySources().addFirst(source);
        String namesBefore = sourceNames(environment);

        processor.postProcessEnvironment(environment, null);

        assertEquals("initial", environment.getProperty(SECRET));
        assertEquals(42, environment.getProperty("test.number", Integer.class));
        assertEquals("plain", environment.getProperty("test.plain"));
        assertEquals(encrypted, values.get(SECRET));
        assertEquals(namesBefore, sourceNames(environment));
        assertFalse(environment.getPropertySources().contains("custom-encrypt"));
    }

    /**
     * 验证高优先级明文及密文都能覆盖低优先级密文。
     */
    @Test
    void preservesPriorityOfPlainAndEncryptedOverrides() {
        StandardEnvironment environment = environment();
        environment.getPropertySources().addFirst(new MapPropertySource("low",
            Collections.<String, Object>singletonMap(SECRET, ConfigCryptoUtil.encrypt("low", KEY))));
        environment.getPropertySources().addFirst(new MapPropertySource("high",
            Collections.<String, Object>singletonMap(SECRET, "high-plain")));
        processor.postProcessEnvironment(environment, null);
        assertEquals("high-plain", environment.getProperty(SECRET));

        environment.getPropertySources().replace("high", new MapPropertySource("high",
            Collections.<String, Object>singletonMap(SECRET, ConfigCryptoUtil.encrypt("high-encrypted", KEY))));
        processor.postProcessEnvironment(environment, null);
        assertEquals("high-encrypted", environment.getProperty(SECRET));
    }

    /**
     * 验证原 Map 更新、改为明文和删除后均不残留旧明文。
     */
    @Test
    void readsLiveValuesWithoutCachingPlaintext() {
        StandardEnvironment environment = environment();
        Map<String, Object> values = new HashMap<>();
        values.put(SECRET, ConfigCryptoUtil.encrypt("first", KEY));
        environment.getPropertySources().addFirst(new MapPropertySource("live", values));
        processor.postProcessEnvironment(environment, null);
        assertEquals("first", environment.getProperty(SECRET));
        values.put(SECRET, ConfigCryptoUtil.encrypt("second", KEY));
        assertEquals("second", environment.getProperty(SECRET));
        values.put(SECRET, "now-plain");
        assertEquals("now-plain", environment.getProperty(SECRET));
        values.remove(SECRET);
        assertNull(environment.getProperty(SECRET));
    }

    /**
     * 验证重复安装幂等、具体类型和配置来源信息保持可用。
     */
    @Test
    void repeatedInstallationPreservesIdentityAndPropertyOrigins() {
        StandardEnvironment environment = environment();
        Origin origin = new Origin() { };
        environment.getPropertySources().addFirst(new OriginTrackedMapPropertySource("origin",
            Collections.<String, Object>singletonMap(SECRET,
                OriginTrackedValue.of(ConfigCryptoUtil.encrypt("origin-value", KEY), origin))));
        environment.getPropertySources().addFirst(nacos(Collections.<String, Object>singletonMap(
            "remote.value", ConfigCryptoUtil.encrypt("remote", KEY))));
        processor.postProcessEnvironment(environment, null);
        PropertySource<?> wrapped = environment.getPropertySources().get("origin");
        PropertySource<?> nested = environment.getPropertySources().get(NACOS);
        processor.postProcessEnvironment(environment, null);
        assertSame(wrapped, environment.getPropertySources().get("origin"));
        assertSame(nested, environment.getPropertySources().get(NACOS));
        assertTrue(wrapped instanceof MapPropertySource);
        assertTrue(wrapped instanceof OriginTrackedMapPropertySource);
        assertSame(origin, OriginLookup.getOrigin(wrapped, SECRET));
        assertArrayEquals(new String[]{SECRET}, ((EnumerablePropertySource<?>) wrapped).getPropertyNames());
        assertTrue(wrapped.containsProperty(SECRET));
        assertEquals("origin-value", environment.getProperty(SECRET));
        assertSame(System.getProperties(), environment.getSystemProperties());
    }

    /**
     * 验证没有启动密钥时保持已有配置读取行为。
     */
    @Test
    void missingKeyLeavesConfigurationUntouched() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        String encrypted = ConfigCryptoUtil.encrypt("unchanged", KEY);
        MapPropertySource source = new MapPropertySource("config",
            Collections.<String, Object>singletonMap(SECRET, encrypted));
        environment.getPropertySources().addFirst(source);
        processor.postProcessEnvironment(environment, null);
        assertSame(source, environment.getPropertySources().get("config"));
        assertEquals(encrypted, environment.getProperty(SECRET));
    }

    /**
     * 验证系统环境变量仍可提供密钥并保留宽松绑定。
     */
    @Test
    void systemEnvironmentRetainsRelaxedBindingAndCanProvideKey() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        Map<String, Object> variables = new HashMap<>();
        variables.put("MACULA_CRYPTO_KEY", KEY);
        variables.put("TEST_SECRET", "environment-override");
        SystemEnvironmentPropertySource source = new SystemEnvironmentPropertySource("systemEnvironment", variables);
        environment.getPropertySources().replace("systemEnvironment", source);
        environment.getPropertySources().addLast(new MapPropertySource("config",
            Collections.<String, Object>singletonMap("other.secret", ConfigCryptoUtil.encrypt("other", KEY))));
        processor.postProcessEnvironment(environment, null);
        assertSame(source, environment.getPropertySources().get("systemEnvironment"));
        assertEquals("other", environment.getProperty("other.secret"));
        assertEquals("environment-override", Binder.get(environment).bind("test", SecretProperties.class).get().getSecret());
    }

    /**
     * 验证损坏密文失败时仅报告位置，不泄漏加密输入。
     */
    @Test
    void malformedCiphertextFailsWithoutExposingContentsOrKey() {
        StandardEnvironment environment = environment();
        String invalid = "mpw:not-valid-ciphertext";
        environment.getPropertySources().addFirst(new MapPropertySource("broken",
            Collections.<String, Object>singletonMap(SECRET, invalid)));
        processor.postProcessEnvironment(environment, null);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> environment.getProperty(SECRET));
        assertTrue(error.getMessage().contains("broken"));
        assertTrue(error.getMessage().contains(SECRET));
        assertFalse(error.getMessage().contains(invalid));
        assertFalse(error.getMessage().contains(KEY));
        assertNull(error.getCause());
    }

    /**
     * 验证可检测的错误密钥失败不会泄漏密文、明文或密钥。
     */
    @Test
    void wrongKeyFailsWithoutLeakingCryptoInput() {
        StandardEnvironment environment = environment();
        String encrypted = ConfigCryptoUtil.encrypt("hello1", "1234567890abcdef");
        environment.getPropertySources().addFirst(new MapPropertySource("wrong-key",
            Collections.<String, Object>singletonMap(SECRET, encrypted)));
        processor.postProcessEnvironment(environment, null);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> environment.getProperty(SECRET));
        assertFalse(error.getMessage().contains(KEY));
        assertFalse(error.getMessage().contains(encrypted));
        assertFalse(error.getMessage().contains("hello1"));
        assertNull(error.getCause());
    }

    /**
     * 按原顺序收集源名，用于检查包装未改变优先级。
     */
    private String sourceNames(StandardEnvironment environment) {
        return StreamSupport.stream(environment.getPropertySources().spliterator(), false)
            .map(PropertySource::getName).collect(Collectors.joining(","));
    }

    /**
     * 用于验证 Binder 和真实 Cloud 重新绑定的可变配置 Bean。
     * @since 6.1.0
     */
    @ConfigurationProperties(prefix = "test")
    public static class SecretProperties {
        private String secret;

        /**
         * 返回当前已绑定的测试配置值。
         */
        public String getSecret() {
            return secret;
        }

        /**
         * 接收绑定器写入的测试配置值。
         */
        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

}
