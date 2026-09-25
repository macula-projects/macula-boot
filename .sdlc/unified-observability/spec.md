# Spec: 统一可观测性方案 (from intent.md 2026-09-22)
Status: accepted

## Source intent
[`.sdlc/unified-observability/intent.md`](./intent.md)：为 Macula Boot 6.1 提供 Metrics、结构化日志和链路追踪的统一入口与 OTLP 输出，并移除 Prometheus、Logstash、Sleuth、SkyWalking 四个旧 Starter。

## Requirements
1. 新增唯一面向使用方的 `dev.macula.boot:macula-boot-starter-observability`；应用只引入该 Starter，即可获得 Spring Boot Actuator、Micrometer Metrics、Micrometer Tracing/OpenTelemetry、OTLP Metrics/Traces/Logs 所需的运行时依赖，不需要再组合四个旧 Starter。
2. Starter 必须兼容 Macula Boot 6.1、Java 17、Spring Boot 4.0.x 和 Spring Cloud 2025.1.x；新增第三方依赖的版本必须由 `macula-boot-parent` 集中管理，子模块不得硬编码版本。
3. Metrics 必须通过 Micrometer `OtlpMeterRegistry` 以 OTLP/HTTP 导出，并保留旧 Prometheus Starter 为所有指标增加 `application=<spring.application.name>` 公共标签的行为；不得将 `micrometer-registry-prometheus` 或 `/actuator/prometheus` 作为统一 Starter 的标准输出。
4. Traces 必须使用 Spring Boot 4 的 OpenTelemetry/Micrometer Tracing 集成，以 OTLP 导出，并为 Spring MVC、Spring WebFlux、Gateway 以及使用 Spring 自动配置 Builder 创建的 HTTP 客户端提供 W3C Trace Context 的接收与传播。
5. 在有效 Span 上下文内产生的日志必须同时具有 `traceId` 和 `spanId` 关联信息：Spring Boot 结构化控制台/文件日志中可查询这两个字段，OTLP LogRecord 中必须带有对应的 OpenTelemetry Trace ID 和 Span ID；无 Span 的启动或后台日志允许不含关联 ID。
6. Starter 必须为默认 Logback 路径提供 OTLP Logs 挂载与初始化，不要求应用复制 Java 初始化代码；日志 OTLP 导出必须可通过 Spring Boot 的 `management.logging.export.otlp.enabled` 独立禁用，并允许用户提供自己的日志附加器或初始化实现来替换默认实现。
7. 结构化日志采用 Spring Boot 4 原生 structured logging 契约；示例和 Archetype 默认使用 `logstash` JSON 格式，用户可改为 `ecs`、`gelf`、自定义格式或关闭结构化输出。自定义 `logback-spring.xml` 时必须在文档中说明如何保留 Spring Boot structured logging 与 OpenTelemetry appender。
8. 统一 Starter 以及它创建的 Bean 必须可通过配置禁用，并使用 classpath、Bean 与属性条件避免无条件装配；用户提供的 OpenTelemetry、Tracer、指标定制器或日志导出实现不得被覆盖。
9. 统一配置优先沿用 Spring Boot 4 的稳定键：`management.otlp.metrics.export.*`、`management.opentelemetry.tracing.export.otlp.*`、`management.opentelemetry.logging.export.otlp.*`、`management.logging.export.*`、`management.tracing.export.*`、`management.opentelemetry.resource-attributes`、`management.tracing.*` 与 `logging.structured.*`。Macula 自定义键仅限 `macula.observability.enabled`、`macula.observability.logging.capture-mdc-attributes` 和网关响应头开关；所有键、默认值、环境变量示例与禁用方式必须写入 Starter README。
10. 默认不得把全部 MDC、请求头、令牌、Cookie、请求体、响应体或个人信息作为日志属性导出；`macula.observability.logging.capture-mdc-attributes` 使用显式 allowlist，空值表示不额外捕获 MDC。Trace/Span 关联由 OpenTelemetry 上下文提供，不依赖捕获全部 MDC。
11. `macula-boot-starter-cloud-gateway` 必须移除对 Sleuth、Brave 和 SkyWalking toolkit 的直接依赖与运行时探测，改用 Micrometer `Tracer` 获取当前 Trace ID；在存在有效 Span 时继续使用现有 `x-traceId` 响应头名称，在无 Tracer 或无有效 Span 时正常放行请求且不产生空响应头。
12. 当前 Observation/Trace 上下文及日志关联信息必须跨 Spring 托管的异步线程边界传播，覆盖自动配置的 `AsyncTaskExecutor`、`@Async` 方法和使用同一受管 Executor 提交的 `CompletableFuture` 任务；异步任务内日志必须保留父调用的 Trace ID，并在任务结束后恢复或清除工作线程上下文，不能污染后续任务。
13. Macula Boot 6.1 必须移除 Alibaba `transmittable-thread-local`：`TenantContextHolder` 与 `GrayVersionContextHolder` 改用普通 `ThreadLocal`，并分别提供 Micrometer `ThreadLocalAccessor` 注册到 `ContextRegistry`。`macula-boot-starter-async` 改用 `ContextPropagatingTaskDecorator`，通过同一个 Micrometer `ContextSnapshot` 同时传播 Tenant、GrayVersion、Observation、OpenTelemetry Context、Trace/Span 和 MDC；用户提供的其他 `TaskDecorator` 必须能加入 Spring Boot 4.0.8 的有序组合，任何 Starter 都不得静默替换它。
14. Reactor/WebFlux 在启用 `spring.reactor.context-propagation=auto` 时必须跨 `publishOn`、`subscribeOn` 等线程切换保持 Observation 与日志关联；直接创建的 `Thread`、未包装的 `Executors.*`、未指定受管 Executor 的 `CompletableFuture.*Async` 以及第三方私有线程池不承诺自动传播，迁移文档必须给出受管 Executor 或 Micrometer `ContextExecutorService` 的显式接入方式。
15. `macula-boot-starter-auditlog` 和 `macula-boot-starter-operationlog` 的业务行为、公共 API 与持久化契约必须保持不变；已有 `traceId` 字段继续使用当前 Trace 上下文填充（若现状没有自动填充，则本变更不额外扩大为审计模型改造），其中 `OperationLogListener` 的 `@Async` 日志必须保留发布事件时的 Trace ID。
16. 删除 `macula-boot-starter-prometheus`、`macula-boot-starter-logstash`、`macula-boot-starter-sleuth`、`macula-boot-starter-skywalking` 四个模块及其聚合声明、依赖管理项、专用版本属性、自动配置、测试和资源；除迁移文档及历史 SDLC 文档外，生产 POM、Java、资源和示例不得再引用这些 artifact、Brave 或 SkyWalking toolkit。
17. 所有当前引用旧 Starter 的 Alibaba/Tencent Gateway、Consumer、Provider 示例必须迁移到统一 Starter；旧 `logback-skywalking.xml` 与过期注释必须删除，示例配置必须展示统一的 service name、三类 OTLP endpoint、采样率、结构化日志、异步/Reactive 上下文传播和各信号禁用方式。
18. Archetype 中每个可运行后端模块必须按职责引入统一 Starter 或从共同父级继承该依赖，并生成可直接覆盖的 OTLP、采样、structured logging 与上下文传播配置；API-only、聚合 POM 与前端模块不得引入运行时可观测性依赖。
19. 提供从四个旧 Starter 到统一 Starter 的迁移指南，至少包含依赖替换、旧配置键/Logback include 到新配置键的逐项映射、Prometheus Pull 到 OTLP Push 的变化、Zipkin/Brave 与 SkyWalking agent/toolkit 的退出方式、`x-traceId` 兼容行为、Alibaba TTL/TtlRunnable 到 Micrometer Context Propagation 的迁移、异步线程传播边界、禁用/回退方式和破坏性变化清单。
20. 示例基础设施必须提供可重复启动的 OpenTelemetry Collector、Prometheus、Loki 和 Tempo 验证环境；Collector 接收应用 OTLP 数据，将 Metrics 暴露给 Prometheus，将 Logs/Traces 分别转发给 Loki/Tempo。该环境不得改变现有 Alibaba/Tencent 默认 profile 的启动语义，端口、镜像版本、健康检查、数据流和启动命令必须写入示例文档。
21. 端到端验证必须通过一次跨 Gateway/Consumer/Provider 的请求证明：同一 Trace 跨服务和受管异步任务传播；至少一个 Trace ID 可同时在 Tempo Trace、异步任务产生的 Loki 日志及应用响应 `x-traceId` 中关联；Prometheus 能查询到带 `application` 或 `service.name` 标识的应用指标；禁用任一信号不会阻止应用启动或影响其余信号。
22. 自动化测试必须覆盖自动配置发现、总开关、每信号开关、依赖/Bean 条件、用户 Bean 覆盖、公共指标标签、日志 appender 安装与去重、同步及异步 Trace/Span 日志关联、Tenant/GrayVersion `ThreadLocalAccessor`、线程复用后的上下文清理、Reactor 自动传播、Gateway 有/无当前 Span 两条路径，以及旧 Starter 和 Alibaba TTL 依赖的静态残留检查。
23. 根 README、Starter 能力清单、示例 README、Archetype README 和测试/运行命令必须与新的模块边界、配置和验证方式一致。

