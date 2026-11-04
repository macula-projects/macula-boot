# API Key GlobalFilter 计划审查

## Plan

- 审查方式：未发现已安装的 codex-plan-review，按 sdlc-build 技能回退为独立 sdlc-reviewer 子代理，仅运行 Plan pass。
- 预算：最多三轮；第二轮起只看变更，不重复已记录发现；同类问题归并。未执行代码审查或测试。

### Round 1

- `.sdlc/apikey-global-filter-alignment/plan.md:28` — Important — 标准 Bearer converter 仅在 GET 解析 Query，Authorization 使用 getFirst，不能提供计划声称的 API Key 全方法 Query、重复 Header 和冲突校验 — 为 API Key 增加统一输入边界验证，明确单一来源、重复 Header/Query、格式及 HTTP 方法规则，并在 converter、authorize、GlobalFilter 使用一致解析。
- 处理：已修正 plan 步骤 2/3、风险及单元/完整链路验证范围；spec 的 Design 补充这一输入校验细节。普通 OAuth 保持标准 converter 行为。
- 其余：审查者未发现其他阻断计划问题。

### Round 2

- 仅复核第一轮修订内容；审查结论：统一 API Key 输入检查已覆盖非 GET Query、重复 Authorization/Query、来源冲突及格式边界，并保持普通 OAuth 委托标准 converter；对应验证范围已补齐，无新增阻断发现。
- 实际总计两轮；计划审查通过，等待工程师明确接受 plan.md。此结论不代表源码、运行时验证或人工门禁已通过。

## Deploy — Round 1

- Depth：Standard；Passes：Bugs、Security、Compliance；REVIEW.md 已读取，最多 3 个代码审查轮、最多 5 个 Nit。
- Reviewed SHA：`5ff9b5c8` 加当前未提交工作区；Java/POM scope fingerprint：`23dd9e8c349377d317ad235f6ccfe58ab9166b643bd414c90b892566157ffdd1`。22 个现存变更 Java 文件校验无漂移。
- 验证：隔离副本完整 mvn verify，单元 309 通过，集成 47 执行通过、3 既有条件跳过；真实 Redis 8.10.2，测试端口隔离，代码校验一致。
- Bugs：未确认新增正确性缺陷。
- Security — Important / Pre-existing：`RmOpaqueTokenEndpointFilter.java:139`（同类 `:83`），DEBUG 输出完整 token 清理请求体和失败请求的全部 Header，可能泄漏 OAuth token、API Key 和 Cookie。基线已存在，但按 REVIEW.md 既有 Security 风险仍阻止合并。Disposition：已修复；删除原文日志并补日志捕获断言，Round 2 独立审查及 fresh verifier 均确认关闭。
- 独立 verifier：仅验证该候选，确认实际发生且基线已有；未调整严重性、未新增发现，静态参数路径证明。
- Compliance：未发现适用的额外监管或数据分类控制要求，不虚构结论。
- Counts：posted 1，suppressed 0，verifier-rejected 0，Nit 0。

## Deploy — Round 2

- Depth：Standard；Passes：Bugs、Security、Compliance；REVIEW.md 规则不变。本轮只审查上一轮之后的 RmOpaqueTokenEndpointFilter 及其测试增量。
- Reviewed SHA：`5ff9b5c8` 加当前未提交工作区；Java/POM scope fingerprint：`967ff1ac2a930e9e1cb1015e6f7b6ad5bd4f7e79fae91b3f91ce8f776c49c98c`。578 个 Java/POM 校验一致，漂移 0。
- 验证：完整 `mvn verify`，Surefire 310 全通过；Failsafe 47 执行通过、3 既有条件跳过，失败/错误 0；Checkstyle 和 git diff --check 通过。
- Bugs：无新增发现；日志修改未改变认证、解析或清理行为。
- Security：无新增发现；上一轮 Important / Pre-existing 已修复。独立 fresh verifier 仅复核原问题，确认当前代码不再输出原始 Header/Body、异常消息或堆栈，日志捕获回归测试覆盖三条路径，原问题关闭。
- Compliance：无适用的额外控制要求，无新增发现。
- Counts：posted 0，suppressed-as-already-reported 1，verifier-rejected 0，Nit 0；关闭 1，未解决 0。本次总计 2 个代码审查轮，未超过上限 3。

## Recurring

扫描 `.sdlc/*/review-findings.md`，未发现凭据日志泄漏类问题同时出现在两个或以上独立变更中，无需新增重复问题编码。现有 REVIEW.md Security pass 已明确检查 sensitive-data logging，当前实例由日志捕获测试约束。

## 人工门禁

本地审查及验证完成，证据与 PR 文案已准备。审查完成时未提交、推送或创建远端 PR。用户随后明确授权“下一步提交、推送”，据此在独立分支提交并推送；未授权创建 PR、合并或部署，不自批人工门禁。
