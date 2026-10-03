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
  - 六个配置统一 profile 分层；Admin BFF 指向外部 Macula Cloud Gateway，生成项目 Gateway 指向外部 IAM，资源服务通过生成项目自身 Gateway 获取 JWK，同时保留生成项目内部路由。
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

## 实际偏差
- 2026-10-02：按 Rain 的实现反馈，资源服务的 JWK 地址不再通过 `GATEWAY_JWK_SET_URI` 从 `deploy/.env`/Compose 注入，而由各模块 `application.yml` 的基础配置与 `docker` profile 直接配置；这收窄了环境变量契约，并继续保证 IDE 指向 `127.0.0.1:8080`、容器指向生成项目自身 Gateway。证明范围增加生成结果扫描，确认变量完全移除且 profile 地址生成正确。
- 2026-10-02：进一步按 `macula-cloud-system` 当前 `application.yml` 对齐六个后端模板的结构与顺序：公共段以 `server`、`spring` 开头，Nacos 连接归入各 profile 的 `spring.config.nacos`，local/docker 配置按职责排序，dev/stg/pet/prd 分为独立文档；保留生成项目自身 Gateway 的 JWK 路由差异。该调整不改变文件范围，证明增加六个生成配置的文档顺序、占位符与 YAML 解析检查。
- 2026-10-02：按 Rain 的实现反馈，六个后端模板的 OpenTelemetry 开关、采样率、OTLP 端点和资源属性直接写入 `application.yml`，移除全部 `OTEL_*`/`DEPLOYMENT_ENVIRONMENT` 环境变量占位符；默认仍关闭三类导出，不向 deploy/Compose 增加可观测性变量。证明增加模板及生成结果的相关环境变量零残留检查。
- 2026-10-02：按 Rain 对外部认证边界的修正，Admin 不再通过 Vite 或 Nginx 的 `/iam` 代理访问 IAM；`MACULA_CLOUD_IAM_URL` 保留为唯一外部 IAM 地址，并在开发时从 `deploy/.env` 显式注入、在容器模式作为前端构建参数注入。该偏差替代原计划的 IAM 同源代理设计，新增外部 IAM CORS 与浏览器/容器双向可达要求；证明增加 `/iam`/`VITE_APP_IAM_*` 零残留、构建产物 URL 和真实跨域登录检查。
- 2026-10-02：按 Rain 要求将同类修正同步到 Examples 六个应用：Alibaba 配置按 `macula-cloud-system` 的公共、local、docker、dev、stg、pet、prd 顺序组织并使用 `spring.config.nacos`，Tencent 保持 Polaris 对应配置模型但采用相同分层；四个资源服务的 JWK 地址改为 local/docker 直接值，移除 Compose 的 `GATEWAY_JWK_SET_URI`。应用侧 OpenTelemetry 不再由 Compose 注入 `OTEL_*`，默认配置直接写入 `application.yml` 并关闭导出；可观测 overlay 仅叠加 `observability` Spring Profile，该 Profile 在应用配置内启用三类导出并指向 Collector。基础设施镜像与宿主机端口变量仍由 Compose 管理。
- 2026-10-02：Rain 指出 Macula Cloud 与 Examples/生成项目同时运行时存在宿主机端口冲突，因此默认端口从“沿用标准端口”调整为分区管理：Macula Cloud 保持标准端口，生成项目基础设施使用 `2xxxx`（MySQL `23306`、Redis `26379`、Nacos `28848/28080/29848`），Examples 基础设施与可观测后端使用 `3xxxx`；生成项目 Gateway 从 `8080` 调整为 `6000`。容器内部标准端口不变，local 配置、JWK、Vite/Nginx、Compose、`.env.example` 与文档同步更新；Task 示例也改为连接 Examples Nacos `38848`。
- 2026-10-02：端口二次审计补齐不加载 `.env` 时的 Compose fallback、dev/stg/pet/prd 回环默认地址、Sentinel Dashboard、Binlog4j MySQL/Redis 以及本机 OTLP endpoint。审计规则区分宿主机默认值与容器内部标准端口：前者分别使用生成项目 `2xxxx`、Examples `3xxxx`，后者继续保留 MySQL `3306`、Redis `6379`、Nacos `8848/8080/9848`、Polaris `8090/8091/8093` 和 OTLP `4318`。
- 2026-10-02：经 Rain 确认进一步收敛 Compose 宿主机攻击面：Examples 完整模式仅发布 Gateway，Archetype 完整模式仅发布 Gateway 与 Admin，provider/consumer 及生成项目内部后端继续通过 Compose DNS 和容器 healthcheck 通信；Collector 仅发布应用实际使用的 OTLP/HTTP，gRPC 与 health extension 保持容器内可达。相应移除无效端口变量，并修正 observability 校验脚本的 `3xxxx` 查询默认值。端口审计同时发现并补齐 Tencent 三个应用的 Polaris-Nacos discovery profile 地址，以及 Examples Gateway 容器访问外部 Macula Cloud IAM 时错误使用自身 `127.0.0.1` 的问题；证明增加发布端口白名单、各 profile YAML 解析和外部 IAM 地址渲染检查。
- 2026-10-02：按 Rain 反馈对 Macula Cloud、Examples 和 Archetype 的 Nacos 客户端配置做三方审计。所有相关 `application.yml` 已完整包含 `server-addr`、`namespace`、`username`、`password`，但部署变量链路存在缺口：Examples Compose 在未加载 env 文件时会把用户名/密码置空，Archetype `.env.example` 未声明两项凭据。实现将 Examples fallback 统一为本地样例 `nacos/nacos`，补齐 Archetype 环境样例与文档，并验证三项变量进入全部应用容器。参考用的 Macula Cloud `.env.example` 已声明三项变量，但普通 `deploy/docker-compose.yml` 误用了 Archetype 模板占位符 `${symbol_dollar}{...}`，实际渲染为字面量而非变量值；它属于另一个仓库且未获本计划写入授权，本次只记录审计结论、不跨仓库修改。
- 2026-10-02：Test 阶段首次执行生成 Admin 的 `CI=true npm run test:unit` 返回 `No test files found`（exit 1），暴露出计划声明了 unit test 但模板只新增 Cypress 用例的缺口。按失败循环新增 `src/api/model/common/auth.test.js`，直接断言 password grant 使用 `URLSearchParams`、`application/x-www-form-urlencoded`，保留自定义请求配置并默认关闭重复错误通知；Archetype 的现有 Admin `src/**/*` fileSet 会自动包含该文件。证明增加重新生成项目后的 Vitest 实际结果。
- 2026-10-02：随后 Cypress 发现上述 unit test 位于 `src/api/model/*/*.js` 时会被生产代码的 `import.meta.globEager` 打包，导致浏览器启动期执行 Vitest 采集器并报 `Cannot read properties of undefined (reading 'config')`。按失败循环将用例移到 `tests/unit/auth.test.js`，在 Archetype metadata 中单独登记 `tests` fileSet，确保 Vitest 可发现且生产 bundle 不再包含测试代码。
- 2026-10-02：生成项目的空卷基础设施验证发现 Nacos 3.2.4 已移除 v1 Console namespace API，`nacos-init` 调用 `/nacos/v1/console/namespaces` 返回 HTTP 500 并退出 22。按失败循环将创建/查询分别迁移到 Nacos 3.x Console API `/v3/console/core/namespace` 和 `/v3/console/core/namespace/list`，保持原有幂等语义。
- 2026-10-02：生成 Admin 在 macOS 上的 Vite 构建通过，但 Linux Docker build 因 `sideM.vue` 引用 `./NavMenu.vue` 而实际文件为 `navMenu.vue` 失败。按失败循环修正 import 大小写，并以真实 Linux 多阶段镜像构建作为最终证明，不仅依赖 macOS 本机构建。
- 2026-10-02：生成项目完整容器启动时，Flyway 在 MySQL 8.4 上会读取 `performance_schema.user_variables_by_thread` 判断 user-variable reset 能力，原有只针对业务 schema 的权限使 Service1 启动失败。按失败循环为生成项目数据库用户增加单表 `SELECT` 权限，不扩大到整个 `performance_schema`。
- 2026-10-02：生成 Gateway 的完整容器启动在 Spring Boot 4 下因三个 `DataRedisProperties` Bean 导致 `DataRedisAutoConfiguration.redisConnectionDetails` 注入歧义。原配置只将业务 `RedissonClient` 标为 `@Primary`，现同步将对应的 `redisProperties` 标为 `@Primary`，保留生成项目 Redis 与外部 System Redis 两套链路。
- 2026-10-02：Test 阶段的 `npm audit --omit=dev` 首次报告 8 个生产依赖漏洞（1 critical、3 high、4 moderate）。在 Rain 明确批准扩展依赖升级范围后，将 axios、crypto-js、echarts、element-plus、tinymce、`@tinymce/tinymce-vue`、vue-i18n 升级到无已知生产漏洞的版本并更新 lock。首次生成项目构建随后因 Element Plus 新版使用 `color.channel()` 而旧 Sass 1.37.5 不支持而失败，按失败循环将 Sass 升到 1.89.2；真实 Linux 构建进一步发现新版 Element Plus/@VueUse 及测试工具链声明 Node 22+，因此将 Admin Docker builder 从已停止常规支持的 Node 20 调整为 Node 22，运行时 Nginx 不变。证明增加重新生成项目后的生产审计、unit、两种构建、Cypress 与无 `EBADENGINE` 的 Linux Docker build。
- 2026-10-02：真实 IAM token 验证发现模板默认 OAuth client `e2fa7e64-249b-46f0-ae1d-797610e88615` 在 Macula Cloud 基线中只允许 authorization_code/client_credentials/refresh_token，因此 password grant 返回 `unauthorized_client`；同一基线中的 Gateway client `e4da4a32-592b-46f0-ae1d-784310e88423` 允许 password grant，并以公开本地样例凭据实测 token HTTP 200。按失败循环同步修正 `.env.example` 与 Compose build-arg fallback，证明扩展为真实 token、当前用户、菜单及 Admin BFF 请求，报告中不输出 token 或 client secret。
- 2026-10-02：真实登录后置请求验证发现模板错误地把 Macula Cloud System 提供的当前用户和菜单接口拼到生成项目 `admin` BFF 路由，实际返回 HTTP 404；参考 Admin 使用 `config.MODEL.system`，且真实 Cloud Gateway 的 `/system/api/v1/users/me` 与 `/system/api/v1/menus/routes` 均返回 HTTP 200。按失败循环将这两条认证后置请求改为 `system`，保留 service1 管理接口继续使用 `admin`，并增加 URL 路由单元测试防止两个模型再次混淆。
- 2026-10-02：真实浏览器复测进一步发现，仅把模型改为 `system` 仍会复用生成项目业务 `API_URL`，导致请求进入生成项目 Gateway 后返回 HTTP 404。生成项目同时存在“自身 Gateway”和“外部 Macula Cloud Gateway”两条前端 API 链路，因此新增 `SYSTEM_API_URL`，由既有 `MACULA_CLOUD_GATEWAY_URL` 在 Vite/容器构建时注入；用户与菜单直接访问外部 Gateway，Service1 管理接口继续通过 `/api` 访问生成项目 Gateway，IAM 仍直接使用 `MACULA_CLOUD_IAM_URL`。文档同步要求外部 Gateway 与 IAM 均可被浏览器访问并允许 CORS，证明增加真实浏览器三段链路验证。
- 2026-10-02：Stage 5 独立审查发现登录 token 成功后，外部 System 用户或菜单请求异常会留下 token 并使按钮持续 loading。返回失败循环后将 token、用户、菜单加载纳入统一 `try/catch/finally`，任何阶段失败都清除部分认证状态并恢复按钮，同时新增用户/菜单失败的 Cypress 用例。审查还发现三个部署契约问题并一并修正：Examples `reset --confirm` 现在正确使用默认 `all` stack，Gateway Dockerfile 默认端口与应用契约统一为 `6000`，MySQL init 对账号/数据库标识符做白名单验证并转义 SQL 字符串中的密码。
- 2026-10-02：以 `macula-samples` 重新生成并运行完整容器栈时，服务端 token、用户、菜单和业务接口均返回 HTTP 200，但 Docker Admin 浏览器请求因 `host.docker.internal` 在 macOS 宿主机不可解析而返回 HTTP 502；同一地址变量同时供浏览器和容器使用的假设不成立。按 Rain 确认将浏览器地址保留为 `MACULA_CLOUD_GATEWAY_URL`/`MACULA_CLOUD_IAM_URL`（默认回环地址），新增 `MACULA_CLOUD_GATEWAY_INTERNAL_URL`/`MACULA_CLOUD_IAM_INTERNAL_URL` 专供容器，并将外部 Cloud Redis 默认宿主机端口与当前部署契约统一为 `16379`。证明增加 `macula-samples` 默认 `.env.example` 的真实 Docker 浏览器登录与菜单加载复测。
- 2026-10-02：按 Rain 对 Admin 镜像契约的复核，业务 API 明确固定为同源 `/api`，Dockerfile 参考 `macula-cloud-admin` 仅保留构建模式与 `VITE_APP_API_BASEURL` 两个构建参数。外部 Cloud Gateway、IAM、OAuth client 与演示账号改由 Nginx 容器启动时从 Compose 环境生成 `config.js`，避免因地址或本地 demo client 变化而重新构建镜像；IAM 仍为浏览器直连外部地址，不恢复 `/iam` 代理。Nginx 配置改为静态 `nginx.conf`，仅 `/api/` 转发生成项目 Gateway。
- 2026-10-02：Rain 补充要求覆盖非容器的本地 IDE 启动。Admin 根 `.env` 显式声明 `VITE_APP_API_BASEURL=/api`，`.env.development` 声明 `VITE_APP_GATEWAY_PROXY_TARGET=http://127.0.0.1:6000` 并开启代理，不再只依赖 Vite 配置中的隐含 fallback；Vite 继续从项目根 `deploy/.env` 读取浏览器可访问的外部 Cloud Gateway/IAM 与 OAuth 示例配置，且开发模式不依赖 Docker 启动时生成的 `config.js`。Java 应用继续使用现有 `local` profile 的本机端口默认值。证明增加真实 Vite 开发服务器的 `/api` 代理与开发时配置注入检查。
- 2026-10-02：进一步参考 `macula-cloud-admin` 的 mode 分层：新增 Admin 根 `.env` 保存所有 mode 共享的 `/api`、外部 Cloud、public/demo OAuth client、scope 与示例账号默认值；`.env.development`、`.env.docker`、`.env.dev` 只保留各自差异。`vite.config.js` 改为合并完整的 Admin mode env，再以项目根 `deploy/.env` 和进程环境覆盖，优先级为 `Admin .env/.env.<mode> < deploy/.env < process.env`。这样直接执行 `npm run dev` 无需先创建 `deploy/.env`，部署模式仍可统一覆盖，Java IDE 进程仍不自动加载 `deploy/.env`。
- 2026-10-03：重新生成项目后额外执行 Admin 的非验收命令 `npm run lint`，发现脚本使用 `--ignore-path .gitignore`，但模板只生成项目根 `.gitignore`，Admin 模块内缺少该文件并以 exit 2 失败。参考 `macula-cloud-admin` 补齐 Admin 自身 `__gitignore__` 模板，由 Archetype 在生成时映射为 `.gitignore`。修复后 lint 能实际检查源码，但仍暴露继承前端的大量既有 Prettier/ESLint 基线问题；该遗留清理不在本次已接受范围内，也不以自动 `--fix` 扩大改动。
- 2026-10-03：Rain 纠正用户与菜单链路：Admin BFF 已通过 `macula-boot-starter-system` 暴露 `/api/v1/users/me` 和 `/api/v1/menus/routes`，前端不应绕过 BFF 直连外部 Cloud Gateway。实现恢复规格中的同源 `/api/admin/api/v1/**` 路径，由生成项目 Gateway 去除 `/admin` 前缀后转发 Admin BFF；仅 Admin BFF 通过 `MACULA_CLOUD_GATEWAY_INTERNAL_URL` 访问外部 Cloud System。相应删除前端 `SYSTEM_API_URL`、浏览器 `MACULA_CLOUD_GATEWAY_URL`、Vite define、运行时 `config.js` 字段及 Admin 容器变量，浏览器跨域边界只保留 IAM；单元测试和 Cypress 拦截同步验证 Admin BFF 路由。
- 2026-10-03：生成项目真实浏览器登录发现 Macula Cloud 菜单响应以 `meta.roles: []` 表示不限制角色，而模板把空数组当作必须匹配，导致接口成功但菜单全部被过滤并跳转无权限页。登录菜单过滤和路由恢复逻辑统一调整为 roles 缺失或为空时放行，仅非空数组执行角色交集判断，并增加 Cypress 回归用例。
- 2026-10-03：Stage 5 Security pass 发现 Nginx 官方 entrypoint 直接 `envsubst` 外部值到 JavaScript 字符串，特殊字符会破坏语法并可能形成同源脚本注入。返回 Test 循环后参考 `macula-cloud-admin`：新增非 root entrypoint 脚本，将 IAM、OAuth demo client 和示例账号逐项按 UTF-8 原始字节 Base64 编码，只对白名单编码变量执行 `envsubst`，浏览器通过 `atob` 与 `TextDecoder` 解码。证明增加包含引号、反斜杠、换行、中文和脚本片段的真实容器生成及 JavaScript 解析断言。
