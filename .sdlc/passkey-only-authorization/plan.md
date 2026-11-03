# Plan: Passkey 请求参数资源凭证校验 (from spec.md 2026-11-03)
Status: accepted

## Files that change
路径相对各分支根目录；src 路径均在 `macula-boot-starters/macula-boot-starter-security/`。主线和 5.x 同步，除另有注明。

- 删除 `src/main/java/dev/macula/boot/starter/security/annotation/CheckOwner.java`，新增同目录 `Passkey.java`（参数目标，默认空属性路径）。
- 删除 `src/main/java/dev/macula/boot/starter/security/authorization/CheckOwnerAdvisor.java`；丢弃未提交的中间版本 `PasskeyAdvisor.java`。
- 删除 `src/main/java/dev/macula/boot/starter/security/authorization/CheckOwnerAccessDeniedException.java`，新增同目录 `PasskeyAccessDeniedException.java`。
- 新增 `src/main/java/dev/macula/boot/starter/security/web/PasskeyArgumentResolver.java`。
- 新增 `src/main/java/dev/macula/boot/starter/security/web/PasskeyArgumentResolverPostProcessor.java`。
- 修改 `src/main/java/dev/macula/boot/starter/security/config/SecurityAutoConfiguration.java`，移除 Advisor、注册 MVC 参数装饰基础设施。
- 修改 `src/main/java/dev/macula/boot/starter/security/passkey/PasskeyRequestResolver.java`、`HmacPasskeyService.java`、`PasskeyService.java`，迁移拒绝异常命名。
- 删除 `src/test/java/dev/macula/boot/starter/security/test/CheckOwnerTest.java`；丢弃未提交的中间版本 `PasskeyTest.java`。
- 新增 `src/test/java/dev/macula/boot/starter/security/test/PasskeyArgumentResolverTest.java`。
- 修改 `src/test/java/dev/macula/boot/starter/security/test/SecurityAutoConfigurationTest.java`、`PasskeyMvcTest.java`，迁移参数级 API 和 MVC/方法安全组合验证。
- 仅主线修改 `src/test/java/dev/macula/boot/starter/security/test/PasskeyServiceTest.java`，补充与 5.x 相同的 golden token。
- 模块 `README.md`：标量/RequestBody/嵌套路径、旧 API 迁移及 Controller 专用边界。
- 两分支 `.sdlc/passkey-only-authorization/{intent,spec,plan,review-findings,verification}.md`（verification 在 Test 阶段生成）。

Commons/Web 生产代码、POM、TSF 依赖、现有方法安全顺序、imports 和历史 SDLC 文档不修改。

## Order of work
1. 保留现有主线/5.x 功能分支，按用户最新指示修订 intent/spec/plan 并做增量计划评审；计划确认后替换未提交的方法级中间实现。
2. Passkey 改为参数级、默认 value 为空。新增参数解析装饰器，内部 composite 保存原解析器，先绑定后校验，不重复读 body；支持标量或受限点分 JavaBean getter 路径，拒绝 SpEL、class 导航、索引、Map 和集合，保留 String/整数编号类型边界。
3. 使用延迟服务/配置 provider 检查开关、真实登录与 Header/Query 凭证；缺服务、失败或身份/编号不匹配抛资源拒绝异常。仅从请求 Header/Query 取凭证。
4. BeanPostProcessor 在 RequestMappingHandlerAdapter 初始化后于列表首位安装装饰器，内部只委托原列表；重复处理不重复包装，保留原解析器/自定义扩展。缺少委托解析器时明确失败，不静默跳过保护。自动配置按 Servlet MVC 条件及用户类型回退注册，避免过早初始化 MVC/SecurityProperties；关闭时仍安装以明确拒绝。
5. 删除旧类型和中间 Advisor，更新异常引用及 README/测试，保留主线模板 Bean 和两分支方法安全配置。Service 不提供本注解保护承诺。
6. 回移至 5.x，使用 Java 8、javax.servlet、Jackson 2、Spring 5.3 可用 API，保持语义一致。新增类型 author Rain，版权、Javadoc/since 按分支。
7. 两分支目标 clean test-compile、变更 Java Checkstyle 与 git diff --check，记录偏差；实现确认后进入正式测试及独立代码评审，保留 TSF 基线限制。

## Risks
- 参数解析早于 Controller 方法鉴权，必须依赖真实 SecurityContext，不能依赖原 Advisor 顺序；所有凭证失败阻止 Controller 执行。
- 自定义解析器、重复包装或 composite 包含自身会导致绕过/递归；必须原解析器优先委托、仅装饰一次、未标注参数行为不变。
- 属性路径只支持有 getter 的 JavaBean，缺失/中间 null 拒绝，禁止 class 等反射导航；不支持任意表达式或批量资源。
- 方法级 Service 测试不能替代 MVC 证据；PostAuthorize 独立生效并保持既有异常映射。
- 删除旧 API/Bean 覆盖点为已确认不兼容迁移；保留服务 SPI、签发协议、TSF 配置。

