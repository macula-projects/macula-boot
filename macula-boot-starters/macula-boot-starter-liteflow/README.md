## 概述

该模块基于[LiteFlow](https://liteflow.yomahub.com/)，是一个轻量，快速，稳定可编排的组件式规则引擎。

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-liteflow</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

### 基本配置

```yaml
liteflow:
  #规则文件路径
  rule-source: config/flow.el.xml
  #-----------------以下非必须-----------------
  #liteflow是否开启，默认为true
  enable: true
  #liteflow的banner打印是否开启，默认为true
  print-banner: true
  #zkNode的节点，只有使用zk作为配置源的时候才起作用，默认为/lite-flow/flow
  zk-node: /lite-flow/flow
  #上下文的最大数量槽，默认值为1024
  slot-size: 1024
  #FlowExecutor的execute2Future的线程数，默认为64
  main-executor-works: 64
  #FlowExecutor的execute2Future的自定义线程池Builder，LiteFlow提供了默认的Builder
  main-executor-class: com.yomahub.liteflow.thread.LiteFlowDefaultMainExecutorBuilder
  #自定义请求ID的生成类，LiteFlow提供了默认的生成类
  request-id-generator-class: com.yomahub.liteflow.flow.id.DefaultRequestIdGenerator
  #并行节点的线程池Builder，LiteFlow提供了默认的Builder
  thread-executor-class: com.yomahub.liteflow.thread.LiteFlowDefaultWhenExecutorBuilder
  #异步线程最长的等待时间秒(只用于when)，默认值为15
  when-max-wait-seconds: 15
  #when节点全局异步线程池最大线程数，默认为16
  when-max-workers: 16
  #when节点全局异步线程池等待队列数，默认为512
  when-queue-limit: 512
  #是否在启动的时候就解析规则，默认为true
  parse-on-start: true
  #全局重试次数，默认为0
  retry-count: 0
  #是否支持不同类型的加载方式混用，默认为false
  support-multiple-type: false
  #全局默认节点执行器
  node-executor-class: com.yomahub.liteflow.flow.executor.DefaultNodeExecutor
  #是否打印执行中过程中的日志，默认为true
  print-execution-log: true
  #简易监控配置选项
  monitor:
    #监控是否开启，默认不开启
    enable-log: false
    #监控队列存储大小，默认值为200
    queue-limit: 200
    #监控一开始延迟多少执行，默认值为300000毫秒，也就是5分钟
    delay: 300000
    #监控日志打印每过多少时间执行一次，默认值为300000毫秒，也就是5分钟
    period: 300000
```

### Nacos规则源配置

依赖了插件包之后，你无需再配置`liteflow.ruleSource`路径。

```yaml
liteflow:
  rule-source-ext-data-map:
    serverAddr: 127.0.0.1:8848
    dataId: demo_rule
    group: DEFAULT_GROUP
    namespace: your namespace id
    username: nacos
    password: nacos
```

其他规则源配置可以参考[官方文档](https://liteflow.yomahub.com/pages/6fa87e/)

## 核心功能

示例：https://gitee.com/bryan31/liteflow-example

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-rule-nacos</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 版权说明

- liteflow：https://github.com/dromara/liteflow/blob/master/LICENSE