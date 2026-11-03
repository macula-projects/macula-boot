# Intent: 网关应用接入租户隔离
Author: Rain. Status: accepted.

## Problem
网关目前按 AK 查找共享应用凭据，无法限定哪些归属租户的应用可以接入本网关。应用归属租户同时用于生成 JWT，无法独立表达认证后的业务租户。

## Proposed outcome
网关可限制仅允许指定归属租户的 AK/SK 应用接入。应用的归属租户与认证后的业务租户分别表达，下游继续按 JWT 的业务租户隔离数据。旧部署和旧缓存保持兼容。

## Affected users and systems
Macula Boot Gateway Starter、Macula Cloud System 应用配置及缓存维护、Macula Samples Gateway 的 application.yml，以及第三方 AK/SK 调用方。

## Constraints
- 按 Rain 后续确认，网关根据 spring.application.name 从租户编码缓存匹配归属，使用忽略大小写的最长前缀；未命中默认 -1 并拒绝，不再使用手工 tenant-id。
- 应用缓存 tenantId 保留为归属租户，新增 authTenantId 表达认证后的业务租户。
- 生成 JWT 优先使用 authTenantId；旧缓存缺少该字段时回退到 tenantId，保持原行为。
- 用户指定的 samples 网关已有 spring.application.name=macula-samples-gateway，沿用该名称；移除本次新增的手工 tenant-id 配置。
- 保留 TenantContextHolder 公共 API；网关不依赖它传递 HMAC 租户，也不接受调用方自行指定网关归属。
- 保留无关改动，不提交、推送或部署。

## Open questions
- 本轮用户明确要求 AK/SK 接入限制；API Key 同类限制是否一并纳入，需在规范阶段明确。
- authTenantId 的持久化、应用管理表单维护与缓存刷新需要在规范阶段确定具体文件范围。
