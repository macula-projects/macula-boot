# Macula Boot Examples

`macula-boot-examples` 提供可以对照源码学习的集成示例，不作为框架模块的依赖来源。示例按云平台和专项能力分组，默认使用 `local` Maven profile。

## 模块一览

| 类别 | 模块 | 默认端口 | 用途 |
| --- | --- | ---: | --- |
| Alibaba | `macula-example-alibaba-gateway` | HTTP 5000 / HTTPS 5443 | Nacos 服务发现、Spring Cloud Gateway、Sentinel 与网关安全 |
| Alibaba | `macula-example-alibaba-provider1` | 5020 | REST 接口、JWT 资源服务器、Nacos 配置刷新与服务注册 |
| Alibaba | `macula-example-alibaba-consumer` | 5010 | OpenFeign 服务调用、Sentinel 降级和 WebSocket |
| Alibaba | `macula-example-alibaba-provider2` | - | 预留的第二提供方模块，当前不启动 |
| Tencent | `macula-example-tencent-gateway` | 4000 | Polaris 服务发现与 Spring Cloud Gateway |
| Tencent | `macula-example-tencent-provider` | 4020 | REST 接口、JWT 资源服务器与 Polaris 服务注册 |
| Tencent | `macula-example-tencent-consumer` | 4010 | OpenFeign 服务调用与 Polaris 服务发现 |
| 任务 | `macula-example-task` | 7099 | XXL-JOB 与 SnailJob 执行器 |
| Binlog | `macula-example-binlog4j` | - | MySQL binlog 订阅与事件处理 |

## 环境要求

- JDK 17、Maven 3.9+。
- Alibaba 链路：本地 Nacos（默认 `127.0.0.1:8848`）；Sentinel Dashboard 为可选项。
- Tencent 链路：本地 Polaris（默认 `grpc://127.0.0.1:8091`）。
- Task 示例：按需启动 Nacos、XXL-JOB Admin 和 SnailJob Server。
- Binlog4j 示例：MySQL 需开启 binlog，并准备 Redis 用于消费位点持久化。

示例中的认证信息均是占位值。真实地址、账号、密码和 token 应通过环境变量或配置中心注入，不要提交到仓库。

## 构建与检查

在仓库根目录执行：

```bash
# 编译并运行 examples 内所有模块的单元测试
mvn -f macula-boot-examples/pom.xml test

# 全仓 Java 风格检查
mvn -N checkstyle:check
```

如果只修改一个示例，优先缩小验证范围：

```bash
mvn -pl macula-boot-examples/macula-example-alibaba-provider1 -am test
```

## 启动微服务链路

Alibaba 链路按以下顺序启动：

1. Nacos。
2. `macula-example-alibaba-provider1`。
3. `macula-example-alibaba-consumer`。
4. `macula-example-alibaba-gateway`。

Tencent 链路将 Nacos 替换为 Polaris，并依次启动 provider、consumer 和 gateway。每个模块的配置项、启动命令和验证端点见其目录下的 README。

## Docker Compose 一键环境

`docker/` 提供 Docker Compose v2 环境，包含 MySQL、Redis、Nacos、Polaris，并通过 `alibaba`、`tencent` 两个 profile 运行对应的 gateway、provider、consumer。既可以完整容器化，也可以只启动 Middleware 后从本机 Maven/IDE 运行应用。

```bash
cd macula-boot-examples/docker

# Alibaba 完整链路
docker compose --profile alibaba up -d --build

# Tencent 完整链路
docker compose --profile tencent up -d --build

# Alibaba Middleware-only
docker compose up -d redis nacos-init

# Tencent Middleware-only
docker compose up -d redis polaris
```

完整命令、端口覆盖、日志、停止、数据保留与重置方式见 [`docker/README.md`](docker/README.md)。默认密码只用于回环地址绑定的本地示例，禁止用于共享或生产环境。

Docker 目录还提供独立的可观测性 overlay，通过 OpenTelemetry Collector 将 Metrics、Logs、Traces
分别送往 Prometheus、Loki、Tempo。六个可运行的 Alibaba/Tencent 示例均使用
`macula-boot-starter-observability`，默认关闭网络导出；组合 overlay 时通过环境变量开启三类信号。

## 配置约定

- 所有配置统一放在 `application.yml`；Spring Cloud Alibaba 2025.1 通过 `spring.config.import` 导入 Nacos 配置，Spring Cloud Tencent 通过 `optional:polaris` 导入 Polaris 配置。
- Alibaba 默认导入 `${spring.application.name}.yml` 和 `${spring.application.name}-${spring.profiles.active}.yml`，保留基础配置与环境配置两层覆盖关系。
- 可变的基础设施参数使用 `${ENV_NAME:默认值}`，本地可直接运行，其他环境显式覆盖。
- Maven profile 通过 `@profile.active@` 写入 Spring profile，可选 `local`、`dev`、`stg`、`pet`、`prd`。
