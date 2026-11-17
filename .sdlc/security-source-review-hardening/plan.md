# Plan: 源码安全审查问题修复 (from spec.md 2026-11-17)
Status: accepted.

## Files that change
全面验证补充：移除固定 Secret 后，集成测试必须显式提供测试值（不修改生产配置）：
- `macula-boot-starters/macula-boot-starter-jpa/src/test/java/dev/macula/boot/starter/jpa/test/BlogRepositoryIT.java`
- `macula-boot-starters/macula-boot-starter-auditlog/src/test/java/dev/macula/boot/starter/auditlog/test/AuditLogAspectIT.java`
复查修复补充（Rain 在逐项确认方案后明确回复“OK，统一修复一下”，授权实现）：
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/AddJwtGlobalFilter.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/KongApiGlobalFilter.java`（仅保留验签应用内部属性，不改 invoker 语义）
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/constants/GatewayConstants.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/AddJwtGlobalFilterTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/KongApiGlobalFilterTest.java`
以下为原计划修改或新增的路径；加上上述复查修复补充构成最终清单。没有删除文件，不修改 POM、依赖版本、自动配置 imports、现有 jose.jks 或 SecurityInnerAspect。
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/main/java/dev/macula/boot/starter/mp/handler/MyDataPermissionHandler.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/main/java/dev/macula/boot/starter/mp/config/MyBatisPlusAutoConfiguration.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/main/java/dev/macula/boot/starter/mp/utils/SecurityUtils.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/handler/MyDataPermissionHandlerTest.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/config/MyBatisPlusAutoConfigurationTest.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/utils/SecurityUtilsTest.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/test/MyDataPermissionMysqlIT.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/README.md`
- `macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/filter/TenantFilter.java`
- `macula-boot-starters/macula-boot-starter-web/src/test/java/dev/macula/boot/starter/web/filter/TenantFilterTest.java`
- `macula-boot-starters/macula-boot-starter-web/README.md`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/config/GatewayProperties.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/config/JwtConfiguration.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/config/GatewayAutoConfiguration.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/security/ResourceServerConfiguration.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/security/CachingReactiveOpaqueTokenIntrospector.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/BodyGlobalFilter.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/CryptoGlobalFilter.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/config/JwtConfigurationTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/config/GatewayPropertiesTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/security/CachingReactiveOpaqueTokenIntrospectorTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/BodyGlobalFilterTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/CryptoGlobalFilterTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/README.md`
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/config/SecurityProperties.java`
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/config/ResourceServerConfiguration.java`
- `macula-boot-starters/macula-boot-starter-security/src/test/java/dev/macula/boot/starter/security/test/ResourceServerJwtConfigurationTest.java`
- `macula-boot-starters/macula-boot-starter-security/README.md`
- `macula-boot-examples/macula-example-alibaba-gateway/src/main/resources/application.yml`
- `macula-boot-examples/macula-example-alibaba-gateway/README.md`
- `macula-boot-examples/macula-example-tencent-gateway/src/main/resources/application.yml`
- `macula-boot-examples/macula-example-tencent-gateway/README.md`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-gateway/src/main/resources/application.yml`
- `macula-boot-archetype/src/main/resources/archetype-resources/__rootArtifactId__-gateway/README.md`

- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/main/java/dev/macula/boot/starter/mp/interceptor/DataPermissionParameterContext.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/main/java/dev/macula/boot/starter/mp/interceptor/ParameterizedDataPermissionInterceptor.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/interceptor/ParameterizedDataPermissionInterceptorTest.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/test/DataPermissionDatabaseIT.java`

