# Plan: 应用菜单与接口授权（源自 spec.md，2026-11-27）
Status: accepted


## Files that change

Macula Boot 的网关路径以下以 `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/` 为根。

- `macula-boot-commons/src/main/java/dev/macula/boot/constants/CacheConstants.java` 定义应用权限快照前缀和共享应用身份字段。旧 Redis 字段的清理字面量仅保留在 Cloud。
- 网关 `src/main/java/dev/macula/boot/starter/cloud/gateway/` 下的 `security/ResourceServerAuthorizationManager.java`、`security/ResourceServerConfiguration.java`、新增 `security/ApplicationPermissionAuthorizer.java`、`filter/ApiKeyGlobalFilter.java`、`filter/KongApiGlobalFilter.java`、`filter/AddJwtGlobalFilter.java`、`utils/TenantUtils.java`。Gateway README 更新鉴权契约。
- 网关 `src/test/java/dev/macula/boot/starter/cloud/gateway/` 下的 `security/ResourceServerAuthorizationManagerTest.java`、`security/ApiKeySecurityChainTest.java`、新增 `security/ApplicationPermissionAuthorizerTest.java`、`filter/ApiKeyGlobalFilterTest.java`、`filter/KongApiGlobalFilterTest.java`、`filter/AddJwtGlobalFilterTest.java`、`config/ApiKeyAutoConfigurationTest.java`、`security/CorsSecurityWebFilterChainTest.java`、`utils/TenantUtilsTest.java`，以及新增 `filter/ApplicationJwtCacheIsolationTest.java`。

Macula Cloud 的 System Java 路径以下以 `macula-cloud-system/src/main/java/dev/macula/cloud/system/` 为根。

- `macula-cloud-system/src/main/resources/db/migration/V56.14__application_menu_permissions.sql` 新增两表、登记应用授权管理 API 的角色权限并移除两列旧字段。既有基线用于验证升级。
- 新增 `pojo/entity/sys/SysApplicationMenu.java`、`pojo/entity/sys/SysApplicationPermission.java`、`mapper/sys/SysApplicationMenuMapper.java`、`mapper/sys/SysApplicationPermissionMapper.java`、`service/sys/SysApplicationPermissionService.java`、`service/sys/impl/SysApplicationPermissionServiceImpl.java`、`service/cache/ApplicationPermissionCacheService.java` 和 `macula-cloud-system/src/main/resources/mapper/SysApplicationPermissionMapper.xml`。
- 修改 `controller/sys/SysApplicationController.java`、`controller/sys/SysApplicationApiKeyController.java`、`controller/sys/SysRoleController.java`、`controller/sys/SysMenuController.java`、`controller/sys/SysPermissionController.java`、`service/sys/impl/SysApplicationServiceImpl.java`、`service/sys/impl/SysApplicationApiKeyServiceImpl.java`、`service/sys/impl/SysMenuServiceImpl.java`、`service/sys/impl/SysPermissionServiceImpl.java`、`service/cache/ApplicationCredentialCacheService.java`、`listener/ApplicationListener.java`。新增 `security/ApplicationManagementAccess.java` 统一应用管理访问判定，校验可信用户的管理角色及租户归属。受影响管理入口的现有授权依据逐项核对。
- 删除旧路径字段的模型与映射变更位于 `pojo/bo/ApplicationBO.java`、`pojo/entity/sys/SysApplication.java`、`pojo/entity/sys/SysApplicationApiKey.java`、`pojo/form/ApplicationApiKeyForm.java`、`vo/app/ApplicationApiKeyVO.java`、`converter/ApplicationConverter.java`、`macula-cloud-system/src/main/resources/mapper/SysApplicationMapper.xml`、`macula-cloud-api/macula-cloud-system-api/src/main/java/dev/macula/cloud/system/form/ApplicationForm.java` 与同包根下 `vo/app/ApplicationVO.java`。
- System 测试位于现有测试包下的 `migration/ApplicationApiKeyIntegrationIT.java`、`service/cache/ApplicationCredentialCacheServiceTest.java`、`controller/sys/SysApplicationApiKeyControllerTest.java`、`pojo/form/ApplicationApiKeyFormTest.java`、`pojo/form/ApplicationFormTest.java`、`service/sys/impl/SysApplicationApiKeyServiceImplTest.java`、`service/sys/impl/SysApplicationServiceImplTest.java`、`service/sys/impl/SysMenuServiceImplTest.java`，另新增 `migration/ApplicationMenuPermissionsIT.java`、`service/sys/impl/SysApplicationPermissionServiceImplTest.java`。核对 Gateway 的 `src/test/java/dev/macula/cloud/gateway/TinyIdGatewayIT.java` 的端到端夹具及 `src/test/java/dev/macula/cloud/gateway/security/LoginRiskListPermissionTest.java` 的角色鉴权回归。联合验证新增 `macula-cloud-gateway/src/test/java/dev/macula/cloud/gateway/security/ApplicationPermissionsGatewayIT.java`，连接测试 System 和 Gateway。更新 System README。
- Admin 修改 `src/api/model/system/application.js`、`src/views/sys/application/{index,save,apiKeys}.vue`、新增 `src/views/sys/application/resource.vue`，清理 `src/utils/applicationAccessRules.js` 中的 URL 处理，更新 `tests/unit/{application-api-key,application-auth-tenant}.test.js`、新增 `tests/unit/application-permissions.test.js`，更新 `cypress/e2e/application-api-key.cy.js` 与 Admin README。上述 Admin 路径均相对于 `macula-cloud-admin/`。

