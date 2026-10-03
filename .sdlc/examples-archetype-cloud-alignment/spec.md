# Spec: Examples 与 Archetype 云端部署规范对齐 (from intent.md 2026-10-01)
Status: accepted

## Source intent
[Accepted intent](./intent.md)：保留 Alibaba、Tencent 两套示例链路，统一本地部署规范，并让 Archetype 生成项目具备 `.env` 驱动的 IDE/完整容器运行能力以及修复后的 Admin 登录流程。

## Requirements
1. `macula-boot-examples` 必须保留现有 `alibaba`、`tencent` 两个且仅两个业务 profile，以及两条已经验收的 gateway -> consumer -> provider 调用链、端口和 Middleware-only 能力。
2. Examples 必须提供与 `macula-cloud/deploy/scripts/compose.sh` 一致风格的单一脚本入口，支持配置渲染、按 Alibaba/Tencent/全部链路启动 Middleware、构建和启动完整应用、查看状态与日志、普通停止，以及带显式确认的清空数据操作；脚本在 `.env` 不存在时必须回退到 `.env.example`。
3. Examples 的 `.env.example`、Compose 插值和应用 `application.yml` 必须明确区分宿主机发布端口、容器监听端口及容器内服务地址，默认仍只绑定 `127.0.0.1`，覆盖宿主机端口不得改变容器内端口。
4. 六个可运行 Examples 应用必须采用公共配置、`local`、`docker`、`dev`、`stg`、`pet`、`prd` 分层；`docker` 必须继承本地默认行为并只覆盖 Compose DNS/容器端口，共享环境只在仓库保存注册/配置中心连接参数。
5. Examples 的现有可观测性 overlay 必须继续能够分别与 Alibaba、Tencent profile 组合，不得因部署脚本或 profile 重构而改变三类 OTLP 信号的默认关闭状态或既有验证入口。
6. Archetype 生成项目必须包含完整 `deploy/` 目录，至少包括 `.env.example`、Compose 文件、启动脚本、MySQL/Nacos 初始化资源和部署 README；它必须支持“基础设施容器 + 本机 Maven/IDE 应用”和 `apps` profile 下的完整容器模式。
7. Archetype 的默认基础设施必须包含 MySQL、Redis、Nacos 及幂等初始化任务；完整容器模式必须覆盖生成项目中所有可运行后端模块和 Admin，按存储/注册中心 -> 业务服务 -> Gateway -> Admin 的健康依赖顺序启动。
8. 生成项目的 Java Dockerfile 必须从生成项目根目录完成 Maven reactor 构建，运行时使用 Java 17 JRE、非 root 用户和单一可执行 JAR；Admin Dockerfile 必须使用锁文件安装依赖、多阶段构建和非 root Nginx，并提供 SPA fallback 与 `/api`、`/iam` 同源代理。
9. Archetype 中所有可运行后端模块的 `application.yml` 必须使用公共、`local`、`docker`、`dev`、`stg`、`pet`、`prd` 分层；不得保留固定内网地址、真实账号或要求生成后手改源码的配置项。
10. 生成项目的数据库初始化必须可从空 volume 重复执行：部署初始化只负责创建应用账号和 schema，业务 schema 由所属服务的版本化 migration 管理；不得依赖手工导入 `docs/*.sql`。
11. Archetype metadata 必须完整包含新增的隐藏环境样例、Shell 脚本、Compose/YAML、Nginx、Docker ignore、前端锁文件和数据库 migration，并正确处理 `${rootArtifactId}`、`${version}`、`${symbol_dollar}` 与 Compose `${...}`，保证生成后不残留 Velocity 指令或被错误替换的环境变量。
12. Admin 必须同步 `macula-cloud` 提交 `e314b55` 中与登录修复直接相关的行为：密码授权参数以 `application/x-www-form-urlencoded` 请求体提交；登录页自行展示 IAM 错误；通用请求层可按请求关闭重复通知；开发模式分别代理 `/api` 与 `/iam`。
13. Admin 必须增加覆盖登录页可访问性和 IAM 失败反馈的前端测试；失败后登录按钮必须恢复可用，并优先展示后端 `msg`、`message`、`error_description` 或 `error`。
14. Admin 仍只保留当前轻量 service1 管理示例，不同步 `macula-cloud-admin` 的租户、用户、角色、菜单、字典、日志、应用维护人等完整系统管理页面。
15. 在认证依赖方案经人工决定后，生成项目必须仅通过填写 `.env` 即可完成：启动环境、使用示例账号登录 Admin、获取当前用户与菜单、通过生成项目 Gateway 调用 `/admin/api/v1/app`，且无需修改生成后的源码。
16. Examples 与 Archetype README 必须分别记录前置条件、两种运行模式、IDE 启动顺序、端口和变量契约、状态/日志命令、登录与管理接口验收、普通停止及不可恢复的显式重置操作。
17. 所有默认账号、密码、client secret 和 token 必须是明确标注的本地示例值；真实凭据不得提交，公开端口不得默认绑定非回环地址。
18. 变更不得修改 Macula Boot 公共 Java API，不得让 examples 成为框架依赖来源，并必须保持现有 Maven reactor、Archetype 生成、Admin 构建以及 Alibaba/Tencent 无容器本机启动方式可用。

