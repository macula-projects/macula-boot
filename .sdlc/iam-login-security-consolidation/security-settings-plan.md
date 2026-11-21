# Plan: 安全菜单收敛与按用户密码策略（2026-11-21设计增量）
Status: accepted

## Source and approval boundary

用户已回复“对的”，确认security-settings-structure.md中三个菜单、两个MFA Tab、默认密码策略加指定用户策略、同一用户一套有效指定策略及接入应用暂不配置MFA的产品方向。本计划补充工程决策；原四因素accepted计划仍执行未被本计划明确替代的部分。本具体计划已完成两轮独立Plan审查，用户于2026-11-21明确回复YES批准；产品确认和工程批准分别记录。

沿用Java17、Spring Boot4/Spring Security、MyBatis、MySQL8、现有密码编码器、PasswordPolicyRules、Vue3/Element Plus/Vitest/Cypress；不增加依赖，不实现任意正则、无校验密码或新的明文检测服务，不改变真实密码/目录/应用绑定，不自动迁移、发布镜像或切换容器。只在Cloud改变业务源码；Boot同步SDLC记录，无本增量Starter代码。

## Files that change

以下路径相对macula-cloud；API=macula-cloud-api/macula-cloud-system-api/src/main/java/dev/macula/cloud/system/；S=macula-cloud-system/src/main/java/dev/macula/cloud/system/；I=macula-cloud-iam/src/main/java/dev/macula/cloud/iam/；UI=macula-cloud-admin/。大括号只展开具体列出的文件。

### 公共密码合同及迁移
- API`security/password/{PasswordCredentialOwner.java,PasswordPolicyDocument.java,PasswordPolicyDefaults.java,EffectivePasswordPolicy.java,PasswordPolicyResolver.java}`新增完整不可变规则、真实本地凭据归属及唯一默认/指定解析；复用API`security/PasswordPolicyRules.java`的强度内核，必要改动只为新完整规则调用，不重新实现密码算法。
- S`security/PasswordPolicyBootstrap.java`、S`cli/PasswordPolicyPreflight.java`新增遗留密码字典的一次性只读预检/标准化，不解析旧MFA模式。只读入口不启动Web/Spring Boot/Flyway/Nacos/定时任务；不输出用户名、密码、哈希、联系方式或完整弱密码列表。
- `macula-cloud-system/src/main/java/db/migration/V40__User_password_policies.java`新增Java迁移；`macula-cloud-system/src/main/resources/db/migration-support/iam-user-password-policies.sql`为其唯一DDL资源，非Flyway第二个版本脚本。V40在DDL前验证全部遗留密码配置的有效性和一致性，随后创建表/默认文档及下面声明的sys_user凭据元数据列；MySQL DDL不宣称可回滚。版本若实施时已被占用，先修订精确文件名。
- `macula-cloud-system/src/main/resources/db/migration/V41__security_settings_menu_structure.sql`新增菜单/权限迁移，保留通用安全及MFA已有菜单ID/角色关系，改显示名及调整排序，增加独立密码策略菜单/按钮/固定URL权限；不改V4/V30–V39历史脚本，不删除用户、密码、因素、消息供应商或审计。

### System管理与所有本地密码写入
- S`controller/{PasswordPolicyController.java,SecurityPolicyController.java}`；S`service/{PasswordPolicyService.java,SecurityPolicyService.java,PasswordPolicyValidator.java}`；S`service/impl/{PasswordPolicyServiceImpl.java,SecurityPolicyServiceImpl.java,PasswordPolicyValidatorImpl.java,SysUserServiceImpl.java}`。
- S`form/{UserPasswordPolicyForm.java,PasswordPolicyUsersForm.java,PasswordPolicyActivationForm.java}`；S`query/PasswordPolicyQuery.java`；S`vo/{UserPasswordPolicyVO.java,PasswordPolicyPreviewVO.java,SecurityPolicyResult.java}`。
- S`mapper/UserPasswordPolicyMapper.java`及`macula-cloud-system/src/main/resources/mapper/UserPasswordPolicyMapper.xml`；S`security/PasswordPolicyAccessGuard.java`。
- 原主计划S`security/ProviderConfigurationStrongAuthGuard.java`的统一当前原Token/权限/近期强证据守卫供新密码策略管理复用；不另建只认Token声明时间的捷径。S`service/impl/AuthenticationPolicyServiceImpl.java`如需复用同一管理守卫，仅替换重复身份校验，不改变其已接受的CAS/预览合同。
- S`form/PasswordPolicyConfigForm.java`、S`vo/PasswordPolicyConfigResult.java`在新合同调用方全部替换后删除；原`SecurityPolicyController PUT /password`固定410，不解析旧密码表单或重定向新保存。聚合`GET /security-policy`不再返回可写密码分区；旧字典只留作历史/一次性预检，不保留运行双读。默认种子保留原有效密码要求，和用户取消旧MFA配置兼容是不同范围。

