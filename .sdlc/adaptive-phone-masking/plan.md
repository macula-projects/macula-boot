# Plan: 手机与座机自适应脱敏 (from spec.md 2026-11-18)
Status: accepted

## Files that change
- `macula-boot-parent/pom.xml` # main 按 AGENTS.md 集中管理 libphonenumber 版本。
- `macula-boot-starters/macula-boot-starter-web/pom.xml` # 添加固定版本的离线国际号码解析依赖。
- `macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/utils/SensitiveUtil.java` # 更新三个电话入口，保留其他脱敏规则。
- `macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/utils/PhoneMaskingSupport.java` # 新增包内解析与数字位置掩码辅助实现。
- `macula-boot-starters/macula-boot-starter-web/src/test/java/dev/macula/boot/starter/web/utils/AdaptivePhoneMaskingTest.java` # 参数化规则、委派一致性及真实 Jackson 序列化验证。
- `macula-boot-starters/macula-boot-starter-web/README.md` # 中文规则、默认地域、国际格式及回退示例。
- `.sdlc/adaptive-phone-masking/plan.md` # 计划与实施记录。
- `.sdlc/adaptive-phone-masking/review-findings.md` # Plan 审查记录。
- `.sdlc/adaptive-phone-masking/verification.md` # Test 阶段实际验证证据。

## Order of work
1. 在 Web POM 固定 `com.googlecode.libphonenumber:libphonenumber:9.0.40`。官方该版本 POM 指定 source/target 8；实施使用真实 JDK 8 编译验证，不使用 SNAPSHOT 或手写全球国家码表。
2. 新增包内 PhoneMaskingSupport，解析明确分机标记及国内 `区号-7或8位主号-分机`；检查输入字符和分组完整性，禁止将任意不规则文本直接交给宽松解析器后原文返回。保存主号数字在原输入中的位置，分机及其原始分隔符独立保存。
3. 利用 libphonenumber 的国家码集合识别 +/00 国际前缀，输出保留原前缀与格式；无国际码默认 CN。只在解析视图移除格式字符，原字符串作为掩码模板。对国际码、国内拨号前导零、国家有效号码及地理区号建立可靠的位置对应，不用简单 substring 搜索猜测重复数字的边界；无法对应时回退 `*`。
4. 可明确识别的座机保留元数据给出的地理区号和原拨号前导零。国内显式区号加短主号时，不调用完整短号码的类型来决定区号：先提取格式明确的 CN 候选区号（含国内前导 0），使用候选区号加独立虚构 7/8 位测试主号（2345678/23456789）构造仅用于核验的号码；必须通过公开 API 的 CN 区域有效性检查、固定电话类型检查，并确认返回地理区号恰等于该候选区号去除拨号前导 0 的部分，才保留区号。核验值不代替真实输入、不影响主号长度和掩码，不使用反射或非公开 metadata API。此核验同时适用于明确 +86/0086 后带或不带拨号前导 0 的国内区号。候选不能确认时作为主号数字参与隐藏，不盲目保留。
5. 统一私有掩码逻辑：主号 1～3 位全部隐藏；4～7 位从 `(n-2)/2` 开始隐藏两位；8 位及以上手机保留前三后四、座机只保留后四。每个被隐藏数字替换为现有 ASTERISK_CHAR，原格式字符及明确分机原样保留。空串/null 沿用 null，其他无法安全解析输入返回 `*`。
6. handlerMobile、handlerFixedPhone 调用对应辅助逻辑；handlerPhone 仅检测 MOBILE 后调用 handlerMobile，其余（含 FIXED_LINE_OR_MOBILE、UNKNOWN、无效输入）调用 handlerFixedPhone；不更改公开签名、注解及序列化映射，不修改姓名算法。
7. 新增 JUnit 5 参数化及 Jackson 序列化测试，补充 Web README。Build 阶段先执行目标模块及依赖 test-compile、依赖树检查和 git diff --check，记录偏差；人工确认实现后进入 Test 执行验证。

## Risks
- 地域固有歧义：无国际码默认 CN；国际类型歧义采用固定电话规则，不承诺识别所有号码真实类别。
- 区号和本地主号均可能包含重复数字或保留前导零（如意大利）；位置映射不能因规范化丢零、误切国家码而暴露或错遮数字。
- 扩展标记和号码分组存在歧义，特别是国际多个连字符；仅规定的明确分机及国内三段语法被保留，无法安全解析时 `*`。
- 国内短号需保留确认的区号；无区号主号、极短号、未知国家码不能依赖 isValidNumber 成功后才脱敏。
- 新依赖会增加 Web Starter 下游 classpath；固定版本且仅引入离线核心库，不引入 geocoder、carrier 或网络服务。
- 当前存在其他姓名、加解密及用户修改；只改声明范围，保留现有行为及工作。全仓测试此前存在 RocketMQ 外部连接失败，仍记录实际结果，不通过跳过或修改无关测试强行标绿。

