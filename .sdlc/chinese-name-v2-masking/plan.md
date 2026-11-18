# Plan: 中文名 V2 脱敏规则优化 (from spec.md 2026-11-18)
Status: accepted

## Files that change
- `macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/utils/SensitiveUtil.java` # 仅调整 V2 方法、中文说明及私有间隔点判断方法。
- `macula-boot-starters/macula-boot-starter-web/src/test/java/dev/macula/boot/starter/web/utils/SensitiveUtilTest.java` # 新增定向单元测试。
- `.sdlc/chinese-name-v2-masking/plan.md` # 记录计划和实际实施偏差。
- `.sdlc/chinese-name-v2-masking/review-findings.md` # 记录 Plan 审查结果。
- `.sdlc/chinese-name-v2-masking/verification.md` # 后续 Test 阶段记录实际验证结果。

## Order of work
1. 保留已有未提交修改；使用 Java 8 兼容的 Unicode 码点 API 统计非间隔点字符数。
2. 再次遍历字符：间隔点保留原位；首字保留；有效字数至少为三时末字保留；其余替换为 `ASTERISK_CHAR`。补充方法中文说明。
3. 新增纯 JUnit 5 单元测试，不启动 Spring、不引入依赖。确认其他脱敏方法未改变。
4. Build 阶段进行目标模块及其依赖的测试源码编译和 `git diff --check`，记录偏差；人工确认实现后进入 Test 阶段执行定向测试。

## Risks
- 用 UTF-16 长度计数会拆开生僻汉字，统一使用 Unicode 码点。
- 间隔点不能作为姓名字数或首尾位置；多段姓名只保留整体首尾。
- 除 `·`、`•`、`・`、`.`、`．` 外的字符均参与姓名计数，不增加格式校验。
- 相关源码已有未提交修改，只在其基础上调整 V2，不覆盖其他工作。

## Proof
- `SensitiveUtilTest` 覆盖 null、空串、单字、两字、三字、四字、五种点、多个间隔点、首尾间隔点、仅含点及 Unicode 扩展汉字；断言星号数量和点的位置。
- 断言 V1 原有行为，检查差异仅涉及声明范围。
- Build：`mvn -pl macula-boot-starters/macula-boot-starter-web -am test-compile -DskipTests`；`git diff --check`。
- 后续 Test：`mvn -pl macula-boot-starters/macula-boot-starter-web -am test -Dtest=SensitiveUtilTest -Dsurefire.failIfNoSpecifiedTests=false`，记录真实输出。

## Implementation record
- 文件清单中的说明改为门禁支持的 `#` 行内注释；仅修正解析格式，文件范围及实施方案不变。
