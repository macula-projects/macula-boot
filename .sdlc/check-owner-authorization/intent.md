# Intent: CheckOwner 数据拥有者校验
Author: 待确认. Status: accepted.

## Problem
业务接口在用户通过登录认证后，仍可能因订单号等资源参数被修改而返回其他用户的数据。重复编写返回值归属校验表达式容易产生遗漏和不一致。

## Proposed outcome
在 macula-boot-starter-security 提供统一的 CheckOwner 注解。业务开发者自行指定返回值中拥有者的 SpEL 表达式，支持不同字段名称及嵌套结构，并与 authentication.name 比较。归属不匹配、未认证、返回值为空或缺少必要归属信息时，数据不能返回给调用者。提供使用文档和回归验证。

## Affected users and systems
Macula Boot Security Starter 及其业务调用方；订单等具有明确个人归属的详情查询接口；使用这些接口的登录用户。

## Constraints
AnnotationTemplateExpressionDefaults 的 Bean 定义加入 Starter 现有 SecurityAutoConfiguration，不单独新建 Configuration 类。
保持 Java 17 和现有 Spring Security 方法鉴权机制兼容。不改变未使用该注解的接口行为。校验依据必须来自可信认证上下文及服务端查询结果。仅覆盖无副作用的同步单对象查询，不替代列表数据范围控制和写操作的执行前鉴权。必须明确 Spring 代理调用和响应包装对使用方式的影响。

## Open questions
- 需求作者署名待确认。
