# Plan: API Key 与 HMAC 网关处理链路对齐 (from spec.md 2026-11-04)

Status: accepted

用户于 2026-11-04 回复“YES”，明确接受本计划，开始实施。

## Files that change
以下路径均相对于 `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/`：

- `src/main/java/dev/macula/boot/starter/cloud/gateway/utils/ApiKeyUtils.java`（新增）：统一 API Key 输入边界验证、格式识别和可信请求身份读取。
- `src/main/java/dev/macula/boot/starter/cloud/gateway/constants/GatewayConstants.java`：API Key 可信身份属性及专用 JWT 缓存前缀。
- `src/main/java/dev/macula/boot/starter/cloud/gateway/security/ResourceServerConfiguration.java`：API Key 使用共享输入边界检查并跳过 introspection；普通 Bearer 委托标准 converter。
- `src/main/java/dev/macula/boot/starter/cloud/gateway/security/ResourceServerAuthorizationManager.java`：API Key 在普通 Bearer 前分流，删除基于空 token 和 authType 的特殊授权。
- `src/main/java/dev/macula/boot/starter/cloud/gateway/filter/ApiKeyGlobalFilter.java`：迁为 GlobalFilter，校验密钥及归属租户后保存可信 principal。
- `src/main/java/dev/macula/boot/starter/cloud/gateway/config/GatewayAutoConfiguration.java`：移除 API Key Bean 定义及不再需要的 import，交由 ResourceServerConfiguration 集中注册。
- `src/main/java/dev/macula/boot/starter/cloud/gateway/filter/AddJwtGlobalFilter.java`：处理 API Key 可信身份、清理 Query、隔离缓存、移除重复链调用路径。
- `src/test/java/dev/macula/boot/starter/cloud/gateway/utils/ApiKeyUtilsTest.java`（新增）。
- `src/test/java/dev/macula/boot/starter/cloud/gateway/security/ResourceServerAuthorizationManagerTest.java`（新增）。
- `src/test/java/dev/macula/boot/starter/cloud/gateway/security/ApiKeySecurityChainTest.java`（新增）：通过 WebTestClient 绑定真实 SecurityWebFilterChain，以实际 Gateway FilteringWebHandler 和模拟路由末端组装链路，模拟 Redis/introspector，不建立外部连接。
- `src/test/java/dev/macula/boot/starter/cloud/gateway/config/ApiKeyAutoConfigurationTest.java`（新增）。
- `src/test/java/dev/macula/boot/starter/cloud/gateway/filter/ApiKeyGlobalFilterTest.java`（新增）。
- `src/test/java/dev/macula/boot/starter/cloud/gateway/filter/AddJwtGlobalFilterTest.java`：JWT 转换及缓存隔离回归。
- `README.md`：调用、过滤顺序、租户缓存准备、开关及接口迁移说明。

SDLC 目录内维护 `intent.md`、`spec.md`、`plan.md`、`review-findings.md`，记录审批、审查和偏差；不修改 POM、HMAC 验签实现或租户映射解析实现。

