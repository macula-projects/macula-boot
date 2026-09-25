# Intent: 统一可观测性方案
Author: rainsoft. Status: accepted.

## Problem
当前 Metrics、日志与链路追踪能力分散在 `macula-boot-starter-prometheus`、`macula-boot-starter-logstash`、`macula-boot-starter-sleuth` 和 `macula-boot-starter-skywalking` 中。旧 Starter 的职责、配置方式和技术路线不统一，应用需要分别选择和组合依赖，网关、示例及项目骨架也保留了旧方案的直接耦合，导致接入、升级和运维标准难以保持一致。

## Proposed outcome
Spring Boot 4 应用可以通过一套统一入口获得指标采集、结构化日志、Trace ID 与 Span ID 日志关联、跨服务链路上下文传播和统一 OTLP 导出能力。旧的 Prometheus、Logstash、Sleuth、SkyWalking Starter 被清理，使用方能够依据升级指南完成接入与迁移，并能通过自动化测试和示例验证三类信号的一致关联。端到端验证覆盖 OpenTelemetry Collector、Prometheus、Loki 和 Tempo。

## Affected users and systems
受影响用户包括使用 Macula Boot 6.1 构建和运维微服务的开发者、平台团队及运维人员。受影响系统包括可观测性相关 Starter、依赖管理、云网关、示例应用、Archetype 项目骨架、根项目文档，以及依赖四个旧 Starter 的下游应用。示例应用与 Archetype 在同一变更中完成迁移，不保留旧方案示例。`macula-boot-starter-auditlog` 和 `macula-boot-starter-operationlog` 继续作为业务审计与操作日志能力保留。

## Constraints
方案面向 Macula Boot 6.1、Java 17、Spring Boot 4.0 和 Spring Cloud 2025.1。6.1 允许破坏性升级，不要求为四个旧 Starter 保留兼容层。统一能力必须覆盖 Metrics、结构化日志、Trace/Span ID 关联、跨服务传播与 OTLP 导出；三类信号统一采用 OTLP，不再将 Prometheus Pull 作为框架标准输出。不能削弱现有审计日志和操作日志能力。依赖版本继续由 `macula-boot-parent` 集中管理，Starter 应保持可配置、可禁用和可被用户自定义实现覆盖。必须提供四个旧 Starter 到统一入口的依赖及配置迁移对照。

## Open questions
无。