## Non-goals
- 不在本变更中建设 Grafana 仪表盘、告警规则、生产 SLO、值班路由或自动修复流程。
- 不提供四个旧 Starter 的兼容 artifact、转发模块、弃用周期或旧配置键别名。
- 不保留 Prometheus Pull、Logstash TCP、Zipkin/Brave 或 SkyWalking 作为框架标准导出路径；下游应用可自行额外集成，但不属于统一 Starter 契约。
- 不用 Java Agent 取代库方式的 Spring Boot 自动配置，也不为任意第三方 SDK 提供全自动插桩覆盖。
- 不改变 AuditLog/OperationLog 的业务语义、数据库结构、留存策略或访问控制。
- 不承诺自定义创建且绕过 Spring `RestTemplateBuilder`、`RestClient.Builder` 或 `WebClient.Builder` 的客户端自动传播 Trace Context。
- 不通过全局字节码增强接管任意 `Thread`、JDK common pool 或第三方私有线程池；这些边界必须使用受管 Executor 或显式包装器接入。
- 不把示例可观测性栈定义为生产部署拓扑或容量基线。

## Design
### Governing policies consulted
- 仓库 `AGENTS.md`：Java 17、集中依赖管理、Starter 可覆盖/可禁用、配置与公共 API 变更同步文档和测试、不得在 accepted plan 前修改源码。
- `.agents/rules/architecture.md`：Starter/Examples/Archetype 模块边界与跨模块同步要求。
- `.agents/rules/starter-development.md`：`@AutoConfiguration`、条件装配、`AutoConfiguration.imports`、配置属性及 Starter 最低测试要求。
- `.agents/rules/testing.md`：Surefire/Failsafe 分层、外部基础设施边界与逐级验证顺序。
- `.agents/rules/dependencies-release.md`：父 POM 版本管理、跨模块验证和发布安全要求。
- `REVIEW.md`：实现完成后必须分别进行 Bugs、Security、Compliance 三类证据审查。
- `bands.yaml`：当前仍是示例 control bands，不能视为本项目已批准的生产 SLO。
- Spring Boot 4.0.8 官方 Observability/Metrics/Tracing/Loggers/Structured Logging 文档：采用 Boot 管理的 OpenTelemetry、Micrometer OTLP、标准配置键和结构化日志接口。
- Spring Boot 4.0.8、Spring Framework 7 与 Micrometer Context Propagation 官方文档：使用 `ContextPropagatingTaskDecorator`、`ContextSnapshot`/`ContextExecutorService` 和 Reactor automatic context propagation 处理线程切换。
- OpenTelemetry Java Instrumentation 官方 Logback appender 文档：OTLP LogRecord 的 Logback 桥接、初始化和属性捕获边界。

