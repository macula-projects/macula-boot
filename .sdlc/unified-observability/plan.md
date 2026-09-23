# Plan: 统一可观测性方案 (from spec.md 2026-09-23)
Status: accepted

## Files that change
- SDLC contract:
  - `.sdlc/unified-observability/plan.md`: record the accepted implementation contract and append any actual deviation before continuing past it.
- Maven aggregation and dependency management:
  - `macula-boot-parent/pom.xml`: remove TTL, Logstash and SkyWalking-specific version/dependency management; add `macula-boot-starter-observability`; use Spring Boot BOM-managed Micrometer/OpenTelemetry artifacts except the explicitly pinned OpenTelemetry Logback appender required by the spec.
  - `macula-boot-starters/pom.xml`: replace the four retired observability modules with `macula-boot-starter-observability`.
  - `macula-boot-commons/pom.xml`: replace `transmittable-thread-local` with `io.micrometer:context-propagation`.
- Commons context propagation:
  - `macula-boot-commons/src/main/java/dev/macula/boot/context/TenantContextHolder.java`: replace `TransmittableThreadLocal` with ordinary `ThreadLocal` without changing its public methods.
  - `macula-boot-commons/src/main/java/dev/macula/boot/context/GrayVersionContextHolder.java`: replace `TransmittableThreadLocal` with ordinary `ThreadLocal` without changing its public methods.
  - Add `macula-boot-commons/src/main/java/dev/macula/boot/context/TenantContextThreadLocalAccessor.java` and `GrayVersionContextThreadLocalAccessor.java` as Micrometer accessors for the two holders.
  - Add `macula-boot-commons/src/main/resources/META-INF/services/io.micrometer.context.ThreadLocalAccessor` to register both accessors through Micrometer's supported SPI.
  - Update `macula-boot-commons/src/test/java/dev/macula/boot/context/ContextHolderTest.java` and add `ContextThreadLocalAccessorTest.java` to prove get/set/reset semantics, SPI discovery, snapshot restoration and no thread-reuse leakage.
- New unified Starter:
  - Add `macula-boot-starters/macula-boot-starter-observability/pom.xml` with Actuator, Spring Boot OpenTelemetry, Micrometer OTLP registry and OpenTelemetry Logback appender dependencies.
  - Add `macula-boot-starters/macula-boot-starter-observability/README.md` covering all supported properties, defaults, environment variables, signal switches, structured logging, custom Logback, security allowlist, async/reactive boundaries and replacement hooks.
  - Add `macula-boot-starters/macula-boot-starter-observability/src/main/java/dev/macula/boot/starter/observability/config/ObservabilityProperties.java` for the Macula total switch and MDC allowlist only.
  - Add `macula-boot-starters/macula-boot-starter-observability/src/main/java/dev/macula/boot/starter/observability/config/ObservabilityAutoConfiguration.java` for conditional property binding, the replaceable `application` metrics tag and Logback integration entry point.
  - Add `macula-boot-starters/macula-boot-starter-observability/src/main/java/dev/macula/boot/starter/observability/logging/OpenTelemetryLogbackAppenderInstaller.java` for idempotent installation, OpenTelemetry initialization and explicit MDC attribute capture.
  - Add `macula-boot-starters/macula-boot-starter-observability/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` for auto-configuration discovery; add a logging-system bootstrap resource only if the verified Spring Boot/OpenTelemetry API requires initialization before the application context, and record that as a plan deviation.
  - Add `macula-boot-starters/macula-boot-starter-observability/src/test/java/dev/macula/boot/starter/observability/config/ObservabilityAutoConfigurationTest.java` for conditions, switches, metric tags and user overrides.
  - Add `macula-boot-starters/macula-boot-starter-observability/src/test/java/dev/macula/boot/starter/observability/logging/OpenTelemetryLogbackAppenderInstallerTest.java` for installation, deduplication, allowlist and span/no-span log correlation.
  - Add test resources under `macula-boot-starters/macula-boot-starter-observability/src/test/resources/` only where required by Logback or Spring Boot context tests; every added resource will be recorded in this plan.