## Order of work
1. 补充必要的失败回归，覆盖现有 JWT API Key 分支在 Mono<Void> 完成后再次执行下游的缺陷；建立 converter 和认证链测试框架。
2. 添加小型 API Key 工具和属性常量。对存在 API Key 候选的请求统一验证：只允许单一来源、一个 Authorization Header 或一个 access_token 值；Header 与 Query 同时出现、重复 Header/Query、空值或非法 Bearer 字符均拒绝。保持 Bearer scheme 大小写兼容及现有 sk- 前缀识别规则，Query API Key 在 GET/POST/PUT/PATCH/DELETE 等方法均采用相同输入检查。共享解析结果仅表达待校验密钥，不是可信认证身份；不向外暴露或记录原始密钥。
3. Bearer converter 先使用统一 API Key 输入边界验证：有效 API Key 候选返回空，非法候选返回认证错误；非 API Key 请求完全委托标准 converter，保留普通 OAuth 的 GET Query 限制及原有格式检查，不将该限制误用为 API Key 的输入校验。authorize 和 API Key GlobalFilter 使用同一解析工具，识别 API Key 后仅放行到 Gateway 校验阶段，不创建 Authentication。忽略授权的 Gateway 路由也必须在 GlobalFilter 再执行同样的输入检查。
4. 将 ApiKeyGlobalFilter 改为 GlobalFilter，当前 order=190；与 order=200 的 KongApiGlobalFilter 互斥处理凭据，均早于 order=1000 的 JWT 过滤器。每个请求先清除旧的 API Key 可信属性。读取现有 apikey.enabled，关闭时拒绝 API Key；非 API Key 直接通过。使用 boundedElastic 读取 Redis 密钥 Hash，并调用 KongApiUtils.resolveGatewayTenantId；校验 applicationCode、applicationId、tenantId 的必要类型和内容，拒绝无效租户、未知网关租户和跨归属访问。验证成功才写入可信 principal，保留原始 token 供后续识别，不构造虚拟 OAuth2AccessToken，不写 ThreadLocal 或 SecurityContext。
5. AddJwtGlobalFilter 在普通 Bearer 分支前处理 API Key，缺少可信身份拒绝；使用现有 JWT 生成方法，保持 sub、applicationId、nickname、authType、tenantId。专用缓存前缀加租户及应用名，避免与 OAuth/HMAC 原键冲突；不清理旧缓存，等待自然过期。清除所有 access_token Query 参数、覆盖 Authorization 为 JWT；移除原 API Key 的 Context 兜底，未携带 token 的普通请求直接执行下游一次。OAuth/HMAC 原有缓存键和身份生成行为保持。
6. 补齐条件装配、租户拒绝、错误处理、Query 清理、链路执行次数和 OAuth/HMAC 回归；更新 README。失败记录不输出原始密钥、Authorization、请求 URL 或 Redis 异常详情，避免凭据进入日志或响应。
7. 先运行定向测试，再运行网关模块及依赖单元测试；若认证链测试实际启动完整应用上下文，则按规则改为 *IT 并使用 Failsafe，记录文件集偏差后再执行相应 verify。

## Risks
- API Key 归属缺失、非法或跨租户的旧调用会被拒绝；升级前准备 System 租户映射及密钥归属缓存。不新增 permitUrls 或 authTenantId 策略。
- 本地非 Gateway 路由不再获得 API Key 认证身份；这是用户已接受的边界。功能关闭时的拒绝保证适用于 Gateway 路由。
- WebFilter 改为 GlobalFilter 会影响依赖旧 filter(exchange, WebFilterChain) 签名的定制；类名按用户要求改为 ApiKeyGlobalFilter，保留 Bean 名，README 给出迁移说明。
- API Key 输入由共享工具显式校验，不能假定标准 converter 覆盖非 GET Query 或重复 Header；非 API Key 请求保持标准 converter 既有行为。Query 凭据仍有接入侧 URL 日志风险。
- 自定义 Bean 替换过滤器时须履行可信身份契约；AddJwt 在缺少验证结果时拒绝，避免仅凭前缀转 JWT。
- 不新增依赖或持久化结构；回滚需恢复整套过滤链实现，不能只回滚某个过滤器。

