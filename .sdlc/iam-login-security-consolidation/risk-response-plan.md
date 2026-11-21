# Plan: 风险处置与认证因素配置（from risk-response-spec.md及totp-webauthn-scope.md，最终因素范围）

> 菜单最新修正（2026-11-21）：通用安全、密码策略独立；多因素认证菜单下使用“MFA策略”“多因素认证配置”两个Tab。密码策略按用户区分，具体增量见security-settings-structure.md。


> 当前范围（2026-11-21）：接入应用暂不增加MFA配置；平台管理因素，统一风险/账号/敏感场景决定认证要求。文末最新用户修正优先于此前应用级MFA扩展建议。

Status: accepted

用户明确取消旧配置保留，并将MFA限定为因素启用/组合与EMAIL、SMS、TOTP、PASSKEY具体配置和用户合格集合内自主选择。此版替换上一版实施方案，不再沿用旧配置映射及legacy源。沿用现有Java17/Spring Security、MyBatis、Redis、Vue3/Element Plus和投递设施，不新增认证协议或脚本引擎。原主任务其他范围继续按原accepted plan执行。

## Files that change
路径相对macula-cloud，API=macula-cloud-api/macula-cloud-system-api/src/main/java/dev/macula/cloud/system/，I=macula-cloud-iam/src/main/java/dev/macula/cloud/iam/，S=macula-cloud-system/src/main/java/dev/macula/cloud/system/。大括号只展开所列文件；本次追加Boot父BOM二维码依赖管理及Gateway固定TOTP协议，范围见下文。

