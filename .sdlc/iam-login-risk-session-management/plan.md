# Plan: IAM 登录风险、在线会话与安全策略执行 (from spec.md 2026-11-15)

## Files that change
本计划 Status: accepted，用户明确回复 YES 批准工程实施。Cloud 根为 ../macula-cloud，Boot 根为当前 macula-boot。下列花括号逐项展开为明确文件，不代表整个目录可任意修改；新增类如需拆分或增加文件，先记录计划偏差再评估是否需要重新审批。无计划删除文件。

Cloud 文档及契约：
- .sdlc/iam-login-risk-session-management/{intent.md,spec.md,plan.md,review-findings.md,verification.md}；实现后仅 verification.md 记录实际结果。
- macula-cloud-api/macula-cloud-system-api/src/main/java/dev/macula/cloud/system/security/{IamUserIdentity.java,IamSecurityVersion.java,IamSecurityAction.java,IamRiskLevel.java,IamRiskReason.java,IamLockType.java}（新增）。
- 同 API 模块 api/{IamSecurityFeignClient.java,fallback/AbstractIamSecurityFeignFallbackFactory.java}、dto/{IamSecurityCommandDTO.java,IamSecurityStateDTO.java,IamSessionSearchDTO.java,IamSessionDTO.java,IamRevocationDTO.java,IamResourceRevocationConfigDTO.java}（新增，纯契约）。

Cloud System（Java 包根 dev/macula/cloud/system，均在 src/main/java 下）：
- src/main/resources/db/migration/V30__iam_login_security.sql（新增，实施前重查编号；账户安全状态、会话摘要、登录审计、密码历史、撤销事件/逐目标回执、资源服务器配置表，策略字典和菜单按钮）。
- pojo/entity/{SysIamUserSecurity.java,SysIamSession.java,SysIamLoginLog.java,SysUserPasswordHistory.java,SysIamRevocation.java,SysIamRevocationTarget.java,SysIamResourceRevocationConfig.java}（新增）。
- mapper/{SysIamUserSecurityMapper.java,SysIamSessionMapper.java,SysIamLoginLogMapper.java,SysUserPasswordHistoryMapper.java,SysIamRevocationMapper.java,SysIamRevocationTargetMapper.java,SysIamResourceRevocationConfigMapper.java}（新增）。
- form/{IamSecurityActionForm.java,IamMfaPolicyForm.java,IamResourceRevocationConfigForm.java,LoginRiskPolicyConfigForm.java}、query/{IamSessionQuery.java,IamLoginLogQuery.java}、vo/{IamUserSecurityVO.java,IamSessionVO.java,IamLoginLogVO.java,IamRevocationVO.java,IamResourceRevocationConfigVO.java,LoginRiskPolicyConfigResult.java}（新增）。
- service/{IamSecurityManagementService.java,IamLoginAuditService.java,IamResourceRevocationConfigService.java,PasswordPolicyValidator.java} 及各自 service/impl/同名 Impl.java（新增）。
- controller/{IamSecurityManagementController.java,IamResourceRevocationConfigController.java}（新增），controller/SecurityPolicyController.java、service/{SecurityPolicyService.java,SysUserService.java}、service/impl/{SecurityPolicyServiceImpl.java,SysUserServiceImpl.java}、vo/SecurityPolicyResult.java、listener/UserImportListener.java、enums/MessageTemplateType.java（修改）。
- src/main/resources/mapper/{SysIamUserSecurityMapper.xml,SysIamSessionMapper.xml,SysIamLoginLogMapper.xml,SysUserPasswordHistoryMapper.xml,SysIamRevocationMapper.xml,SysIamRevocationTargetMapper.xml,SysIamResourceRevocationConfigMapper.xml}（仅对应事务/分页查询需要的映射）。

