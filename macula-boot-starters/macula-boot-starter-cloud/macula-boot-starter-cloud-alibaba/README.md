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

Spring Cloud Alibaba 2025.1 不再使用旧的 bootstrap 配置路径。请在 `application.yml` 中通过 Config Data 导入 Nacos 配置：

```yaml
server:
  port: 8081

spring:
  profiles:
    # maven打包的时候指定profile，包括local,dev,test,staging,pet,prd，默认启用local
    active: @profile.active@
  application:
    name: macula-cloud-system
  config:
    import:
      - optional:nacos:${spring.application.name}.yml?refreshEnabled=true
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml?refreshEnabled=true
  cloud:
    nacos:
      username: ${nacos.username}
      password: ${nacos.password}
      config:
        server-addr: ${nacos.config.server-addr}
        namespace: ${nacos.config.namespace}
        # group:
        refresh-enabled: true
        file-extension: yml

# 和环境有关的配置信息，不同环境覆盖此处的配置
nacos:
  username: ${NACOS_USERNAME:nacos}
  password: ${NACOS_PASSWORD:nacos}
  config:
    server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
    namespace: ${NACOS_NAMESPACE:MACULA5}

---
spring:
  config:
    activate:
      on-profile: dev
nacos:
  username: maculav5
  #password: 请通过启动命令赋予密码
  config:
    server-addr: 10.94.108.55:8848
    namespace: MACULA5
```

在nacos中以spring.application.name命名dataId，如果有profile则加上-xxx命名，后缀是yml，配置注册中心

```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: 127.0.0.1:8848
        namespace: MACULA5
        # group:
    sentinel:
      enabled: false
```

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
