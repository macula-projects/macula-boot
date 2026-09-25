## 概述

本模块提供 Spring 异步任务与定时任务的基础配置，并基于 Micrometer Context Propagation 在 Spring
托管线程池之间传播调用上下文。

主要功能包括：

- 启用 Spring `@Async` 和 `@Scheduled` 能力
- 为 Spring 托管 Executor 提供 `ContextPropagatingTaskDecorator`
- 传播 Observation、Trace、MDC、Tenant 和 GrayVersion 上下文
- 在任务执行结束后恢复工作线程原状态，避免线程复用造成上下文污染
- 支持与用户自定义的有序 `TaskDecorator` 组合使用

Macula Boot 6.1 已移除 Alibaba TTL、`TtlRunnable`、`TtlCallable` 和 `TtlWrappers`，统一使用
Micrometer Context Propagation。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-async</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用说明

### `@Async` 上下文传播

引入组件后，Spring Boot 管理的异步执行器会使用
`maculaContextPropagatingTaskDecorator`。调用 `@Async` 方法时，提交线程中的 Observation、Trace、
MDC、Tenant 和 GrayVersion 会传播到异步线程，并在任务完成后自动清理。

```java
@Service
public class OrderService {

    @Async
    public void processAsync(Long orderId) {
        // 当前 Trace、MDC、Tenant 和 GrayVersion 均可在此处获取
    }
}
```

### `CompletableFuture` 使用方式

使用 `CompletableFuture` 时必须显式传入同一个 Spring 托管 Executor：

```java
CompletableFuture.runAsync(() -> {
    // do your business
}, taskExecutor);
```

未指定 Executor 的 `CompletableFuture.*Async` 默认使用 JDK common pool，不会自动传播上下文。

### 原生 Executor 接入

自行创建的 `ExecutorService` 可使用 Micrometer `ContextExecutorService` 显式包装：

```java
ExecutorService executor = ContextExecutorService.wrap(
    Executors.newFixedThreadPool(4)
);

executor.submit(() -> {
    // 当前上下文可在此处获取
});
```

包装器在任务提交时捕获上下文。应用仍需按照自身生命周期管理并关闭底层 Executor。

### Reactor 上下文传播

WebFlux 和 Gateway 应开启 Reactor 自动上下文传播：

```yaml
spring:
  reactor:
    context-propagation: auto
```

开启后，已注册的 Observation、Trace/MDC、Tenant 和 GrayVersion 上下文可跨 `publishOn`、
`subscribeOn` 等线程切换恢复。

### 自定义 TaskDecorator

Starter 提供的 `TaskDecorator` 使用最高优先级。用户可以继续声明其他带 `@Order` 的
`TaskDecorator`，由 Spring Boot 按顺序组合。自行创建 Executor 时，必须显式把容器中的 Decorator
组合并应用到该 Executor，单独声明 Bean 不会自动接管任意线程池。

### 不支持自动传播的边界

以下执行方式不会自动传播上下文：

- 直接创建的 `Thread`
- 未包装的 `Executors.*`
- 未指定受管 Executor 的 `CompletableFuture.*Async`
- 第三方组件内部创建且未开放定制入口的私有线程池

这些场景应改用 Spring 托管 Executor，或通过 `ContextExecutorService.wrap(...)` 显式接入。

## 版权说明

- Spring Framework：https://github.com/spring-projects/spring-framework/blob/main/LICENSE.txt
- Spring Boot：https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt
- Micrometer Context Propagation：https://github.com/micrometer-metrics/context-propagation/blob/main/LICENSE