## Proof
- AdaptivePhoneMaskingTest 覆盖三入口：CN +86/0086/无前缀，+1 分类歧义、+44 移动和座机、+852、意大利保留前导零；国内 010/020/0755 等区号、括号、空格、连字符、无分隔数字及无区号主号。
- 主号 1～8 位逐长度断言，另测 11 位手机；验证奇数短号的左右保留位数、国际码/区号/分机不计数，数字只在选定位置被替换。明确断言 `010-1 → 010-*`、`010-1234 → 010-1**4`、`0755-1234567 → 0755-12**567`，以及 +86/0086 对应短号；合法区号 0999 加入回归；`0000` 不符合 CN 区号结构且无对应地理区号，不得作为区号保留。
- 显式 ext/ext./x/#/转/分机、国内连字符分机，以及国际多段连字符不误保留末组；未知国家码、不规则文本、空白、null/空串及无法映射输入的回退。
- 明确手机和座机断言 handlerPhone 与专用入口相同；FIXED_LINE_OR_MOBILE/UNKNOWN 验证委派座机规则。真实 ObjectMapper 序列化带 MOBILE/FIXED_PHONE/PHONE 注解的字段，验证三映射生效。
- 回归现有 SensitiveUtilTest 姓名规则及 Web/Commons 测试，不以旧版结果代替本次实际运行。
- Build：JDK 8 `mvn -pl macula-boot-starters/macula-boot-starter-web -am test-compile -DskipTests`；`mvn -pl macula-boot-starters/macula-boot-starter-web dependency:tree -Dincludes=com.googlecode.libphonenumber:libphonenumber`；`git diff --check`。
- Test：JDK 8 先执行目标模块 `-am test -Dtest=AdaptivePhoneMaskingTest,SensitiveUtilTest -Dsurefire.failIfNoSpecifiedTests=false`，再执行 Web/Commons 全部测试及全仓 `mvn test`，记录命令、真实输出、退出码和跳过项。全仓受外部依赖阻塞时不得宣称整体绿色。

## References
- 固定版本的官方 Java POM：https://github.com/google/libphonenumber/blob/v9.0.40/java/pom.xml 。该版本明确 source/target 8，仍以本项目 JDK 8 编译及测试为最终证据。

## Implementation record
- 用户明确批准计划后，已完成三个电话入口、包内 PhoneMaskingSupport、参数化与 Jackson 序列化测试、Web POM 依赖及 README。handlerPhone 只识别并委派；姓名算法、注解、序列化映射及其他已有工作保持原样。
- 辅助类按原数字位置替换，支持国家码、可确认的地理区号与拨号前导零、明确分机；国内显式短主号通过独立虚构号码核验区号。未知类型保留原数字计数，不依赖解析器删除的运营商前缀，防止短号被错误截短。
- 首次下载固定版本依赖被沙箱的 Maven 缓存写限制阻止，经授权下载后使用 Tencent Kona JDK 8u472 执行 `mvn -pl macula-boot-starters/macula-boot-starter-web -am test-compile -DskipTests`，退出码 0，BUILD SUCCESS；日志 `/tmp/adaptive-phone-build.log`。
- 单模块 dependency:tree 因本地尚无当前版本 Commons 安装产物尝试访问缓存失败；改为同一 Reactor 内 `mvn -pl macula-boot-starters/macula-boot-starter-web -am test-compile dependency:tree -DskipTests -Dincludes=com.googlecode.libphonenumber:libphonenumber`，退出码 0，BUILD SUCCESS；依赖树确认 `libphonenumber:9.0.40:compile`，日志 `/tmp/adaptive-phone-dependencies.log`。未通过 install 改变本地项目依赖快照。
- 固定版本 PhoneNumberUtil class major version 为 52，真实 JDK 8 编译成功；`git diff --check` 通过。
- 除为解析本地 Reactor 依赖而调整检查命令外，无范围或行为偏差。测试源码已编译，未运行单测或正式运行时验证；等待人工确认实现完整后进入 Test 阶段。
- 用户随后明确确认实现完整；已进入 Test。首轮发现国际码与区号之间带连字符时分机识别失败，已在原范围修复并补回归断言。重新执行后电话 79 项、姓名 37 项全部通过，Web/Commons 合计 129 项通过且 0 跳过。
- 全仓 `mvn test` 仍因 RocketMQ 外部连接失败退出 1，未跳过或修改无关测试；真实命令、输出及限制见 verification.md，不声明全仓绿色。

## Main forward port
用户明确要求同步推送合并 main；保持本次验收的行为，按 main 的 AGENTS.md 适配 Java 17/Boot 4/Jackson 3、父 POM 集中版本和 Javadoc。上述为必要的主线兼容调整，不把 5.x 构建证据当作 main 验证结果。

## Delivery
用户明确授权双分支推送合并；已完成独立代码审查及修复、分支适配和重新验证，详见 review-findings.md 与 verification.md。现有姓名/电话注解类型和序列化调用为本次功能必要组成，一并交付。
