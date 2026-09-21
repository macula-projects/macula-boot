# Spec: Establish the 6.0 maintenance line and advance main to 6.1 (from intent.md 2026-09-21)
Status: accepted

## Source intent
[Accepted intent](intent.md): preserve the current 6.0 state as `6.0.x`, then advance `main` to Macula Boot 6.1 on the Spring Boot 4 and Spring Cloud 2025.1 generation without changing `5.x`.

## Requirements
1. Create local and remote branch `6.0.x` at exactly `fbd20cfd4020734880ff4cc41f7cec95a0bfd70e` before any 6.1 implementation commit is added to `main`; leave `5.x` unchanged.
2. Set the Maven reactor revision to `6.1.0-SNAPSHOT` everywhere the maintained source tree, documentation, and generated archetype output declare the Macula Boot development version; no maintained file may continue to advertise `6.0.1-SNAPSHOT` on `main`.
3. Set the `main` dependency baseline to Spring Boot `4.0.8`, Spring Cloud `2025.1.1`, Spring Cloud Tencent `2.1.2.0-2025.1.1`, and Spring Cloud Alibaba `2025.1.0.0`, with dependency management continuing to be centralized in `macula-boot-parent/pom.xml`.
4. Preserve Java 17 source and runtime compatibility across the Maven reactor.
5. Resolve every compile-time and test-time incompatibility introduced by Spring Framework 7, Spring Boot 4, Spring Cloud 2025.1, Spring Cloud Gateway 5, Jackson 3, and the upgraded Alibaba and Tencent dependencies without introducing unrelated features.
6. Replace Alibaba configuration bootstrap behavior that is no longer supported by Spring Cloud Alibaba 2025.1 with its supported `spring.config.import`-based contract. Update affected examples, archetype templates, Starter dependencies, default environment overrides, and documentation together so generated and checked-in applications use the same configuration model.
7. Keep Alibaba and Tencent discovery, configuration, gateway routing, Sentinel, authentication, and Docker Compose example behavior equivalent to the accepted 6.0 behavior except where an upstream 2025.1 contract requires an explicitly documented migration.
8. Preserve Starter override and disable semantics, conditional bean creation, `AutoConfiguration.imports` discovery, public configuration prefixes, and public APIs unless an upstream incompatibility makes a change unavoidable. Every unavoidable user-facing change must have migration documentation and regression coverage.
9. Update the root README, affected Starter and example READMEs, and archetype resources so dependency versions, configuration examples, generated parent versions, build commands, and migration guidance agree with the implemented 6.1 behavior.
10. Verify that all four requested BOMs resolve from configured public repositories, inspect the effective Maven model for the exact selected versions, run focused cloud/web Starter and example tests, run `mvn clean verify`, and validate the deploy/flatten build with `mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy`.
11. Run representative Alibaba and Tencent application-context or runtime smoke checks against the repository's Docker Compose middleware where local infrastructure permits. Record every external-service or platform check that cannot run instead of treating it as passed.
12. Do not release artifacts, create a release tag, run `release.sh`, merge a remediation, or deploy applications as part of this change.

## Non-goals
This change does not modify or merge into `5.x`; backport 6.1 dependencies to `6.0.x`; add product features; redesign public APIs beyond changes forced by the platform migration; change Java beyond version 17; publish a 6.0 or 6.1 release; configure production infrastructure; or claim production readiness from compile-only evidence.

## Design
The branch transition has two ordered boundaries. First, `6.0.x` is created and published at the immutable pre-upgrade commit recorded in the accepted intent. Second, all 6.1 work continues only on `main`, so the maintenance line cannot accidentally inherit Boot 4 changes.

The 6.1 dependency baseline remains owned by the root reactor and `macula-boot-parent`. The root revision, parent revision, Spring parent, Spring Cloud BOM, Alibaba BOM, Tencent BOM, documentation, and archetype parent references move as one coherent version set. Child modules continue to consume dependency management rather than adding local versions.

Compatibility work proceeds from the dependency boundary inward: resolve the effective BOM graph; compile the reactor to expose removed or relocated APIs; migrate shared web, serialization, gateway, and auto-configuration code; migrate vendor-specific Starters; then update examples and archetype templates. Compiler or runtime evidence determines whether a Jackson 2 compatibility path is retained or code moves to Jackson 3 APIs; the implementation must not perform a cosmetic repository-wide namespace rewrite without evidence.

Alibaba configuration moves away from legacy bootstrap loading to the 2025.1-supported Config Data import flow. Source examples and archetype templates must express equivalent application names, namespaces, addresses, credentials, refresh behavior, and environment-variable overrides through supported configuration. Tencent configuration may retain supported behavior, but shared bootstrap dependencies must not be kept solely to preserve the removed Alibaba path.

Governing policies consulted:

