# Intent: 网关跨域认证响应与端点处理修正
Author: Codex. Status: accepted.

## Problem
网关路由层的跨域处理晚于 Spring Security，允许来源的浏览器客户端在认证失败时可能无法读取真实的 HTTP 状态码和错误正文。保护配置端点未按跨域校验结果停止处理，预检仍可能输出业务正文。Alibaba 示例允许任意来源携带凭据，不适合作为生产配置直接使用。

## Proposed outcome
- 已配置的允许来源可以读取认证成功与失败的响应；非允许来源不能读取跨域响应。
- 合法预检正常结束，不执行业务处理、不输出业务正文；被拒绝的跨域请求停止处理。
- 示例明确可信来源和浏览器凭据策略，文档说明生产配置要求。
- 保持当前显式 Token 认证及 CSRF 禁用的适用边界。

## Affected users and systems
使用 Gateway Starter 的浏览器客户端、网关维护者、保护配置端点及 Alibaba Gateway 示例。

## Constraints
- 保留用户已有未提交改动，特别是默认 URL 权限检查配置与内省器相关改动。
- 本次不调整 Query Token 兼容行为，不改变 API Key、HMAC、Opaque Token 或下游 JWT 的认证授权契约。
- 不变更真实部署域名、凭据或外部配置中心；示例域名仅作为占位说明。
- 验证须覆盖允许和拒绝来源、预检、认证失败响应、特殊端点，以及无 CORS 配置时的既有请求行为。
- 不提交、推送或部署。

## Open questions
- 无阻塞性的业务问题；真实生产来源由部署方按环境配置，不在仓库中推断。
