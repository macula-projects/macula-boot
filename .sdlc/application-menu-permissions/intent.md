# Intent: 应用菜单与接口授权
Author: Rain. Status: accepted.

## Problem

管理员在应用管理和 API Key 管理中通过 URL 文本配置接口访问范围，无法像设置角色权限一样按菜单授权。API Key 可以配置独立的 URL 范围，同一应用的不同凭据因此可能拥有不同接口权限。网关当前还会在角色规则命中时拒绝 API Key 和 HMAC 请求进入后续凭据校验。

## Proposed outcome

- 管理员在应用管理中参照角色权限设置勾选菜单。应用的菜单关联和菜单对应的接口权限分别记录在 `sys_application_menu` 与 `sys_application_permission`，接口权限复用现有权限目录。
- 应用与 API Key 移除 `permitUrl` 相关配置及其存量数据。新授权由管理员通过菜单设置。应用与接口的授权关系同步到 `systemRedis`，网关依据已验证凭据所属的应用检查接口权限。同一应用的有效 API Key 与应用 HMAC 使用同一套接口授权。
- 移除 API Key 和 HMAC 请求的角色权限前置判断，接口访问由应用授权决定。同一接口可以同时配置角色授权和应用授权。网关仍各自核验凭据有效性、租户归属和来源 IP。

## Affected users and systems

管理员、现有应用 HMAC 和 API Key 调用方、Macula Cloud Admin 与 System、System Redis、Macula Boot Gateway Starter。

## Constraints

- 沿用角色权限设置的菜单授权方式以及接口权限目录。普通 Bearer 请求继续按现有角色权限规则鉴权。
- 应用接口授权按租户边界生效。授权调整、撤销和权限目录变更需要反映到网关使用的授权关系。
- 现有凭据、认证租户和来源 IP 规则继续各自生效。

## Open questions

- 已明确废弃存量 URL 规则，不迁移、不回填。
- 网关应用授权无匹配或授权缓存不可用时，拟拒绝应用凭据请求。现有公开和内部入口的具体边界在设计阶段核对。