- API`security/protection/{LoginProtectionPolicy.java,AuthenticationFactor.java,RiskLevel.java,RiskAction.java,AuthenticationStrength.java,RiskResponsePolicy.java,AuthenticationPolicy.java,AuthenticationFactorPolicy.java,AuthenticationCapability.java,EffectiveAuthenticationPolicy.java,AuthenticationPolicyDefaults.java}`：MFA配置只含因素开关、适用主认证方式/场景/用户范围、绑定要求、组合及因素参数；渠道/模板关联仅作为只读VO状态，不进入可写配置。删除旧MfaMode和风险触发字段，不新增LegacyAuthenticationPolicyAdapter。
- `macula-cloud-system/src/main/resources/db/migration/V38__iam_authentication_policy.sql`：新平台风险/因素单例、不可变版本、一次性预览和脱敏动作审计；不含LEGACY/MANAGED来源字段，不复制旧policy_json。V38以纯新默认文档初始化version=1、draft_version=1、active_version=1，随后管理员修改才进入草稿/预览/启用；部署activate开关不阻断初始有效版本读取。新增明确的新默认配置（LOW=ALLOW、MEDIUM=REQUIRE_AUTH、HIGH=BLOCK；SMS/EMAIL初始允许、TOTP/PASSKEY初始禁用直到平台设施就绪并显式启用；四类都需本人绑定且实际可用，所有组合仍需绑定/独立性及设施可用检查），不宣称保留旧策略行为。迁移脚本清理sys_iam_mfa_policy及固定type_code=system_security_policy、name=loginProtectionMfa的退役键（不按包含mfa的模糊条件删除）；不清理MFA事件、用户安全状态、密码、绑定、供应商配置/秘密。V38若已占用须先更新计划，不改V31–V37历史脚本。
- S`controller/AuthenticationPolicyController.java`、`service/AuthenticationPolicyService.java`、`service/impl/AuthenticationPolicyServiceImpl.java`、`mapper/AuthenticationPolicyMapper.java`及`resources/mapper/AuthenticationPolicyMapper.xml`。
- S`form/{AuthenticationPolicySectionForm.java,AuthenticationPolicyActivationForm.java}`、`vo/{AuthenticationPolicyVO.java,AuthenticationPolicyPreview.java}`、`security/AuthenticationCapabilityEvaluator.java`。
- S`controller/MfaPolicyController.java`退役旧合同，明确返回410/新入口提示；S`service/{LoginProtectionPolicyService.java}`、`service/impl/LoginProtectionPolicyServiceImpl.java`、`mapper/MfaPolicyMapper.java`及`resources/mapper/MfaPolicyMapper.xml`删除旧全局JSON读写，保留并改为新因素能力投影查询。S`controller/SecurityPolicyController.java`仅澄清租户评分与平台处置边界，原租户risk接口不写平台因素/处置。
- S`config/{AuthenticationPolicyProperties.java,AuthenticationPolicyConfiguration.java}`、`macula-cloud-system/src/main/resources/application.yml`、`deploy/{README.md,.env.example,docker-compose.yml,scripts/check-authentication-policy-rollout.sh}`：默认关闭的activate部署开关及只读全实例/配置就绪检查；脚本不得修改环境、启动容器或迁移真实库。
- I`mapper/AuthenticationPolicyMapper.java`、`resources/mapper/AuthenticationPolicyMapper.xml`；I`mapper/MfaPolicyMapper.java`及其XML删除旧配置读取映射，不再被运行链装配（确认无其他用途后移除文件）。
- I`security/protection/{AuthenticationPolicyReader.java,RiskResponseService.java,RiskResponseDecision.java,AuthenticationFactorPolicyReader.java,MfaPolicyService.java,MfaDecision.java,LoginProtectionPolicyReader.java,AuthenticationEvidenceStore.java,MfaRiskSnapshot.java,PolicyEvaluationContext.java,AuthenticationPolicyFence.java,AccessTokenPublicationStore.java}`。
- I`security/impl/DefaultIamSecurityCoordinator.java`、`authentication/mfa/impl/{DefaultMfaChallengeService.java,DefaultStepUpService.java}`、`security/{MfaEvidenceReader.java,AuthenticationEvidence.java}`、`authentication/mfa/MfaFreshnessService.java`、`protocol/oauth2/stepup/OAuth2MfaCheckService.java`、`security/account/AccountSecurityService.java`：统一风险处置→所需认证→因素选择/执行。移除散落HIGH拒绝、MFA风险触发开关和旧策略读取；账号个人安全要求独立。
- I`security/protection/MessageSecurityGuard.java`与`authentication/verification/VerificationCodeSettings.java`：MFA用途读取新因素的验证码参数；使用同一原子校验器、人机准入和发码预算。非MFA用途继续自己的有效配置，公共投递/反滥用预算不能新增计数来源。短信/邮件因素设置与租户公共预算取实际更严约束，并展示实际生效参数。
- `macula-cloud-admin/src/api/model/system/{authenticationPolicy.js,mfaPolicy.js,securityPolicy.js}`；`src/views/system/securityPolicy/{index.vue,components/RiskResponseSettings.vue}`；`src/views/system/mfaPolicy/{index.vue,components/AuthenticationFactorSettings.vue}`。
- `macula-cloud-admin/src/api/model/system/messageSettings.js`与`src/views/system/mfaPolicy/components/AuthenticationFactorSettings.vue`复用消息渠道读/模板/脱敏状态API，固定展示实际SMS→SMS_LOGIN及EMAIL→MAIL_LOGIN关联（投递Channel为MAIL，认证Factor为EMAIL，显式适配），不提供可编辑渠道/模板选择字段；凭据或供应商编辑使用原消息管理入口和权限，不在MFA角色下新增读取秘密或改投递基础设施的旁路。
- `macula-cloud-admin/src/locales/{riskResponse.js,mfaPolicy.js}`、`src/locales/lang/{zh-cn.js,en.js}`；`macula-cloud-iam/{README.md,docs/architecture.md,docs/configuration.md}`、`macula-cloud-system/README.md`、`macula-cloud-admin/README.md`更新唯一新合同、配置职责、准备条件和不兼容切换要求。
- S`cli/AuthenticationPolicyPreflight.java`新增仅供部署准备的离线只读入口，`src/test/java/dev/macula/cloud/system/cli/AuthenticationPolicyPreflightTest.java`验证无写入及缺数据拒绝；复用AuthenticationPolicyDefaults与同一AuthenticationCapabilityEvaluator及Mapper的能力投影，不启动Web/Spring Boot业务上下文、Flyway、Nacos初始化或定时任务，不提供HTTP管理旁路。只从受控部署环境读取已确认数据库连接、显式操作者identity清单；输出聚合就绪/缺失原因，不输出凭据或联系方式。
- IAM`src/test/java/dev/macula/cloud/iam/support/MfaPolicyTestSupport.java`改写为新因素/风险模型fixture；`security/protection/PlatformMfaPolicyReaderTest.java`退役旧Mapper/构造器用例，合并到AuthenticationPolicyReaderTest；System`integration/MfaPolicyMigrationIT.java`改为历史V33局部SQL快照及V38删除边界测试，不依赖旧运行Mapper。
- IAM新增`src/test/java/dev/macula/cloud/iam/security/protection/{RiskResponseServiceTest.java,AuthenticationPolicyReaderTest.java,AuthenticationFactorPolicyReaderTest.java,AuthenticationPolicyFenceTest.java}`；修改现有`MfaPolicyServiceTest.java`、`MessageSecurityGuardTest.java`、`security/IamSecurityCoordinatorTest.java`、`security/account/AccountSecurityServiceTest.java`、`authentication/mfa/{MfaChallengeServiceTest.java,StepUpServiceTest.java,MfaFreshnessServiceTest.java}`、`protocol/oauth2/stepup/OAuth2MfaCheckServiceTest.java`、`security/AccountSecurityPersistenceIT.java`。
- System新增`src/test/java/dev/macula/cloud/system/{service/impl/AuthenticationPolicyServiceImplTest.java,controller/AuthenticationPolicyControllerTest.java,security/AuthenticationCapabilityEvaluatorTest.java,config/AuthenticationPolicyConfigurationTest.java,integration/AuthenticationPolicyMigrationIT.java}`；修改`service/impl/MfaPolicyServiceImplTest.java`，按新因素合同验证旧API410，不保留旧JSON接口用例。
- Admin新增`tests/unit/risk-response.test.js`、`tests/unit/authentication-factor-settings.test.js`、`cypress/e2e/risk-response-policy.cy.js`；修改`tests/unit/{mfa-policy.test.js,security-policy.test.js}`、`cypress/e2e/mfa-policy.cy.js`。两仓同slug的spec/plan/review/build-notes/verification记录同步本修订。