Cloud IAM（Java 包根 dev/macula/cloud/iam，均在 src/main/java 下）：
- security/{IamSecurityStateRepository.java,IamSecurityPolicyReader.java,IamSecurityCoordinator.java,IamSessionRegistry.java,IamLoginAuditWriter.java,IamPasswordPolicyGate.java}、security/impl/{DatabaseIamSecurityStateRepository.java,DictionaryIamSecurityPolicyReader.java,DefaultIamSecurityCoordinator.java,RedisIamSessionRegistry.java,DatabaseIamLoginAuditWriter.java,DefaultIamPasswordPolicyGate.java}（新增）；共享数据库表以 IAM 自有投影访问，不依赖 System 实现。
- pojo/entity/{IamUserSecurityRecord.java,IamSessionRecord.java,IamLoginLogRecord.java,IamRevocationRecord.java,IamRevocationTargetRecord.java,IamResourceRevocationConfigRecord.java}、mapper/{IamUserSecurityMapper.java,IamSessionMapper.java,IamLoginLogMapper.java,IamRevocationMapper.java,IamRevocationTargetMapper.java,IamResourceRevocationConfigMapper.java}、src/main/resources/mapper/同名 XML（新增）。
- authentication/risk/{LoginRiskContext.java,LoginRiskDecision.java,LoginRiskEvaluator.java,LoginRiskProperties.java,TrustedLoginRequestResolver.java,LoginRiskAuthenticationFilter.java}、authentication/risk/impl/RuleBasedLoginRiskEvaluator.java（新增）。
- authentication/risk/geo/{IpGeolocationClient.java,IpGeolocationResult.java,IpGeolocationProperties.java}、authentication/risk/geo/impl/{IpDataCloudGeolocationClient.java,CachingIpGeolocationClient.java}（新增）。
- authentication/mfa/{MfaChallenge.java,MfaChallengeRepository.java,MfaChallengeService.java,MfaContinuationService.java,MfaContinuationProof.java}、authentication/mfa/impl/{RedisMfaChallengeRepository.java,DefaultMfaChallengeService.java,DefaultMfaContinuationService.java,RiskAwareMfaCompletionPolicy.java}、controller/{MfaChallengeController.java,IamInternalSecurityController.java}（新增）。
- authentication/mfa/{AuthenticationCompletionContext.java,MfaCompletionService.java,MfaSessionAuthenticationStrategy.java,AdditionalAuthenticationRequiredException.java}、authentication/verification/{VerificationCodePurpose.java,VerificationCodeContext.java,VerificationCodeScope.java,VerificationCodeService.java,VerificationCodeChannelService.java,redis/RedisVerificationCodeStore.java}（修改）。
- revocation/{IamRevocationService.java,IamRevocationDispatcher.java,IamRevocationSigner.java,IamRevocationRecoveryJob.java}、revocation/impl/{DatabaseIamRevocationService.java,HttpIamRevocationDispatcher.java,JwsIamRevocationSigner.java}（新增）。
- config/{IamLoginSecurityConfiguration.java,IamSecurityMaintenanceConfiguration.java}（新增），config/{MfaExtensionConfiguration.java,DefaultSecurityConfiguration.java}、config/oauth2/{ProtocolOAuth2Configuration.java,OAuth2TokenConfiguration.java,OAuth2ProtocolExtensionConfiguration.java}（修改）。
- protocol/oauth2/{ProtocolCheckingTokenGenerator.java,LocalAccessTokenIntrospector.java,ResourceTokenIntrospectionAuthenticationProvider.java}、protocol/oauth2/persistence/MaculaOAuth2AuthorizationService.java、protocol/oauth2/grant/base/OAuth2UserGrantTokenIssuer.java、protocol/oauth2/grant/externalcode/{OAuth2ExternalCodeAuthenticationProvider.java,OAuth2ExternalCodeAuthenticationConverter.java,OAuth2ExternalCodeParameterNames.java}、protocol/oauth2/endpoint/{OAuth2ExtensionErrorCodes.java,OAuth2TokenEndpointFailureHandler.java}、protocol/oauth2/token/claims/impl/DefaultAccessTokenClaimsService.java（修改）。
- handler/BrowserAuthenticationFailureHandler.java、service/account/impl/{DefaultClientAccountDetailsService.java,SysUserServiceImpl.java}、service/userdetails/SysUserDetails.java、message/template/LoginMessageConfigurationService.java（修改）。
- authentication/{email/EmailCodeService.java,sms/SmsCodeService.java}、message/dispatch/{LoginMailDispatcher.java,LoginSmsDispatcher.java}、message/audit/LoginMessageAudit.java（修改）：扩展既有投递器以受信 purpose 选择 LOGIN/MFA 场景；不新增平行供应商实现。
- authentication/session/IamHtmlSecurityHeadersFilter.java（新增）：仅为 IAM HTML 响应装配租户 contentSecurityPolicy，并覆盖登录/MFA/改密/授权页面，沿用 Security header 机制，不对 API JSON 强塞页面策略。
- authentication/session/{IamUserSessionFilter.java,IamRememberMeServices.java}、controller/IamPasswordChangeController.java（新增），src/main/resources/templates/{login.html,mfa.html,password-change-required.html}、src/main/resources/application.yml（修改/新增）；同名静态脚本仅在模板不能复用现有资源时记录偏差再添加。

