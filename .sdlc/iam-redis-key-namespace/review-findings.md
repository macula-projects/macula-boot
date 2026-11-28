# IAM Redis Key 命名实施计划审查

## Plan

### 第 1 轮

默认 `codex-plan-review` 未安装，按 `sdlc-build` 使用 `sdlc-reviewer` 执行 Plan pass。

- `plan.md:23` — Important — 第三方目录会把部分原 Redis 键用作加密绑定上下文或任务摘要输入，直接改字符串会改变后缀或破坏密文兼容 — 将 Redis 存储键和既有密码学输入分开，并在目录用例验证稳定性。已纳入实施顺序和验证。
- `plan.md:18` — Important — 原计划只列出 IAM 的密码到期链接读取测试，System 签发测试通过自身 helper 计算预期值，可能漏掉两端前缀不一致 — 加入 System 签发测试，两端分别断言真实规范 Key。已纳入计划。

## 交付实现复核

- Important · Security · 存量。网关 nonce 原先先查询再写入。现改为验签成功后原子登记，并发和有效期回归已加到网关签名测试。
