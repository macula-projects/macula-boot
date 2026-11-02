# Plan: 将 PassKey 与 CheckOwner 资源鉴权回移至 5.x (from spec.md 2026-11-02)
Status: accepted

## Files that change
以下路径均相对 5.x 仓库根目录。

Commons：
- `macula-boot-commons/src/main/java/dev/macula/boot/security/PasskeyResource.java`（新增）
- `macula-boot-commons/src/main/java/dev/macula/boot/security/PasskeyIssuer.java`（新增）
- `macula-boot-commons/src/main/java/dev/macula/boot/security/PasskeyResources.java`（新增）
- `macula-boot-commons/src/main/java/dev/macula/boot/security/ResourceAccessDenied.java`（新增）
- `macula-boot-commons/src/main/java/dev/macula/boot/security/ResourceAccessException.java`（新增）
- `macula-boot-commons/src/test/java/dev/macula/boot/security/PasskeyResourcesTest.java`（新增）
- `macula-boot-commons/src/test/java/dev/macula/boot/result/ResultModelJsonTest.java`（新增）
- `macula-boot-commons/README.md`

Security Starter：
- `macula-boot-starters/macula-boot-starter-security/pom.xml`（仅增加 Macula Web Starter 的 test 范围依赖）
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/annotation/CheckOwner.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/authorization/CheckOwnerAccessDeniedException.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/authorization/CheckOwnerAdvisor.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/config/SecurityAutoConfiguration.java`
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/config/SecurityProperties.java`
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/config/ResourceServerConfiguration.java`（Test 阶段发现 Spring Security 5.7 旧方法安全 Advisor 默认与 CheckOwner 同为最低优先级，需显式提前一位）
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/passkey/PasskeyService.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/passkey/HmacPasskeyService.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/passkey/PasskeyRequestResolver.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/test/java/dev/macula/boot/starter/security/test/CheckOwnerTest.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/test/java/dev/macula/boot/starter/security/test/PasskeyMvcTest.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/test/java/dev/macula/boot/starter/security/test/PasskeyRequestResolverTest.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/test/java/dev/macula/boot/starter/security/test/PasskeyServiceTest.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/src/test/java/dev/macula/boot/starter/security/test/SecurityAutoConfigurationTest.java`（新增）
- `macula-boot-starters/macula-boot-starter-security/README.md`

Web Starter：
- `macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/advice/ControllerResponseAdvice.java`
- `macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/advice/ControllerExceptionAdvice.java`
- `macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/config/WebAutoConfiguration.java`
- `macula-boot-starters/macula-boot-starter-web/src/test/java/dev/macula/boot/starter/web/advice/ControllerResponseAdviceTest.java`（新增）
- `macula-boot-starters/macula-boot-starter-web/src/test/java/dev/macula/boot/starter/web/config/WebAutoConfigurationTest.java`（新增）
- `macula-boot-starters/macula-boot-starter-web/README.md`