### TOTP、用户选择与UV Passkey的精确实施增量
- Boot相对路径：`macula-boot-parent/pom.xml`统一管理ZXing core/javase 3.5.3（本地缓存及官方发布已核对），Cloud `macula-cloud-iam/pom.xml`仅声明实际需要的二维码依赖；TOTP算法复用已管理Hutool5.8.47的TOTP/HOTP generate，不手写HMAC/截断。新增依赖不扩散到其他Starter。
- IAM `authentication/totp/{TotpCredentialService.java,TotpVerifier.java,TotpCredentialRepository.java,TotpProtocol.java,TotpVerificationResult.java}`、`authentication/totp/impl/DatabaseTotpCredentialRepository.java`；I`mapper/TotpCredentialMapper.java`及`resources/mapper/TotpCredentialMapper.xml`，I`controller/TotpCredentialController.java`、I`config/AccountSecurityConfiguration.java`、I`authentication/hosted/LoginRequestMatchers.java`（精确个人端点与CSRF/owner边界），I`security/account/FactorManagementGuard.java`，I`config/TotpConfiguration.java`。接口与实现分包，必需依赖final构造器注入。
- System `src/main/resources/db/migration/V39__iam_totp_credentials.sql`：owner固定identity、随机credentialId、encrypted_seed/keyId、seed_version、row_version、label、PENDING/ACTIVE/REVOKED状态、pending_expires_at、last_accepted_step及审计时间。首期每身份最多一个ACTIVE绑定，旧撤销行保留；在账号锁内处理pending替换/过期，不能覆盖已生效seed。last_accepted_step与凭据状态/安全版本、认证证据在同一SQL事务校验/推进；适当唯一索引防并发双激活。V39同步把V37的因素/独立性CHECK扩为TOTP（PASSWORD+TOTP或已验证UV的PASSKEY等允许组合），不改既有证据数据或放宽UV要求。密文AAD绑定identity/credentialId/seed_version，命名只改row_version，不改变seed的AAD。实施前复核V39未占用，禁止改旧已执行迁移。
- 固定TOTP互通：HMAC-SHA1、6位ASCII数字（保留前导零）、30秒、至多±1步。服务端生成至少160bit密码学随机seed，Base32登记。Hutool只负责算法生成；应用返回实际匹配时间步并原子CAS推进last_accepted_step，不能仅调用布尔validate就宣称防重放。计数/时钟由服务器固定值决定，不接受客户端timeStep/window/seed参数。
- seed只使用现有AuthenticatedSecretCipher的独立部署keyring加密，DB/Redis均不明文保存。缺密钥时TOTP标记未配置并拒绝登记/核验，不令无TOTP要求的整个IAM无条件启动失败。TOTP配置引用主计划已有秘密密钥装配，不复用字段加密密码，不生成真实部署密钥。
- 本人API固定`/api/v1/me/totp`：GET只返回状态/名称/时间；POST `/enrollments`生成5分钟pending并一次性返回本地二维码和人工录入材料；POST `/enrollments/{id}/confirm`提交code确认，PATCH `/{credentialId}`改名，DELETE `/enrollments/{id}`取消，DELETE `/{credentialId}`撤销。除只读状态外均要求Session+CSRF和owner绑定的近期强身份复核，复用主计划因素注册边界（PASSWORD+已绑定OTP或既有UV Passkey）；没有独立证明走既有受控恢复，不接受匿名新绑定。
- URI和二维码只用于受控登记：otpauth URI按标准含secret，属于一次性响应/本地二维码材料；不得把它放到HTTP URL/query、外部二维码服务、访问日志或日常凭据列表。响应Cache-Control:no-store，日志/toString/审计不展开seed/二维码/验证码。启用后不能再次查询seed或二维码；登记材料仅经HTTPS或显式受信localhost开发环境返回。
- 首次confirm只确认登记、不自行签发Token或通用MFA通过证明，同时消费该TOTP时间步，不能拿同一码立刻再完成另一个challenge；UI提示等待下一个码。撤销/重绑必须按账号锁、近期身份复核与最后可用强因素保护执行，同事务推进安全版本并撤销受影响成果；已有用户/供应商凭据不清理。
- I`security/protection/MfaAttemptBudget.java`、`authentication/mfa/MfaVerificationLimits.java`：EMAIL/SMS/TOTP/PASSKEY的实际核验共用原owner/challenge总体尝试预算，切换因素、请求发码或申请WebAuthn options不创建新的认证challenge或重置核验次数。options/发码另有有界准入，取消系统对话框不当作核验成功；不自动重放原业务写请求。
- I`authentication/mfa/MfaChallengeService.java`、I`security/protection/MfaStepUpService.java`、I`controller/MfaChallengeController.java`、I`protocol/oauth2/stepup/{OAuth2MfaEndpoints.java,OAuth2StepUpController.java}`增加固定TOTP核验适配。登录路径`POST /api/v1/mfa/challenges/{id}/totp/verify`，敏感路径`POST /oauth2/mfa/challenges/totp/verify`；只接受原challengeId和code，owner来自原Session/原Bearer，不提供send动作。TOTP结果使用AuthenticationFactor.TOTP，消息Channel保持SMS/MAIL/EMAIL现有投递合同不变。
- Boot Gateway（模块`macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/`）明确修改`src/main/java/dev/macula/boot/starter/cloud/gateway/security/mfa/{MfaFactor.java,MfaControlEndpoints.java,MfaChallengeRequest.java,MfaChallengeResponse.java}`、`security/mfa/impl/HttpMfaChallengeClient.java`、`filter/MfaChallengeWebFilter.java`及README；固定`POST /gateway/mfa/challenges/totp/verify`只收id/code，原Bearer恢复与代理/响应白名单不变。对应`src/test/java/.../security/mfa/{MfaFactorTest.java,HttpMfaChallengeClientTest.java}`及`filter/MfaChallengeWebFilterTest.java`同步验证，不增加业务URL授权或新的Token。
- 用户选择：RiskResponsePolicy每档/场景固定action、allowedFactors、minimumStrength、maxEvidenceAgeSeconds；AuthenticationFactorPolicy配置enabled/allowedPrimaryFactors/scenes/userTypes、绑定要求、SMS/EMAIL验证码参数、TOTP固定互通配置、PASSKEY要求UV及可信RP配置状态。验证码参数仍受公共反滥用guard更严格约束。挑战只返回平台启用/适用∩风险允许∩本人绑定有效∩设施/浏览器可用且强度/独立性合格的因素，多种可选/切换、单种直接展示、空集合拒绝。接口伪造factor不能绕过后端重算与上下文绑定。
- UV Passkey是可满足多因素强度的认证方式：当前策略允许PASSKEY，证据真实UV、owner/应用/设备/认证会话/版本/环境及新鲜度都符合时，密码后补验或Passkey主登录均可直接满足，不再追加短信或重复调用Passkey。只有UP/签名无UV不得满足此强度；强度比较不能绕过策略明确限定的因素，也不能绕过BLOCK/账号硬拒绝/业务权限。由主计划官方WebAuthn链产生结果，不能用客户端uv=true生成证据。
- I`security/protection/AuthenticationEvidenceStore.java`统一recordVerifiedFactor（含原PolicyEvaluationContext、factor、真实完成时间、服务器UV/认证来源），不把TOTP/PASSKEY伪装为Channel；I`security/MfaEvidenceReader.java`和新因素/能力模型使用AuthenticationFactor。密码/OTP/第三方主认证类别与独立性按原可信结果分类，SMS/EMAIL/TOTP两个持有通道不自动构成独立MFA。
- IAM `src/main/resources/{templates/account-security.html,templates/mfa.html,static/iam/account-security.js,static/iam/mfa.js,static/iam/totp.js}`增加TOTP绑定确认/状态/命名/取消/撤销、四因素选择和差异交互；EMAIL/SMS有发码、TOTP只有动态码输入、PASSKEY调用浏览器。Admin主计划已有`src/components/Security/StrongAuthenticationDialog.vue`、`src/api/iamStrongAuthentication.js`及`tests/unit/admin-strong-authentication.test.js`一并纳入本增量，消费同一因素列表和固定端点，不创建第二套策略。
- WebAuthn登记、登录、补验与个人管理继续按原主计划I WebAuthnConfiguration/官方operations/仓库及passkeys.js等精确文件集实施，不按Apple/Windows/Android新增三套服务端认证协议。用户自主注册启用，平台开放不自动创建凭据；RP/Origin由可信部署配置，HTTPS/显式localhost边界不变。配置未就绪显示状态，不能假装已支持或强制用户绑定不可用因素。
- 追加IAM测试`src/test/java/dev/macula/cloud/iam/authentication/totp/{TotpVerifierTest.java,TotpCredentialServiceTest.java,TotpCredentialRepositoryIT.java}`、`controller/TotpCredentialControllerTest.java`、`security/account/FactorManagementGuardTest.java`、`security/protection/MfaAttemptBudgetIT.java`；System `src/test/java/dev/macula/cloud/system/integration/TotpCredentialMigrationIT.java`。Admin新增`tests/unit/iam-totp.test.js`、`cypress/e2e/totp-enrollment.cy.js`；修改既有`tests/unit/{iam-account-pages.test.js,iam-passkeys.test.js}`、`cypress/e2e/passkeys.cy.js`。测试路径中父计划尚未落地的文件按新增处理，不能因空测试文件名声称已有覆盖。