## Proof
- ApiKeyUtilsTest：Header/Query 格式识别、scheme 大小写、非 API Key、可信属性读取；覆盖 GET 及非 GET Query、重复 Authorization、重复 Query、来源冲突、空值及非法字符。
- ResourceServerAuthorizationManagerTest：API Key 分流、普通 Bearer URL 角色限制及无 token 拒绝。
- ApiKeyGlobalFilterTest：有效密钥、密钥不存在、字段非法、同租户通过、跨租户拒绝、租户缺失或非法、网关映射未知、伪造租户 Header、Redis 故障、开关关闭；失败不调用下游、不保留可信身份、不泄漏密钥。
- ApiKeyAutoConfigurationTest：默认、关闭及用户 Bean 覆盖；默认 Bean 为 GlobalFilter 而非 WebFilter。
- AddJwtGlobalFilterTest：捕获 JWT claims，测试 Header/Query 清理、缓存身份隔离、缺少可信身份拒绝、下游仅执行一次；继续验证 HMAC 租户和普通 OAuth 原键。
- ApiKeySecurityChainTest：真实 Spring Security 授权和 Gateway 排序链配合模拟 Redis/introspector。覆盖 API Key Header/Query 不调用 introspection（GET 及 POST/PUT/PATCH/DELETE）、普通 Bearer 调用 introspection 并受角色限制、HMAC 路径、非法及重复凭据（含多个 Authorization Header）、Header/Query 冲突、忽略认证路由上的密钥校验、开关关闭以及凭据不向末端泄漏。使用模拟末端统计每次请求仅调用一次，避免只测试某个过滤器而遗漏组装问题。
- Maven 定向：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=ApiKeyUtilsTest,ResourceServerAuthorizationManagerTest,ApiKeySecurityChainTest,ApiKeyAutoConfigurationTest,ApiKeyGlobalFilterTest,AddJwtGlobalFilterTest -Dsurefire.failIfNoSpecifiedTests=false`。
- 模块回归：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test`。按实际报告记录测试数和结果；模拟链路证据不等同于真实 Redis/IAM 或部署验证。

## Implementation record
- 2026-11-04：完成上述文件内的实现、README 及回归测试。没有修改 HMAC 验签、租户映射解析、POM 或 imports 登记。
- 复现：`AddJwtGlobalFilterTest#anonymousRequestRunsDownstreamOnce` 在原实现中失败，下游实际执行 2 次，期望 1 次；移除 Context 兜底后通过。
- 计划澄清：converter 的文件描述按审查后步骤 3 更新，明确 API Key 专属输入校验先于普通 converter。无新增范围或文件集偏差。
- 状态：Build 实现已由用户在最后一轮“好的，我写好了。确认”明确确认；进入 Stage 4 执行当前代码验证，结论见 test-report.md，其他人工门禁未自动批准。

## Verification record
- 定向测试：执行 Proof 中六个测试类的 Maven 命令，25 项，失败 0、错误 0、跳过 0，BUILD SUCCESS。
- 模块及依赖回归：执行 Proof 中 `-am test` 命令，Commons 28 项、Redis 5 项、Gateway 48 项，共 81 项，失败 0、错误 0、跳过 0，BUILD SUCCESS。
- Checkstyle：对本次新增/修改的全部 Java 文件运行 `mvn -N checkstyle:check -Dcheckstyle.includes=<本次 Java 文件路径列表>`，0 violations，BUILD SUCCESS。
- `git diff --check` 通过。未执行 Failsafe、真实 Redis/IAM/下游联调或部署验证；认证链测试使用真实 SecurityWebFilterChain、FilteringWebHandler 与模拟基础设施，不启动完整应用。

## User-requested adjustment
- 用户要求将 `ApiKeyAuthenticationFilter` 更名为 `ApiKeyGlobalFilter`。同步重命名源文件、测试类及所有 Java 引用；保留既有 Bean 名 `apiKeyAuthenticationFilter`，避免额外改变按名称注入契约。此项是用户明确批准的命名调整。
- 更名后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=ApiKeyGlobalFilterTest,ApiKeyAutoConfigurationTest,ApiKeySecurityChainTest -Dsurefire.failIfNoSpecifiedTests=false`，14 项，失败 0、错误 0、跳过 0；对现存变更 Java 文件再次执行 Checkstyle，通过；`git diff --check` 通过。

- 用户要求将 API Key Bean 定义从 GatewayAutoConfiguration 移至 ResourceServerConfiguration，与 KongApiGlobalFilter 放在一起。保留 Bean 名、缺失 Bean 回退和运行开关语义；装配测试检查两者由同一配置 Bean 创建。
- 移动后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=ApiKeyAutoConfigurationTest,ApiKeySecurityChainTest -Dsurefire.failIfNoSpecifiedTests=false`，8 项，失败 0、错误 0、跳过 0；涉及的三个 Java 文件 Checkstyle 通过，`git diff --check` 通过。