### IAM执行与发布复核
- I`service/account/impl/{DefaultClientAccountDetailsService.java,SysUserServiceImpl.java}`、I`service/userdetails/SysUserDetails.java`、I`pojo/{dto/UserAuthInfo.java,entity/SysUser.java}`、I`mapper/SysUserMapper.java`及`macula-cloud-iam/src/main/resources/mapper/SysUserMapper.xml`：本地原始凭据版本与哈希同一次SQL读取，可信本地来源标记仅在真正sys_user路径由服务器设置；不得采信扩展目录返回的employee类型/同名username为本地凭据来源。S`pojo/entity/SysUser.java`增加只供持久层的凭据版本，用户Form/远程普通用户信息不能写入该字段。
- I`mapper/UserPasswordPolicyMapper.java`、`macula-cloud-iam/src/main/resources/mapper/UserPasswordPolicyMapper.xml`；I`security/{IamPasswordPolicyReader.java,PasswordPolicyFence.java}`。
- I`security/impl/{DefaultIamPasswordPolicyGate.java,DefaultIamSecurityCoordinator.java}`；I`security/account/{AccountSecurityService.java,PasswordRecoveryService.java}`；I`security/{AuthenticationPublicationContext.java,IssuedAuthorizationSnapshot.java}`；I`security/protection/{AccessTokenPublicationStore.java,PolicyEvaluationContext.java}`；I`protocol/oauth2/persistence/MaculaOAuth2AuthorizationService.java`及I`authentication/session/IamRememberMeServices.java`。
- 原计划统一MFA执行链内如需读取密码有效期，必须用上述快照，不在MfaPolicyService/Filter再读原password字典；本计划不重新增设应用级MFA或改动业务授权接口。

### 后台菜单、页面与文档
- UI`src/views/system/securityPolicy/{index.vue,components/LoginProtectionSettings.vue}`移除密码和风险分级表单，保留真实通用防御能力。
- UI`src/views/system/passwordPolicy/{index.vue,edit.vue,components/PasswordPolicySettings.vue}`新增策略列表/编辑/指定用户/默认来源及预览启用。
- UI`src/views/system/mfaPolicy/{index.vue,components/MfaPolicySettings.vue,components/AuthenticationFactorSettings.vue}`；原risk-response-plan计划新增的`src/views/system/securityPolicy/components/RiskResponseSettings.vue`改为`src/views/system/mfaPolicy/components/MfaPolicySettings.vue`，不产生两个实现。
- UI`src/api/model/system/{securityPolicy.js,passwordPolicy.js,authenticationPolicy.js,mfaPolicy.js}`；UI`src/locales/{securityPolicy.js,passwordPolicy.js,mfaPolicy.js,riskResponse.js}`、UI`src/locales/lang/{zh-cn.js,en.js}`。现有动态菜单由System数据库映射组件，实施前核对具体路由加载器；不另加静态平行菜单。
- `macula-cloud-{iam,system,admin}/README.md`、`macula-cloud-iam/docs/{configuration.md,architecture.md}`、`deploy/README.md`、`deploy/scripts/check-authentication-policy-rollout.sh`、`deploy/scripts/check-password-policy-rollout.sh`；仅新增只读离线预检步骤，不改变实际.env/Compose环境、不自动运行迁移。
- 两仓`.sdlc/iam-login-security-consolidation/{security-settings-structure.md,security-settings-plan.md,spec.md,plan.md,risk-response-spec.md,risk-response-plan.md,review-findings.md,build-notes.md,verification.md}`维护本修订的来源、审查、审批和真实开发证据。