Cloud Admin：
- src/api/model/system/{iamSecurity.js,iamResourceRevocation.js}（新增）、src/api/model/system/securityPolicy.js（修改）。
- src/views/system/iamSecurity/{index.vue,OnlineSessions.vue,LoginLogs.vue,RevocationOperations.vue,UserSecurityDialog.vue}（新增）。
- src/views/system/oauth2Client/components/ResourceServerSettings.vue、src/views/system/securityPolicy/index.vue、src/locales/lang/{zh-cn.js,en.js}（修改）。

Boot Gateway（模块 macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway）：
- src/main/java/dev/macula/boot/starter/cloud/gateway/security/revocation/{IamRevocationEvent.java,IamRevocationReceipt.java,IamRevocationVerifier.java,IamRevocationStore.java,RedisIamRevocationStore.java,IamAwareOpaqueTokenIntrospector.java,IamRevocationEndpointFilter.java}（新增；不依赖 Cloud DTO）。
- src/main/java/dev/macula/boot/starter/cloud/gateway/config/{IamRevocationProperties.java,IamRevocationAutoConfiguration.java}（新增），config/{GatewayProperties.java,GatewayAutoConfiguration.java}、security/ResourceServerConfiguration.java、filter/{RmOpaqueTokenEndpointFilter.java,AddJwtGlobalFilter.java}（修改，后四者按上述相同 Java 包根）。
- src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports、README.md（修改）；Cloud macula-cloud-gateway/src/main/resources/application.yml（已核实存在，修改接入配置）。
- Boot .sdlc/iam-login-risk-session-management/{intent.md,spec.md,plan.md,review-findings.md}：批准后同步对应已批准跨仓库文档，在任何 Boot 源码写入前完成本仓库工件门禁，不能绕过 hook。

测试及说明（以下文件均限定在所属模块既有 src/test/java 的对应包，前端路径另列）：
- IAM security/{IamSecurityCoordinatorTest.java,IamSecurityConcurrencyIT.java,IamSecurityRecoveryIT.java,IamSessionLifecycleIT.java,IamLoginAuditIT.java}、authentication/risk/{LoginRiskEvaluatorTest.java,LoginRiskEntryCoverageIT.java,TrustedLoginRequestResolverTest.java}、authentication/risk/geo/{IpDataCloudGeolocationClientTest.java,IpDataCloudGeolocationClientIT.java}、authentication/mfa/{MfaChallengeServiceTest.java,MfaChallengeIT.java,MfaContinuationIT.java}、revocation/{IamRevocationDispatcherTest.java,IamRevocationDeliveryIT.java}（新增）。
- IAM authentication/{email/EmailCodeServiceTest.java,sms/SmsCodeServiceTest.java}、message/{mail/LoginMailDispatcherTest.java,sms/LoginSmsDispatcherTest.java}（修改既有测试）；authentication/verification/IamVerificationPolicyIT.java、authentication/session/IamHtmlSecurityHeadersIT.java、message/audit/LoginMessageAuditTest.java（新增）。
- System service/impl/{PasswordPolicyValidatorTest.java,IamSecurityManagementServiceTest.java}、controller/{IamSecurityManagementControllerTest.java,IamResourceRevocationConfigControllerTest.java}、integration/{IamLoginSecurityMigrationIT.java,IamPasswordPolicyIT.java,IamAuditRetentionIT.java}（新增）。
- Boot security/revocation/{IamRevocationVerifierTest.java,IamAwareOpaqueTokenIntrospectorTest.java,IamRevocationMultiInstanceIT.java}、config/IamRevocationAutoConfigurationTest.java（新增）；既有 RmOpaqueTokenEndpointFilterTest.java、AddJwtGlobalFilterTest.java、GatewayPropertiesTest.java（修改）。
- 既有测试按对应修改生产类型的同路径同名 *Test.java 更新，仅本功能断言，不扩大无关测试重构；若需要新 *IT.java，在本计划补列后再添加。
- Admin tests/unit/{iam-security.test.js,iam-resource-revocation.test.js,security-policy.test.js}、cypress/e2e/{iam-security.cy.js,iam-mfa.cy.js}（新增/修改）。
- Cloud {macula-cloud-iam,macula-cloud-system,macula-cloud-api/macula-cloud-system-api,macula-cloud-gateway,macula-cloud-admin}/README.md；IAM docs/{configuration.md,third-party-integration.md}（修改）；不创建内容重复的根说明。

