# 应用菜单与接口授权审查记录

## Plan

### 第 1 轮

默认 `codex-plan-review` 未安装，按 `sdlc-build` 使用 `sdlc-reviewer`。本轮仅执行 Plan pass。

- `plan.md:15` — Important — 计划遗漏新增依赖影响的现有测试构造调用 — 补齐已核对的 Cloud 与 Boot 测试，按实际构造变更更新。
- `plan.md:39` — Important — Maven `test` 无法执行计划的 Flyway 与 Redis 集成测试 — 追加定向 Failsafe `verify` 命令，列出真实 Redis 联合验证的入口、运行条件和判定。

## 交付实现复核

- Important · Security。待验证应用凭据只能进入实际的 Gateway 路由。System 的 IAM 管理接口和用户管理写操作保留可信用户边界。
- Important · Bugs。内部转发保留原入口路径用于授权。应用凭据通过验证后再清理待路由标记，定制过滤器按既有约定发布可信主体也能继续转发。
- Important · Bugs/Security。应用授权与目录写入在首次读库前取得快照锁，普通用户写入租户与 SQL 租户保持一致。重建在事务外等待短时锁竞争。
- 三轮分别执行 Bugs、Security、Compliance。重复的事务锁顺序和租户问题合并为 2 处。上述事项均有对应回归测试。