## Proof
- PasskeyArgumentResolverTest：supportsParameter、委托一次/原对象、String/整数/JavaBean/嵌套属性；null/空值/缺属性/中间 null/class/SpEL/索引/Map/集合/小数拒绝；禁用、缺服务、无认证/匿名/未认证/ADMIN/ROOT、缺/错凭证及身份/编号绑定。
- PasskeyMvcTest：真实 PathVariable/RequestParam/RequestBody、Valid、单次 body 解析；列表签发 token 到请求验证；Header/Query 合法与冲突、失败 Controller 计数零、合法调用成功；未标注标量/DTO 保持原行为；参数 Passkey 与 PreAuthorize/PMS/PostAuthorize 同时生效，后置拒绝查询已执行。
- SecurityAutoConfigurationTest：Servlet MVC 条件、用户回退、延迟配置；开启/默认关闭/显式关闭、服务/Clock 可替换、关闭不创建默认服务/Clock；适配器原列表保留且重复处理幂等；主线保留模板 Bean。
- 保留服务/请求提取/Commons/Web 测试，主线补 golden token，两分支固定格式一致；静态确认无旧源码及方法级 Advisor。
- 两分支定向与目标 clean verify、Checkstyle、git diff --check，5.x 真实 JDK 8；全仓检查记录 TSF 基线，不扩大依赖修复。

## Implementation record
- 第一版方法级实现仅通过编译/静态检查，尚未正式测试或提交源码。用户进一步确认参数解析器、直接属性路径与仅 Controller 后，该中间实现不再交付。
- 此修订新增 MVC 装饰器并替换方法代理测试，属于用户明确变更带来的必要计划调整。旧编译/评审结果保留在历史提交及 review-findings，不作为新方案验证证据；用户已回复“确认”，修订计划已接受并提交至两条分支。
- 已在两条分支完成参数级 Passkey、受限 JavaBean 属性提取、原解析器委托、初始化后幂等安装与延迟服务/配置装配；未提交的 PasskeyAdvisor/PasskeyTest 已移除。既有 CheckOwner 源码与测试删除，拒绝异常更名，保留原服务与签发协议。
- 已重写参数解析单测、真实 MVC 和自动配置测试源码，覆盖 PathVariable/RequestParam/RequestBody、Valid、body 单次实例化、嵌套属性、自定义解析器、未标注参数、默认关闭/缺服务及标准方法安全组合；README 使用参数级示例。主线固定向量断言保留，5.x 既有向量不改。
- Build 编译：主线 Tencent JDK 17.0.17、5.x Tencent JDK 1.8.0_472，分别执行 Security 及依赖的 `clean test-compile -DskipTests`，退出码均为 0。此阶段未运行正式测试。
- 对两条分支变更 Java 文件（含新增 web 包）使用主线版本化 Checkstyle 规则，均 0 violations；5.x 用 `checkstyle.source.root` 指向该工作树，确保实际扫描新源码。两条分支 git diff --check 均无输出。
- 无额外文件范围偏差；Commons/Web 生产代码、POM、TSF 依赖、方法安全配置、imports 及历史 SDLC 文件未改。等待人工实现确认后进入 sdlc-test；当前只证明编译和静态检查，不声称测试通过。
- 按用户要求，新增注解、解析器、安装处理器、拒绝异常和自动配置接入点补充中文职责与 API 文档；在委托绑定、凭证来源、受限属性路径、缺服务拒绝和幂等安装处解释设计原因。两分支同步，测试类职责说明也改为中文。
- Test 阶段首轮定向测试发现新增 MVC 用例错误假设 Valid 错误返回 400；仓库原有 ControllerExceptionAdvice 明确返回 HTTP 500/A0400。仅修正两分支测试预期并断言 MethodArgumentNotValidException，不修改既有生产异常处理。之后定向测试主线 22、5.x 21 全部通过，相关模块 clean verify 主线 87、5.x 41 全部通过；完整证据以 verification.md 为准。
- 用户最终确认 README 仅展示当前接入方式，不提旧注解名称；保留 PasskeyResource 接口源码、OrderListItem 实现、列表查询、前端 Query/Header 和参数注解示例，压缩其余说明。该文档要求替代早期计划的旧 API 迁移对照描述；最后几轮仅改 README，不影响既有测试结论。