### 最终因素审查修正：跨挑战预算与登记上下文
- MfaAttemptBudget以Redis服务端TIME为准，在同一owner hash-slot的Lua内预检并原子预扣所有适用预算：单challenge总核验默认5次（沿challenge TTL）、跨challenge/Session/来源的owner总核验默认10次/滚动300秒；TOTP实际credential或pending确认默认5次/滚动300秒（且不超过pending TTL）。owner键不包含challenge、来源、Session、安全版本或可被切换重置的字段；credential/pending预算不能代替owner预算。所有因素切换都共享owner/challenge计费，TOTP额外满足凭据预算。
- 滚动窗口用有界有序集合及随机服务器attemptId记录已准入尝试，所有适用窗口预检成功后才一起预扣；TTL与窗口绑定，集合大小不超过上限。拒绝请求不无限延长锁定时间；返回429及真实Retry-After。预算设施不可用返回503，不能免计或回退。请求预扣成功后不因验证码失败、SQL/网络/设施异常退费，成功也不清零；同一服务器核验调用只计一次，不在Filter/Service重复计同维度次数。窗口/上限集中于MfaVerificationLimits及强类型约束，不接受HTTP修改。
- TOTP enrollment创建/重建另限owner每滚动600秒最多5次，confirm纳入同一owner核验预算及pending最多5次；取消/过期/重开pending、替换凭据均不清空owner预算。原子预算在解密seed/运行核验算法前执行，owner来自原受信会话或challenge，不让匿名请求提交owner来消耗他人预算。原消息投递预算继续单独负责send，不能再复制计数来源；未执行核验的近期合格证据复用不预扣核验预算。
- pending登记持久化完整服务器EnrollmentContext：identity/securityVersion/authenticationId/device、原Session绑定摘要、可信来源client/用途TOTP_ENROLL、BIND/REPLACE意图、当前有效平台/租户策略摘要、创建/到期时间、替换时原activeCredentialId及rowVersion。可嵌入同row的固定类型JSON及摘要；HTTP不能自报这些字段或替换seed。
- create取得近期强复核后捕获上下文；confirm/取消/替换必须核对原owner、安全版本、未撤销原认证会话、相同Session/应用/用途、策略摘要与TTL，再在统一策略→租户→owner锁内检查pending状态/rowVersion及原active快照，变化则拒绝并重新登记，不能用当前会话重标旧pending。新建登记可以经当前强复核原子终止旧pending并创建全新seed/上下文，旧confirm不得成功；不复用旧seed或修改其归属。
- 追加MfaAttemptBudgetIT覆盖多节点并发、不同challenge/Session/敏感来源、因素切换、同owner更换credential/pending、失败及设施异常计费、窗口边界/TTL和Retry-After；TotpCredentialRepositoryIT及TotpCredentialControllerTest覆盖confirm失败预算、跨Session/应用/用途、原会话撤销、策略/安全版本变化、pending替换和确认竞争。局部模拟与真实Redis/MySQL结果分开记录。

