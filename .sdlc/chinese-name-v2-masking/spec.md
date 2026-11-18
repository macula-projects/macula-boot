# Spec: 中文名 V2 脱敏规则优化 (from intent.md 2026-11-18)
Status: accepted

## Source intent
[已验收需求](intent.md)，验收提交：`4af1830b`。

## Requirements
1. 两字姓名仅保留首字：`张三 → 张*`。
2. 三字及以上姓名保留整体首尾字：`张三丰 → 张*丰`，`欧阳娜娜 → 欧**娜`。
3. `·`、`•`、`・`、`.`、`．` 保留原位且不计入字数；带点姓名按整体首尾处理：`阿不都·热合曼 → 阿**·**曼`。
4. 单字姓名保持原样；null 和空字符串仍返回 null；仅含间隔点时保持原样。
5. 按 Unicode 码点处理字符，生僻汉字不被拆开，每个被隐藏的字替换为一个星号。

## Non-goals
不更改 V1、其他脱敏规则、注解或序列化映射，不引入依赖。

## Design
只调整 `SensitiveUtil.handlerUsernameV2` 及其私有辅助方法。先统计非间隔点字符数，再遍历保留间隔点和允许展示的首尾字符，其余替换为现有 `ASTERISK_CHAR`。

## Data and interfaces
方法签名保持不变，无数据库或配置变更。

## Flagged concerns
- 字符范围：除明确列出的五种点字符外，其他字符均参与姓名计数；不扩展为姓名格式校验，non-blocking。
- 既有工作：相关源码已有用户未提交修改，实施时保留并在其基础上调整，non-blocking。
- 项目策略：未发现仓库内 AGENTS.md、CLAUDE.md、REVIEW.md 或额外组织策略；遵循当前会话的 SDLC 人工验收规则，non-blocking。

## Verification strategy
定向单元测试覆盖 null、空串、单字、两字、多字、五种间隔点、多段姓名、仅含点及 Unicode 扩展汉字；检查修改范围并运行 `git diff --check`。
