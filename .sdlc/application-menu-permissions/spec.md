# Spec: 应用菜单与接口授权（源自 intent.md，2026-11-27）
Status: accepted

## Source intent

[已确认的应用菜单与接口授权意图](intent.md)。

## Requirements

1. 应用管理员可在应用列表打开权限设置、查看并勾选当前租户菜单。保存后，应用的有效接口权限等于所选菜单关联的接口权限。清空勾选即撤销全部接口权限。
2. 应用和 API Key 的 `accessPath`、数据库 `access_path` 与凭据缓存 `permitUrls` 退出当前契约。本次直接启用新授权，同时删除旧字段和旧值。新关系表初始为空，由管理员通过菜单设置权限。
3. 业务 API Key 与 HMAC 认证成功后，按凭据所属应用及其归属租户的接口授权判断。现有角色 URL 规则对这两类凭据的分流和判定均无影响。角色和应用可以分别取得同一接口的授权。
4. 含应用凭据的路由请求只有在凭据校验、租户及来源 IP 准入、应用接口规则匹配均通过后才转发。无匹配、身份字段不完整或权限缓存不可读时拒绝。授权撤销和目录变化须同步到 Gateway 判定。
5. 普通 Bearer 继续走角色鉴权，保留现有 CORS 预检、匿名公开路径及本地 token 清理入口的鉴权契约。业务路由即使位于 `ignore-urls`，携带业务应用凭据时仍执行应用校验。

## Non-goals

OAuth 客户端和用户角色算法、租户及 IP 策略、凭据生命周期、Gateway JWT 声明均沿用现有契约。上线和环境变更另行授权。

## Design

- **Admin 与 System**。用角色权限设置的菜单树交互新增应用权限入口。System 在查询和保存时校验调用方为当前租户内有权管理应用的用户，并单独登记相应的角色 URL 权限。保存是全量替换，服务端验证应用以及全部菜单 ID 的租户归属，事务内更新两张关联表。服务端维护接口关联，输入仅包含菜单 ID。菜单移除、接口增删改及应用删除时同步维护相关关联。专属管理操作依据服务端用户管理权限执行，业务应用身份无法为自己授权。
- **System Redis**。每个归属租户和应用发布一份完整的 URL 规则快照。授权及目录写入与缓存重建按相同的共享锁串行化，涉及多应用的目录变更先确定受影响范围。事务提交前使受影响快照失效，提交后根据持久化结果原子发布完整值；回滚后按持久化状态重建。失效失败则中止写入，发布失败则保留拒绝状态，记录可运维的错误并支持重建。System 启动与授权缓存重建复用同一发布路径。空授权集以空快照表示。
- **Gateway**。Spring Security 只把识别出的业务 API Key 和 HMAC 分流到各自 GlobalFilter，移除角色规则查询。各过滤器共享应用权限判定，在确认身份并通过租户和 IP 校验后、发布可信 principal 前查询快照。应用 ID 和归属租户来自凭据缓存，HMAC 发布方补充应用 ID。`authTenantId` 继续用于 JWT 业务租户。Redis 读取沿用隔离阻塞调用的调度方式。规则使用现有接口目录的 HTTP 方法与 Ant 路径语义，比对路由改写前的网关入口路径，核对 StripPrefix、RewritePath 与白名单路由。无登记或无匹配规则返回拒绝。
- **入口边界**。匿名请求沿用 Gateway 公开路由规则。OPTIONS 预检继续通过现有 CORS 处理。本地 token 清理使用 `RmOpaqueTokenEndpointFilter` 自己的资源服务器 API Key 校验。现有业务白名单路由的匿名可访问性与其应用凭据准入分别判定。

## Data and interfaces

| 边界 | 契约 |
| --- | --- |
| 数据库 | 新增 `sys_application_menu(tenant_id, application_id, menu_id)` 和 `sys_application_permission(tenant_id, application_id, permission_id)`，分别约束三列唯一并支持按菜单和权限反查受影响应用。关联目标为 `sys_application_tenant`、`sys_menu_tenant` 和 `sys_permission_tenant`。用增量 Flyway 迁移移除应用和 API Key 表的 `access_path`。 |
| 管理 API | `GET /api/v1/sys/applications/{appId}/menuIds` 返回已选菜单 ID；`PUT /api/v1/sys/applications/{appId}/menus` 接收菜单 ID 列表并替换授权。空列表表示撤销，缺少请求体是无效请求。 |
| Redis | 在 `systemRedis` 使用 Boot 公共常量定义的授权键 `macula:cloud:system:app_perms:{tenantId}:{applicationId}`，值为完整的 `METHOD:/入口路径模式` 列表。凭据缓存重建显式清理已存在的 `permitUrls` Hash 字段。 |

## Flagged concerns

- 管理边界: 新的应用授权可以覆盖过去仅由角色规则覆盖的接口。切换前核对应用管理、角色授权、菜单权限维护等原先依赖网关角色拦截的管理接口，并为只接受用户身份的操作落实服务端约束。用有效应用凭据验证这些操作仍会被拒绝。 [blocking]
- 缓存收敛: 当前角色缓存采用先删后写。应用快照按上述失效与发布约束实现，重点验证撤权后发布失败及多实例重建排序。 [non-blocking]

## Verification strategy

- System 测试覆盖正常授权、清空、非法租户和角色、接口目录增删改、应用删除、事务回滚、Redis 故障和并发重建。Flyway 测试覆盖基线及存量升级，核对旧字段已删除、新表初始为空，并验证未设置授权的应用默认拒绝。
- Gateway 测试覆盖同应用多把 API Key 与 HMAC 授权一致、角色规则命中时已授权应用通行、未授权拒绝、其他租户、来源 IP、凭据错误、缓存不可用、路径重写、匿名公开路由、CORS 与本地 token 清理。普通 Bearer 走现有角色规则。
- Admin 测试覆盖菜单回显、保存、清空、失败和旧 URL 字段移除。联合验证使用真实 System Redis 检查授权生效与撤销后拒绝。
- 依据 Boot 与 Cloud 的 `AGENTS.md`、相关架构和测试规则、Cloud 后端和管理端规则、Boot `REVIEW.md`。本阶段按 `sdlc-design` 交由人工确认。
