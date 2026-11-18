# Intent: 手机与座机自适应脱敏
Author: Rain. Status: accepted.

## Problem
`SensitiveUtil.handlerPhone` 当前对所有号码统一隐藏至最后四位；现有 `handlerMobile` 和 `handlerFixedPhone` 也未完整处理国际码、区号及分机号，导致不同调用入口的脱敏能力不一致。

## Proposed outcome
- `handlerPhone` 自动区分手机与座机并使用对应规则。
- 同步增强 `handlerMobile` 和现有 `handlerFixedPhone`；`handlerPhone` 仅识别号码类型并委派调用对应方法，不重复实现脱敏规则。
- 手机号保留国际码（若有）；本地号码部分保留前三位和后四位，例如 `13812340000 → 138****0000`、`+8613812340000 → +86138****0000`。
- 国际码支持全球，覆盖 `+` 和 `00` 前缀形式。
- 座机保留区号，号码部分保留后四位，例如 `010-87651234 → 010-****1234`。
- 座机分机号及其分隔符保留，例如 `010-87651234-888 → 010-****1234-888`。
- 考虑短号码：长度按去掉国际码、座机区号和分机后的有效号码计算；短号码不能因首尾保留区间重叠而完全暴露。
- 4～7 位隐藏中间两位，奇数位时左侧保留位数比右侧少一位，例如 `1234567 → 12**567`、`123456 → 12**56`；1～3 位全部隐藏。

## Affected users and systems
使用 Web Starter 的 `handlerMobile`、`handlerFixedPhone`、`handlerPhone` 及对应 MOBILE、FIXED_PHONE、PHONE 注解类型展示电话号码的应用和用户。

## Constraints
- 保留现有姓名脱敏、配置加解密工具及其他未提交修改。
- 不把姓名的单星压缩规则扩展到电话脱敏。
- 保持现有 null 和空字符串返回 null 的行为。
- 本次同时调整三个电话脱敏方法；保留既有方法名称及签名，不将 `handlerFixedPhone` 重命名为 `handlerFixPhone`。

## Open questions
- 无待确认需求：用户已确认极短号码全部隐藏；4～7 位采用中间两位规则，8 位及以上沿用各自的手机/座机规则。
- 国际号码类型识别、不带分隔符号码的歧义、格式保留及无法识别输入的回退策略在设计阶段明确，不以猜测暴露更多号码。