## Order of work
1. 先补充各修复点的失败回归：SQL 注入/OR、非 ROOT 请求租户、请求上下文清理、缓存过期、请求体超限及日志；配置测试同时固定网关旧密钥兼容与自定义 Bean 回退行为。仅使用既有 JUnit、Mockito、Spring Test、JSqlParser、MySQL JDBC 和 JDK KeyStore 工具，不新增依赖。
2. 修改 MyDataPermissionHandler，用 AST 参数表达式代替原始用户名拼接，由 ParameterizedDataPermissionInterceptor 同步绑定 BoundSql 参数；部门范围保留原 SQL 子查询，改用参数化等值/LIKE。校验注解列名/别名，空用户名使用恒假条件，分别括起原 WHERE 和权限条件。保留公共签名、ROOT 豁免、ALL/缺失/DEFAULT 行为；非法范围显式报错而不误转 ALL。
3. 在 MyBatisPlusAutoConfiguration 内仅允许 SecurityUtils.isRoot() 的身份采用请求上下文租户，其他身份沿用认证租户和默认回退。SecurityUtils.isRoot() 同时要求 Authentication 已认证且不是匿名身份，避免未认证对象仅凭 ROLE_ROOT 获得切换资格。TenantFilter 的 finally 覆盖租户读取、转换和下游调用，确保正常、异常及非法租户值退出均清理本次上下文；不将角色判断移到 Filter。
4. GatewayProperties 新增 JWT KeyStore 配置和正数 DataSize 请求体上限。JwtConfiguration 保留原默认资源/别名/密码及 RS256，按配置加载 file/classpath 的 RSA 私钥，显式配置失败不回退；加载默认公开材料时仅提示一次 WARN。自定义 JWKSource/JwtEncoder 路径跳过默认材料校验，避免发布不匹配的默认 JWK；新增配置对象不生成含密码的 toString。GatewayAutoConfiguration 注入请求体上限，保留相关 Bean 名和自动配置发现入口；现有公开无参构造或方法保持可调用并委托默认值。
5. SecurityProperties 移除固定默认 HMAC Secret；ResourceServerConfiguration 仅在选择对称密钥验签模式时检查非空且 UTF-8 至少 32 字节。已有 JWK、issuer、公钥路径及自定义 JwtDecoder 不受该校验干扰，缺少所有有效验证来源时启动失败，异常不包含 Secret。
6. 提取 CachingReactiveOpaqueTokenIntrospector，接收既有内省 delegate、RedisTemplate 和 Clock，配置层委托该实现并保留原 Bean/公开构造契约。缓存命中先核验 exp，过期缓存删除后重新内省；非法 exp 拒绝认证；delegate 返回身份也校验 exp。按 Duration 写入剩余 TTL，无 exp 时最多缓存 5 分钟；不改键、序列化和撤销协议。生产使用 UTC Clock，单元测试固定 Clock；阻塞 Redis 操作调度到 boundedElastic，避免响应式事件循环被阻塞。
7. BodyGlobalFilter 保持原分支、缓存命中短路和 then 链调用，仅为原 join 添加实际聚合字节上限，超限在 then 之前转成标准 413 异常。保留原链构造时机，错误和取消释放缓冲区；移除敏感请求体日志，保留不含数据的 debug 提示。
8. 更新四个 Starter 的 README，以及 Alibaba/Tencent Gateway 和 Archetype Gateway 的 YAML 注释与 README：展示可选 JWT 外部配置和请求体上限，不向 YAML 写入默认密码、不使旧部署必须新增配置，说明资源服务器 Secret 的独立迁移要求及 ROOT 租户规则。
9. 运行最小回归后扩大到四个目标模块及其依赖，再执行跨模块 mvn test。MySQL 集成测试只通过专用测试环境参数运行，不读业务数据源配置，在独立随机命名临时表上验证，finally 删除测试表。记录真实测试结果、未执行项和实际偏离；通过后交给 sdlc-test 正式验证，不提交、推送或部署。

