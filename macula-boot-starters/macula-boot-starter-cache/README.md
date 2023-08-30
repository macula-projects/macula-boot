## 概述

该模块提供两级缓存功能，第一级是本地缓存，使用Caffeine，第二级是Redis缓存，使用Redis。

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cache</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
  cache:
    type: redis    # 必须指定是redis，否则使用系统默认的缓存
    # These properties are custom
    two-level:
      # Redis properties
      default-time-to-live: 86400000   # 所有缓存默认的过期时间，默认是24h
      time-to-live:                    # 指定某个缓存的过期时间，不配置使用default
        user-service: 86400000
        tag-service: 43200000
      use-key-prefix: true             # 默认true，可以不用配置
      key-prefix: "macula:cache:"      # 默认是macula:cache:，可以不用配置，建议加上appName
      topic: "macula:cache:two-level:topic" # 默认是macula:cache:two-level:topic，可以不用配置

      # Local Caffeine cache properties
      local:
        max-size: 2000              	 # 本地缓存的最大容量
        expiry-jitter: 50           	 # 本地缓存的过期时间偏移量

      # Resilience4j Circuit Breaker properties for Redis
      open-circuit-breaker: false  		 # 是否开启熔断器，以下都是默认值，可以不配置
      circuit-breaker:
        failure-rate-threshold: 25
        slow-call-rate-threshold: 25
        slow-call-duration-threshold: 250ms
        sliding-window-type: count_based
        permitted-number-of-calls-in-half-open-state: 20
        max-wait-duration-in-half-open-state: 5s
        sliding-window-size: 40
        minimum-number-of-calls: 10
        wait-duration-in-open-state: 2500ms
```

## 核心功能

1. 读取缓存时优先从Caffeine读取，如果没有，则从Redis读取，并且将结果写入Caffeine
2. 写入缓存时优先写入Redis，如果写入失败，则写入Caffeine
3. 当写入或者删除缓存时，通过redis的pub/sub发布订阅机制通知其他实例清除本地缓存
4. 默认已经开启了缓存注解功能，可以直接使用@Cacheable、@CacheConfig等注解
5. 引入了CircuitBreaker，当redis访问出现问题的时候可以启用熔断

使用方式可以参考标准的[Spring Cache](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)
文档

```java
@Component
@CacheConfig(cacheNames = "user-service")
public class UserServiceImpl implements UserService {

    @Cacheable(key = "#root.methodName + ':' + #userId")
    public User getUser(String userId) {
        return new User(userId, "Rain", "password");
    }
    
    @Cacheable
    public User getUser2(String userId) {
        return new User(userId, "Rain2", "password2");
    }
    
    @Cacheable
    public User getUser3() {
        return new User("3333x", "Rain3", "password3");
    }
}
```

## 依赖引入

```xml
<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-circuitbreaker</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-core</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 版本说明

- caffeine：https://github.com/ben-manes/caffeine/blob/master/LICENSE
- resilience4j：https://github.com/resilience4j/resilience4j/blob/master/LICENSE.txt