## Order of work

本次界面调整沿用已接受的角色授权交互。Admin 的角色和应用入口共用菜单权限弹窗，保留现有各自的加载及保存 API。树区优先加高，窄屏和矮屏随视口调整，验证已有勾选的回显与展开、父子勾选规则、空列表撤销和保存失败反馈。

交付审查补齐已接受的管理边界。Boot 按 WebFlux 实际处理器的优先级约束待验证的应用凭据，只进入 Gateway 路由。通过验证的内部转发继续处理，仍用入站路径匹配授权。Cloud System 的 IAM 管理端点及用户管理写操作要求可信用户身份。授权写事务在首次读库前获取共享锁，校验权限目录实际写入租户与快照刷新租户一致，启动重建等待短期锁竞争。通用安全管理页与控制器对齐到 `/api/v1/iam/login-protection`，V56.15 修正对应的角色权限路径并保留原授权关联。

1. 先在 Boot 定义 Redis 键和值的契约。使用 Spring Data Redis 单值快照，应用数字 ID 与归属租户构成缓存键。网关使用 Spring Gateway 返回的最早可信路由 URL 或尚未重写的当前 URI，从现有规则目录匹配 HTTP 方法及路径。保留现有 IP 准入工具和响应式调度。
2. 在 Boot 完成 API Key 和 HMAC 分流、验证后授权、身份发布。覆盖 HMAC 的内部 JWT 缓存命名空间，避免与普通 Bearer 共用同一个缓存身份键，保持已签发的 JWT 字段契约。构建本地 Boot SNAPSHOT，供 Cloud 编译验证共享常量和 Gateway 行为。
3. 在 Cloud 编写 Flyway 增量及关联查询。通过可信用户、应用管理角色和租户归属限定授权管理，使用同租户目录生成接口关系。菜单删除、权限目录更新和应用删除纳入关联刷新。核对角色授权、菜单权限维护、应用与 API Key 管理等用户操作，确保服务端拒绝应用身份。新接口的角色目录和角色关联沿用现有 System 应用管理项的授权范围。
4. 使用 System 现有 Redisson 和事务同步设施串行化授权写入、目录写入和重建。在同一锁内确定受影响应用，提交前失效旧快照，事务完成后从已提交状态发布。启动重建复用发布入口。旧字段在本次直接清理，更新凭据 Hash 时显式删除遗留 `permitUrls`，同时写入 HMAC 的可信应用 ID。
5. 接入 Admin 菜单树、加载、提交和失败反馈，移除旧 URL 表单字段和列表。执行分层及联合验证，记录实际变更和结果。

## Risks

