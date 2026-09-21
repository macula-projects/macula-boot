# Spec: Examples Docker Compose (from intent.md 2026-09-20)
Status: accepted

Port revision accepted by the originator on 2026-09-21: Alibaba gateway/provider/consumer use `5000/5443`, `5020`, `5010`; Tencent gateway/provider/consumer use `4000`, `4020`, `4010`.

## Source intent
[Accepted intent](./intent.md): 为 Alibaba 与 Tencent 示例提供可选择完整容器化或仅启动 Middleware 的 Docker Compose v2 运行方式。

## Requirements
1. `macula-boot-examples/docker/docker-compose.yml` 必须通过 Docker Compose v2 解析，并公开且仅公开 `alibaba`、`tencent` 两个业务 profile。
2. 启用 `alibaba` profile 时，必须能够启动 MySQL、Redis、Nacos、`macula-example-alibaba-provider1`、`macula-example-alibaba-consumer` 和 `macula-example-alibaba-gateway`；不得启动 Tencent 应用或 Polaris。
3. 启用 `tencent` profile 时，必须能够启动 MySQL、Redis、Polaris、`macula-example-tencent-provider`、`macula-example-tencent-consumer` 和 `macula-example-tencent-gateway`；不得启动 Alibaba 应用或 Nacos。
4. 使用者必须能够只启动 Alibaba Middleware（MySQL、Redis、Nacos）或 Tencent Middleware（MySQL、Redis、Polaris），且此时六个示例应用容器均不得运行，以便从宿主机通过 Maven 或 IDE 启动应用。
5. 同时启用 `alibaba` 与 `tencent` profile 时，两条链路必须能够共享 MySQL、Redis 和 Compose 网络，并在现有默认端口互不冲突地同时运行。
6. 所有长驻服务必须有健康检查；依赖服务必须等待其关键依赖健康后再启动，启动失败必须能从 Compose 状态或容器日志中被观察到。
7. Alibaba 完整模式必须通过以下可观察行为验收：provider 健康；consumer 经 Nacos 发现 provider；访问 `http://127.0.0.1:5000/consumer/api/v1/consumer/echo/demo?str=hello` 返回 HTTP 2xx，响应同时包含 provider 回声和 `demo`。
8. Tencent 完整模式必须通过以下可观察行为验收：provider 健康；consumer 经 Polaris 发现 provider；访问 `http://127.0.0.1:4000/consumer/api/v1/consumer/echo` 返回 HTTP 2xx，响应包含 provider 对 `consumer` 的回声。
9. Middleware-only 模式必须保持现有宿主机连接约定：Nacos 可通过 `127.0.0.1:8848` 访问，Polaris 可通过 `grpc://127.0.0.1:8091` 访问，MySQL 和 Redis 分别可通过 `127.0.0.1:3306`、`127.0.0.1:6379` 访问；端口必须可由环境变量覆盖。
10. 所有运行镜像必须固定到明确版本，不得使用 `latest`；所选镜像及应用运行时基础镜像必须提供 `linux/amd64` 与 `linux/arm64` manifest。
11. Compose 必须可在 macOS 和 Windows 的 Docker Compose v2 环境中运行，不依赖 Bash、GNU 专用命令或宿主机路径语法；Windows 支持边界见 Flagged concerns。
12. 默认凭据只能用于本地示例，服务端口默认仅绑定宿主机回环地址；不得提交真实凭据、token 或外部服务密钥。
13. MySQL、Redis 以及注册/配置中心需要持久化的数据必须使用命名卷；文档必须说明普通停止与清空数据重建的区别。
14. `macula-boot-examples/README.md` 与 `macula-boot-examples/docker/README.md` 必须给出完整模式、Middleware-only 模式、同时启动两条链路、查看状态/日志、停止以及重置数据的可复制命令。
15. 容器化所需的地址调整必须保留本机 Maven/IDE 启动行为；未设置容器环境变量时，应用使用 2026-09-21 经 originator 确认的新端口映射，注册中心地址、namespace 和覆盖机制不得回归。