- 用户要求将租户解析独立为 gateway/utils/TenantUtils.java。新增该源文件；修改 KongApiUtils.java 为内部调用 TenantUtils 并保留原公开方法的兼容委托；ApiKeyGlobalFilter 改用 TenantUtils；原有租户解析测试迁至 utils/TenantUtilsTest.java，保留 HMAC 未知租户拒绝和旧入口委托的回归。此项为用户批准的职责抽取，解析规则不变。

- 验证期间发现工作区已有 API Key 缓存前缀改为 `CacheConstants.SECURITY_SYSTEM_APPS + "apikey:"`、Bean 名改为 `apiKeyGlobalFilter`、order 改为 190 的调整。本轮保留实现，更新认证链测试缓存桩、装配测试 Bean 名和 README，使其与当前工作区一致。
- 抽取后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=TenantUtilsTest,KongApiUtilsTest,ApiKeyGlobalFilterTest,KongApiGlobalFilterTest,RmOpaqueTokenEndpointFilterTest,ApiKeySecurityChainTest,ApiKeyAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false`，29 项，失败 0、错误 0、跳过 0；本轮涉及 Java 文件 Checkstyle 0 violations，`git diff --check` 通过。

- 用户进一步要求 KongApiGlobalFilter 参考 API Key：验签成功后构造 OAuth2AuthenticatedPrincipal，AddJwtGlobalFilter 统一生成 JWT 和改写请求。新增修改范围为 filter/KongApiGlobalFilter.java 及 filter/KongApiGlobalFilterTest.java（均在当前 gateway 模块）；复用现有 GatewayConstants、AddJwtGlobalFilter 和测试文件。保留 HmacIdentity 供验签调用方使用，新增 HMAC principal 请求属性；invoker、tokenId、业务租户和缓存键规则保持不变，统一转换时清理 access_token。该项为用户明确要求的职责调整。
- 实现：KongApiGlobalFilter 写入 HMAC_PRINCIPAL_ATTRIBUTE；AddJwtGlobalFilter 的 resolvePrincipal 仅提取 HMAC/API Key 的可信 principal 或 OAuth 的已认证 principal。JWT 生成、Query 清理、请求头覆盖及下游调用共用一个 map/defaultIfEmpty/flatMap 流程；空身份回退发生在 Mono<ServerWebExchange> 阶段，确保下游只执行一次。
- 定向验证：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=KongApiGlobalFilterTest,AddJwtGlobalFilterTest,ApiKeySecurityChainTest,ApiKeyGlobalFilterTest -Dsurefire.failIfNoSpecifiedTests=false`，21 项，失败/错误/跳过均为 0。覆盖 HMAC principal 的 invoker/jti/业务租户、POST 请求体恢复、缺少可信 principal 拒绝和原缓存键。
- 模块回归：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test`，Commons 28、Redis 5、Gateway 51，共 84 项，失败/错误/跳过均为 0；全部现存变更 Java 文件 Checkstyle 0 violations，`git diff --check` 通过。未做真实 Redis/IAM/下游联调。

- 用户要求补充类、方法及关键逻辑注释，并将要求写入根 AGENTS.md。新增文档范围 AGENTS.md；对本轮改动的认证过滤器、输入工具及配置/授权入口补充中文职责与信任边界说明，仅修改注释，不改变行为。
- 已补充 AddJwtGlobalFilter、ApiKeyGlobalFilter、KongApiGlobalFilter 的类/方法职责，以及可信身份来源、租户准入、阻塞查询、失败关闭、缓存隔离和 Mono<Void> 兜底风险说明；同步补充工具和配置/授权入口注释。全部现存变更 Java 文件 Checkstyle 0 violations，git diff --check 通过；本轮仅注释与规则文档调整，未重复执行行为测试。

- 用户要求减少硬编码、使用常量或合适的枚举，并更新 AGENTS.md。沿用现有 GatewayConstants 定义认证标识、缓存字段和过滤顺序；复用 JWTClaimNames、SecurityConstants，正则按共享 API Key 前缀构造。仅进行常量抽取，保持协议值、Redis 结构、JWT claims、解析规则和顺序不变。
- 已将 API Key 前缀、认证来源字段/值、应用字段、缓存前缀及三种过滤阶段顺序提取为 GatewayConstants；JWT 有效期与租户缓存段保留为 AddJwtGlobalFilter 私有常量。ApiKeyUtils 的候选识别、Header/Query 正则和前缀长度共用定义；已有 JWTClaimNames.SUBJECT、JWT_NICKNAME_KEY、TENANT_ID_NAME 均直接复用。
- 常量抽取后运行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=ApiKeyUtilsTest,ApiKeyGlobalFilterTest,AddJwtGlobalFilterTest,KongApiGlobalFilterTest,ApiKeySecurityChainTest -Dsurefire.failIfNoSpecifiedTests=false`，24 项，失败/错误/跳过均为 0；涉及 Java 文件 Checkstyle 0 violations，git diff --check 通过。

