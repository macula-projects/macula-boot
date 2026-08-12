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
package dev.macula.boot.starter.async.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

/**
 * {@link AsyncAutoConfiguration} 自动配置测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class AsyncAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AsyncAutoConfiguration.class));

    @Test
    void providesTtlTaskDecoratorAndCapturesSubmittingThreadContext() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TaskDecorator.class);

            TransmittableThreadLocal<String> threadLocal = new TransmittableThreadLocal<>();
            AtomicReference<String> observed = new AtomicReference<>();
            try {
                threadLocal.set("captured");
                Runnable decorated = context.getBean(TaskDecorator.class)
                    .decorate(() -> observed.set(threadLocal.get()));
                threadLocal.set("changed-after-submit");

                decorated.run();

                assertThat(observed).hasValue("captured");
            } finally {
                threadLocal.remove();
            }
        });
    }
}