### 验证文件
- `macula-cloud-api/macula-cloud-system-api/src/test/java/dev/macula/cloud/system/security/password/{PasswordPolicyResolverTest.java,PasswordPolicyDocumentTest.java}`。
- System新增`src/test/java/dev/macula/cloud/system/{service/impl/PasswordPolicyServiceImplTest.java,controller/PasswordPolicyControllerTest.java,security/PasswordPolicyAccessGuardTest.java,security/PasswordPolicyBootstrapTest.java,cli/PasswordPolicyPreflightTest.java,integration/UserPasswordPolicyMigrationIT.java}`。
- System修改`src/test/java/dev/macula/cloud/system/{service/impl/PasswordPolicyValidatorTest.java,service/impl/SysUserServiceImplTest.java,service/impl/SecurityPolicyServiceImplTest.java,controller/SecurityPolicyControllerTest.java,integration/SecurityPolicyMigrationIT.java}`；历史迁移测试只断言历史SQL，不装配退役运行写入口。
- IAM修改`src/test/java/dev/macula/cloud/iam/service/account/impl/DefaultClientAccountDetailsServiceTest.java`；IAM新增`src/test/java/dev/macula/cloud/iam/security/{IamPasswordPolicyReaderTest.java,PasswordPolicyFenceTest.java,UserPasswordPolicyPersistenceIT.java}`；修改`src/test/java/dev/macula/cloud/iam/{security/IamPasswordPolicyGateTest.java,security/IamSecurityCoordinatorTest.java,security/AccountSecurityPersistenceIT.java,security/account/PasswordRecoveryServiceTest.java,security/account/AccountSecurityServiceTest.java,authentication/mfa/MfaChallengeServiceTest.java,authentication/mfa/StepUpServiceTest.java}`。
- UI新增`tests/unit/{password-policy.test.js,password-policy-settings.test.js}`及`cypress/e2e/password-policy.cy.js`；修改`tests/unit/{security-policy.test.js,mfa-policy.test.js,risk-response.test.js,authentication-factor-settings.test.js}`和`cypress/e2e/{security-policy.cy.js,mfa-policy.cy.js,risk-response-policy.cy.js}`。原计划已列出但尚未创建的文件仍按具体最终路径实现。

## Order of work