- 新许可默认空。此次按已确认方案直接启用新功能，两张关系表初始为空，旧规则删除由 Flyway 执行。数据库旧列删除后已删除的值只能从备份恢复，实施时只在测试环境验证迁移。
- HMAC 原有主体覆盖字段不能参与应用权限缓存定位或用于判定管理身份。应以可信凭据记录定位应用，以服务端实际用户角色判定用户管理权限。HMAC 内部 JWT 缓存隔离须覆盖原有主体和 jti 两种命中方式。
- 通用 Gateway 使用入口路径匹配，尤其要覆盖带租户或服务路由前缀及改写路径。应用规则、角色规则和网关公开入口的优先顺序按已确认设计验证。
- Redis 失效失败导致写入中止。数据库已提交但快照发布失败时应用访问保持拒绝，重建恢复。多实例重建和目录修改需共享同一串行化边界。启动重建走同一机制。
- 当前两仓在 main。Cloud 位于本会话写入范围之外，实际实施需要单独的仓库写入权限。审阅计划期间仅更新 Boot 的 SDLC 文档。

## Proof

- 网关定向 Maven 测试证明同应用的 Key 和 HMAC 授权相同、有角色映射的已授权请求可通过、缓存异常及未授权默认拒绝、跨租户和 IP 规则保持、入口路径改写正确、Bearer 与 CORS 和 token 清理边界保持。JWT 缓存包含同名主体及同 jti 的隔离用例。
- System 和 System API 执行 `mvn -pl macula-cloud-api,macula-cloud-api/macula-cloud-system-api,macula-cloud-system -am test -Plocal`。使用 `mvn -pl macula-cloud-system -am verify -Plocal -Dit.test=ApplicationApiKeyIntegrationIT,ApplicationMenuPermissionsIT -Dfailsafe.failIfNoSpecifiedTests=false` 执行 Flyway 和 Redis 集成测试，运行条件为测试 MySQL 和 Redis。测试证明新表与删除列、全量替换、清空、权限和应用删除、授权管理用户边界、更新及重建失败、重建和提交并发顺序。使用 `mvn -pl macula-cloud-gateway -am verify -Plocal -Dit.test=ApplicationPermissionsGatewayIT -Dfailsafe.failIfNoSpecifiedTests=false` 在连接同一测试 Redis 的 System 与 Gateway 上执行联合验证。用测试管理员经管理 API 授权和撤销，用同一个有效 API Key 调用测试路由，对比授权前拒绝、授权后通过、撤销后拒绝。联合验证也使用已获相应应用授权的有效 HMAC 和 API Key 请求角色授权、菜单及接口目录维护入口，确认拒绝，并以现有已授权用户验证管理操作仍可执行。
- Admin 运行相关 Vitest、`npm run build`，并验证列表入口、回显、清空、错误提示及 API 请求契约。应用旧 URL 字段退出由字段扫描、迁移断言与页面测试共同核对。

## 实施记录

- Boot 与 Cloud 的实现都已写入本地工作区。Cloud 的授权关联使用 `SysApplicationPermissionMapper` 和固定 SQL 直接管理复合主键，省去仅用于中转的两个实体和一个 Mapper。Gateway 两种过滤器直接复用同一判定器，构造和自动装配契约沿用现有入口。
- System 的真实迁移和缓存用例扩充到既有 `ApplicationApiKeyIntegrationIT`。Cloud Gateway 的真实路径改写和签名沿用 `TinyIdGatewayIT`。用相同的值序列化验证 System Redis，完整的部署态 HTTP 联调仍待执行。
- 目录写入通过菜单反向索引定位应用。事务提交前生成新快照并使旧值失效，提交后原子发布。明确回滚时恢复失效前的快照，避免恢复过程再次开启数据库事务。
- 补充检查发现，活跃基线的应用管理菜单使用 `/sys/applications`，原管理角色查找使用 `/system/application`。权限判断支持两个菜单路径。应用列表会返回管理凭据，服务端要求真实用户角色。
- 实施期验证通过 Boot Gateway 的 111 个单元测试、Cloud System 的 65 个定向单元测试、8 个执行真实 MySQL 和 Redis 的集成测试，另有 Cloud Gateway 单元和集成测试 6 个。Admin 40 个定向单元测试通过，生产构建成功。完整测试阶段及部署态 HTTP 联调待确认后进行。
- 根据确认，将授权 Key 固定为 `macula:cloud:system:app_perms:{tenantId}:{applicationId}`。Cloud 发布与 Boot 读取共用同一前缀常量。
