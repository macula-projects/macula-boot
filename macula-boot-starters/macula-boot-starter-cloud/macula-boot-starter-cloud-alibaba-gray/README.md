## 概述

基于 Nacos 实例元数据和 Spring Cloud LoadBalancer，按 `grayversion` 标签筛选实例，再在候选实例中轮询。适用于 Alibaba 服务调用和 Spring Cloud Gateway 的服务发现路由，不提供百分比放量、用户分群或强隔离策略。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cloud-alibaba-gray</artifactId>
    <version>${macula.version}</version>
</dependency>
```

通常直接选择 [Alibaba Starter](../macula-boot-starter-cloud-alibaba/README.md) 或 `macula-boot-starter-cloud-alibaba-scg`，二者已传递引入本模块，无需重复声明。单独引入时还需提供 Nacos Discovery、Spring Cloud LoadBalancer；它们在本模块 POM 中为 optional 依赖。

## 使用配置

沿用最新 [Alibaba Provider application.yml](../../../macula-boot-examples/macula-example-alibaba-provider1/src/main/resources/application.yml) 的注册发现配置，在需要灰度发布的实例上追加：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        metadata:
          grayversion: v2
```

同一服务的基线实例不配置 `grayversion`，灰度实例配置如 `v2`。samples 中已有的 `metadata.version: v1` 不是灰度匹配键，不能代替 `metadata.grayversion`。

| 配置 / 输入 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.cloud.nacos.discovery.metadata.grayversion` | 未设置 | 本实例灰度标签；未设置或空串视为基线，避免使用空白字符 |
| HTTP 请求头 `grayversion` | 未设置 | 本次请求希望访问的灰度标签，使用第一个头值、精确匹配 |
| `GrayVersionContextHolder` | 当前线程未设置 | 非 `RequestDataContext` 请求从该上下文读取标签 |
| `macula.cloud.gray.enabled` | `true` | **Gateway 模块**创建灰度过滤器的开关，不是本模块开关 |
| `macula.gateway.gray.enabled` | `false` | **Gateway 模块**读取请求头并设置灰度上下文的开关 |
| `spring.reactor.context-propagation` | samples 为 `auto` | WebFlux/Gateway 上下文传播配置，不是实例筛选开关 |

需要网关灰度上下文处理时，在 [Gateway 示例](../../../macula-boot-examples/macula-example-alibaba-gateway/src/main/resources/application.yml) 上补充：

```yaml
macula:
  cloud:
    gray:
      enabled: true
  gateway:
    gray:
      enabled: true
```

本模块没有独立的 `enabled` 属性。关闭上述网关开关既不会卸载 `GrayRoundRobinLoadBalancer`，也不会移除原始请求头；负载均衡器仍可直接读取请求中的 `grayversion`。如需停用本模块，可通过 `spring.autoconfigure.exclude` 排除 `dev.macula.boot.starter.cloud.alibaba.gray.config.GrayNacosAutoConfiguration`，并核对应用剩余的负载均衡配置。

## 核心功能

### 实例选择规则

| 请求与实例情况 | 候选实例 |
| --- | --- |
| 指定标签，存在相同标签实例 | 仅匹配标签的实例 |
| 指定标签，但没有匹配实例 | 优先基线实例 |
| 未指定标签 | 优先基线实例 |
| 上述筛选后没有候选，但注册列表非空 | 回退到全部实例，包括其他灰度实例 |
| 注册列表为空 | 返回无可用实例响应，由调用端处理 |

候选集内使用轮询，不按 Nacos 权重分配。此实现优先保持可用性：即使请求未带灰度头，只有灰度实例存活时也可能访问灰度实例，不能将其作为租户或安全隔离边界。

### 标签传播

- `RequestDataContext` 类型的负载均衡请求直接读取请求头；即使头不存在，也不会再回退读取线程上下文。其他请求类型读取 `GrayVersionContextHolder`。
- 配合 Web Starter 的 `GrayHandlerInterceptor`，Servlet 入站灰度头写入请求线程上下文，请求完成后清理；Feign Starter 的 `HeaderRelayInterceptor` 将上下文标签传递给下游。
- 实例注册时，`GrayNacosRegistrationCustomizer` 将本实例标签写入 `GrayVersionMetaHolder`。这是实例级信息，不会自动让该实例发出的所有 HTTP 调用携带相同标签。
- 异步调用需配合 [Async Starter](../../macula-boot-starter-async/README.md) 的受管执行器传播上下文；手动设置线程上下文时应在 `finally` 中清理。

以已启动的 Alibaba 示例链路为例，可发送带标签请求：

```bash
curl -H 'grayversion: v2' \
  'http://127.0.0.1:5000/consumer/api/v1/consumer/echo/demo?str=hello'
```

需在实际被调用的服务上部署并注册 `v2` 实例，再观察实例日志确认路由结果；仅返回 HTTP 成功不能证明命中灰度实例。公网入口应自行校验或重写灰度头，本模块不会验证调用者是否有权指定灰度标签。

## 依赖引入

| 依赖 | 是否传递 | 用途 |
| --- | --- | --- |
| `macula-boot-commons` | 是 | 灰度常量和上下文 |
| `spring-cloud-starter-loadbalancer` | 否，optional | 客户端实例选择 |
| `spring-cloud-starter-alibaba-nacos-discovery` | 否，optional | Nacos 服务发现与注册定制 |
| `spring-boot-starter-web` | 否，optional | 可选 Servlet 环境依赖，不会强制网关引入 MVC |

默认负载均衡 Bean 可由应用自定义的 `ReactorLoadBalancer<ServiceInstance>` 替代，需在对应 LoadBalancer 子上下文中配置。

## 版权说明

本模块遵循仓库 [Apache License 2.0](../../../LICENSE)。
