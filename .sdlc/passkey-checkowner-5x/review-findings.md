# Review findings: PassKey 与 CheckOwner 5.x 回移

## Plan

### Round 1 — 2026-11-02
- Reviewer: `sdlc-reviewer` fallback；5.x 未配置 Stage 3 reviewer，默认 `codex-plan-review` 未安装。
- Budget: Plan only，最多三轮；第 1 轮审查全量计划，后续仅审增量。
- Finding: `plan.md:71` — Minor — 本地签发/验证闭环不能证明凭证编码格式与主线兼容。
- Resolution: Proof 增加由主线固定密钥、Clock、用户和资源编号生成的 golden token 向量，要求 5.x 生成值完全一致并能验证该向量。
- Finding: `plan.md:73` — Minor — 未显式证明 ADMIN/ROOT 无隐式豁免。
- Resolution: CheckOwner Proof 增加 `ROLE_ADMIN`、`ROLE_ROOT` 的错误拥有者与错误 PassKey 拒绝用例，并断言前置方法不执行。
- Human plan approval: accepted，用户回复 `YES`。

## Deploy

### Round 1 — 2026-11-02
- Reviewed SHA: `382afc41e19b2bc0e36fb61bc2102f9d5eb7ee43`
- Scope: `origin/5.x..382afc4`，包含完整 PassKey/CheckOwner 回移与授权的 MyBatis 测试隔离修正。
- Policy: 5.x 没有版本化 `REVIEW.md`；依据本会话提供的仓库约束，使用主线 `REVIEW.md` 的 Bugs、Security、Compliance 三项，Standard 深度，最多 5 Nits。
- Reviewer: 独立只读子代理 `passkey_5x_code_review`，执行 sdlc-reviewer review mode。
- Bugs: No findings.
- Security: No findings.
- Compliance: 无额外适用要求，No findings.
- Findings posted: 0；suppressed as already reported: 0；Nits: 0。
- Verifier: 全新上下文 `passkey_5x_review_verify`，候选列表为空，verified findings 0、verifier-rejected 0。
- 验证: JDK 8 scoped `clean verify` 46 tests 全通过，Checkstyle 0；全仓 TSF/SnakeYAML 基线限制见 `verification.md`。用户明确要求保留该配置并继续 PR。
- Human PR approval: pending；本记录不表示批准合并或部署。

## Recurring

扫描当前 5.x 的 `.sdlc/*/review-findings.md`，未发现跨两个变更重复的发现类别，无待补充的规则编码。

### Round 2 — 2026-11-02
- Scope: 仅审查第 1 轮后的 Proof 增量和审查记录。
- Result: No findings；golden token 与 ADMIN/ROOT 无豁免证明已覆盖第 1 轮发现。
- Actual total rounds: 2；未达到三轮上限。
- Human plan approval: accepted，用户回复 `YES`。
