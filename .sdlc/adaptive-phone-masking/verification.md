# 手机与座机自适应脱敏验证记录

## Main delivery verification
- 基线 origin/main b735b292，源码快照 74d07f02；Java 17、Boot 4、Jackson 3 兼容移植，父 POM 集中管理国际号码依赖。
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.0.17.jdk/Contents/Home mvn clean verify` 退出码 0，`BUILD SUCCESS`，日志 `/tmp/macula-main-port-verify.log`。
- Surefire：Tests run 502 / failures 0 / errors 0 / skipped 0；Failsafe：Tests run 58 / failures 0 / errors 0 / skipped 4（按项目既有配置跳过）。
- 首次沙箱执行被 MapStruct Plus 的 ~/.msp 增量标记写权限阻止；获准后完成真实全仓 clean verify，不修改测试或构建配置来跳过失败。
- 变更的 10 个 Java 文件精确 Checkstyle：退出码 0，`You have 0 Checkstyle violations.`；日志 `/tmp/macula-main-checkstyle.log`。
- 实际 cli JAR 包含入口、内部类和 Hutool，无 Spring 自动配置；独立 Java 17 进程的固定向量加解密、stdin 交互、EOF 及无效输入 5 项检查通过。原普通 Starter JAR 保留。
- git diff --check 通过；本次不修改 protected 配置/技能/hook，eval 不适用。
- Verification is green（仅指 main 本次完整验证）；以下为 5.x 历史记录，不作为 main 测试证据。


日期：2026-11-18。用户已明确确认实现完整后进入 Test 阶段。

## 环境
Tencent Kona JDK 8u472，Maven 3.9.6，libphonenumber 9.0.40。Maven 命令设置 `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_472.jdk/Contents/Home`。

## 失败与修复
- 首轮定向测试执行 116 项，1 项失败：`+86-10-87651234-888` 预期 `+86-10-****1234-888`，实际 `+86-10-*******4-888`。
- 原因：国内连字符分机识别未接受国际码后的连字符，分机数字被计入主号。
- 修复：国际码和国内区号之间允许格式连字符，仍由主号码格式校验和区号元数据核验；补充 0086 对应回归断言。不更改已接受规则或范围。
- 修复后重新执行全部定向、目标模块及全仓测试，结果如下。

## 实际命令和输出
1. `mvn -pl macula-boot-starters/macula-boot-starter-web -am test -Dtest=AdaptivePhoneMaskingTest,SensitiveUtilTest -Dsurefire.failIfNoSpecifiedTests=false`
   - 退出码 0；日志 `/tmp/adaptive-phone-test.log`。
   - 电话：`Tests run: 79, Failures: 0, Errors: 0, Skipped: 0`。
   - 姓名：`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`。
   - 合计：`Tests run: 116, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
2. `mvn -pl macula-boot-starters/macula-boot-starter-web -am test`
   - 退出码 0；日志 `/tmp/adaptive-phone-web-tests.log`。
   - Web：`Tests run: 124, Failures: 0, Errors: 0, Skipped: 0`。
   - Commons：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
   - 合计 129 项通过，0 跳过；`BUILD SUCCESS`。
3. `mvn test`
   - 退出码 1；日志 `/tmp/adaptive-phone-all-tests.log`。
   - RocketMQ `OrderServiceTest` 加载上下文失败，根因为 `RemotingConnectException: connect to null failed`。
   - 输出：`Tests run: 1, Failures: 0, Errors: 1, Skipped: 0`；`BUILD FAILURE`。
   - 未更改或跳过该测试，未扩展外部服务配置；全仓在该模块中断，后续模块未全部执行。
4. `git diff --check`
   - 退出码 0，无输出。

## Proof 与需求对应
- 手机、座机、自适应三个入口、可识别类型的委派一致性通过。
- +86/0086、+44 手机及座机、+852、+1 类型歧义、意大利有效前导零用例通过；国家码及原空格、括号、连字符保留。
- 010/020/0755/0999 区号、无分隔的完整国内号码、国内显式区号与短主号、+86/0086 短号通过。
- 主号 1～8 位边界、11 位手机、奇数长度中间两位、无区号主号及未知类型原数字计数通过。
- ext/ext./x/#/转/分机、国内连字符分机及国际码后的连字符格式通过；分机不参与长度计算，国际普通分组不被误识别为分机。
- 未知国际码、不规则文本、无效区号、空白、null/空串的保守回退通过。
- 真实 ObjectMapper 验证 MOBILE、FIXED_PHONE、PHONE 注解映射通过；已有姓名脱敏 37 项回归通过。
- Build 阶段真实 JDK 8 测试源码编译及依赖树检查通过，固定依赖版本和 Java 8 class 格式已确认；本阶段使用同一 JDK 完成真实测试。

## 配置与 eval
本次未修改 CLAUDE.md、技能、hook、REVIEW.md 或审查器定义；配置 eval 不适用。

## 结论
电话脱敏及姓名回归、Web/Commons 全部测试通过。全仓验证仍受 RocketMQ 外部连接失败阻塞，不声明整体绿色，不进入 PR 审查或部署交接。


## 2026-11-19 自定义 Handler 与 Nacos 解密主线 PR 验证

- 范围：从 5.x 已验收实现前移植到 main，源码快照 `3cd560feb1f34cb00b3d5b01cfdba994e9668646`，基线 `cc05c252`。使用 Tencent Kona JDK 17.0.17 / Maven 3.9.6 / main Boot 4.0.8、Cloud 2025.1.3、Jackson 3。
- 目标命令：`mvn -o -pl macula-boot-starters/macula-boot-starter-web,macula-boot-starters/macula-boot-starter-crypto -am verify`，退出码 0，216 项单元测试 + 5 项集成测试，0 failures/errors/skipped；日志 `/tmp/handler-nacos-main-target-verify.log`。其中 SensitiveSerializerTest 11 项；CryptoPostProcessorTest 11 项、CryptoPostProcessorIT 3 项，包含真实 bootstrap/日志占位符及 Cloud 事件/实际重绑定。
- 全仓首次运行被注解处理器写入 `~/.msp/incrementMark` 的沙箱限制阻止；正常权限重跑 `mvn -o clean verify`，退出码 0、BUILD SUCCESS，Surefire 549 项（0 失败/错误/跳过），Failsafe 61 项（0 失败/错误、4 项既有跳过）；日志 `/tmp/handler-nacos-main-full-verify.log`。
- 既有跳过：KafkaProducerIT.testSend、OrderServiceIT.testSendTxMsg、TinyIdClientIT.testNextId、RmOpaqueTokenCallbackIT.platformCredentialAuthorizesExistingCacheRemovalAcrossConnections。本次未新增或修改跳过条件。
- 按 CI 范围对变更的 7 个 Java 文件运行 `mvn -o -N checkstyle:check -Dcheckstyle.includes=<变更文件清单>`，退出码 0，0 violations；日志 `/tmp/handler-nacos-main-checkstyle.log`。拆分测试时遗留的无用导入已删除。
- 普通 Crypto Starter JAR 含 LifecycleListener 及注册；CLI JAR 含 0 个 Spring 类且不含监听器。`git diff --check` 通过。
- 未更改 main 框架/发布版本、父 POM 或用户的本地 Gateway 修改；未引入示例凭据和环境配置。真实 Nacos 服务端到端测试未运行；离线源和真实 Cloud 事件测试不替代该证据。此结果只证明本次 main 快照，不代替 5.x 验证。
