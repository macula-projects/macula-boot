# Macula Boot Task Starter

`macula-boot-starter-task` 以 SnailJob Server 作为唯一调度服务端，同时支持两种客户端写法：SnailJob 原生 `@JobExecutor` / `@Retryable`，以及历史 XXL-JOB Bean Handler 的 `@XxlJob`。旧 Handler 由兼容桥注册为 SnailJob executor，不需要 XXL-JOB Admin，也不会启动 XXL executor、回调或日志拉取通信。

当前固定使用 SnailJob `1.9.0`，保持 Java 17 兼容。SnailJob 2.x 要求更高 JDK，本 Starter 不升级到 2.x。

## 引入依赖

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-task</artifactId>
</dependency>
```

Starter 会传递提供 SnailJob starter、Job Core、Retry Core，以及旧 Handler 编译所需的最小 XXL 兼容 API。应用不要再引入 `com.xuxueli:xxl-job-core`；检测到官方客户端的 `XxlJobExecutor` 时，应用会启动失败并提示移除冲突依赖。

## 自动启用与配置

| 属性 | 默认值 / 要求 | 说明 |
| --- | --- | --- |
| `macula.task.enabled` | `true` | 总开关；为 `false` 时即使 `snail-job.enabled=true` 也不启用 |
| `macula.task.xxl-job-adapter.enabled` | `true` | 仅控制旧 XXL Handler 兼容桥 |
| `snail-job.enabled` | 本 Starter 默认启用 | 显式 `false` 关闭原生客户端及兼容桥 |
| `snail-job.server.host` / `port` | 需匹配服务端 | 调度通信地址，不是管理页面地址 |
| `snail-job.namespace` / `group` / `token` | 需匹配服务端 | 命名空间、分组和访问凭证 |
| `snail-job.host` / `port` | 按部署配置 | 服务端可访问的客户端地址和端口 |

引入 Starter 后会默认启用 SnailJob，启动类无需添加 `@EnableSnailJob`，也无需配置 `snail-job.enabled=true`。连接配置示例：

```yaml
snail-job:
  server:
    host: ${SNAIL_JOB_SERVER_HOST:127.0.0.1}
    port: ${SNAIL_JOB_SERVER_PORT:17888}
  namespace: ${SNAIL_JOB_NAMESPACE:example}
  group: ${SNAIL_JOB_GROUP:${spring.application.name}}
  token: ${SNAIL_JOB_TOKEN:example}
  # 客户端可被服务端访问的地址；host 留空时自动推断
  # host: 127.0.0.1
  port: ${SNAIL_JOB_PORT:17889}
```

可用开关：

```yaml
macula:
  task:
    enabled: true                 # false：关闭整个 Task Starter，不连接调度服务
    xxl-job-adapter:
      enabled: true               # false：只关闭 @XxlJob 兼容桥

snail-job:
  enabled: true                   # 显式 false 优先，关闭 SnailJob 客户端和兼容桥
```

## SnailJob 原生任务

```java
@Component
public class ReportJob {

    @JobExecutor(name = "reportJob")
    public ExecuteResult execute(JobArgs args) {
        return ExecuteResult.success();
    }
}
```

SnailJob 1.9.0 的 `JobArgs`、`ExecuteResult`、`@JobExecutor` 和 `@Retryable` 均按原生语义使用。

## 兼容旧 XXL Handler

旧代码可保持原 import、注解和方法体：

```java
@Component
public class LegacyJob {

    @XxlJob(value = "legacyJob", init = "init", destroy = "destroy")
    public void execute() {
        XxlJobHelper.log("parameter: {}", XxlJobHelper.getJobParam());
    }

    public void init() {
    }

    public void destroy() {
    }
}
```

Starter 只实现常用的最小兼容面：`@XxlJob`、`XxlJobContext`、`XxlJobHelper`。不提供 `XxlJobExecutor`、`IJobHandler`、GLUE、脚本协议、Admin OpenAPI 等完整 XXL 客户端能力。

Handler 支持无参数方法；引用类型参数按旧 Bean Handler 兼容行为传入 `null`；primitive 参数会在启动扫描时被拒绝。`init` 在注册前调用，初始化失败会阻止注册；`destroy` 在上下文关闭时调用。

### 上下文与结果映射

| SnailJob | XXL 兼容上下文 |
| --- | --- |
| `jobId` | `XxlJobHelper.getJobId()` |
| `taskBatchId` | `XxlJobHelper.getLogId()` |
| 字符串 `jobParams` | 原样映射到 `getJobParam()` |
| 其他 `jobParams` | JSON 字符串映射到 `getJobParam()` |
| 普通任务 | `shardIndex=0`、`shardTotal=1` |
| 分片任务 | 映射 `ShardingJobArgs` 的 index/total |

方法正常返回或调用 `handleSuccess` 时返回 SnailJob 成功；`handleFail`、`handleTimeout` 或业务异常返回失败及可用消息。执行完成后会清理线程上下文，避免线程池复用时串任务。

`@XxlJob.value` 与原生 `@JobExecutor.name` 共用 SnailJob executor 名称空间。空名称、重复名称或两种注解重名都会使应用启动失败，不会静默覆盖。

## 日志

`XxlJobHelper.log(...)` 写入 SLF4J 的 `xxl-job logger`。要把任务日志上报到 SnailJob Server，请在 Logback 配置中挂载官方 appender：

```xml
<appender name="snailLogAppender"
          class="com.aizuda.snailjob.client.common.appender.SnailLogbackAppender"/>

<root level="info">
    <appender-ref ref="snailLogAppender"/>
</root>
```

这里不再生成供 XXL-JOB Admin 拉取的专用日志文件。

## 迁移边界

- SnailJob Server 是唯一服务端；原 XXL-JOB Admin 中的 Cron、路由、阻塞、超时、重试、历史、告警和用户数据需要在 SnailJob 中重新建立或导入。
- 两个平台的高级策略不保证一一等价，最终以 SnailJob 1.9 Server 配置为准。
- 通用命令执行 Handler 只能用于受信任的本地演示，生产环境不得对外开放任务参数形成的系统命令。
- token 必须通过环境变量或安全配置中心注入，不要提交真实凭据。

SnailJob 及其客户端使用其项目声明的开源许可证；Macula 提供的最小 XXL 兼容层是独立实现的 Apache License 2.0 代码，不包含官方 XXL-JOB 客户端源码。
