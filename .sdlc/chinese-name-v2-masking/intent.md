# Intent: 中文名 V2 脱敏规则优化
Author: Rain. Status: accepted.

## Problem
当前 `SensitiveUtil.handlerUsernameV2` 保留前两个字，导致两字姓名完全暴露，三字及以上姓名也不符合保留首尾字的要求。

## Proposed outcome
- 两字姓名只保留首字，例如：张三 → 张*。
- 三字及以上姓名保留整体首尾字，例如：张三丰 → 张*丰，欧阳娜娜 → 欧**娜。
- 带间隔点的少数民族姓名同样按整体保留首尾字，点保留原位，例如：阿不都·热合曼 → 阿**·**曼。

## Affected users and systems
使用 Web Starter 中文名 V2 脱敏规则的应用及其姓名展示场景。

## Constraints
- 在现有未提交修改的基础上，只调整中文名 V2 相关行为。
- 不更改其他脱敏规则。
- 保持 null 和空字符串返回 null 的现有行为。
- 单字姓名保持原样。
- 间隔点不计入姓名字数，并保留 `·`、`•`、`・`、`.`、`．`。

## Open questions
无。用户已确认上述规则。
