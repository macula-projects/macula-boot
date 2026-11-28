# IAM 与网关 Redis 键验证

2026-11-28 在 Boot 执行 `mvn -o verify`，并在 Cloud 执行 `mvn -o verify -Plocal`。两仓均输出 `BUILD SUCCESS`。Boot 共计 621 项，4 项按条件跳过。Cloud 共计 1433 项，89 项按条件跳过。失败和错误均为零。

- 使用仓库指定的已编译历史夹具、独立 MySQL 和 Redis 运行 `AccountSecurityPersistenceIT`、`ProviderApplicationTokenCacheIT`、`MfaSendAdmissionIT`、`MfaAttemptBudgetIT`，分别通过 18、4、5、5 项。覆盖旧围栏无法放行、已登记授权记录仍使用兼容定位、发布撤销和第三方缓存有效期。
- IAM 和 System 的密码过期链接断言同一字面 Redis 键。第三方目录测试在新命名下验证旧密文仍可解密、成员与任务摘要不变。Lua 多键操作保留原哈希标签。
- 网关 `SignCheckGlobalFilterTest` 和 `CryptoDemoIT` 分别通过 2 和 7 项。合法签名使用新 Key 执行原子登记，并发重复请求只有一次进入下游，验签失败不写入 Redis。
- 未修改 `CLAUDE.md`、技能、hooks、`REVIEW.md` 或审查者配置，无适用配置 eval。