验证基线修复：
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/test/UserSeqMapperTest.java`（用户授权增加测试上下文隔离，不改变生产行为）

SDLC 证据：
- `.sdlc/passkey-checkowner-5x/plan.md`
- `.sdlc/passkey-checkowner-5x/review-findings.md`
- `.sdlc/passkey-checkowner-5x/verification.md`（Test 阶段生成）

现有 `AutoConfiguration.imports` 不新增配置类，预计无需修改；实施时仍核对现有 Security/Web 自动配置登记可发现修改后的配置。

## Order of work
1. 以主线 PR #43 的最终代码和测试为行为基准，先将 Commons 公共契约与受限容器遍历工具改写为 Java 8 语法，使用 Jackson 2 注解保持 `resourceId` 隐藏、`passkey` 输出及单对象不签发语义；同步建立纯单元测试。
2. 回移 `PasskeyService` 与 `HmacPasskeyService`。保留主线 token 格式、HMAC-SHA256、长度前缀编码、Base64URL、常量时间比较、身份/资源/过期绑定、输入长度限制及可注入 `Clock`；使用 Java 8 API 表达 TTL 合法性和字符串判空，不引入新加密依赖。
3. 回移 `PasskeyRequestResolver`，将 Jakarta Servlet 改为 `javax.servlet`，保持 Header/原始 Query 的重复、冲突、编码错误和表单忽略行为；先用独立单元测试固定输入边界。
4. 新增统一 `@CheckOwner` 与 `CheckOwnerAdvisor`。将主线的模式匹配、record、`String.isBlank` 等语法/API 改写为 Java 8；在 Spring 5.3 中解析实际注解声明方法、接口/实现/桥接方法参数名和 SpEL AST。Advisor 作为基础设施 Bean 注册，并显式验证它位于 5.x `@PreAuthorize` 之后、目标方法之前；前置拒绝不得执行方法，后置检查只在方法返回后执行。
5. 扩展 `SecurityProperties` 和 `SecurityAutoConfiguration`：默认关闭，合法启用时条件创建可替换的 UTC Clock 与默认服务；关闭时不创建两者；Advisor 始终存在以保留后置检查并拒绝不可用的前置模式。沿用现有 Boot 2.7 `AutoConfiguration.imports`，不引入主线仅有的 `AnnotationTemplateExpressionDefaults`。
6. 扩展现有 Web Advice 和自动配置：用 Jackson 2、`javax.servlet` 和 5.x Spring MVC API 在包装判定之外签发列表；保留两参构造兼容入口，新增延迟 `PasskeyIssuer` 与 enabled 注入；公共资源拒绝映射为 HTTP 403/A0312，其他异常和 Result 包装行为保持不变。Security 仅通过 Commons SPI 与 Web 解耦，Macula Web Starter 只作为 Security 测试依赖。
7. 按主线用例语义重写 Java 8/JUnit 5 测试，补足 Commons、Security、Web 和实际 MVC 组合覆盖；同步三份模块 README，说明统一注解模式、配置、列表签发声明、PMS/业务责任、TTL 与关闭语义。所有新增 Java 类型和测试补齐仓库版权头、类级 Javadoc、`@author Rain` 和适合 5.x 的 `@since 5.0.21`。
8. 完成 Build 阶段的定向编译和静态检查，记录任何与计划文件范围或实现顺序的偏差。代码完成后等待人工确认，再进入 Test 阶段执行正式验证，不在 Build 阶段用主线历史结果替代 5.x 证据。

## Risks
- Spring Security 5.7 的 `@EnableGlobalMethodSecurity` 使用旧方法安全拦截链；Advisor order 若错误，可能让 `@PreAuthorize`/PMS 与 CheckOwner 的组合顺序变化，甚至在前置拒绝前执行方法。计划通过正式自动配置、JDK/CGLIB、接口/实现和方法计数测试证明，而不是假定主线 order 数值可直接复用。
- Spring 5.3 的 `MergedAnnotation`、桥接方法和参数名发现行为可能与主线不同。必须从真正承载注解的方法建立表达式上下文，避免接口参数名与实现参数名不同时误拒绝。
- Java 8 语法改写容易改变空白字符串、TTL 边界、类型识别、嵌套容器和循环保护语义。每个改写点须由与主线等价的边界用例覆盖；凭证编码不得因回移而变化。
- 5.x 使用 Jackson 2 与 `javax.servlet`，主线使用 Jackson 3 与 Jakarta Servlet。只替换类型和异常 API，不改变 JSON 属性、Header/Query 或 HTTP 契约。
- `SecurityProperties.afterPropertiesSet()` 依赖 MVC HandlerMapping，自动配置单测必须使用合适的 Web 上下文并区分既有装配前提与新功能失败。
- 列表 DTO 实现接口即授权声明，错误业务过滤或跨用户共享可变 DTO 仍可能造成越权或竞态；框架在每次响应覆盖 PassKey，README 明确禁止未授权列表和跨用户共享 DTO。
- Security Starter 已依赖 Spring Web，但不能新增对 Macula Web Starter 的生产依赖；否则会改变下游 classpath 和模块边界。组合测试依赖严格限定为 test scope。
- 回滚应为单个回移 PR 的 revert；默认关闭保证未配置应用不签发 PassKey，但新增 `@CheckOwner` 的业务方法依赖该安全能力，不能靠关闭开关静默放行。

## Proof
- `PasskeyResourcesTest`：Java 8 下覆盖 Collection、PageVO、成功/失败 Result、嵌套与混合列表、空列表、单对象、循环/深度保护；`ResultModelJsonTest` 证明 Jackson 2 隐藏 `resourceId` 并输出 `passkey`。
- `PasskeyServiceTest`：固定 Clock 覆盖确定性 token、正确签发/验证、篡改、换用户、换编号、到期精确边界、非法 Base64/短密钥/非正 TTL、错误格式、超长输入、编码歧义边界和并发使用；从主线以固定密钥、Clock、用户和资源编号生成 golden token 向量，断言 5.x 生成值完全一致且能够验证该向量，防止签发与验证同时偏离主线格式；确认异常和 token 不泄漏密钥。
- `PasskeyRequestResolverTest`：Header、URL Query、同值双来源成功；重复、冲突、非法编码、表单同名、缺少请求和缺少凭证拒绝。
- `CheckOwnerTest`：returnObject 后置兼容；`#p0`、`#a0`、命名参数和嵌套属性前置验证；混合/未知/字面量伪装拒绝；匿名、关闭开关、缺服务拒绝；携带 `ROLE_ADMIN`、`ROLE_ROOT` 时错误拥有者和错误 PassKey 仍拒绝且前置方法不执行；JDK/CGLIB、接口/实现、泛型桥接和 PMS 组合。前置拒绝方法计数为零，后置拒绝查询计数增加但结果不返回。
- `SecurityAutoConfigurationTest`：Boot 2.7 默认关闭、显式关闭、合法/非法启用、默认服务与 Clock 条件装配、用户服务/Clock 替换、基础设施 Advisor 角色与真实调用行为。
- `ControllerResponseAdviceTest` 与 `WebAutoConfigurationTest`：Jackson 2/MockMvc 覆盖所有列表包装、混合元素、单对象、Result/ResponseEntity、Feign/`@NotControllerResponseAdvice`、String 响应、关闭且存在自定义 issuer、启用但缺 issuer、旧构造和属性绑定。
- `PasskeyMvcTest`：真实 CheckOwner Advisor、Passkey 服务、两个 Advice 联合覆盖列表签发到不同详情 DTO、PathVariable/RequestBody、Header/Query、PMS、写计数、HTTP 403/A0312、普通异常兼容及 tokenless/错误 token 拒绝。
- Build 阶段先执行新增测试的 `test-compile` 和变更 Java Checkstyle；Test 阶段依次执行单类、Security/Web 目标模块 `-am test`、全仓 `mvn test`、`mvn clean verify`、精确变更 Java Checkstyle 与 `git diff --check`，并记录 Java 8 版本、测试总数和任何外部服务跳过项。

