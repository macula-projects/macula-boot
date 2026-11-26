# Intent: Gateway 凭据与 URL 授权调整
Author: Rain. Status: accepted.

## Problem

当前 Gateway 工作区修改了不同凭据的 URL 授权行为，以及签名校验遇到密钥解密错误时的响应。相关测试仍依赖旧构造方法，验证无法编译。README、示例和项目生成模板还描述已移除的授权绕过配置。

## Proposed outcome

- Bearer 请求始终依据现有 URL 角色规则鉴权。API Key 和 HMAC 请求无法访问配置了角色权限的 URL，其它请求仍由相应凭据校验链核验。
- 签名密钥解密失败时返回稳定的加密错误，客户端响应不包含底层异常详情。
- 更新测试、文档、示例及生成模板，明确配置移除后部署方需要检查的路由。当前分支通过验证和独立审查后推送并请求合并。

## Affected users and systems

Macula Boot Gateway Starter、使用 Bearer、API Key 或 HMAC 的调用方、引用旧配置的网关部署及示例。

## Constraints

- 保留用户当前工作区改动。CORS、CSRF、预检处理、可信身份来源与原凭据核验次序沿用现有边界。
- 合并目标为 Macula Boot 的 main。使用远端受保护分支的正常 PR 流程。

## Open questions

- 无。用户确认移除两个配置项并同步文档、示例及生成模板。