## Non-goals
- 不把 `macula-cloud` 的 Gateway、IAM、System、TinyID、Seata、SnailJob、RocketMQ 或 Docs 业务实现复制进 `macula-boot`。
- 不把 `macula-cloud-admin` 的完整系统管理功能同步进 Archetype Admin。
- 不新增第三条云厂商示例链路，也不容器化 Examples 中的 Task、Binlog4j 或预留 Provider2 模块。
- 不提供 Kubernetes、Helm、生产高可用、证书、备份、灾备或远程镜像发布方案。
- 不改变现有 Examples 业务 API、响应模型或 Java 公共接口。
- 不以本次工作确定生产 SLO；项目根 `bands.yaml` 中的示例控制带不作为本地部署验收标准。

## Design
### Governing policies
- 根 `AGENTS.md`：保持 Java 17 和现有代码风格；先保护工作区；行为、配置和公共契约变化同步 README 与测试；不得发布、推送或提交凭据。
- `.agents/rules/architecture.md`：Examples 仅作为可运行集成示例；Alibaba/Tencent 配置保持隔离；Archetype 必须保留占位符、多模块关系和生成后路径。
- `.agents/rules/testing.md`：纯逻辑测试不得依赖外部服务；Docker、MySQL、Redis、Nacos、Polaris 和真实 HTTP 链路属于集成验证，环境缺失必须明确报告。
- `.agents/rules/dependencies-release.md`：依赖版本继续由 parent 管理；POM 变更扩大 Maven 验证；不触发发布流程。
- `REVIEW.md`：实现后分别执行 Bugs、Security、Compliance 三个证据驱动的 review pass，登录、凭据、代理、镜像和公开端口按安全边界检查。
- `bands.yaml`：当前内容是待替换的示例 production 控制带，与本地开发部署没有可执行约束。
- `.agents/skills/macula-ai-coding/SKILL.md` 已发现但内容为空，没有额外可应用的组织规则；未发现其他品牌、数据分类或合规政策。

### Examples deployment
- 保留 `macula-boot-examples/docker/docker-compose.yml` 及其 `alibaba`、`tencent` profile 语义，不新增 `apps` profile。现有直接执行 `docker compose --profile ...` 的命令继续有效。
- 在 `docker/scripts/compose.sh` 增加统一入口。脚本把 `alibaba`、`tencent`、`all` 映射为现有 profile；Middleware 模式显式点名 MySQL、Redis、Nacos/Nacos-init 或 Polaris，完整模式启用对应 profile 并构建应用。
- 脚本使用 POSIX `sh`，通过自身路径定位 Compose 文件，并允许 `MACULA_COMPOSE_ENV` 指定环境文件；重置数据必须要求 `--confirm`。
- 六个应用的基础段只保存应用名、监听端口、注册中心公共引用及环境无关配置。`local` 保存 IDE 可直接运行的回环地址；`docker` 通过 profile group 继承 `local`，再覆盖 Nacos/Polaris、Redis、JWT/IAM 等容器地址；共享环境仅保留注册/配置中心入口。
- Compose 为应用固定传入 `SPRING_PROFILES_ACTIVE=docker`，并继续使用 service name 通信。现有主 Compose 与 observability overlay 的 service name、网络和 OTLP 环境变量保持兼容。