- 用户要求对齐 API Key 工具接口：KongApiUtils 新增 resolveHmacToken 与 verifiedPrincipal，分别负责 Header 中 HMAC 凭据分流和可信 principal 读取。AddJwtGlobalFilter、KongApiGlobalFilter、ResourceServerAuthorizationManager 统一调用该工具，验签与 JWT 规则不变；沿用计划中的现有文件，补充方法注释与相关回归。

- HMAC 工具封装后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=KongApiUtilsTest,KongApiGlobalFilterTest,AddJwtGlobalFilterTest,ApiKeySecurityChainTest,ResourceServerAuthorizationManagerTest,RmOpaqueTokenEndpointFilterTest -Dsurefire.failIfNoSpecifiedTests=false`，28 项，失败/错误/跳过均为 0；涉及 Java 文件 Checkstyle 0 violations，git diff --check 通过。保留 isKongApiRequest 原有应用名识别语义，避免影响请求体缓存和清理入口。

- 用户要求 KongApiGlobalFilter 按 API Key 的 validate 流程组织：在订阅时清理旧身份，非 HMAC 直接继续；通过 fromCallable/subscribeOn(boundedElastic) 执行 HMAC 验签及阻塞 Redis 查询，validate 返回包含 principal 的 Result 以保留原错误码、消息和原因；成功后发布 principal、恢复请求体并调用下游。认证异常兜底仅覆盖验证阶段，不吞掉下游异常。补充调度、异常边界及空请求体回归。

- HMAC validate 流程调整后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=KongApiGlobalFilterTest,KongApiUtilsTest,AddJwtGlobalFilterTest,ApiKeySecurityChainTest,RmOpaqueTokenEndpointFilterTest -Dsurefire.failIfNoSpecifiedTests=false`，29 项，失败/错误/跳过均为 0；KongApiGlobalFilter 及测试 Checkstyle 0 violations，git diff --check 通过。补充验证 Redis 不占用事件线程、下游异常原样传播、缓存异常拒绝且清除旧身份，以及缺少缓存数组时恢复空 POST 请求体。

- 用户确认将 ApiKeyGlobalFilter.validate 统一为 Result<OAuth2AuthenticatedPrincipal>：移除 Optional，预期身份字段/租户失败直接返回 Result.failed，Redis 等异常仅在验证阶段兜底，HTTP 403 和 ACCESS_UNAUTHORIZED 保持不变。验证成功才发布 principal；补充下游异常不被吞掉的回归。
- 已完成 Result 流程统一；字段读取改为可空的 nonBlankText，字段及租户非法直接返回失败结果。执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=ApiKeyGlobalFilterTest,ApiKeySecurityChainTest,AddJwtGlobalFilterTest,ApiKeyAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false`，21 项，失败/错误/跳过均为 0；涉及 Java 文件 Checkstyle 0 violations，git diff --check 通过。

- 用户要求简化 buildKey：所有认证来源共用租户段和 jti/主体选择逻辑，API Key 仅在公共 JWT 缓存前缀后增加 apikey: 类型段；OAuth/HMAC 原键不变。旧 API Key 专用前缀缓存不读取、等待自然过期；无租户字段不拼接 null，存在 jti 时统一优先使用 jti。同步更新测试和 README。
- 简化后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=AddJwtGlobalFilterTest,ApiKeySecurityChainTest,RmOpaqueTokenEndpointFilterTest -Dsurefire.failIfNoSpecifiedTests=false`，15 项，失败/错误/跳过均为 0；涉及 Java 文件 Checkstyle 0 violations，git diff --check 通过。

