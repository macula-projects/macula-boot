# Task 示例

该模块展示 Macula Boot 任务 Starter 与 XXL-JOB、SnailJob 的集成方式，包含一个普通执行器和一个命令行执行器。

## 前置条件

- Nacos，默认 `127.0.0.1:8848`。
- XXL-JOB Admin，默认 `http://127.0.0.1:9084/macula-cloud-xxljob`。
- SnailJob Server，默认通信地址 `127.0.0.1:17888`。

## 启动

```bash
export XXL_JOB_ACCESS_TOKEN=default_token
export SNAIL_JOB_NAMESPACE=example
export SNAIL_JOB_TOKEN=example
mvn -pl macula-boot-examples/macula-example-task -am spring-boot:run
```

默认应用端口为 `7099`，SnailJob 客户端通信端口为 `17899`。Nacos 参数也可通过 `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_USERNAME` 和 `NACOS_PASSWORD` 覆盖。

## 执行器

| 调度平台 | Handler | 说明 |
| --- | --- | --- |
| XXL-JOB | `demoJobHandler` | 连续输出 5 次执行日志 |
| XXL-JOB | `commandJobHandler` | 执行调度参数指定的单一命令，只应用于受信任的本地演示环境 |
| SnailJob | `demoJobHandler` | 记录任务参数并返回成功 |

生产环境不应开放通用命令执行器，也不应使用 README 中的占位 token。