1. 本轮产品确认只将已明确的菜单与默认/指定用户方案标为accepted；保留本工程计划draft，完成独立Plan pass（最多三轮）并展示实际审查结论，工程师批准后先把本清单完整并入原plan.md的Files that change/顺序/证明（不只放链接），再写本增量源码。原已批准四因素未完成项不自行宣称完成。
2. 冻结真实凭据主体和默认初始化：当前密码写入只支持本地employee/sys_user，凭据主体为固定LOCAL_EMPLOYEE+sys_user.id，和租户派生身份/第三方同号用户严格区分。策略为平台目录级，一份默认文档加按实际本地账号的唯一指定绑定；切换租户不能切换同一密码的规则。外部目录仅显示不适用，不写其密码或自动映射employee。
3. 定义完整PasswordPolicyDocument，含现有强度、弱密码/账号信息/重复及连续字符、历史、有效天数及提前提醒；不把角色/部门继承、任意正则、明文检测、通知服务或无强度校验引入本增量。所有ID和版本以十进制字符串跨HTTP，未知/缺失字段拒绝。
4. 只读预检枚举实际租户及本地账号已存在的IAM租户/空租户上下文，含没有任何密码字典的租户；明确分别执行IAM的status=1过滤和System当前不过滤status的真实读取语义，逐端采用当前真实默认值、复杂度/弱密码/历史/时限校验标准化。先证明同一上下文IAM与System的完整有效文档一致，再证明全部上下文一致；禁用行造成两端分歧同样拒绝，不静默选一端。如果不同租户有效要求不一致、配置损坏/重复或不能证明统一默认，V40在任何DDL前失败，输出聚合冲突原因；不自行挑平台租户、取最宽规则、合成未经批准的更强规则或改写旧配置。需操作者另行确认目标默认/指定分配后修订受控初始化，不由迁移猜测。无冲突时将原统一规则一次性固化为新默认active=1；后续运行仅读新表。当前只在隔离库验证，本轮不跑真实预检或迁移。
5. V40另给sys_user追加password_credential_version BIGINT NOT NULL DEFAULT 0（非负、溢出拒绝），仅代表实际密码的代际，不标记历史密码通过、不重写任何哈希。真实密码设置/更换/重置在原事务内推进该版本；编码升级保持不变。持久化采用固定catalog行active_revision、policy实体/不可变version、唯一本地userId有效绑定、待激活用户绑定变更、一次性preview及脱敏审计。默认必须存在且enabled；指定绑定优先，缺绑定用默认。损坏/悬空/重复绑定失败关闭；禁用指定策略须明确预览并原子移除其绑定回默认，不能让解析器把损坏绑定当未分配。默认不支持直接禁用/删除。已有active策略不物理删除，只经预览停用；从未active的空草稿可删除。
6. 新管理API固定`/api/v1/password-policies`：GET分页列表（≤100）、GET `/{id}`、POST建草稿、PUT `/{id}`存草稿（expectedDraftVersion）、PUT `/{id}/users`存add/remove的待激活绑定（每次≤500）、POST `/{id}/preview`、POST `/{id}/activate`，DELETE只删除从未生效且无绑定的草稿。所有写入校验期望版本；同用户已有有效指定策略则409，显式替换必须在同一预览中列出旧/新绑定版本，不支持隐式抢占。总影响人数游标扫描批次≤500，不无界加载用户。
7. 权限分离：增加`system:iam-password-policy:read/save/assign`平台权限及精确URL；仅可信平台管理身份可操作，并从当前主库直接赋权及原认证角色范围复核，处理反向/排他角色。初始只向平台租户1当前有效ROOT明确赋权，不将原普通租户密码保存权限升级为全局目录策略权限；非平台ROOT/租户security权限不绕过。用户搜索也受平台目录read权限，返回ID/显示名等最小投影，不返回哈希/原联系方式。所有策略保存、指定用户、预览/启用均要求当前原Token发布账本及近期强证据，不信任JWT旧MFA时间或HTTP自报owner。
8. 预览绑定操作者identity/原authenticationId+Token引用摘要/安全版本、policy草稿版本、catalog active_revision、定义及待绑定变更摘要，TTL300秒。启用在主库事务内重查原权限/强证据/有效期/版本/实际用户归属、重复绑定及默认可用性；消费预览、更新指针/用户绑定、提升active_revision和审计同事务。草稿不改变active_revision，不能让未启用编辑改变用户执行规则。重放/跨owner/过期/角色撤销或CAS冲突拒绝。
9. 实施统一锁序：凡同事务同时涉及密码策略和认证围栏，先锁password catalog（读取SHARE，启用UPDATE），再平台认证策略→租户评分→相关IAM身份按稳定identity顺序→sys_user凭据→密码历史。管理强身份守卫在取得catalog后复核；不得先锁actor再拿catalog。AccountSecurityService.password/PasswordRecoveryService、SysUserServiceImpl的新增/编辑/重置/导入必须在其外层入口先取得catalog，再做原账号证明/owner/user锁，不能在现有lock(user)或lockForPassword之后才调用新reader。外层管理/密码写入/导入/恢复/最终发布事务逐入口显式设置REPEATABLE_READ，包括注解和TransactionTemplate；不把默认isolation元数据null当作围栏满足，也不能在已有低隔离事务中用内层注解假装升级。新UserPasswordPolicyMapper以ORDER BY identity_key FOR UPDATE当前读锁定该实际本地userId的全部派生身份，再取得sys_user锁，recorded()只消费已锁身份并按同顺序处理；不依赖既有无序枚举。没有真实事务或隔离不足拒绝锁快照，不在持锁时远程调用供应商。
10. 单独处理Spring框架认证后编码升级：DefaultClientAccountDetailsService.updatePassword只接受服务器确认为本地sys_user来源且具有原userId/原password_credential_version/原预期哈希的主体，扩展目录即使employee同名也拒绝本地更新。按统一catalog→身份排序→sys_user锁序读取当前行，用userId+原凭据版本+预期旧哈希CAS升级编码；失败终止本次认证，不返回过期主体。此入口不重新校验明文强度、不增加历史、不更新passwordChangedAt、凭据代际或安全版本，不将编码迁移伪装成实际改密。主认证加载时原哈希与password_credential_version必须在同一SQL快照返回，之后保留不可由HTTP自报的原本地凭据快照；真实密码变更推进版本后，即使coordinator读到了新securityVersion，也不能把旧口令验证重标成当前认证。
11. 同一纯解析器供IAM和System共用规则语义；各自Mapper只读同一主库新表，不跨服务import实现或增加远程密码校验调用。新用户尚无ID时锁定默认快照；指定策略仅适用于已存在且管理员可管理的本地用户。System创建/修改/重置/导入、IAM个人改密/恢复全部用同一有效文档校验强度与历史，保留原PasswordEncoder/历史哈希/所有派生安全身份撤销和锁优先级，修改成功后才记密码实际时间。
12. 密码有效期和提前提示从同一EffectivePasswordPolicy读取。评估起点捕获owner/default-or-assigned policyId、policyVersion、activeRevision及实际passwordChangedAt，本地PASSWORD主认证另携带加载原哈希时的password_credential_version和服务器来源证明；锁内计算deadline；MFA挑战/授权存储/Remember-me/刷新/最终AccessToken发布保留原服务器快照并在提交事务以统一锁序复核。最终发布同时以当前读校验原本地凭据版本，真实重置发生在口令验证后、coordinator或提交前均拒绝旧认证；不能只靠事后读取的新安全版本。生效策略或绑定改变后旧新成果评估拒绝并重新评估，不把旧判断重标新策略、不延长改密时间。没有本地密码的外部主体不进入本地密码到期处理；本地账户既有强制改密行为保留原适用场景，不通过策略调整清除人工锁或假报密码错误。
13. 退役原`PUT /security-policy/password`为410，移除通用安全聚合密码分区和运行password字典读取；历史字典保留为审计/预检资料，不在运行失败时回退。更新所有调用方和测试，不为旧测试增加兼容构造器。跨版本不混跑，正式切换另行授权并在旧服务停机前执行离线预检；预检不通过不得停旧服务或迁移。
14. 实现三个菜单：通用安全、密码策略、多因素认证。多因素认证恰有MFA策略/多因素认证配置两个Tab；评分/分级及风险动作在前者，四因素卡片及参数在后者。租户评分与平台策略写权限仍独立，页面合并不提升权限；分区保存/CAS合并、预览/启用状态、未保存Tab切换/取消/错误恢复保持一致。
15. 密码策略页使用列表和分组编辑，展示实际默认/指定来源、有效版本、适用用户及预览影响。移动布局采用单列，桌面分组；沿用现有组件/国际化，不复制第三方商标/下载二维码，不增加截图中未实现的开关。个人中心按已确认卡片/向导继续原计划，不能让个人选择宽松密码策略。
16. 执行最小规则/服务/HTTP测试→隔离真实MySQL迁移及并发→IAM/System/API相关回归和testCompile→前端Vitest/构建/Cypress菜单/关键密码流程。聚焦Checkstyle及diff检查，记录允许/拒绝/未执行范围；不运行自动重排或带--fix全仓lint，不动用户其他未提交修改，不自批Build完成或部署。

