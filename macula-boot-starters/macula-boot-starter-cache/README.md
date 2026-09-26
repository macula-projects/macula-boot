## 概述

本模块提供基于 Spring Cache 的两级缓存能力：一级缓存使用 Caffeine，二级缓存使用 Redis。读取时优先访问本地缓存，Redis 命中后回填本地缓存；写入、删除和清空时通过 Redis Pub/Sub 通知其他应用实例清理本地缓存。

Macula Boot 6.1 起，本模块不再依赖 Resilience4j。Redis 连接、资源或命令超时时采用 fail-open 策略，优先保证业务调用和当前实例的本地缓存可用。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cache</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用说明

### 配置两级缓存

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      connect-timeout: 2s       # Redis 建连超时，生产环境应按网络条件设置
      timeout: 3s               # Redis 命令超时，决定故障时单次请求的最长等待时间
  cache:
    type: redis                 # 必须选择 Redis，才会启用两级缓存自动配置
    cache-names:
      - user-service
      - tag-service
    two-level:
      default-time-to-live: 24h
      time-to-live:
        user-service: 24h
        tag-service: 12h
      use-key-prefix: true
      key-prefix: "macula:cache:"
      topic: "macula:cache:two-level:topic"
      local:
        max-size: 2000
        expiry-jitter: 50
```

`expiry-jitter` 表示本地缓存过期时间的随机偏移百分比，用于避免大量缓存同时失效，取值必须大于等于 `0` 且小于 `100`。

### 使用 Spring Cache 注解

```java
@Service
@CacheConfig(cacheNames = "user-service")
public class UserService {

    @Cacheable(key = "#userId")
    public User getUser(String userId) {
        return loadUser(userId);
    }

    @CacheEvict(key = "#userId")
    public void updateUser(String userId, User user) {
        saveUser(userId, user);
    }
}
```

具体注解语义参考 [Spring Cache](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)。

### Redis 故障时的行为

- 本地缓存命中时不会访问 Redis。
- Redis 读取发生连接、资源或命令超时时，该次访问按缓存未命中处理；带 `valueLoader` 的读取只执行一次 loader，并将结果保存到当前实例的本地缓存。
- Redis 写入失败时，`put` 和 `putIfAbsent` 仍维护当前实例的本地缓存。
- Redis 删除或清空失败时，当前实例的本地缓存仍会失效。
- Redis Pub/Sub 不可用时，不中断业务调用，但其他实例可能继续持有旧值，直到其本地缓存 TTL 到期。
- 序列化、类型和非法配置等程序错误不会被静默降级，仍会向调用方抛出。

本模块不再提供熔断器，单次 Redis 访问的阻塞时间由 `spring.data.redis.connect-timeout`、`spring.data.redis.timeout` 及客户端连接池配置决定。生产环境需要设置与业务延迟目标匹配的超时。

### 从 6.0 升级到 6.1

删除以下配置，它们在 6.1 中不再生效：

```yaml
spring:
  cache:
    two-level:
      open-circuit-breaker: true
      circuit-breaker: {}
```

同时删除应用代码中对以下接口的直接使用：

- `TwoLevelCacheProperties.CircuitBreakerProperties`
- 带 `io.github.resilience4j.circuitbreaker.CircuitBreaker` 参数的 `TwoLevelCache` 构造函数
- 带 `CircuitBreaker` 参数的 `TwoLevelCacheManager` 构造函数

如果业务应用仍需要通用熔断能力，应在业务或远程调用边界独立引入，而不是依赖 Cache Starter 传递 Resilience4j。

## 版权说明

本模块遵循项目根目录的 [Apache License 2.0](../../LICENSE)。Caffeine 与 Spring Framework/Spring Data Redis 同样基于 Apache License 2.0 发布。
