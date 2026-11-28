# Plan: 统一 IAM Redis Key 命名（源自 spec.md，2026-11-28）
Status: accepted

## 已确认的网关 nonce 补充范围

用户回复 `ok` 确认直接将网关签名 nonce 切换为 `macula:cloud:gateway:nonce:{nonce}`，旧 Redis 键按原 TTL 到期。交付审查发现了存量的非原子登记问题，修正为验签成功后用 Redis 原子登记。TTL 至少为 300 秒，签名时间领先时覆盖剩余的有效窗口。验证完整 Key、并发重复请求和失败验签不会占用 nonce。WebSocket 不在此次补充范围内。


## Files that change

- **Boot**。`macula-boot-commons/src/main/java/dev/macula/boot/constants/CacheConstants.java` 和 `macula-boot-commons/README.md`。增加统一 IAM Redis 前缀，调整 OAuth2 Client 的旧常量值，复用前缀表达其他已有 IAM 常量的相同值。
- **Cloud IAM 主代码**。以下路径相对于 `macula-cloud-iam/src/main/java/dev/macula/cloud/iam/`。
  - `authentication/thirdparty/ExternalBindingTicketStore.java`、`authentication/thirdparty/binding/AccountBrowserBindingService.java`、`authentication/thirdparty/cache/ProviderApplicationTokenCache.java`、`authentication/thirdparty/directory/ProviderDirectoryRefreshService.java`、`authentication/thirdparty/directory/ProviderMemberSnapshotStore.java`。
  - `protocol/oauth2/grant/externalcode/ExternalCodeRateLimiter.java`、`protocol/oauth2/grant/externalcode/ExternalCodeReplayStore.java`。
  - `security/IamSecurityVersion.java`、`security/password/PasswordExpiryReminderLink.java`、`security/protection/LoginProtectionKeys.java`。
  - `service/account/impl/DefaultAccountVerificationService.java`、`service/account/impl/DefaultCommonVerificationService.java`、`service/human/impl/DefaultHumanVerificationService.java`、`service/human/impl/DefaultPasswordHumanVerificationGuard.java`、`service/human/impl/MessageSecurityGuard.java`。
  - `service/login/impl/DefaultIamSecurityCoordinator.java`、`service/risk/LoginRiskAssessmentService.java`、`service/mfa/MfaVerificationLimits.java`、`service/mfa/impl/DefaultMfaChallengeService.java`、`service/mfa/impl/DefaultStepUpService.java`、`service/mfa/impl/MfaAttemptBudget.java`。
  - `service/password/impl/PasswordStrengthDisplay.java`、`service/session/impl/DefaultDeviceSessionService.java`、`service/verification/redis/RedisVerificationCodeStore.java`。
- **Cloud System 主代码**。以下路径相对于 `macula-cloud-system/src/main/java/dev/macula/cloud/system/`。更新 `security/IamSecurityVersion.java`、`security/password/PasswordExpiryReminderLink.java`、`security/protection/LoginProtectionKeys.java`。
- **Cloud IAM 测试**。以下路径相对于 `macula-cloud-iam/src/test/java/dev/macula/cloud/iam/`。
  - `authentication/mfa/MfaSendAdmissionIT.java`、`authentication/mfa/StepUpServiceTest.java`、`authentication/thirdparty/binding/AccountBrowserBindingServiceTest.java`、`authentication/thirdparty/cache/ProviderApplicationTokenCacheIT.java`、`authentication/thirdparty/directory/ProviderDirectoryRefreshServiceTest.java`。
  - `controller/HostedEmailLoginFlowTest.java`、`controller/HostedSmsLoginFlowTest.java`、`controller/PasswordExpiryReminderLinkControllerTest.java`、`protocol/oauth2/grant/externalcode/ExternalCodeRateLimiterTest.java`、`protocol/oauth2/grant/externalcode/ExternalCodeReplayStoreTest.java`。
  - `security/AccountSecurityPersistenceIT.java`、`service/mfa/MfaChallengeServiceTest.java`、`service/mfa/impl/MfaAttemptBudgetIT.java`、`service/verification/redis/RedisVerificationCodeStoreTest.java`。
- **Cloud System 测试与说明**。`macula-cloud-system/src/test/java/dev/macula/cloud/system/service/iam/impl/SecurityPolicyServiceImplTest.java`、`macula-cloud-system/src/test/java/dev/macula/cloud/system/service/password/PasswordExpiryReminderLinksTest.java`，以及 `macula-cloud-iam/README.md`、`macula-cloud-system/README.md`。

## Order of work

