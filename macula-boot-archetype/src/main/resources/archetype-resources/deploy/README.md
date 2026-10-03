# ${rootArtifactId} 本地部署

本目录支持两种开发方式：默认只启动 MySQL、Redis、Nacos 与初始化任务，Java 服务和 Admin 在 IDE 中运行；`apps` profile 会从生成项目源码构建并启动全部应用容器。

登录依赖一个已经运行并完成应用/OAuth client 注册的 Macula Cloud Gateway、IAM 和 System。本目录不会复制或启动这些平台服务。

## 快速开始

```bash
cp deploy/.env.example deploy/.env
./deploy/scripts/compose.sh config
./deploy/scripts/compose.sh up
```

编辑 `deploy/.env` 中的 `MACULA_CLOUD_IAM_URL`、`MACULA_CLOUD_GATEWAY_INTERNAL_URL`、`MACULA_CLOUD_IAM_INTERNAL_URL`、`MACULA_CLOUD_REDIS_HOST`、`MACULA_CLOUD_REDIS_PORT`、应用凭据和 `VITE_APP_OAUTH_*`。浏览器只直接访问外部 IAM，因此仅该地址会在 Admin 容器启动时写入运行配置，并且必须允许 Admin 来源的 CORS。用户和菜单请求固定经同源 `/api/admin/api/v1/**` 进入生成项目 Gateway 与 Admin BFF；Admin BFF 再通过 `MACULA_CLOUD_GATEWAY_INTERNAL_URL` 访问外部 Cloud System。不要把容器专用主机名写入浏览器配置。Gateway 的系统会话 Redis 也指向外部 Cloud Redis，默认使用 Macula Cloud 发布到宿主机的 `16379` 端口；项目自身 Redis 只服务生成项目。资源服务的 JWK 地址直接由各模块 `application.yml` 的 profile 管理：IDE 使用 `http://127.0.0.1:6000/oauth2/jwks`，容器模式使用生成项目自身 Gateway 的 Compose service name。

默认宿主机端口使用独立的生成项目区间：MySQL `23306`、Redis `26379`、Nacos HTTP/Console/gRPC `28848/28080/29848`、Gateway `6000`、Admin `5800`，可与使用标准端口的 Macula Cloud 同时运行。`NACOS_NAMESPACE`、`NACOS_USERNAME`、`NACOS_PASSWORD` 会同时传给全部 Java 应用；样例中的 `MACULA5`/`nacos`/`nacos` 仅用于关闭 Nacos 鉴权的本地环境。完整容器模式只发布 Gateway 与 Admin；Admin BFF、OpenAPI、Service1、Basic、Thirdparty 仅在 Compose 网络内可达，并由容器内 healthcheck 检查。

完整容器模式：

```bash
./deploy/scripts/compose.sh config-apps
./deploy/scripts/compose.sh up-apps
./deploy/scripts/compose.sh status
```

## IDE 启动顺序

1. 执行 `./deploy/scripts/compose.sh up`。
2. 启动 `${rootArtifactId}-service1`、Basic、OpenAPI、Thirdparty。
3. 启动 `${rootArtifactId}-admin-bff`。
4. 启动 `${rootArtifactId}-gateway`。
5. 在 `${rootArtifactId}-admin/` 执行 `npm ci && npm run dev`。

Compose 和 Vite 会从项目根 `deploy/.env` 读取本地配置；`.env.example` 只是样例，不会被直接加载，使用部署脚本前必须复制为 `deploy/.env`。Admin 根 `.env` 提供各 mode 共享的 `/api`、外部 IAM、demo OAuth client 和示例账号默认值，`.env.development` 等 mode 文件只覆盖模式差异；因此可以直接执行 `npm run dev`，而 `deploy/.env` 中的同名值具有更高优先级。开发模式不使用 Docker 的运行时 `config.js`。IDE 启动 Java 服务时使用 `local` profile，其默认值已对应本机 `2xxxx` 基础设施、生成项目 Gateway `6000` 和外部 Macula Cloud 标准端口；只有需要覆盖默认值时才在运行配置中注入变量，Java 服务不会直接加载 Compose 的 `.env`。`VITE_APP_OAUTH_CLIENT_SECRET` 只允许填写本地 public/demo client 的兼容值，它会进入浏览器，不能作为真正的 secret。

浏览器访问 `http://127.0.0.1:5800`。成功登录后，Admin 经生成项目 Gateway 和 Admin BFF 调用 `/admin/api/v1/users/me`、`/admin/api/v1/menus/routes` 与 `/admin/api/v1/app`；Admin BFF 的 System Starter 负责访问外部 Macula Cloud。

## 数据与重置

普通停止保留 MySQL、Redis 和 Nacos 数据：

```bash
./deploy/scripts/compose.sh down
```

永久删除本地 volume 必须显式确认：

```bash
./deploy/scripts/compose.sh reset --confirm
```

该操作不可恢复。示例账号、密码和 OAuth client 只能用于回环地址绑定的本地开发环境，禁止用于共享或生产环境。