### Module architecture
新增 `macula-boot-starters/macula-boot-starter-observability`，并在 `macula-boot-starters/pom.xml` 和 `macula-boot-parent/pom.xml` 中登记。模块依赖分工如下：

- `spring-boot-starter-actuator`：Observation、应用指标和 Actuator 基础能力。
- `spring-boot-starter-opentelemetry`：Micrometer Tracing 到 OpenTelemetry、W3C 上下文传播、Trace OTLP exporter 与共享 `OpenTelemetry` Bean。
- `micrometer-registry-otlp`：Micrometer 指标的 OTLP/HTTP push。
- `opentelemetry-logback-appender-1.0`：把 Logback 事件转换为 OpenTelemetry LogRecord；其版本在父 POM 中固定并接受依赖兼容测试。

`ObservabilityAutoConfiguration` 只负责 Macula 增量行为：旧指标公共标签、Logback OTLP appender 的幂等安装、配置属性与用户扩展点。Metrics/Traces/Logs exporter 的网络参数、Resource、采样和批处理继续交给 Spring Boot/OpenTelemetry 的标准自动配置，避免复制 SDK 生命周期。

`macula-boot-commons` 将 Tenant/GrayVersion Holder 的内部存储切换为普通 `ThreadLocal`，并提供对应的 Micrometer `ThreadLocalAccessor`；Accessors 通过 Micrometer 支持的 SPI 或等价的幂等自动配置注册到全局 `ContextRegistry`。`io.micrometer:context-propagation` 版本由 Spring Boot BOM/父 POM 管理。

