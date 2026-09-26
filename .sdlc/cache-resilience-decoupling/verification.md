# Verification Report

Date: 2026-09-26

Change: `cache-resilience-decoupling`

Change under test: branch `feat/cache-remove-resilience4j`, accepted plan commit `98e51d14b868431308adbcaa5f516d946359944b` plus the uncommitted implementation.

Environment: macOS, Java 17.0.17, Maven 3.9.6, Redis on `localhost:6379`.

## Result

`Verification is green`.

Cache Starter 的聚焦单元测试、自动配置测试、Redis 集成测试、依赖树、配置元数据、Checkstyle 和差异检查均通过。经人工同意修复升级提交引入的 Maven 回归后，目标 Reactor 与全仓 54 个模块均验证通过。

## Focused module verification

Commands executed after the final source correction:

```shell
redis-cli -h localhost -p 6379 ping
mvn -f macula-boot-starters/macula-boot-starter-cache/pom.xml clean verify -Dit.test=RedisCacheIT -Dfailsafe.failIfNoSpecifiedTests=false
mvn -q -f macula-boot-starters/macula-boot-starter-cache/pom.xml failsafe:integration-test failsafe:verify -Dit.test=RedisCacheIT -Dfailsafe.failIfNoSpecifiedTests=false
mvn -N checkstyle:check
mvn -f macula-boot-starters/macula-boot-starter-cache/pom.xml dependency:tree -Dincludes=io.github.resilience4j -Dverbose
git diff --check
mvn -pl macula-boot-starters/macula-boot-starter-cache -am test
mvn clean verify
```

Actual results:

```text
Redis ping                 PONG
Surefire                   17 tests, 0 failures, 0 errors, 0 skipped
Failsafe                    2 tests, 0 failures, 0 errors, 0 skipped
Repeated Redis IT           3 additional runs, all successful
Checkstyle                 0 violations, BUILD SUCCESS
Resilience4j dependency    no matching dependency rows, BUILD SUCCESS
git diff --check           no errors
Cache target reactor       4/4 modules successful, BUILD SUCCESS
Full reactor               54/54 modules successful, BUILD SUCCESS
Full test reports          217 tests, 0 failures, 0 errors, 3 skipped
```

The 17 unit tests include 14 `TwoLevelCacheTest` cases and 3 `TwoLevelCacheAutoConfigurationTest` cases. They cover local and Redis hits, loader single execution, availability-failure degradation, unknown-exception propagation, local write fallback (including propagated Redis write failures), eviction/clear behavior, immediate `evictIfPresent`/`invalidate` behavior when Redis has or lacks the entry, Pub/Sub failure, auto-configuration conditions, and enabled startup without Resilience4j classes.

The Redis integration tests cover real serialization, Redis-to-Caffeine backfill, write, immediate eviction, and immediate invalidation. During verification, an initial assertion treated Spring Cache's potentially deferred `evict`/`clear` API as immediate. The implementation and tests now cover the dedicated immediate APIs, `evictIfPresent` and `invalidate`, and ensure that these operations also invalidate the local Caffeine tier.

## Static proof

The generated `spring-configuration-metadata.json` contains no `open-circuit-breaker` or `circuit-breaker` property. Production Cache Starter source, its POM, and the parent POM contain no Resilience4j or CircuitBreaker reference. Remaining textual references are limited to migration documentation and the classpath-filtering test that proves the starter starts without Resilience4j.

## Maven corrections made during verification

The first Reactor run exposed regressions introduced by dependency-upgrade commit `40030fa`:

- Corrected the misspelled `macuala-boot-starter-async` dependency-management coordinate.
- Restored dependency management for `macula-boot-starter-observability` and `opentelemetry-logback-appender-1.0` at `2.21.0-alpha`, aligned with OpenTelemetry SDK `1.55.0` managed by Spring Boot 4.0.8.
- Retained the working-tree correction from `liteflow-spring-boot-starter` to the managed Boot 4 artifact `liteflow-spring-boot4-starter`.
- Removed stale parent-POM management for retired Logstash, Sleuth, and SkyWalking starters and libraries; repository search found no remaining implementation modules using them.
- Kept MyBatis-Plus `3.5.17` and migrated `IService`/`ServiceImpl` imports from `com.baomidou.mybatisplus.extension.service` to `com.baomidou.mybatisplus.spring.service` in tests, Archetype sources, and Seata README examples.
- Restored SnailJob from `2.0.2` to `1.9.0`: version `2.0.2` is compiled for Java 21 (class version 65), while the repository baseline is Java 17 (class version 61); `1.9.0` is class version 61 and preserves the existing API.

After these corrections, `mvn clean verify` completed all 54 modules successfully. The three skipped integration tests are environment-dependent tests already configured to skip when their external middleware is unavailable.

## Review corrections

The first three-pass review found two Important issues and one test-coverage Nit. All were corrected before the final verification:

- Successful loader results now always populate the local cache, even when a non-degradable Redis write exception is propagated.
- Successful Redis `evictIfPresent` and `invalidate` calls now publish invalidation messages even when Redis reports no matching entry, so other nodes cannot retain local-only values.
- The Resilience4j-free startup test now exercises the enabled Redis cache path rather than disabling Spring Cache.
- The final review Nit was closed by aligning the LiteFlow README dependency example with the Boot 4 artifact already used by its POM.

After the documentation correction, `mvn -q clean verify` was run again and completed successfully with exit code 0.

## Handoff

The implementation and expanded Maven compatibility corrections are verified and ready for the human-gated `sdlc-deploy` review pass. No merge, release, deployment, or review handoff was performed by this report.