## Risks
- 网关默认公开私钥按人工要求保留，配置能力不消除默认材料风险；旧签发密钥、JWKS 和默认验签关系必须维持，自定义编码器不应误发布默认公钥。
- 移除资源服务器默认 Secret 是已接受的兼容性变化；仅依赖旧 Secret 的部署须先显式配置验证来源。回滚将重新引入默认 Secret 风险，不能把回滚视为安全完成。
- 非 ROOT 的旧请求租户切换不再生效；请求上下文退出即清理，测试需在下游链内部断言租户可见性。其他代码手工设置 TenantContextHolder 不因此获得跨租户授权。
- JDBC 参数绑定与默认部门标准比较仍需在目标数据库验证；H2 兼容模式不能代替厂商数据库。保持对原列直接比较，不引入字符集转换函数，无需数据迁移。
- 1 MiB 上限仅约束现有缓存分支，上传或较大签名请求需调整配置；取消、异常和池化 DataBuffer 的释放必须覆盖，不能误处理下游同类型异常。
- 现有 exp 的缓存类型需保持兼容：接受正常 Instant，明确检查非法类型；没有 exp 的身份仍有 5 分钟缓存窗口，本次不扩大撤销协议或内部 JWT 生命周期。
- 回滚以代码版本回退为边界，不修改真实密钥、数据库、缓存或部署；回退前评估越权、过期认证及无上限缓存重新出现的风险。新增配置默认兼容，但资源服务器 Secret 需保留显式值。
- 计划制定阶段没有源码写入；Rain 接受 plan 后已按批准范围实施。文件集合或方案实质变化时更新计划并说明，不能自动扩大范围。

## Proof
- spec 1–3：MyDataPermissionHandlerTest 覆盖 AST 结构、恶意/Unicode/引号/反斜杠用户名、空值、无 WHERE、OR/嵌套条件，以及 ROOT/ALL/缺失/DEFAULT/非法范围。MyDataPermissionMysqlIT 在独立 MySQL 验证生成 SQL 的真实结果集和字符比较，覆盖默认模式、ANSI_QUOTES、NO_BACKSLASH_ESCAPES。
- spec 4：MyBatisPlusAutoConfigurationTest 与 SecurityUtilsTest 覆盖已认证 ROOT、非 ROOT、匿名、未认证且伪带 ROLE_ROOT、无身份、认证租户回退、无请求租户、插件关闭和表名后缀；TenantFilterTest 在链内部断言并检查正常/异常/非法输入退出后的清理。
- spec 5–6：JwtConfigurationTest/GatewayPropertiesTest 验证默认旧密钥、公钥验签、有效外部 JKS/PKCS12、错误资源/密码/别名/非 RSA 私钥不回退、file/classpath 限制、密码不出现在日志/toString、属性绑定及自定义 JWKSource/JwtEncoder 回退；使用 JDK keytool 在测试临时目录生成材料，不提交私钥。ResourceServerJwtConfigurationTest 覆盖有效 Secret、空白/不足 32 字节、非 ASCII 字节长度、JWK/issuer/公钥/自定义 JwtDecoder 路径。
- spec 7：CachingReactiveOpaqueTokenIntrospectorTest 使用固定 Clock、模拟 Redis 和内省 delegate，覆盖命中过期/刚好到期/非法类型、重新内省成功与失败、delegate 返回过期身份、剩余不足一分钟、无 iat/无 exp，并验证实际写入 Duration 及未改变缓存键和 principal 结构。
- spec 8–9：BodyGlobalFilterTest 固定原链构造/订阅时机、任意非 null 缓存短路和方法/协议分支；覆盖新聚合上限、空体、未知长度、声明长度不改变实际聚合语义、缓冲释放及下游错误原样传播。CryptoGlobalFilterTest 验证正常加解密和日志不暴露密钥/明文。
- spec 10：核对模块 README、三套 YAML/README 的配置名称及默认值，检查 Archetype 占位符和现有资源路径；检查无私钥增删、无 invoker/@Inner 修改、无 POM 或自动配置 imports 变更。
- 命令从目标模块 -pl/-am test 开始，随后跨模块 mvn test；独立 MySQL 可用时运行指定 MyDataPermissionMysqlIT，环境不满足则明确保留未验证项。按仓库规则对变更 Java 执行 Checkstyle，运行 git diff --check，记录 Surefire/Failsafe 的真实结果。计划制定时未运行这些测试；实施阶段实际结果见下文。


