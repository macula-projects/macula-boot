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
package dev.macula.boot.starter.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * {@code KafkaAutoConfigurationCompatibilityTest} Kafka自动配置兼容性单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class KafkaAutoConfigurationCompatibilityTest {

    @Test
    void springBootKafkaInfrastructureCanStartWithoutBrokerConnection() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
            .withPropertyValues("spring.kafka.bootstrap-servers=localhost:9092")
            .run(context -> {
                assertThat(context).hasSingleBean(ProducerFactory.class);
                assertThat(context).hasSingleBean(KafkaTemplate.class);
            });
    }
}
