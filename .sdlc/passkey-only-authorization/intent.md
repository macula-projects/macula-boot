# Intent: Passkey 单一前置资源凭证校验
Author: Rain. Status: accepted.

## Problem
现有 CheckOwner 根据表达式在返回值拥有者检查和调用前资源凭证检查之间选择行为，同一注解承担两种鉴权职责，增加理解和维护成本。

## Proposed outcome
将 CheckOwner 改为仅校验 Passkey 的方法注解，所有凭证拒绝发生在业务方法执行前；返回值鉴权由业务直接使用 Spring Security 的 PostAuthorize 表达式完成。

## Affected users and systems
主线与 5.x 的 Security Starter、其自动配置、相关 MVC 测试及模块接入文档，以及使用原 CheckOwner 的应用。

## Constraints
用户于 2026-11-03 明确确认：主线和 5.x 同时调整，直接移除 CheckOwner，不保留兼容入口。新注解名为 Passkey；5.x 保持 Java 8/Spring Boot 2.7.18 兼容，主线保持其现有版本基线。保留列表签发、配置开关、凭证格式与 TSF 依赖配置。作者 Rain。

## Open questions
无待用户决定的范围问题；鉴权顺序和错误处理边界在 spec 中明确。