- `AGENTS.md`: Java 17 compatibility, focused module changes, documentation and test synchronization, and no unsolicited release or push.
- `.agents/rules/architecture.md`: module ownership, cloud-vendor isolation, public API review, and archetype consistency.
- `.agents/rules/dependencies-release.md`: centralized BOM management, compatibility-matrix checks, full verification, deploy-profile validation, and release safety.
- `.agents/rules/starter-development.md`: conditional and replaceable auto-configuration, dependency hygiene, configuration documentation, and auto-configuration discovery tests.
- `.agents/rules/testing.md`: unit/integration separation, verification order, external-infrastructure disclosure, and result reporting.
- `REVIEW.md`: independent Bugs, Security, and Compliance review passes with evidence-backed findings.
- `sdlc-design`: accepted-intent entry gate, explicit concerns, reviewer approval, and no implementation during design.

No separate organization brand, data-classification, regulatory, or security-design policy was found in the repository. Existing repository security and review rules still apply.

## Data and interfaces
No database schema, persistence format, HTTP endpoint, or message schema change is intended. The Maven dependency contract changes from the Boot 3.5 / Cloud 2025.0 generation to Boot 4.0 / Cloud 2025.1, which is externally visible to downstream applications.

The application configuration contract changes for Alibaba users: legacy `bootstrap.yml`-driven Nacos configuration must be represented through supported Config Data imports and documented properties. Existing environment-variable names and local Docker Compose connectivity should remain stable where the upstream libraries permit it. Any changed property, removed compatibility dependency, Java type, serialized representation, gateway behavior, or generated archetype file is a user-facing interface change and must be listed in migration documentation.

The Git interface gains remote maintenance branch `6.0.x` at the exact accepted commit. No tag or release artifact is created.

## Flagged concerns
- Spring Boot patch selection: Maven Central lists `4.0.8` as the newest stable 4.0.x release at design time, while Spring Cloud `2025.1.1` records Boot `4.0.2` in its BOM metadata; the proposed target is `4.0.8`, and the reviewer must approve that newer compatible patch rather than the BOM baseline, blocking.
- Platform migration breadth: Boot 4 and Framework 7 introduce breaking changes including Jackson 3 and Gateway 5, so accepting this spec authorizes compatibility changes across affected commons, Starters, examples, tests, documentation, and archetype sources while prohibiting unrelated refactors, blocking.
- Alibaba bootstrap removal: Spring Cloud Alibaba 2025.1 removes its legacy bootstrap configuration path, while this repository currently has four cloud Starters depending on `spring-cloud-starter-bootstrap` and thirteen source-side `bootstrap.yml` files; the reviewer must accept migration of this configuration contract, blocking.
- Third-party readiness: non-Spring dependencies managed by the project may expose Boot 3, Framework 6, Jackson 2, or `javax` assumptions; the exact required upgrades cannot be known until dependency resolution and compilation, and any expansion beyond compatibility-only changes must return to the reviewer, non-blocking.
- Downstream source compatibility: `6.1.0-SNAPSHOT` is a minor project version but adopts a major Spring platform generation, so some downstream recompilation or migration may be unavoidable and must be documented rather than claimed backward compatible, non-blocking.
- Runtime coverage: local Docker smoke tests cannot prove every supported operating system, architecture, or external cloud-service integration; unexecuted environments must remain explicit verification gaps, non-blocking.
- Branch protection: creating and pushing `6.0.x` does not itself prove that GitHub branch-protection rules cover the new maintenance branch; repository administration may need a separate human action, non-blocking.

## Verification strategy
Before source migration, verify with Git and the remote that `6.0.x` resolves exactly to `fbd20cfd4020734880ff4cc41f7cec95a0bfd70e` and that `5.x` has not moved. Resolve the four target BOM coordinates from Maven Central and capture the effective parent/dependency-management versions.

Use repository searches to ensure maintained `main` sources contain `6.1.0-SNAPSHOT` consistently and no stale `6.0.1-SNAPSHOT` declaration remains. Inspect generated archetype content as well as its source templates. Compare effective POM output for representative Alibaba, Tencent, Gateway, Web, and archetype modules.

Run focused tests first for modules changed by compiler or configuration migration, including auto-configuration enable/disable and user-override paths. Then run `mvn clean verify`, `mvn -N checkstyle:check`, and `mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy`. Report test totals and distinguish unit tests, integration tests, disabled external-infrastructure tests, and genuine failures.

For configuration and runtime behavior, render the Docker Compose profiles, build the representative application images, start Alibaba and Tencent middleware/application paths where feasible, verify health and gateway/provider/consumer flows, and inspect logs for configuration-import, serialization, and classpath failures. Validate an archetype-generated project at least through Maven model resolution and compilation. Record any environment that cannot be exercised.

Finally, run the `REVIEW.md` Bugs, Security, and Compliance passes against the complete diff and immediate behavior. Treat unresolved dependency vulnerabilities, secret exposure, unsafe configuration fallback, broken public flows, or undocumented compatibility breaks as merge blockers.