### Generated project deployment
- Archetype 生成项目新增根级 `deploy/`。默认启动 MySQL、Redis、Nacos 及一次性初始化任务；`apps` profile 启动 `${rootArtifactId}-service1`、Admin BFF、Basic、OpenAPI、Thirdparty、Gateway 和 Admin。
- `deploy/scripts/compose.sh` 与 Macula Cloud 入口保持同一命令族：`config`、`config-apps`、`up`、`up-apps`、`build`、`status`、`logs`、`down`、`reset --confirm`。默认 `up` 只启动基础设施和初始化任务。
- Java 镜像使用每模块 Dockerfile，但共享“根 reactor 构建 -> 精确选择非 sources/javadoc/original JAR -> Java 17 非 root runtime”契约。构建上下文为生成项目根目录，Docker ignore 排除 `.git`、IDE 文件、`target`、前端 `node_modules/dist` 和本地 `.env`。
- Admin 使用 Node 20 + `npm ci` 构建和 `nginxinc/nginx-unprivileged` 运行。Nginx 在 8080 提供静态文件，把 `/api/` 转发到生成项目 Gateway，把 `/iam/` 转发到最终选定的认证服务，并为前端路由提供 `try_files ... /index.html`。
- 所有 Java 应用由 Compose 激活 `docker` profile；`SERVER_PORT` 表示容器监听端口，`*_HOST_PORT` 只控制宿主机发布端口。每个服务通过自身 `application.yml` 维护数据库名、Redis database、注册中心和下游地址。
- Service1 的现有示例表从手工 SQL 迁入 `src/main/resources/db/migration/V1__baseline.sql`，由 Service1 独占 migration；MySQL 初始化脚本只创建应用账号、Nacos schema 和生成项目业务 schema。Migration 必须修正现有 `DROP TABLE sapplication` 与 `CREATE TABLE application` 不一致并保证首次启动可重复。
- Nacos 初始化负责创建本地 namespace 和必要的最小配置，不覆盖已有配置，除非 `.env` 明确启用 force 开关。

### Admin login and request flow
- Admin 的开发和容器构建统一使用相对路径：业务请求经 `/api`，认证请求经 `/iam`。Vite 在 IDE 模式代理到 `.env.development` 指定的 Gateway/IAM；Nginx 在容器模式代理到 Compose 服务。
- `systemToken.post(data)` 把授权参数编码成 `URLSearchParams`，设置 `Content-Type: application/x-www-form-urlencoded`，并通过 `showErrorNotification: false` 避免通用拦截器与登录表单双重提示。
- 密码登录表单捕获认证错误、恢复 loading 状态并展示服务端错误。成功后维持现有顺序：保存 access token -> 请求 `/admin/api/v1/users/me` -> 请求 `/admin/api/v1/menus/routes` -> 保存菜单/权限 -> 进入首页。
- 业务验收接口确定为现有 `/admin/api/v1/app`：Gateway 去除 `/admin` 前缀转发到 Admin BFF，Admin BFF 通过现有 Feign client 调用 Service1。该选择关闭 intent 中“管理接口由 Design 确定”的开放问题，不新增 API。
- 认证服务只按接口契约接入，不在本规格中偷偷指定实现：`POST /oauth2/token` 接受 password grant 表单并返回 `access_token` 或标准错误字段；token 必须能支持 Gateway/Admin BFF 获取当前用户和菜单。实际提供者取决于阻塞 concern 的人工决策。

## Data and interfaces
- 不新增或改变 Macula Boot 公共 Java API。现有 Examples echo API 和生成项目的 `/admin/api/v1/app` 请求/响应保持兼容。
- 新增 Examples CLI 契约为 `docker/scripts/compose.sh`，接受部署动作以及 `alibaba|tencent|all` 链路参数；原始 Compose CLI 仍受支持。
- 新增生成项目 CLI 契约为 `deploy/scripts/compose.sh {up|up-apps|build|status|logs|down|reset --confirm|config|config-apps}`。
- 新增配置契约包括 `BIND_ADDRESS`、基础镜像、MySQL/Redis/Nacos、本机发布端口、应用镜像与端口、Admin 端口、认证地址和本地示例凭据。`.env.example` 被提交，实际 `.env` 被忽略。
- Profiles 契约为 Maven 的 `local/dev/stg/pet/prd` 与 Spring 的 `local/docker/dev/stg/pet/prd`；`docker -> local` profile group 只用于继承默认配置，不新增 Maven `docker` profile。
- Admin 接口契约：`POST /iam/oauth2/token` 使用 form-urlencoded；`GET /api/admin/api/v1/users/me`、`GET /api/admin/api/v1/menus/routes` 获取会话上下文；`GET /api/admin/api/v1/app` 验证管理调用。开发代理和 Nginx 会在转发时去除 `/api`、`/iam` 前缀。
- Service1 schema 由 Flyway `V1__baseline.sql` 管理；Nacos schema 仍由部署初始化资源管理。后续业务 schema 变更必须新增 migration，不得修改已执行版本。
- Compose volume 的普通 `down` 保留数据；只有 `reset --confirm` 删除 volume。该操作不可恢复，脚本和 README 必须同时警告。