## Risks

- 跨租户共享同一sys_user密码不能按当前请求tenant决定规则。采用平台凭据级单一绑定和严格旧默认预检；迁移遇到已有分歧会中止，可能需要独立的人审初始化安排。此约束不是授权修改旧真实配置。
- 平台密码策略权限是新全局能力，不通过旧租户security:save自动继承；UI可见性不是授权，原Token主库发布/当前角色/强证据都必须复核。READ搜索也不能枚举不可管理用户。
- MySQL DDL不能假装回滚；预检先行，真实切换前备份/恢复演练及停止旧版本。回滚是受控应用+策略恢复，不通过旧运行双读绕过不兼容合同；不能恢复被撤销凭据。
- password catalog锁是跨节点串行化点；持锁操作有界、先CAS验证、无远程调用。策略编辑只有草稿不影响active快照；启用与密码写入/最终发布必须真实SQL线性化。
- 原四因素执行链、官方WebAuthn、统一管理强身份守卫尚未全部接好；本增量依赖这些真实能力，不能临时用无条件通过或JWT时间替代，也不能部署过渡源码。

## Proof

- 默认/用户A/用户B不同策略；无绑定回默认，悬空/损坏不回退，重复分配409，外部同号用户和跨tenant请求不能换本地规则。完整规则与System/IAM/过期判断得到相同结果。
- 真实隔离MySQL：V40种子与原统一默认逐字段一致，旧tenant分歧/禁用行两端分歧/缺字典租户/空租户上下文/损坏/重复在DDL前拒绝；V41旧菜单/角色ID和业务数据指纹不变，平台ROOT赋权和普通租户拒绝；全部迁移不写密码哈希/用户绑定。
- 并发密码变更↔策略启用、绑定调整↔最终发布、默认切换↔新建/批量导入、两个管理员绑定同用户、预览消费与事务回滚，严格验证锁序、只一次提交、旧快照拒绝及无死锁；增加延迟注入覆盖校验后/哈希前/提交前切换，不只测试先读后顺序保存。
- 所有本地密码写入口及恢复复用同规则/历史；当前密码/历史重用拒绝，策略保存不改密码；框架编码升级↔真实重置并发不覆盖新哈希且保留密码年龄，扩展employee同名目录不能升级本地密码，主口令核验后并发真实改密在最终发布拒绝原凭据代际；过期/提醒按新有效文档和真实改密时间，MFA/刷新/Remember-me不续期。账号人工锁、资源授权和跨目录保护回归。
- 管理API匿名、机器、普通租户、只读、save但无assign、有效强认证、错误owner/会话/权限撤销/过期/预览重放/CAS分别允许或拒绝。ID>2^53及版本精度完整往返。
- Vitest/Cypress：三个菜单及MFA两Tab，通用页无重复密码/分级；指定用户策略表单、默认保护、冲突、分区并发不丢值，loading/空态/失败/取消/重复提交、Tab未保存及键盘/窄屏。模拟API证据不冒充真实IAM端到端或真实供应商/设备验收。

