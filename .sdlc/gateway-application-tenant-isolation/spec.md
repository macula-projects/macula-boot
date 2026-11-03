# Spec: 网关应用接入租户隔离 (from intent.md 2026-11-03)
Status: accepted.

## Source intent
[intent.md](intent.md)，Rain 已于会话中明确回复 YES 接受。

## Requirements
1. 从 System 租户编码缓存按 spring.application.name 以 tenantCode 开头的规则 匹配网关归属；多个匹配取最长前缀，不区分大小写。应用名缺失、映射缺失、未匹配或命中 ID 非法时返回 -1，并拒绝内置 AK/SK 接入；应用归属必须存在且与网关归属相等，否则返回 403。不再提供 macula.gateway.tenant-id 配置。
2. 应用缓存保留 tenantId 为归属租户，新增 authTenantId 为认证租户。JWT 的 tenantId 优先取 authTenantId；未配置时回退到归属 tenantId，不引入新的默认业务租户。
3. 非空但格式非法的 authTenantId 必须拒绝认证，不能按未配置处理。
4. 接入检查覆盖普通 HMAC 请求及使用同一验签逻辑的 opaque token 清理端点，且发生在 JWT 签发或端点副作用之前。
5. 可信认证结果仅在签名与路径权限校验成功后写入请求属性；AddJwtGlobalFilter 从该属性获取认证租户，不再通过 ThreadLocal 传递 HMAC 租户。
6. 应用配置支持新增、编辑、读取和清空可选 authTenantId；清空表示恢复归属租户回退。缓存刷新必须体现该规则，发生异常时恢复原租户上下文。
7. 租户管理负责租户编码到 ID 的 Hash：System 启动时初始化；租户新增、修改、删除在事务提交成功后重建，回滚不得刷新；应用缓存刷新不再维护该 Hash。samples 沿用现有 application.name，不增加手工租户配置。

## Non-goals
本轮不修改 OAuth Client、用户登录、API Key 认证、API URL 白名单默认策略、上游调用名称逻辑或 IAM 签发流程。不提交、推送、部署或执行真实数据库迁移。

## Design
- KongApiGlobalFilter 和 RmOpaqueTokenEndpointFilter 从服务端注入 spring.application.name，并将其传入 checkSign；KongApiUtils 内部统一解析 System 租户缓存并校验归属。
- 归属租户只决定接入资格；认证租户只决定 HMAC 转 JWT 的业务上下文。网关应用名来自服务端配置，归属 ID 来自租户映射，不接受请求头覆盖。
- 保留 TenantContextHolder 公共 API 及旧 checkSign 方法签名以兼容调用方；网关内置调用点全部使用传入 applicationName 的入口。旧签名继续不限制接入租户；按 Rain 后续要求，所有签名均不读写 TenantContextHolder，仅在请求属性中传递认证结果。
- JWT 缓存沿用原 buildKey，实现已按最终认证租户区分；不增加 HMAC 专用命名空间或编码。每次请求仍先执行接入与签名检查。
- Cloud System 沿用 Entity/Form/BO/VO/Converter/Mapper 链路增加字段；管理端应用编辑页增加“认证租户”，说明留空回退到归属租户。

## Data and interfaces
- sys_application_tenant 增加可空 BIGINT auth_tenant_id，使用新的 Flyway 迁移，不修改已执行的 V1。旧记录保持 NULL，不改写历史归属租户。
- 应用接口增加可选 authTenantId，旧调用方省略字段时不意外覆盖既有值；显式 null 表达清空，实现阶段须区分缺省与显式清空。
- Redis 新增 macula:cloud:system:tenants Hash，tenantCode → tenantId，由租户管理在启动及事务提交后重建。
- Redis macula:cloud:system:apps:{AK} 结构保持不变，tenantId 含义为归属租户；新增 authTenantId，刷新时可写入已回退的有效认证租户。
- 新增常量表达缓存字段及可信请求属性；同步更新 Boot Gateway 与 Cloud System README。

## Flagged concerns
- 跨归属租户配置认证租户的授权：Rain 已接受仅平台 ROOT 可配置与归属不同的认证租户，普通管理者只能使用归属租户；服务端必须校验，resolved。
- API Key：本轮按用户明确的 AK/SK 范围实施，API Key 不具备新增接入隔离；启用 API Key 的部署仍须单独评估入口，non-blocking。
- 滚动升级：旧网关忽略 authTenantId，必须先升级网关，再启用不同于归属租户的认证租户；回滚旧网关前须恢复两者一致，non-blocking。
- 缓存启用：Rain 已接受无命中返回 -1 并拒绝；启动初始化及租户事务提交后自动刷新映射，重建期间可能短暂拒绝接入，non-blocking。
- 数据库及管理端：可维护的认证租户需要字段迁移、接口和表单配套，不能只修改 Redis；迁移只交付脚本，不操作真实数据库，non-blocking。

## Verification strategy
- 网关单元测试覆盖最长前缀、无匹配/空应用名/空缓存/非法 ID 返回 -1 并拒绝；归属匹配/不匹配/缺失、旧认证租户回退、新认证租户、验签失败以及请求间租户不串用。
- 覆盖普通 HMAC 与 token 清理入口，验证拒绝请求不会签发 JWT 或清理 token。
- Cloud System 测试覆盖字段保存/读取/清空、旧请求保留既有值、跨租户配置权限、缓存归属与认证租户分离，以及刷新异常恢复上下文。
- 检查 Flyway 增量迁移、前端表单字段与 samples YAML；真实数据库升级和端到端认证另行报告，不能以 mock 测试代替。
