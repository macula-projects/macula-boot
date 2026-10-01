# Plan: Examples 与 Archetype 云端部署规范对齐 (from spec.md 2026-10-01)
Status: accepted

## Files that change
### SDLC contract
- `.sdlc/examples-archetype-cloud-alignment/plan.md`：记录获批实施顺序、风险、证明范围和实际偏差。

### `macula-boot-examples`
- `macula-boot-examples/README.md`：补充统一脚本入口并保持两条链路说明。
- `macula-boot-examples/docker/.env.example`：统一项目名、绑定地址、镜像、宿主机端口和本地示例配置变量。
- `macula-boot-examples/docker/docker-compose.yml`：固定容器 `docker` profile、区分容器端口与宿主机端口，并保持现有 profile/service/observability 契约。
- `macula-boot-examples/docker/README.md`：以脚本为首选入口，记录直接 Compose 兼容方式、IDE/完整模式和重置风险。
- `macula-boot-examples/docker/scripts/compose.sh`（新增）：POSIX Shell 部署入口，支持 `alibaba|tencent|all` 与 Middleware/完整应用操作。
- `macula-boot-examples/macula-example-alibaba-gateway/src/main/resources/application.yml`
- `macula-boot-examples/macula-example-alibaba-provider1/src/main/resources/application.yml`
- `macula-boot-examples/macula-example-alibaba-consumer/src/main/resources/application.yml`
- `macula-boot-examples/macula-example-tencent-gateway/src/main/resources/application.yml`
- `macula-boot-examples/macula-example-tencent-provider/src/main/resources/application.yml`
- `macula-boot-examples/macula-example-tencent-consumer/src/main/resources/application.yml`
  - 上述六个配置改为公共、`local`、`docker`、`dev`、`stg`、`pet`、`prd` 分层，保持现有端口、路由、安全和 OTLP 行为。

### Archetype descriptor and generated-project root
- `macula-boot-archetype/README.md`：修正生成命令并补充生成后验证入口。
- `macula-boot-archetype/src/main/resources/META-INF/maven/archetype-metadata.xml`：登记 deploy、隐藏文件、脚本、migration、Admin Docker/Nginx/锁文件等生成规则。
- `macula-boot-archetype/src/main/resources/archetype-resources/README.md`：替换手工改源码说明，记录外部 Macula Cloud 前置条件、IDE/容器启动顺序和验收链路。
- `macula-boot-archetype/src/main/resources/archetype-resources/__gitignore__`：忽略实际 `.env`、前端依赖/产物和本地部署文件，保留 `.env.example`。
- `macula-boot-archetype/src/main/resources/archetype-resources/.dockerignore`（新增）：排除 Git、IDE、`target`、`node_modules`、`dist` 和实际 `.env`。

### Generated-project deploy assets
- `macula-boot-archetype/src/main/resources/archetype-resources/deploy/.env.example`（新增）：统一基础设施、应用、外部 Macula Cloud、OAuth 示例 client、镜像和端口契约。
- `macula-boot-archetype/src/main/resources/archetype-resources/deploy/README.md`（新增）：两种模式、外部 Cloud 前置条件、端口、登录、状态、日志和重置说明。
- `macula-boot-archetype/src/main/resources/archetype-resources/deploy/docker-compose.yml`（新增）：MySQL/Redis/Nacos/init 默认服务及 `apps` profile 下的生成项目应用/Admin。
- `macula-boot-archetype/src/main/resources/archetype-resources/deploy/scripts/compose.sh`（新增）：与 Macula Cloud 对齐的 Compose 命令入口。
- `macula-boot-archetype/src/main/resources/archetype-resources/deploy/init/mysql/init.sh`（新增）：幂等创建 Nacos 与生成项目业务 schema/账号。
- `macula-boot-archetype/src/main/resources/archetype-resources/deploy/init/mysql/nacos-mysql.sql`（新增）：与所选 Nacos 镜像匹配的上游 schema，保留许可证和版本来源。
- `macula-boot-archetype/src/main/resources/archetype-resources/deploy/init/nacos/init.sh`（新增）：幂等创建 namespace 和必要的最小本地配置。

