# Macula Boot 6.1 统一可观测性迁移指南

## 依赖替换

删除以下依赖：

- `macula-boot-starter-prometheus`
- `macula-boot-starter-logstash`
- `macula-boot-starter-sleuth`
- `macula-boot-starter-skywalking`

统一替换为 `dev.macula.boot:macula-boot-starter-observability`。Macula Boot 6.1 不提供兼容 artifact、
旧配置别名或双运行路径。

## 配置映射

| 旧能力 | 新配置/行为 |
| --- | --- |
| `/actuator/prometheus` 与 Prometheus Pull | `management.otlp.metrics.export.url`，应用向 Collector OTLP Push |
| Sleuth/Zipkin endpoint | `management.opentelemetry.tracing.export.otlp.endpoint` |
| Brave 采样 | `management.tracing.sampling.probability` 或 OpenTelemetry sampler |
| Logstash TCP appender | `management.opentelemetry.logging.export.otlp.endpoint` |
| `logback-skywalking.xml` | Spring Boot `logging.structured.*` 加默认 OpenTelemetry Logback appender |
| SkyWalking toolkit/agent trace API | Micrometer `Tracer` 与 W3C `traceparent`/`tracestate` |
| 全量 MDC 捕获 | `macula.observability.logging.capture-mdc-attributes` 显式 allowlist |

Gateway 继续返回 `x-traceId`，但只在 Micrometer 当前 Span 有效且
`macula.gateway.trace-id-response-header-enabled=true` 时写入。

## Alibaba TTL 迁移

`com.alibaba:transmittable-thread-local` 已移除。Tenant 与 GrayVersion 的公共 Holder 方法保持不变，
底层改为普通 `ThreadLocal` 并由 Micrometer `ThreadLocalAccessor` 接入 `ContextSnapshot`。

| TTL 用法 | Micrometer/Spring 替换 |
| --- | --- |
| `TtlRunnable.get(runnable)` | Spring 受管 Executor + `ContextPropagatingTaskDecorator` |
| `TtlCallable.get(callable)` | `ContextSnapshot.wrap(callable)` |
| `TtlWrappers.wrapSupplier(...)` | 显式使用受管 Executor，或自行捕获 `ContextSnapshot` |
| `TtlExecutors.getTtlExecutorService(...)` | `ContextExecutorService.wrap(executorService)` |

`@Async`、异步事件监听器及显式传入受管 Executor 的 `CompletableFuture` 在提交时捕获
Observation、Trace/MDC、Tenant、GrayVersion，并在任务结束后恢复线程。未指定 Executor 的
`CompletableFuture.*Async`、JDK common pool、直接创建的 Thread 和第三方私有线程池不自动传播。

Reactor/WebFlux 使用：

```yaml
spring:
  reactor:
    context-propagation: auto
```

上下文传播只延续父上下文，不会为每个 Runnable 自动创建子 Span；需要独立耗时和错误信息时，
业务代码应显式创建子 Observation。

## 禁用与回退

Metrics、Traces、Logs 可分别通过各自 `enabled=false` 关闭。Spring Boot 4.0.8 没有统一的
`management.opentelemetry.enabled` 开关；同时关闭三类导出可停止 OTLP 网络发送。
`macula.observability.enabled=false` 只关闭 Macula 增量配置，不恢复已删除的旧 Starter。

6.1 的回退方式是回退应用和 Macula Boot 版本；不能在同一应用中同时依赖新 Starter 与四个旧 Starter。
升级前应搜索下游对 Brave、SkyWalking toolkit、TTL 类型、旧 Logback include 和
`/actuator/prometheus` 的直接引用。
