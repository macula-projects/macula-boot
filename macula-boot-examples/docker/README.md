# Macula Boot Examples Docker 环境

本目录通过 Docker Compose v2 提供 Alibaba 与 Tencent 两条示例链路。支持两种用法：

- 完整模式：Middleware 与 gateway、provider、consumer 全部运行在容器中。
- Middleware-only：只运行 MySQL、Redis、Nacos 或 Polaris，应用继续从本机 Maven/IDE 启动。

支持 macOS 和 Windows Docker Desktop 的 Linux container 模式，以及 `linux/amd64`、`linux/arm64`。不支持原生 Windows containers。

## 准备

在本目录执行命令：

```shell
cd macula-boot-examples/docker
cp .env.example .env
./scripts/compose.sh config all
```

`.env` 可覆盖镜像、宿主机端口、Nacos `NACOS_NAMESPACE`/`NACOS_USERNAME`/`NACOS_PASSWORD`、外部 Macula Cloud IAM 地址和本地示例密码，已被 Git 忽略。完整容器模式下 Gateway 通过 `MACULA_CLOUD_IAM_URL` 访问外部 IAM，默认使用 Docker Desktop/host-gateway 可达的 `http://host.docker.internal:9010`。本地 Nacos 关闭鉴权，客户端默认使用 `nacos/nacos`；独立的 `mysql-init` 服务补建缺失的 `nacos` 用户与 `ROLE_ADMIN` 角色，与 macula-cloud/deploy 一致。这套配置仅供回环地址绑定的本地示例使用，禁止复用于共享环境或生产环境。

已有 `.env` 若曾将 `NACOS_USERNAME`、`NACOS_PASSWORD` 改为空，请恢复为 `nacos`；应用容器需重新创建才能读取新环境变量。初始化仅补建固定的本地示例用户，不覆盖已有用户密码；自定义客户端凭据必须对应实际存在的用户，设置环境变量不会自动创建自定义账号或开启鉴权。

脚本找不到 `.env` 时会回退到 `.env.example`；也可通过 `MACULA_COMPOSE_ENV` 指定其他环境文件。原有 Docker Compose v2 命令继续兼容，脚本是推荐入口。

### 与 macula-cloud/deploy 对齐的版本

默认镜像为 MySQL `8.4.6`、Redis `7.4.5-alpine` 和 Nacos `3.2.4`。Nacos 3 的客户端 HTTP/gRPC 端口继续使用 `38848`/`39848`，独立控制台改为 `http://127.0.0.1:38088/`（容器内 `8080`），可通过 `NACOS_CONSOLE_PORT` 覆盖。客户端仍连接 `38848`，不要改为控制台端口。

Nacos 的客户端、Admin 和 Console 鉴权均关闭，客户端使用 `nacos/nacos`；token 和 identity 使用与参考目录一致的本地示例值。`nacos/init.sh` 等待 Console readiness 后，通过 v3 Console API 初始化命名空间和缺失的示例配置，已有配置不覆盖。Nacos 日志保存在 `nacos-logs` 命名卷中。保留 examples 的 Redis 密码和 Polaris 配置。

已有 `.env` 会覆盖新默认值，请同步检查 `MYSQL_IMAGE`、`REDIS_IMAGE`、`NACOS_IMAGE`，并添加 `NACOS_CONSOLE_PORT=38088`。初始化 SQL 只在空 MySQL 数据目录执行；现有卷不会自动补齐 Nacos 3.2.4 的新增表。复用旧卷前应备份并核对 schema、MySQL 升级条件及 Polaris 兼容性，不能直接重跑全量建表 SQL。本次文件升级不执行数据库迁移或删除旧卷；需要独立验证时，可使用新的 `COMPOSE_PROJECT_NAME` 和未占用的宿主机端口创建隔离环境。

### 已有数据卷补建 Nacos 用户

独立 `mysql-init` 在 MySQL 健康后执行，与只在空数据目录运行的 schema 脚本分开。Nacos 依赖它成功完成；缺少数据库或用户表时会失败，不会通过重建数据库掩盖错误。

在本目录执行下列命令，可为已运行且 schema 完整的数据库补建缺失的本地用户和角色，无需删除数据卷：

```shell
# --no-deps 避免为了补建用户而升级或重建现有 MySQL 容器。
docker compose --env-file .env --profile alibaba run --rm --no-deps mysql-init
```

没有 `.env` 时，将 `--env-file .env` 改为 `--env-file .env.example`。该命令连接当前 Compose 项目的 MySQL，使用与现有实例一致的数据库名和 root 密码。已有 `nacos` 密码或禁用状态不变；若已有账号仍登录失败，应核对原密码和状态，不会自动重置。

## 完整模式

启动 Alibaba 链路：

```shell
./scripts/compose.sh up-apps alibaba
```

先验证普通 gateway -> consumer 访问：

```shell
curl --get 'http://127.0.0.1:5099/consumer/api/v1/consumer/plain' --data-urlencode 'str=hello'
```

