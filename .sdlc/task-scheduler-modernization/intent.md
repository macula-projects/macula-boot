# Intent: 任务调度能力整理与 SnailJob 统一执行
Author: Rain. Status: accepted.

## Problem
当前 `macula-boot-starter-task` 的能力说明与实际依赖不一致：README 仍包含 PowerJob，SnailJob 的接入方式没有形成清晰、完整且与当前兼容版本一致的使用契约。已有业务大量使用 `@XxlJob`、`XxlJobHelper` 等 XXL-JOB 客户端代码，如果直接改用 SnailJob Server，就必须改写任务代码或继续维护 XXL-JOB Admin，无法用一套服务端统一承载新旧任务。

## Proposed outcome
`macula-boot-starter-task` 对外兼容 XXL-JOB 与 SnailJob 两种客户端编程方式，彻底移除 PowerJob，并统一使用 SnailJob Server 提供调度服务。业务应用只需引入该 Starter 并配置 SnailJob：新的 SnailJob Handler 可原生执行，现有基于 `@XxlJob` 编写的任务代码无需改写，也能由 SnailJob 调度和执行；运行环境不再需要 XXL-JOB Admin。

## Affected users and systems
受影响对象包括使用 `macula-boot-starter-task` 的业务开发者、已有 XXL-JOB Handler 的应用、新增 SnailJob Handler 的应用、统一部署的 SnailJob Server，以及该 Starter 的依赖管理、兼容适配、自动配置、示例、测试和 README 文档。

## Constraints
- 保持 Java 17；SnailJob 必须保留现有 `1.9.0`，不得升级到要求 JDK 21 的 `2.0.0`。
- 只保留 SnailJob Server，不要求部署或连接 XXL-JOB Admin。
- 同时兼容 SnailJob 原生客户端代码与 XXL-JOB 客户端代码。
- 现有 `@XxlJob` Handler 及其常用 `XxlJobHelper` 参数、分片、日志和执行结果用法应无需修改，并由 SnailJob 执行。
- 使用方只引入 `macula-boot-starter-task`，不应再手工补充 XXL-JOB 或 SnailJob 客户端核心依赖。
- 移除 PowerJob 的依赖、配置、示例和文档残留。
- SnailJob 配置说明应以兼容 Java 17 的 `1.9.0` 官方配置为准。
- README、示例、自动配置和测试应保持一致。
- 保持 Spring Boot 4.0 及仓库现有模块约束。

## Open questions
无。
