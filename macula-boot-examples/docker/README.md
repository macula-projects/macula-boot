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
docker compose config --profiles
```

`.env` 可覆盖镜像、宿主机端口、namespace 和本地示例密码，已被 Git 忽略。默认密码仅供绑定在 `127.0.0.1` 的本地环境使用，禁止复用于共享环境或生产环境。

## 完整模式

启动 Alibaba 链路：

```shell
docker compose --profile alibaba up -d --build
```

验证 gateway -> consumer -> provider：

```shell
curl "http://127.0.0.1:8000/consumer/api/v1/consumer/echo/demo?str=hello"
```

启动 Tencent 链路：

```shell
docker compose --profile tencent up -d --build
```

验证 gateway -> consumer -> provider：

```shell
curl "http://127.0.0.1:4010/consumer/api/v1/consumer/echo"
```

同时启动两条链路：

```shell
docker compose --profile alibaba --profile tencent up -d --build
```

MySQL 与 Redis 由两条链路共享；Nacos 与 Alibaba 应用只属于 `alibaba` profile，Polaris 与 Tencent 应用只属于 `tencent` profile。

## Middleware-only

只启动 Alibaba Middleware 与 namespace 初始化，不启动应用容器：

```shell
docker compose up -d redis nacos-init
```

随后可从仓库根目录通过 Maven 或 IDE 启动 Alibaba 应用。默认连接地址为 Nacos `127.0.0.1:8848`、MySQL `127.0.0.1:3306`、Redis `127.0.0.1:6379`。

只启动 Tencent Middleware，不启动应用容器：

```shell
docker compose up -d redis polaris
```

随后可从仓库根目录通过 Maven 或 IDE 启动 Tencent 应用。默认 Polaris 地址为 `grpc://127.0.0.1:8091`，Spring Cloud Tencent 使用的 Nacos 兼容 HTTP/gRPC 端口为 `127.0.0.1:18849` / `127.0.0.1:19849`。

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
# Alibaba：当 NACOS_HTTP_PORT=18848时，NACOS_GRPC_PORT 应为 19848
NACOS_SERVER_ADDR=127.0.0.1:18848 \
  mvn -pl macula-boot-examples/macula-example-alibaba-provider1 spring-boot:run

# Tencent：对应 POLARIS_DISCOVERY_GRPC_PORT=18091、POLARIS_NACOS_PORT=18849
POLARIS_SERVER_ADDR=grpc://127.0.0.1:18091 \
POLARIS_NACOS_SERVER_ADDR=127.0.0.1:18849 \
  mvn -pl macula-boot-examples/macula-example-tencent-provider spring-boot:run
```

三个应用终端需使用同一组环境变量。Nacos 客户端会使用 HTTP 端口 `+1000` 的 gRPC 端口，因此 `NACOS_GRPC_PORT` 必须等于 `NACOS_HTTP_PORT + 1000`，`POLARIS_NACOS_GRPC_PORT` 必须等于 `POLARIS_NACOS_PORT + 1000`。

也可以在 IDE 中以相同顺序启动。不设置容器环境变量时，应用保持原有的 `127.0.0.1` 地址和端口默认值。

## 状态与日志

查看全部服务：

```shell
docker compose --profile alibaba --profile tencent ps
```

查看某个服务的日志：

```shell
docker compose logs -f nacos
docker compose logs -f polaris
docker compose logs -f macula-example-alibaba-gateway
```

容器按以下健康依赖启动：MySQL/Redis -> Nacos/Polaris -> provider -> consumer -> gateway。若启动失败，使用 `docker compose ps` 和对应服务日志定位第一个不健康服务。

## 停止与重置

停止并删除容器和网络，保留 MySQL、Redis 数据：

```shell
docker compose --profile alibaba --profile tencent down
```

再次执行对应的 `up` 命令即可复用原数据。

删除容器、网络和全部本地示例数据卷：

```shell
docker compose --profile alibaba --profile tencent down -v
```

`down -v` 不可恢复；只在需要从空数据库重新初始化时使用。

## 服务与默认端口

| 服务 | 宿主机地址 |
| --- | --- |
| MySQL | `127.0.0.1:3306` |
| Redis | `127.0.0.1:6379`，默认密码 `redis` |
| Nacos | `http://127.0.0.1:8848/nacos` |
| Polaris HTTP | `http://127.0.0.1:8090` |
| Polaris discovery gRPC | `grpc://127.0.0.1:8091` |
| Polaris config gRPC | `grpc://127.0.0.1:8093` |
| Polaris Nacos 兼容 HTTP / gRPC | `127.0.0.1:18849` / `127.0.0.1:19849` |
| Alibaba provider / consumer / gateway | `7081` / `7090` / `8000`、`8443` |
| Tencent provider / consumer / gateway | `4011` / `4019` / `4010` |

端口均可在 `.env` 中覆盖。容器之间使用 Compose service name 通信，不使用宿主机回环地址。

## 上游数据文件

- `mysql/init/02-nacos-mysql-schema.sql` 来自 Nacos v2.5.1，保留 Apache License 2.0 头。
- `mysql/init/03-polaris-server.sql` 与 `polaris/polaris-server.yaml` 来自 Polaris v1.18.1 Docker Compose 发布包，保留 BSD 3-Clause 来源信息。

这些文件与镜像版本绑定。升级 Nacos 或 Polaris 时，应同步核对 schema、配置及双架构 manifest，并使用空卷验证初始化。
