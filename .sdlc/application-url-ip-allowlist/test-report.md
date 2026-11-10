# Test report: application-url-ip-allowlist
Status: green

## Scope
来源为已接受的 [spec.md](spec.md) 和 [plan.md](plan.md)。本报告核对多行 URL、允许 IP、两级独立继承、字段省略/清空、缓存生命周期及兼容升级。用户已确认实现并要求推送 PR、合并。

## Actual verification
- 当前源码提交：`53037ed7f810ba7bc7facd6f418f9dbdae9763d3`。
- `mvn test`，exit 0，全仓 334 项，0 failure/error/skip。

```text
Tests run (module totals): 334, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  02:09 min
```

- 新增 AccessRuleUtilsTest 6 项覆盖 URL 旧逗号/换行、调用方 Ant 匹配、空配置、无效输入、IPv4/IPv6/CIDR、mapped 地址和地址族边界。
- TenantUtils、HMAC、APIKEY、token 清理端点及 Security/Gateway 链测试覆盖允许/拒绝、缺失来源、错误缓存、伪造转发头，以及拒绝后不发布身份、不调用下游/清理缓存。

## Required checks and boundaries
- 本 PR 全部变更 Java 执行 `mvn -N checkstyle:check -Dcheckstyle.includes=<精确文件列表>`：0 violations；`git diff --check` 通过。
- 未修改 CLAUDE.md、AGENTS.md、REVIEW.md、skills、hooks 或 reviewer 定义，无适用的配置 eval。
- 保持 HMAC 签名与普通 OAuth 行为。来源仍按直连对端 IP，真实代理拓扑及真实第三方调用方端到端验收未包含在本地单元/Mock 链测试中。
- Verification is green。

## Review correction
Code Round 1 发现并独立确认 IPv6 依赖校验边界问题，已修正并新增压缩/词数/尾段测试；修复后已重新运行全仓 mvn test：334 项 0 failure/error/skip；精确变更 Java Checkstyle 0，Verification is green。