## Order of work
1. 确认所有路径、相邻实现/测试、模块 POM/README、CLAUDE.md（如存在）和规则；新迁移重查 V30 冲突；已核实语言文件为 zh-cn.js/en.js、Gateway 配置为 src/main/resources/application.yml。新增 Query 仅供 System Controller 使用，留在 System query 命名空间，不扩大 System API。仅当计划获人工批准后，将跨仓库已批准工件同步至 Boot。不开 fast path，不修改无关分支或部署。
2. 先定义身份复合键、版本、锁型、风险原因、管理及内部 Feign 契约；在 System 增量建表，唯一约束区分合法空 tenant 的目录，防止 MySQL NULL 唯一键绕过。加入策略配置、消息场景及菜单/按钮权限；保留现有应用及凭据。验证隔离库 V29→新版本以及重复执行/约束。
3. 实现账户权威状态、主库版本读取、禁用/锁定区分、持久化事件及逐目标状态；数据库事务和 Redis CAS 组合实现撤销，先提供可测试核心再接 Controller。所有副作用命令携带幂等 operationId；事务失败不能返回撤销成功。版本失配或恢复未完成拒绝签发/内省。全部踢出覆盖各 client、授权码、refresh、SSO、remember-me 与挑战。
4. 在既有安全策略上实现统一密码校验，覆盖创建/导入/改密/重置和历史哈希比较；补齐密码修改时间、过期及提醒。登录过期密码仅进入受限改密流程，不能创建业务 Session/token；修改后调用第 3 步全量撤销。
5. 实现可信 IP、设备标识、成功基线、分数/原因及策略版本，接入 IPDataCloud 固定 HTTPS GET 契约与可配置密钥；超时/配额/格式/未知地理处理均按 spec。只有成功完成全部认证才更新基线；主认证失败仅进入普通失败计数和 IP 限频，不让用户名触发永久锁号。
6. 在验证码内核加独立 MFA purpose 及模板；扩展现有 LoginMailDispatcher/LoginSmsDispatcher 和消息审计场景，模板场景由服务端 purpose 选择，不能让请求决定任意模板。EmailCodeService/SmsCodeService 的普通登录验证码 TTL 消费受信租户 verifyCodeValidTime，保留通道 settings 作为未配置时默认值；MFA 另取 min(verifyCodeValidTime,300 秒)。配置版本变短时读取/消费按当前有效期重新校验，不得依赖旧 Redis TTL 放行；受主认证约束的挑战绑定用户/client/目录/来源/安全版本/绑定版本。短信或邮箱任选，同通道新码可用、主登录码不可用；无绑定或投递配置缺失拒绝。实现受限 MFA 页及原事务 continuation，外部 code 不能再次核销。公共客户端使用原事务绑定的 S256 continuation proof，challenge 本身不能代替 PKCE 或产生匿名换 token 入口；必要参数按受控扩展加入原 external_code/刷新处理，不恢复已退役 grant。
7. 接入 Session 建立、SSO 复用、授权码、external_code、刷新、生成器与授权保存前后检查；一次事务复用风控结果，后续刷新/SSO 重新评估。实现闲置有效期、remember-me（0 禁用）、最大会话数超限拒绝新登录；以受信租户策略装配 IAM HTML contentSecurityPolicy，覆盖登录、MFA、受限改密和 OAuth 授权页面及第三方跳转，不能直接相信请求 tenant 指定策略。给内省结果附上身份键及 securityVersion，保持 client_credentials 原规则。
8. 实现持久化 outbox 调度、JWS 回调签名、每目标独立回执/重试/恢复；同 eventId 重试重新签发短期 JWS，避免 60 秒 exp 造成后续补偿永远失败。仅发已登记允许的 HTTPS 回调地址，禁重定向与任意用户提供地址；私有部署目标由管理员登记和服务网络访问策略约束，用户查询参数不能改变目标。
9. 实现 Boot 可覆盖/可禁用的接收自动配置，可信 JWKS、typ/aud/算法/有效期校验，版本先写共享状态后清缓存，乱序取最大值。用户 active 缓存默认 60 秒、0–300 秒、上限不超过 token 到期，TTL 从内省开始计、命中不续期；缓存读/回填均校验版本。保留旧 HMAC 清 token 接口。Cloud 显式启用新协议；未启用的其他 Starter 使用方保持原行为并标记未接入，不能计入全量收敛证据。
10. Boot 目标测试通过后仅安装本地 Maven 制品供 Cloud 使用，不发布或变更远端仓库；保持 revision 机制，不随意升级其他依赖。Cloud Gateway 配置新协议，并以两实例验证共享状态/逐目标回执、一个离线与迟到内省；所有实例完成能力验证才标为同步成功。
11. 实现 System 管理 API 及 Admin 三页签、详情与动作权限、token 密码框 POST 查询、资源服务器登记回调配置与新风险策略 UI。服务端保证授权与租户隔离，前端禁用按钮不能替代权限校验。启用 MFA 自动全部踢出，启用账户不解锁。
12. 登录审计和 outbox 在关键路径确定结果后保存，日志写失败不能无提示声称审计完整；定义可恢复状态并按关键安全操作失败关闭。UTC 三日历月截止每天分批清理审计，不删未完成 outbox；会话状态按有效性而非最后一条日志判定。更新各模块 README、配置/接入文档、迁移与回滚说明。
13. 执行 Proof 的相关验证，修复失败并记录实际命令、数量、跳过和限制到 verification.md；完成后进入 sdlc-test 正式验证，不把计划中的测试当已通过。源码完成再请求实现完成确认，不自行进入部署/PR。

