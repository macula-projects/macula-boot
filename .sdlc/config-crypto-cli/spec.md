# Spec: 配置加解密工具与独立可执行 JAR (from intent.md 2026-11-18)
Status: accepted

## Source intent
[已验收需求](intent.md)，验收提交 `61dc5b0d`。

## Requirements
1. 提供可复用的配置加解密工具类：加密输入明文和密钥，输出 `mpw:` 加 Base64 密文；解密输入完整 `mpw:` 密文和密钥，返回原始明文。
2. 与现有 `CryptoPostProcessor` 完全兼容，采用 UTF-8、AES/ECB/PKCS5Padding；密钥为 16、24 或 32 个 UTF-8 字节。
3. 支持中文、空字符串明文及前后空白，内容不得自动 trim；拒绝 null 内容、无效密钥及缺少 `mpw:` 前缀的解密输入。
4. 提供 Java 8 可运行的独立 JAR，包含运行所需依赖，无需业务应用、Spring 容器、Nacos 或网络。
5. CLI 支持 `encrypt`、`decrypt`、`--key`、`--value`、`--help`。指定操作后省略密钥或内容时进入交互输入；无参数时交互选择操作、输入密钥及内容。
6. 参数模式成功时 stdout 仅输出结果和行结束符；提示及错误写 stderr。成功或帮助退出码为 0，参数错误为 2，检测到的加解密失败为 1；错误输出不包含密钥、输入内容或异常堆栈。
7. 提供打包和运行中文说明，并实际产出 JAR 供使用。

## Non-goals
不修改现有 CryptoPostProcessor、配置来源或动态刷新逻辑，不处理批量文件，不引入新加密算法、真实凭据或远程部署，不改变已有 Starter 的普通 JAR 用途。

## Design
- 在 Crypto Starter 的 utils 包新增 `ConfigCryptoUtil`，提供 `encrypt(String plaintext, String key)`、`decrypt(String ciphertext, String key)`；复用当前 Hutool AES，以现有运行时解密行为作为兼容基准。
- 在 cli 包新增 `ConfigCryptoCli`，只负责参数、交互、输出和退出码，不启动 Spring。交互使用完整行输入；有 Console 时密钥和待加密明文使用不回显输入，无 Console 时支持 stdin，EOF 返回明确错误。
- 在 Crypto Starter 打包时附加 `cli` classifier 的可执行 JAR，配置 Main-Class 并包含工具、CLI 和 Hutool 运行依赖；不替换原 Starter JAR，不打包业务配置或测试资源。
- README 展示加解密参数模式、交互模式及构建命令；示例使用虚构密钥。

## Data and interfaces
- 加密：`java -jar <cli.jar> encrypt --key <key> --value <plaintext>`。
- 解密：`java -jar <cli.jar> decrypt --key <key> --value <mpw:ciphertext>`。
- 交互：`java -jar <cli.jar>`。
- 无数据库、配置协议或既有 API 变更；解密结果按调用者要求输出到 stdout。

## Flagged concerns
- 既有协议限制：ECB 无认证，不能保证识别所有错误密钥或密文篡改；本次保持兼容，不承诺完整性校验，non-blocking。
- 输入可见性：命令行参数可能出现在历史或进程信息中；提供交互输入，有 Console 时隐藏密钥及待加密明文；无 Console 的 stdin 不保证隐藏终端回显，non-blocking。
- 配置解密范围：生成密文不扩大现有 CryptoPostProcessor 支持的配置来源，non-blocking。
- 项目策略：未发现仓库内 AGENTS.md、CLAUDE.md、REVIEW.md 或额外组织策略；遵循当前 SDLC 门禁及保留已有修改的要求，non-blocking。

## Verification strategy
- Java 8 下验证工具类的中英文、空串、空白及合法密钥长度；覆盖无效密钥、null、错误前缀及无效 Base64 等可检测异常。
- 使用已知测试向量以及真实 CryptoPostProcessor 验证工具生成的密文可还原，避免只验证自加自解。
- CLI 测试参数及交互模式、帮助、EOF、未知或重复参数、缺失值、成功输出和错误退出码，确认错误不回显输入。
- 实际打包后通过独立 `java -jar` 进程完成加解密及 stdin 交互验证；检查普通 Starter JAR 保留，CLI JAR 包含依赖且不含应用配置。
- 执行 Crypto 模块及其依赖测试、仓库要求的检查和 `git diff --check`，记录实际输出及环境限制。
