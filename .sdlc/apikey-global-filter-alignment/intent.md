# Intent: API Key 与 HMAC 网关处理链路对齐
Author: 待确认. Status: accepted.

用户在本次会话确认迁移方向：“OK，帮我改改”。具体兼容边界及遗留策略由 spec.md 列明后提交审阅。

## Problem
当前 API Key 和 HMAC 均用于系统间调用，但分别在 Spring Security 授权之前、之后验证凭据。API Key 依赖删除 Authorization 请求头和读取认证上下文实现分流，增加授权与 JWT 转换的特殊处理，使两种认证方式的行为难以一致维护。

## Proposed outcome
API Key 接入 Gateway GlobalFilter 体系，与 HMAC 的处理阶段保持一致；无效凭据无法到达下游，JWT 只根据已验证的身份生成，每个请求仅执行一次下游过滤链。普通 OAuth Bearer 和 HMAC 调用继续正常工作。

## Affected users and systems
使用网关 API Key 的系统间调用方，以及网关的 Spring Security 认证、授权、API Key 校验和 JWT 转换组件。

## Constraints
遵循仓库认证链路变更的 SDLC 门禁，未经接受的 plan.md 不修改源码。迁移必须处理 Bearer sk- 与普通 OAuth Bearer 的区分，关闭 API Key 功能或缺少可信验证结果时不得因前缀分流而放行到下游。避免日志或转发请求泄漏原始 API Key。

## Open questions
- 本轮是否同时对齐 HMAC 的网关归属租户、permitUrls 和 authTenantId 策略，还是仅调整处理阶段？
- Query access_token 形式的 API Key 是否作为正式支持方式保留？
- JWT 缓存身份是否在本轮区分 API Key 与 HMAC，避免同名应用复用不同 claims 的缓存？
- 除现有链路维护成本外，是否有需覆盖的线上故障、调用场景或额外兼容约束？
- intent 作者署名待确认。