加密签名的 gateway -> consumer -> provider 链路使用 `http://127.0.0.1:5099/crypto-demo/index.html` 中的 GET 表单；保护接口不再支持缺加密签名头的明文 curl。

启动 Tencent 链路：

```shell
./scripts/compose.sh up-apps tencent
```

验证 gateway -> consumer -> provider：

```shell
curl "http://127.0.0.1:4000/consumer/api/v1/consumer/echo"
```

同时启动两条链路：

```shell
./scripts/compose.sh up-apps all
```

MySQL 与 Redis 由两条链路共享；Nacos 与 Alibaba 应用只属于 `alibaba` profile，Polaris 与 Tencent 应用只属于 `tencent` profile。
完整模式只向宿主机发布 Gateway；provider 和 consumer 仅在 Compose 网络内提供服务，并继续由容器内 healthcheck 检查。

## Middleware-only

只启动 Alibaba Middleware 与 namespace 初始化，不启动应用容器：

```shell
./scripts/compose.sh up alibaba
```

随后可从仓库根目录通过 Maven 或 IDE 启动 Alibaba 应用。默认宿主机地址为 Nacos `127.0.0.1:38848`、MySQL `127.0.0.1:33306`、Redis `127.0.0.1:36379`。

只启动 Tencent Middleware，不启动应用容器：

```shell
./scripts/compose.sh up tencent
```

随后可从仓库根目录通过 Maven 或 IDE 启动 Tencent 应用。默认 Polaris 地址为 `grpc://127.0.0.1:38091`，Spring Cloud Tencent 使用的 Nacos 兼容 HTTP/gRPC 端口为 `127.0.0.1:38849` / `127.0.0.1:39849`。

Redis 已启动、健康并持久化，但当前两条 gateway-provider-consumer 回声链路不会主动访问 Redis。

### 从本机运行应用

先在仓库根目录安装六个示例及其依赖：

```shell
cd ../..
mvn -pl macula-boot-examples/macula-example-alibaba-provider1,macula-boot-examples/macula-example-alibaba-consumer,macula-boot-examples/macula-example-alibaba-gateway,macula-boot-examples/macula-example-tencent-provider,macula-boot-examples/macula-example-tencent-consumer,macula-boot-examples/macula-example-tencent-gateway -am install -DskipTests
```

启动对应 Middleware-only 环境后，按 provider -> consumer -> gateway 的顺序在三个终端执行。Alibaba：

```shell
mvn -pl macula-boot-examples/macula-example-alibaba-provider1 spring-boot:run
mvn -pl macula-boot-examples/macula-example-alibaba-consumer spring-boot:run
mvn -pl macula-boot-examples/macula-example-alibaba-gateway spring-boot:run
```

Tencent：

```shell
mvn -pl macula-boot-examples/macula-example-tencent-provider spring-boot:run
mvn -pl macula-boot-examples/macula-example-tencent-consumer spring-boot:run
mvn -pl macula-boot-examples/macula-example-tencent-gateway spring-boot:run
```

如果在 `.env` 中覆盖了 Middleware 宿主机端口，Maven/IDE 进程不会自动读取该文件，需同步传入应用连接变量。例如：

```shell
# Alibaba：例如改成 NACOS_HTTP_PORT=48848 时，NACOS_GRPC_PORT 应为 49848
NACOS_SERVER_PORT=48848 \
  mvn -pl macula-boot-examples/macula-example-alibaba-provider1 spring-boot:run

# Tencent：例如改成 POLARIS_DISCOVERY_GRPC_PORT=48091、POLARIS_NACOS_PORT=48849
POLARIS_SERVER_ADDR=grpc://127.0.0.1:48091 \
POLARIS_NACOS_SERVER_ADDR=127.0.0.1:48849 \
  mvn -pl macula-boot-examples/macula-example-tencent-provider spring-boot:run
```

三个应用终端需使用同一组环境变量。Nacos 客户端会使用 HTTP 端口 `+1000` 的 gRPC 端口，因此 `NACOS_GRPC_PORT` 必须等于 `NACOS_HTTP_PORT + 1000`，`POLARIS_NACOS_GRPC_PORT` 必须等于 `POLARIS_NACOS_PORT + 1000`。

也可以在 IDE 中以相同顺序启动。三个 Alibaba 应用的 local/docker 配置默认使用 `nacos/nacos`，请先完成用户初始化。若 IDE 中曾设置空的 `NACOS_USERNAME`、`NACOS_PASSWORD` 或对应启动参数，请移除空值覆盖或恢复为实际凭据；Maven/IDE 不会自动读取 Docker 的 `.env`。应用保持原有的 `127.0.0.1` 地址和端口默认值。

## 可观测性 Overlay

