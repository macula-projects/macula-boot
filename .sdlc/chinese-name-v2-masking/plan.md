# Plan: 中文名 V2 脱敏规则优化 (from spec.md 2026-11-18)
Status: accepted

## Files that change
- `macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/utils/SensitiveUtil.java` # 仅调整 V2 方法、中文说明及私有间隔点判断方法。
- `macula-boot-starters/macula-boot-starter-web/src/test/java/dev/macula/boot/starter/web/utils/SensitiveUtilTest.java` # 新增定向单元测试。
- `.sdlc/chinese-name-v2-masking/plan.md` # 记录计划和实际实施偏差。
- `.sdlc/chinese-name-v2-masking/review-findings.md` # 记录 Plan 审查结果。
- `.sdlc/chinese-name-v2-masking/verification.md` # 后续 Test 阶段记录实际验证结果。

## Order of work
1. 保留已有未提交修改；按 `/` 分段并保留空段，使用 Java 8 兼容的 Unicode 码点 API 为每段独立统计非间隔点字符数。
2. 每段再次遍历字符：间隔点保留；首字保留；有效字数至少为三时末字保留；连续隐藏区只输出一个 `ASTERISK_CHAR`，间隔点重置隐藏状态；按原顺序保留所有 `/`。补充单星与分段规则中文说明。
3. 更新既有纯 JUnit 5 测试的逐字星号预期，新增斜杠分段、单字段、首尾及连续斜杠、空段、多段带间隔点姓名用例。不启动 Spring、不引入依赖，确认其他脱敏方法未改变。
4. Build 阶段进行目标模块及其依赖的测试源码编译和 `git diff --check`，记录偏差；人工确认实现后进入 Test 阶段执行定向测试。

## Risks
- 用 UTF-16 长度计数会拆开生僻汉字，统一使用 Unicode 码点。
- 间隔点不参与姓名字数，每个斜杠分段独立保留首尾；间隔点保留并分隔连续隐藏区。
- 除 `·`、`•`、`・`、`.`、`．` 外的字符均参与姓名计数，不增加格式校验。
- 相关源码已有未提交修改，只在其基础上调整 V2，不覆盖其他工作。

## Proof
- `SensitiveUtilTest` 覆盖 null、空串、单字、两字、三字、四字、五种点、多个间隔点、首尾间隔点、仅含点及 Unicode 扩展汉字；断言新版连续隐藏压缩、间隔点位置及斜杠分段结果（包括空段、连续和首尾斜杠）。
- 断言 V1 原有行为，检查差异仅涉及声明范围。
- Build：`mvn -pl macula-boot-starters/macula-boot-starter-web -am test-compile -DskipTests`；`git diff --check`。
- 后续 Test：`mvn -pl macula-boot-starters/macula-boot-starter-web -am test -Dtest=SensitiveUtilTest -Dsurefire.failIfNoSpecifiedTests=false`，记录真实输出。

## Implementation record
- 文件清单中的说明改为门禁支持的 `#` 行内注释；仅修正解析格式，文件范围及实施方案不变。
- 已调整 `handlerUsernameV2`：按 Unicode 码点统计和遍历，保留整体首尾及五种间隔点，沿用单字和空值边界规则。
- 已新增 `SensitiveUtilTest`：共 25 个参数化或普通测试用例（尚未执行），覆盖计划中的姓名边界并检查 V1 行为。
- 使用 Tencent Kona JDK 8u472 执行目标模块及依赖的 `mvn -pl macula-boot-starters/macula-boot-starter-web -am test-compile -DskipTests`，退出码 0，BUILD SUCCESS；日志 `/tmp/chinese-name-v2-build.log`。`git diff --check` 通过。
- 仅修改计划内的 V2 实现和新增测试；现有注解与序列化修改保留。除文件清单格式修正外无实施偏差。
- 等待人工确认实现完整，再进入 Test 阶段执行正式定向测试；当前编译结果不作为测试通过证据。
- 用户随后明确确认实现完整；已进入 Test 阶段。25 个脱敏用例及 Web/Commons 全部 38 个测试通过；全仓测试被 RocketMQ 外部连接失败阻塞，详细命令、输出和限制见 verification.md。

## Revision record
- 用户补充要求：连续隐藏合并单星，间隔点两侧各单星；斜杠分段独立脱敏并保留斜杠。已更新设计和步骤，文件清单不变，用户已明确确认。旧版实现和测试结果作为历史保留，不视为新版通过证据。
- 修订设计、计划及审查已验收提交 `32b78210`；实现已改为保留空段的 `/` 分段，每段独立统计首尾，以隐藏状态合并连续星号，并在间隔点处重置状态。
- 更新旧测试预期并新增斜杠、多段带点、连续和首尾斜杠、空段及长姓名用例；仍使用 Unicode 码点处理生僻汉字，不改变 V1。
- 首次编译发现 Javadoc 示例中星号紧接斜杠形成注释终止符；已换用不含该组合的分段示例，脱敏逻辑不变。
- 修正后使用 JDK 8u472 执行目标模块及依赖的 `test-compile -DskipTests`，退出码 0，BUILD SUCCESS；日志 `/tmp/chinese-name-v2-revision-build.log`；`git diff --check` 通过。修订后的测试尚未执行，等待人工确认实现后进入 Test 阶段。
- 用户随后确认修订实现完整；执行 Test 后新版 37 个姓名用例、Web/Commons 合计 50 个测试全部通过且 0 跳过。全仓 `mvn test` 仍在 RocketMQ 因外部连接失败退出 1；真实命令、输出和限制已更新 verification.md，不声明全仓绿色。

## Main forward port
用户明确要求同步推送合并 main；保持本次验收的行为，按 main 的 AGENTS.md 适配 Java 17/Boot 4/Jackson 3、父 POM 集中版本和 Javadoc。上述为必要的主线兼容调整，不把 5.x 构建证据当作 main 验证结果。
