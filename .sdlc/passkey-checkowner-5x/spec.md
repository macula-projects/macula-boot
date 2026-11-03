# Spec: 将 PassKey 与 CheckOwner 资源鉴权回移至 5.x (from intent.md 2026-11-02)
Status: accepted

## Source intent
[intent.md](intent.md)：将主线 PR #43 已确认的完整 PassKey 与统一 CheckOwner 外部契约及安全语义回移到 Java 8、Spring Boot 2.7.18 的 `5.x` 分支。

## Requirements
1. 5.x 提供与主线一致的单一 `@CheckOwner` 契约：引用 `returnObject` 的表达式在目标方法成功返回后比较当前登录用户与拥有者；引用方法入参的表达式在目标方法执行前验证 PassKey。混合来源、未知来源、空值或无法明确分类的表达式必须拒绝，不能根据客户端是否携带凭证切换模式。
2. 两种模式都要求真实登录用户，不为 ADMIN、ROOT 或其他角色提供隐式豁免。前置模式缺少凭证、格式错误、签名错误、过期、用户不匹配或资源编号不匹配时必须拒绝，并且目标方法不得执行；后置模式拒绝时查询可以已经执行，但结果不得返回给调用方。
3. Commons 提供与主线同名、同包名和同语义的 `PasskeyResource`、`PasskeyIssuer`、`PasskeyResources`、`ResourceAccessDenied`、`ResourceAccessException` 公共契约。`resourceId` 不输出到 JSON，`passkey` 正常输出；列表 DTO 与详情 DTO 可以不同，但可以实现同一契约。
4. 启用 PassKey 时，现有响应 Advice 自动签发已完成业务数据权限过滤的列表元素，不新增签发注解。支持 `Collection`、`PageVO`、成功的 `Result` 及 `ResponseEntity` 正文中的候选列表；混合列表只处理实现 `PasskeyResource` 的元素；单对象不自动签发。原有 Result 包装、Feign 跳过、`@NotControllerResponseAdvice` 及 String 响应行为保持兼容。
5. PassKey 绑定当前 `authentication.name`、全局唯一且稳定的资源编号及有效期，不包含租户、资源类型或 action。默认实现使用 HMAC-SHA256、无歧义长度编码、Base64URL 和常量时间比较，不记录密钥或凭证；默认 TTL 为 5 分钟，签名密钥至少 32 字节且没有默认值。
6. 新增 `macula.security.passkey.enabled`、`ttl`、`secret-base64` 配置，默认 `enabled=false`。关闭时不创建默认 PassKey 服务和专用 Clock，不遍历或签发列表；后置 CheckOwner 继续工作，前置 PassKey 模式明确拒绝而不是放行。启用但缺少有效签发器或配置时必须失败。
7. 接受 `X-Resource-Passkey` Header 或 URL Query 中的 `passkey`；拒绝重复值、冲突值和非法编码，不读取表单参数；Header 与 Query 各一个且值相同可以接受。资源拒绝通过现有 Web 异常体系返回 HTTP 403 和 `A0312`，其他异常的既有响应保持不变。
8. 公开 API、配置键、JSON、HTTP 状态及鉴权时序与主线 PR #43 保持一致；实现源码、依赖和测试必须兼容 Java 8、Spring Boot 2.7.18、Spring Framework 5.3、Spring Security 5.7、Jackson 2 和 `javax.servlet`。

## Non-goals
不修改网关、JWT 认证流程、数据库、MyBatis 生产行为、租户或 PMS 权限模型；不新增批量资源写入、异步/流式签发、立即撤权、一次性凭证、在线密钥轮换或自动推导业务权限；不改变现有 `@Inner`、URL 白名单或资源服务器过滤链；不合并 PR，不发布、不打 tag、不部署。全仓验证发现 MyBatis 两个测试类共享 H2 上下文并相互污染后，用户于 2026-11-02 明确同意在同一 PR 中增加测试上下文隔离；该修正不改变生产代码或接口。

## Design
公共契约放在 Commons，保持对具体 Starter 和 Spring Security 的零依赖。`PasskeyResources` 仅遍历明确支持的响应容器并进行循环和深度保护，不扫描任意对象图。实现 `PasskeyResource` 仍表示业务已经完成当前用户的数据权限过滤；框架不尝试替代业务授权。

Security Starter 提供可替换的 `PasskeyService`，默认 `HmacPasskeyService` 同时实现公共 `PasskeyIssuer`。服务从 Spring Security 上下文取得可信身份，使用注入的 `Clock` 计算到期时间。Java 8 实现保留主线的字段长度限制、密钥校验、HMAC 格式、常量时间比较和并发安全，不使用 Java 9+ 集合工厂、模式匹配或其他高版本语法/API。

`@CheckOwner` 由一个基础设施级 Advisor 统一处理。Advisor 从具体方法、接口方法和桥接方法中解析实际注解声明及参数名，使用 Spring 5.3 可用的 SpEL 与参数名发现能力建立求值上下文。表达式只引用返回值时执行后置拥有者检查，只引用入参时在调用前解析单个字符串或整数资源编号并验证请求 PassKey。它与 5.x 的 `@EnableGlobalMethodSecurity(prePostEnabled = true)`、`@PreAuthorize` 和 PMS 独立叠加，所有鉴权都必须通过；代理顺序不得允许目标方法在前置拒绝后执行。

`PasskeyRequestResolver` 使用 `javax.servlet.http.HttpServletRequest` 和 Spring 5.3 Web 工具解析 Header 与原始 URL Query，保持主线重复、冲突和表单忽略规则。没有当前 Servlet 请求时，前置模式拒绝。