## Flagged concerns
- 生成项目缺少认证与系统数据提供者：当前 Archetype 没有 OAuth2 Authorization Server、用户或菜单服务，`macula-boot-starter-system` 只是调用外部 Macula Cloud System；因此仅同步 Admin 登录修复无法满足“只配置 `.env` 即成功登录”的 Requirement 15。Rain 必须在 Build 前选择：把 Macula Cloud IAM/System 作为明确的外部/镜像依赖、授权新增轻量本地开发认证与静态用户菜单能力，或修改已接受的成功登录目标；blocking。
- 自包含边界：若选择依赖 Macula Cloud IAM/System，生成项目将不再是仅由当前 Archetype 源码构建的自包含环境，并需要可取得的镜像、数据库 migration、client 注册和版本兼容承诺；若选择轻量认证模块，则会扩大当前“不复制 Cloud 业务实现”的范围，需要单独确认安全边界；blocking。
- Examples 已有已验收部署契约：`examples-docker-compose` 已处于 Maintain，脚本和 profile 分层必须保持其两个 profile、端口、Middleware-only、双链路和 observability 证据；这是兼容性风险但可通过回归验证控制，non-blocking。
- Nacos 版本差异：Examples 当前固定 Nacos 2.5.1，而 Macula Cloud deploy 使用 3.2.4；本规格对齐部署规范而不自动升级版本，后续若升级必须单独验证客户端、schema、健康检查和双架构镜像，non-blocking。
- 前端可重复构建：Archetype Admin 当前未跟踪 `package-lock.json`，而目标 Dockerfile 要求 `npm ci`；实现必须生成并纳入与 `package.json` 一致的锁文件，且不能复制 `node_modules` 或构建产物，non-blocking。
- 本地演示凭据：成功登录需要可重复的示例账号和 OAuth client，但其密码/client secret 只能用于回环地址绑定的本地环境；README 必须明确禁止复用于共享和生产环境，non-blocking。
- 交付期限：intent 未指定期限或发布版本，不影响规格安全性，但计划阶段不能假定必须进入某个发布窗口，non-blocking。
- 未实施风险：intent 未量化跳过改造的业务成本；本规格以可重复开发环境和减少生成后手工修改为工程价值，不据此声明业务收益或优先级，non-blocking。

## Verification strategy
1. Artifact inspection：检查 Archetype metadata 与一次真实生成结果，确认隐藏文件、脚本执行内容、YAML、migration、Docker/Nginx 文件和前端锁文件齐全，且不残留 Velocity 指令、不丢失 Compose 环境变量。
2. Static deployment checks：对 Examples 的 Alibaba、Tencent、双 profile、observability overlay，以及生成项目的默认与 `apps` profile 分别执行 `docker compose config --quiet` 和 service/profile 清单断言；对所有 Shell 脚本执行 `sh -n`。
3. Maven checks：从最小相关模块开始运行 `mvn -pl macula-boot-examples,macula-boot-archetype -am test`；生成 Archetype 项目后运行其 Maven test/package，若修改 POM 或 lifecycle 则扩大到仓库 `mvn verify`。
4. Admin checks：在生成项目 Admin 中执行锁文件安装、unit test、Cypress 登录失败用例和生产/Docker mode 构建；确认 form-urlencoded 请求体、单一错误提示、loading 恢复、`/api` 与 `/iam` 代理及 SPA fallback。
5. Examples host mode：分别启动 Alibaba/Tencent Middleware-only，确认没有应用容器运行，再从 Maven/IDE 启动对应链路，验证现有 gateway echo 行为和可观测性默认关闭状态。
6. Examples container mode：从空 volume 分别启动 Alibaba、Tencent 和双链路完整环境，验证健康顺序、已有 echo 请求、端口覆盖、普通停止后数据保留、显式 reset 后可重新初始化，并复跑 observability overlay 的静态与适用运行验证。
7. Generated project infrastructure mode：从新生成项目和空 volume 启动默认 deploy，验证 MySQL/Redis/Nacos 健康、账号/schema/Nacos namespace 幂等初始化，然后从 IDE 按文档顺序启动后端与 Admin。
8. Generated project full mode：在阻塞的认证方案获批并实现后，从空 volume 执行 `up-apps`，等待全部服务健康，使用示例账号完成真实 token 获取、用户/菜单加载和 `/admin/api/v1/app` 调用；不得用 mock 结果替代最终端到端验收。
9. Image checks：构建所有生成项目 Java/Admin 镜像，确认 Java 17、非 root 用户、精确 JAR、Admin `npm ci`、Nginx 非 root 及运行镜像中不包含源码凭据、`.env`、Maven cache、`node_modules` 或无关构建产物。
10. Security and review：验证默认仅绑定回环地址、重置需确认、日志不输出密码/token/client secret、认证失败不泄露额外信息，并按 `REVIEW.md` 完成 Bugs、Security、Compliance 三个独立 review pass。
11. Evidence reporting：记录实际命令、测试数、失败/错误/跳过、Docker/CPU 平台和未执行项；静态 Compose/Maven 成功不得被描述成运行时登录或完整部署成功。
