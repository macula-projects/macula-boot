# Spec: Cache Starter 移除 Resilience4j 耦合 (from intent.md 2026-09-25)
Status: accepted

## Source intent
[已接受的 intent.md](./intent.md)：Cache Starter 不再强制引入 Resilience4j，并为 Redis 暂时不可用建立一致、可观察且不会重复执行缓存加载器的降级行为。

## Requirements
1. `macula-boot-starter-cache` 的编译和运行依赖树不得包含任何 `io.github.resilience4j` 组件，父 POM 在全仓无其他使用方时同步移除对应版本属性和 dependency management 声明。
2. Redis 正常可用时，缓存读取仍按 Caffeine 本地缓存、Redis 二级缓存的顺序进行；Redis 命中结果仍回填本地缓存，正常的写入、删除、清空和 Pub/Sub 跨实例失效通知行为保持不变。
3. Redis 发生连接不可用、资源不可用或命令超时时，缓存读取应将该次 Redis 访问视为未命中；带 `valueLoader` 的读取最多执行一次 loader，并将成功结果写入当前实例的本地缓存。
4. Redis 发生上述可用性故障时，`put` 和 `putIfAbsent` 应继续维护当前实例的本地缓存，`evict` 和 `clear` 应继续使当前实例的本地缓存失效；Redis 写入及跨实例通知允许暂时失败，不得因此中断核心业务。
5. 序列化错误、类型错误、非法配置及其他非 Redis 可用性异常不得被伪装成缓存未命中或静默降级，必须继续向调用方暴露。
6. Redis Pub/Sub 发布发生可用性故障时不得改变缓存操作的业务结果；故障需要通过包含缓存名和操作类型且不包含缓存值、密钥敏感内容的日志进行观测。
7. 删除 `spring.cache.two-level.open-circuit-breaker` 和 `spring.cache.two-level.circuit-breaker.*` 配置契约，README、测试配置和生成的配置元数据不得继续声明这些键，并提供 6.1 迁移说明。
8. 对外不再暴露 Resilience4j 类型；`TwoLevelCacheProperties`、`TwoLevelCacheManager`、`TwoLevelCache` 和自动配置的构造及装配路径仅依赖本模块、Spring Cache、Spring Data Redis 和 Caffeine 类型。
9. 改动必须保持 Java 17、Spring Boot 4.0、Spring Cloud 2025.1 基线，并通过目标模块测试、Redis 集成测试和全仓验证。

## Non-goals
- 不引入 Spring Cloud CircuitBreaker、Spring Retry 或其他通用熔断/重试框架替代 Resilience4j。
- 不在本模块自行实现滑动窗口、半开状态或其他轻量熔断器。
- 不改变缓存键格式、Redis 序列化格式、默认 TTL、本地缓存容量或随机过期策略。
- 不承诺 Redis 故障期间的跨实例强一致性；无法写入 Redis 或发布失效消息时，其他实例可能保留旧值直到本地 TTL 到期。
- 本次不新增 Micrometer Redis 降级计数器；日志满足本次最低可观察性要求。
- 不调整 Redis 客户端连接、命令和读取超时的默认值，只在 README 中说明生产环境必须合理配置这些超时。

## Design
适用政策包括根 `AGENTS.md`、`.agents/rules/architecture.md`、`.agents/rules/starter-development.md`、`.agents/rules/dependencies-release.md`、`.agents/rules/testing.md`、`REVIEW.md` 和 `bands.yaml`。其中 Starter 依赖最小化、禁止泄漏第三方类型、公共 API 兼容性、配置迁移、自动配置测试以及父 POM 变更后的全仓验证直接约束本设计；未发现额外的品牌、数据合规或监管政策。

两级缓存保留“本地优先、Redis 次级”的结构，但不再包含熔断状态机。Redis 调用由 `TwoLevelCache` 内部统一的可用性边界处理：仅将 Spring Data/Spring DAO 表达的连接、资源和超时故障分类为可降级故障，其他运行时异常原样抛出。异常分类逻辑集中在单一包内实现中，避免各方法使用不一致的宽泛 `catch (Exception)`。

读取流程为：先查询 Caffeine；未命中时尝试 Redis；Redis 命中则回填 Caffeine；Redis 未命中或发生可降级故障时返回缓存未命中。`get(key, valueLoader)` 在确认两级缓存均未命中后只调用一次 loader，随后以尽力而为方式写入 Redis，并始终在 loader 成功后写入当前实例的 Caffeine。loader 自身异常仍包装为 Spring Cache 的 `ValueRetrievalException`，不得按 Redis 故障处理。