统一 Starter 不反向依赖 `macula-boot-starter-async`。Async Starter 依赖 Commons 与 Micrometer Context Propagation，只提供一个 `ContextPropagatingTaskDecorator`；Spring Boot 4.0.8 的 Task Executor 自动配置收集全部有序 `TaskDecorator` 并在需要时构造 `CompositeTaskDecorator`，从而允许用户扩展且避免 Starter 循环依赖。

### Signal flow
1. HTTP 请求进入 MVC/WebFlux/Gateway，Spring Observation 创建或继续 Span，并通过 W3C `traceparent`/`tracestate` 传播。
2. Micrometer 指标写入 `OtlpMeterRegistry`；Micrometer Tracing 的当前 Span 同时为支持的指标提供 exemplar 上下文。
3. Micrometer Tracing 将 `traceId`/`spanId` 放入日志 MDC；Spring Boot structured logging 将其写入 JSON，OpenTelemetry Logback appender从当前上下文建立带 Trace/Span 关联的 LogRecord。
4. 任务提交到受管 Executor 时，`ContextPropagatingTaskDecorator` 通过已注册的 Accessors 捕获 Micrometer Context 及 Tenant/GrayVersion；执行线程在受限 Scope 内恢复上下文，任务完成后恢复执行前状态。异步代码显式创建新的 Observation 时，该 Observation 继承捕获到的父 Trace。
5. 应用通过 OTLP/HTTP 把 Metrics、Traces 和 Logs 发往 OpenTelemetry Collector。Collector 分流：Metrics 由 Prometheus 抓取，Logs 发送至 Loki，Traces 发送至 Tempo。
6. Gateway 的 `TraceIdGlobalFilter` 只依赖 Micrometer `Tracer`，在响应提交前写入当前 Trace ID，保持既有 `x-traceId` 外部契约。

