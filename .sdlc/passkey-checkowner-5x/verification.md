# Verification: 将 PassKey 与 CheckOwner 资源鉴权回移至 5.x

Status: scoped-green; repository-baseline-failure

## Environment

- Branch: `codex/passkey-checkowner-5x`
- Base: `origin/5.x` at `7d8435d`
- Java: Tencent OpenJDK `1.8.0_472-b1`, class version 52
- Maven project version: `5.0.21-SNAPSHOT`
- Framework baseline: Spring Boot `2.7.18`, Spring Framework `5.3.31`, Spring Security `5.7.11`

## Acceptance coverage

- Commons tests cover supported list containers, mixed elements, cycle/depth protection, single-object exclusion, and Jackson 2 serialization (`resourceId` hidden, `passkey` visible).
- PassKey service tests cover the mainline-compatible golden token, issue/verify, identity and resource binding, tampering, exact expiry, invalid configuration/input, ambiguity boundaries, and concurrent use.
- Request resolver tests cover Header/query success, duplicate/conflicting/invalid values, form-parameter exclusion, and missing request/credential rejection.
- CheckOwner tests cover pre/post modes, named/indexed/nested expressions, mixed/unknown expression rejection, anonymous users, disabled/missing service, ADMIN/ROOT non-bypass, JDK/CGLIB/interface/bridge methods, PMS composition, and method execution counters.
- MVC and Web tests cover list signing, Result/PageVO/ResponseEntity, mixed and single objects, wrapper exclusions, disabled behavior, missing issuer, HTTP 403/A0312, and ordinary exception compatibility.
- Auto-configuration tests cover default/explicit disablement, valid and invalid enablement, replaceable service/Clock beans, and infrastructure Advisor registration.

## Commands and results

1. Build-stage clean test compilation with the Java 8 toolchain:
   `mvn -pl macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-web -am clean test-compile -DskipTests`
   Result: PASS.
2. First targeted test run exposed one 5.x-specific ordering defect: Spring Security 5.7 method security and CheckOwner both used lowest precedence, so CheckOwner could run before PMS. The implementation now sets method security to `Ordered.LOWEST_PRECEDENCE - 1`; the affected `CheckOwnerTest` rerun passed 9/9.
3. Targeted PassKey/CheckOwner regression suite:
   `mvn -pl macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-web -am test -Dtest=PasskeyResourcesTest,ResultModelJsonTest,PasskeyServiceTest,PasskeyRequestResolverTest,CheckOwnerTest,SecurityAutoConfigurationTest,PasskeyMvcTest,ControllerResponseAdviceTest,WebAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false`
   Result: PASS, 37 tests across Commons/Web/Security, 0 failures, 0 errors, 0 skipped.
4. Target modules and dependencies:
   `mvn -pl macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-web -am test`
   Result: PASS, 38 tests, 0 failures, 0 errors, 0 skipped. The extra test is the existing Commons `DateTimeTest`.
5. Full repository unit tests:
   `mvn test`
   Result: FAIL in unchanged `macula-boot-starter-mybatis-plus`; Commons, Web, and Security passed before the reactor stopped. `UserMapperTest` expected five rows but observed seven, then `selectOne` observed three matching rows: 7 tests, 2 failures, 1 error.
6. Isolation check for the failing unchanged test:
   `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am test -Dtest=UserMapperTest -Dsurefire.failIfNoSpecifiedTests=false`
   Result: PASS, 5 tests, 0 failures, 0 errors, 0 skipped. In the full module run, `UserSeqMapperTest` shares the H2 `T_USER` table/context and inserts two rows before `UserMapperTest`; `git diff origin/5.x -- macula-boot-starters/macula-boot-starter-mybatis-plus` is empty. This is an existing test-isolation failure outside the accepted change scope.
7. Clean unit and integration verification:
   `mvn clean verify`
   Result: FAIL at the same unchanged MyBatis tests after clean compilation; Commons, Web, and Security passed. Later reactor modules and Failsafe integration tests were not reached.
8. Root Checkstyle:
   `mvn -N checkstyle:check`
   Result: PASS, 0 violations.
9. Whitespace validation:
   `git diff --check`
   Result: PASS, no output.

## Evidence summary

- Change-scoped verification is green on a real Java 8 runtime: all 37 new/regression tests pass, and all 38 tests in the affected reactor slice pass.
- Clean compilation of production and test code succeeds with Java 8.
- Full-repository `test` and `clean verify` are not green because of a reproducible, unchanged MyBatis test-isolation defect. No production or test file in that module is changed by this branch.
- No external Redis, Kafka, RocketMQ, database, cloud service, deployment, or real-client verification was performed. The applicable tests use unit/ApplicationContext/MockMvc and in-memory H2 coverage.
- No evaluation suite applies: this change does not modify agent instructions, skills, hooks, review policy, or other configuration-artifact behavior.

## Conclusion

The PassKey and CheckOwner backport satisfies its scoped Java 8/Spring Boot 2.7.18 verification. Repository-wide verification remains blocked by the documented pre-existing MyBatis test-order isolation failure, so this record does not claim that the complete repository verification is green.
