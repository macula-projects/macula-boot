# Verification Report

Date: 2026-09-28

Change: `unified-observability`

Implementation commit: `a7a11bb04aaffb57da3e1a49543bf60f925c2f65`

Merged commit: `bb4dc9b94972b79ea51b989a859342763e92fc82`

Pull request: `#35 feat: 统一 OpenTelemetry 可观测性与上下文传播`

Current verification base: `ba7c51904bedd49b8235cde078d32e735e9e256c` on `main`

Environment: macOS/ARM64, Java 17.0.17, Maven 3.9.6, Docker Client/Server 29.4.0.

## Result

`Verification is not green`.

The unified OTLP implementation, Maven regression suite, generated-project smoke test, three-signal correlation path, independent signal degradation, and Collector-outage behavior passed. Two current repository defects block a green Stage 4 result:

1. Alibaba Gateway listens for plain HTTP on container port `5080`, while `macula-boot-examples/docker/docker-compose.yml` maps and health-checks container port `5000`. The documented combined Compose command therefore leaves the Gateway in `health: starting` and `docker compose --wait` cannot succeed. HTTPS on container/host port `5443`/`25443` was used only to continue signal verification; it does not waive the broken HTTP Compose contract.
2. Requirement 13 and the Maven contract require Alibaba TTL dependency management to be removed, but `macula-boot-parent/pom.xml` still contains `transmittable-thread.version=2.14.5` and the managed `com.alibaba:transmittable-thread-local` dependency. No maintained source import or active child dependency was found, but the accepted removal contract is incomplete.

No release, tag, push, deployment, or source-code correction was performed during this backfill.

## Maven verification

Focused tests:

```shell
mvn -pl macula-boot-commons,macula-boot-starters/macula-boot-starter-observability,macula-boot-starters/macula-boot-starter-async,macula-boot-starters/macula-boot-starter-operationlog,macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test
```

Result: `BUILD SUCCESS`, 9/9 reactor modules successful. Relevant suites executed 57 tests with 0 failures, 0 errors, and 0 skips: Commons 24, Async 5, Observability 8, Gateway 9, OperationLog 6, and Redis 5.

Full verification and packaging:

```shell
mvn clean verify
mvn -N checkstyle:check
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy
```

Actual results:

```text
clean verify reactor       54/54 modules successful, BUILD SUCCESS, 01:23 min
Surefire                   192 tests, 0 failures, 0 errors, 0 skipped
Failsafe                    49 tests, 0 failures, 0 errors, 3 skipped
Combined                   241 tests, 0 failures, 0 errors, 3 skipped
Checkstyle                 0 violations, BUILD SUCCESS
deploy-profile install     54/54 modules successful, BUILD SUCCESS, 41.462 s
```

The three skipped integration tests require external services not started by the Maven reactor: `OrderServiceIT` (RocketMQ), `TinyIdClientIT`, and `KafkaProducerIT`. No observability-focused test was skipped.

## Archetype and static proof

The installed `dev.macula.boot:macula-boot-archetype:6.1.0-SNAPSHOT` generated a fresh 11-module project. Exactly six runnable backend POMs contained `macula-boot-starter-observability`: Basic, OpenAPI, Service1, Third-party, Gateway, and Admin BFF. The generated project completed `mvn test` with all 11 modules successful.

Searches over effective POM, Java, resource, example, and Archetype sources found no old Prometheus, Logstash, Sleuth, or SkyWalking Starter, Brave, SkyWalking toolkit, or Alibaba TTL import outside documentation and dependency management. The remaining TTL property and managed dependency in `macula-boot-parent/pom.xml` are the blocking residual described above.

Compose rendering succeeded for the overlay alone and for both supported combinations:

```shell
docker compose -f observability/docker-compose.observability.yml config --quiet
docker compose -f docker-compose.yml -f observability/docker-compose.observability.yml --profile alibaba config --quiet
docker compose -f docker-compose.yml -f observability/docker-compose.observability.yml --profile tencent config --quiet
```

## Docker correlation proof

An isolated Compose project named `macula-observability-verify` used non-conflicting host ports and explicit `1.0` sampling. Collector, Prometheus, Loki, Tempo, MySQL, Redis, Nacos, Provider, and Consumer became healthy. The Gateway process started, but its configured HTTP `5080` versus Compose `5000` mismatch caused the combined `--wait` command to fail the health gate.

The Gateway HTTPS route remained available and returned HTTP 200. Request trace `66588eaa96dea6e7a063dec4356ce6e2` provided the following backend proof:

```text
Tempo services  macula-example-alibaba-gateway,
                macula-example-alibaba-consumer,
                macula-example-alibaba-provider1
Loki sync log   1 matching result
Loki async log  1 matching result
Prometheus      180 matching consumer service series
```

This proves one response `x-traceId` crossed Gateway, Consumer, Provider, synchronous logging, and managed-async logging while metrics were exported. It does not convert the failed Compose health contract into a pass.

## Failure-mode proof

Each signal exporter was disabled independently by recreating the three application containers while retaining the other two exporters:

```text
Metrics off  HTTP ready; trace 604754a8e28285479c50db2c594aed27 reached all 3 services; Loki log found
Traces off   HTTP ready; Loki log found; 183 Prometheus service series found
Logs off     HTTP ready; trace 631da5e034e6a281bfabd532405d243f reached all 3 services; 183 Prometheus service series found
```

With the isolated OpenTelemetry Collector stopped, a routed request still returned HTTP 200 and Provider/Consumer remained healthy. This verifies that exporter failure does not synchronously block business handling.

The isolated containers and network were removed with `docker compose down`; named verification volumes were retained. Pre-existing user containers were not stopped or modified.

## CI and merge evidence

Pull request `#35` merged on 2026-09-25. GitHub records successful Maven Verify, Checkstyle, and CodeQL checks for implementation head `a7a11bb04aaffb57da3e1a49543bf60f925c2f65`; Snapshot publication was skipped for the pull request. Historical green CI and merge evidence are recorded for provenance only and do not override the two failures reproduced on the current `main`.

## Protected configuration and evals

The implementation commit did not change `AGENTS.md`, `CLAUDE.md`, AI-SDLC skills, hooks, or other protected agent configuration. No applicable repository eval suite was identified, so no eval command was required.

## Handoff

Keep this change in Test until both blockers are corrected and current verification is rerun. The next implementation should align the Alibaba Gateway Compose mapping/health check with its real HTTP port and remove the obsolete TTL property plus dependency-management entry. After those corrections, rerun the combined Compose `--wait`, deterministic correlation script, static residual scan, focused tests, `mvn clean verify`, Checkstyle, deploy-profile packaging, and `git diff --check` before starting deployment review.
