# 配置加解密 CLI 验证记录

日期：2026-11-18。用户确认实现完整，进入 Test 阶段。

## 环境与命令
Tencent Kona JDK 8u472，Maven 3.9.6。Maven 使用 `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_472.jdk/Contents/Home`。

1. `mvn -pl macula-boot-starters/macula-boot-starter-crypto -am test`
   - 退出码 0，日志 `/tmp/config-crypto-cli-test.log`。
   - `ConfigCryptoUtilTest`：`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
   - `ConfigCryptoCliTest`：`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
   - 连同既有集成测试合计：`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
2. `mvn test`
   - 退出码 1，日志 `/tmp/config-crypto-cli-all-tests.log`。
   - RocketMQ `OrderServiceTest` 加载上下文时失败：`RemotingConnectException: connect to null failed`。
   - 输出：`Tests run: 1, Failures: 0, Errors: 1, Skipped: 0`；`BUILD FAILURE`。
   - 外部 RocketMQ 连接不可用，后续模块未全部执行；未跳过或修改该测试，不声明全仓验证绿色。
3. 使用 JDK 8 独立 `java -jar` 进程执行 15 项检查，全部通过。
   - 参数加解密与已有 hello1 固定向量一致；中文、空串、空白及换行还原一致。
   - stdin 交互加解密、帮助、无效选项、EOF、无效密文、确定可检测的错误密钥退出码符合设计。
   - 实际输出：`Independent Java 8 JAR checks passed: 15`。
   - 用例结果保存在 `/tmp/config-crypto-cli-smoke.json`；仅使用虚构测试数据。
4. 真实 PTY 下运行 `java -jar <cli.jar> encrypt`：输入测试密钥和 hello1；输入均未回显，退出码 0，输出 `mpw:Or0wNwIcctnekUyDfeN8cw==`。
5. 真实 PTY 下运行 `java -jar <cli.jar> decrypt --value mpw:Or0wNwIcctnekUyDfeN8cw==`：测试密钥未回显，退出码 0，输出 `hello1`。
6. `git diff --check`：退出码 0，无输出。

## Proof 对应
- 中英文、合法密钥长度、保留空白、空串和已知向量：工具单测及独立进程通过。
- 拒绝 null、无效密钥、全空白密钥、错误前缀、非法 Base64：单测通过。
- 工具生成的配置值由真实 CryptoPostProcessor 还原：单测通过。
- 参数与交互、帮助、EOF、无效和重复选项、空值、短横线值、错误无输入泄漏：CLI 单测通过。
- Console 隐藏输入和无 Console stdin 分支均已运行验证。
- Java 8 编译和 package 已在 Build 完成，退出码 0；原 Starter JAR 保留；CLI 入口、内部类及 Hutool 依赖存在，无 Spring 自动配置和业务 YAML；产物约 2.5 MB。
- 产物：`macula-boot-starters/macula-boot-starter-crypto/target/macula-boot-starter-crypto-5.0.22-SNAPSHOT-cli.jar`。

## 配置与 eval
本次未修改 CLAUDE.md、技能、hook、REVIEW.md 或审查器定义；配置 eval 不适用。

## 结论
Crypto 模块全部测试和独立 JAR 运行验证通过。全仓验证仍被 RocketMQ 外部连接失败阻塞，不进入 PR 审查或部署交接。
