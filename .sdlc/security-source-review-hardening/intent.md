# Intent: 源码安全审查问题修复

当前范围更正：Rain 要求恢复原 MyDataPermissionHandler 并重新评估；SQL 注入/OR 修复目标尚未完成，参数框架已撤销。保留其他已接受修复，PR 维持草稿，不代表接受原 SQL 风险。

Author: Rain. Status: accepted.

## Problem
当前源码的数据权限条件存在用户名 SQL 注入及 OR 分组绕过问题；请求指定租户缺少负责人授权校验。网关使用内置签名私钥，资源服务器存在固定默认签名 Secret，网关 Token 缓存可能在过期后继续接受身份，请求体聚合缺少大小上限，日志可能暴露密钥和业务明文。这些行为可能影响业务数据隔离、身份可信性、服务可用性与信息保密。

## Proposed outcome
- 用户名中的特殊字符不能改变 SQL 语义，已有 OR 查询条件仍完整受数据权限约束；按 Rain 后续要求使用 JDBC 参数绑定，部门及子部门保留在原 SQL 的子查询中，不增加额外 JDBC 查询或 MySQL 专用函数。
- 请求指定租户仅在当前身份具有 ROOT 角色时生效，非 ROOT 调用方不能通过请求参数跨租户访问。
- 网关签名密钥支持部署者配置，按 Rain 后续要求优先兼容，未配置时保留现有默认签名材料；资源服务器不再依赖公开的默认签名 Secret。
- Token 到期后不再因本地缓存继续通过认证。
- 大请求被明确限制，日志不输出密钥及未经脱敏的敏感业务明文。

## Affected users and systems
Macula Boot MyBatis-Plus、Gateway、Security Starter 的使用方、租户负责人、业务用户和运维人员；受配置调整影响的网关示例与 Archetype 模板。

## Constraints
- 保留缺失或 DEFAULT dataScope 时不追加数据权限条件的现有设计，本次不收紧该策略。
- 按用户指定，在 MyBatisPlusAutoConfiguration 中复用 SecurityUtils.isRoot() 判断当前身份是否具有 ROOT 角色；仅 ROOT 允许采用请求指定的租户 ID，不引入其他租户负责人角色或属性。
- 按用户指定，将 JwtConfiguration 使用的密钥配置纳入网关配置项；第五点以兼容优先，保留旧部署不配置时的默认签名行为，不删除现有 jose.jks 资源。
- 保留 @Inner 的现有行为和慎用约定，本次不改造内部调用认证协议。
- KongApiGlobalFilter 的 invoker 主体覆盖行为暂缓，等待用户后续决策，本次不修改。
- 其余按本轮安全扫描建议修复，同步维护相关 README 与回归验证。
- 保持 Java 17 兼容和 Starter 可覆盖能力；不操作真实部署密钥，不执行密钥轮换、发布、提交或推送。
- 源码修改须在 spec.md 和 plan.md 经人工接受后进行。

## Open questions
- 未提供网关签名配置时保留原有默认加载行为，已由 Rain 明确；请求体默认上限 1 MiB、非 ROOT 忽略请求租户、第六点移除资源服务器默认 HMAC Secret 均已随 spec 获得 Rain 显式接受，暂无未决需求。
