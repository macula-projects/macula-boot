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
package dev.macula.boot.starter.operationlog;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.macula.boot.starter.async.config.AsyncAutoConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

/**
 * {@link OperationLogListener} 受管异步线程日志上下文测试。
 *
 * @author Rain
 * @since 6.1.0
 */
class OperationLogAsyncContextTest {

    @Test
    void shouldKeepPublishingTraceIdOnManagedAsyncTask() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AsyncAutoConfiguration.class))
            .run(context -> {
                Logger logger = (Logger) LoggerFactory.getLogger(OperationLogListener.class);
                ListAppender<ILoggingEvent> appender = new ListAppender<>();
                appender.start();
                logger.addAppender(appender);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    executor.submit(() -> { }).get();
                    MDC.put("traceId", "0123456789abcdef0123456789abcdef");
                    OperationLogDTO operationLog = new OperationLogDTO();
                    operationLog.setMethod("createOrder");
                    Runnable decorated = context.getBean(TaskDecorator.class).decorate(() ->
                        new OperationLogListener().saveOperationLog(new OperationLogEvent(operationLog)));

                    executor.submit(decorated).get();

                    assertThat(appender.list).singleElement().satisfies(event ->
                        assertThat(event.getMDCPropertyMap().get("traceId"))
                            .isEqualTo("0123456789abcdef0123456789abcdef"));
                    assertThat(executor.submit(() -> MDC.get("traceId")).get()).isNull();
                } finally {
                    MDC.clear();
                    executor.shutdownNow();
                    logger.detachAppender(appender);
                    appender.stop();
                }
            });
    }
}
