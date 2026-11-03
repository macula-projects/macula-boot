# Spec: Passkey 请求参数资源凭证校验 (from intent.md 2026-11-03)
Status: accepted

## Source intent
[intent.md](intent.md)。用户在原方案获批后明确指定 HandlerMethodArgumentResolver、直接属性路径、无需 Service 支持，只校验 HTTP 请求的 passkey 与 resourceId。这条明确指示替代原方法级 Advisor 设计。

## Requirements
1. 参数级 `dev.macula.boot.starter.security.annotation.Passkey`：默认空 value 使用参数本身；`orderId` 或 `order.id` 表示已绑定 JavaBean 的属性路径。不使用 SpEL、位置参数别名或方法参数名。
2. 原 Spring 解析器先完成类型转换、RequestBody 反序列化及适用校验，再验证凭证；请求体只读取一次，绑定后的对象原样交给 Controller。未标注参数行为不变。
3. resourceId 只接受非空 String 或整数；缺认证/凭证/服务、非法路径、属性缺失/null、错误编号类型、关闭功能或验证失败均拒绝且 Controller 不执行。ADMIN/ROOT 无隐式豁免。
4. 使用可信认证身份验证凭证的用户、resourceId、签名及有效期；DTO 内 passkey 字段不是请求凭证来源。Controller 上的 PreAuthorize/PMS/PostAuthorize 仍独立生效。
5. 删除 CheckOwner 注解/Advisor/命名异常，不保留兼容入口，不交付中间版本 PasskeyAdvisor。无需 Service 方法保护；返回值鉴权完全使用标准 PostAuthorize。
6. 保留列表签发、Header/Query 规则、HMAC/token、配置、默认关闭与可替换服务/Clock、凭证拒绝 HTTP 403/A0312。
7. 主线与 5.x API 和语义一致，5.x 保持 Java 8/Boot 2.7.18/Javax/Jackson 2，主线沿用当前基线；TSF 依赖不改。

## Non-goals
不提供 Service 方法校验、任意 SpEL、Map/集合/数组批量资源校验、拥有者自动判断或凭证续期/撤销；不修改列表签发、Web 异常映射、网关/PMS/租户/数据库或 TSF 依赖；不合并或部署。

## Design
Security Starter 增加 HandlerMethodArgumentResolver 装饰器。RequestMappingHandlerAdapter 初始化完成后，轻量 BeanPostProcessor 捕获其原解析器列表，在最前面安装只处理 Passkey 参数的装饰器。内部 composite 仅包含原列表，先委托完成绑定再验证凭证，避免递归或重复读取 body；未标注参数继续走原列表。普通 WebMvcConfigurer 追加解析器不能接管已被内置解析器处理的 RequestBody/PathVariable，因此采用装饰方式。

空属性路径使用参数本身；非空使用受限 JavaBean 点分属性读取，禁止 class 反射导航、SpEL、方法调用和索引。缺属性或中间 null 拒绝。验证成功返回原绑定对象。

现有 SecurityAutoConfiguration 通过 Servlet MVC 条件注册可替换的 BeanPostProcessor，服务/配置用延迟 provider，避免提前初始化 SecurityProperties/MVC。关闭时仍安装校验器并拒绝标注参数，默认服务/Clock 仍不创建。主线模板 Bean、现有方法安全配置及 imports 保留。

## Data and interfaces
`@PathVariable @Passkey Long orderId` 使用标量；`@RequestBody @Passkey("orderId") UpdateRequest request` 使用 DTO 属性。旧入参 CheckOwner 迁移至对应参数，旧返回值用法改为标准 PreAuthorize/PostAuthorize。不再提供旧 Advisor 覆盖点，自定义 PasskeyService 保留；配置/Header/Query/JSON/token/持久化不变。

## Flagged concerns
- 删除方法级 API 与 Service 保护：non-blocking，用户明确要求 Controller 专用，文档说明迁移与范围。
- 装饰器必须兼容内置/自定义解析器且幂等安装：non-blocking，通过 MVC 与配置测试证明绑定、Valid、未标注参数及重复初始化行为。
- 参数绑定发生在方法鉴权之前，绑定/Valid 失败按 Spring 既有流程处理：non-blocking，Passkey 在绑定成功后检查，计数证明失败不执行 Controller。
- PostAuthorize 不能阻止副作用，也不继承资源异常标记：non-blocking，沿用已接受的标准异常映射边界。
- 5.x TSF 基线可能限制全仓测试：non-blocking，保留配置并如实报告。

## Verification strategy
单测覆盖标量/JavaBean/嵌套路径、非法属性或类型、认证/开关/缺服务/错误凭证、委托一次且对象不替换；真实 MVC 覆盖 PathVariable/RequestParam/RequestBody、Valid、Header/Query、列表签发、Controller 计数及 PreAuthorize/PostAuthorize 组合；配置测试证明延迟装配与幂等安装。服务 golden token 两分支一致。两分支分别做编译/Checkstyle，正式验证阶段执行目标 clean verify 与适用全仓检查，不将旧 Advisor 的编译记录当作新方案测试证据。