## Risks
- 用户接受资源服务器部分不一致：callback 未到时默认最多 60 秒、配置上限 300 秒；缓存不能 stale-if-error/滑动续期。第三方未实现协议不承诺时间上界，未接入/失败状态持续可见；仅平台 stub 测试不能代替外部联调。
- 数据库主库安全状态为权威；Redis 恢复、事务回滚、并发刷新/签发/撤销必须可重复证明。版本不得 ABA/重置；不得把锁状态误当用户 status。已开始的业务请求不强制终止。
- 用户已选择同通道新验证码可做再次验证，不宣称独立因子。密钥缺失、邮箱/手机均未绑定、消息供应商失败会拒绝该次认证；首次新设备或地理未知可能要求 MFA。
- IPDataCloud 仅用用户指定归属地产品；不自动查询真实用户 IP，不编造产品 key。真实供应商、IPv6、所购产品字段与额度须部署前联调，当前 contract fixture 不代表真实调用。URL query 含 key，HTTP trace/APM/异常必须脱敏。
- 密码无存量迁移但现有应用/身份绑定必须保留；不得重设现有数据作为测试准备。新密码历史/过期状态不猜历史值。用户数据修改只在隔离测试库验证。
- 多租户/多目录复合键、NULL tenant 的唯一性、ROOT 权限及 userId 重合必须测试。token 搜索不得将凭据写入 URL、访问日志或前端持久存储。
- Boot 原行为兼容与 Cloud 新行为必须区分，安装本地依赖后检查 Cloud Gateway 实际打包的 SDK；远端制品未发布不声称所有外部资源服务器可直接升级。
- 只增量迁移不修改旧 Flyway。应用回滚先停入口并撤销新授权、保留审计/outbox及版本状态；旧版本不识别 securityVersion 时不能接流量，需前滚修复。不执行真实环境迁移、部署或回滚。

