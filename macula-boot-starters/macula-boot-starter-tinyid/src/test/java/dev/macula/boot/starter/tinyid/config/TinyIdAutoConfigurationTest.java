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
package dev.macula.boot.starter.tinyid.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.macula.boot.starter.tinyid.base.factory.IdGeneratorFactory;
import dev.macula.boot.starter.tinyid.base.service.SegmentIdService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import dev.macula.boot.starter.tinyid.remote.TinyIdFeignClient;

/**
 * {@link TinyIdAutoConfiguration} 自动配置测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class TinyIdAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TinyIdAutoConfiguration.class, FeignAutoConfiguration.class));

    @Test
    void createsClientBeansWithoutCallingServer() {
        contextRunner
            .withPropertyValues("macula.cloud.endpoint=http://localhost:9000",
                "macula.cloud.app-key=test-app", "macula.cloud.secret-key=test-only-secret")
            .run(context -> {
                assertThat(context).hasSingleBean(SegmentIdService.class);
                assertThat(context).hasSingleBean(IdGeneratorFactory.class);
                assertThat(context).hasSingleBean(TinyIdFeignClient.class);
            });
    }

    @Test
    void backsOffForApplicationProvidedBeans() {
        SegmentIdService segmentService = mock(SegmentIdService.class);
        IdGeneratorFactory generatorFactory = mock(IdGeneratorFactory.class);

        contextRunner.withBean(SegmentIdService.class, () -> segmentService)
            .withBean(IdGeneratorFactory.class, () -> generatorFactory)
            .run(context -> {
                assertThat(context.getBean(SegmentIdService.class)).isSameAs(segmentService);
                assertThat(context.getBean(IdGeneratorFactory.class)).isSameAs(generatorFactory);
                assertThat(context).doesNotHaveBean(TinyIdFeignClient.class);
            });
    }

    @Test
    void serverServiceCreatesLocalFactoryWithoutRemoteCredentialsOrMybatisPlus() {
        contextRunner.withClassLoader(new FilteredClassLoader("com.baomidou.mybatisplus"))
            .withBean(SegmentIdService.class, () -> mock(SegmentIdService.class)).run(context -> {
                assertThat(context).hasSingleBean(IdGeneratorFactory.class);
                assertThat(context).doesNotHaveBean(TinyIdFeignClient.class);
            });
    }
}