写入流程为：尝试写入 Redis，成功后尝试发布本地缓存失效消息，再更新当前实例的 Caffeine；若 Redis 发生可降级故障，则跳过无法完成的远端步骤但仍更新当前实例的 Caffeine。删除和清空流程无论 Redis 是否发生可降级故障，都必须使当前实例的 Caffeine 失效；正常情况下继续执行 Redis 删除和 Pub/Sub 通知。非可降级异常仍失败并暴露，但本地失效操作不得因异常路径被遗漏。

自动配置删除熔断器常量、Registry、事件监听及 Bean 构造逻辑。`TwoLevelCacheManager` 直接创建不带第三方容错类型的 `TwoLevelCache`。现有 `TwoLevelCacheMeterAutoConfiguration` 保持不变，本次不扩展指标模型。

## Data and interfaces
不新增持久化数据、Redis key、Redis value 或消息格式。

移除的配置接口：
- `spring.cache.two-level.open-circuit-breaker`
- `spring.cache.two-level.circuit-breaker.failure-rate-threshold`
- `spring.cache.two-level.circuit-breaker.slow-call-rate-threshold`
- `spring.cache.two-level.circuit-breaker.slow-call-duration-threshold`
- `spring.cache.two-level.circuit-breaker.sliding-window-type`
- `spring.cache.two-level.circuit-breaker.permitted-number-of-calls-in-half-open-state`
- `spring.cache.two-level.circuit-breaker.max-wait-duration-in-half-open-state`
- `spring.cache.two-level.circuit-breaker.sliding-window-size`
- `spring.cache.two-level.circuit-breaker.minimum-number-of-calls`
- `spring.cache.two-level.circuit-breaker.wait-duration-in-open-state`

移除的 Java 接口包括 `TwoLevelCacheProperties.CircuitBreakerProperties`，以及 `TwoLevelCache`、`TwoLevelCacheManager` 中包含 `CircuitBreaker` 参数或返回值的构造函数和包内测试入口。由于目标是彻底移除 Resilience4j 类型，不能在不保留该依赖的前提下维持这些签名的二进制兼容。

README 需要说明 Redis 故障时的 fail-open 行为、跨实例一致性限制、Redis 超时配置责任以及旧配置键的删除方式。

## Flagged concerns
- fail-open 一致性：Redis 写入、删除、清空和 Pub/Sub 可用性故障将不再中断核心业务，但会暂时降低跨实例一致性；需要框架负责人确认这是 Cache Starter 的统一故障策略，blocking。
- 配置与 Java API 兼容性：旧熔断配置键、嵌套属性类型及带 `CircuitBreaker` 的公共构造函数将直接删除，现有使用方需要按 6.1 迁移说明调整；需要框架负责人确认 6.1 允许该不兼容变更，blocking。
- 异常分类覆盖：Spring Data Redis 通常将连接和超时错误翻译为 Spring DAO 异常，但不同 Redis 驱动或未来版本可能出现未覆盖的底层异常；首版采用明确白名单并让未知异常失败，避免静默掩盖程序错误，non-blocking。
- 故障期间延迟：移除熔断后，每个本地未命中的请求都可能等待 Redis 客户端超时，高基数访问在 Redis 故障期间可能放大延迟和后端加载压力；本次通过文档要求合理配置 Redis 超时，不新增框架级快速失败机制，non-blocking。
- Micrometer 指标：本次只提供安全日志而不增加 Redis 降级计数器，能够交付但降低聚合监控能力；是否追加指标可作为后续增强，non-blocking。

## Verification strategy
1. 新增不连接外部 Redis 的单元测试，使用可控的 Redis writer/template 替身覆盖本地命中、Redis 命中、连接故障、超时故障和非可降级异常。
2. 验证 `get(key, valueLoader)` 在 Redis 故障时 loader 只执行一次，loader 异常不会被重试或吞掉。
3. 验证 Redis 可用性故障时 `put`、`putIfAbsent`、`evict` 和 `clear` 的本地缓存结果符合要求，并验证 Pub/Sub 故障不改变业务结果。
4. 使用 `ApplicationContextRunner` 验证 Cache 自动配置可发现、条件不满足时不装配，并确认不再创建或要求 CircuitBreaker 类型。
5. 更新现有 `RedisCacheIT` 与测试 `application.yml`，在 Redis 7 环境验证正常两级缓存和失效流程。
6. 运行 `rg` 和 Maven dependency tree，确认生产源码、README、测试资源、父 POM及解析后的依赖树均不再包含 Resilience4j。
7. 依次执行目标单元测试、`mvn -pl macula-boot-starters/macula-boot-starter-cache -am test`、指定 Redis 集成测试以及 `mvn clean verify`，并报告 Surefire/Failsafe 的测试数量和结果。