- Async Starter:
  - `macula-boot-starters/macula-boot-starter-async/pom.xml`: remove TTL and add Commons plus Micrometer Context Propagation.
  - `macula-boot-starters/macula-boot-starter-async/src/main/java/dev/macula/boot/starter/async/config/AsyncAutoConfiguration.java`: replace `TtlRunnable` with an ordered, conditional `ContextPropagatingTaskDecorator` while retaining async/scheduling behavior.
  - `macula-boot-starters/macula-boot-starter-async/src/test/java/dev/macula/boot/starter/async/config/AsyncAutoConfigurationTest.java`: cover capture-at-submission, Observation/MDC/Tenant/GrayVersion propagation, cleanup on a reused thread and coexistence with another ordered decorator.
  - `macula-boot-starters/macula-boot-starter-async/README.md`: document managed executors, `@Async`, explicitly managed `CompletableFuture`, `ContextExecutorService.wrap(...)`, Reactor auto propagation and unsupported unmanaged pools.
- Gateway and operation-log compatibility:
  - `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/pom.xml`: remove Brave/SkyWalking-related dependencies and use Micrometer Tracing APIs.
  - `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/TraceIdGlobalFilter.java`: resolve the current trace through Micrometer `Tracer` and preserve `x-traceId` only for a valid span.
  - `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/config/GatewayProperties.java` and `GatewayAutoConfiguration.java`: add and wire the backward-compatible response-header switch under the existing `macula.gateway` prefix.
  - `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/TraceIdGlobalFilterTest.java` and `config/GatewayPropertiesTest.java`: cover valid span, missing span, missing tracer and disabled header.
  - `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/README.md`: replace old tracing guidance with the Micrometer contract.
  - Add `macula-boot-starters/macula-boot-starter-operationlog/src/test/java/dev/macula/boot/starter/operationlog/OperationLogAsyncContextTest.java` to prove that `OperationLogListener` keeps the publishing trace through the managed async executor; production operation-log classes remain unchanged unless a failing compatibility test proves otherwise, which would require a recorded deviation.
- Retired modules, deleted in full from tracked source:
  - `macula-boot-starters/macula-boot-starter-prometheus/**`.
  - `macula-boot-starters/macula-boot-starter-logstash/**`.
  - `macula-boot-starters/macula-boot-starter-sleuth/**`.
  - `macula-boot-starters/macula-boot-starter-skywalking/**`.
- Alibaba/Tencent examples:
  - Update the `pom.xml`, `src/main/resources/application.yml` and `README.md` files in `macula-example-alibaba-gateway`, `macula-example-alibaba-consumer`, `macula-example-alibaba-provider1`, `macula-example-tencent-gateway`, `macula-example-tencent-consumer` and `macula-example-tencent-provider` to use the unified Starter and documented OTLP/structured logging/context-propagation settings.
  - Delete `macula-boot-examples/macula-example-alibaba-{gateway,consumer,provider1}/src/main/resources/logback-skywalking.xml`.
  - `macula-boot-examples/README.md`: document the unified signal flow and end-to-end example path.
  - `macula-boot-examples/macula-example-alibaba-provider2` remains a non-runnable reserved module and receives no runtime observability dependency.
- Repeatable observability example infrastructure:
  - `macula-boot-examples/docker/.env.example`: add overridable, non-secret observability image, endpoint and port defaults.
  - `macula-boot-examples/docker/README.md`: add overlay startup, health, query, shutdown and troubleshooting commands without changing the existing `alibaba` or `tencent` profile contract.
  - Add `macula-boot-examples/docker/observability/docker-compose.observability.yml` with pinned OpenTelemetry Collector, Prometheus, Loki and Tempo services and explicit health checks.
  - Add `macula-boot-examples/docker/observability/otel-collector.yaml`, `prometheus.yml`, `loki.yaml` and `tempo.yaml` for the Collector-to-backend routes.
  - Add `macula-boot-examples/docker/observability/verify-observability.sh` to issue one deterministic example request and query Prometheus, Loki and Tempo for correlated evidence.