## Review and gate

Plan round 1已完成，3类Important：旧字典两端语义、显式RR与排序当前读、框架编码升级遗漏。全部已在本修订补入，并扩展原凭据代际与最终发布证明；详情见review-findings.md。round 2已按精确diff完成，无新增Important/Minor/Nit，累计实际2轮。用户2026-11-21明确YES批准本具体计划，Status为accepted；进入Build实施。只做计划审查，不进行Bugs/Security/Compliance代码审查，不宣称实现/测试/部署通过。配置的codex-plan-review在当前CLI不可用，按sdlc-build回退既有sdlc-reviewer子代理；Plan pass最多三轮，后续只审新改动，不重复既有review-findings。


工程批准：用户于2026-11-21回复YES，批准完整文件/数据模型/权限/初始化/锁序计划及两轮Plan审查结果。用户随后明确“个人中心也一并实现哦”：原accepted主计划的account-security模板/JS、passkeys.js、TOTP/WebAuthn个人API及前端验证同批交付；本次精确补入现有`macula-cloud-iam/src/main/resources/static/iam/account-security.css`样式文件，以及`macula-cloud-api/macula-cloud-system-api/README.md`公共模型文档。不是新建恢复代码/账号注销功能的授权，也不授权真实迁移/策略启用/镜像切换。


实施文件展开补记：框架密码升级构造依赖需同步`macula-cloud-iam/src/test/java/dev/macula/cloud/iam/service/application/IamApplicationTestFixtures.java`，仅给既有测试工厂显式注入新围栏替身，不新增生产兼容构造器或改变目录优先级。该文件属于批准计划调用方编译与验证的必要测试支撑。


恢复上下文实施补记：原主计划`AccountVerificationService.java`的私有Operation追加原本地来源/凭据代际，拒绝无字段旧恢复证明；对应精确测试`macula-cloud-iam/src/test/java/dev/macula/cloud/iam/security/account/AccountVerificationServiceTest.java`同步真实服务器fixture，不增加兼容构造器。
