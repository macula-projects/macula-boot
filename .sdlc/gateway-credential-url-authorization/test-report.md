# Gateway URL 授权验证

- 仓库全量 `mvn -o test` 输出 `BUILD SUCCESS`。汇总 557 项，0 失败、0 错误、0 跳过，Gateway 110 项。
- 按 GitHub 工作流范围运行变更 Java 的 Checkstyle，输出 `You have 0 Checkstyle violations.`。
- Gateway 单测覆盖普通 Bearer 的角色、ROOT 和反向角色，API Key 与 HMAC 未匹配和匹配规则、空角色条目、未初始化权限快照和 Redis 异常。
- 安全链回归覆盖真实的 Spring Security 与 Gateway 过滤器，保留凭据冲突拒绝及有效凭据验证。签名单测验证固定加密错误和下游未执行。
- 未使用开发机持久 Redis 执行完整集成测试。PR 的 Maven Verify 使用 GitHub 工作流提供的独立 Redis。
