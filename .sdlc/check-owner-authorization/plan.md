# Plan: CheckOwner 数据拥有者校验 (from spec.md 2026-11-01)
Status: accepted

## Files that change
- macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/annotation/CheckOwner.java（新增）
- macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/config/SecurityAutoConfiguration.java
- macula-boot-starters/macula-boot-starter-security/src/test/java/dev/macula/boot/starter/security/test/CheckOwnerTest.java（新增）
- macula-boot-starters/macula-boot-starter-security/src/test/java/dev/macula/boot/starter/security/test/SecurityAutoConfigurationTest.java（新增）
- macula-boot-starters/macula-boot-starter-security/README.md
- .sdlc/check-owner-authorization/intent.md、spec.md、plan.md、review-findings.md（流程记录）
- .sdlc/check-owner-authorization/verification.md（实际测试证据）

## Order of work
1. 添加仅方法可用、运行期保留的 CheckOwner 注解，value 必填。组合 isAuthenticated() 前置检查与已接受设计中的返回后 SpEL 模板检查。Java 文件使用标准版权头和含 author、since 的职责 Javadoc。
2. 在已有 SecurityAutoConfiguration 添加静态 AnnotationTemplateExpressionDefaults Bean 方法，并使用 ConditionalOnMissingBean 按类型回退。不新增配置类、自动配置入口、依赖或配置键。
3. 添加 CheckOwnerTest，使用无外部服务的小型 Spring 测试上下文，通过真实方法代理执行注解校验。包含允许、拒绝、缺失身份、匿名、未认证、null 结果、null 拥有者、嵌套安全导航、错误属性、不同业务属性和未标注方法；核实前置拒绝时方法体不执行，ADMIN/ROOT 不自动绕过。
4. 添加 SecurityAutoConfigurationTest，通过 WebApplicationContextRunner 加载现有自动配置，提供测试用 JwtDecoder 及所需属性，避免外部网络和凭据。验证默认模板 Bean、用户同类型 Bean 覆盖，并检查已有方法鉴权启动链路中模板生效。
5. README 补充注解导入、业务表达式、身份标识、null 行为、模板 Bean 覆盖及代理/只读/租户/响应包装限制。不得将它描述为列表过滤或写操作防护。
6. 实现完成后按流程交付；获实现确认后正式执行定向测试，再执行目标模块及依赖单元测试，并记录实际结果。

## Risks
- 模板配置为 Spring Security 全局能力，用户覆盖可能改变模板行为；通过覆盖测试及文档说明。
- 错误表达式或缺失属性可能抛出配置求值异常，不保证统一为 403，但必须不能放行；测试要求不返回数据。
- 拥有者与 authentication.name 标识或类型不同会拒绝；不新增自动身份映射或管理员绕过。
- 返回后校验不能防止方法副作用，自调用可绕过；仅限代理调用下的同步单对象只读查询，租户授权单独实施。
- 加入的公开 API 为新增能力，不改旧调用方；回退可删除注解及 Bean、文档和对应测试，无数据库迁移。

## Proof
- CheckOwnerTest 使用真实代理和认证上下文证明业务可自定义表达式、不同字段和嵌套字段正常匹配；覆盖所有上述拒绝分支、前置拒绝不执行方法、无角色隐式豁免、未标注方法行为不变。
- SecurityAutoConfigurationTest 证明现有自动配置能够装配模板默认值，并在用户提供 Bean 时只保留用户实例；至少一个调用贯穿 Starter 自动配置到注解模板解析。
- 定向验证：mvn -pl macula-boot-starters/macula-boot-starter-security -am test -Dtest=CheckOwnerTest,SecurityAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false。
- 模块验证：mvn -pl macula-boot-starters/macula-boot-starter-security -am test。
- 检查变更 Java 的版权头、职责 Javadoc、格式及 git diff --check；无跨模块源代码改动，无需外部基础设施。

## Implementation record
- 已添加 CheckOwner、现有配置中的可替换模板 Bean、10 个方法鉴权测试和 2 个自动配置测试，并补充 README。
- 实际偏差：无。生产代码未新增配置类、配置键或依赖。
- 构建检查：mvn -pl macula-boot-starters/macula-boot-starter-security -am test-compile -DskipTests 成功，生产代码及测试代码均按 Java 17 编译通过。
- git diff --check 通过。
- 用户确认实现完成后已执行测试：新增 12 个、模块及依赖 46 个、全仓 230 个单元测试均通过；变更 Java 文件 Checkstyle 0 违规。详见 verification.md。
- 测试修正：现有 SecurityProperties 依赖 SpringUtil 和 MVC 映射，自动配置测试补入静态 SpringUtil 与真实 RequestMappingHandlerMapping，并使用 WebApplicationContextRunner 自带的请求依赖。未修改原生产配置行为。
- 补录 verification.md 作为测试证据文件；仅扩充流程记录文件集，无源码范围偏差。
