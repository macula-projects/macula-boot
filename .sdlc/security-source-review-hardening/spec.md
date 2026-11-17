# Spec: 源码安全审查问题修复 (from intent.md 2026-11-17)
Status: accepted.

## Source intent
[intent.md](intent.md) 已由 Rain 在会话中回复 YES 接受；请求租户切换资格明确为 ROOT。Rain 后续明确第五点兼容优先，网关未配置签名材料时保留旧默认行为。

## Requirements
1. SELF 权限中的用户名始终通过 JDBC 参数绑定参与比较，单引号、双引号、反斜杠、中文及 SQL 片段均不能改变查询结构；空或缺失用户名不得匹配其他用户记录。
2. 追加数据权限时，既有条件与权限条件分别分组，保证 `(原条件) AND (权限条件)`，覆盖含 OR、嵌套表达式及无 WHERE 的查询。
3. 保留 ALL、缺失或 DEFAULT dataScope 的现有放行语义；非法范围不得误转为 ALL，不将本次工作扩展为数据权限策略收紧。
4. 在 MyBatisPlusAutoConfiguration 中复用 SecurityUtils.isRoot()：只有 ROOT 可以采用 TenantContextHolder 中的请求租户；非 ROOT 忽略该值，继续使用认证租户及既有默认租户规则。关闭租户插件、表名后缀规则及 ROOT 的数据权限豁免保持现状。
5. 网关签名私钥支持由 macula.gateway.jwt 配置；未配置时使用现有 classpath:jwk/jose.jks、jose 别名及原密码，保证旧部署保持原签发密钥和验证关系。显式配置无效时启动失败，不静默回退。自定义 JWKSource 或 JwtEncoder 保持可覆盖，不生成临时随机私钥。
6. 资源服务器不再使用固定默认 HMAC Secret；未提供有效 JWT 验证来源且无自定义 JwtDecoder 时启动失败。显式 Secret 至少为 32 字节，不影响已配置 JWK、issuer 或公钥路径的验证模式。
7. 网关对缓存命中的 principal 检查 exp，过期或非法时间属性不接受；缓存 TTL 不超过从当前时间到 exp 的剩余时间。有 exp 但无 iat 时仍按 exp 处理；无 exp 时保留最多 5 分钟的缓存上限，不给无剩余有效期的 Token 写入缓存。
8. BodyGlobalFilter 的请求体缓存默认限制 1 MiB，配置必须为正且不得超过底层聚合 API 的容量；本过滤器新聚合的请求体按实际字节数受限，超限通过标准 ResponseStatusException 返回 HTTP 413，不订阅后续处理。未进入缓存分支的普通请求保持原行为。
9. 删除请求体、解密密钥、解密请求及响应明文日志，包括 INFO 与 DEBUG 路径；保留不含敏感值的诊断信息，密码配置不得进入自动生成的 toString 或异常消息。
10. 同步维护受影响模块 README、示例和 Archetype 配置，并提供对应回归测试及升级说明。

## Non-goals
不改变缺失 dataScope 的策略；不改造 @Inner；不改变 KongApiGlobalFilter 的 invoker 语义；不修改 IAM 身份契约、数据库结构、应用凭据或真实部署，不执行密钥轮换、提交、推送或发布。

## Design
- 适用政策：AGENTS.md、.agents/rules/architecture.md、starter-development.md、testing.md、REVIEW.md；bands.yaml 只约束后续监控，不授权部署。仓库 macula-ai-coding/SKILL.md 为空，没有额外规则；未发现额外组织安全或合规政策。保持 Java 17、既有包边界、中文 Javadoc 和可覆盖的自动配置。
- SQL（按 Rain 纠正后的简化方案）：SELF 使用 JDBC 占位符，数据权限拦截器在 BoundSql 中同步维护 ParameterMapping 和 additionalParameters；按最终 SQL 输出顺序交织原参数与权限参数。DEPT_AND_SUB 保留 sys_dept/tree_path 子查询，使用等值及完整逗号边界 LIKE；5 个条件值均由 MyBatis 绑定，不额外查询数据源，不展开部门 ID 集合。
- 租户：在 MyBatis 租户解析点检查 ROOT，不在 TenantFilter 中判断角色；ROOT 来自已认证 SecurityContext 的权限集合，不接受请求头自报角色。TenantFilter 在 finally 清理本次请求的租户上下文，包含下游异常路径。
- 签名配置：GatewayProperties 新增 Jwt 配置组；JwtConfiguration 按 Resource 加载本地文件或 classpath 的 RSA KeyStore，验证别名对应私钥。密钥配置只在创建默认签名组件时校验；自定义编码器无需提供默认 KeyStore，也不自动发布与其不匹配的默认 JWK。保留 RS256 和既有 claims/issuer 规则。
- 密钥兼容：保留现有 jose.jks 资源和原密码默认值，将默认值从 JwtConfiguration 的方法体移入网关配置属性；示例与 Archetype 保留无新增配置也能工作的默认行为，并说明如何通过环境变量覆盖。默认组件加载公开内置密钥时输出一次不含密码或密钥内容的 WARN，说明生产应配置独立签名材料；不自动轮换。新增测试的私有 KeyStore 在测试运行时创建。HTTPS 的 springboot.p12 不属于本次 JWT 签名资源范围。
- Token：以可控制的时间源计算剩余有效期，使用精确 Duration 写 Redis；过期缓存清除后交由标准内省流程确认，只有内省返回有效且未过期的身份才可接受。不修改缓存键、principal 序列化结构和现有撤销回调。
- 请求体（按 Rain 要求收敛）：保留原 POST/PUT/PATCH 及加密/HMAC 分支、Object 缓存非 null 即跳过、defaultIfEmpty/doOnNext/then(chain.filter(exchange)) 和链构造时机。仅为原 DataBufferUtils.join 增加实际字节上限，聚合的大小异常转换为标准 HTTP 413 异常；不额外读取 Content-Length，不检查或转换既有缓存对象，不捕获下游错误。

