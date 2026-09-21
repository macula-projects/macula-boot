# Intent: Establish the 6.0 maintenance line and advance main to 6.1
Author: Rain Wang. Status: accepted.

## Problem
The current `main` branch still represents the Macula Boot 6.0 line and uses the Spring Boot 3.5 / Spring Cloud 2025.0 dependency train. There is no dedicated `6.0.x` maintenance branch preserving that state, so advancing `main` to the Spring Boot 4 / Spring Cloud 2025.1 generation would leave the 6.0 line without a stable maintenance target.

## Proposed outcome
The current accepted `main` state is available as the `6.0.x` maintenance line. The continuing `main` line identifies itself as `6.1.0-SNAPSHOT` and uses a mutually compatible Spring Boot 4.x, Spring Cloud `2025.1.1`, Spring Cloud Tencent `2.1.2.0-2025.1.1`, and Spring Cloud Alibaba `2025.1.0.0` stack. Maintainers can build and test both release lines independently without changing the existing `5.x` line.

## Affected users and systems
Macula Boot maintainers, downstream users selecting the 5.x, 6.0.x, or main/6.1 development lines, the Maven reactor and dependency-management BOM, Spring Cloud Alibaba and Tencent starters and examples, version-bearing documentation and archetype templates, CI verification, and the remote Git branch structure are affected.

## Constraints
Create `6.0.x` from the current accepted `main` commit `fbd20cfd4020734880ff4cc41f7cec95a0bfd70e` before advancing `main`. Do not modify or merge into the existing `5.x` branch. Keep Java 17 compatibility unless an upstream dependency makes that impossible and the originator explicitly approves a change. Treat the Spring Boot 4 / Spring Cloud 2025.1 transition as a compatibility migration rather than changing only two BOM properties. Keep project-version declarations, documentation, examples, and archetype output consistent with `6.1.0-SNAPSHOT`. Verify affected starters and examples and then run the repository-wide Maven verification required for dependency-management changes. Do not release, tag, or deploy artifacts as part of this change.

## Open questions
Which stable Spring Boot 4.0.x patch should `main` target alongside Spring Cloud `2025.1.1`? Which additional dependency or source compatibility changes will be required by Spring Framework 7, Jackson 3, Spring Cloud Gateway 5, and the Alibaba removal of bootstrap configuration support? These questions require design-time dependency resolution and compatibility analysis before an implementation plan can be approved.