## Order of work
1. 按用户明确修正更新设计与计划，独立审查新方案并获得具体计划批准；此前旧方案审查结论不冒充本版已审。记录用户未提交基线，不先写源码。
2. 定义新风险处置和因素模型、固定默认配置及独立因素矩阵；无旧模式转换、不保证旧策略行为，缺新配置/损坏配置失败关闭。版本/ID用字符串传给前端，不丢精度。
3. 实现TOTP凭据/登记确认/原子防重放与近期身份守卫，以及新平台草稿、预览、启用、权限/CAS和能力预检；V38/V39在隔离MySQL中演练。SQL仅清理退役策略，不删除真实账号/消息凭据/因素绑定/历史审计；真实库依然不执行。
4. 冻结API：`/api/v1/iam-authentication-policy` GET；PUT `/risk`和`/factors`各只保存本分区，要求expectedDraftVersion；POST `/preview`绑定owner稳定identity/认证会话、draftVersion、draftDigest、activeVersion/activeDigest，TTL5分钟；POST `/activate`要求同owner的一次性previewId及版本。服务端锁内重查权限、状态、因素能力及操作者路线；未预览/过期/重放/跨owner/版本变更拒绝，冲突409；预览消费、有效指针和审计同事务提交。没有legacySourceDigest或旧源切换。
5. 复用`system:iam-mfa:read/save`平台权限；消息供应商/秘密沿用原权限，不让旧租户安全策略权限获得平台配置权限。分区保存以锁内最新草稿合并，避免风险/因素表单互相覆盖。
6. 在统一类型/能力/证据链纳入TOTP和真实UV Passkey，接IAM唯一执行链：风险/场景策略输出动作、allowedFactors、minimumStrength、证据时限和复用约束，MFA只按因素配置筛选/执行。风险ALLOW不解除账号/敏感接口要求，BLOCK与账号硬拒绝优先。企业通用必须认证规则在风险/场景分区，MFA没有强制触发范围或风险开关。
7. 完成个人TOTP/WebAuthn管理及用户挑战方式选择；短信/邮件/TOTP分别显示启用、主认证方式适用、绑定要求、OTP参数、只读渠道/模板关联及实际可用性，渠道/模板关联只读且编辑走原消息管理入口；Passkey显示启用、UV、可信RP/Origin状态和注册/管理入口。按现有投递设施实现，MFA不存第二份AK/SK/邮箱秘密。验证码参数经唯一guard生效，公共预算只计一次。Passkey实现依赖主计划官方链，未完成时只读显示未就绪，不能启用依赖它的强制策略。
8. 因素预检统一使用启用应用允许的主认证路线、已绑定因素、设施可用性、UV强度和作用范围；操作者与受影响人群同一规则，游标批次≤500，只返回布尔能力/聚合原因，不返回密码哈希或完整联系方式。SMS-only+SMS及SMS+EMAIL不合格；PASSWORD+绑定OTP可合格；未实现/缺UV Passkey不合格。无可用操作者路线阻止启用，不能因联系人存在就显示可用。
9. PolicyEvaluationContext由服务端捕获owner/安全版本/设备/认证会话/应用/场景、平台有效版本及摘要、租户评分快照、认证要求和风险情境；评估→挑战→核验→证据写入→成果发布使用同一上下文。新平台策略→租户风险记录/缺失键范围→owner统一锁序，证据与发布在实际事务内复核；配置改变则拒绝旧结果，不把旧验证重标为新策略。同情境补验后允许复用，不每请求更新起点，不因草稿变更失效已有证据。
10. 在停止旧服务或应用迁移之前，check-authentication-policy-rollout.sh的preflight阶段调用候选新System产物的离线只读入口，使用与V38种子完全一致的新默认文档，核对明确指定的平台操作者存在至少一种可登录路线、独立因素、绑定及配置就绪状态。不读旧MFA JSON、不映射旧模式；预检不依赖尚未迁移的新策略/凭据表，V34不存在的TOTP/Passkey表只计未绑定/未就绪，不计为可用。未就绪在停机/迁移之前中止，不放宽新规则。记录候选产物/新默认文档摘要及操作者就绪聚合，数据改变则切换前重跑。
11. 风险/MFA页面按新合同重做，旧接口返回410；更新i18n、README和测试。验证新默认配置及至少一个平台操作者的有效主认证+独立因素路线后，才允许安排不兼容切换。
12. 执行开发验证并记录证据；新版本需协调停止旧IAM/System后再按单一合同切换。System部署级`IAM_AUTHENTICATION_POLICY_ACTIVATION_ENABLED`默认false，HTTP不能修改；关闭时后续activate返回409/POLICY_ROLLOUT_NOT_READY，但新IAM仍可读取V38已初始化的active_version=1完成登录；GET/首次读取不临时生成默认策略。真实迁移/清理旧表、启用新策略、环境变量变更、容器重建及新镜像均须分别获得对应授权，当前不执行。