## Implementation notes
- 2026-11-17：按修复点分批先补失败回归再实现，便于定位问题，不改变工作依赖顺序。
- 首轮实现因 JSqlParser 解析限制曾改用 CONVERT；该尝试已被 Rain 后续要求的 JDBC 参数绑定修订替代，当前生产代码已移除 CONVERT 和 find_in_set。
- 仓库使用 Mockito subclass MockMaker，不支持 static mock；回归使用真实 SecurityContext 和 BearerTokenAuthentication，不修改测试基础设施。

## 首轮 Build 结果（可移植性修订前）
- Rain 已回复 YES 接受计划；实施文件与 Files that change 的 34 项完全一致，无计划外源码文件、删除文件、POM/版本/imports 变更或真实环境操作。
- 保留全部旧 jose.jks；KongApiGlobalFilter、SecurityInnerAspect 和缺失/DEFAULT dataScope 策略未修改。网关可选 JWT 配置和旧默认兼容、资源服务器显式 HMAC Secret、ROOT 租户切换、Token 过期、请求体上限及日志修复均已实现。
- 先运行的 MyDataPermissionHandlerTest 在旧实现上失败，证实用户名直接拼接、OR 未分组和非法范围异常行为；测试最初因 Mockito static mock 不支持产生错误，改为真实 SecurityContext 后才作为缺陷回归证据。
- 聚焦回归命令：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway,macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-mybatis-plus -am test -Dtest=JwtConfigurationTest,GatewayPropertiesTest,CachingReactiveOpaqueTokenIntrospectorTest,BodyGlobalFilterTest,CryptoGlobalFilterTest,ResourceServerJwtConfigurationTest,MyDataPermissionHandlerTest,MyBatisPlusAutoConfigurationTest,SecurityUtilsTest,TenantFilterTest -Dsurefire.failIfNoSpecifiedTests=false`：41 tests，0 failures/errors/skips。随后增加部门树/ROOT、空流/上游异常、公钥-only/非 RSA 条目边界测试。
- 四个相关模块及依赖的完整 `-pl/-am test`：189 tests，0 failures/errors/skips（补充上述边界测试前）。
- 最终跨模块 `mvn test`：363 tests，0 failures/errors/skips，90 个测试类，BUILD SUCCESS；包含新增边界测试。日志 `/tmp/macula-security-all.log`。最初一次全仓尝试因新增测试的 principal 强转缺失而编译失败，修正后完整重跑得到该结果。
- 指定集成命令：`mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am verify -Dtest=NoSecurityUnitTestSelected -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=MyDataPermissionMysqlIT -Dfailsafe.failIfNoSpecifiedTests=false`：1 test，0 failures/errors，1 skipped；未设置独立测试 URL，不能视为 MySQL 真实 SQL mode/字符集验证通过。日志 `/tmp/macula-security-mysql-it.log`。
- 24 个变更 Java 文件的 Checkstyle：0 violations；`git diff --check` 通过。自动配置 imports 和依赖不变，配置测试已覆盖默认与自定义 Bean。
- 未执行依赖业务 Redis/MySQL/云服务的全仓 `clean verify`、真实环境冒烟、密钥轮换或线上验证；没有可用独立测试 MySQL 配置。SQL 的真实数据库兼容仍待专用环境验证。
- 当前处于 Build 完成待人工确认；本记录是已运行检查的事实，不代表自批准 sdlc-test、代码审查或部署 gate。

## 可移植性修订历史（额外 JDBC 策略已被下文简化方案撤销）
- 授权来源：Rain 针对 CONVERT 和 find_in_set 的数据库依赖明确回复“改改”；在既有已接受计划上实施此修订，不重新启动需求访谈。其余已完成修复保持原范围。
- 实施顺序：先补充绑定顺序和部门范围失败回归；新增可覆盖的 DepartmentDataScopeProvider 及 JDBC 默认实现；新增支持 SELECT 和 UPDATE/DELETE 的 ParameterizedDataPermissionInterceptor 与可嵌套、finally 恢复的参数上下文；修改 handler/自动配置使用它们；将旧十六进制及 MySQL 测试改为实际 JDBC 参数执行；更新 README 并执行单元及 H2 集成测试，再运行全仓单元测试。
- 原理：权限值暂表示为唯一命名参数；在最终 AST 反解析时，按输出顺序将其改为问号，交织原 ParameterMapping 并设置 additionalParameters。校验原参数和新增参数均完整消费，异常不留下线程上下文。别名/注解规则、OR 分组、ROOT 和 DEFAULT 策略不变。
- 默认部门策略：绑定等值与四种逗号边界比较，返回与原 sys_dept/find_in_set 语义一致的 ID；接口位于 scope，实现在 scope/impl。空结果拒绝，非法 ID 拒绝；大范围 IN 按每组 500 个参数拆分 OR，目标数据库总参数限制仍适用。
- 自动配置提供可替换策略 Bean，查询时才解析 JdbcTemplate，关闭数据权限时不创建默认策略。保留原工厂方法的手工调用入口，手工调用有子部门需求时应注入策略；缺少必要策略或参数上下文均明确失败。
- 验证：覆盖原参数在 SELECT/WHERE/ORDER/LIMIT 及 UNION、子查询和写入 SET 中的位置；新增值保持原 Java 类型；嵌套/异常/并发上下文隔离；默认树路径的精确分隔匹配及可替换/禁用 Bean。DataPermissionDatabaseIT 使用内存 H2 多模式运行实际 MyBatis 查询与写入，不连接业务库；外部 MySQL 仍单独报告。
- 风险：提供标准 SQL 和 JDBC 参数机制不等于所有数据库、所有 SQL 语法或无限参数量都已验证；未覆盖语法中参数不能完整消费时失败，避免错误绑定。原静态 helper 的 SELF/DEPT_AND_SUB 需要拦截器上下文，README 明确迁移方式。

## 可移植性首版结果（额外 JDBC 策略已撤销）
- 生产代码已移除 CONVERT、utf8mb4 和 find_in_set；SELF/DEPT 的值及 DEPT_AND_SUB 的 ID 集合使用 JDBC 参数。新增策略接口、默认 JDBC 实现、参数上下文和拦截器，自动配置已接入。
- 实际文件集合为计划内 40 项（首轮 34 + 修订 6），没有计划外实现文件、POM/依赖/imports 变化；原有网关兼容行为、ROOT 租户限制和其他安全修复保持不变。
- 实施顺序偏离：本补充先实现参数化 API 并完成编译验证，再更新旧用例并补齐绑定/数据库回归，未记录新增用例在旧版的红状态；原因是先确认当前 JSqlParser 和 MyBatis API 的组合方式。最终证明依赖实际参数序列断言及真实 MyBatis 查询/写入结果，不宣称未运行的红绿步骤。
- 目标模块命令 `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am verify -Dit.test=DataPermissionDatabaseIT,MyDataPermissionMysqlIT -Dfailsafe.failIfNoSpecifiedTests=false`：52 个单元测试通过；IT 共 2 个，H2 通过 1 个、外部 MySQL 因无专用 URL 跳过 1 个，0 failures/errors。日志 `/tmp/macula-portable-verify.log`。
- H2 IT 在默认、MySQL、PostgreSQL、Oracle、MSSQLServer 五种模式执行同一套真实 MyBatis 查询/写入；覆盖危险及 Unicode 用户名、完整逗号边界、UNION、子查询、foreach、更新/删除，默认模式另覆盖分页 count 与分页参数。兼容模式不等同于真实厂商数据库验证。
- 补充单元测试覆盖 SELECT/WHERE/ORDER BY/LIMIT 参数交织、空原映射、嵌套与并发上下文、参数数量不匹配拒绝、大范围分组及默认策略替换/禁用。
- 最终全仓 `mvn test`：373 tests，0 failures/errors/skips，BUILD SUCCESS。日志 `/tmp/macula-portable-all.log`。
- 30 个变更 Java 文件的 Checkstyle：0 violations；`git diff --check` 通过。日志 `/tmp/macula-portable-style.log`。
- 手工接入原生数据权限插件的应用需迁移到 ParameterizedDataPermissionInterceptor；自动配置使用方无需修改。README 已记录迁移和目标数据库总参数数量限制。
- 未连接业务数据库、未轮换密钥、未提交/推送/部署。当前 Build 等待 Rain 确认；未自批准 Test 或代码审查 gate。

## User-requested simplification
- Rain 明确反对为 DataPermission 新增 JDBC 部门查询；本轮按此纠正过度设计。
- 删除本次新增且尚未提交的 DepartmentDataScopeProvider / JdbcDepartmentDataScopeProvider，撤掉自动配置中的 JdbcTemplate、策略 Bean、延迟查询及额外工厂重载。
- 保留原 sys_dept/tree_path 子查询，把 find_in_set 改为绑定参数的等值/LIKE；不额外查询、不展开部门 ID 集合。SELF 和部门参数继续由 MyBatis BoundSql 绑定，保留 OR 分组、ROOT/DEFAULT 策略。
- 参数上下文与拦截器改为无策略参数构造，更新现有测试和 README；原两项策略源文件从最终 Files that change 删除，不保留废弃抽象。H2 五模式继续验证真实子查询、分页和写操作，检查无额外 JDBC 查询或策略装配。

## Simplification review
- Plan 第三轮仅审用户要求的简化补充，sdlc-reviewer 返回 No findings，未批准任何人工 gate；累计 Plan 三轮，到达本轮预算上限，不继续重复审查。
- 最终删除的两项只是本次新增且尚未提交的策略源码，未删除用户原有文件。原自动配置工厂签名恢复，保留 MyBatis 参数化拦截器，不引入新的数据源访问。

## Simplified build result
- 已删除额外部门策略接口、JdbcTemplate 实现及 Bean 装配；MyBatisPlusAutoConfiguration 恢复原单一工厂签名，仅装配无参 ParameterizedDataPermissionInterceptor。生产源码已无 JdbcTemplate/JdbcOperations/DepartmentDataScopeProvider 引用。
- DEPT_AND_SUB 保留业务 SQL 内的 sys_dept 子查询，使用 5 个固定绑定值处理部门 ID 与逗号边界；没有预查部门、ID 集合展开、find_in_set 或 CONVERT。
- 相应更新参数上下文、测试和 README。最终实现文件 38 项，均在最终 Files that change 内；没有删除基线已有文件，没有计划外实现修改。
- 全仓 `mvn test`：373 tests，0 failures/errors/skips，BUILD SUCCESS；日志 `/tmp/macula-permission-simple-all.log`。
- `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am clean verify -Dit.test=DataPermissionDatabaseIT,MyDataPermissionMysqlIT -Dfailsafe.failIfNoSpecifiedTests=false`：成功。H2 在五种兼容模式执行查询/子查询、动态参数、分页和更新/删除；外部 MySQL 因缺少独立 URL 跳过 1 项，不能视作厂商数据库验证通过。日志 `/tmp/macula-permission-simple-clean.log`。
- 28 个变更 Java 文件 Checkstyle 0 violations，git diff --check 通过；清理重建后的主 JAR 不含已撤销的部门策略类或 mp/scope 目录。
- 未提交、推送、部署或修改真实数据库；保持其他已完成安全修复，等待人工确认 Build 完整性。

## BodyGlobalFilter user correction
- Rain 指出原过滤器逻辑被改变。恢复原来的双层方法/协议分支、Object 缓存命中即跳过、defaultIfEmpty/doOnNext/then(chain.filter(exchange))，撤销外层 Mono.defer、flatMap、Content-Length 预判及对已有缓存的类型/大小检查。
- 在原 join 上添加 maxBytes 参数，并在 then 之前仅将聚合的 DataBufferLimitException 转成 ResponseStatusException(413)；保持下游错误和链构造时机，不再在过滤器中额外改写超限响应控制流。
- 回归测试明确区分 chain.filter 构造与返回 Mono 的订阅：构造时缓存尚未填充，订阅时缓存已完成。覆盖缓存对象保持原样、普通协议分支、已知/未知长度、空体、取消、释放和下游异常；只验证相应模块，不额外扩大实现范围。

## BodyGlobalFilter correction result
- 已从 HEAD 原实现恢复 filter 的分支结构、Object 缓存非 null 即直接传递、defaultIfEmpty/doOnNext/then、debug 起始提示和原过滤顺序；撤销 defer/flatMap、Content-Length 预判、已缓存对象转换与限额以及 tooLarge 响应控制流。
- 当前增量仅为配置大小的构造入口、join(body, maxBytes)、局部缓冲 finally 释放、聚合异常映射标准 ResponseStatusException(413) 和敏感 body 日志删除。
- 新增回归明确固定原链构造时机：调用 filter 时已构造下游 Mono，此时尚未缓存；聚合成功后才订阅下游。聚合超限不会订阅已构造的下游 Mono，下游同类型异常不会被本次聚合映射捕获。
- 验证命令：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test`，125 tests，0 failures/errors/skips，BUILD SUCCESS；其中 BodyGlobalFilterTest 8 项通过。日志 `/tmp/macula-body-minimal-module.log`。
- 当前两个变更 Java 文件 Checkstyle 0 violations，git diff --check 通过。本轮未重复全仓测试，之前的全仓 373 结果仅对应本次 BodyGlobalFilter 纠正前版本，不能混称本次验证。
- 用户纠正直接收窄原实现范围；没有增加依赖、文件、额外响应控制流或其他功能，未自批准 Test 或部署 gate。

