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

## Implementation record
- Code 审查独立复核确认 BC IPv6 boolean 存在尾部冒号边界误判：本类改用无 DNS 的压缩位置、分组数量及 IPv4 尾段结构校验，IPv4 范围继续使用现有 BC 校验。补充合法单零组压缩及非法单冒号回归；保留已接受的地址/CIDR 语义，未新增依赖或扩大文件范围。
- 按用户要求进一步提取 AccessRuleUtils.matchesUrl，与 matchesIp 一致封装规则拆分、空配置放行及任一命中语义；Ant 匹配器由 TenantUtils 传入，不增加 Commons 依赖，集中准入入口及拒绝行为保持不变。
- 按用户后续要求，checkTenantAndUrls 中 URL/IP 均以局部配置变量、请求目标、显式 Allowed 布尔结果及失败返回组织，保持空配置放行、非法配置拒绝和原匹配语义；仅调整集中入口写法。
- 用户指定的集中入口已落实：TenantUtils.checkTenantAndUrls 直接执行 URL 与 IP 准入；HMAC、APIKEY 及强制 HMAC 清理端点保持既有调用链，不新增独立准入分支。AccessRuleUtils 仅提供规范化和纯字面量 IP/CIDR 解析、匹配。
- Cloud 新增 allowIps / allow_ips；Redis 新增 permitIps；URL 与 IP 分别按 APIKEY 非空优先、应用兜底。省略 allowIps 保留，显式 null/空白清空，清空缓存显式写空字符串。
- 管理端两个入口增加多行 IP，旧 URL 多行回显；应用提交使用 try/finally 恢复 loading，并只在 payload 中转换 manager，避免错误重试时破坏选择数据。此为原提交流程内的适配，无新增业务范围。
- V24 未被占用，使用 V24__application_url_ip_allowlist.sql。未改动旧迁移。新增隔离迁移测试和既有真实缓存 IT 增量已实现；所有实际改动文件均在已接受清单内，无跨合同偏差。

### Build validation evidence
以下为实现阶段实际运行的验证，尚未替代人工实现完整性确认和 Stage 4 正式验收。
- Boot: mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test：Commons 32、Redis 5、Gateway 67，共 104，0 failure/error/skip。日志 /private/tmp/allowlist-boot-full-test.log。
- Cloud: mvn -pl macula-cloud-api/macula-cloud-system-api,macula-cloud-system -am test -Plocal：API 19、System 199，共 218，0 failure/error，1 个已有迁移测试按条件跳过；本次隔离迁移测试随后实际通过。日志 /private/tmp/allowlist-cloud-full-test.log。
- Admin: npm run test:unit -- --run：220 passed；npm run build 成功，保留既有大 chunk 提示。日志 /private/tmp/allowlist-admin-full-test.log、/private/tmp/allowlist-admin-build.log。
- Cypress: npx start-server-and-test preview http://localhost:4173/ 'cypress run --e2e --spec cypress/e2e/application-api-key.cy.js'：3/3 passed，API 为可控 Mock；已检查桌面表单和移动端抽屉截图。日志 /private/tmp/allowlist-admin-cypress.log。
- 真实隔离 IT: 在本任务创建的临时 MySQL 8.4 和 Redis 7.4.5 中，使用随机 loopback 端口、两个空测试库执行 mvn verify -Plocal -Dit.test=ApplicationUrlIpAllowlistMigrationIT,ApplicationApiKeyIntegrationIT（仅相关模块，Surefire 同时定向16项）：Failsafe 8/8 passed；验证 V23→V24 旧逗号数据不丢失、扩列、多行 IP 持久化、真实 Mapper/Redis 继承/覆盖/清空及撤销/锁行为。日志 /private/tmp/allowlist-isolated-it.log。所有本任务临时容器已移除，没有使用业务库。
- 两仓仅本次变更/新增 Java 的 Checkstyle：0 violations；Admin 变更文件 ESLint：0 errors，961 warnings（主要为既有文件及沿用风格的 Prettier 提示，未自动格式化）；git diff --check 通过。
- 按已批准计划仅本机安装了 Boot Commons 及必要父 POM，供 Cloud 使用本轮常量和解析工具，未修改依赖版本、未发布远端。

### Remaining boundaries（实现阶段记录）
- 实现阶段结束时未更新业务数据库和运行镜像；后续用户已明确授权本地镜像更新，实际执行结果见下文。本次仅本地提交过已批准 SDLC 工件，业务源码未提交或推送。
- 网关安全链测试使用生产过滤链配合模拟基础设施；未启动部署中的网关执行真实 HTTP/代理拓扑验收。来源仍按直连对端 IP，可信代理边界未扩展。
- 用户后续回复“OK”并授权本地镜像更新；该授权不代替 PR 人工审批或远端发布授权。

## Local image refresh: 2026-11-11
用户明确回复“OK，更新镜像”，并引用 IAM ProtocolVerificationCodeController.java。本次更新范围为 Gateway、System、Admin，以及该文件所属 IAM；未扩大到其他应用或中间件。

- 本机 Maven 安装本轮 Boot Commons/Gateway 及必要父模块，将六个精确 Boot 构件写入 Docker Maven 构建缓存；原源码缓存构建因额外插件解析缓慢而停止，改用已验证的本机构件。无依赖版本变更，无远端发布。
- 使用现有 Compose Dockerfile 构建四个镜像。从未启动临时容器提取应用 JAR，SHA-256 核对三个 Java 镜像的 Commons 以及 Gateway 镜像的网关 Starter 与本次构件完全一致；IAM 包含引用的控制器。
- 按 Gateway → System → IAM/Admin 顺序，使用 up -d --no-deps --no-build --force-recreate --wait 定向重建。四个目标容器均运行最新镜像且 healthy；其余 13 个容器 ID 不变。
- System 成功执行唯一待执行的 V24 追加迁移，实际两表 allow_ips 及 access_path 均为 nullable VARCHAR(2000)。启动日志确认凭据缓存刷新；现有应用缓存 2/2、APIKEY 缓存 2/2 均包含 permitIps 字段，未读取或输出密钥值。
- HTTP 冒烟与更新前一致：Admin / 为 200，IAM discovery 为 200，Gateway /system/api/v1/menus/routes 与 System /api/v1/menus/routes 的无凭据请求均为预期 401。此为启动/未认证边界验证，不代表真实代理 IP 或每种调用凭据的端到端验收。
- 无业务源码提交、Git 推送、镜像推送、凭据轮换或其他服务重启。
- macula-cloud-gateway: sha256:5da8d23ac0bdc8551d2c5e72959b665e98caaae0dc3e47fbd71e0954766631ab
- macula-cloud-system: sha256:dc5f3eafe093a12af431f35fa0b0dc2cb76ef78ebe12fcec3fa1c7fee17476bb
- macula-cloud-iam: sha256:a91df4d2b07c26d54c28b5edb991b73d4a6a12108fce808e91f0b41e76754325
- macula-cloud-admin: sha256:608159744db563c08b42798db8022838e4f31aa8dee0076be8129a8a52d870c4
- 不含凭据的运行证据：/var/folders/bh/4nmzcd6n0dx7zgq1hq6g_d080000gp/T/allowlist-image-update-pzdcqig9/verification-summary.json
