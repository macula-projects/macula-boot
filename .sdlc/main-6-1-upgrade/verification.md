# Verification Report

Date: 2026-09-22

Change: `main-6-1-upgrade`

Baseline: `fbd20cfd4020734880ff4cc41f7cec95a0bfd70e`

Change under test: branch `feat/main-6-1-upgrade`, commit `0e9b320583a52aff5378b5b9322b6e048f3e067c` plus the Stage 4 corrections listed below.

Environment: macOS, Java 17.0.17, Maven 3.9.6, OrbStack Docker Engine 29.4.0 on `linux/arm64`, Docker Compose v5.1.2.

## Result

`Verification is green`.

Verified platform:

```text
Macula Boot revision       6.1.0-SNAPSHOT
Spring Boot               4.0.8
Spring Cloud              2025.1.1
Spring Cloud Alibaba      2025.1.0.0
Spring Cloud Tencent      2.1.2.0-2025.1.1
Springdoc                 3.1.1
Redisson                  4.7.0
Java                      17
```

No release, tag, merge, production deployment, or `5.x` modification was performed. Native Windows and `linux/amd64` runtime were not executed; runtime evidence is macOS/ARM64 only.

## Source and branch proof

```text
branch=feat/main-6-1-upgrade
HEAD=0e9b320583a52aff5378b5b9322b6e048f3e067c
6.0.x=fbd20cfd4020734880ff4cc41f7cec95a0bfd70e
origin/6.0.x=fbd20cfd4020734880ff4cc41f7cec95a0bfd70e
local 5.x=beaa74b1639786306aa817bef0e522a23c995fd4
origin/5.x=7d8435df4d0edd59ddb6a440fc282b81fab3246d
```

The local `5.x` branch was already stale relative to `origin/5.x` and was left untouched. `git diff --check` completed without errors. Searches found no maintained `6.0.1-SNAPSHOT`, archetype `5.0.0`, Springdoc `2.8.14`, Redisson `3.52.0`, or `redisson-spring-data-35` references.

## Maven verification

Commands executed after the final source correction:

```shell
mvn clean verify
mvn -N checkstyle:check
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy
```

Actual results:

```text
clean verify reactor       57/57 modules successful, BUILD SUCCESS, 01:21 min
Surefire                   133 tests, 0 failures, 0 errors, 0 skipped
Failsafe                    48 tests, 0 failures, 0 errors, 3 skipped
Combined                   181 tests, 0 failures, 0 errors, 3 skipped
Checkstyle                 0 violations, BUILD SUCCESS
deploy-profile install     57/57 modules successful, BUILD SUCCESS, 42.030 s
```

The three skipped integration tests require services not started by the Maven reactor:

```text
KafkaProducerIT
OrderServiceIT (RocketMQ)
TinyIdClientIT
```

Focused correction proof also passed for Springdoc OpenAPI/Swagger UI (2 tests), Redis/Redisson (8 tests), and leader election (1 integration test). The final dependency trees contained:

```text
org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1
org.springdoc:springdoc-openapi-starter-webmvc-api:3.1.1
org.springdoc:springdoc-openapi-starter-common:3.1.1
org.redisson:redisson-spring-data-40:4.7.0
org.springframework.data:spring-data-redis:4.0.7
```

Tencent provider, consumer, and gateway dependency checks contained neither `spring-cloud-starter-tencent-polaris-contract` nor a transitive Springdoc 2 artifact.

## Archetype and configuration proof

The installed `dev.macula.boot:macula-boot-archetype:6.1.0-SNAPSHOT` generated a fresh project at:

```text
/var/folders/bh/4nmzcd6n0dx7zgq1hq6g_d080000gp/T/tmp.oFligMWlTC/stage4-sample
```

Generation succeeded and `mvn compile` reported all 11 generated modules successful. The six generated application modules produced `target/classes/application.yml` with `spring.profiles.active: local`; none retained the Maven token `@profile.active@`.

Compose rendering succeeded for all supported selections:

```shell
docker compose --profile alibaba config --quiet
docker compose --profile tencent config --quiet
docker compose --profile alibaba --profile tencent config --quiet
```

## Docker runtime proof

All six application images rebuilt successfully from `bellsoft/liberica-runtime-container:jre-17.0.20.1_1-glibc`. Runtime used host-only port overrides because local defaults were occupied:

```text
MYSQL_PORT=13306
REDIS_PORT=16379
ALIBABA_GATEWAY_HTTP_PORT=15000
```

MySQL, Redis, Nacos, Polaris, and all six Alibaba/Tencent application containers reached healthy state; `nacos-init` exited successfully.

Real routed requests succeeded:

```text
Alibaba: HTTP 200, Hello final, test=nacos-initial ... health
Tencent: HTTP 200, Hello consumer ...
OpenAPI /v3/api-docs: HTTP 200
Swagger UI /swagger-ui/index.html: HTTP 200
```

Nacos Config Data supplied `example.test=nacos-initial`, overriding the local value. Publishing `example.test=nacos-stage4-refreshed` through the Nacos API changed the live provider response after 2 seconds without restart. The value was restored to `nacos-initial` afterward.

The final application-log scan returned no matches for configuration placeholder failures, Nacos authentication failures, `APPLICATION FAILED`, `NoClassDefFoundError`, `NoSuchMethodError`, or `UnsatisfiedDependencyException`. Verification containers were stopped with normal `docker compose down`; named volumes were retained.

## Corrections made during this verification

The first renewed runtime attempt exposed a hidden incompatibility in `spring-cloud-starter-tencent-polaris-contract:2.1.2.0-2025.1.1`: it requests Springdoc 2.8.14. With the platform aligned on Springdoc 3.1.1, Tencent Gateway failed startup because `ObjectMapperProvider` was unavailable, while provider and consumer logged background `NoSuchMethodError` failures during contract reporting.

Both Macula Tencent Starters now exclude only that incompatible contract sub-starter. Discovery, configuration, routing, and the remaining `tencent-all` capabilities remain enabled. The Tencent README documents how contract reporting can be explicitly restored after Spring Cloud Tencent provides Springdoc 3 compatibility. Rebuilt containers, dependency-tree checks, clean application logs, and repeated routed requests prove the correction.

The stale-version search also corrected the archetype README from version `5.0.0` to `6.1.0-SNAPSHOT`.

## Protected configuration and evals

Compared with the accepted baseline, `AGENTS.md` changes only the project description from Boot 3.5/Cloud 2025 to Boot 4.0/Cloud 2025.1. `CLAUDE.md` remains its symlink view. No workflow policy, skill, or hook changed. Repository search found no versioned eval suite guarding this descriptive change, so no applicable eval command exists.

## Handoff

Stage 4 evidence is complete. The change is ready for the human-gated `sdlc-deploy` review pass; this report does not self-approve review, merge, release, or deployment.
