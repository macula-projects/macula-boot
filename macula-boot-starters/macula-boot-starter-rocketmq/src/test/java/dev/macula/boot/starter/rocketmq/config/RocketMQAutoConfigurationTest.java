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
package dev.macula.boot.starter.rocketmq.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.starter.rocketmq.DefaultRocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link RocketMQAutoConfiguration} 自动配置测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class RocketMQAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RocketMQAutoConfiguration.class));

    @Test
    void createsMessageConverterAndGrayPropertiesWithoutConnectingToBroker() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RocketMQMessageConverter.class);
            assertThat(context).hasSingleBean(GrayRocketMQProperties.class);
            assertThat(context).doesNotHaveBean(DefaultRocketMQLocalTransactionListener.class);
            assertThat(context.getBean(GrayRocketMQProperties.class).isEnabled()).isFalse();
        });
    }
}
