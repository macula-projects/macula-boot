# Spec: CheckOwner 数据拥有者校验 (from intent.md 2026-11-01)
Status: accepted

## Source intent
[intent.md](intent.md)，用户于 2026-11-01 明确回复“好，做吧”接受需求。

## Requirements
1. Security Starter 提供方法级 @CheckOwner，value 为必填 SpEL 表达式，由业务指定返回数据的拥有者，支持嵌套属性及安全导航。
2. 执行方法前要求真实登录；执行方法后，仅当返回值非空、拥有者非空且与 authentication.name 相等时允许返回。不自动授予 ADMIN 或 ROOT 绕过权限。
3. 使用 Spring Security 注解模板组合 @PreAuthorize 与 @PostAuthorize，不新增业务鉴权切面。
4. AnnotationTemplateExpressionDefaults 的静态 Bean 方法添加到现有 SecurityAutoConfiguration，并允许应用提供同类型 Bean 覆盖；不新增 Configuration 类。
5. 未标注方法保留现有行为。README 提供直接返回对象、嵌套返回对象、当前用户标识和代理限制的示例。
6. 测试验证真实 Spring 方法代理中的允许和拒绝行为，以及模板 Bean 默认装配和用户覆盖。

## Non-goals
不修改 Result、网关或数据库；不提供数字用户 ID claim 选择、租户授权、管理员绕过、列表过滤、写操作授权和异步返回对象解析。

## Design
新增 dev.macula.boot.starter.security.annotation.CheckOwner：运行期保留、仅标注方法，组合 @PreAuthorize("isAuthenticated()") 和 @PostAuthorize("returnObject != null and ({value}) != null and ({value}) == authentication.name")。

value 接收完整表达式，如 returnObject.buyerUsername 或 returnObject.data?.buyerUsername。表达式直接参与现有 SpEL 求值，不做字符串/数字隐式归一化。配置 Bean 加 @ConditionalOnMissingBean(AnnotationTemplateExpressionDefaults.class)，复用现有 @EnableMethodSecurity。

方法返回后检查发生在 ResponseBodyAdvice 包装前。属性不存在或表达式执行异常不能放行，但不保证所有配置错误都转换为业务拒绝响应。业务必须通过 Spring 代理调用，查询不得产生副作用；多个方法安全注解的组合须遵循 Spring Security 规则。

## Data and interfaces
新增公开注解 String value()，无默认值。新增可替换的模板配置 Bean；无配置键、数据库和 HTTP 响应结构变化。

## Flagged concerns
- 当前比较对象为 authentication.name，默认 JWT sub；业务拥有者必须采用相同身份标识及兼容类型，否则拒绝或表达式失败，non-blocking。
- 返回后校验不能撤销方法已产生的副作用，不支持列表自动过滤；文档明确只用于同步单对象只读查询，non-blocking。
- 同类自调用绕过默认代理，业务需经注入 Bean 调用；测试通过代理验证，non-blocking。
- 租户边界不在本次范围；标识非全局唯一的业务必须另行落实租户校验，non-blocking。
- 用户提供的模板 Bean 会决定全局模板解析行为；提供覆盖测试并记录这是 Spring Security 全局能力，non-blocking。
- 需求作者署名尚未确认，不影响行为和实现范围，non-blocking。

## Verification strategy
使用无外部依赖的 Spring 上下文和方法代理，覆盖匹配、不匹配、匿名、未认证、null 返回、null 拥有者、安全导航嵌套、错误属性不放行、未标注方法不受影响。使用 ApplicationContextRunner 验证模板 Bean 默认装配及用户替换。

执行目标 Security Starter 及其依赖的单元测试；检查变更 Java 文件的版权头、Javadoc 和格式。无需外部 Redis、认证服务器或数据库。

政策依据：AGENTS.md、.agents/rules/starter-development.md、.agents/rules/testing.md、REVIEW.md、bands.yaml；未发现额外适用的组织技能政策。
