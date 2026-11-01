# Plan: 统一 CheckOwner 资源鉴权 (from spec.md 2026-11-01)
Status: accepted

## Files that change
以下路径均相对仓库根目录。

Commons：
- macula-boot-commons/src/main/java/dev/macula/boot/security/PasskeyResource.java（新增）
- macula-boot-commons/src/main/java/dev/macula/boot/security/PasskeyIssuer.java（新增，供现有 Advice 调用的签发扩展契约）
- macula-boot-commons/src/main/java/dev/macula/boot/security/PasskeyResources.java（新增，枚举受支持包装中的列表资源）
- macula-boot-commons/src/main/java/dev/macula/boot/security/ResourceAccessDenied.java（新增，无 Spring 依赖的资源拒绝标记）
- macula-boot-commons/src/main/java/dev/macula/boot/security/ResourceAccessException.java（新增，Web 无签发器时的拒绝异常，实现上述标记）
- macula-boot-commons/src/test/java/dev/macula/boot/security/PasskeyResourcesTest.java（新增）
- macula-boot-commons/src/test/java/dev/macula/boot/result/ResultModelJsonTest.java
- macula-boot-commons/README.md

Security Starter（下列相对 macula-boot-starters/macula-boot-starter-security）：
- pom.xml（仅新增 Web Starter 的 test 范围依赖，用于双 Starter 的真实 MVC 组合验证）
- src/main/java/dev/macula/boot/starter/security/annotation/CheckOwner.java
- src/main/java/dev/macula/boot/starter/security/config/SecurityAutoConfiguration.java
- src/main/java/dev/macula/boot/starter/security/config/SecurityProperties.java
- src/main/java/dev/macula/boot/starter/security/passkey/PasskeyService.java（新增，可替换服务契约）
- src/main/java/dev/macula/boot/starter/security/passkey/HmacPasskeyService.java（新增，兼作 PasskeyIssuer）
- src/main/java/dev/macula/boot/starter/security/passkey/PasskeyRequestResolver.java（新增）
- src/main/java/dev/macula/boot/starter/security/authorization/CheckOwnerAdvisor.java（新增，统一模式判定及前后置执行）
- src/main/java/dev/macula/boot/starter/security/authorization/CheckOwnerAccessDeniedException.java（新增，继承 AccessDeniedException 并实现公共资源拒绝标记）
- src/test/java/dev/macula/boot/starter/security/test/CheckOwnerTest.java
- src/test/java/dev/macula/boot/starter/security/test/SecurityAutoConfigurationTest.java
- src/test/java/dev/macula/boot/starter/security/test/PasskeyServiceTest.java（新增）
- src/test/java/dev/macula/boot/starter/security/test/PasskeyRequestResolverTest.java（新增）
- src/test/java/dev/macula/boot/starter/security/test/PasskeyMvcTest.java（新增，聚焦 MVC 与真实代理的单元上下文）
- README.md

Web Starter（下列相对 macula-boot-starters/macula-boot-starter-web）：
- src/main/java/dev/macula/boot/starter/web/advice/ControllerResponseAdvice.java
- src/main/java/dev/macula/boot/starter/web/advice/ControllerExceptionAdvice.java
- src/main/java/dev/macula/boot/starter/web/config/WebAutoConfiguration.java
- src/test/java/dev/macula/boot/starter/web/advice/ControllerResponseAdviceTest.java（新增）
- src/test/java/dev/macula/boot/starter/web/config/WebAutoConfigurationTest.java
- README.md

流程证据：.sdlc/passkey-resource-authorization/{intent,spec,plan,review-findings,verification}.md。

## Order of work
场景原则：passkey 仅服务于需用前置授权列表确定用户可访问资源范围的接入方；明确拥有者的只读查询继续直接使用 returnObject 模式，无需 PasskeyResource、列表或凭证。文档与用例必须分别展示两类业务；任何模式失败不得降级到另一模式。HTTP 动词不决定归属模式；写操作仍不得用返回后校验来阻止副作用。

