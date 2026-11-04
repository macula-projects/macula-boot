# Spec: API Key 与 HMAC 网关处理链路对齐 (from intent.md 2026-11-04)
Status: accepted

用户于 2026-11-04 回复“YES”，接受本设计及列明的兼容边界。

## Source intent
[已接受的迁移方向](intent.md)：API Key 使用 GlobalFilter 验证，再由 AddJwtGlobalFilter 转换已验证身份。

## Requirements
1. 保留 Authorization: Bearer sk-xxx 及 Query access_token=sk-xxx 调用方式；API Key 不进入 OAuth introspection，普通 OAuth Bearer 继续按原认证与角色授权流程处理。
2. API Key 在 Spring Security 中仅分流，在 GlobalFilter 中验证；无效、缓存数据非法或 Redis 故障时不能到达下游。
3. apikey.enabled=false 时明确拒绝 API Key，包括忽略认证的 Gateway 路由，不允许退回 OAuth 认证或转发原始凭据。
4. JWT 只能从 API Key 已验证身份生成；每个请求的下游过滤链仅执行一次。转发时清除原始 access_token，日志不得输出密钥。
5. 保留 API Key 现有 claims，隔离 API Key 的 JWT 缓存键，防止与 HMAC 同名应用串用；API Key 仅增加认证类型段，所有来源使用当前统一的 tn 租户段和 jti 优先规则。
6. API Key 复用 TenantUtils.resolveGatewayTenantId，根据网关 spring.application.name 解析归属租户，与 API Key 缓存记录中的 tenantId 比较。网关租户解析为未知值、API Key 租户缺失、非法或不匹配时拒绝；不得使用调用方租户 Header 决定归属。

## Non-goals
API Key 不增加有效期、轮换及防重放策略；不修改 HMAC 协议、OAuth 权限或 Trace ID 行为。

## Design
API Key 与 HMAC 使用不同凭据及缓存记录，不共用签名算法；KongApiUtils.checkSign 按用户追加要求只负责纯验签。两种凭据保留独立验证入口，复用 GlobalFilter 的验证阶段、请求级可信身份传递模式和现有 JWT 生成方法。租户准入按用户确认的范围参考 HMAC，将 resolveGatewayTenantId 抽到 TenantUtils 共用，并检查 API Key 归属；不通过认证类型开关把两种验证塞入 checkSign。

统一 API Key 输入识别及校验逻辑，拒绝重复 Header/Query、Header 与 Query 同时提供凭据及非法格式；Query API Key 在各 HTTP 方法使用相同校验。Bearer converter 对通过输入校验的 API Key 返回空，其他请求仍委托标准 converter，并保留普通 OAuth 既有请求格式和 GET Query 规则。标准 converter 不能替代 API Key 对非 GET Query 与重复 Header 的显式检查。authorize 在普通 Bearer 分支之前识别 API Key，仅允许进入 Gateway 验证阶段，不建立已认证身份。

ApiKeyGlobalFilter 改为 GlobalFilter，在 AddJwtGlobalFilter 前验证 Redis 记录，使用 boundedElastic 执行阻塞 Redis 查询；不再删除 Authorization 或构造虚拟 OAuth2AccessToken。清除旧的可信属性后，验证成功才保存请求级可信 principal。

API Key Bean 定义位于 ResourceServerConfiguration，与 KongApiGlobalFilter 集中管理。API Key 过滤器始终装配，读取现有开关：关闭时拒绝识别到的 API Key，非 API Key 请求直接通过。按用户要求重命名为 ApiKeyGlobalFilter，Bean 名为 apiKeyGlobalFilter，新增用户 Bean 回退条件。方法签名由 WebFilter 改为 GlobalFilter，直接依赖旧接口的下游定制需迁移。

