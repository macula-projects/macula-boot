# 手机与座机自适应脱敏验证记录

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

## Delivery re-verification
- 最终源码快照 08dde421；独立审查发现的原始输入长度问题已修复。
- Java 8 执行 Web、Crypto 及 Commons 的 `-am test`：Web 125、Crypto 18、Commons 5，共 148 项，0 failures/errors/skipped，BUILD SUCCESS；日志 `/tmp/macula-5x-delivery-test.log`。
- 再次执行全仓 mvn test，仍在 RocketMQ OrderServiceTest 因 connect to null failed 退出 1；日志 `/tmp/macula-5x-delivery-all-tests.log`。保留已知外部环境限制，不宣称 5.x 全仓绿色。
- Crypto package -DskipTests 成功，独立 Java 8 JAR 的固定向量加解密、stdin、EOF和无效格式 5 项进程检查通过；普通 Starter JAR 保留。
- 用户在获知上述外部限制后明确要求本次推送合并。main 已独立完整 clean verify 通过，详见主线 PR 证据；该结果不替代 5.x 运行证据。