## Implementation record
- 已按计划顺序完成 Commons 公共契约、HMAC 签发验证、请求提取、统一 CheckOwner Advisor、Security/Web 自动配置和 Advice 接入、测试源码及三个模块 README。
- 主线行为采用手工语义回移：Java 17 模式匹配、record、`String.isBlank`、Jakarta Servlet、Jackson 3 和新方法安全 API 分别改写为 Java 8 POJO/显式类型、Spring `StringUtils`、`javax.servlet`、Jackson 2 和 Spring Security 5.7 `@EnableGlobalMethodSecurity` 兼容实现；token 字段编码保持不变。
- 初始实施文件范围无偏差；Test 阶段首轮定向测试发现 Spring Security 5.7 的 CheckOwner Advisor 实际位于旧方法安全 Advisor 之前，与计划要求的 PMS 先执行不符。计划新增 `ResourceServerConfiguration.java`，将 `@EnableGlobalMethodSecurity` 的 order 设为 `Ordered.LOWEST_PRECEDENCE - 1`，CheckOwner 保持最低优先级；测试配置同步相同顺序。这是实现既定顺序的 5.x 兼容修正，不改变外部契约。现有 Security/Web `AutoConfiguration.imports` 已核对，仍指向被修改的原配置类，无需变更。
- Build 阶段使用真实 Tencent Kona JDK 8u472 执行目标模块及依赖的 `clean test-compile -DskipTests`，BUILD SUCCESS；随后 JDK 8 增量 `test-compile` 成功。这里只证明源码与测试源码可由 JDK 8 编译，不作为正式测试通过证据。
- 本次全部变更 Java 文件的 Checkstyle 为 0 violations，`git diff --check` 无输出。尚未运行测试；正式定向、模块、全仓和 verify 验证留给获得人工实现确认后的 Test 阶段。
- Test 阶段全仓运行暴露既有 `UserSeqMapperTest` 与 `UserMapperTest` 复用 H2 上下文、前者残留两行数据导致后者失败。用户于 2026-11-02 明确授权在同一 PR 中修复；在 `UserSeqMapperTest` 完成后标记上下文为 dirty，使后续测试重建数据库，不改变 MyBatis 生产代码或业务契约。
