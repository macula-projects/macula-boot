# Spec: 统一 CheckOwner 资源鉴权 (from intent.md 2026-11-01)
Status: accepted

## Source intent
[intent.md](intent.md)。用户明确确认使用单个 CheckOwner 注解自动选择校验方式及时机。

## Requirements
场景划分：能从返回数据明确判断当前用户归属的查询，使用 CheckOwner(returnObject...)；无法直接判断用户与资源编号关系、需通过已授权列表框定范围的操作，使用 CheckOwner(入参表达式) 验证 passkey。两者不是按 HTTP 动词或是否传凭证自动切换，也不互相兜底。直接归属查询不必实现 PasskeyResource 或先查询列表。

1. returnObject 表达式返回后检查拥有者；入参表达式执行前验证 passkey。混合或不能明确分类的表达式拒绝，不能依据请求是否携带凭证切换模式。
2. 所有模式要求登录，无 ADMIN/ROOT 隐式豁免。前置模式拒绝缺失、错误、过期、用户或资源不匹配的凭证，目标方法不执行；兼容标准参数绑定及 PMS PreAuthorize。
3. 统一 PasskeyResource 接口声明资源编号和 passkey 的 getter/setter；隐藏辅助 resourceId 的 JSON 属性。不同列表、详情 DTO 可实现同一接口。
4. 不新增签发注解；Advice 对列表中实现 PasskeyResource 的元素自动签发，其他元素保持原样。支持 Collection、PageVO、Result 包装及 ResponseEntity 正文。实现接口即代表业务已完成当前用户的列表数据权限校验。单对象返回不因实现接口自动签发。
5. 凭证绑定当前用户、全局唯一编号、有效期，不含租户、类型、action。PMS 及业务继续负责操作权限和实时状态。

## Non-goals
不修改网关，不支持批量写、异步/流式对象、即时撤权、一次性消费、在线密钥轮换或自动推导业务权限。

## Design
CheckOwner 使用统一方法拦截组件替代固定 PostAuthorize 组合，根据表达式上下文确定前置/后置，保留现有模板 Bean。后置示例 returnObject.buyerUsername；前置示例 #p0、#p0.orderId 或命名入参。

复用 SecurityAutoConfiguration、SecurityProperties 和 ControllerResponseAdvice。公共 DTO 接口及响应处理扩展契约放 commons；Web Advice 调用可选处理器，Security 提供实现，避免两个 Starter 相互依赖。处理与原包装判定分开，防止 Result 跳过、Feign 等包装规则造成漏签发；启用时检测到需签发的列表元素却无有效处理器或签名配置则拒绝；关闭时所有列表正常返回且跳过签发。

签名采用服务端独占密钥、HMAC-SHA256、长度明确的字段编码、Base64URL、常量时间比较及可注入时间源。凭证包含版本、到期时间和签名，不记录凭证或密钥日志。

现有 SecurityProperties 新增 passkey 子配置：enabled 默认 false，ttl 默认 5 分钟，secret-base64 无默认值；启用时验证至少 32 字节密钥及正 ttl，支持自定义服务 Bean。用户补充确认关闭功能的语义：关闭后不创建默认 PasskeyService 与专用 Clock，列表正常返回且跳过资源遍历/签发；后置 CheckOwner 不受影响，前置 Passkey 校验仍拒绝。启用时有候选列表但无有效签发器仍拒绝。

接受 X-Resource-Passkey Header 或 URL Query 的 passkey，拒绝重复和冲突，不读取表单参数。同值双来源可接受。校验失败使用 AccessDeniedException；异常处理与现有机制兼容。

## Data and interfaces
保留 CheckOwner(value)，新增 PasskeyResource、公共响应处理扩展和签发验证服务。Result 外壳不变，仅接入 DTO 新增 passkey；新增 macula.security.passkey.* 配置。Security POM 仅新增 Web Starter 测试范围依赖，用于计划审查要求的真实 MVC 组合验证；生产依赖及自动配置 imports 不变。

## Flagged concerns
- 业务必须保证所有返回 PasskeyResource 列表元素的入口均已完成当前用户的数据权限过滤；不得在未授权公共列表复用该 DTO。凭证授权持续至过期，non-blocking。
- 当前用户为 authentication.name，编号须全局唯一且表示稳定，non-blocking。
- 单注解实现改变底层拦截机制，须回归现有用法、真实代理、接口方法和 PMS 组合；自调用仍不受代理保护，non-blocking。
- Advice 的原包装/跳过行为必须保持兼容，同时覆盖已包装结果签发，non-blocking。
- 用户已确认接口实现即为列表签发声明，不新增 IssuePasskey；用户回复 YES 接受包含 5 分钟默认值及上述首版容器范围的本设计，原 blocking concern 已解决。
- 需求作者署名待确认，不影响行为，non-blocking。

## Verification strategy
回归现有 CheckOwner，验证前置模式的入参解析、PMS 组合、模式不可降级、失败不执行、匿名及配置缺失拒绝。固定 Clock 覆盖篡改、用户/编号不符、到期边界、格式错误、配置校验、Bean 替换及 Header/Query 冲突。

通过实际 Advice/MockMvc 验证列表与不同详情 DTO、上述包装、混合列表只处理接口实现元素、单对象不自动签发、JSON 字段及未接入接口兼容。运行目标模块及依赖、跨模块全仓单元测试、变更 Java Checkstyle。无需修改审查策略或对应 eval。

政策依据：AGENTS.md、architecture.md、starter-development.md、testing.md、REVIEW.md；未发现额外适用组织政策。
