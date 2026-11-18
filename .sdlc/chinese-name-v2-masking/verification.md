# 中文名 V2 脱敏验证

## 两字末字规则：双分支交付验证（2026-11-18）

- 用户已明确要求提交、推送、合并到 5.x 和 main；本次仅交付姓名 V2 两字规则修订及其测试和文档，不重做历史功能。
- 5.x / Java 8：`mvn -pl macula-boot-starters/macula-boot-starter-web -am test` 退出 0，Web 131、Commons 5，共 136 项，0 failures/errors/skipped。姓名测试 43 项；日志 `/tmp/chinese-name-v2-last-character-final-tests.log`。
- main / Java 17：相同 Maven 命令退出 0，Web 143、Commons 34，共 177 项，0 failures/errors/skipped。姓名测试 45 项（包含主线原有通用回归）；日志 `/tmp/name-tail-main-test.log`。
- main：针对本次两个 Java 文件执行 `mvn -N checkstyle:check -Dcheckstyle.includes=macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/utils/SensitiveUtil.java,macula-boot-starters/macula-boot-starter-web/src/test/java/dev/macula/boot/starter/web/utils/SensitiveUtilTest.java`，退出 0，0 violations；日志 `/tmp/name-tail-main-style.log`。
- 两个分支 `git diff --check` 均通过；本次未在本地重跑全仓测试。main PR 按现有 CI 执行 Maven Verify，不改动 CI、外部服务或发布配置。


## Main delivery verification
- 基线 origin/main b735b292，源码快照 74d07f02；Java 17、Boot 4、Jackson 3 兼容移植，父 POM 集中管理国际号码依赖。
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.0.17.jdk/Contents/Home mvn clean verify` 退出码 0，`BUILD SUCCESS`，日志 `/tmp/macula-main-port-verify.log`。
- Surefire：Tests run 502 / failures 0 / errors 0 / skipped 0；Failsafe：Tests run 58 / failures 0 / errors 0 / skipped 4（按项目既有配置跳过）。
- 首次沙箱执行被 MapStruct Plus 的 ~/.msp 增量标记写权限阻止；获准后完成真实全仓 clean verify，不修改测试或构建配置来跳过失败。
- 变更的 10 个 Java 文件精确 Checkstyle：退出码 0，`You have 0 Checkstyle violations.`；日志 `/tmp/macula-main-checkstyle.log`。
- 实际 cli JAR 包含入口、内部类和 Hutool，无 Spring 自动配置；独立 Java 17 进程的固定向量加解密、stdin 交互、EOF 及无效输入 5 项检查通过。原普通 Starter JAR 保留。
- git diff --check 通过；本次不修改 protected 配置/技能/hook，eval 不适用。
- Verification is green（仅指 main 本次完整验证）；以下为 5.x 历史记录，不作为 main 测试证据。


## 当前修订：连续隐藏单星与斜杠分段

用户已明确确认修订后的实现完整，进入 Test。以下命令使用 Tencent Kona JDK 8u472（JAVA_HOME 同下文环境）。

1. `mvn -pl macula-boot-starters/macula-boot-starter-web -am test -Dtest=SensitiveUtilTest -Dsurefire.failIfNoSpecifiedTests=false`
   - 退出码 0；`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
   - 日志 `/tmp/chinese-name-v2-revision-test.log`。
2. `mvn -pl macula-boot-starters/macula-boot-starter-web -am test`
   - 退出码 0；Web：`Tests run: 45, Failures: 0, Errors: 0, Skipped: 0`；Commons：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
   - 共 50 个测试通过，0 跳过；日志 `/tmp/chinese-name-v2-revision-web-tests.log`。
3. `mvn test`
   - 退出码 1；RocketMQ `OrderServiceTest` 因 `RemotingConnectException: connect to null failed` 加载上下文失败，`BUILD FAILURE`。
   - 日志 `/tmp/chinese-name-v2-revision-all-tests.log`。未跳过或修改该测试，后续模块未全部执行。
4. `git diff --check`：退出码 0，无输出。

需求对应：更新后的断言验证长姓名连续隐藏区仅单星、五种间隔点两侧分别压缩、所有斜杠及空段保留、各姓名段独立计数；并覆盖单字段、多段带点姓名、Unicode 扩展汉字、空值和 V1 兼容。全部定向用例通过。

本次未修改技能、hook、CLAUDE.md、REVIEW.md 或审查器定义，配置 eval 不适用。新版脱敏及 Web/Commons 验证通过，全仓仍受 RocketMQ 外部连接失败阻塞，不能声明全仓验证绿色，不进入 PR 审查或部署交接。

## 历史：旧版逐字星号规则

以下结果仅对应旧版，不用于证明当前修订通过。

日期：2026-11-18。用户已明确确认实现完整并进入 Test 阶段。

## 环境
Tencent Kona JDK 8u472，Maven 3.9.6。下列 Maven 命令均设置 `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_472.jdk/Contents/Home`。

## 实际执行与证据
1. `mvn -pl macula-boot-starters/macula-boot-starter-web -am test -Dtest=SensitiveUtilTest -Dsurefire.failIfNoSpecifiedTests=false`
   - 退出码 0，日志 `/tmp/chinese-name-v2-test.log`。
   - 输出：`Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
2. `mvn test`
   - 退出码 1，日志 `/tmp/chinese-name-v2-all-tests.log`。
   - RocketMQ `OrderServiceTest.testSendTxMsg` 加载上下文失败：`RemotingConnectException: connect to null failed`。
   - 输出：`Tests run: 1, Failures: 0, Errors: 1, Skipped: 0`；`BUILD FAILURE`。
   - 测试配置使用 `localhost:9876` NameServer；当前未获得可用 RocketMQ 连接。全仓测试在此停止，后续模块未全部执行。不跳过或修改该测试，不将全仓结果标记为通过。
3. `mvn -pl macula-boot-starters/macula-boot-starter-web -am test`
   - 退出码 0，日志 `/tmp/chinese-name-v2-web-tests.log`。
   - Commons 输出：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
   - Web 输出：`Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
4. `git diff --check`
   - 退出码 0，无输出。

## Proof 与需求对应
- 两字、三字、四字姓名：定向用例通过。
- 五种间隔点、整体首尾、多段、重复点和首尾点：定向用例通过。
- null、空串、单字、仅含点、Unicode 扩展汉字：定向用例通过。
- V1 行为：兼容性用例通过；实现差异限定 V2 及其私有辅助方法。
- Build 阶段已完成 Java 8 测试源码编译；本阶段完成真实定向及 Web/Commons 模块测试。
- 已有 Sensitive 注解和序列化映射修改保留；本次未更改这些文件。

## 配置与 eval
本次未修改 CLAUDE.md、技能、hook、REVIEW.md 或审查器定义；配置 eval 不适用。

## 结论
本次脱敏测试和 Web/Commons 模块测试通过。全仓验证被 RocketMQ 外部连接失败阻塞，尚不能声明整体验证通过，不进入 PR 审查或部署交接。