## Test recheck result
- Rain 回复 OK 后要求检查兼容性与剩余漏洞，本轮按 sdlc-test 复查；完整记录见 verification.md。
- 全仓单元测试 375 项通过，H2 五种兼容模式及实际 HTTP 加解密链通过；真实 MySQL、Redis 回调集成环境缺失。
- 额外探针确认两个新增兼容性回归（TOP/层级 SQL 参数、依赖默认 JWKSource 的自定义 JwtEncoder）及一个原有 JWT 缓存身份混用漏洞。
- 本轮未改业务源码；测试结论未通过，尚未进入 PR/部署 gate。

## Accepted recheck fixes
- 顺序：先补三个缺陷的回归并确认失败；补齐 TOP、Oracle 层级表达式遍历；使用 v2 派生 JWT 缓存键绑定认证来源、认证应用、租户、最终主体、issuer、jti；恢复 JWKSource 与 JwtEncoder 各自独立的缺失条件；更新 README 并运行受影响测试及全仓单元测试。
- HMAC 验签应用名只通过服务端 exchange 属性传递，缓存不得仅绑定 invoker 覆盖后的主体。JWT claims、签名算法、有效期、invoker 行为保持原样；opaque 缓存键和撤销调用入口保持原样，后者继续调用公共 buildKey。
- v2 各字段使用长度前缀防止分隔符碰撞，缺失与空字符串区分；不回读旧键，不清理实际 Redis。滚动部署中旧实例仍有原风险，应统一升级网关实例。
- 自定义编码器仍可依赖默认 JWKSource；仅自定义 encoder 不再抑制默认 source，因此无效的显式 KeyStore 配置仍会阻止启动，与旧装配一致。
- SQL 保留完整消费校验，不引入 JDBC 查询；测试覆盖 TOP 括号/PERCENT/WITH TIES、层级顺序/NOCYCLE、WHERE/ORDER 参数交织、真实 H2 TOP 执行。
- 此为用户明确授权的缺陷修复补充，沿用既有计划；不重复超过三轮的 Plan review，不自批准后续 gate。