- 用户要求优化 OAuth principal 读取并处理 SecurityContext 中 Authentication 为 null 的情况。使用 mapNotNull 和 ofType 代替可返回 null 的 map 及重复 filter/cast，缺少上下文、空认证、未认证或错误类型统一抛出 BadCredentialsException，不执行 JWT 编码或下游。先添加回归复现再修复，保留用户自行调整的 buildKey。

- 空 Authentication 回归在旧实现复现 Reactor map 返回 null 的 NullPointerException，改为 mapNotNull 后通过。组合测试发现当前工作区另有 TENANT_CACHE_SEGMENT_PREFIX 从 a 改为 tn 的用户调整；本轮保留该实现并同步测试预期及 README。该缓存键格式变化使旧缓存不再命中，等待自然过期；非本轮空安全修改引入。
- 最终执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=AddJwtGlobalFilterTest,ApiKeySecurityChainTest -Dsurefire.failIfNoSpecifiedTests=false`，12 项，失败/错误/跳过均为 0；空安全回归覆盖无上下文、空 Authentication、非 Bearer 类型、未认证 Bearer、空 principal 及错误 principal 类型，不调用 JWT 编码、Redis 或下游。涉及 Java 文件 Checkstyle 0 violations，git diff --check 通过。

- 用户要求将 APIKEY_IDENTITY_ATTRIBUTE 更名为 APIKEY_PRINCIPAL_ATTRIBUTE，与保存的 OAuth2AuthenticatedPrincipal 类型及 HMAC_PRINCIPAL_ATTRIBUTE 对齐。请求属性后缀同步由 .apiKeyIdentity 改为 .apiKeyPrincipal；统一更新生产代码、测试及 README，不改变验证或 JWT 行为。
- 更名后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=ApiKeyGlobalFilterTest,ApiKeyUtilsTest,AddJwtGlobalFilterTest,ApiKeySecurityChainTest -Dsurefire.failIfNoSpecifiedTests=false`，22 项，失败/错误/跳过均为 0；涉及 Java 文件 Checkstyle 0 violations，git diff --check 通过；模块源码和 README 无旧属性名残留。

- 用户要求删除已废弃的 API Key 专用 JWT 缓存前缀常量；生产 buildKey 已使用公共前缀加类型段，仅删除冗余常量，并将剩余两个测试引用改为公共前缀及明确的协议预期值。
- 清理后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=AddJwtGlobalFilterTest -Dsurefire.failIfNoSpecifiedTests=false`，7 项，失败/错误/跳过均为 0；模块源码无废弃常量残留，git diff --check 通过。

- 用户要求参考 KongApiGlobalFilter，在 ApiKeyGlobalFilter 抽取 createPrincipal。validate 仅完成缓存字段与租户准入检查，成功后将已校验的应用编码、应用 ID 和租户 ID 传给 createPrincipal；claims、权限集合及响应保持不变，新增方法级 Javadoc。
- 抽取后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=ApiKeyGlobalFilterTest,AddJwtGlobalFilterTest,ApiKeySecurityChainTest -Dsurefire.failIfNoSpecifiedTests=false`，19 项，失败/错误/跳过均为 0；ApiKeyGlobalFilter Checkstyle 0 violations，git diff --check 通过。

- 用户要求先独立构建 attributes，再创建 principal。createPrincipal 改为局部 Map 按字段构建后传入构造器，与 HMAC 写法一致；属性值保持不变。

