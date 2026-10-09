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
| 任务 | `macula-example-task` | 7099 | 单 SnailJob Server 下的原生与 XXL 兼容执行器 |
| Binlog | `macula-example-binlog4j` | - | MySQL binlog 订阅与事件处理 |

## 环境要求

各示例的 `application.yml` 是配置入口，下面列出常用覆盖项；Maven/IDE 不会自动读取 Docker `.env`。Cloud Starter 的配置说明分别对照 [Alibaba Provider](macula-example-alibaba-provider1/src/main/resources/application.yml)、[Alibaba Gateway](macula-example-alibaba-gateway/src/main/resources/application.yml)、[Tencent Provider](macula-example-tencent-provider/src/main/resources/application.yml) 和 [Tencent Gateway](macula-example-tencent-gateway/src/main/resources/application.yml)。

云平台示例按 profile 分层：公共段定义应用名和 Config Data 导入；`local` 提供本地开发默认值；`docker` 通过 `spring.profiles.group.docker=local` 复用 local 并覆盖连接信息；`dev/stg/pet/prd` 主要保留配置中心连接信息。`observability` 是额外叠加的导出配置，不是普通环境默认开启。

| 配置 / 环境变量 | 说明 |
| --- | --- |
| `server.port` / `SERVER_PORT` | 应用 HTTP 端口；支持占位变量的模块可直接覆盖 |
| `NACOS_SERVER_PORT` | Alibaba local 的本机 Nacos 端口，默认 `38848` |
| `NACOS_SERVER_ADDR` / `NACOS_NAMESPACE` | Alibaba docker / 共享环境的完整地址及命名空间；local 远程地址直接覆盖 `spring.config.nacos.server-addr` |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | Nacos 认证信息 |
| `POLARIS_SERVER_ADDR` / `POLARIS_NAMESPACE` | Tencent Polaris 地址及命名空间 |
| `POLARIS_NACOS_SERVER_ADDR` | Polaris 的 Nacos 兼容服务发现地址 |
| `spring.data.redis.*` | Redis 连接地址、数据库及认证信息 |
| `management.otlp.metrics.export.*` | Metrics 导出开关和 URL |
| `management.tracing.export.otlp.enabled` / `management.tracing.sampling.probability` | Trace 导出开关 / 采样比例 |
| `management.logging.export.otlp.enabled` | Logs 导出开关 |
| `management.opentelemetry.tracing.export.otlp.endpoint` / `management.opentelemetry.logging.export.otlp.endpoint` | Trace / Logs endpoint |

OTLP 网络导出在云平台示例的普通环境默认关闭，Docker observability overlay 开启；不要把示例值当成 Starter 默认值。

- JDK 17、Maven 3.9+。
- Alibaba 链路：本地 Nacos（默认 `127.0.0.1:38848`）；Sentinel Dashboard 为可选项。
- Tencent 链路：本地 Polaris（默认 `grpc://127.0.0.1:38091`）。
- Task 示例：按需启动 Nacos 和 SnailJob 1.9 Server；不需要 XXL-JOB Admin。
- Binlog4j 示例：MySQL 需开启 binlog，并准备 Redis 用于消费位点持久化。

示例中的认证信息均是占位值。真实地址、账号、密码和 token 应通过环境变量或配置中心注入，不要提交到仓库。
Examples 默认使用 `3xxxx` 中间件宿主机端口，可与使用标准端口的 Macula Cloud 同时运行；容器内部仍使用组件标准端口。

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
cp .env.example .env

# Alibaba Middleware-only，应用从 Maven/IDE 启动
./scripts/compose.sh up alibaba

# Alibaba 完整链路
./scripts/compose.sh up-apps alibaba

# Tencent Middleware-only
./scripts/compose.sh up tencent

# Tencent 完整链路
./scripts/compose.sh up-apps tencent
```

脚本在 `.env` 不存在时自动使用 `.env.example`，并支持 `MACULA_COMPOSE_ENV` 指定其他环境文件。原有 `docker compose --profile ...` 命令继续兼容。完整命令、端口覆盖、日志、停止、数据保留与重置方式见 [`docker/README.md`](docker/README.md)。默认密码只用于回环地址绑定的本地示例，禁止用于共享或生产环境。
完整容器模式只向宿主机发布 Alibaba/Tencent Gateway；provider 与 consumer 的模块端口仍用于 IDE 运行和容器内部通信，不由 Compose 映射到宿主机。

Docker 目录还提供独立的可观测性 overlay，通过 OpenTelemetry Collector 将 Metrics、Logs、Traces
分别送往 Prometheus、Loki、Tempo。六个可运行的 Alibaba/Tencent 示例均使用
`macula-boot-starter-observability`，默认关闭网络导出；组合 overlay 时叠加应用内的
`observability` Spring Profile 开启三类信号，不由 Compose 注入 OTEL 应用参数。

## 配置约定

- 所有配置统一放在 `application.yml`；Spring Cloud Alibaba 2025.1 通过 `spring.config.import` 导入 Nacos 配置，Spring Cloud Tencent 通过 `optional:polaris` 导入 Polaris 配置。
- Alibaba 默认导入 `${spring.application.name}.yml` 和 `${spring.application.name}-${spring.profiles.active}.yml`，保留基础配置与环境配置两层覆盖关系。
- 可变的基础设施参数使用 `${ENV_NAME:默认值}`，本地可直接运行，其他环境显式覆盖。
- Maven profile 通过 `@profile.active@` 写入 Spring profile，可选 `local`、`dev`、`stg`、`pet`、`prd`。
- 容器固定激活 Spring `docker` profile；该 profile 继承 `local` 默认值，只覆盖 Compose DNS 和容器端口。