## Recheck fix build result
- 三项已按上述补充实现。额外细节：Oracle 默认比较表达式 deparser 会丢弃 PRIOR，回归发现后在同一参数上下文中补齐位置保留，保持查询语义。
- 先执行红状态回归（SQL 两项失败；缓存及 encoder 复现失败），补丁后目标模块 148 项、全仓 380 项单元测试通过；H2 五模式含 TOP 实际执行、HTTP 加解密 7 项通过，真实 MySQL/Redis 回调各跳过 1 项。33 个变更 Java 文件 Checkstyle 零违规，git diff --check 通过。
- 原漏洞探针现为 JWT_SUB_FORWARDED_AFTER_A=appA；encoder 启动成功；TOP/层级参数改写成功。证据与完整命令见 verification.md 末节。
- 修改集中于上述授权文件。HMAC 仅新增内部验签应用属性、失败前清理属性，不改变 claims 或 invoker；没有新增数据库访问、依赖或真实缓存操作。
- 实现完成，等待人工确认；未自批准 Test/PR/部署 gate。

## User correction: documentation scope and body default
- Rain 要求 JWT 默认材料兼容与环境变量说明只集中在 Gateway Starter README；删除示例/Archetype README 和 YAML 中的重复 JWT 段落，不改变配置行为。
- Rain 指定 maxCachedBodySize 默认 10 MiB；更新 GatewayProperties、BodyGlobalFilter 无参入口、已有默认值断言和相关说明。无参入口复用配置属性默认值，避免两处常量漂移，原过滤器控制流保持不变。
- 本补充只涉及既有 Files that change 中的文件，按用户明确纠正实施；验证使用 GatewayPropertiesTest、BodyGlobalFilterTest、变更 Java Checkstyle 和 git diff --check。

