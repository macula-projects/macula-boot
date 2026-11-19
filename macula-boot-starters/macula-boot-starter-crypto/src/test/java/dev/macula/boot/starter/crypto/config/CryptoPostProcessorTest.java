package dev.macula.boot.starter.crypto.config;

import dev.macula.boot.starter.crypto.utils.ConfigCryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.bootstrap.config.BootstrapPropertySource;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

/**
 * 使用离线配置源模拟 Nacos bootstrap 加载及刷新，密钥和内容均为虚构测试数据。
 */
class CryptoPostProcessorTest {
    private static final String KEY = "abcdef1234567890";
    private static final String SECRET = "test.secret";
    private static final String NACOS = "bootstrapProperties-NACOS";

    private final CryptoPostProcessor processor = new CryptoPostProcessor();

    private StandardEnvironment environment() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test-key",
            Collections.<String, Object>singletonMap("macula.crypto.key", KEY)));
        return environment;
    }

    private CompositePropertySource nacos(Map<String, Object> values) {
        CompositePropertySource nested = new CompositePropertySource("NACOS");
        nested.addPropertySource(new BootstrapPropertySource<>(new MapPropertySource("test.yaml,DEFAULT_GROUP", values)));
        CompositePropertySource outer = new CompositePropertySource(NACOS);
        outer.addPropertySource(nested);
        return outer;
    }

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

    @Test
    void lateBootstrapConfigurationIsDecryptedBeforeValueInjectionAndBinding() {
        try (ConfigurableApplicationContext context = application().run("--macula.crypto.key=" + KEY)) {
            assertEquals("initial", context.getEnvironment().getProperty(SECRET));
            assertEquals("initial", context.getBean(SecretProperties.class).getSecret());
            assertEquals("initial", context.getBean(InjectedSecret.class).value);
        }
    }

    @Test
    void realBootstrapPropertiesSurviveMergingBeforeLoggingInitialization(@TempDir Path directory) throws Exception {
        Path bootstrap = directory.resolve("bootstrap.properties");
        Path applicationProperties = directory.resolve("application.properties");
        Files.write(bootstrap, ("spring.application.name=crypto-bootstrap-test\nbootstrap.secret="
            + ConfigCryptoUtil.encrypt("bootstrap-value", KEY) + "\n").getBytes(StandardCharsets.UTF_8));
        String logPattern = directory.resolve("${spring.application.name}")
            .resolve("${spring.application.name}.log").toString();
        Files.write(applicationProperties, ("logging.file.name=" + logPattern + "\n")
            .getBytes(StandardCharsets.UTF_8));

        SpringApplication application = application();
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("spring.cloud.bootstrap.enabled", "true");
        defaults.put("spring.cloud.bootstrap.location", bootstrap.toUri().toString());
        defaults.put("spring.config.location", applicationProperties.toUri().toString());
        application.setDefaultProperties(defaults);
        try (ConfigurableApplicationContext context = application.run("--macula.crypto.key=" + KEY)) {
            assertEquals("crypto-bootstrap-test", context.getEnvironment().getProperty("spring.application.name"));
            assertEquals("bootstrap-value", context.getEnvironment().getProperty("bootstrap.secret"));
            assertEquals(directory.resolve("crypto-bootstrap-test/crypto-bootstrap-test.log").toString(),
                context.getEnvironment().getProperty("logging.file.name"));
        }
    }

    @Test
    void realCloudEventsRewrapReplacedSourcesBeforeRebindingAndDoNotRetainRemovedValues() {
        try (ConfigurableApplicationContext context = application().run("--macula.crypto.key=" + KEY)) {
            RebindingListener rebinding = new RebindingListener(context);
            context.addApplicationListener(rebinding);
            SecretProperties bean = context.getBean(SecretProperties.class);
            replaceAndRefresh(context, ConfigCryptoUtil.encrypt("updated", KEY));
            assertEquals("updated", rebinding.value);
            assertEquals("updated", bean.getSecret());
            replaceAndRefresh(context, "plain-update");
            assertEquals("plain-update", rebinding.value);
            assertEquals("plain-update", bean.getSecret());
            context.getEnvironment().getPropertySources().replace(NACOS, nacos(Collections.emptyMap()));
            context.publishEvent(new EnvironmentChangeEvent(context, Collections.singleton(SECRET)));
            assertEquals("fallback", bean.getSecret());
            context.getEnvironment().getPropertySources().remove(NACOS);
            context.publishEvent(new EnvironmentChangeEvent(context, Collections.singleton(SECRET)));
            assertEquals("fallback", rebinding.value);
            assertEquals("fallback", bean.getSecret());
            assertEquals("fallback", context.getEnvironment().getProperty(SECRET));
            assertTrue(context.getBean(ConfigurationPropertiesRebinder.class).getErrors().isEmpty());
        }
    }

    private void replaceAndRefresh(ConfigurableApplicationContext context, String value) {
        context.getEnvironment().getPropertySources().replace(NACOS,
            nacos(Collections.<String, Object>singletonMap(SECRET, value)));
        context.publishEvent(new EnvironmentChangeEvent(context, Collections.singleton(SECRET)));
    }

    private SpringApplication application() {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        application.setDefaultProperties(Collections.singletonMap("spring.cloud.bootstrap.enabled", "false"));
        application.addInitializers(context -> {
            context.getEnvironment().getPropertySources().addFirst(nacos(
                Collections.<String, Object>singletonMap(SECRET, ConfigCryptoUtil.encrypt("initial", KEY))));
            context.getEnvironment().getPropertySources().addLast(new MapPropertySource("fallback",
                Collections.<String, Object>singletonMap(SECRET, "fallback")));
        });
        return application;
    }

    private String sourceNames(StandardEnvironment environment) {
        return StreamSupport.stream(environment.getPropertySources().spliterator(), false)
            .map(PropertySource::getName).collect(Collectors.joining(","));
    }

    private static final class RebindingListener implements ApplicationListener<EnvironmentChangeEvent>, Ordered {
        private final ConfigurableApplicationContext context;
        private String value;

        private RebindingListener(ConfigurableApplicationContext context) {
            this.context = context;
        }

        @Override
        public void onApplicationEvent(EnvironmentChangeEvent event) {
            value = Binder.get(context.getEnvironment()).bind("test", Bindable.of(SecretProperties.class)).get().getSecret();
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecretProperties.class)
    static class TestApplication {
        @Bean
        static ConfigurationPropertiesBeans configurationPropertiesBeans() {
            return new ConfigurationPropertiesBeans();
        }

        @Bean
        ConfigurationPropertiesRebinder configurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
            return new ConfigurationPropertiesRebinder(beans);
        }

        @Bean
        InjectedSecret injectedSecret(@Value("${test.secret}") String secret) {
            return new InjectedSecret(secret);
        }
    }

    @ConfigurationProperties(prefix = "test")
    public static class SecretProperties {
        private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    private static final class InjectedSecret {
        private final String value;

        private InjectedSecret(String value) {
            this.value = value;
        }
    }
}
