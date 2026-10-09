## 概述

引入基于Dubbo的微服务框架。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-dubbo</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 核心功能

### 基于springboot使用dubbo

本模块无自有配置前缀，常用配置由 Dubbo 绑定：

| 属性 | 说明 |
| --- | --- |
| `dubbo.application.name` | Dubbo 应用名 |
| `dubbo.registry.address` | 注册中心地址，按所用注册中心协议填写 |
| `dubbo.protocol.name` / `port` | 服务协议 / 暴露端口 |
| `dubbo.scan.base-packages` | Dubbo 服务实现扫描包 |
| `dubbo.consumer.timeout` / `retries` | 消费端调用超时（毫秒）/ 重试次数；非幂等操作慎用重试 |
| `dubbo.provider.timeout` | 提供端默认超时（毫秒） |

请参考[官方文档](https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/quick-start/spring-boot/)

### dubbo与spring cloud互通

本模块提供 Dubbo RPC 依赖，不会自动将 HTTP/Feign 接口转换为 Dubbo 接口。注册中心、协议及服务暴露使用 `dubbo.*` 配置；与 Spring Cloud 共用注册中心时，仍需分别配置两套服务发现与调用方式。

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- dubbo：https://github.com/apache/dubbo/blob/3.2/LICENSE
