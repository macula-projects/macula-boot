## 概述

基于[Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba)
的微服务开源套件[Nacos](https://nacos.io/zh-cn/)、[Sentinel](https://sentinelguard.io/zh-cn/)等搭建微服务平台。

## 组件坐标

```xml
<!-- 微服务模块依赖 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cloud-alibaba</artifactId>
    <version>${macula.version}</version>
</dependency>

        <!-- 网关模块依赖 -->
<dependency>
<groupId>dev.macula.boot</groupId>
<artifactId>macula-boot-starter-cloud-alibaba-scg</artifactId>
<version>${macula.version}</version>
</dependency>
```

## 使用配置

以下按当前 [Alibaba Provider application.yml](../../../macula-boot-examples/macula-example-alibaba-provider1/src/main/resources/application.yml) 精简，Gateway / Consumer 使用相同的配置中心组织方式。示例值不是 Starter 默认值。

| 属性 | 示例值 / 说明 |
| --- | --- |
| `spring.profiles.active` | `@profile.active@` 由 Maven 资源过滤，默认构建 profile 为 local |
| `spring.profiles.group.docker` | `local`，Docker 复用本地配置并覆盖连接信息 |
| `spring.config.import` | 导入应用及应用-profile 两个 Nacos Data ID |
| `spring.config.nacos.*` | 示例自定义连接属性，按 profile 提供；通过占位符传入 `spring.cloud.nacos.*` |
| `spring.cloud.nacos.server-addr` / `username` / `password` | Nacos 地址与认证信息 |
| `spring.cloud.nacos.discovery.enabled` / `namespace` | 启用注册发现并显式指定命名空间 |
| `spring.cloud.nacos.discovery.metadata.version` | 示例为 `v1`，服务实例版本标签 |
| `spring.cloud.nacos.discovery.register-enabled` / `register-delayed` | 本模块延迟注册扩展，仅显式配置 `false` / `true` 时启用 |
| `spring.cloud.sentinel.transport.dashboard` / `port` | Sentinel 控制台地址 / 客户端通信端口 |
| `spring.cloud.sentinel.datasource.<名称>.nacos.*` | Sentinel 规则的 Nacos 地址、Data ID、namespace、data-type 和 rule-type |

```yaml
server:
  port: ${SERVER_PORT:5020}
spring:
  profiles:
    active: '@profile.active@'
    group:
      docker: local
  application:
    name: macula-example-alibaba-provider1
  config:
    import:
      - optional:nacos:${spring.application.name}.yml?refreshEnabled=true
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml?refreshEnabled=true
  cloud:
    nacos:
      server-addr: ${spring.config.nacos.server-addr}
      namespace: ${spring.config.nacos.namespace}
      username: ${spring.config.nacos.username}
      password: ${spring.config.nacos.password}
      discovery:
        enabled: true
        namespace: ${spring.cloud.nacos.namespace}
        metadata:
          version: v1
---
spring:
  config:
    activate:
      on-profile: local
    nacos:
      server-addr: 127.0.0.1:${NACOS_SERVER_PORT:38848}
      namespace: ${NACOS_NAMESPACE:MACULA5}
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD:nacos}
---
spring:
  config:
    activate:
      on-profile: docker
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:nacos:8848}
      namespace: ${NACOS_NAMESPACE:MACULA5}
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD:nacos}
```

`local` 使用 `NACOS_SERVER_PORT` 覆盖本机端口；远程地址可直接覆盖 `spring.config.nacos.server-addr`。`docker` 及共享环境使用 `NACOS_SERVER_ADDR` 提供完整地址。`dev/stg/pet/prd` 只保留配置中心连接信息，业务配置由对应 Data ID 提供；完整 Redis、Sentinel 和可观测性配置见上面的源文件。

`optional:` 仅表示导入可选，不保证注册中心等其他依赖不可用时应用仍可启动；生产环境是否允许缺失配置需明确选择。

## 核心功能

请参考[官方文档](https://sca.aliyun.com/zh-cn/)

## 依赖引入

微服务模块

```xml

<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-feign</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.csp</groupId>
        <artifactId>sentinel-datasource-nacos</artifactId>
    </dependency>

</dependencies>
```

网关模块

```xml

<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-cloud-gateway</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.csp</groupId>
        <artifactId>sentinel-datasource-nacos</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- spring-cloud-alibaba：https://github.com/alibaba/spring-cloud-alibaba/blob/2021.x/LICENSE
