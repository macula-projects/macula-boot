# Task 示例

该模块只连接 SnailJob Server，同时演示原生 SnailJob Handler 与保持源码不变的 `@XxlJob` Handler。`macula-boot-starter-task` 会自动启用 SnailJob，启动类不需要 `@EnableSnailJob`，模块也不直接依赖 SnailJob 或官方 XXL-JOB 客户端。

## 前置条件

- JDK 17。
- Nacos，默认 `127.0.0.1:38848`。
- SnailJob 1.9 Server，默认通信地址 `127.0.0.1:17888`。

不需要部署 XXL-JOB Admin。

## 启动

```bash
export SNAIL_JOB_NAMESPACE=example
export SNAIL_JOB_TOKEN=example
# 在仓库根目录先安装所需框架依赖，再只启动当前应用
mvn -pl macula-boot-examples/macula-example-task -am install -DskipTests -Dgpg.skip=true -Pdeploy
mvn -f macula-boot-examples/macula-example-task/pom.xml spring-boot:run
```

默认应用端口为 `7099`，SnailJob 客户端通信端口为 `17889`。可通过 `SNAIL_JOB_SERVER_HOST`、`SNAIL_JOB_SERVER_PORT`、`SNAIL_JOB_PORT`、`NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_USERNAME` 和 `NACOS_PASSWORD` 覆盖。

## 执行器

| 代码写法 | SnailJob executor name | 说明 |
| --- | --- | --- |
| `@XxlJob` | `demoJobHandler` | 旧 XXL Handler 原样保留，由 SnailJob 执行并连续输出日志 |
| `@XxlJob` | `commandJobHandler` | 由 SnailJob 执行调度参数指定的单一命令，仅限受信任本地演示 |
| `@JobExecutor` | `snailDemoJobHandler` | SnailJob 原生 Handler，记录参数并返回成功 |

在 SnailJob 控制台使用表中的 executor name 创建任务。旧 XXL-JOB 控制台中的任务定义和执行历史不会自动迁移。

示例已在 `logback-spring.xml` 中配置 SnailJob 官方 Logback appender，因此 `XxlJobHelper.log(...)` 与原生 SnailJob 日志都可进入 SnailJob 日志链路。生产环境不得开放通用命令执行器，也不得使用 README 中的占位 token。