现有 `SecurityProperties` 增加 passkey 子配置。`SecurityAutoConfiguration` 继续使用 5.x 已登记的 Boot 2.7 自动配置入口，条件创建可替换的默认服务和 Clock，并注册基础设施 Advisor；不新增自动配置类。`enabled=false` 时不创建默认签发服务，但 Advisor 保留以兼容后置模式并拒绝不可用的前置模式。

现有 `ControllerResponseAdvice` 使用 Jackson 2 与 `javax.servlet` 兼容类型，在原响应包装判定之外调用公共 `PasskeyIssuer`。签发判断不受 Result、ResponseEntity、Feign 或 `@NotControllerResponseAdvice` 的原包装跳过逻辑影响。现有 `ControllerExceptionAdvice` 只对公共 `ResourceAccessDenied` 标记映射 HTTP 403/A0312，其他异常维持 5.x 当前语义。Web Starter 只依赖 Commons；Security Starter 通过 Commons SPI 与 Web 解耦，并仅在测试范围引入 Macula Web Starter 验证真实组合。

## Data and interfaces
不新增数据库表或持久化数据。新增的公共 Java 类型、包名、配置键、Header、Query 参数和 JSON 行为与主线一致：

- `dev.macula.boot.security.PasskeyResource`
- `dev.macula.boot.security.PasskeyIssuer`
- `dev.macula.boot.security.PasskeyResources`
- `dev.macula.boot.security.ResourceAccessDenied`
- `dev.macula.boot.security.ResourceAccessException`
- `dev.macula.boot.starter.security.annotation.CheckOwner`
- `dev.macula.boot.starter.security.passkey.PasskeyService`
- `macula.security.passkey.enabled`，默认 `false`
- `macula.security.passkey.ttl`，默认 `5m`
- `macula.security.passkey.secret-base64`，无默认值
- `X-Resource-Passkey` Header 或 `passkey` URL Query

使用 Jackson 2 的 `com.fasterxml.jackson.annotation.JsonIgnore` 隐藏辅助 `resourceId`。凭证格式和校验输入与主线兼容，使同一密钥、身份、资源编号和时间条件下的 5.x 与主线服务遵循相同验证规则。

## Flagged concerns
- Spring Security 5.7 使用旧版方法安全配置和代理基础设施，主线 Advisor 不能未经验证直接复制；必须证明与 `@PreAuthorize`/PMS 的顺序、JDK/CGLIB 代理、接口注解、泛型桥接方法均正确，否则可能出现前置鉴权晚于方法执行或合法请求被拒绝：non-blocking，因为无需外部决策，可通过 5.x 专用实现和回归测试解决。
- 主线源码包含 Java 17 语法以及 Jackson 3、Jakarta Servlet 等较新 API，回移必须保持行为而不是逐文件 cherry-pick；任何语法降级都可能引入边界差异：non-blocking，因为外部契约已经明确，差异可通过交叉测试固定。
- 5.x 的 `SecurityProperties.afterPropertiesSet()` 会访问 MVC HandlerMapping，自动配置测试必须使用合适的 Web 上下文或隔离该既有初始化行为，避免把测试装配问题误判为 PassKey 缺陷：non-blocking，因为不需要改变既有生产语义。
- 列表实现 `PasskeyResource` 仍是业务已经完成数据权限过滤的授权声明；框架无法验证列表过滤正确性，跨用户缓存同一可变 DTO 也可能造成竞态：non-blocking，这是主线已经接受的接入约束，README 必须在 5.x 中同样明确。
- PassKey 在 TTL 内可重复使用且没有立即撤权、一次性消费或在线轮换能力：non-blocking，这是已接受的首版边界，PMS 和业务仍负责操作权限与实时状态。

## Verification strategy
使用 Java 8 工具链从最小相关范围逐步验证。Commons 测试覆盖支持容器、混合列表、单对象、循环保护和 Jackson 2 JSON；固定 Clock 的服务测试覆盖签发、篡改、换用户、换编号、精确到期边界、非法密钥/TTL/格式、超长输入及并发。

Security 测试覆盖 Header/Query 解析、表单忽略、配置开关、自定义 Bean 覆盖、无请求/无服务拒绝，以及 CheckOwner 的入参位置、命名、嵌套属性、returnObject、混合表达式拒绝、JDK/CGLIB/接口/泛型桥接和 PMS 组合。前置拒绝通过方法计数证明目标方法未执行；后置拒绝证明查询已执行但结果未返回。

Web 与真实 MVC 组合测试覆盖列表、分页、Result、ResponseEntity、混合元素、单对象、Feign/跳过包装、String 响应、禁用签发、缺少签发器、HTTP 403/A0312 及普通异常兼容。自动配置测试覆盖默认关闭、显式关闭、合法启用、非法配置和用户 Bean 替换。

执行定向测试、Security/Web 及依赖模块测试、全仓 `mvn test`、适用的 `mvn clean verify`、变更 Java Checkstyle 和 `git diff --check`，并核对编译器实际使用 Java 8。任何依赖外部服务而未运行的测试必须单独说明，不得用主线历史结果替代 5.x 当前证据。

政策依据：本会话提供的 `AGENTS.md`；主线 `.agents/rules/architecture.md`、`starter-development.md`、`testing.md`、`dependencies-release.md`；主线 `REVIEW.md` 的 Bugs、Security、Compliance 三类评审。5.x 当前没有版本化的 `AGENTS.md`、`REVIEW.md`、`bands.yaml` 或组织级技能，未发现额外品牌、数据处理或合规政策。