1. 新增 Commons 的 DTO 接口（resourceId 仅辅助识别且 JsonIgnore；passkey getter/setter 正常序列化）与签发 SPI。提供受限的列表提取工具，只遍历 Collection/PageVO/Result，混合列表仅选接口实现元素；单对象不签发。防止循环包装或无限递归；ResponseEntity 由 Web 层处理，Commons 不引入 Spring。
2. 在现有 SecurityProperties 增加 passkey 子配置：enabled=false、ttl=5m、secret-base64 无默认值。实现可替换 PasskeyService 及 HmacPasskeyService，注入 Clock（默认 Bean 可替换，默认服务及 Clock 仅 enabled=true 时创建），使用 HMAC-SHA256、版本/到期/签名格式、长度前缀规范编码、Base64URL、常量时间比较。定义输入长度及 token 格式约束；禁用/缺少有效配置不能签发或验证。HmacPasskeyService 实现公共签发 SPI，真实登录后填充 DTO。
3. 实现 Header/Query 提取，只解析 URL Query，拒绝重复、非法编码及冲突，同值双来源可接受。无当前 Servlet 请求的前置模式拒绝；不让客户端覆盖身份或签名配置。
4. CheckOwner 去除固定 Pre/Post 元注解，保留原注解名称、参数及目标。统一 Advisor 解析具体方法及接口注解，使用 Spring 的参数名发现和 SpEL 求值能力。依据解析后的表达式引用分类，而非 contains 字符串匹配：returnObject 模式后置，参数模式前置；混合、未知或无来源拒绝。校验真实登录、拒绝空值，无角色绕过。前置从表达式取得单个 String/整数编号，采用稳定十进制字符串；不支持集合。后置保持原认证 name 相等语义。
5. 在现有 SecurityAutoConfiguration 注册 Advisor 和可替换默认服务/Clock，配置缺失时仍保证后置兼容、前置拒绝。复用现有 AOP 基础设施；设置与 Spring Security 前置权限检查兼容的执行顺序。标准 PreAuthorize/PMS 和 CheckOwner 都必须通过，不相互覆盖。对 JDK/类代理、接口/实现、桥接方法、多个代理调用验证有效。
6. 修改现有 ControllerResponseAdvice，在原包装和跳过判定之外处理候选列表并调用公共 PasskeyIssuer。兼容既有两参构造入口，工厂使用带明确 enabled 参数的新增依赖入口。不新增响应 Advice。已包装 Result、ResponseEntity 正文、Feign/NotControllerResponseAdvice 不得绕过候选列表签发。无候选元素保持旧行为；enabled=false 时正常返回且跳过候选遍历和签发；enabled=true 有候选却无有效签发器则拒绝。关闭 response-advice 时不再自动签发，前置保护仍拒绝缺失凭证，文档明确开关边界。
7. 复用现有 ControllerExceptionAdvice：识别公共资源拒绝标记，响应 HTTP 403 与 ApiResultCode.TOKEN_ACCESS_FORBIDDEN（A0312），不输出凭证或资源内容；其他异常保持旧处理。兼容原有公开 handler 调用入口。Security 抛出的专用拒绝异常仍是 AccessDeniedException，标记让 Web 无需依赖 Security 类型。补入安全模块对 Web Starter 的 test 范围依赖，联合实际 Advisor、响应 Advice 与异常 Advice 验证真实拒绝响应。未增加生产 Starter 依赖。
8. 添加/更新上述测试与模块 README，说明统一注解模式、接口即授权签发声明、JSON 行为、Header/Query、5 分钟授权窗口、PMS/租户/实时业务状态及自调用边界。所有新 Java 类型遵循版权头和 Javadoc 要求。
9. 完成实现并记录偏差后按 Build 门禁交付；用户确认实现完成后进入 Test 阶段执行 Proof 并记录真实输出。

## Risks
- 替换 CheckOwner 底层实现必须保持现有后置语义，不能仅用切面 annotation 表达式而漏掉接口或桥接方法；参数/返回模式误判属于安全缺陷。
- 列表实现接口意味着业务授予资源访问范围；框架无法证明列表过滤正确。缓存 DTO 若跨用户共享，不能将上次用户的凭证直接当作本次签发结果；每次响应覆盖凭证，异常则不发送候选列表。
- Advice 原 supports 跳过包装，改动须分离签发与包装，避免改变 Result/ResponseEntity/Feign 行为；关闭 Advice 或用户替换 Advice 需自行接入签发，不影响前置拒绝。
- 启用时配置非法须明确失败，服务禁用不得使前置注解绕过。自定义 PasskeyService 必须同时提供签发 SPI 适配契约，避免替换服务后仍使用旧密钥签发。
- 凭证到期前可重复使用；无需读取时请求顺序要求或 Redis 状态，但无即时撤权、一次性和在线轮换。本地默认无密钥，不将凭证/密钥写日志。
- Commons 及 Web 不新增 Security 依赖；仅 Security POM 增加 Web Starter 的测试依赖，不修改生产依赖、自动配置 imports 或已有配置类划分。