- 用户要求抽取 API Key 与 HMAC 重复的网关归属检查和认证租户解析。TenantUtils 新增 checkTenant(gatewayTenantId, ownerTenant, authTenant)，统一归属匹配、未知网关拒绝和 authTenant 回退；沿用当前工作区 API Key 已支持 authTenantId 的实现及 CacheConstants 中 API Key 常量。HMAC 旧三参数验签的 null 网关租户兼容模式保留，不新增准入策略。同步针对归属租户/业务租户分离、旧入口及非法值进行验证。

- 同步当前工作区常量迁移：API Key JWT 的 applicationId 字段复用 CacheConstants.APIKEY_APPLICATION_ID_ATTRIBUTE，修复已删除 GatewayConstants 常量的残留引用；认证链测试使用当前 CacheConstants.APIKEY_CACHE_PREFIX，README 同步 API Key 的 authTenantId 回退规则。
- 抽取后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=TenantUtilsTest,ApiKeyGlobalFilterTest,KongApiUtilsTest,KongApiGlobalFilterTest,ApiKeySecurityChainTest,RmOpaqueTokenEndpointFilterTest -Dsurefire.failIfNoSpecifiedTests=false`，35 项，失败/错误/跳过均为 0；涉及 Java 文件 Checkstyle 0 violations，git diff --check 通过。覆盖共同准入、独立认证租户、认证租户缺省回退、未知网关拒绝、非法缓存值及旧三参数验签兼容模式。

- 用户明确要求 checkSign 仅验签，将应用缓存、租户及 URL 准入移入 KongApiGlobalFilter.validate。checkSign 新签名为 (exchange, secretKey)，保留时间、算法、路径/Query/Body 摘要规则且不读 Redis、不发布身份；删除原 Redis 参数的三/四参数重载并记录 API 迁移。KongApiGlobalFilter.validate 对外供本模块 token 清理入口复用，继续在 boundedElastic 执行。
- 必要扩展文件集：filter/RmOpaqueTokenEndpointFilter.java、config/GatewayAutoConfiguration.java、filter/RmOpaqueTokenEndpointFilterTest.java，均在当前 gateway 模块。清理入口注入 KongApiGlobalFilter 并调用完整 validate，保留租户及 URL 许可检查；同步工具测试与过滤器策略回归，避免把旧 checkSign 的策略测试误当成纯验签测试。
- 已完成职责迁移：纯 checkSign 不读 Redis、不修改身份属性；完整 validate 包含应用读取、密钥存在性、归属及认证租户、URL 许可和签名验证。token 清理入口的 URL 拒绝、租户拒绝和错误签名均先于 token 缓存读取/删除。README 明确旧重载与清理过滤器构造器的迁移方式。
- 定向执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=KongApiUtilsTest,KongApiGlobalFilterTest,RmOpaqueTokenEndpointFilterTest,AddJwtGlobalFilterTest,ApiKeySecurityChainTest,ApiKeyAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false`，35 项，失败/错误/跳过均为 0。
- 模块及依赖执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test`，Commons 28、Redis 5、Gateway 61，共 94 项，失败/错误/跳过均为 0。全部现存变更 Java 文件 Checkstyle 0 violations，git diff --check 通过。清理两个已无使用的 import；未执行真实 Redis/IAM 联调或部署。

- 用户指出 ApiKeyUtils 正则常量拆分过度。移除 KEY_PATTERN、KEY_GROUP 和仅在该工具内部使用的全局 APIKEY_PREFIX，保留三个预编译 Pattern，但直接写出完整表达式；Query 候选使用直观的忽略大小写前缀判断。认证字段等跨组件常量保留，AGENTS.md 补充避免机械拆分局部表达式的规则。解析与拒绝策略不变。

- 验证时发现工作区已移除 HmacIdentity/旧身份属性，并将 TenantUtils.checkTenant 改为接收 Redis 与网关应用名。保留实现，同步现有测试到新契约，去除废弃身份引用并验证未知网关拒绝；更新相关注释，不回退用户改动。
- 简化后执行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=ApiKeyUtilsTest,ApiKeySecurityChainTest,TenantUtilsTest,KongApiUtilsTest,KongApiGlobalFilterTest,AddJwtGlobalFilterTest -Dsurefire.failIfNoSpecifiedTests=false`，33 项，失败/错误/跳过均为 0；涉及 Java 文件 Checkstyle 0 violations，git diff --check 通过。同步 README 中已移除的身份和旧租户委托说明，清理无用 RedisTemplate import。