## Non-goals
- 首期不容器化或编排 `macula-example-task`、`macula-example-binlog4j`、`macula-example-alibaba-provider2`。
- 不提供 Kubernetes、Helm、Swarm、生产集群、高可用、TLS 证书管理或云环境部署方案。
- 不把示例镜像发布到远程镜像仓库；应用镜像由本地源码构建。
- 不新增业务 API，不改变 provider、consumer、gateway 的业务行为。
- 不验证 GAPI、iPaaS、OSS、OAuth2 外部授权服务器或 Sentinel Dashboard 等非本地调用链必需能力。
- 不承诺原生 Windows container 镜像；该边界必须由 reviewer 明确确认。

## Design
### Governing policies
- 根 `AGENTS.md`：保持 Java 17、聚焦 `macula-boot-examples`、同步 README、不得提交凭据或生成物、按风险逐步验证。
- `.agents/rules/architecture.md`：examples 只作为可运行集成示例，不得成为框架模块的依赖来源；Alibaba/Tencent 特有配置保持隔离。
- `.agents/rules/testing.md`：外部 MySQL、Redis、Nacos、Polaris 验证属于集成验证；缺少运行环境时必须明确报告，不得误判为代码失败。
- `.agents/rules/dependencies-release.md`：第三方版本应明确管理；本变更不得触发发布流程。
- `REVIEW.md`：实现后分别执行 Bugs、Security、Compliance 三个证据驱动的 review pass；镜像依赖和公开端口视为安全相关。
- `bands.yaml`：现有 production SLO 控制带不适用于本地 examples Compose，不为本变更新增或修改生产监控带。
- `.agents/skills/macula-ai-coding/SKILL.md` 已发现但为空，没有可应用的额外组织级规则。未发现其他品牌、数据分类或合规策略。

