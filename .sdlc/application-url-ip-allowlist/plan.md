# Plan: 应用与 APIKEY 的多行 URL 和允许 IP 配置 (from spec.md 2026-11-10)
Status: accepted

## Files that change
- `.sdlc/application-url-ip-allowlist/intent.md`
- `.sdlc/application-url-ip-allowlist/spec.md`
- `.sdlc/application-url-ip-allowlist/plan.md`
- `.sdlc/application-url-ip-allowlist/review-findings.md`
- `macula-boot-commons/src/main/java/dev/macula/boot/constants/CacheConstants.java`
- `macula-boot-commons/src/main/java/dev/macula/boot/security/AccessRuleUtils.java`
- `macula-boot-commons/src/test/java/dev/macula/boot/security/AccessRuleUtilsTest.java`
- `macula-boot-commons/README.md`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/utils/TenantUtils.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/utils/TenantUtilsTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/ApiKeyGlobalFilterTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/KongApiGlobalFilterTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/RmOpaqueTokenEndpointFilterTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/security/ApiKeySecurityChainTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/README.md`

## Order of work
1. 在两仓记录相同的 accepted intent/spec；本计划经人工明确接受后同步 accepted plan 及评审记录。已核对插件 artifact-gate 要求 plan 存在于 HEAD：本计划的人工审批同时请求授权在两仓仅本地提交本次已接受的 SDLC 工件，提交说明含 Status: accepted；不提交业务源码、不推送、不部署。保留用户已有改动，提交前核对暂存清单，只包含本次工件；若该前置动作未获批准，不绕过门禁。
2. Boot Commons 增加 permitIps 常量及 AccessRuleUtils。沿用 Java 17 和现有依赖，使用现有 Bouncy Castle 字面量地址校验能力及按字节掩码匹配，不新增依赖、不对主机名执行 DNS。URL 兼容逗号和 LF/CRLF/CR，输出 LF；IP 仅按行解析，限制 2000 字符。仅空白视为未配置；非空但仅有分隔符、非法条目或非法前缀必须拒绝，不能被规范化成不限。IPv4-mapped IPv6 地址和网段统一为 IPv4 等价规则，仅接受可无歧义转换的前缀，拒绝无法等价转换的 mapped 网段。
3. Boot 网关直接在 TenantUtils.checkTenantAndUrls 方法集中执行 URL 和 IP 准入判断，公共工具只负责解析、规范化及地址匹配，不改变过滤器次序、租户逻辑、签名算法、StripPrefix 路径时点及 principal 发布顺序。只匹配 request 的直连对端地址，不读取转发头。通过既有调用链覆盖 HMAC、APIKEY 和强制 HMAC token 清理；非法缓存、缺少来源及拒绝均不进入下游。
4. Cloud 追加 V24，两张表新增 allow_ips，可空、2000 字符；应用 access_path 扩为 2000。先核对迁移号，无需更改旧迁移。同步 Entity/BO/VO/Form、查询列及转换。两种 Form 使用显式字段是否提交标记，省略 allowIps 时保留，null/空白时清空；提供服务端校验并在 Service 规范化，保证非 HTTP 调用也不能绕过。URL/IP 分开处理，保持既有 URL 更新语义。
5. Cloud 缓存集中组装 permitUrls/permitIps，分别按 Key 非空优先、应用兜底。应用及 Key 新增、更新、清空、启动重建共用同一路径；明确写空字符串覆盖旧缓存。保留原有 afterCommit、租户作用域、锁、撤销及失败反馈。
6. 管理端使用小型共享工具将旧逗号 URL 转为多行，表单增加允许 IP，传递 allowIps，更新示例、长度限制、继承说明和列表换行。维持原有 loading、错误反馈、防重提交及凭据脱敏行为。
7. 按依赖顺序验证 Commons、Boot Gateway、Cloud API/System、Admin。需要本机 Maven 安装才能让 Cloud 解析新 Commons 时，仅安装必要 Boot 构件，记录命令与范围，不发布远端。同步 README 的配置、直连 IP 边界、先网关后配置写入的升级顺序及回退限制。
8. 实现阶段记录实际偏差与验证证据；若需要超出文件清单或改变设计合同，先更新计划并寻求人类决定。代码完成后按技能门禁确认完整性，再交 sdlc-test 正式验证；外部环境缺失与代码失败分别报告。

## Risks
- 已接受的代理边界保持：经过代理时按对端代理 IP 匹配，不保证识别最终调用者；部署须防止不可信转发头改写匹配地址。本轮不新增可信代理配置。
- IP 和 CIDR 严格解析容易在地址族、/0、最大前缀、mapped IPv6 上出错，先用边界测试固定语义。遇到一个坏条目时拒绝整份非空 IP 配置，不能让好条目掩盖损坏配置。
- 旧网关不能解释 LF URL 且忽略 permitIps；先升级全部网关，再更新 System/Admin 配置及缓存。数据库执行、镜像更新及真实网络配置由后续明确授权操作。
- 更新省略字段不能清空已有 IP；MyBatis 非空更新策略可能跳过 null，必须用显式 update wrapper 清空并用序列化/服务测试验证。
- 现有数据库提交与 Redis 发布不原子，保留错误报告与重建机制，不宣称同步失败时限制已生效。
- 本地 Boot SNAPSHOT 依赖可能过期，验证必须确认 Cloud 使用本轮构件；不为解决本机依赖而修改 POM 版本或扩大安装范围。
- 新迁移只增加列及扩列，回退不删列、不缩列；不在业务库运行测试，不自动改动旧迁移校验和。
- 两仓计划分别只列本仓精确路径；新增或变更清单需重新记录审批及本地工件提交，不能利用其他在途计划的宽泛准入绕过本次范围。

## Proof
- AccessRuleUtilsTest：多种 URL 分隔、trim/空白、纯分隔符异常、长度；IPv4/IPv6、CIDR /0 和最大前缀、非法地址、主机名/端口/通配符、mapped 地址及网段、地址族不匹配、坏条目混合、无 DNS 路径。
- TenantUtilsTest 与三类过滤器测试、安全链测试：允许、拒绝、来源缺失、缓存缺字段和损坏、伪造 Forwarded/X-Forwarded-For、URL 和 IP 均须满足；断言拒绝后无 principal、无下游调用及 token 清理副作用。
- ApplicationFormTest/ApplicationApiKeyFormTest、服务/Controller 测试：2000 长度、IPv4/IPv6/CIDR、未知身份字段拒绝、省略/null/空白、新建与更新、URL/IP 各自继承、无越权及租户边界回归。
- ApplicationCredentialCacheServiceTest：两种凭据字段、新增/更新/清空/重建、应用变更刷新所属 Key、旧值覆盖、Key URL 非空而 IP 继承及反向组合、缓存发布失败反馈。
- 隔离 ApplicationUrlIpAllowlistMigrationIT 与既有 ApplicationApiKeyIntegrationIT：V24 前已有逗号数据，新增列/扩列、旧数据不丢失、新配置往返、缓存发布；只使用隔离实例，运行条件不具备时明确标记未验证。
- Vitest/Cypress：应用及 APIKEY 旧 URL 换行回显、多行 URL/IP 保存和清空、继承提示、列表展示、提交错误及重复提交；前端构建。
- Maven 从受影响模块开始，Cloud 使用 -Plocal；适用集成测试使用 verify 和 Failsafe，不能用 mvn test 冒充集成验证。执行变更 Java 的 Checkstyle、git diff --check，报告未完成的真实网关/代理环境验证。

## Approval
Rain 回复“YES，直接集中到TenantUtils.checkTenantAndUrls方法里面验证。”，接受本计划及两仓仅本地提交 SDLC 工件的前置动作。所有 URL/IP 准入判断集中在该方法，HMAC/APIKEY 不新增独立判断分支。
