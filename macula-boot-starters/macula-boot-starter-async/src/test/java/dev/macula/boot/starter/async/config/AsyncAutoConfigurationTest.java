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

import dev.macula.boot.context.GrayVersionContextHolder;
import dev.macula.boot.context.TenantContextHolder;
import io.micrometer.context.ContextExecutorService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.test.simple.SimpleTracer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.slf4j.MDC;

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
    void providesContextDecoratorAndCleansReusedWorkerThread() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TaskDecorator.class);
            assertThat(context.getBean(TaskDecorator.class))
                .isInstanceOf(ContextPropagatingTaskDecorator.class);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                assertThat(executor.submit(TenantContextHolder::getCurrentTenantId).get()).isNull();
                TenantContextHolder.setCurrentTenantId(1001L);
                GrayVersionContextHolder.setGrayVersion("gray-v2");
                MDC.put("requestId", "request-1");
                AtomicReference<String> observed = new AtomicReference<>();
                Runnable decorated = context.getBean(TaskDecorator.class)
                    .decorate(() -> observed.set(TenantContextHolder.getCurrentTenantId() + ":"
                        + GrayVersionContextHolder.getGrayVersion() + ":" + MDC.get("requestId")));
                TenantContextHolder.setCurrentTenantId(2002L);
                GrayVersionContextHolder.setGrayVersion("changed");
                MDC.put("requestId", "changed");

                CompletableFuture.runAsync(decorated, executor).get();

                assertThat(observed).hasValue("1001:gray-v2:request-1");
                assertThat(executor.submit(() -> Map.of(
                    "tenant", String.valueOf(TenantContextHolder.getCurrentTenantId()),
                    "gray", String.valueOf(GrayVersionContextHolder.getGrayVersion()),
                    "mdc", String.valueOf(MDC.get("requestId")))).get())
                    .containsEntry("tenant", "null")
                    .containsEntry("gray", "null")
                    .containsEntry("mdc", "null");
            } finally {
                executor.shutdownNow();
                TenantContextHolder.clearCurrentTenantId();
                GrayVersionContextHolder.clear();
                MDC.clear();
            }
        });
    }

    @Test
    void propagatesContextThroughAsyncAndManagedCompletableFuture() {
        contextRunner.withUserConfiguration(AsyncTestConfiguration.class).run(context -> {
            TenantContextHolder.setCurrentTenantId(1001L);
            GrayVersionContextHolder.setGrayVersion("gray-v2");
            MDC.put("requestId", "request-1");
            Observation observation = Observation.start("async-test", context.getBean(ObservationRegistry.class));
            try (Observation.Scope ignored = observation.openScope()) {
                assertThat(context.getBean(AsyncProbe.class).capture().get())
                    .isEqualTo("1001:gray-v2:request-1:true");
            } finally {
                observation.stop();
                TenantContextHolder.clearCurrentTenantId();
                GrayVersionContextHolder.clear();
                MDC.clear();
            }
        });
    }

    @Test
    void propagatesParentSpanAndCreatesDistinctAsyncChildObservation() {
        contextRunner.withUserConfiguration(AsyncTestConfiguration.class).run(context -> {
            ObservationRegistry registry = context.getBean(ObservationRegistry.class);
            SimpleTracer tracer = context.getBean(SimpleTracer.class);
            Observation parent = Observation.start("parent", registry);
            try (Observation.Scope ignored = parent.openScope()) {
                Span parentSpan = tracer.currentSpan();
                AsyncSpanCapture capture = context.getBean(AsyncProbe.class).captureSpan().get();

                assertThat(capture.propagatedTraceId()).isEqualTo(parentSpan.context().traceId());
                assertThat(capture.propagatedSpanId()).isEqualTo(parentSpan.context().spanId());
                assertThat(capture.childTraceId()).isEqualTo(parentSpan.context().traceId());
                assertThat(capture.childSpanId()).isNotEqualTo(parentSpan.context().spanId());
            } finally {
                parent.stop();
            }
        });
    }

    @Test
    void wrapsExplicitExecutorWithContextExecutorService() throws Exception {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ExecutorService executor = ContextExecutorService.wrap(delegate);
        try {
            TenantContextHolder.setCurrentTenantId(1001L);
            assertThat(executor.submit(TenantContextHolder::getCurrentTenantId).get()).isEqualTo(1001L);
            TenantContextHolder.clearCurrentTenantId();
            assertThat(executor.submit(TenantContextHolder::getCurrentTenantId).get()).isNull();
        } finally {
            executor.shutdownNow();
            TenantContextHolder.clearCurrentTenantId();
        }
    }

    @Test
    void keepsUserTaskDecoratorForBootOrderedComposition() {
        TaskDecorator userDecorator = runnable -> () -> runnable.run();

        contextRunner.withBean("userTaskDecorator", TaskDecorator.class, () -> userDecorator)
            .run(context -> assertThat(context.getBeansOfType(TaskDecorator.class))
                .containsKeys("maculaContextPropagatingTaskDecorator", "userTaskDecorator")
                .hasSize(2));
    }

    /**
     * 提供受管异步执行器的测试配置。
     *
     * @since 6.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class AsyncTestConfiguration {

        @Bean("taskExecutor")
        ThreadPoolTaskExecutor taskExecutor(TaskDecorator taskDecorator) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(1);
            executor.setTaskDecorator(taskDecorator);
            return executor;
        }

        @Bean
        ObservationRegistry observationRegistry(SimpleTracer tracer) {
            ObservationRegistry registry = ObservationRegistry.create();
            registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
            return registry;
        }

        @Bean
        SimpleTracer simpleTracer() {
            return new SimpleTracer();
        }

        @Bean
        AsyncProbe asyncProbe(ObservationRegistry observationRegistry, SimpleTracer tracer) {
            return new AsyncProbe(observationRegistry, tracer);
        }
    }

    /**
     * 用于验证 {@link Async} 代理传播上下文的测试 Bean。
     *
     * @since 6.1.0
     */
    static class AsyncProbe {

        private final ObservationRegistry observationRegistry;
        private final SimpleTracer tracer;

        AsyncProbe(ObservationRegistry observationRegistry, SimpleTracer tracer) {
            this.observationRegistry = observationRegistry;
            this.tracer = tracer;
        }

        @Async
        public CompletableFuture<String> capture() {
            return CompletableFuture.completedFuture(TenantContextHolder.getCurrentTenantId() + ":"
                + GrayVersionContextHolder.getGrayVersion() + ":" + MDC.get("requestId") + ":"
                + (observationRegistry.getCurrentObservation() != null));
        }

        @Async
        public CompletableFuture<AsyncSpanCapture> captureSpan() {
            Span propagated = tracer.currentSpan();
            Observation child = Observation.start("async-child", observationRegistry);
            try (Observation.Scope ignored = child.openScope()) {
                Span childSpan = tracer.currentSpan();
                return CompletableFuture.completedFuture(new AsyncSpanCapture(
                    propagated.context().traceId(), propagated.context().spanId(),
                    childSpan.context().traceId(), childSpan.context().spanId()));
            } finally {
                child.stop();
            }
        }
    }

    /**
     * 保存异步任务中传播 Span 与显式子 Span 的标识。
     *
     * @since 6.1.0
     */
    private record AsyncSpanCapture(String propagatedTraceId, String propagatedSpanId,
                                    String childTraceId, String childSpanId) {
    }
}
