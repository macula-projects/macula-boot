# Spec: 网关跨域认证响应与端点处理修正 (from intent.md 2026-11-18)
Status: accepted

## Source intent
[已接受的意图](intent.md)，用户于本轮以“OK”确认。

## Requirements
1. 配置 CORS 后，允许来源的正常请求和认证失败响应均包含正确的跨域响应头，保留原状态码和错误正文。
2. 已配置路径上的非法来源、方法或请求头被跨域处理拒绝；合法预检提前结束，不触发认证内省或业务处理。
3. 保护配置端点遵守跨域拒绝结果；预检不返回业务正文，同源实际请求保持原返回格式。
4. 未配置 CORS 时，保持既有同源与非浏览器认证行为，不新增任意来源放行规则。
5. Alibaba 示例使用有限的本地可信来源并关闭浏览器凭据；文档说明生产来源替换、签名请求头和 Cookie 认证的注意事项。
6. 保留显式 Token 认证和 CSRF 禁用；保留现有未提交改动。

## Non-goals
不关闭 Query Token，不改变 API Key/HMAC/Opaque Token/JWT 契约，不调整 URL 权限规则，不修改真实环境配置，不提交、推送或部署。

## Design
- Security 启用 CORS，复用现有基于 GlobalCorsProperties 的 CorsConfigurationSource，在认证前执行校验；无有效配置源时不添加开放规则。
- Gateway 路由层继续使用同一 globalcors 配置，保留未匹配路由预检的处理；验证响应头不重复。
- ProtectUrlsEndpointFilter 在生成正文或读取保护配置前处理 CORS；校验失败或属于预检时直接结束。其执行早于 Security，须保留自身跨域处理。
- 示例明确列出 http://127.0.0.1:5099、http://localhost:5099、https://localhost:5443、https://127.0.0.1:5443，allow-credentials 为 false；保留所需方法与请求头的兼容性，不推断生产域名。
- 按 AGENTS.md 补充修改方法的中文 Javadoc、边界注释及模块说明。

## Data and interfaces
无数据库或 API 结构变化，无新依赖及配置键。已有 globalcors 配置将提前影响安全过滤链；示例之外的部署来源与凭据策略不自动变更。

## Flagged concerns
- 现有部署可能依赖宽泛来源或 Cookie 凭据：本次只收窄本地示例，生产需自行配置可信来源；Cookie 认证接口仍需独立 CSRF 防护，non-blocking。
- 特殊端点早于 Security：必须在自身处理跨域并覆盖拒绝和预检，否则仅启用 Security CORS 无法覆盖，non-blocking。
- Query Token 的日志泄漏风险：沿用已接受意图，不在本次关闭，non-blocking。
- 当前 ResourceServerConfiguration.java 存在用户修改：以工作区内容为基线，保留默认 URL 权限检查和内省器修改，non-blocking。

## Verification strategy
使用现有 JUnit、Mockito、ApplicationContextRunner、WebTestClient/模拟 exchange；不连接真实 Redis、IAM 或云服务。覆盖允许/拒绝来源、预检及非法方法/请求头、认证失败状态正文与跨域头、特殊端点无业务处理、同源/无配置行为、响应头无重复。先执行新增定向测试，再执行 Gateway Starter 及其依赖的单元测试；报告实际失败、跳过和环境限制。运行 git diff --check 并核对用户原有改动。

## Governing policies
已读取 AGENTS.md、REVIEW.md、.agents/rules/architecture.md、starter-development.md、testing.md，以及 sdlc-plan、sdlc-design、sdlc-build 技能。无另行发现的组织安全或数据处理技能；不声称执行未发现的政策。
