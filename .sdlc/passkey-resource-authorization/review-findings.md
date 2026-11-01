# Review findings: unified CheckOwner

## Plan

### Round 1 — 2026-11-01
- Reviewer: passkey_plan_review，sdlc-reviewer Plan pass fallback；REVIEW.md 未指定 Stage3 reviewer，未安装 codex-plan-review。
- Budget: Plan only，最多三轮，后续仅审增量，不重复已有发现。
- Finding: plan.md:45 — Minor — 现有异常 Advice 将 AccessDeniedException 转为 500，缺少两个 Starter 同时启用的真实拒绝响应证明。
- Resolution: 公共资源拒绝标记、Security 的 AccessDeniedException 子类及 Web 通用拒绝异常；现有异常 Advice 映射 403/A0312；安全模块新增 Web 测试依赖，联合真实拦截器与 Advice 验证。已更新文件范围、工作步骤、风险和 Proof。
- Human approval: pending。

### Round 2 — 2026-11-01
- Scope: 仅第 1 轮后增量。
- Original finding: 已解决，403 映射、生产依赖隔离、真实组合测试已补齐。
- Finding: plan.md:69 — Minor — 后置归属检查需要查询先执行，不能要求方法计数保持零。
- Resolution: Proof 拆分前置拒绝计数零与后置拒绝查询已执行但响应不含详情两种断言。

### Round 3 — 2026-11-01
- Scope: 仅第 2 轮后的 Proof 修订。
- Result: 无新增发现；已确认前后置拒绝的证明时机正确。
- Actual total rounds: 3；所有已发现问题均已在计划中修正。
- Human plan approval: accepted，用户回复 OK。