## Risks
- 这是用户明确选择的不兼容配置替换，不保留旧行为、旧读写接口、旧策略映射或回退。V38清理旧策略属于计划内容，不是本轮执行真实库删除的授权。部署前需备份用于故障恢复，不将备份当运行兼容层。
- 新默认配置可能与旧有效配置不同；部署前按新规则检查因素设施、绑定和操作者可登录路线。缺就绪条件时停止切换，不能临时放宽新策略解决自锁；部署初始化/切换材料须写明实际新配置并另行确认。
- 回滚只能协调应用与配置的受控恢复；不能单独回滚旧IAM继续读已退役表，不能恢复被撤销凭据。未授权时不操作真实环境；缺独立部署密钥/RP/Origin时保持相关因素未就绪，不擅自生成密钥或登记生产凭据。
- 短信/邮件配置是认证因素对现有消息能力的只读关联，平台MFA权限不扩张成消息供应商管理权限。具体渠道来源仍使用原租户/平台边界，HTTP不能选择他人的租户/目标或任意供应商URL。
- 旧审查中关于证据上下文、能力预检、版本精度和默认关闭部署门禁的原则继续适用；关于legacy源/旧新映射的结论作废，待本版独立审查。

## Proof
- 三档动作与因素启用/场景/主认证方式/绑定/设施/独立性矩阵；MFA页没有风险触发或ALWAYS/RISK/OFF设置，风险页能配置认证处置，业务拒绝优先。
- 短信/邮件参数实际生效、统一预算一次计费；只读SMS_LOGIN/MAIL_LOGIN关联与当前Dispatcher一致，HTTP伪造渠道/模板选择字段拒绝，原消息设置修改后实际投递及MFA展示读取同一配置与原权限；短信/邮件投递失败不生成认证成果；Passkey未就绪/无UV拒绝，不冒称可用。
- 首次启动按active_version=1读取纯新默认文档，与AuthenticationPolicyDefaults/V38种子逐字段及摘要一致；activate=false只阻止后续变更激活；离线预检缺操作者/路线/绑定/设施配置在停机和迁移之前拒绝，不产生写入。新配置缺失/损坏拒绝；运行路径无旧表/旧mode/adaptive读取，旧API410且不能保存；删除旧类型/构造器/Mapper后全量testCompile检查，搜索全部引用，历史SQL/审计说明作为显式allowlist，不用运行兼容类型维持历史测试；V38只清理退役策略，账号/密码/绑定/消息凭据/审计指纹保持。
- 同情境复用、风险/环境变化后补验、草稿不失效证据、有效版本切换使旧挑战失效；OTP核验与record、record与commit、发布与activate并发、缺失评分键新增的隔离MySQL测试。
- API匿名/租户越权/平台read/save分别允许与拒绝；预览owner/TTL/重放/CAS；两个页面并发保存不丢字段；大整数版本往返。Cypress覆盖风险页等级处置及MFA因素配置、加载/取消/错误恢复。
- TOTP证明：RFC6238向量及6位前导零/±1窗口边界，二维码本地解码核对issuer/参数/seed，pending过期/取消/确认，跨owner/用途拒绝；真实隔离MySQL对同码并发、跨challenge/场景、策略切换、撤销/重绑、事务回滚及lastAcceptedStep作断言，失败路径不泄密。用户多方式切换不扩充总尝试，TOTP无send动作。
- Passkey证明：UV主登录/补验满足已允许强度时无多余短信/重复挑战；错UV、错owner/RP/Origin/策略、过期/撤销和权限拒绝不放行。Apple Safari、Windows Hello+Edge/Chrome、Android Chrome能力检测和实机结果与虚拟认证器分开记录。
- Build源码及开发验证按verification.md逐批记录；Redis/MySQL、浏览器及真实供应商证据分开，不把计划审查、局部源码或镜像构建当整体验收。


