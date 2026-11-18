# Plan: 配置加解密工具与独立可执行 JAR (from spec.md 2026-11-18)
Status: accepted

## Files that change
- `macula-boot-starters/macula-boot-starter-crypto/src/main/java/dev/macula/boot/starter/crypto/utils/ConfigCryptoUtil.java` # 新增配置加解密工具。
- `macula-boot-starters/macula-boot-starter-crypto/src/main/java/dev/macula/boot/starter/crypto/cli/ConfigCryptoCli.java` # 新增独立 CLI 入口。
- `macula-boot-starters/macula-boot-starter-crypto/src/test/java/dev/macula/boot/starter/crypto/utils/ConfigCryptoUtilTest.java` # 新增工具和现有配置解密器兼容测试。
- `macula-boot-starters/macula-boot-starter-crypto/src/test/java/dev/macula/boot/starter/crypto/cli/ConfigCryptoCliTest.java` # 新增 CLI 参数及交互测试。
- `macula-boot-starters/macula-boot-starter-crypto/pom.xml` # 附加独立可执行 JAR。
- `macula-boot-starters/macula-boot-starter-crypto/README.md` # 添加中文构建和运行说明。
- `.sdlc/config-crypto-cli/plan.md` # 计划与实施记录。
- `.sdlc/config-crypto-cli/review-findings.md` # 计划审查记录。
- `.sdlc/config-crypto-cli/verification.md` # Test 阶段实际验证证据。

## Order of work
1. 新增 Java 8 兼容的 ConfigCryptoUtil，复用已有 Hutool SecureUtil.aes，密钥及文本均使用 UTF-8，输出 mpw: 加 Base64。校验 null、16/24/32 字节密钥、解密前缀及 Base64 格式；使用与 CryptoPostProcessor 一致的 StrUtil.isNotBlank 拒绝全空白密钥，合法密钥中的首尾空白保留；不 trim 内容、不生成或存储真实密钥。
2. 新增 ConfigCryptoCli：main 仅转发至可测试的执行方法，返回退出码后退出；实现 encrypt/decrypt、--key、--value、--help。拒绝未知或重复参数、缺少参数值及多余参数；省略操作则交互选择，省略 key/value 则交互补齐，显式空字符串明文仍合法。提示使用 stderr，参数模式成功 stdout 仅为结果和换行。
3. 交互有 Console 时用 readPassword 隐藏密钥及待加密明文；无 Console 时从 UTF-8 stdin 读取完整行。EOF 不继续尝试加解密。退出码：成功或帮助 0、参数错误 2、可检测的加解密失败 1；错误不包含输入或堆栈。
4. 在现有 Crypto Starter 的 package 阶段使用 Maven Shade 插件附加 classifier 为 cli 的 JAR，配置 Main-Class。只纳入 ConfigCryptoUtil、ConfigCryptoCli 及 Hutool，保持普通 Starter JAR 和原 POM 不变，不生成 dependency-reduced-pom，不混入 Spring、业务配置及测试资源；移除依赖签名元数据。
5. 增加 JUnit 5 工具、兼容性、CLI 参数和交互测试；README 提供构建、参数模式、交互模式、密钥 UTF-8 字节长度说明及现有协议限制，示例使用虚构密钥。
6. Build 阶段执行 Java 8 目标模块及依赖的 test-compile 和 git diff --check，检查改动范围并记录偏差；人工确认实现后进入 Test 阶段。

## Risks
- AES/ECB/PKCS5Padding 无认证，错误密钥和篡改不一定可检测；不承诺此项检测能力，不改变现有协议。
- 命令行参数可见；交互有 Console 时不回显，无 Console 时不承诺隐藏终端回显。解密明文是调用者要求的输出。
- Shade 过滤错误可能丢失内部类或依赖，或打入 Spring 自动配置资源；通过独立进程及 JAR 内容检查验证。
- 命令行内容允许空白、空字符串及以短横线开头的值；参数解析以选项后一个参数为值，值等于保留选项名称时可通过交互输入，文档说明此边界。
- 全仓测试曾被未就绪的 RocketMQ 服务阻塞，仍执行并记录真实结果，不擅自跳过、修改测试或扩展基础设施范围。
- 保留已有姓名脱敏和相关 SDLC 文件修改；不修改 CryptoPostProcessor 或其他功能。

## Proof
- ConfigCryptoUtilTest：16/24/32 字节密钥、中英文、空串、首尾空白、换行、已知加密向量；null、无效密钥长度、全空格及 Unicode 全空白密钥、错误前缀和非法 Base64 失败；合法密钥首尾空白不被 trim；以真实 CryptoPostProcessor 验证加密结果，并解密已有测试向量。
- ConfigCryptoCliTest：参数和交互加解密、部分参数交互补齐、help、未知/重复选项、缺值、空串、以短横线开头的内容、EOF、退出码及错误不泄漏输入。Console 隐藏输入分支通过真实终端验证；无 Console 分支用流输入测试。
- Build：使用 JDK 8 执行 `mvn -pl macula-boot-starters/macula-boot-starter-crypto -am test-compile -DskipTests`，以及 `git diff --check`。
- Test：目标模块及依赖 `mvn -pl macula-boot-starters/macula-boot-starter-crypto -am test`，随后 `mvn test`；捕获输出、退出码及外部依赖阻塞，禁止在全仓未通过时声明整体绿色。
- 打包：`mvn -pl macula-boot-starters/macula-boot-starter-crypto -am package -DskipTests`。检查普通和 cli 两个 JAR，确认 Main-Class、Hutool 依赖、工具内部类完整，排除 Spring 自动配置及业务配置。
- 使用实际 JDK 8 `java -jar` 进程验证参数加解密、已知向量、UTF-8 中文、无 Console 的 stdin 交互和错误退出码；另外使用真实终端检查 Console 输入。交付 cli JAR 的本地路径及 README。
