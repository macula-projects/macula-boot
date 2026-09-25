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
package dev.macula.boot.starter.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * {@link OpenTelemetryLogbackAppenderInstaller} 日志关联与幂等测试。
 *
 * @author Rain
 * @since 6.1.0
 */
class OpenTelemetryLogbackAppenderInstallerTest {

    @Test
    void shouldInstallOnceAndExportAllowedMdcWithSpanContext() {
        InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
            .build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setLoggerProvider(loggerProvider)
            .setTracerProvider(SdkTracerProvider.builder().build())
            .build();
        Logger root = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(Logger.ROOT_LOGGER_NAME);

        try (OpenTelemetryLogbackAppenderInstaller first = new OpenTelemetryLogbackAppenderInstaller(
                 openTelemetry, List.of("tenant"));
             OpenTelemetryLogbackAppenderInstaller second = new OpenTelemetryLogbackAppenderInstaller(
                 openTelemetry, List.of("tenant"))) {
            assertThat(root.iteratorForAppenders())
                .toIterable()
                .filteredOn(appender -> OpenTelemetryLogbackAppenderInstaller.APPENDER_NAME
                    .equals(appender.getName()))
                .hasSize(1);

            Span span = openTelemetry.getTracer("test").spanBuilder("log-test").startSpan();
            try (Scope ignored = span.makeCurrent()) {
                MDC.put("tenant", "1001");
                MDC.put("secret", "do-not-export");
                LoggerFactory.getLogger(getClass()).info("correlated log");
            } finally {
                MDC.clear();
                span.end();
            }

            assertThat(exporter.getFinishedLogRecordItems()).singleElement().satisfies(record -> {
                assertThat(record.getSpanContext().getTraceId()).isEqualTo(span.getSpanContext().getTraceId());
                assertThat(record.getSpanContext().getSpanId()).isEqualTo(span.getSpanContext().getSpanId());
                assertThat(record.getAttributes().get(AttributeKey.stringKey("tenant"))).isEqualTo("1001");
                assertThat(record.getAttributes().get(AttributeKey.stringKey("secret"))).isNull();
            });
        } finally {
            openTelemetry.close();
        }
    }

    @Test
    void shouldInitializeExistingOfficialAppenderWithoutTakingOwnership() {
        InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
            .build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setLoggerProvider(loggerProvider)
            .build();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        OpenTelemetryAppender appender = new OpenTelemetryAppender();
        appender.setContext(loggerContext);
        appender.setName(OpenTelemetryLogbackAppenderInstaller.APPENDER_NAME);
        appender.start();
        root.addAppender(appender);

        try (OpenTelemetryLogbackAppenderInstaller installer = new OpenTelemetryLogbackAppenderInstaller(
            openTelemetry, List.of())) {
            LoggerFactory.getLogger(getClass()).info("existing appender log");

            assertThat(exporter.getFinishedLogRecordItems()).singleElement()
                .satisfies(record -> assertThat(record.getBody().asString()).isEqualTo("existing appender log"));
        } finally {
            assertThat(root.getAppender(OpenTelemetryLogbackAppenderInstaller.APPENDER_NAME)).isSameAs(appender);
            root.detachAppender(appender);
            appender.stop();
            openTelemetry.close();
        }
    }
}
