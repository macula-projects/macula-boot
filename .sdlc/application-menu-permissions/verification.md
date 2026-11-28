# 应用菜单与接口授权验证

验证日期为 2026-11-28。初轮结果保留用于追溯，交付回归见末节。

## 命令与结果

| 范围 | 实际命令 | 输出 |
| --- | --- | --- |
| Boot 全仓 | `mvn -o -Dmaven.repo.local=/tmp/macula-application-m2 test` | `BUILD SUCCESS`。模块汇总共 558 项，失败、错误、跳过均为 0。 |
| Cloud 全仓 | `mvn -f /tmp/macula-app-perms-cloud-full/pom.xml -o -Dmaven.repo.local=/tmp/macula-application-m2 test -Plocal` | `BUILD SUCCESS`。模块汇总共 1265 项，失败和错误均为 0，`SysUserDetailsSerializationTest` 跳过 1 项。 |
| Cloud System 与 Gateway | `mvn -o -Dmaven.repo.local=/tmp/macula-application-m2 -pl macula-cloud-system,macula-cloud-gateway -am clean verify -Plocal -Dit.test=ApplicationApiKeyIntegrationIT,TinyIdGatewayIT -Dfailsafe.failIfNoSpecifiedTests=false` | `BUILD SUCCESS`。System 332 项单测，Gateway 1 项单测，System 8 项真实 MySQL/Redis 集成测试，Gateway 5 项路径与签名测试。 |
| 隔离 HTTP 联调 | 在 `/tmp/macula-app-perms-joint` 执行 `mvn -o -Dmaven.repo.local=/tmp/macula-application-m2 -pl macula-cloud-system -am verify -Plocal -Dtest=SysApplicationPermissionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false '-Dit.test=ApplicationApiKeyIntegrationIT#gateChecksSystemWriteOverRealHttpAndSharedRedis' -Dfailsafe.failIfNoSpecifiedTests=false` | `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。同一 API Key 的响应依次为 `403 → 204 → 403`。 |
| Admin | `npm run test:unit`、`npm run build` | `Test Files 33 passed (33)`、`Tests 315 passed (315)`、`built in 7.66s`。 |
| 应用管理浏览器 | Cypress `--spec cypress/e2e/application-api-key.cy.js` | `4 passing` 和 `All specs passed!`，含菜单回显、清空和 API Key 操作。 |
| 全量浏览器与基线 | `npm run test:e2e:ci`，另在 Cloud HEAD 的独立副本运行 `--spec cypress/e2e/security-policy.cy.js` | 基线与变更版都出现 `cy.wait() timed out ... loadBasicPolicy. No request ever occurred.`。 |

## 授权证据

- 角色规则存在时，应用凭据按应用授权放行，普通 Bearer 保持角色校验。Boot 回归覆盖同应用 API Key、HMAC、无匹配、租户、来源 IP、公开路由及本地 token 清理。
- System 用真实 MySQL 验证迁移删除 `access_path`、新增关联表。System Redis 集成覆盖授予、回滚、撤销及旧缓存字段清理。
- 隔离 HTTP 联调用 System Controller 与真实 Mapper 更新应用授权，运行中的 Reactor Netty Gateway 过滤器直接读取同一 Redis。用例显式断言键 `macula:cloud:system:app_perms:1:900111`，以及同一凭据在三个授权状态的响应。完整部署形态的 IAM 登录、服务发现和 HMAC 管理接口联调仍未执行。
- 后端管理身份和归属校验有定向测试，覆盖跨租户、应用无角色及只有 Scope 的身份。应用权限快照的多实例重建顺序和 Redis 发布故障演练仍需补足。
- 初次 Cloud 验证的旧 Key 来自编译缓存中的内联常量。干净重编译后，Gateway 5 项和 System 8 项集成测试通过。首次本地 JVM 附加错误已通过允许测试进程正常附加的复验解决。
- 首轮应用管理 Cypress 参考了已移除的 URL 字段。已改成验证来源 IP 界面、菜单授权回显和清空，后续定向运行四项通过。

未修改 `CLAUDE.md`、技能、hooks、`REVIEW.md` 或审查者配置，无适用配置 eval。相关日志位于 `/tmp/macula-app-perms-*.log`。

## 本地镜像预览

- 本地数据库来自历史增量路径。临时 System 镜像沿用旧镜像的 Flyway 资源，并在保留 IAM 表、列和设置校验的前提下识别成功应用的 V56.12。原库副本通过 101 条迁移校验及 V56.14 增量升级。此兼容装配仅包含在本地预览镜像。
- 实际容器启动发现授权缓存重建经租户、用户、菜单服务形成依赖环。改用 Mapper 查询存在应用的归属租户后，Cloud System 重新打包并启动成功。
- 仅替换本地 System、Gateway 和 Admin 三个容器，均为 `healthy`。System 和 Gateway 的运行 JAR 与构建产物逐字节一致。Admin 资源包含应用授权组件。
- 实际数据库报告 `56.14 / success=1`、两张授权表、旧 URL 列数为零。System Redis 已发布两份应用授权快照。Admin 首页 HTTP `200`，匿名访问应用授权接口 HTTP `401`。实际登录后的授权操作留给本地界面验证。

本地旧库的菜单路径和权限 URL 仍使用 B56.13 之前的命名，导致已登录 Admin 的视图组件找不到。经确认，对本地租户 1 修正 21 条精确匹配的菜单映射、79 条接口路径，并为原有应用管理角色补齐 GET/PUT 菜单授权接口。刷新租户路由缓存并重启 System 后，三项服务健康，System 启动日志确认已刷新角色 URL 权限。

## 交付回归

- Boot 执行 `mvn -o verify`，结果为 `BUILD SUCCESS`。共计 621 项，0 失败、0 错误、4 项条件跳过。WebFlux 分派验证中，本地受保护处理器拒绝应用凭据，实际 Gateway 路由保留验签和授权，合法内部转发以原入口路径授权后到达本地处理器。Gateway 示例的 7 项签名集成测试通过。变更 Java Checkstyle 为 0 项违规。
- Cloud 执行 `mvn -o verify -Plocal`，使用文档指定的已编译历史夹具、隔离 MySQL 和 Redis，结果为 `BUILD SUCCESS`。共计 1433 项，0 失败、0 错误、89 项条件跳过。`ApplicationApiKeyIntegrationIT` 10 项全部通过，覆盖当前基线、菜单及接口目录变更、删除应用和保留通用安全的角色关联。`TinyIdGatewayIT` 5 项全部通过。MVC 用户边界、租户一致性和快照锁顺序通过相应单元测试。
- Admin 执行 `npm run test:unit -- --run` 得到 `Tests 317 passed (317)`，`npm run build:docker` 成功。全量 Cypress 得到 `All specs passed`，145 项通过、38 项原有待执行。Cypress 覆盖应用授权树桌面与矮屏尺寸、角色入口和通用安全五个页签。已登录的浏览器也验证修正后的本地 Admin 实际加载了五个通用安全页签，未执行真实配置写入。
- 初轮通用安全前端引用了错误地址。最终实现和测试统一使用控制器注册的 `/api/v1/iam/login-protection`，V56.15 在隔离数据库中验证保留原授权关联。