1. 在 Boot Commons 定义 `IAM_REDIS_PREFIX = "macula:cloud:iam:"`。用新前缀表达 IAM Redis 常量，OAuth2 授权记录、Consent、资源回调和小程序 Session Key 保持原字符串值。构建本地 SNAPSHOT，确保 Cloud 可基于最新常量重新编译。
2. 在 IAM 与 System 只修改确定属于 Redis 的键。双端共享状态同时改名。第三方目录任务及其成员快照明确区分物理 Redis 键与稳定的密码学输入。复用同一物理键根和 ready 键，保留历史成员快照定位字符串作为任务摘要输入，原加密上下文继续用于加解密。保留 Lua 入参顺序和 Cluster 哈希标签。浏览器绑定分别使用 Session 属性和 Redis 键的前缀，只改 Redis 部分。安全协调器与风险评估引用同一安全缓存前缀。
3. 更新测试数据和文档，在隔离环境验证新命名。检查 OAuth2 令牌发布表与清理表的 `record_locator`、摘要和旧围栏拒绝行为。已认可的直接切换仅由新版本代码实现，运行验证使用隔离实例。
4. 核对最终差异和相关构建产物，记录计划偏差。Cloud 是相邻受限工作区，应用源码补丁时使用对应的写入授权。

## Risks

- 两个 JVM 服务编译时会内联公共常量。切换使用由同一 Boot SNAPSHOT 重新编译的 IAM 与 System，验证双方实际键值。
- 测试环境中尚未结束的短期流程会因旧键停止读取而失效。现有 TTL 规则继续执行，永久旧键可留在未上线环境中，日后清理独立授权。
- 缺失围栏仍拒绝，既有 OAuth2 授权记录位置沿用现有命名。对需要依赖旧围栏的测试令牌重新发起认证。运行验证期间使用隔离的 MySQL 与 Redis，保留本地应用授权任务的未提交改动。

## Proof

- Boot Commons 的 Maven 单测及常量值检查证明命名和既有规范键值。扫描 Cloud 源码及配置中的旧前缀，逐项确认保留项是加密上下文、Session 属性或其他协议标识。
- IAM、System 和依赖模块用 `mvn -pl macula-cloud-iam,macula-cloud-system -am test -Plocal`。定向测试覆盖共享就绪状态、浏览器绑定的 Session 属性与 Redis 键、MFA、短信和邮件验证码、限频、防重放及第三方目录。IAM 链接读取测试和 System 签发测试均断言固定的规范密码到期链接 Key。共享就绪状态也使用独立的预期值，确保测试能识别任一端使用旧前缀。
- 用 `mvn -pl macula-cloud-iam,macula-cloud-system -am verify -Plocal -Dit.test=AccountSecurityPersistenceIT,ProviderApplicationTokenCacheIT,MfaSendAdmissionIT,MfaAttemptBudgetIT -Dfailsafe.failIfNoSpecifiedTests=false` 在可用的隔离 MySQL 与 Redis 环境验证令牌发布、撤销、旧围栏缺失拒绝、Redis TTL 和限制状态。对第三方目录及令牌记录的已标记多键操作检查 Redis Cluster 同槽。在现有目录测试中覆盖成员快照和任务后缀稳定、加密上下文不变及旧上下文密文可解密。
- 运行受影响模块已有的 IP 定位和人机验证密文回归，验证既有协议值和密文上下文；再检查测试日志及两仓差异。

## 实施记录

- Boot Commons 提供 `IAM_REDIS_PREFIX`。IAM 和 System 均以该常量构造实际 Redis 键，既有 OAuth2 授权数据与索引键值保持相同。
- 登录风控协调器原先用同一个前缀构造 Redis 键和请求属性。Redis 部分复用风险评估缓存前缀，请求属性继续使用原有标识。浏览器绑定按相同原则分离 Redis 键和 Session 属性。
- 第三方目录成员快照、任务及队列使用统一的物理 Redis 键根。摘要输入和加密绑定上下文保留既有字符串，目录测试断言成员和任务后缀、实际密文解密及所有权保护。
- Cloud 校验在受限工作区的临时副本执行，已将通过差异检查的源代码写回相邻仓库。保护此前应用授权任务的改动。修订现有测试并保留旧围栏作为拒绝用例的输入。
- 实施期验证。Boot Commons 共 34 项单测通过。Cloud IAM 与 System 合计 1228 项单测，0 失败、0 错误、1 项既有条件跳过。四组定向集成测试共 32 项通过。令牌发布与撤销用例使用从 B56.13 之前提交提取的历史合成迁移夹具复验，18 项实际执行通过。目录测试的精确后缀与密文断言在最终副本重跑 3 项通过。相关日志为 `/tmp/macula-iam-redis-boot-build.log`、`/tmp/macula-iam-redis-cloud-tests.log` 和 `/tmp/macula-iam-redis-fence-with-fixture.log`。