- Archetype runtime modules:
  - Update `pom.xml` and `src/main/resources/application.yml` in `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-{admin-bff,basic,gateway,openapi,service1,thirdparty}/` to add the unified Starter and overridable OTLP, sampling, structured logging and applicable Reactor propagation settings.
  - `macula-boot-archetype/src/main/resources/archetype-resources/README.md`: document generated observability defaults and overrides.
  - API-only modules, the parent/aggregator POMs and the frontend module remain free of runtime observability dependencies.
- Project documentation:
  - `README.md`: replace the four retired Starter entries with the unified Starter and link its usage/migration documentation.
  - Add `docs/migration/unified-observability.md` with the four-Starter dependency/config mapping, Prometheus Pull-to-OTLP Push change, Brave/Zipkin/SkyWalking removal, `x-traceId`, TTL-to-Micrometer replacements, managed/unmanaged async boundaries, disable/rollback guidance and breaking-change list.
  - Existing generated `flattened.xml` and all `target/**` files are not edited; if the repository release process regenerates tracked flattened descriptors, their update is deferred to release tooling rather than hand-written in this implementation.

## Order of work
1. Recheck the accepted spec, branch and clean worktree; resolve the effective Spring Boot 4.0.8 dependency/API surface and capture a dependency tree baseline for OpenTelemetry, Micrometer Context Propagation, Logback, Brave, SkyWalking and TTL.
2. Update parent/aggregator dependency contracts, Commons holders/accessors and their tests first. Prove SPI discovery and cleanup before any asynchronous integration uses the new accessors.
3. Create `macula-boot-starter-observability` with conditional auto-configuration, standard Boot properties, metric identity and idempotent Logback OTLP installation. Add focused context-runner and logging tests, then verify this module in isolation.
4. Migrate the Async Starter to `ContextPropagatingTaskDecorator`; prove propagation and cleanup with a prestarted single-thread executor, then add the operation-log async compatibility test.
5. Migrate Gateway to Micrometer `Tracer`, add the response-header switch and run its focused tests for valid/missing context paths.
6. Remove the four retired modules only after their replacement compiles and focused tests pass; run static searches across effective source/POM/resources to ensure TTL, old Starter, Brave and SkyWalking runtime references are gone while allowing migration and SDLC documentation.
7. Migrate the six runnable Alibaba/Tencent applications and delete the three SkyWalking Logback files. Keep ordinary example defaults usable without an observability backend and make all exporter endpoints/environment identity overrideable.
8. Add the separate observability Compose overlay and pinned backend configurations. Validate it with `docker compose config` independently and combined with each existing `alibaba` and `tencent` profile.
9. Update the six runnable Archetype modules and perform an archetype generation smoke test that checks placeholders, dependency boundaries and compilation of generated backend modules.
10. Complete root/Starter/example/archetype documentation and the migration guide, then run focused module tests, affected reactor builds, full `mvn clean verify`, deploy-profile packaging with signing disabled, static residue checks and `git diff --check`.
11. When Docker is available, start one selected example chain plus the observability overlay, force sampling to `1.0`, execute the verification script and retain exact evidence that one response Trace ID appears in Tempo and synchronous/managed-async Loki logs while Prometheus exposes the service metric. Independently disable Metrics, Traces and Logs and verify the remaining application behavior.
12. Compare the final diff with every listed file, requirement and non-goal. Record any unavoidable file, API, order or proof deviation in this plan and pause for engineer direction if it invalidates the contract.

