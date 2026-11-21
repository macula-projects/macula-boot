# Spec: 登录安全收敛与认证能力完善 (from intent.md 2026-11-20)
Status: accepted

## Source intent
[intent.md](intent.md)；用户已明确回复yes。保留本地并完整接入腾讯，Passkey覆盖无密码登录、MFA与敏感补认证。

## Requirements
1. 安全策略只呈现登录防护、认证强度、会话设备、账号恢复四类职责。MFA策略仍全平台统一；租户限频与账号边界不改变。每个可保存字段必须有执行点与允许/拒绝回归，否则界面不得宣称已生效。
2. 统一主认证尝试、口令失败与验证码发送预算的事件及计数来源；不同维度可以叠加，同一维度不得在Filter与Service重复计费。刷新、MFA续接和授权码兑换不计成新口令尝试。
3. 设备数量/冲突动作成为唯一登录配额；删除旧sessionMaximum业务准入，但保留会话族、超时与撤销账本。闲置/绝对时长、首改密码及安全通知配置必须实际执行，通知无可用渠道时显示未配置并保留审计，不伪报已投递。
4. 软风险信号只能导致补认证、临时阻断或受控恢复，不直接形成永久锁号；明确禁用/管理员锁定继续硬拒绝。未知地域与网络故障不当成确认攻击；风险仍须参与所有新认证成果发布。
5. 人机验证LOCAL/TENCENT完整可用，保留既有Aliyun配置而不删除其凭据。适用页面共用验证交互，后端始终消费绑定用途的一次性证明；关闭弹窗、失败、过期、供应商故障均可恢复且不提交业务副作用。
6. 人机供应商及IPDataCloud管理支持权限控制、秘密设置状态、版本、草稿测试、显式启用、清除与审计；普通租户管理员不能读取或改变平台供应商秘密。IP查询不成为任意URL代理。
7. Passkey注册/登录/MFA/敏感补认证均使用服务器可信账号与应用上下文；强认证要求userVerification=required并验证真实UV标志。没有UV不得标记为已完成MFA。
8. 密码+已绑定SMS/EMAIL可按现有策略作为组合；重复同一SMS/EMAIL不能计为第二因素，SMS+EMAIL不默认认定为独立双因素。OTP主登录要求独立且足够强的后续证据，本轮提供UV Passkey；第三方登录不猜测上游认证强度。
9. 统一MFA决策及近期证据：账号/设备/认证会话/安全版本/方式/完成时间/环境共同约束；满足要求即复用，刷新、访问、续接不刷新完成时间。业务授权始终独立。
10. Passkey个人管理支持注册、命名、列表、撤销；注册/撤销需当前owner及近期强认证，首次注册允许密码主认证加已绑定OTP形成的近期证据，或既有UV Passkey；OTP主登录不能再以同类OTP作为注册授权，不能仅凭已盗取Session注册。无独立证明则走受审计人工恢复，不开放匿名新注册。
11. 每个用户启用强制认证前检查至少有一种满足强度的已绑定方式；移除最后可用方式必须先新增替代方式或完成受控恢复，管理员也不能直接跳过；凭据撤销推进安全版本并撤销受影响会话。

## Non-goals
不实现商业反机器人引擎、不承诺杜绝所有攻击；不新增人脸/企业硬件证明强制策略，不改变Boot资源授权注解@Passkey，不替换OAuth/OIDC或业务URL授权，不自动开启真实云调用/更改运行策略/推送发布。

## Design
### 权威来源与执行顺序
复用Spring Security原生Filter/Provider/Session策略和现有认证成果发布围栏：入口预算→应用/账号上下文→必要人机→主认证→账号状态/风险→MFA判定与续接→事务内设备准入→Session/Token发布。登录及高敏场景共享MfaPolicyService与MfaEvidenceReader。保留各发布时点版本复核，移除重复规则实现。

新增认证因素枚举PASSWORD/SMS/EMAIL/TOTP/PASSKEY用于证据与MFA，短信/邮件投递Channel仍仅SMS/EMAIL，避免把Passkey伪装为消息渠道。旧SMS/EMAIL序列化值保持；Gateway的factors契约追加PASSKEY且与IAM允许因素求交。浏览器补认证只更新原认证会话证据，不切账号、不签新业务Token、不自动重放非幂等业务请求。

### 配置与管理界面
现有安全策略页收敛为四个职责分区；“认证强度”链接已有MFA管理入口，不复制其表单。人机分区内包含触发策略和独立的平台供应商配置；登录风险分区内提供IPDataCloud配置与诊断。个人安全中心新增“通行密钥”。

供应商配置由System维护，IAM通过共库只读投影读取不可变有效快照；保存草稿与启用分开，测试结果绑定版本，CAS并发更新冲突返回409。秘密采用标准认证加密，密钥由部署注入且不与密文同库保存，nonce/用途绑定与keyId进入信封，不自写密码算法。人机供应商测试须完成本版本真实挑战并由服务器核票，不能以假票据返回或SDK加载成功冒称服务可用。有效配置读取失败拒绝相关新操作，不回退到更弱供应商。仅有密钥配置完整不等于外部服务已验证。