### Generated Java services
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin-bff/Dockerfile`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-basic/Dockerfile`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-gateway/Dockerfile`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-openapi/Dockerfile`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-service1/Dockerfile`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-thirdparty/Dockerfile`
  - 六个 Dockerfile 改为从生成项目根 reactor 构建、精确选择 JAR、Java 17 非 root 运行的多阶段镜像。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin-bff/src/main/resources/application.yml`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-basic/src/main/resources/application.yml`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-gateway/src/main/resources/application.yml`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-openapi/src/main/resources/application.yml`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-service1/src/main/resources/application.yml`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-thirdparty/src/main/resources/application.yml`
  - 六个配置统一 profile 分层；Admin BFF 指向外部 Macula Cloud Gateway，Gateway/资源服务指向外部 IAM/JWK，同时保留生成项目内部路由。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-service1/pom.xml`：使用 parent 已管理的 Flyway 依赖支持 MySQL migration。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-service1/docs/__rootArtifactId__-service1.sql`（删除）：移除手工导入入口。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-service1/src/main/resources/db/migration/V1__baseline.sql`（新增）：接管并修正示例表初始化。

### Generated Admin
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/.dockerignore`（新增）。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/.env.development`：本地代理默认值，不再保存固定内网地址。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/.env.dev`：移除固定内网地址，仅保留共享环境标题/可覆盖值。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/.env.docker`（新增）：容器构建使用相对 `/api`、`/iam`。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/Dockerfile`（新增）：Node 20 `npm ci` 构建与非 root Nginx runtime。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/nginx/templates/default.conf.template`（新增）：运行时由 `.env` 注入外部 IAM 地址，同时把 `/api` 转发到生成项目 Gateway。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/package.json`：增加 Docker build script，不改变现有依赖集合。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/package-lock.json`（新增）：锁定现有前端依赖，供 `npm ci` 使用。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/vite.config.js`：使用 `loadEnv` 配置 `/api`、`/iam` 代理，并从生成项目 `deploy/.env` 接收本地开发覆盖值。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/public/config.js`：记录生产相对代理覆盖方式。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/src/config/index.js`：为 API、IAM 和 OAuth 示例 client 提供环境变量/运行时配置入口，不复制 Cloud 系统菜单模型。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/src/api/model/common/auth.js`：同步 form-urlencoded token 请求和关闭重复通知能力，保留 Admin BFF 用户/菜单路径。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/src/utils/request.js`：同步可抑制通用错误通知及错误字段优先级。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/src/views/common/login/components/passwordForm.vue`：从配置读取 OAuth client，捕获 IAM 错误并恢复 loading，不同步租户逻辑。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/cypress/e2e/example.cy.js`：覆盖登录页和 IAM 失败反馈。
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-admin/README.md`：记录单一 `.env` 来源、构建/测试和外部 Cloud 依赖。

## Order of work
1. 固化外部认证决策：将“已运行的 Macula Cloud Gateway/IAM/System 是前置条件”写入生成项目配置与文档；定义 `MACULA_CLOUD_GATEWAY_URL`、`MACULA_CLOUD_IAM_URL`、OAuth client、示例账号等 `.env` 契约。生成项目不构建、不启动 Cloud 服务。
2. 先更新 Examples 的六个 `application.yml`：抽取公共配置，补全 `docker -> local` profile group 与 `dev/stg/pet/prd` 连接段，同时保持已验收端口、路由、JWT/Sentinel 和 OTLP 配置。
3. 更新 Examples Compose 与 `.env.example`，固定应用使用 `docker` profile；新增 POSIX `compose.sh`，再同步两份 Examples README。不得改变现有两个业务 profile、service name 和 observability overlay 接口。
4. 在 Archetype 模板中建立 `deploy/`：加入 `.env.example`、Compose、初始化脚本和 README。先完成默认基础设施模型，再加入 `apps` profile、健康依赖和外部 Macula Cloud 地址注入。
5. 更新生成项目六个 Java Dockerfile和六个 `application.yml`；所有容器地址只出现在 `docker` 段，IDE 地址只出现在 `local` 段，共享环境移除当前固定内网账号/地址。
6. 将 Service1 手工 SQL 迁移为 Flyway baseline，更新其 POM 与配置，修正表删除/创建名称不一致；部署 MySQL init 只创建账号与 schema。
7. 实施 Admin 部署与登录修复：增加锁文件、多阶段 Dockerfile、Nginx template、Vite 双代理、相对 API/IAM 配置、可配置 OAuth client、form-urlencoded token 请求、单一错误提示和 Cypress 用例。只摘取 `e314b55` 的相关行为，不同步租户或系统管理页面。
8. 更新 Archetype metadata、根/模块 README、ignore 文件，确保所有新增隐藏文件、脚本、migration 和模板生成；针对 Velocity 与 Compose 双重 `${...}` 做逐文件审计。
9. 生成一个全新样例项目，比较预期文件清单并修复模板过滤问题；只在生成结果正确后执行 Maven、npm、Compose 静态验证。
10. 按 Proof 依次完成 Examples 回归和生成项目基础设施/完整模式验证。真实登录只在外部 Macula Cloud 已启动且 `.env` 提供有效应用/OAuth 注册信息时执行；环境缺失必须明确报告，不以 mock 替代。

## Risks
- 认证依赖决策已由 Rain 于 2026-10-01 明确：允许依赖已运行的 Macula Cloud Gateway/IAM/System。生成项目完整容器模式因此不是身份平台自包含部署；外部 Cloud 不可用时，Admin 登录、用户/菜单和受保护接口验收必然失败，但生成项目自身基础设施与应用仍应可独立启动。
- 外部 Cloud 必须预先创建与生成项目匹配的应用和 OAuth client。SPA 中的 `client_secret` 无法成为真正机密，只能使用为本地示例批准的 public/demo client；不得把生产 secret 编译进前端或提交仓库。
- Admin 容器同时访问生成项目 Gateway 和宿主机/外部 IAM。Nginx template 与 Compose 必须处理 Linux 的 `host-gateway` 和 macOS/Windows 的 `host.docker.internal`，并允许 `.env` 改成可路由的外部地址。
- Velocity、Maven 资源过滤、Spring `${...}` 与 Compose `${...}` 四类占位符共存，最容易产生生成后语法正确但变量已被提前替换的问题；真实 archetype generation 和生成结果扫描是阻断性检查。
- Service1 从手工 SQL 迁到 Flyway 会改变数据库初始化所有权。已经手工建表的本地数据库可能出现 baseline 冲突；README 需要说明本地示例数据重置路径，且 migration 一旦发布不可原地改写。
- Examples 已有 Maintain 阶段的已验收 Compose 和 observability 行为。配置分层必须避免改变 Gateway HTTP/HTTPS、Nacos/Polaris namespace、Sentinel、JWT、Redis和 OTLP 默认值；回归失败时优先恢复兼容而非修改既有验收。
- `package-lock.json` 会产生较大的新文件，并可能暴露当前 `package.json` 的旧依赖告警。除非构建失败且重新获得范围批准，本次不升级前端依赖或迁移 UI 框架。
- 多阶段 Java Dockerfile从根 reactor 构建会增加镜像构建时间；使用 BuildKit Maven cache 缓解，但不得选择已有 `target` 中的陈旧 JAR。
- 本地默认端口可能与用户现有服务冲突。所有宿主机端口必须可覆盖，运行验证可以覆盖 host port，但必须同时证明默认渲染契约未改变。
- 回滚以提交级回退本功能分支为主；不得删除用户 volume。数据库回滚只允许丢弃明确确认的本地示例 volume 后重建，不提供逆向 migration。

## Proof
1. Requirements 1-5：对 Examples 运行 `sh -n docker/scripts/compose.sh`；渲染默认、Alibaba、Tencent、双 profile 及两种 observability 组合；断言 profile 仍只有 `alibaba/tencent`、service/端口/健康依赖不变。分别执行 Middleware-only 与完整链路 smoke test，并调用现有 Alibaba/Tencent gateway echo。
2. Requirements 2-3、16-17：使用缺省 `.env.example` 和一组宿主机端口覆盖运行脚本；验证 `.env` 缺失回退、`MACULA_COMPOSE_ENV`、普通 `down` 保留数据、`reset` 无确认失败、`reset --confirm` 才删除数据，以及所有默认发布地址为回环地址。
3. Requirements 4-5、18：清除容器专用变量后从 Maven/IDE 启动两条 Examples 链路；确认本机默认地址、现有响应和 OTLP 默认关闭不回归。运行 `mvn -pl macula-boot-examples -am test`，并在跨模块完成后运行仓库 `mvn verify`。
4. Requirements 6-11：安装当前 Archetype 后执行一次非交互 `archetype:generate`；扫描生成目录不存在 `#set`、未解析 `${rootArtifactId}`/`${symbol_dollar}` 或缺失的 Compose 变量；核对 deploy、隐藏文件、锁文件、migration、Docker/Nginx 和脚本权限，并在生成项目运行 Maven test/package。
5. Requirements 6-10、15-17：在生成项目对默认与 `apps` profile 执行 `docker compose config --quiet` 和 service 清单检查；从空 volume 启动基础设施，验证 MySQL/Redis/Nacos、账号/schema/namespace 初始化；重复启动证明幂等，普通停止/重启证明持久化，显式 reset 后证明可重建。
6. Requirements 7-10、18：构建并检查六个 Java 镜像和 Admin 镜像，确认 Java 17、非 root、精确单 JAR、`npm ci`、Nginx 非 root、健康检查与启动顺序；运行镜像不得包含 `.git`、实际 `.env`、Maven cache、源码、`node_modules` 或无关产物。
7. Requirements 12-14：在生成 Admin 中执行 `npm ci`、unit test、Cypress 登录页/IAM 失败用例、`npm run build` 和 Docker mode build；网络断言 token 请求为 form-urlencoded，请求失败只有一条可读提示且按钮恢复可用。文件清单检查不得出现从 Cloud 同步的租户/角色/菜单/字典/日志页面。
8. Requirement 15：准备已运行的 Macula Cloud，并在 `.env` 填写有效 Gateway/IAM 地址、应用标识、OAuth demo client 与示例账号；分别在 Admin IDE 模式和生成项目 `apps` 容器模式完成 token、当前用户、菜单加载和 `/admin/api/v1/app` 请求。该项必须是真实端到端结果，外部 Cloud 未准备时标为未验证。
9. Security proof：检查前端产物和容器环境不包含生产 secret；默认端口仅绑定 `127.0.0.1`；日志不输出密码、token 或 client secret；Nginx 仅代理预期路径；执行 `REVIEW.md` 的 Bugs、Security、Compliance 三个独立 pass。
10. 最终验证报告必须列出每条实际命令、测试数、失败/错误/跳过、Docker 与 CPU 平台、外部 Cloud 版本及所有未执行项；Compose 静态解析、Maven 编译或 mock Cypress 成功不得被表述为真实登录成功。