用户已明确“OK，确认”接受UV Passkey适用于MFA及合格时直接满足强认证的规则。该确认已纳入本计划；具体新增文件/依赖与TOTP/方式选择的合并计划已完成独立Plan审查，并获本轮用户YES实施批准，不自动部署或启用真实策略。


独立Plan审查结论（最终四因素方案）：共2轮。Round1跨挑战预算及登记上下文2类Important已修正；round2无新增Important/Minor/Nit。详review-findings.md。工程师已在本轮明确回复YES批准具体实施；不代表源码/测试/运行验收通过。


实施批准记录：用户于2026-11-20回复“YES”，批准最终四因素实施计划及其已记录的两轮Plan审查修正。开始Build源码实施；真实迁移、策略启用、镜像/容器切换仍须各自授权。


## Build实施记录（2026-11-20，首批）
已实现新风险/四因素不可变合同、初始默认文档、严格有效读取、风险动作/合格集合计算、V38/V39迁移源码、SQL提交围栏及TOTP密文仓库/算法/登记生命周期/本人API/强身份守卫；Gateway新增固定TOTP核验协议。个人安全中心使用既有固定`account`可信来源，原Session摘要与用途TOTP_ENROLL、完整策略/身份/设备/版本及替换对象快照一起绑定。该来源不是HTTP可选业务client，不新增跨应用代理。

数据类型修正：TOTP小型信封使用1024字节ASCII列，隔离MySQL验证原先宽utf8mb4 VARCHAR定义不可用后修正；不改变AAD/算法或历史迁移。评估上下文显式保存可信userType及最终风险动作，BLOCK不能进入提交围栏。围栏在owner锁后用SQL当前读复核硬状态/设备墓碑，避免RR旧快照漏掉未提升安全版本的设备撤销。

整个计划尚未完成，Build门禁仍开放：System草稿/预览/启用、能力预检/离线切换检查、旧MfaMode/旧表运行读写接口退役、IAM统一执行链/证据发布上下文、所有因素共用预算接入、登录/敏感TOTP适配、官方WebAuthn、用户四因素选择、后台及个人UI和相应回归仍待完成。当前不得提出“implementation complete”验收，不得部署这一过渡源码状态。


Build文件清单补记（System旧合同退役）：删除`macula-cloud-system/src/main/java/dev/macula/cloud/system/vo/MfaPolicyPreview.java`。其唯一用途是已明确退役的旧MFA预览响应，调用方已改为新AuthenticationPolicyPreview；搜索无其他引用。此为批准的旧合同清理中的精确文件展开，不新增兼容层或改动真实库。


System接入记录：V38源迁移补充5个固定新URL与原MFA读/预览/保存URL权限的精确映射，保持旧权限行ID、菜单及既有角色关系；仅将原明确URL授权继承到对应新动作，仅读角色不获得保存/启用。已批准方案的“复用平台权限”据此落到Gateway URL授权链，未执行真实迁移。隔离IAM TOTP迁移测试的最小schema同步补入权限表，历史V31–V37不修改。


System策略管理接入进度：新GET/风险保存/因素保存/预览/启用服务与严格HTTP合同已实现；分区保存锁内合并不可变版本，原owner/jti认证引用及当前角色由主库复核，原ROOT声明不能绕过已撤销权限；预览绑定双版本/摘要/原认证引用/安全版本与五分钟TTL。部署开关默认false，仅限制后续启用，不限制初始active=1读取。旧System MFA读写/预览API均410，旧JSON分支及未使用VO已删除，MfaPolicyMapper仅保留新的布尔能力投影。能力预检包含真实主认证路线与独立因素，所有登录风险档BLOCK时即使有密码和绑定也拒绝自锁启用。IAM旧配置运行链及页面仍待后续统一替换，不能部署过渡源码。


System授权实现补记：管理员权限同时受原已认证角色范围及当前本地主库直接角色赋权约束，并保留反向角色/排他优先级；角色范围变化要求重新认证，不从不能证明当前本地赋权的群组声明推断平台权限。统一近期强身份证据守卫仍须随IAM新执行链完成，原发布行/权限检查不替代完整强认证验收。


原上下文证据实现补记：V39（从未执行真实库的本特性新增源迁移）追加evaluation_context JSON NULL，保存计划要求的完整原评估上下文；历史证据保留但不回填，NULL不参与新执行链复用。新增AuthenticationEvidenceStoreTest为上述已批准证明的精确测试文件展开。本人因素强身份和TOTP核验已改用上下文证据路径，原登录/step-up与发布调用方整体转换、旧端口删除仍待完成，不能部署当前过渡源码。


## 历史修正：平台因素能力与应用级MFA要求（2026-11-20，已被下文2026-11-21决定取代）