环境注入的现有秘密保留为ENV来源，只显示已设置；切换到MANAGED来源必须显式操作，不能因数据库出现记录就覆盖环境。平台配置修改、测试、启用在服务端要求管理员权限及owner绑定的近期强认证，不能只靠前端按钮或Gateway拦截。管理端提交秘密采用KEEP/REPLACE/CLEAR显式动作，空字符串不代表清除，掩码不写回。加密存储采用JCA标准AES-GCM，随机nonce、用途AAD与keyId，主密钥只从独立部署secret注入；缺失密钥时不能写入/启用MANAGED秘密。响应、缓存通知、日志和异常不包含秘密或密文。权限复用平台角色体系，细分read/save/test/activate，不能靠前端隐藏建立边界。

IPDataCloud固定供应商端点与HTTPS，不接受HTTP入参URL，禁重定向；仅公网IP出站，超时/响应上限/预算有界。测试由受权管理员显式输入IP，返回归属地与脱敏诊断；未配置、非公网、超时、配额耗尽与未知结果分开。地域仅作为信号。

### 人机交互
公共组件覆盖密码、发码、登录MFA、恢复、联系方式与绑定。LOCAL改进可访问性、移动布局及取消重试；腾讯按官方TJCaptcha.js动态加载，只传公开AppId，ticket/randstr交IAM官方SDK核验。ret=0、trerror容灾票据或前端pass=true都不能直接放行；后端只接受明确成功，云故障不自动降级LOCAL。挑战/证明绑定owner、应用、用途、目标摘要和策略版本，短期、原子消费；并发错误/成功验证不能扩充尝试次数或生成多份证明。

### WebAuthn/Passkey
仅IAM引入BOM管理的spring-security-webauthn；复用官方WebAuthnRelyingPartyOperations及凭据仓库扩展，不复制签名、attestation或origin验证。普通浏览器登录接原生认证链；MFA/敏感操作只适配官方校验结果到现有挑战/证据，不额外建立认证体系。

凭据owner映射稳定IamUserIdentity与随机userHandle，不使用可变用户名、邮箱或自报tenant/userId。登录采用可发现凭据；未知/撤销凭据使用统一失败反馈。注册、认证的challenge均有短TTL、用途/owner/应用/RP/Origin/策略版本绑定及跨节点原子claim，成功或失败不能重放；不接受客户端自报成功或已验证UV。

无密码登录在官方校验、账号与应用准入、设备识别成功后，才将UV Passkey完成时间提交统一强认证证据；不能在主体映射或设备准入前发布可复用成果。高敏Passkey挑战还绑定原授权摘要/会话，不接受另一用户凭据或用重新登录替换挑战owner。

RP ID与allowedOrigins由部署可信配置给定，界面展示状态与本地测试入口；不从Host/转发头猜测，不提供有凭据时直接修改RP的按钮。生产要求HTTPS；开发使用显式localhost Origin，不能把127.0.0.1现有登录入口自动当作可用RP。同步Passkey的BE/BS和零计数器按标准/库处理，不以计数器未增加直接永久锁整个账号。

### 配置兼容与切换
统一EffectiveLoginPolicy把新旧配置映射成一份执行快照。已有值无冲突时保留明确来源；新devices配置与旧sessionMaximum/deviceMaximum冲突必须在预检中展示并由管理员选择，不静默覆盖。过渡读取是单一快照适配，不保留两套准入引擎。

不自动把RISK改成ALWAYS、不自动放宽现有要求；切换前盘点管理员/OTP用户可用独立因素，报告缺失者并阻止会锁死操作者的启用。审计锁定用户与已有配置只读取，不自动解锁或改写。新架构部署、配置启用和生产凭据登记分别验证，不能以部署授权推断策略变更授权。

