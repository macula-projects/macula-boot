# Intent: 应用与 APIKEY 的多行 URL 和允许 IP 配置
Author: Rain. Status: accepted.

## Problem
应用及 APIKEY 的可访问 URL 当前以逗号分隔，管理员希望改为每行填写一条规则；同时无法在应用和 APIKEY 上配置允许访问的来源 IP。

## Proposed outcome
- 应用与 APIKEY 的可访问 URL 均使用回车换行表示多条规则，编辑、保存、回显及实际访问校验保持一致。
- 应用与 APIKEY 均增加“允许 IP”，使用回车换行表示多个 IP，并在访问时执行限制。
- 保留现有 URL 的 HTTP方法:/路径及通配符语义。
- URL 与 IP 配置修改后，对相应应用凭据和 APIKEY 的后续访问生效。

## Affected users and systems
应用管理员、应用凭据和 APIKEY 调用方；Macula Cloud Admin、System、System API、Gateway，以及 Macula Boot 网关公共鉴权逻辑。设计阶段需核对 IAM 调用链影响。

## Constraints
- 应用和 APIKEY 两个管理入口均支持，不能只改变表单提示而不改变访问校验。
- 现有 APIKEY URL 配置优先、未配置时继承应用 URL 的规则保持。
- 保持应用身份、租户与管理权限边界；来源 IP 不能直接信任调用方可伪造的请求头。
- 同步必要的数据升级、缓存、说明及测试。
- 遵守仓库的人工审批门禁。

## Open questions
无。用户于 2026-11-10 明确确认：
- 兼容已有逗号分隔的 URL，管理端统一按换行回显和保存。
- 应用 IP 留空不限制；APIKEY IP 留空继承应用，非空使用自身配置，处理方式参考 permitUrls。
- IP 支持 IPv4、IPv6 和 CIDR 网段。
- Author 填写 Rain。