## Data and interfaces
- 新增 `macula.gateway.jwt.key-store`：Resource 位置，默认 classpath:jwk/jose.jks；仅允许 file/classpath 资源。
- 新增 `macula.gateway.jwt.key-store-type`：默认 JKS，支持显式 PKCS12。
- 新增 `macula.gateway.jwt.key-store-password`、`key-alias`、`key-password`：KeyStore 密码保留原默认值，别名默认 jose；key-password 未提供时使用 KeyStore 密码。所有值均可被外部配置覆盖，密码不出现在日志或配置对象 toString 中。
- 新增 `macula.gateway.max-cached-body-size`：DataSize，默认 1MB，即 1,048,576 字节；不支持负数或零表达无限制。
- 保留 `spring.security.oauth2.resourceserver.jwt.secret` 配置键，移除其固定默认值。
- 不新增部门策略 Bean 或数据源查询；沿用 sys_dept/tree_path 的现有表结构和同一条 SQL 子查询。
- 数据权限插件改用 `ParameterizedDataPermissionInterceptor`；手动接入原生插件的调用方需迁移，自动配置直接接入。
- 不新增数据库、Redis 键或认证 claims；不改变现有 HTTP 成功响应，新增请求体超限的 HTTP 413 边界。

## Flagged concerns
- 网关默认密钥风险：按 Rain 的兼容要求保留公开内置私钥，新增配置能力并不消除使用该私钥时的身份伪造风险；默认加载 WARN 和 README 明示生产替换建议。本次不强制迁移，non-blocking。
- 资源服务器 Secret：第六点仍移除固定默认 HMAC Secret，未配置其他验证来源的资源服务器需要显式补齐配置；这与第五点网关 RSA 签名材料兼容分别处理。Rain 已回复 YES 接受本 spec，resolved。
- 请求租户处理：非 ROOT 忽略请求租户并继续使用认证租户，而非返回 403；普通调用方的既有越权切换将失效。Rain 已回复 YES 接受该处理，resolved。
- 请求体上限：默认 1 MiB 可能影响较大签名或加密请求，部署者可调高；本限制不替代入口代理限额及并发防护。Rain 已回复 YES 接受默认值，resolved。
- 数据库边界：通用参数绑定与标准 LIKE 不再要求 MySQL 方言；仍须遵守目标数据库的参数数量限制及应用自己的部门模型。保留既有部门表结构与 SQL 子查询；H2 多模式不等同于真实 PostgreSQL/Oracle/SQL Server 验证，non-blocking。
- 剩余信任边界：@Inner、invoker、缺失 dataScope 策略按用户要求保留，其风险不因其他修复自动消失，non-blocking。
- 存量密钥与缓存：源码修复不撤销历史私钥信任，不清理实际环境 JWT 缓存，也不执行密钥轮换；部署者仍需安排迁移，non-blocking。
- 外部验证：真实 MySQL、Redis 和完整应用冒烟依赖可用环境；未执行的检查须明确报告，不能视为通过，non-blocking。

