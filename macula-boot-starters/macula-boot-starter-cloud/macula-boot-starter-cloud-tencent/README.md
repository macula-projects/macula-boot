## 概述

基于[Spring Cloud Tencent](https://github.com/tencent/spring-cloud-tencent)
的微服务开源套件[Polarismesh](https://polarismesh.cn/)搭建微服务平台。

## 组件坐标

```xml
<!-- 微服务模块依赖 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cloud-tencent</artifactId>
    <version>${macula.version}</version>
</dependency>

        <!-- 网关模块依赖 -->
<dependency>
<groupId>dev.macula.boot</groupId>
<artifactId>macula-boot-starter-cloud-tencent-scg</artifactId>
<version>${macula.version}</version>
</dependency>
```

## 使用配置

以下按当前 [Tencent Provider application.yml](../../../macula-boot-examples/macula-example-tencent-provider/src/main/resources/application.yml) 精简；网关差异见 [Tencent Gateway application.yml](../../../macula-boot-examples/macula-example-tencent-gateway/src/main/resources/application.yml)。

| 属性 | 示例值 / 说明 |
| --- | --- |
| `spring.config.import` | `optional:polaris`，通过 Config Data 导入配置 |
| `spring.profiles.group.docker` | `local`，Docker 复用本地默认配置 |
| `spring.cloud.polaris.config.enabled` / `auto-refresh` | 示例均为 `true`，启用配置中心和刷新 |
| `spring.cloud.polaris.config.groups[].name` | 应用名对应的配置组 |
| `spring.cloud.polaris.discovery.enabled` / `register` | 示例均为 `true`，启用发现和注册 |
| `spring.cloud.polaris.address` | local：`grpc://127.0.0.1:38091`；docker：`grpc://polaris:8091` |
| `spring.cloud.polaris.namespace` | `POLARIS_NAMESPACE`，示例为 `macula-dev` |
| `spring.cloud.nacos.discovery.server-addr` | Polaris Nacos 兼容地址：local `127.0.0.1:38849`，docker `polaris:8848` |

本模块无额外自有属性；表中地址和开关是 samples 配置，不是所有应用的默认值。

```yaml
server:
  port: ${SERVER_PORT:4020}
spring:
  profiles:
    active: '@profile.active@'
    group:
      docker: local
  application:
    name: macula-example-tencent-provider
  config:
    import: optional:polaris
  cloud:
    polaris:
      config:
        enabled: true
        auto-refresh: true
        groups:
          - name: ${spring.application.name}
      discovery:
        enabled: true
        register: true
---
spring:
  config:
    activate:
      on-profile: local
  cloud:
    nacos:
      discovery:
        server-addr: ${POLARIS_NACOS_SERVER_ADDR:127.0.0.1:38849}
    polaris:
      address: ${POLARIS_SERVER_ADDR:grpc://127.0.0.1:38091}
      namespace: ${POLARIS_NAMESPACE:macula-dev}
---
spring:
  config:
    activate:
      on-profile: docker
  cloud:
    nacos:
      discovery:
        server-addr: ${POLARIS_NACOS_SERVER_ADDR:polaris:8848}
    polaris:
      address: ${POLARIS_SERVER_ADDR:grpc://polaris:8091}
      namespace: ${POLARIS_NAMESPACE:macula-dev}
```

`dev/stg/pet/prd` 仅保留配置中心连接信息，其他配置由 Polaris 配置组提供。`optional:polaris` 只放宽配置导入，不代表服务发现等依赖可以完全离线运行。

## 核心功能

请参考[官方文档](https://github.com/tencent/spring-cloud-tencent)

## 依赖引入

微服务模块

```xml

<dependencies>
    <!-- Spring Cloud -->
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-feign</artifactId>
    </dependency>

    <!-- Spring Cloud Tencent -->
    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-all</artifactId>
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
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-all</artifactId>
    </dependency>
</dependencies>
```

Macula Boot 6.1 的 Tencent 与 Tencent SCG Starter 默认排除 Polaris Contract。该组件当前仍依赖 Springdoc
2.x：在普通应用中会导致合约上报出现 `NoSuchMethodError`，在 reactive gateway 中会直接导致启动失败。
确需合约上报时，请等待 Spring Cloud Tencent 提供 Springdoc 3 兼容版本后再显式引入
`spring-cloud-starter-tencent-polaris-contract`。

## 版权说明

- spring-cloud-tencent：https://github.com/Tencent/spring-cloud-tencent/blob/2021.0/LICENSE
