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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 将 OpenTelemetry Logback appender 幂等挂载到根 Logger。
 *
 * @author Rain
 * @since 6.1.0
 */
public final class OpenTelemetryLogbackAppenderInstaller implements AutoCloseable {

    public static final String APPENDER_NAME = "MACULA_OTEL";

    private final Logger rootLogger;

    private final OpenTelemetryAppender installedAppender;

    public OpenTelemetryLogbackAppenderInstaller(OpenTelemetry openTelemetry, List<String> captureMdcAttributes) {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext)) {
            this.rootLogger = null;
            this.installedAppender = null;
            return;
        }
        Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        Appender<ILoggingEvent> existingAppender = root.getAppender(APPENDER_NAME);
        if (existingAppender != null) {
            if (existingAppender instanceof OpenTelemetryAppender openTelemetryAppender) {
                openTelemetryAppender.setOpenTelemetry(openTelemetry);
            }
            this.rootLogger = null;
            this.installedAppender = null;
            return;
        }
        OpenTelemetryAppender appender = new OpenTelemetryAppender();
        appender.setContext(loggerContext);
        appender.setName(APPENDER_NAME);
        appender.setOpenTelemetry(openTelemetry);
        appender.setCaptureMdcAttributes(String.join(",", captureMdcAttributes));
        appender.start();
        root.addAppender(appender);
        this.rootLogger = root;
        this.installedAppender = appender;
    }

    @Override
    public void close() {
        if (rootLogger != null && installedAppender != null) {
            rootLogger.detachAppender(installedAppender);
            installedAppender.stop();
        }
    }
}
