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
package dev.macula.boot.starter.binlog4j.test;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.starter.binlog4j.BinlogClientConfig;
import dev.macula.boot.starter.binlog4j.config.Binlog4jAutoConfiguration;
import dev.macula.boot.starter.binlog4j.config.Binlog4jAutoProperties;
import dev.macula.boot.starter.binlog4j.config.Binlog4jInitializationBeanProcessor;
import dev.macula.boot.starter.binlog4j.enums.BinlogClientMode;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

/**
 * {@code Binlog4jAutoConfigurationTest} Binlog4j自动配置单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class Binlog4jAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(Binlog4jAutoConfiguration.class));

    @Test
    void createsAutoConfigurationInfrastructureWithoutConfiguredClients() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Binlog4jAutoProperties.class);
            assertThat(context).hasSingleBean(Binlog4jAutoConfiguration.class);
            assertThat(context).hasSingleBean(Binlog4jInitializationBeanProcessor.class);
        });
    }

    @Test
    void bindsMultipleClientConfigurationsWithoutOpeningDatabaseConnections() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("binlog4j.client-configs.main-client.host", "127.0.0.1")
            .withProperty("binlog4j.client-configs.main-client.port", "3306")
            .withProperty("binlog4j.client-configs.main-client.username", "root")
            .withProperty("binlog4j.client-configs.main-client.password", "password")
            .withProperty("binlog4j.client-configs.main-client.server-id", "1")
            .withProperty("binlog4j.client-configs.main-client.mode", "standalone")
            .withProperty("binlog4j.client-configs.second-client.host", "127.0.0.1")
            .withProperty("binlog4j.client-configs.second-client.port", "3307")
            .withProperty("binlog4j.client-configs.second-client.username", "root");

        Binlog4jAutoProperties properties = Binder.get(environment)
            .bind("binlog4j", Bindable.of(Binlog4jAutoProperties.class))
            .orElseThrow(() -> new AssertionError("binlog4j properties were not bound"));
        Map<String, BinlogClientConfig> clients = properties.getClientConfigs();

        assertThat(clients).hasSize(2);
        assertThat(clients.get("main-client"))
            .extracting(BinlogClientConfig::getHost, BinlogClientConfig::getPort, BinlogClientConfig::getUsername, BinlogClientConfig::getPassword, BinlogClientConfig::getServerId, BinlogClientConfig::getMode)
            .containsExactly("127.0.0.1", 3306, "root", "password", 1L, BinlogClientMode.standalone);
        assertThat(clients.get("second-client"))
            .extracting(BinlogClientConfig::getHost, BinlogClientConfig::getPort, BinlogClientConfig::getUsername)
            .containsExactly("127.0.0.1", 3307, "root");
    }

    @Test
    void clientConfigurationCanBeCreatedWithDefaults() {
        assertThat(new BinlogClientConfig()).isNotNull();
    }
}
