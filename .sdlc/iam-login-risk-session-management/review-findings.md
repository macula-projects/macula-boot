# Review findings: IAM 登录风险、在线会话与安全策略执行

## Stage: Plan

- 配置：REVIEW.md 未指定 Stage 3 reviewer；默认 codex-plan-review 未发现可用安装/工具，按 sdlc-build 回退到只读 sdlc-reviewer 子代理。
- 预算：仅 Plan pass，最多三轮；round 1 全文，后续仅修订 diff；不重复已记录问题，同类问题只报一次。不执行 Bugs/Security/Compliance，不替代人工 gate。
- 源码基线：Cloud HEAD fd96050；计划尚未提交，不伪称已提交 SHA。逐轮记录实际工作树计划 SHA-256 与 /tmp 快照。

### Round 1

- 实际轮数：1；评审者 /root/sdlc_plan_review；Passes run: Plan。
- 计划 SHA-256：5cfa3e8cb133c44044f1cd19eb62fdf13568d8dd6351db211ca1955ff114cd1d；快照 /tmp/iam-login-plan-round1.md。
- plan.md:56 — Important — 普通登录验证码未明确消费租户 verifyCodeValidTime，CSP 装配及会话策略 Proof 缺失 — 在步骤 6/7 和 R7 Proof 补全有效期、CSP、会话/remember-me/并发上限及租户边界。状态：round 2 已验证修复。
- plan.md:30 — Important — MFA 独立模板缺少现有投递器与 CodeService 文件及审计场景 — 补列 LoginMailDispatcher/LoginSmsDispatcher、EmailCodeService/SmsCodeService、消息审计和测试，以服务端 purpose 选择场景。状态：round 2 已验证修复。
- 其他：已核实 locales 与 Gateway 配置文件存在；新增 Query 仅为 System 本地 Controller 使用。

### Round 2

- 实际轮数：2；Passes run: Plan，仅审 /tmp/iam-login-plan-round2.diff 与必要上下文；修订后计划 SHA-256：09321a90ef9e2e2ce3437cc722c9648e7dc4809056fc9c490d5ef65e7873a076。
- 复核结论：两项 Important 均已修复；新增 No findings；避免重复报告 2 项；未修改源码、未执行测试、未批准人工 gate。
- 总计两轮，未超出三轮预算。人工工程批准尚未发生；不得将 review no findings 当作 plan accepted。


## Stage: PR review — Round 1 (2026-11-17)

- Reviewer: /root/review_boot_security，按 REVIEW.md 独立 Bugs / Security / Compliance passes，只读审查。
- Scope：base HEAD `ed50b15fdd915e9e81c243a963a4b2cb7914de50` 上本功能全部工作树改动及未跟踪文件，包含缓存常量、自动装配、仅API Key回调与真实Redis IT；accepted plan与callback-revision为合同。
- Bugs: No findings. Security: No findings. Compliance: No findings.
- Findings posted 0；重复抑制 0；verifier rejected 0（无候选需验证）；Nits 0。
- Recurring：扫描现有review-findings，未见此次回调实现需新增的重复缺陷类别。
- 人工授权：用户明确“OK，推送代码合并”；此审查不自批 gate，不绕过分支保护。代码工作树指纹见 verification.md。