## User correction: restore original JWT cache
- Rain 认为 AddJwtGlobalFilter 改动复杂并要求先恢复原版本；将 AddJwtGlobalFilter、KongApiGlobalFilter、GatewayConstants 及对应两个测试恢复为 HEAD 原内容，已核对差异全部属于本轮缓存修复。
- 撤销 README 中 v2 缓存键说明。之前的修复/验证记录保留为历史，不能视为当前缓存漏洞已解决；同租户 jti 与应用名碰撞仍是已知问题。
- 保留 SQL 参数遍历修复、source/encoder 独立条件、请求体 10 MiB 等其他已接受修改。没有修改 Redis 数据或执行远程操作。

## User-selected cache key prefixes
- Rain 提出 JWT_ID 分支在 prefix 后加 id:、name 分支加 u:，并明确不同应用网关使用不同缓存。本次按该部署边界收敛，只区分键类型，不再增加应用字段、摘要或 v2 元组。
- 保留 API Key 前缀和租户段，生成 prefix + id:/u: + tenant + 原值；不回读旧未分类键，旧值自然过期，不操作实际 Redis。
- 修改 AddJwtGlobalFilter 两个返回分支、注释、对应旧断言及相同值跨类型回归，补充 Gateway Starter README 中简短迁移说明。使用 AddJwtGlobalFilterTest、RmOpaqueTokenEndpointFilterTest 验证当前键生成和撤销调用。
- 实际结果：新增碰撞回归先红后绿，12 项相关测试通过，Checkstyle 零违规，git diff --check 通过。用户已有 tenant 变量更名保持；未改 KongApiGlobalFilter、共享常量或其他认证逻辑。
