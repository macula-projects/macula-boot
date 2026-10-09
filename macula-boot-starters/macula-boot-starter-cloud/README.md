## 概述

Macula Boot 的云能力聚合模块，面向 Java 17、Spring Boot 4 和 Spring Cloud 2025.1，提供 Alibaba/Nacos、Tencent/Polaris 两套接入方式，以及共享网关与 Alibaba 灰度路由能力。

本模块的 packaging 为 `pom`，不承载运行时代码，也不是引入后自动启用所有云组件的 Starter。业务应用按角色选择下面的子模块，通常不同时引入两套云平台。

## 组件坐标

所有子模块的 groupId 均为 `dev.macula.boot`，版本由 `macula-boot-parent` 管理；未使用统一版本管理时需显式指定与项目一致的版本。

| 应用角色 | artifactId | 主要能力 / 文档 |
| --- | --- | --- |
| Alibaba 业务服务 | `macula-boot-starter-cloud-alibaba` | [Nacos 配置与发现、Sentinel、Feign、灰度](macula-boot-starter-cloud-alibaba/README.md) |
| Alibaba 网关 | `macula-boot-starter-cloud-alibaba-scg` | 组合共享 Gateway、Nacos、Sentinel Gateway 和灰度模块 |
| Tencent 业务服务 | `macula-boot-starter-cloud-tencent` | [Polaris 接入与 Feign](macula-boot-starter-cloud-tencent/README.md) |
| Tencent 网关 | `macula-boot-starter-cloud-tencent-scg` | 组合共享 Gateway 与 Tencent 云组件 |
| 共享网关基础 | `macula-boot-starter-cloud-gateway` | [WebFlux 路由、认证鉴权、签名与加解密](macula-boot-starter-cloud-gateway/README.md) |
| Alibaba 灰度扩展 | `macula-boot-starter-cloud-alibaba-gray` | [Nacos 灰度标签与负载均衡](macula-boot-starter-cloud-alibaba-gray/README.md) |

例如 Alibaba 业务服务引入：

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cloud-alibaba</artifactId>
</dependency>
```

Alibaba 网关改选 `macula-boot-starter-cloud-alibaba-scg`；Tencent 同理选择对应 `-tencent` 或 `-tencent-scg`。SCG 已包含共享 Gateway，无需再重复引入；业务服务的 Web、持久化等能力按需组合其他 Starter。

## 使用配置

聚合模块没有独立配置前缀。配置展示以当前 [Examples](../../macula-boot-examples/README.md) 的 `application.yml` 为准，完整示例与说明如下：

| 配置范围 | 主要属性 | 参考 |
| --- | --- | --- |
| Nacos | `spring.config.import`、`spring.cloud.nacos.*`；`spring.config.nacos.*` 是示例自定义占位属性 | [Alibaba Provider](../../macula-boot-examples/macula-example-alibaba-provider1/src/main/resources/application.yml) |
| Sentinel | `spring.cloud.sentinel.transport.*`、`spring.cloud.sentinel.datasource.*` | [Alibaba 配置说明](macula-boot-starter-cloud-alibaba/README.md) |
| Polaris | `spring.config.import`、`spring.cloud.polaris.*`；兼容发现地址为 `spring.cloud.nacos.discovery.server-addr` | [Tencent Provider](../../macula-boot-examples/macula-example-tencent-provider/src/main/resources/application.yml) |
| 网关 | `spring.cloud.gateway.server.webflux.*`、`spring.security.oauth2.resourceserver.opaquetoken.*`、`macula.gateway.*` | [Alibaba Gateway](../../macula-boot-examples/macula-example-alibaba-gateway/src/main/resources/application.yml)、[Tencent Gateway](../../macula-boot-examples/macula-example-tencent-gateway/src/main/resources/application.yml) |
| Redis | `spring.data.redis.*` | [Redis 配置说明](../macula-boot-starter-redis/README.md) |
| 灰度 | `spring.cloud.nacos.discovery.metadata.grayversion`、请求头 `grayversion` | [灰度规则与开关边界](macula-boot-starter-cloud-alibaba-gray/README.md) |
| 可观测性 | `management.*` 导出配置、`spring.reactor.context-propagation` | [Observability](../macula-boot-starter-observability/README.md) |

示例按 profile 分层：公共段定义应用名和 Config Data 导入；`local` 提供本地默认值；`docker` 通过 `spring.profiles.group.docker=local` 复用并覆盖连接信息；`dev/stg/pet/prd` 主要保留配置中心连接信息。`observability` 额外开启 OTLP 网络导出，普通环境默认关闭。

Alibaba local 的 Nacos 地址为 `127.0.0.1:${NACOS_SERVER_PORT:38848}`，docker / 共享环境使用 `NACOS_SERVER_ADDR`；Tencent local 的 Polaris / Nacos 兼容地址分别为 `38091` / `38849` 端口。以上是示例值，不是 Starter 默认值；应用外部地址、认证信息及环境差异请在对应配置源覆盖。

## 核心功能

- 业务服务：通过所选云平台完成注册发现与配置加载，使用 Feign 调用下游；Alibaba 额外提供延迟注册、Sentinel 及灰度负载均衡。
- 网关：基于 WebFlux 的 HTTP/WebSocket 转发及安全扩展。签名、加解密还需业务提供 `CryptoService`；不要把 Servlet Web Starter 混入 reactive 网关。
- 灰度：Alibaba 与 Alibaba SCG 已传递引入灰度模块；Tencent 不使用该 Nacos 灰度实现。HTTP 灰度与 RocketMQ 灰度是不同机制，不能互相替代配置。

本模块不部署 Nacos、Polaris、Redis 或身份服务。运行条件、启动顺序和本地容器环境见 [Examples](../../macula-boot-examples/README.md)；构建、测试规范见[仓库根 README](../../README.md)。

## 依赖引入

云平台组合与可选依赖以各子模块 `pom.xml` 为准，版本统一在 [macula-boot-parent](../../macula-boot-parent/pom.xml) 管理。Tencent 与 Tencent SCG 当前排除 Polaris Contract，兼容边界见 [Tencent README](macula-boot-starter-cloud-tencent/README.md)。

## 版权说明

本模块遵循仓库 [Apache License 2.0](../../LICENSE)；第三方组件许可见各子模块说明。