## Data and interfaces
- System拥有新增Flyway迁移（在Build时依据实际最大版本分配，不改已执行V31–V34）；新增平台供应商配置表，保存kind/provider/source、public_config、encrypted_secrets、key_id、version、state及审计字段。
- 新增WebAuthn用户映射表与凭据表：随机userHandle、唯一身份键、唯一credentialId、公钥、计数器、BE/BS、transports、名称、状态及时间。外键/唯一性/长度按官方模型验证；不存私钥、生物特征或原始可重放断言。
- Passkey撤销与安全版本推进在同一主库事务，发布前复核凭据状态；Redis挑战只保存短期最小状态。新表索引、并发更新、备份和配套回滚进入Build计划；回滚保留数据，不恢复已撤销凭据。
- System管理API固定在/api/v1/iam-security-providers/{kind}，kind仅HUMAN/GEOLOCATION；GET脱敏、PUT写草稿、POST /test与/activate。test只接受固定供应商参数，不能提交任意URL；管理响应沿用Result封装。
- IAM个人API /api/v1/me/passkeys支持列表/命名/撤销；注册与登录复用原生WebAuthn注册/认证options和提交端点。端点精确清单及原生Filter顺序在plan核对当前依赖API后冻结，不能广泛permitAll(/webauthn/**)。
- MFA/敏感挑战追加Passkey options/verify适配，保持既有Session+CSRF或原Bearer的owner校验；不扩大CORS、不把原Token写入query/表单、注册端点不能因认证端点匿名而匿名开放。
- 全部配置/身份/凭据ID保持精度；只对已知统计计数做前端数值转换。审计记录配置动作与版本、因素/认证结果，不写密钥、ticket、断言、完整验证码或收件目标。

## Flagged concerns
- 正式IAM域名尚未提供：[non-blocking] 可完成实现与显式localhost测试；生产RP配置、凭据注册和部署是受该信息阻断的交付项，不能宣称生产可用。
- 尚无本轮腾讯/IPDataCloud真实凭据与独立托管密钥：[non-blocking] 实现配置、模拟合同与拒绝边界；真实供应商联调及MANAGED启用须取得部署配置，既有秘密不擅自迁移。
- 当前OTP主登录用户可能没有独立因素：[non-blocking] 编码不受阻，但严格策略启用须完成受影响用户预检、替代因素绑定或既有受审计人工恢复；禁止因缺因素静默降级。
- 浏览器/设备及Spring Security补丁版本：[non-blocking] 核对本仓BOM实际版本和官方7.0扩展，不为此升级整个框架；需要额外兼容层或新增依赖时在plan明确，不凭最新文档臆造API。平台原生生物识别验收需用户参与，自动化虚拟认证器证据单列。
- 数据库与配置切换：[non-blocking] 本spec批准仅允许设计/后续计划，不等于授权迁移已有环境、启用策略或执行新密钥轮换。迁移演练/恢复校验及独立启用前检查属于交付条件。

## Verification strategy
1. 规则矩阵覆盖PASSWORD/SMS/EMAIL/EXTERNAL/PASSKEY及LOW/MEDIUM/HIGH、禁用/撤销/刷新/缺绑定：同因素不能冒充MFA，UV证据满足要求时不追加OTP；授权不足不能通过MFA变成允许。
2. 真实Redis验证统一限频一次计费、验证码预算、challenge并发claim/重放/TTL；真实MySQL验证设备竞争、凭据唯一性/撤销与版本事务、迁移与回滚保留数据。绝对时长不因刷新/Remember-me延长。
3. 腾讯SDK可控HTTP合同与LOCAL交互测试覆盖成功/取消/超时/错误/容灾票据/未配置；IPDataCloud固定端点、防SSRF/重定向、未知/失败及预算；Secrets的KEEP/REPLACE/CLEAR、版本冲突、密文篡改与不回显。
4. 浏览器虚拟WebAuthn认证器覆盖注册/登录/补认证、UV缺失、错Origin/RP/challenge/owner、重放、撤销、零计数器/同步标志；原生用户设备和真实供应商验证独立记录未执行范围。
5. UI覆盖四类配置、权限拒绝、草稿/测试/启用状态、加载/错误/重试、移动/键盘、最后因素保护与操作者预检。保持既有OIDC登录、菜单、角色/租户拒绝、CSRF、CORS和普通业务接口回归；仅完成上述可适用检查后准备人工验收。

政策依据：两仓AGENTS.md及architecture/backend-development/frontend-development/testing/dependencies-release规则、REVIEW.md；未发现另行适用的组织级合规技能，不自行补造合规标准。
参考：[Spring Security Passkeys](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passkeys.html)、[官方RelyingParty扩展](https://docs.spring.io/spring-security/reference/7.0/api/java/org/springframework/security/web/webauthn/management/WebAuthnRelyingPartyOperations.html)、[腾讯客户端](https://cloud.tencent.com/document/product/1110/36841)、[腾讯票据核验](https://cloud.tencent.com/document/product/1110/36926)、[WebAuthn标准](https://www.w3.org/TR/webauthn-3/)。引用仅用于合同设计，不是已实现或真实供应商验证证据。

设计批准：用户于2026-11-20明确回复yes，接受完整spec及其全部Flagged concerns；本轮进入实施计划审查，尚未批准plan或生产切换。

跨仓说明：权威完整工件位于../macula-cloud/.sdlc/iam-login-security-consolidation/；本仓副本仅同步用户已确认的合同，不自行批准后续plan/验收。


用户后续明确追加TOTP动态口令（Authy等标准验证器）及用户可启用的Apple/Windows/Android WebAuthn，具体见[totp-webauthn-scope.md](totp-webauthn-scope.md)。该要求覆盖旧Non-goal中的TOTP排除；保留不兼容旧风险/MFA配置的用户决定。实施计划和真实环境授权未因此自动通过。


2026-11-21产品确认补记：用户“对的”确认security-settings-structure.md的菜单与按用户密码策略方向。原页面职责由该确认替代；新增具体工程计划见[security-settings-plan.md](security-settings-plan.md)，尚待独立Plan pass和工程师批准，不扩大原accepted源码清单。
