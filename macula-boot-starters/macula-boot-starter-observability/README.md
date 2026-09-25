## 概述

本模块为 Macula Boot 提供统一可观测性能力，基于 Spring Boot Actuator、Micrometer Observation、
Micrometer Tracing 和 OpenTelemetry 实现 Metrics、Traces、Logs 三类信号的采集与 OTLP 导出。

主要功能包括：

- Metrics：通过 Micrometer OTLP Registry 推送应用指标，并自动增加 `application` 公共标签
- Traces：使用 OpenTelemetry 和 W3C Trace Context 完成服务端、客户端及网关链路传播
- Logs：自动挂载 OpenTelemetry Logback Appender，导出带 `traceId`、`spanId` 的日志
- Structured Logging：支持 Spring Boot 原生 `logstash`、`ecs`、`gelf` 及自定义结构化日志格式
- Context Propagation：配合 `macula-boot-starter-async` 传播 Trace、MDC、Tenant 和 GrayVersion 上下文
- 安全控制：默认不采集任意 MDC、请求头、Token、Cookie、请求体或响应体

本模块统一替代 Macula Boot 6.1 之前的 Prometheus、Logstash、Sleuth 和 SkyWalking Starter。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-observability</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用说明

### 基础配置

引入组件后，根据需要开启三类 OTLP 导出。以下配置默认把数据发送到本机 OpenTelemetry Collector：

```yaml
management:
  otlp:
    metrics:
      export:
        enabled: true
        url: ${OTEL_EXPORTER_OTLP_METRICS_ENDPOINT:http://127.0.0.1:4318/v1/metrics}
  tracing:
    sampling:
      probability: ${OTEL_TRACES_SAMPLER_PROBABILITY:0.1}
    export:
      otlp:
        enabled: true
  logging:
    export:
      otlp:
        enabled: true
  opentelemetry:
    resource-attributes:
      service:
        name: ${OTEL_SERVICE_NAME:${spring.application.name}}
        namespace: ${OTEL_SERVICE_NAMESPACE:macula}
      deployment:
        environment:
          name: ${DEPLOYMENT_ENVIRONMENT:local}
    tracing:
      export:
        otlp:
          endpoint: ${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:http://127.0.0.1:4318/v1/traces}
    logging:
      export:
        otlp:
          endpoint: ${OTEL_EXPORTER_OTLP_LOGS_ENDPOINT:http://127.0.0.1:4318/v1/logs}
logging:
  structured:
    format:
      console: logstash
      file: logstash
```

OTLP/HTTP endpoint 必须分别包含 `/v1/metrics`、`/v1/traces`、`/v1/logs`。普通环境默认采样率为
`0.1`，应根据业务流量和存储容量调整；仅端到端验收环境建议使用 `1.0` 全采样。

### 功能开关

- `macula.observability.enabled=false`：关闭 Macula 提供的增量自动配置
- `management.otlp.metrics.export.enabled=false`：关闭 Metrics 导出
- `management.tracing.export.otlp.enabled=false`：关闭 Traces 导出
- `management.logging.export.otlp.enabled=false`：关闭 Logs 导出

Spring Boot 4.0.8 没有统一的 `management.opentelemetry.enabled` 开关。同时关闭上述三类导出可停止
OTLP 网络发送；`macula.observability.enabled=false` 只关闭 Macula 的指标标签和日志 Appender 等增量配置。

用户提供同名 `maculaApplicationMetricsCustomizer` 或
`OpenTelemetryLogbackAppenderInstaller` Bean 时，默认实现会自动退让。

### 结构化日志与日志导出

默认 Logback 会幂等挂载名为 `MACULA_OTEL` 的 OpenTelemetry Appender。在自定义
`logback-spring.xml` 中声明同名官方 `OpenTelemetryAppender` 时，Starter 会向其注入当前
`OpenTelemetry` 实例，但不会重复挂载或接管其生命周期。

默认不导出额外 MDC 属性。如需导出，必须显式配置 allowlist：

```yaml
macula:
  observability:
    logging:
      capture-mdc-attributes:
        - tenantId
        - requestId
```

Trace ID 和 Span ID 由 OpenTelemetry 当前上下文提供，无需使用 `*` 捕获全部 MDC。请勿将 Token、
Cookie、请求体、响应体或个人信息加入 allowlist。

### HTTP 链路传播

Spring MVC、WebFlux 和 Gateway 使用 Spring Boot Observation 自动插桩。OpenFeign 客户端应通过
`macula-boot-starter-feign` 创建，该 Starter 使用 `feign-micrometer` 和
`MicrometerObservationCapability`，以 W3C `traceparent` 继续当前链路。

绕过 Spring 自动配置，直接创建 `RestTemplate`、`RestClient` 或 `WebClient` 时，需要自行挂载相应的
Observation 插桩。

### 异步线程上下文传播

Spring 托管 Executor 的上下文传播由 `macula-boot-starter-async` 提供，可覆盖 `@Async` 及显式传入
同一受管 Executor 的 `CompletableFuture`：

```java
CompletableFuture.runAsync(() -> {
    // 当前 Trace、MDC、Tenant 和 GrayVersion 均可在此处获取
}, taskExecutor);
```

WebFlux 和 Gateway 应开启 Reactor 自动上下文传播：

```yaml
spring:
  reactor:
    context-propagation: auto
```

原生 `ExecutorService` 可使用 `ContextExecutorService.wrap(...)` 显式接入。直接创建的 `Thread`、
未包装的 `Executors.*`、未指定 Executor 的 `CompletableFuture.*Async` 以及第三方私有线程池不会自动传播。

### 迁移与验证

- 旧 Starter、配置键、Logback include 和 Alibaba TTL 的迁移方式参见
  [统一可观测性迁移指南](../../docs/migration/unified-observability.md)
- OpenTelemetry Collector、Prometheus、Loki、Tempo 的本地验证环境参见
  [示例 Docker 使用说明](../../macula-boot-examples/docker/README.md)

## 版权说明

- Spring Boot：https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt
- Micrometer：https://github.com/micrometer-metrics/micrometer/blob/main/LICENSE
- OpenTelemetry Java：https://github.com/open-telemetry/opentelemetry-java/blob/main/LICENSE
- OpenTelemetry Java Instrumentation：https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/LICENSE
