# Intent: 任务调度能力整理与 SnailJob 迁移支持
Author: Rain. Status: accepted.

## Problem
当前 `macula-boot-starter-task` 的能力说明与实际依赖不一致：README 仍包含 PowerJob，SnailJob 的接入方式没有在 Starter 中形成清晰、完整且与最新版本一致的使用契约。业务应用若希望从 XXL-JOB 迁移到 SnailJob，仍需自行判断依赖与配置方式，同时还要担心既有 XXL-JOB 任务代码受到影响。

## Proposed outcome
`macula-boot-starter-task` 对外明确支持 XXL-JOB 和 SnailJob，彻底移除 PowerJob 相关内容，并将 SnailJob 作为推荐的新任务调度方案。业务应用只需引入该 Starter，即可按所选调度平台的最新配置完成接入；现有基于 XXL-JOB 编写的任务代码继续兼容，无需为本次整理而改写。

## Affected users and systems
受影响对象包括使用 `macula-boot-starter-task` 的业务开发者、已有 XXL-JOB 任务的应用、计划采用或迁移至 SnailJob 的应用，以及该 Starter 的依赖管理、自动配置、示例、测试和 README 文档。

## Constraints
- 保留 XXL-JOB 支持，并尽可能保证已有 XXL-JOB 任务代码无需修改。
- SnailJob 作为推荐方案，但不能以破坏 XXL-JOB 兼容性为代价。
- 使用方只引入 `macula-boot-starter-task`，不应再手工补充 XXL-JOB 或 SnailJob 的核心依赖。
- 移除 PowerJob 的依赖、配置、示例和文档残留。
- SnailJob 与 XXL-JOB 的配置说明应以当前采用版本的最新官方配置为准。
- README、示例、自动配置和测试应保持一致。
- 保持 Java 17、Spring Boot 4.0 及仓库现有模块约束。

## Open questions
无。