## Risks
- Spring Boot 4.0.8 and its managed OpenTelemetry modules may expose different artifact names or initialization hooks than older Boot/OpenTelemetry examples. Resolve against the effective POM and compiled APIs before writing the installer; do not invent configuration keys or duplicate SDK lifecycle management.
- The OpenTelemetry Logback appender version is outside the Spring Boot BOM and can conflict with Boot's OpenTelemetry SDK line. Pin one compatible version in the parent and prove dependency convergence plus in-memory log export before example migration.
- Direct OTLP logs plus JSON stdout scraping can duplicate Loki records. The example overlay will use one documented ingestion route for application logs and state the alternative rather than enabling both silently.
- Context capture has per-task cost and duplicate decorators can nest scopes or leak state. Use exactly one Macula decorator, retain Boot's ordered composition, and prove restoration on a reused worker thread.
- Removing TTL is a deliberate 6.1 breaking change for downstream imports and implicit propagation. The migration guide must map `TtlRunnable`, `TtlCallable`, `TtlWrappers` and transitive dependency assumptions to Micrometer managed-executor patterns.
- Unmanaged common-pool and third-party executors cannot be covered transparently. Tests and docs must state this boundary and never imply universal propagation.
- Sampling below 100% can produce an `x-traceId` that is absent from Tempo. End-to-end proof fixes sampling at `1.0`; production sampling remains operator-owned.
- Capturing arbitrary MDC or headers can disclose credentials or personal data. The default allowlist stays empty and tests assert that unlisted keys do not enter OTLP records.
- Collector, Loki, Tempo and Prometheus configuration schemas vary by pinned release. Pin versions, validate configuration/health endpoints and keep backend-specific translation inside the example overlay.
- Removing four modules changes the Maven reactor and can reveal downstream compile failures outside initial search results. Static scans and full reactor verification are required before implementation can be considered complete.
- Existing `bands.yaml` is still illustrative, so implementation can prove local behavior but cannot claim approved production SLOs or operational readiness.
- Rollback is source-level: revert the implementation commit(s) to restore the old Starters and TTL. No compatibility artifacts, old property aliases or dual-running framework path will be introduced.

## Proof
- Commons unit tests: `ContextHolderTest` and `ContextThreadLocalAccessorTest` prove unchanged holder APIs, ServiceLoader registration, snapshot capture/reset and no leakage after thread reuse.
- Observability Starter context tests: `ObservabilityAutoConfigurationTest` proves discovery, total/signal conditions, standard property binding, metric `application` identity and user-bean backoff.
- Logging tests: `OpenTelemetryLogbackAppenderInstallerTest` uses an in-memory exporter to prove one appender, idempotent initialization, empty-by-default MDC allowlist, explicit attribute capture and correct Trace ID/Span ID inside a span.
- Async tests: `AsyncAutoConfigurationTest` proves capture at task submission, `@Async`/managed Executor/managed `CompletableFuture` propagation, ordered decorator composition, Observation/MDC/Tenant/GrayVersion visibility and cleanup on the next context-free task; it also covers `ContextExecutorService.wrap(...)`.
- Reactive tests in the affected Starter/example test scope prove Observation and log correlation across `publishOn`/`subscribeOn` with `spring.reactor.context-propagation=auto`, and document behavior when disabled.
- Operation-log compatibility: `OperationLogAsyncContextTest` proves the asynchronous listener sees the publishing Trace ID without changing its public API or persistence contract.
- Gateway tests: `TraceIdGlobalFilterTest` and `GatewayPropertiesTest` prove the unchanged `x-traceId` name for a valid current Micrometer span, and no empty header for missing tracer/span or a disabled switch.
- Maven/static proof: focused `mvn -pl ... -am test` runs for Commons, Observability, Async, OperationLog and Gateway precede affected Examples/Archetype verification; repository searches prove no effective old Starter, Brave, SkyWalking toolkit or Alibaba TTL dependency/import remains outside migration/SDLC history.
- Archetype proof: generate a project from the template, verify only runnable backend POMs receive observability, assert API/aggregator/frontend boundaries, and compile/test generated backend modules.
- Compose proof: `docker compose config` succeeds for the overlay alone and with each existing platform profile; explicit health checks become healthy for Collector, Prometheus, Loki and Tempo.
- Correlation proof: one deterministic Gateway/Consumer/Provider request yields an `x-traceId` that is queryable as the same trace in Tempo and in synchronous plus managed-async Loki records, while Prometheus returns a metric with `application` or `service.name` identity.
- Failure-mode proof: disabling each exporter independently, disabling all OpenTelemetry, and stopping the Collector do not prevent application readiness or block requests; unaffected signals continue operating.
- Final regression proof: `mvn clean verify`, deploy-profile packaging with signing disabled, configuration/document command execution, dependency convergence review and `git diff --check` complete without weakening the accepted non-goals.
