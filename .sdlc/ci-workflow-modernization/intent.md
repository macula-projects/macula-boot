# Intent: Modernize CI actions and gate Snapshot publication
Author: Rain. Status: accepted.

## Problem
The repository's GitHub Actions workflows use inconsistent and, in the Snapshot and release paths, deprecated action generations. Snapshot publication also starts in parallel with verification after a push to `main`, so an unverified merged commit can begin publishing artifacts before its Maven and Checkstyle checks finish.

## Proposed outcome
Pull requests and the actual merged `main` commit are both verified with supported GitHub Actions. Snapshot artifacts are published only for `main` commits whose verification has completed successfully. Existing manual release behavior remains available without deprecated action warnings.

## Affected users and systems
Repository maintainers, pull-request contributors, downstream Snapshot consumers, GitHub Actions workflows for verification, Snapshot publication, and release, Maven Central credentials, and the Sonatype Snapshot repository are affected.

## Constraints
Keep Java 17 and the existing Maven verification and deployment semantics. Preserve verification for both pull requests and pushes to `main`. Do not expose Maven Central or GPG credentials to pull-request jobs. Do not trigger a release, tag, merge, or artifact publication while implementing or testing this change. Use currently supported stable GitHub-maintained action generations compatible with `ubuntu-latest`.

## Open questions
None.