用户明确：“后台主要是配置可用的MFA方式。IAM接入应用那里配置对应的启用那种MFA方式，是否强制启用还是根据风险自适应启用。”本条是需求职责的最新修正，优先于此前把企业通用强制触发范围放在平台风险分区的约定；不是恢复旧MFA JSON或旧全局OFF/RISK/ALWAYS合同。

1. 平台MFA管理提供EMAIL、SMS、TOTP、PASSKEY的全平台可用开关、因素参数及设施就绪状态；消息供应商/模板及秘密继续沿用唯一现有管理来源。这里不再配置某应用是否强制或自适应要求MFA。
2. IAM接入应用配置本应用允许的MFA因素（平台已启用集合的子集），并选择“强制认证”或“风险自适应”。主登录方式与MFA因素是不同字段：例如PASSWORD主登录加TOTP补验，不把SMS主登录再次验证SMS算作独立MFA。
3. 强制认证在允许继续的用户登录中始终要求满足本应用的MFA强度；已具备本应用允许且满足强度/时限/环境的服务器证据可直接满足，真实UV Passkey主登录也可满足，不机械地重复挑战。账号硬拒绝和风险BLOCK仍优先。
4. 风险自适应使用风险规则对LOW/MEDIUM/HIGH的处置结果决定是否补验；风险判断不在MFA页面复制一套。平台风险分区的通用强制触发范围（例如requiredLoginUserTypes）须按本修正重新归属到应用要求，避免与应用模式重复。
5. 用户可选集合=平台启用且适用 ∩ 本应用允许 ∩ 本次风险/敏感场景允许 ∩ 本人已绑定有效 ∩ 实际可用 ∩ 独立性/UV/最低强度要求。用户在该集合内选择或切换；需要MFA而集合为空时明确拒绝/进入既有受控恢复，不跨应用借用允许集合或自动放宽。
6. 应用限制必须进入服务器原PolicyEvaluationContext及版本围栏：创建挑战时捕获可信应用ID、应用授权/认证配置版本与有效MFA要求，后续核验/证据/发布复核同一原应用版本；应用配置变化使旧挑战失效，不能只检查平台有效版本。预览与能力预检亦须按实际应用路线及该应用MFA要求计算。
7. 个人强安全设置、因素管理近期强认证和受保护敏感接口的明确要求继续收紧准入；应用自适应不能关闭这些要求。机器凭据不进入交互式用户MFA挑战。

本次仅同步用户已明确的产品职责修正。接入应用的数据模型、表单/API、授权版本联动、应用围栏及预检/页面测试文件清单须并入修订实施计划；此前计划的应用归属不足部分不视为已批准的新源码清单。本记录不自批新设计/实施门禁，业务源码未因本条修改，真实迁移/配置启用/镜像切换仍未执行。


## 当前范围修正：暂不提供接入应用MFA配置（2026-11-21）

用户明确：“OK，IAM接入应用暂时不要MFA配置了。”此前应用级MFA修正中有关应用允许因素、强制/自适应模式及其新增数据/API/表单/版本的要求暂不实施，不作为本轮源码任务。

1. 平台MFA后台管理EMAIL、SMS、TOTP、PASSKEY的可用方式、适用规则、具体因素参数及真实设施状态。
2. 是否要求补验仍由统一风险/场景处置、账号个人强安全要求及敏感操作要求决定；MFA页面不另放重复的风险触发开关。
3. IAM接入应用本轮不增加MFA方式选择、MFA强制/自适应模式、对应字段/API/表单/迁移或预览激活流程，也不恢复旧全局MFA JSON模式。
4. 用户在平台与本次风险/场景允许、本人已绑定且实际可用的独立因素集合中选择；个人中心继续参考用户提供的卡片和登记向导设计。需要补验而集合为空时不放宽要求。
5. 原有可信client、应用授权版本及owner/设备/认证会话绑定继续有效；“暂不增加应用MFA配置”不代表删除既有应用隔离或放宽证据的跨应用复用边界。

本条仅记录用户直接给出的范围收缩，取代前一节应用级MFA扩展建议；不自批实施/测试门禁，不修改业务源码、真实配置或部署环境。


## 最新菜单与密码策略需求（2026-11-21）

用户明确通用安全与密码策略拆为独立菜单，密码策略允许按不同用户区分；原风控分级界面命名为“MFA策略”，与“多因素认证配置”置于“多因素认证”同一菜单的两个Tab。此需求取代此前风险/因素分属两个菜单以及密码表单仍位于通用安全页的安排，IAM接入应用仍不增加MFA配置。

具体信息架构、按用户默认/指定策略建议、现有租户字典与共享本地密码边界及验证要求见[security-settings-structure.md](security-settings-structure.md)。用户已明确的职责修正直接记录；按用户密码策略新增持久化/解析/分配及跨目录边界仍须列入具体修订实施计划，不能把原accepted plan自动当作这些新增源码范围已获批准。本次仅更新设计记录，未修改业务代码、运行配置或数据库。


2026-11-21产品确认补记：用户“对的”确认security-settings-structure.md的菜单与按用户密码策略方向。原页面职责由该确认替代；新增具体工程计划见[security-settings-plan.md](security-settings-plan.md)，尚待独立Plan pass和工程师批准，不扩大原accepted源码清单。