AddJwtGlobalFilter 在普通 Bearer 之前处理 API Key，要求存在可信 principal，再生成 JWT、清理 Query 凭据并执行下游链。移除原先基于 SecurityContext 的 API Key 兜底及 Mono<Void>.switchIfEmpty 写法。按用户追加要求，KongApiGlobalFilter 在应用准入及验签成功后根据已验证应用字段构造 principal，保留 invoker、tokenId 和业务租户规则，写入 HMAC_PRINCIPAL_ATTRIBUTE。AddJwtGlobalFilter 只解析各来源已验证 principal，统一生成 JWT、清除 access_token 并覆盖 Authorization；不再构造 HMAC principal。旧 HmacIdentity 中间结果已移除，principal 是应用身份传递的统一载体。

## Data and interfaces
不修改 Redis API Key 记录结构或增加依赖。新增请求级身份属性和 API Key 专用 JWT 缓存命名空间；旧 API Key JWT 缓存不再读取，保留自然过期。README 说明执行顺序、开关、Query 支持及接口迁移。

## Flagged concerns
- API Key 新增租户准入：历史租户缺失、非法或跨归属网关的调用将被拒绝，升级前需确保网关租户映射和 API Key 归属缓存就绪；已按用户要求纳入范围，non-blocking。
- API Key 按用户最新实现增加 permitUrls 应用 URL 许可；认证业务租户按用户当前实现共用 authTenantId 优先、归属租户回退规则，non-blocking。
- 未路由到 Gateway 的本地 WebFlux 端点不会执行 GlobalFilter；这些端点不再获得 API Key 认证身份。用户已接受此兼容边界，本轮不为本地端点增加 API Key 认证，non-blocking。
- 保留 Query 凭据会延续 URL 被访问日志等记录的风险；本轮仅确保不向下游转发，non-blocking。
- 直接依赖 ApiKeyGlobalFilter 的 WebFilter 接口的下游定制需要适配，需记录迁移说明，non-blocking。
- intent 作者署名待确认；不影响技术设计，non-blocking。

## Verification strategy
使用现有 JUnit、Mockito、Spring Test 和 WebTestClient；不增加真实 Redis 或 IAM 依赖。覆盖 Bearer/Header/Query 分流、无效及冲突凭据、开关关闭、Redis 异常、非法缓存、可信身份缺失、JWT claims 与缓存隔离、凭据清理及下游执行一次。租户检查覆盖同租户通过、跨租户拒绝、网关租户未知、API Key 租户缺失或非法、伪造租户 Header 无法改变结果。通过真实 Spring Security WebFilter 链和 Gateway 过滤链配合模拟 introspector/Redis，证明 API Key 不调用 introspection，普通 Bearer 仍调用且受角色授权约束，HMAC 行为不变。先运行定向测试，再运行网关模块及依赖测试。

适用政策：AGENTS.md、REVIEW.md、.agents/rules/starter-development.md、.agents/rules/testing.md 和 .agents/rules/architecture.md。未发现额外组织安全技能；遵循 Java 17、标准版权头/Javadoc、可覆盖装配与中文 README 约束。

## 用户追加的验签职责调整
用户要求 checkSign 只验签：新 API 接收请求与服务端密钥，不读缓存、不做应用授权、不写身份。应用查询、TenantUtils.checkTenant、URL 许可和 principal 构造移至 KongApiGlobalFilter.validate；token 清理端点复用完整 validate，避免授权旁路。旧 Redis 参数 checkSign 重载移除，README 列出迁移方式；不改变 HMAC 请求签名格式。

## 最终实现确认
用户在持续调整代码后明确回复“好的，我写好了。确认”。最终实现以当前工作区和 plan.md 的用户追加调整记录为准；本文同步记录确认后的 authTenantId、tn 缓存段和移除旧身份结构等契约，验证状态独立记录于 test-report.md。

用户最新实现将 TenantUtils.checkTenant 的输入统一为应用 Map，API Key 同样执行 permitUrls 许可校验。输入来源唯一性和非 API Key 请求分流仍是已接受的兼容及安全约束；验证发现遗漏后补回，保留最新代码结构。
