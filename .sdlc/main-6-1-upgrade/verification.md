# Verification Report

Date: 2026-09-22

Change: `main-6-1-upgrade`

Baseline: `fbd20cfd4020734880ff4cc41f7cec95a0bfd70e`

Change under test: branch `feat/main-6-1-upgrade`, commit `56459b000d935d195343ffeaec141e0e55c3868d` plus the Stage 4 corrections listed below.

Environment: macOS, Java 17.0.17, Maven 3.9.6, OrbStack Docker Engine 29.4.0 on `linux/arm64`, Docker Compose v5.1.2.

## Result

`Verification is green`.

The verified platform is:

```text
Macula Boot revision       6.1.0-SNAPSHOT
Spring Boot               4.0.8
Spring Cloud              2025.1.1
Spring Cloud Alibaba      2025.1.0.0
Spring Cloud Tencent      2.1.2.0-2025.1.1
Java                      17
```

No release, tag, merge, production deployment, or `5.x` modification was performed. Native Windows and `linux/amd64` runtime were not executed; runtime evidence is macOS/ARM64 only.

## Source and branch proof

```text
branch=feat/main-6-1-upgrade
HEAD=56459b000d935d195343ffeaec141e0e55c3868d
6.0.x=fbd20cfd4020734880ff4cc41f7cec95a0bfd70e
origin/6.0.x=fbd20cfd4020734880ff4cc41f7cec95a0bfd70e
```

The local `5.x` branch was already stale relative to `origin/5.x` and was left untouched:

```text
local 5.x=beaa74b1639786306aa817bef0e522a23c995fd4
origin/5.x=7d8435df4d0edd59ddb6a440fc282b81fab3246d
```

`git diff --check` completed with no whitespace errors. The protected-instruction diff changes only the project description in `AGENTS.md` from Boot 3.5/Cloud 2025 to Boot 4.0/Cloud 2025.1. No workflow policy, skill, hook, or evaluation suite changed. No repository evaluation suite was found for this migration.

## Maven verification

Commands:

```shell
mvn clean verify
mvn verify -q
mvn -N checkstyle:check
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy
```

Results:

```text
clean verify reactor       57/57 modules successful
Surefire                   133 tests, 0 failures, 0 errors, 0 skipped
Failsafe                    46 tests, 0 failures, 0 errors, 3 skipped
Combined                   179 tests, 0 failures, 0 errors, 3 skipped
Checkstyle                 0 violations
deploy-profile install     57/57 modules successful, 40.188 s
```

The three skipped tests require services not started by the reactor: Kafka, RocketMQ, and TinyID. Their absence was not treated as a code failure. Focused integration proof also passed for MyBatis Plus (3), JPA/Fenix (1), and Redis/Redisson (8).

Dependency-tree and effective-model checks resolved the accepted versions for representative web, gateway, Alibaba, Tencent, and archetype modules without unresolved artifacts.

## Archetype and configuration proof

The installed `dev.macula.boot:macula-boot-archetype:6.1.0-SNAPSHOT` generated a fresh multi-module project with eight child modules at:

```text
/var/folders/bh/4nmzcd6n0dx7zgq1hq6g_d080000gp/T/tmp.Cspbkbg4UC/stage4-sample
```

`mvn compile` passed for the generated reactor. All six generated application modules produced `target/classes/application.yml` with:

```yaml
spring:
  profiles:
    active: local
```

No packaged application resource retained the Maven token `@profile.active@`. A strict per-document YAML duplicate-key check passed for 20 maintained example, archetype-template, and generated-project YAML files.

## Docker Compose and runtime proof

Static rendering passed for all supported selections:

```shell
docker compose --profile alibaba config --quiet
docker compose --profile tencent config --quiet
docker compose --profile alibaba --profile tencent config --quiet
```

The only profiles remain `alibaba` and `tencent`. Because host ports 3306, 6379, and 5000 were occupied, runtime used only host-side overrides:

```text
MYSQL_PORT=13306
REDIS_PORT=16379
ALIBABA_GATEWAY_HTTP_PORT=15000
```

All six application images built successfully with `bellsoft/liberica-runtime-container:jre-17.0.20.1_1-glibc`. The combined environment reached healthy state for MySQL, Redis, Nacos, Polaris, and all Alibaba/Tencent provider, consumer, and gateway containers; `nacos-init` exited with code 0.

Alibaba routed request:

```text
GET http://127.0.0.1:15000/consumer/api/v1/consumer/echo/health?str=hello
HTTP 200
{"code":"00000","data":"Hello hello, test=365, by anonymousUser, ...","success":true}
```

Tencent routed request:

```text
GET http://127.0.0.1:4000/consumer/api/v1/consumer/echo
HTTP 200
{"code":"00000","data":"Hello consumer, by anonymousUser, ...","success":true}
```

The final application-log scan found no configuration-import, linkage, serialization, classpath, or authentication errors. Containers created for verification were stopped with normal `docker compose down`; named data volumes were retained.

## Stage 4 corrections

The formal verification found and corrected four compatibility defects before the final green run:

1. MyBatis Plus used its Boot 3 starter, so Boot 4 did not create `SqlSessionFactory`. It now uses `mybatis-plus-spring-boot4-starter`.
2. Fenix 3.1.0 called a removed Spring Data JPA method. The managed version is now Fenix 4.0.0.
3. Spring Data Redis 4 JSON serialization required Jackson 3 databind at runtime. The Redis Starter now declares the Boot-managed `tools.jackson.core:jackson-databind` dependency.
4. Packaged examples retained `@profile.active@`, and Tencent Gateway assumed that a Brave class implied a `Tracing` Bean. Parent resource filtering now includes `application*`; the gateway filter selects Brave only when its Bean exists, with a focused regression test.

Runtime retesting also removed noisy Nacos login failures by leaving Compose credentials empty when the local server has authentication disabled. Explicit `NACOS_USERNAME` and `NACOS_PASSWORD` values remain supported for authenticated servers.

## Handoff

Stage 4 evidence is complete and green. The next step is the human-gated `sdlc-deploy` review pass; this report does not self-approve or perform that transition.
