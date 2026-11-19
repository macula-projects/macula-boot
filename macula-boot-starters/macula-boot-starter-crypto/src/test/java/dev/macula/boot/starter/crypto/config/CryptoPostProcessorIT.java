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
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
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
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 真实 Boot 启动、bootstrap 合并及 Cloud 事件重绑定验证，不连接外部 Nacos。
 * @author Rain
 * @since 6.1.0
 */
class CryptoPostProcessorIT {
    private static final String KEY = "abcdef1234567890";
    private static final String SECRET = "test.secret";
    private static final String NACOS = "bootstrapProperties-NACOS";

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
     * 验证晚加载源在普通 Value 注入和配置绑定之前完成包装。
     */
    @Test
    void lateBootstrapConfigurationIsDecryptedBeforeValueInjectionAndBinding() {
        try (ConfigurableApplicationContext context = application().run("--macula.crypto.key=" + KEY)) {
            assertEquals("initial", context.getEnvironment().getProperty(SECRET));
            assertEquals("initial", context.getBean(SecretProperties.class).getSecret());
            assertEquals("initial", context.getBean(InjectedSecret.class).value);
        }
    }

    /**
     * 验证真实 bootstrap 合并保留应用名称及密文属性，日志路径占位符可正确解析。
     */
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

    /**
     * 验证真实 Cloud 事件及重新绑定器读取新值，属性或源删除后按优先级回退。
     */
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

    /**
     * 模拟远程源替换后发布真实 Cloud 环境变更事件。
     */
    private void replaceAndRefresh(ConfigurableApplicationContext context, String value) {
        context.getEnvironment().getPropertySources().replace(NACOS,
            nacos(Collections.<String, Object>singletonMap(SECRET, value)));
        context.publishEvent(new EnvironmentChangeEvent(context, Collections.singleton(SECRET)));
    }

    /**
     * 创建无外部连接的完整 Boot 测试应用，在初始化器阶段模拟晚加载源。
     */
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

    /**
     * 模拟普通监听器中的配置读取，检查解密包装先于重新绑定执行。
     * @since 6.1.0
     */
    private static final class RebindingListener implements ApplicationListener<EnvironmentChangeEvent>, Ordered {
        private final ConfigurableApplicationContext context;
        private String value;

        /**
         * 保存测试上下文及策略依赖。
         */
        private RebindingListener(ConfigurableApplicationContext context) {
            this.context = context;
        }

        /**
         * 在启动后期或标准同步刷新事件中补充包装当前环境。
         */
        @Override
        public void onApplicationEvent(EnvironmentChangeEvent event) {
            value = Binder.get(context.getEnvironment()).bind("test", Bindable.of(SecretProperties.class)).get().getSecret();
        }

        /**
         * 声明框架调用顺序；启动处理靠后，刷新监听优先于配置重新绑定。
         */
        @Override
        public int getOrder() {
            return 0;
        }
    }

    /**
     * 仅供完整启动测试使用的配置，不参与其他应用的组件扫描。
     * @since 6.1.0
     */
    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecretProperties.class)
    static class TestApplication {
        /**
         * 收集被绑定的配置 Bean，供真实 Cloud 重新绑定器使用。
         */
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

    /**
     * 记录普通 Value 注入时实际读取到的配置值。
     * @since 6.1.0
     */
    private static final class InjectedSecret {
        private final String value;

        /**
         * 保存测试上下文及策略依赖。
         */
        private InjectedSecret(String value) {
            this.value = value;
        }
    }
}