### Auto-configuration and override model
- `ObservabilityProperties` 使用前缀 `macula.observability`，总开关默认开启；日志 appender 的安装服从 `management.logging.export.enabled`、`management.logging.export.otlp.enabled`、`management.opentelemetry.enabled` 和缺失类条件。
- 自动配置使用 `@ConditionalOnClass`、`@ConditionalOnProperty`、`@ConditionalOnBean(OpenTelemetry.class)` 与 `@ConditionalOnMissingBean`；无 Logback 时不创建 Logback 相关对象。
- Logback appender 安装器先按固定名称检查已有 appender，存在时只完成必要初始化，不重复挂载；用户自定义安装器或关闭 Macula 日志导出时完全退让。
- `capture-mdc-attributes` 仅把显式列出的键交给 appender；默认空 allowlist。Trace ID/Span ID 由 OpenTelemetry LogRecord 上下文字段承载，结构化控制台则使用 Micrometer Tracing 已提供的 MDC。
- 指标 `application` 标签使用可替换的命名 Bean 或标准 `management.metrics.tags.application` 方式实现；不得覆盖用户已有同名定制。
- 新自动配置登记到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`；仅 Logback 在日志系统初始化前确需的扩展允许使用 `spring.factories`。

### Async and reactive context propagation
- Async Starter 删除 `TtlRunnable`，改为提供有序、可覆盖的 Spring `ContextPropagatingTaskDecorator`。Spring Boot 自动配置的 `AsyncTaskExecutor` 将容器中的 Decorator 应用于执行器；存在其他用户 Decorator 时组成一条有序链，组合顺序与嵌套 Scope 必须保证任务结束时按相反顺序清理上下文，并避免同一种传播器被重复注册。
- 自定义 `AsyncTaskExecutor` 必须显式使用从容器取得的全部有序 Decorator 构建 `CompositeTaskDecorator`，或使用能够接收该组合链的 Spring Boot Builder。文档不得声称仅声明一个未被 Executor 使用的 Decorator 就能覆盖所有线程池，也不得使用 Spring Boot 4.0.8 不存在的 `spring.task.execution.propagate-context` 属性。
- `@Async`、异步事件监听器和显式传入受管 Executor 的 `CompletableFuture` 在提交时捕获上下文，而不是在线程池创建时捕获。任务中对 MDC、Tenant、GrayVersion 或 Observation 的修改不得回写提交线程。
- Reactor/WebFlux 示例启用 `spring.reactor.context-propagation=auto`，依靠 Reactor Context 与已注册的 `ThreadLocalAccessor` 跨线程切换恢复上下文。
- 原生 `ExecutorService` 可通过 Micrometer `ContextExecutorService.wrap(...)` 接入全部已注册上下文。未包装的 common pool 与第三方线程池属于显式边界。
- 上下文传播只继续父上下文，不默认把每个 Runnable 都创建为新 Span；需要独立耗时与错误数据的异步业务必须显式创建子 Observation，且父子关系应可在 Tempo 验证。

### Example and archetype topology
现有 Alibaba/Tencent 示例 POM 统一替换依赖。应用配置用环境变量给出 Collector endpoint，并允许本地默认值；采样率在验证 profile 中为 `1.0`，常规示例不得暗示生产必须全采样。

可观测性基础设施以独立 Compose overlay/文件提供，由文档明确与现有 `alibaba` 或 `tencent` profile 组合启动，避免给当前 profile 增加隐式服务。Collector、Prometheus、Loki、Tempo 使用固定镜像版本、显式健康检查和非冲突端口；Collector 配置作为版本化示例资源提交。验证脚本或步骤通过各后端 HTTP API 查询实际接收结果，不以“容器已启动”代替信号验证。

Archetype 的 Gateway、Admin BFF、OpenAPI、Basic、Service、Third-party 等可运行 Java 模块获得统一依赖与公共配置片段；使用 Async Starter 的模块同时获得组合上下文传播，WebFlux/Gateway 模块显式配置 Reactor automatic context propagation。模板变量与父子 POM 结构保持不变。生成式 smoke test 必须验证生成项目不存在旧 Starter，并能解析/编译统一依赖。

### Removal and migration
删除四个旧模块后，父 POM、Starter aggregator、Gateway、Examples、Archetype 与 README 同步更新。迁移指南保留旧 artifact/config 名称用于检索，但静态残留检查排除 `.sdlc/**`、迁移指南和 Git 历史，只对有效构建与运行资源判定失败。

## Data and interfaces
### Maven contract
- 新增：`dev.macula.boot:macula-boot-starter-observability:${revision}`。
- 删除：`macula-boot-starter-prometheus`、`macula-boot-starter-logstash`、`macula-boot-starter-sleuth`、`macula-boot-starter-skywalking`。
- 删除 `com.alibaba:transmittable-thread-local` 及父 POM 中的 `transmittable-thread.version`；Commons/Async 改用由 Spring Boot BOM 管理的 `io.micrometer:context-propagation`，不得硬编码版本。
- Gateway 不再传递或可选引入 Brave/SkyWalking 类型；统一 Starter 的第三方类型不得出现在 Macula 公共 API 中。

### Configuration contract
- Metrics endpoint：`management.otlp.metrics.export.url`，HTTP/Protobuf URL 包含 `/v1/metrics`。
- Traces endpoint：`management.opentelemetry.tracing.export.otlp.endpoint`，传输与超时沿用同前缀属性。
- Logs endpoint：`management.opentelemetry.logging.export.otlp.endpoint`，HTTP/Protobuf URL 包含 `/v1/logs`。
- Resource identity：`management.opentelemetry.resource-attributes`，示例至少设置 `service.name`、`service.namespace`、`deployment.environment.name`；环境变量可使用 Spring Boot/OpenTelemetry 官方支持的 `OTEL_SERVICE_NAME` 与 `OTEL_RESOURCE_ATTRIBUTES`。
- Sampling：`management.tracing.sampling.probability` 与 `management.opentelemetry.tracing.sampler`。
- Structured logging：`logging.structured.format.console` / `logging.structured.format.file`。
- Signal switches：`management.otlp.metrics.export.enabled=true`、`management.tracing.export.otlp.enabled=true`、`management.logging.export.otlp.enabled=true`；`management.opentelemetry.enabled=false` 可关闭整个 OpenTelemetry SDK。
- Async/Reactive propagation：Spring Boot 4.0.8 自动配置 Executor 通过有序 `TaskDecorator` Bean 组成的 `CompositeTaskDecorator` 传播上下文，不新增虚构的配置键；Reactor/WebFlux 使用 `spring.reactor.context-propagation=auto`。自定义 Executor 必须显式应用同一组合链。
- Macula controls：`macula.observability.enabled=true`、`macula.observability.logging.capture-mdc-attributes=[]`；Gateway 响应头使用现有 `x-traceId`，新增开关归入 `macula.gateway` 前缀且默认保持开启。

配置文档必须分别给出关闭 Metrics、Traces、Logs 和整个 OpenTelemetry SDK 的标准属性；密钥与认证 header 只通过环境变量或外部配置示例表达，不提交真实值。

### Telemetry contract
- Metrics 必须包含稳定的 service identity，旧查询可继续按 `application` 标签筛选；同一资源还使用 `service.name`。
- 日志事件保留时间、级别、logger、线程、消息、异常以及有效时的 Trace ID/Span ID；额外 MDC 字段只有 allowlist 中的键可进入 OTLP。
- Traces 使用 W3C Trace Context；不再产生 Brave/Zipkin 或 SkyWalking 专用传播格式作为框架默认值。
- 受管异步任务继承提交时的 Trace ID、当前 Observation、日志 MDC、Tenant 与 GrayVersion 上下文；任务结束后线程上的这些值恢复为执行前状态。传播不等于自动创建子 Span，子 Observation 仍是显式业务契约。
- 不新增数据库表、消息 schema 或业务 REST API。唯一保留的 HTTP 可观察契约是 Gateway 成功取得当前 Span 时返回 `x-traceId`。

## Flagged concerns
- OpenTelemetry Logback appender is outside Spring Boot and versioned with the OpenTelemetry instrumentation release train: it is required for direct OTLP Logs, so the parent must pin a Boot/OpenTelemetry-compatible version and dependency convergence plus startup tests must guard upgrades, non-blocking.
- Direct OTLP Logs and structured stdout can create duplicate Loki entries when an operator also tails container stdout: the example stack uses direct OTLP as the single Loki ingestion path and the migration guide must warn operators not to enable both ingestion paths without deduplication, non-blocking.
- Log attributes can contain credentials or personal data: arbitrary MDC capture is disabled by default, documentation must use an allowlist and the Security review must verify that headers, bodies, tokens and cookies are not captured, non-blocking.
- `bands.yaml` still contains worked example owners, metrics and routes rather than approved Macula Boot SLOs: implementation and local end-to-end verification can proceed, but production monitoring/maintenance gates cannot claim those bands as valid until the responsible humans replace them, non-blocking.
- Prometheus, Loki and Tempo versions differ in their native OTLP support: the Collector is the stable application-facing boundary and backend-specific translation belongs only in version-pinned example Collector configuration, non-blocking.
- Sampling below 100 percent means a response can contain a Trace ID whose completed trace is not queryable in Tempo: automated end-to-end verification must force 100 percent sampling, while documentation must explain that production sampling is an operator decision, non-blocking.
- Capturing and restoring Micrometer context adds per-task allocation and execution overhead, especially for very small or high-frequency tasks: benchmarks are not a release gate, but the implementation must avoid double decoration, document the boundary, and include a regression test that proves cleanup on reused threads, non-blocking.
- Removing Alibaba TTL breaks downstream code that imports `TtlRunnable`, `TtlCallable`, `TtlWrappers` or relies on unwrapped executor propagation through the transitive dependency: Macula Boot 6.1 permits this breaking change, but the migration guide must provide direct Micrometer replacements and the release notes must call it out, non-blocking.
- Context propagation cannot transparently cover unmanaged JDK common-pool tasks or every third-party executor without instrumentation: documentation and static examples must consistently use the managed Executor or an explicit Micrometer wrapper and must not imply universal propagation, non-blocking.

## Verification strategy
1. Static structure: verify the new module is present in both aggregator and dependency management; verify the four old module directories and effective build references are absent; allow old names only in migration/SDLC documentation.
2. Unit tests with `ApplicationContextRunner`: cover total/logging switches, classpath conditions, endpoint/property binding, `application` metric tag, user Bean override and absence of unconditional beans.
3. Logback tests: use an in-memory or test OTLP exporter to assert one appender is installed, repeated initialization does not duplicate it, explicit MDC allowlist works, arbitrary MDC is excluded, and logs inside/outside a Span have the expected correlation fields.
4. Tracing tests: verify Spring MVC/WebFlux client/server propagation with W3C headers and `traceId`/`spanId` correlation; do not require a real network collector for unit tests.
5. Async propagation tests: prestart and reuse a single worker thread, submit from a parent Observation through the managed Executor, and assert the worker sees the same Trace ID, expected MDC, tenant and gray-version values; then submit a context-free task and assert no values leaked. Cover accessor registration, `@Async`, `OperationLogListener`, an explicitly managed `CompletableFuture`, `ContextExecutorService.wrap(...)` and user-provided `TaskDecorator` composition.
6. Reactive propagation tests: with automatic propagation enabled, cross `publishOn` and `subscribeOn`, then assert Observation/Trace and correlated logging survive the switch; with it disabled, assert the documented boundary rather than silently relying on thread affinity.
7. Child-span test: create an async child Observation from the propagated context and assert its parent Trace ID and distinct Span ID; separately prove that plain propagation does not invent a child Span.
8. Gateway tests: replace Brave/SkyWalking branches with Micrometer `Tracer` fixtures and assert valid current Span, missing Span and missing Tracer behavior, including the unchanged `x-traceId` header.
9. Module verification: run focused tests for the observability Starter, Async Starter and Gateway first, then all affected Examples and Archetype tests; because parent dependency management and modules change, finish with `mvn clean verify` and a deploy-profile packaging check with signing disabled.
10. Archetype smoke test: generate a project from the archetype, verify placeholders/module structure, confirm no old Starter dependency, and compile/test the generated backend modules.
11. Compose validation: run `docker compose config`, start the selected Alibaba or Tencent example chain with the observability overlay, wait on explicit health checks, send a deterministic request and query Prometheus, Loki and Tempo APIs for real data.
12. Correlation acceptance: record one request's response `x-traceId`; prove that exact ID appears in Tempo and in synchronous plus managed-async Loki log records, and prove an application metric is queryable in Prometheus with the expected service identity.
13. Failure-mode acceptance: independently disable Metrics, Traces and Logs export; verify application readiness and unaffected signals, then stop the Collector and verify exporter failure does not crash or block request handling.
14. Documentation and migration review: execute every published command/config example against the pinned Compose stack and verify the four-way migration mapping plus managed/unmanaged async boundary is complete.
15. Review gate: after implementation/testing, run the repository's Bugs, Security and Compliance review passes, with special attention to sensitive log capture, cross-request context leakage, dependency exposure, bounded exporter queues/timeouts and preservation of audit/operation logs.
