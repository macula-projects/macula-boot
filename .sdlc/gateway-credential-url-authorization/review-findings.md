# 实施计划审查

## Plan

REVIEW.md 未指定计划审查器，环境没有默认审查器。按 sdlc-build 使用独立子代理执行 Plan 审查，最多三轮。

### 第 1 轮

- Important · 已纳入待确认计划。Cloud 刷新 URL 权限映射会短时间产生空缓存，已配置角色的 URL 不能在这段时间退回应用凭据校验。Boot 在无可核验快照时拒绝应用凭据，并增加缺失和空快照测试。
- Minor · 已修订。现有 API Key 测试把开放和角色受保护路径混用。区分两种路径并保留端到端凭据检查。
- Important · 已修订计划。新增的应用凭据 URL 查 Redis 使用延迟订阅和有界调度，不能在 WebFlux 事件循环中同步执行。
- Minor · 已修订计划。沿用既有白名单的前置处理，文档明确角色保护路径不得与该配置重叠。
- Minor · 已修订计划。同步 Alibaba 示例说明的受保护 POST 和 WebSocket 握手迁移。

## Code

- Round 1，在 `a57f8913` 基础上对实现差异独立执行 Bugs、Security、Compliance。无 Important。
- Minor · 已修订。补方法级 Javadoc，并将 Redis 故障的逐请求日志调整为 DEBUG。
- 修订后复测全量 Maven 和变更 Java Checkstyle。
