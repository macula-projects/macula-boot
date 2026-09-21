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

在 `application.yml` 中通过 Spring Config Data 导入 Polaris 配置：

```yaml
spring:
  application:
    name: macula-cloud-system
  config:
    import: optional:polaris
  cloud:
    nacos:
      discovery:
        server-addr: ${POLARIS_NACOS_SERVER_ADDR:127.0.0.1:18849}
    polaris:
      address: ${polaris.server-addr}
      namespace: ${polaris.namespace}
      config:
        auto-refresh: true
        groups:
          - name: ${spring.application.name}

polaris:
  namespace: ${POLARIS_NAMESPACE:macula-dev}
  server-addr: ${POLARIS_SERVER_ADDR:grpc://127.0.0.1:8091}
```

`optional:polaris` 保留配置中心不可用时的本地启动能力；生产环境若要求配置中心强依赖，可移除 `optional:`。

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

## 版权说明

- spring-cloud-tencent：https://github.com/Tencent/spring-cloud-tencent/blob/2021.0/LICENSE