## Proof
- PasskeyResourcesTest：Collection/PageVO/Result 嵌套、混合及空列表、单对象不签发、循环保护；ResultModelJsonTest：接口 JsonIgnore 生效，passkey 输出，列表和详情可为不同 DTO。
- PasskeyServiceTest：固定 Clock 的正确签发验证、篡改/换用户/换编号/到期精确边界/错误格式/过长输入/无密钥及非法 ttl/并发使用；密钥不出现在凭证或异常中。
- PasskeyRequestResolverTest：Header/Query 独立与同值双来源成功，重复/冲突/编码错误/表单同名/缺失拒绝。
- CheckOwnerTest：保留原全部拒绝与允许覆盖；新增前置 #p0/#a0/命名参数/嵌套属性、混合/无来源拒绝、字符串常量含 returnObject 不能伪造分类、接口/JDK/桥接方法及 PMS 组合。无凭证不得降级；方法体计数证明拒绝前未执行。
- SecurityAutoConfigurationTest：默认禁用、启用合法/非法、用户服务与 Clock 覆盖、旧后置无请求仍可用、前置配置/请求缺失拒绝、模板 Bean 兼容。
- PasskeyMvcTest：同时加载实际 ControllerResponseAdvice、ControllerExceptionAdvice 和 CheckOwnerAdvisor；验证 PathVariable/RequestBody、PMS 与资源保护、Header/Query 拒绝、有效凭证完成写入。缺失/错误凭证在前置拒绝时返回 HTTP 403、A0312 且方法体计数为零；后置归属不符时允许只读查询执行，随后返回 HTTP 403、A0312 且响应不包含详情；同时验证签发配置缺失的 403、普通异常既有响应不变。使用小型测试上下文，无 SpringBootTest、数据库或外部服务。
- ControllerResponseAdviceTest 与 WebAutoConfigurationTest：真实 Advice/MockMvc 加测试签发 SPI，覆盖所有包装、混合列表、单对象、无签发器、关闭/替换 Advice、Feign/跳过包装不绕过签发、旧 JSON 外壳及原构造兼容；安全模块测试覆盖真实签发 SPI 实现和双 Starter 组合。
- 定向与模块：mvn -pl macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-web -am test；先按新增/变更测试类 -Dtest 定向验证。
- 跨模块全仓 mvn test；变更涉及集成边界，按 testing.md 尝试 mvn clean verify，先确认外部环境条件，不虚报环境缺失为代码失败。
- 变更 Java 文件 Checkstyle、git diff --check、Java 17 编译；无发布/CI/REVIEW/skill/hook 变更，不需要对应配置 eval。

## Plan review adjustments
第 1 轮发现现有异常 Advice 把 AccessDeniedException 归入 500（Minor）。计划补入公共拒绝标记与专用异常、现有异常 Advice 的 403 映射及安全模块仅测试范围的 Web 依赖，建立真实组合测试。设计中的异常兼容细化为资源拒绝返回 403/A0312；不改变其他异常或生产依赖。该增量随本计划交用户接受。


## Implementation record
- 用户回复 OK 接受计划后开始实施，分支 codex/passkey-resource-authorization。
- 已实现公共契约、HMAC 服务与请求提取、统一 CheckOwner Advisor、现有配置和两个 Advice 的接入；Security 仅新增 Web Starter 的测试范围依赖。
- 支持入参位置/命名/嵌套表达式的前置凭证检查，保留返回值后置归属语义，PMS 独立检查。列表接口实现自动签发，不新增签发注解。
- 已编写/更新计划列出的单元及 MVC 测试和三个模块 README；构建使用 Java 17，mvn -pl macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-web -am test-compile -DskipTests 成功。
- 所有变更 Java 文件定向 Checkstyle 通过（0 violations）；git diff --check 通过。
- 实施修正：403 分支采用现有异常 Advice 手动设置状态，通用分支显式保持 500，避免方法级 ResponseStatus 在调用后覆盖资源拒绝状态。原一参 handler 调用入口保留。
- 文件范围偏差：无。用户回复“整体上代码我审查后是OK的”，确认实施完成并进入 Test。
- 测试修正：两个手工测试上下文的 Advisor Bean 补齐与正式自动配置一致的 ROLE_INFRASTRUCTURE，避免 Spring 方法安全基础设施忽略自定义 Advisor；正式自动配置原已包含该标记。补充正式自动配置下的有效/换编号/禁用前置验证。

## 用户补充的开关语义
用户提出“enabled=false 不定义相关 Bean”，进一步明确“列表 issue 如果 enabled=false 就不用”。据此修订既有步骤 2/5/6：默认 HMAC 服务及专用 Clock 条件装配；既有 Advice 注入同一配置开关，关闭时不遍历、不签发且正常返回列表；即使存在用户自定义 issuer，关闭时也不调用。统一 Advisor 保留供后置归属检查，并继续在关闭时拒绝前置资源调用。文件范围不扩展，补充默认/显式关闭、关闭且有自定义 issuer、启用但缺 issuer 的验证。此调整由用户直接指定，取代此前关闭时拒绝列表的设计。

- 用户补充语义已实施，最终目标模块 88 个、全仓单元 259 个、实际集成 47 个全部通过；既有可选集成跳过 3 个。clean verify、变更 Java Checkstyle 和 diff 检查成功，详见 verification.md。

- Code review round 1 修复：采用 Spring MergedAnnotation 的实际声明方法统一识别参数名与建立求值上下文，避免类代理继承接口注解时实现参数改名导致误拒绝。新增泛型接口在 JDK/CGLIB 下的有效/换编号与方法计数回归；文件范围和业务语义未扩展。