### Compose topology
- 保留一个主文件 `macula-boot-examples/docker/docker-compose.yml`。MySQL、Redis 不分配 profile，作为共享基础服务；Nacos 与三个 Alibaba 应用分配 `alibaba` profile；Polaris 与三个 Tencent 应用分配 `tencent` profile。
- 完整 Alibaba 命令契约为 `docker compose --profile alibaba up -d --build`；完整 Tencent 命令契约为 `docker compose --profile tencent up -d --build`；两个 `--profile` 可同时使用。Compose v2 支持同时启用多个 profile，且显式点名带 profile 的服务时只会启动该服务及声明的依赖，依据 [Docker Compose profiles 文档](https://docs.docker.com/compose/how-tos/profiles/)。
- Middleware-only 不新增第三、第四个 profile，而是显式点名服务：Alibaba 启动 `mysql redis nacos`，Tencent 启动 `mysql redis polaris`。这保证业务 profile 始终代表“完整链路”，并保持 intent 要求的两个 profile。
- 所有服务加入同一个用户定义 bridge 网络。容器内通过 Compose service name 通信；宿主机通过回环地址和映射端口访问。
- MySQL 使用两个相互隔离的 schema 保存 Nacos 与 Polaris 数据；初始化 SQL 随 Docker 配置版本化，并保留上游许可证与来源说明。Redis 作为共享、可直接使用的本地 Middleware 服务运行。
- Nacos 初始化步骤创建示例所需的 `MACULA5` 与 `SENTINEL` namespace；Polaris 配置允许自动创建 `macula-dev` namespace。初始化必须幂等。

### Application images and startup flow
- 使用 `macula-boot-examples/docker/Dockerfile` 的多阶段构建，从仓库根 Maven reactor 按模块构建六个 Spring Boot 可执行 JAR，再放入 Java 17 JRE 运行镜像；Compose 为每个应用传入明确模块参数。
- 构建上下文为仓库根目录，并使用 Dockerfile 专用 ignore 文件排除 `.git`、IDE 元数据、已有 `target` 和其他无关内容。运行容器使用非 root 用户。
- 应用运行时通过环境变量把 Nacos/Polaris、网关 JWK 地址及 Sentinel Nacos 数据源从宿主机回环地址切换为 Compose service name；未设置变量时继续使用当前本机默认值。
- 配置文件中当前不一致的 Nacos 属性路径统一为可由 `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_USERNAME`、`NACOS_PASSWORD` 覆盖的契约；Tencent 链路继续使用 `POLARIS_SERVER_ADDR`、`POLARIS_NAMESPACE`。修改只涉及地址与 namespace 参数化，不改变业务逻辑。
- 启动顺序为：共享存储健康 -> 注册/配置中心健康并初始化完成 -> provider 健康 -> consumer 健康 -> gateway 健康。Compose 使用带条件的 `depends_on` 表达此顺序，但健康检查失败仍作为最终真相，而不是仅依赖容器启动顺序。

### Image and platform constraints
- 候选基础版本为 Nacos `nacos/nacos-server:v2.5.1-slim`、Polaris `polarismesh/polaris-server:v1.18.1`、MySQL 8.0 系列、Redis 7.4 系列和多架构 Java 17 JRE；最终 patch tag 在 plan 中锁定并通过 manifest 检查记录。
- [Nacos 官方 Docker 说明](https://github.com/nacos-group/nacos-docker)要求 Apple Silicon 使用带 `-slim` 的 ARM 镜像；候选 tag 已确认同时存在 `linux/amd64` 与 `linux/arm64`。[Polaris `v1.18.1`](https://hub.docker.com/r/polarismesh/polaris-server/tags)、MySQL 8.0、Redis 7.4 也已确认具有这两个平台 manifest。[MySQL 官方镜像](https://hub.docker.com/_/mysql)列出的支持架构包含 amd64 和 arm64v8，[Redis 官方镜像](https://hub.docker.com/_/redis)亦包含两者。
- Windows 验证目标为 Docker Desktop 的 Linux container 模式；Compose 文件不设置硬编码 `platform`，由 Docker 选择当前主机对应 manifest。

## Data and interfaces
- 不新增或改变 Java 公共 API、HTTP 路径、请求/响应 schema 或 Maven 依赖。
- 新增用户接口为 Compose CLI：profiles `alibaba`、`tencent`；可点名的 Middleware services `mysql`、`redis`、`nacos`、`polaris`；应用 service names 与模块名保持一一对应。
- 新增配置契约包括镜像版本、端口、MySQL/Redis 本地凭据、Nacos/Polaris 地址与 namespace。默认值写入 `.env.example` 或 Compose 插值表达式，实际 `.env` 不提交。
- 默认宿主机端口：MySQL `3306`、Redis `6379`、Nacos HTTP `8848` 与 gRPC `9848`、Polaris HTTP `8090`、服务发现 gRPC `8091`、配置 gRPC `8093`；应用使用 Alibaba provider `5020`、consumer `5010`、gateway HTTP/HTTPS `5000/5443` 与 Tencent provider `4020`、consumer `4010`、gateway `4000`。
- 命名卷至少分离 MySQL、Redis 和需要独立持久化的注册中心数据；`docker compose down` 保留数据，文档化的显式 `down -v` 才删除本地示例数据。
- MySQL/Redis/Nacos/Polaris 和应用端口默认以 `127.0.0.1:hostPort:containerPort` 发布，避免默认暴露到局域网。

## Flagged concerns
- Windows 的含义：本设计支持 Windows Docker Desktop 的 Linux container 模式，不支持原生 Windows containers；两者架构完全不同，需要 reviewer 确认前者满足 intent，blocking。
- Redis 的验收角色：当前 Alibaba/Tencent gateway-provider-consumer 调用链没有 Redis 业务依赖；本设计保证 Redis 可启动、健康、持久化并可供本机或容器使用，但不人为修改业务代码制造 Redis 调用，需要 reviewer 确认“包含 Redis”不等于“必须进入端到端调用链”，blocking。
- 端到端入口：intent 未确定可观察验收行为；本规格依据各模块 README 选定 gateway echo 路径，并要求响应证明 consumer 已调用 provider，需要 reviewer 确认该行为足以关闭原 open question，non-blocking。
- 第三方版本兼容性：候选镜像均有 amd64/arm64 manifest，但 Nacos/Polaris 服务端与当前 Spring Cloud Alibaba/Tencent 客户端的最终版本组合仍需通过真实启动与调用测试证明；失败时必须回到 plan 调整 pin，不得用架构模拟或忽略健康检查，non-blocking。
- 上游配置与 SQL：Nacos/Polaris 的数据库初始化可能需要纳入上游 SQL 和示例配置；复制时必须记录版本、来源及许可证，避免无来源 vendoring，non-blocking。
- 本地演示凭据：Nacos、MySQL、Redis 和 Polaris 需要可重复的本地默认凭据；这些值仅能用于回环地址绑定的示例环境，README 必须明确禁止复用于共享或生产环境，non-blocking。
- 配置兼容性：现有 Alibaba 配置中 `nacos.namespace`、`nacos.config.namespace`、`nacos.server-addr` 与 `nacos.config.server-addr` 使用不一致；统一参数化可能暴露当前未被测试覆盖的问题，因此必须同时验证容器模式和无环境变量的本机模式，non-blocking。

## Verification strategy
1. 静态检查：运行 `docker compose config`、`docker compose config --profiles`，分别渲染无 profile、`alibaba`、`tencent` 和双 profile 模型；确认服务集合、依赖关系、端口绑定和变量均符合要求。
2. 镜像检查：对所有固定镜像及 Java 构建/运行基础镜像运行 manifest 检查，记录 `linux/amd64`、`linux/arm64` 均存在；拒绝 `latest` 和硬编码单一 `platform`。
3. 构建检查：分别构建六个应用镜像，确认使用 Java 17、以非 root 用户运行，且未把 `.git`、源码凭据或宿主机生成物带入运行镜像。
4. Middleware-only 检查：分别执行 Alibaba 与 Tencent 的 Middleware-only 命令，等待健康后验证 MySQL、Redis、Nacos/Polaris 的宿主机端口；确认没有应用容器运行，再从宿主机启动至少 provider 验证注册中心连通。
5. Alibaba 完整链路：从空卷启动 `alibaba` profile，验证所有服务健康、Nacos namespace 初始化成功、三个应用注册完成，并调用 Requirement 7 的 gateway URL 检查 HTTP 2xx 和响应内容。
6. Tencent 完整链路：从空卷启动 `tencent` profile，验证所有服务健康、三个应用注册完成，并调用 Requirement 8 的 gateway URL 检查 HTTP 2xx 和响应内容。
7. 组合与持久化：同时启用两个 profile，确认无端口/服务名冲突；执行普通 `down` 后重启验证数据保留，再显式 `down -v` 验证可从空状态重复初始化。
8. 本机兼容：清除容器专用环境变量，按现有 README 用 Maven/IDE 启动两条链路中的应用，确认默认地址与端口未回归。
9. 项目验证：对受影响 examples 模块运行 Maven 测试，并按 `REVIEW.md` 完成 Bugs、Security、Compliance 三个 review pass；报告真实测试数和所有因平台条件未运行的项目。
10. 平台验收：至少在一台 `linux/amd64` Docker 环境和一台 `linux/arm64` Docker 环境完成完整链路 smoke test；macOS 与 Windows Docker Desktop 各完成 Compose 解析和至少一条完整链路 smoke test。无法取得某平台时必须将其报告为未验证，不得仅凭 manifest 宣称运行通过。