## Verification strategy
- SQL 回归覆盖恶意用户名、Unicode、引号和反斜杠、空值，含 OR/嵌套表达式及既有范围策略；真实 MyBatis/H2 五模式验证 JDBC 参数顺序、动态 foreach、UNION、子查询、分页 count、更新/删除和部门树逗号边界。独立 MySQL 测试继续覆盖 SQL mode 与字符比较，但无专用环境时明确跳过。
- 租户测试覆盖 ROOT、普通用户、匿名、无认证身份、相同/不同请求租户、缺失请求租户、插件关闭及 TenantFilter 正常/异常退出后的清理。
- 配置测试覆盖无配置使用原内置密钥及旧公钥验签、WARN 不泄露敏感值、外部 JKS/PKCS12 签发与验签、错误路径/密码/别名/非私钥条目不回退、自定义 JWKSource/JwtEncoder/JwtDecoder 回退、Secret 缺失与不足 32 字节；测试不新增提交私钥文件。
- 缓存测试使用固定时间与模拟 Redis，覆盖 exp 已过期、刚好到期、剩余不足一分钟、无 iat、有/无 exp、非法属性及缓存清理；记录实际写入 TTL，确认不会重新缓存过期身份。
- 请求体测试覆盖低于/等于/超过上限、无 Content-Length 的分块流、空体、已有缓存、取消与下游错误；检查超限不进入后续链且不输出明文或密钥日志。
- 先运行目标模块回归，再运行跨模块 mvn test；按环境执行相关 *IT 并核对变更 Java 的 Checkstyle。记录真实命令、结果和未验证项，不做线上攻击或部署。

## 可移植性修订（其中额外 JDBC 策略已按后续纠正撤销）
- Rain 在确认 CONVERT 和 find_in_set 的方言限制后明确要求“改改”，授权移除这两处限制并采用参数绑定及可替换部门范围策略；原有其他修复范围、默认密钥兼容和人工 gate 不变。
- 新增可覆盖的 DepartmentDataScopeProvider Bean；默认实现按当前 JdbcTemplate 数据源执行绑定查询，不修改表结构，不要求应用必须另写实现。
- 直接调用旧静态 dataScopeFilter 并自行执行含参数的 Expression，或自行组合原生 DataPermissionInterceptor 的应用，须迁移为本次提供的 ParameterizedDataPermissionInterceptor；缺少绑定上下文时抛错，不返回未绑定问号或字符串拼接回退。
- 补充真实 MyBatis/H2 集成测试，验证原参数/新增参数顺序、UNION、子查询、写操作和默认策略的逗号边界；真实 MySQL 测试仍按独立环境条件执行。

## User correction: keep SQL subquery
- Rain 明确反对额外 JDBC 处理 DataPermission：撤销部门策略接口、JdbcTemplate 默认实现及相关 Bean，直接保留原 SQL 子查询。
- DEPT_AND_SUB 在同一条业务 SQL 内按 sys_dept/tree_path 的等值及逗号边界 LIKE 匹配；所有值由 MyBatis 参数绑定，不使用 find_in_set/CONVERT，不提前查询部门或将部门集合展开为大量占位符。
- 自动配置恢复单一的原工厂方法签名；参数化拦截器无参构造，其他已接受行为保持。

## User correction: preserve BodyGlobalFilter flow
- Rain 指出 BodyGlobalFilter 改动偏离原逻辑，本次恢复原分支结构、缓存命中短路和链构造/订阅语义。
- 只保留配置的聚合字节上限、聚合超限 413 映射、敏感 body 日志删除及缓冲释放；已有缓存由原逻辑直接传递。

## Accepted recheck corrections
- Rain 逐项确认复查发现的三项问题及修复解释，明确要求统一修复，授权更新既有缓存键兼容约束。
- 派生 JWT 缓存改用 v2，绑定认证来源、验签应用、租户、最终主体、issuer 和 jti；不回读旧键。opaque 内省缓存键保持原样，撤销回调继续调用同一 buildKey；不操作实际缓存。HMAC 验签应用只存 exchange 内部属性，不增加 JWT claims，也不改变 invoker。
- JWKSource、JwtEncoder 分别按各自类型退让，恢复自定义 encoder 依赖框架 source 的兼容性；替代此前“仅有自定义 encoder 就跳过 source”的设计。
- 补齐 TOP 与 Oracle 层级表达式参数访问，保持参数完整消费与异常拒绝执行；不宣称所有数据库语法都已验证。

## Latest user corrections
- Rain 要求 JWT 默认兼容及环境变量说明不要重复散布：集中在 Gateway Starter README，移除示例和 Archetype 的重复说明。
- Rain 明确将 maxCachedBodySize 默认改为 10 MiB（10MB，10,485,760 字节），替代前述 1 MiB；保留正数和 int 容量校验、外部配置覆盖及原 BodyGlobalFilter 控制流。无参过滤器构造复用 GatewayProperties 默认值。
- Rain 随后要求先恢复原 JWT 缓存实现：撤销 v2 缓存键及其配套内部 HMAC 应用属性、常量和测试改动。保留原 jti 优先规则，缓存身份碰撞问题恢复为已知未修复项；SQL、JwtConfiguration 与 10 MiB 默认值等其他修改不受影响。
- 随后 Rain 选择 prefix 后分别追加 id: / u:，并明确不同应用网关缓存隔离；按此部署前提只修复 JWT_ID 与 name 跨类型碰撞。保留原 API Key 与租户分段，不扩展到应用身份绑定或摘要键；旧未分类派生键不回读，撤销仍使用公共 buildKey。
