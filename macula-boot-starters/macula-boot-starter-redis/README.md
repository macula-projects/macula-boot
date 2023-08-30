## 概述

该模块提供连接Redis的相关配置，使用了[redisson库](https://redisson.pro/)

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-redis</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

使用时有两种方式配置，一种是spring-boot默认的配置形式，另一种是redisson的配置形式。如下所示：

```yaml
spring:
  redis:
    database: 
    host:
    port:
    password:
    ssl: 
    timeout:
    cluster:
      nodes:
    sentinel:
      master:
      nodes:
```

redisson的配置方式如下，包括单点、主从、集群，下面是集群模式：

```yaml
spring:
  redis:
   redisson: 
      file: classpath:redisson.yaml  # file和config选一个
      config: |
        clusterServersConfig:
          idleConnectionTimeout: 10000
          connectTimeout: 10000
          timeout: 3000
          retryAttempts: 3
          retryInterval: 1500
          failedSlaveReconnectionInterval: 3000
          failedSlaveCheckInterval: 60000
          password: null
          subscriptionsPerConnection: 5
          clientName: null
          loadBalancer: !<org.redisson.connection.balancer.RoundRobinLoadBalancer> {}
          subscriptionConnectionMinimumIdleSize: 1
          subscriptionConnectionPoolSize: 50
          slaveConnectionMinimumIdleSize: 24
          slaveConnectionPoolSize: 64
          masterConnectionMinimumIdleSize: 24
          masterConnectionPoolSize: 64
          readMode: "SLAVE"
          subscriptionMode: "SLAVE"
          nodeAddresses:
          - "redis://127.0.0.1:7004"
          - "redis://127.0.0.1:7001"
          - "redis://127.0.0.1:7000"
          scanInterval: 1000
          pingConnectionInterval: 0
          keepAlive: false
          tcpNoDelay: false
        threads: 16
        nettyThreads: 32
        codec: !<org.redisson.codec.Kryo5Codec> {}
        transportMode: "NIO"
```

## 核心功能

### 默认提供的Bean

- RedissonClient
- RedissonRxClient
- RedissonReactiveClient
- RedissonConnectionFactory
- RedisTemplate
- StringRedisTemplate
- ReactiveRedisTemplate
- ReactiveStringRedisTemplate

### 多Redis源的配置-Redisson方式

首先需要在配置文件中添加：

```yaml
spring:
  redis:
    redisson:
      one:
        file: classpath:redisson.yaml
        config: |
          xxxxx
      two:
        file: classpath:redisson.yaml
        config: |
          xxxxx
```

然后添加配置Bean，注意其中一个要设置@Primary注解，以便给默认的RedisConnectionFactory使用

```java
public class Config {
    @Bean
    @ConfigurationProperties(prefix = "spring.redis.redisson.one")
    public RedissonProperties redissonPropertiesOne() {
        return new RedissonProperties();
    }

    @Primary
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClientOne(ApplicationContext ctx, RedissonProperties redissonPropertiesOne) throws Exception {
        Config config = RedissonConfigBuilder.create().build(ctx, null, redissonProperties);
        return Redisson.create(config);
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.redis.redisson.tow")
    public RedissonProperties redissonPropertiesTwo() {
        return new RedissonProperties();
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClientTwo(ApplicationContext ctx, RedissonProperties redissonPropertiesTwo) throws Exception {
        Config config = RedissonConfigBuilder.create().build(ctx, null, redissonProperties);
        return Redisson.create(config);
    }
}
```

### 多Redis源的配置-Redis方式

首先在配置文件中添加

```yaml
spring:
  redis:
    one:
    xxx
    two:
      xxx
```

然后添加配置Bean，注意其中一个要设置@Primary注解，以便给默认的RedisConnectionFactory使用

```java

@Configuration
public class Config {
    @Bean
    @ConfigurationProperties(prefix = "spring.redis.one")
    public RedisProperties redisPropertiesOne() {
        return new RedisProperties();
    }

    @Primary
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClientOne(ApplicationContext ctx, RedisProperties redisPropertiesOne)
        throws Exception {
        Config config = RedissonConfigBuilder.create().build(ctx, redisPropertiesOne, new RedissonProperties());
        return Redisson.create(config);
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.redis.two")
    public RedisProperties redisPropertiesTwo() {
        return new RedisProperties();
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClientTwo(ApplicationContext ctx, RedisProperties redisPropertiesTwo)
        throws Exception {
        Config config = RedissonConfigBuilder.create().build(ctx, redisPropertiesTwo, new RedissonProperties());
        return Redisson.create(config);
    }
}
```

{{% alert title="提示" color="primary" %}}

注意：如果定义多个RedisConnectonFactory，需要标识其中一个为@Primary，否则会报错。你的配置需要保证在RedissonAutoConfiguration配置前，可以使用@AutoConfigureBefore注解。同时要注意bean的方法命名与构造注入时参数名称要一致。

{{% /alert %}}

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-data-26</artifactId>
    </dependency>

    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        <exclusions>
            <exclusion>
                <groupId>redis.clients</groupId>
                <artifactId>jedis</artifactId>
            </exclusion>
            <exclusion>
                <groupId>io.lettuce</groupId>
                <artifactId>lettuce-core</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
</dependencies>
```

## 版权说明

- redisson：https://github.com/redisson/redisson/blob/master/LICENSE.txt
- jackson：https://github.com/FasterXML/jackson-core/blob/master/LICENSE