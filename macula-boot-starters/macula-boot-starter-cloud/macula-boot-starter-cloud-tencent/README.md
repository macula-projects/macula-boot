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

在bootstrap.yml中如下配置，主要是配置中心的相关配置

```yaml
TODO
```

在polarismesh中以spring.application.name命名dataId，如果有profile则加上-xxx命名，后缀是yml，配置注册中心

```yaml
TODO
```

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

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>

    <!-- Spring Cloud Tencent -->
    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-polaris-discovery</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-polaris-config</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-polaris-ratelimit</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-polaris-circuitbreaker</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-polaris-router</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-metadata-transfer</artifactId>
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
        <artifactId>spring-cloud-starter-tencent-polaris-discovery</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-polaris-ratelimit</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-polaris-router</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-tencent-gateway-plugin</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-metadata-transfer</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-tencent-featureenv-plugin</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- spring-cloud-tencent：https://github.com/Tencent/spring-cloud-tencent/blob/2021.0/LICENSE