## 当前实现确认及正式验证
- 用户明确确认当前实现已完成。2026-11-04 执行全仓 `mvn test`，26 个模块汇总 308 项，失败 0、错误 0、跳过 0；网关模块 60 项。变更 Java 文件 Checkstyle 0 violations，git diff --check 通过。
- Stage 4 的完整集成验证尚未完成：CI 执行 mvn verify；本地 Redis IT 固定访问 6379 并写测试数据，该端口存在服务，未使用或修改该服务。环境隔离未就绪，不标记 Verification is green，不进入 PR 审查。详见 test-report.md。

- 用户再次确认已修改完成：当前 TenantUtils.checkTenant 接收应用 Map，API Key 新增 URL 许可检查，resolveApiKey 改为前缀识别。验证阶段仅同步 TenantUtilsTest 到新方法签名，不修改用户的认证源码；保留重复/冲突/非法凭据的原负面用例，检查已接受的输入边界是否回退。

- 最新模块验证实际发现 6 个回归失败：ApiKeyGlobalFilter 丢失非 API Key 分流，合法 OAuth/HMAC 被拒；简化 resolver 丢失重复/冲突/非法格式检查。按原已接受约束修复，保留用户 validate(exchange) 和直接表达式写法，不恢复正则片段常量。先在过滤器中分流，再检查开关；resolver 仅补充来源唯一性和完整格式检查。

- 修复后网关 60 项及依赖测试通过。进一步补充 API Key 新增 permitUrls 允许/拒绝用例，并扩展非 API Key 分流测试覆盖 enabled=false，验证关闭功能不会拦截其他认证类型；同步 README 与 spec 中的最终 URL 许可行为。

- 最终全仓复验 `mvn test`，26 个模块汇总 309 项，失败 0、错误 0、跳过 0；Gateway 61 项，BUILD SUCCESS，耗时 02:10。全部现存变更 Java 文件 Checkstyle 0 violations，git diff --check 通过。test-report.md 同时记录本轮 6 项初始回归失败和修复后证据；未宣称完整 Redis 集成验证或后续审查门禁通过。

- 用户要求继续后，在临时副本中运行完整 mvn verify。578 个 Java/POM 校验一致，仅 5 个测试资源替换 Redis 端口；真实 Redis 8.10.2 隔离实例验证后关闭，原 6379 不受影响。Surefire 309 项全通过；Failsafe 50 条目，47 执行通过、3 既有条件跳过，失败/错误 0，BUILD SUCCESS。Verification is green，允许进入 Stage 5 本地审查；Redis 7 CI、真实 IAM 和既有外部 E2E 证据边界保留。

- Stage 5 独立审查与独立复核确认 token 清理入口既有 DEBUG 日志泄漏完整 token/Header。按 REVIEW.md 的既有安全问题处置要求修复：不再输出 Header/Body；解析异常仅记录异常类型，不输出消息或堆栈。补充日志捕获测试，覆盖有效清理 token、被拒绝的 API Key/Cookie 及非法 JSON 片段，并在修复前实际复现失败。修复后返回 Stage 4 重新运行完整验证。

- 安全日志修复后再次完整 `mvn verify` 成功：Surefire 310 项、Failsafe 执行通过 47 项、既有条件跳过 3 项，失败/错误 0；578 个 Java/POM 漂移 0。临时 Redis 已关闭，Checkstyle 和 diff 检查通过，返回 Stage 5 第二轮增量审查。