## Proof
- R1/R6：IamSecurityManagementControllerTest、IamSecurityManagementServiceTest 验证 token/用户名搜索、每个动作授权/跨租户拒绝、禁用/锁定独立；iam-security.test.js 与 iam-security.cy.js 验证全部客户端影响提示、空态/错误/加载/重复点击、禁用/启用/MFA 操作及 token 不落路由/存储。
- R2：IamLoginAuditIT、IamAuditRetentionIT 验证主认证失败、MFA、中高风险、签发 client_id 的时机、原始 IP/UA/设备及登录结果，UTC 月末三日历月删除边界、未完成 outbox 保留，应用日志不泄露密码/token/验证码/地址密钥。
- R3/R4：LoginRiskEvaluatorTest 覆盖 29/30/69/70、城市/国家不重叠、失败窗口和普通临时锁；LoginRiskEntryCoverageIT 覆盖密码/短信/邮箱/第三方/端内/SSO/授权码/external_code/刷新及 client_credentials 不受用户风险限制；IamSecurityRecoveryIT 验证高风险永久锁重启不丢、自动解锁不能解除。
- R5：MfaChallengeServiceTest、MfaChallengeIT、MfaContinuationIT 覆盖一个通道可用、均未绑定拒绝、同通道新码、purpose 隔离、5 次上限/间隔/有效期、重发旧码、双消费、版本/绑定变更、CSRF、来源失效、跨 client/PKCE/proof 拒绝、平台 code 只能核销一次、未完成不签发。iam-mfa.cy.js 覆盖托管登录挑战和恢复。
- R6：IamSecurityConcurrencyIT、IamSessionLifecycleIT、IamRevocationDeliveryIT 和 IamRevocationMultiInstanceIT 用隔离 MySQL/Redis、两个资源实例、HTTP stub 验证签发/刷新/SSO/remember-me/挑战与踢出并发、JWS 过期后重试重签、错 aud/typ/签名、乱序/重放幂等、离线重启补偿、单 200 不能冒充全部完成。
- R6 缓存：IamAwareOpaqueTokenIntrospectorTest 验证默认 60、0/300 上下限、绝对到期/不滑动、迟到内省不能回填旧版本、IAM 不可达仅有效缓存可用、过期拒绝、JWT 内省路径不靠长寿命绕过。IamRevocationAutoConfigurationTest 验证开/关/替换 Bean/classpath 条件及 imports；保留旧 HMAC/API Key/非用户 token 回归。
- R7：PasswordPolicyValidatorTest、IamPasswordPolicyIT 覆盖各复杂度枚举、长度/弱密码/账号/连续重复/连续序列/历史哈希/有效期/提醒，创建/导入/重置/修改均调用，同事务更新与改密全踢。IamLoginSecurityMigrationIT 验证 V29 增量升级、索引/NULL tenant 唯一性、既有应用/凭据/身份绑定不变。
- R7 通用与防御策略：IamVerificationPolicyIT 验证普通短信/邮箱验证码使用租户 verifyCodeValidTime、租户隔离、配置调短后旧码过期、未配置回退、MFA 300 秒上限；投递器与 LoginMessageAudit 测试验证 LOGIN/MFA 模板隔离及请求不能改写 purpose。IamHtmlSecurityHeadersIT 验证 contentSecurityPolicy 生效、租户隔离、未知租户拒绝及登录/MFA/改密/授权页和第三方跳转兼容，JSON API 不误用页面 CSP。IamSessionLifecycleIT 用可控时钟验证闲置到期/活动刷新、rememberMeValidTime=0/到期/撤销、sessionMaximum 的 1/N/N+1 与并发建立、策略缩短及跨租户计数边界。
- 外部契约：IpDataCloudGeolocationClientTest 使用纯解析/模拟传输；IpDataCloudGeolocationClientIT 使用 HTTP stub 验证 URI 编码、IPv4/IPv6、数字/字符串 code=200、data.location 映射、业务失败/超时/未知字段、无效/缺失地理和凭据脱敏。禁止把这些结果写成真实供应商联调通过。
- 命令：Boot `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test`，相关 IT 用 verify；Cloud `mvn -pl macula-cloud-api,macula-cloud-system,macula-cloud-iam,macula-cloud-gateway -am test -Plocal`，相关 IT 用 verify，再执行跨服务全仓 test/verify。Admin 相关 Vitest、npm run build、相关 Cypress；所有新增修改 Java 检查版权头/Javadoc/Checkstyle，最后 git diff --check。
- 所有外部测试先检查运行条件；运行日志/报告与正式 sdlc-test 的 coverage、第三方实测、性能证据分开。记录默认/关闭缓存及地理超时的 p95/p99，不捏造 SLO 或以本地指标替代生产承诺。