将 overlay 与任一现有 profile 组合即可。Overlay 会为应用叠加 `observability` Spring Profile；
Metrics、Traces、Logs 的开关、Collector 地址、采样率与导出周期均直接维护在各应用的
`application.yml`，Compose 不再注入应用侧 OTEL 配置：

```shell
docker compose -f docker-compose.yml -f observability/docker-compose.observability.yml \
  --profile alibaba up -d --build
```

Tencent 链路将 profile 改为 `tencent`。默认宿主机入口为 Collector OTLP/HTTP `34318`、Prometheus `39090`、
Loki `33100`、Tempo `33200`，均只绑定 `127.0.0.1`。验证配置与端到端关联：

```shell
docker compose -f docker-compose.yml -f observability/docker-compose.observability.yml \
  --profile alibaba config
observability/verify-observability.sh \
  "http://127.0.0.1:5099/consumer/api/v1/consumer/echo/demo?str=hello"
```

脚本需要 Node.js 18+、curl 和 jq，并复用 Gateway 样板 request.js 自动加密签名；实际 Web Crypto 支持以当前 Node 版本为准。脚本针对 Alibaba 的确定性回声链路，读取响应 `x-traceId`，随后校验 Tempo 中的链路、Loki 中同一
Trace ID 的同步与受管异步日志，以及 Prometheus 中带 Consumer 服务身份的指标。Tencent overlay 使用
同一套 Collector 与后端配置，但不作为该关联脚本的默认验收链路。验证环境强制使用
`management.tracing.sampling.probability=1.0`；生产采样率与容量不以本示例为基线。
Collector、Loki 和 Tempo 的运行镜像不包含 HTTP 客户端，因此 overlay 使用固定版本的
`curlimages/curl` 探针容器分别查询 Collector health extension、Loki `/ready` 和 Tempo `/ready`；
`docker compose ps` 中对应的 `*-health` 服务为 `healthy` 才表示后端真正就绪。

## 状态与日志

查看全部服务：

```shell
./scripts/compose.sh status all
```

查看某个服务的日志：

```shell
./scripts/compose.sh logs alibaba nacos
./scripts/compose.sh logs tencent polaris
./scripts/compose.sh logs alibaba macula-example-alibaba-gateway
```

容器按以下健康依赖启动：MySQL -> mysql-init -> Nacos -> nacos-init -> Alibaba provider -> consumer -> gateway；Tencent 仍为 MySQL -> Polaris -> provider -> consumer -> gateway，Redis 作为相关应用的健康依赖。若启动失败，使用 `docker compose ps` 和对应服务日志定位第一个不健康服务。

## 停止与重置

停止并删除容器和网络，保留 MySQL、Redis 数据：

```shell
./scripts/compose.sh down all
```

再次执行对应的 `up` 命令即可复用原数据。

删除容器、网络和全部本地示例数据卷：

```shell
./scripts/compose.sh reset all --confirm
```

`reset --confirm` 不可恢复；只在需要从空数据库重新初始化时使用。缺少 `--confirm` 时脚本会拒绝执行。

## 服务与默认端口

| 服务 | 宿主机地址 |
| --- | --- |
| MySQL | `127.0.0.1:33306` |
| Redis | `127.0.0.1:36379`，默认密码 `redis` |
| Nacos Console | `http://127.0.0.1:38088/` |
| Nacos 客户端 HTTP / gRPC | `127.0.0.1:38848` / `127.0.0.1:39848` |
| Polaris HTTP | `http://127.0.0.1:38090` |
| Polaris discovery gRPC | `grpc://127.0.0.1:38091` |
| Polaris config gRPC | `grpc://127.0.0.1:38093` |
| Polaris Nacos 兼容 HTTP / gRPC | `127.0.0.1:38849` / `127.0.0.1:39849` |
| Alibaba Gateway | `http://127.0.0.1:5099` / `https://127.0.0.1:5443` |
| Tencent Gateway | `http://127.0.0.1:4000` |
| Collector OTLP/HTTP | `http://127.0.0.1:34318` |
| Prometheus / Loki / Tempo | `39090` / `33100` / `33200` |

表中的宿主机端口均可在 `.env` 中覆盖。provider、consumer、Collector gRPC 与 Collector health 端口不向宿主机发布；容器之间使用 Compose service name 和容器端口通信，不使用宿主机回环地址，也不需要额外声明 `expose`。

## 上游数据文件

- `mysql/init/02-nacos-mysql-schema.sql` 与 `macula-cloud/deploy/init/mysql/nacos-mysql.sql` 对齐，来自 Nacos v3.2.4（来源 commit 见文件头），保留 Apache License 2.0 头及参考目录的 utf8mb4 字符集设置。
- `mysql/init/03-polaris-server.sql` 与 `polaris/polaris-server.yaml` 来自 Polaris v1.18.1 Docker Compose 发布包，保留 BSD 3-Clause 来源信息。

这些文件与镜像版本绑定。升级 Nacos 或 Polaris 时，应同步核对 schema、配置及双架构 manifest，并使用空卷验证初始化。
