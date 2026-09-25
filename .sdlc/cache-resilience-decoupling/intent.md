# Intent: Cache Starter 移除 Resilience4j 耦合
Author: Rain. Status: accepted.

## Problem
`macula-boot-starter-cache` 将 Resilience4j 熔断器直接暴露在依赖、配置属性和公共构造路径中，使缓存 Starter 与通用容错框架强耦合。当前熔断能力不能限制单次 Redis 调用耗时，也没有覆盖所有 Redis 访问路径；Redis 故障时读、写、删除操作的降级行为不一致，README 描述与实际行为也存在偏差。使用方因此需要承担额外依赖和配置复杂度，却无法获得清晰、可预测的缓存故障语义。

## Proposed outcome
Cache Starter 不再要求应用引入 Resilience4j。Redis 正常时继续提供现有的 Caffeine 与 Redis 两级缓存能力；Redis 暂时不可用时，缓存访问具备清晰且一致的行为，不因缓存基础设施故障意外中断核心业务，并能通过日志或指标观察降级情况。缓存加载器不会因为 Redis 异常被重复执行，模块文档、配置元数据与实际行为保持一致。

## Affected users and systems
受影响对象包括使用 `macula-boot-starter-cache` 的应用、两级缓存自动配置、Caffeine 本地缓存、Redis 二级缓存、Redis Pub/Sub 本地缓存失效通知，以及父 POM 中统一管理的依赖版本。已经配置 `spring.cache.two-level.open-circuit-breaker` 或 `spring.cache.two-level.circuit-breaker.*` 的应用需要迁移说明。

## Constraints
保持 Java 17、Spring Boot 4.0 和 Spring Cloud 2025.1 基线；不能用另一个通用熔断框架替换现有耦合；Redis 正常时不能破坏既有两级缓存和跨实例失效通知；Redis 故障不能导致缓存加载器重复执行；需要区分基础设施不可用与序列化、类型、配置等程序错误；依赖版本仍由 `macula-boot-parent` 统一管理；保留当前工作区中与依赖升级相关的既有修改。

## Open questions
Redis 写入、删除或清空失败时，是否统一采用 fail-open 策略，即先保证当前实例的本地缓存更新或失效，并允许跨实例一致性暂时降级？旧的熔断配置键是直接删除并在迁移文档中说明，还是保留一个版本但标记为废弃且不再生效？是否要求本次同时增加 Redis 降级次数的 Micrometer 指标？
