

## 2026-11-20 最终四因素计划首批实施与开发证据

用户已明确YES批准最终risk-response-plan.md，两仓副本已记为accepted。当前仍在Build，整体实施未完成，未提出实现完成/正式Test验收门禁。

已落地：
- API不可变风险/因素模型、固定新默认文档、显式因素/动作/强度、文本有效版本和规范摘要。
- IAM严格新有效策略读取、风险动作及因素交集计算、不可变评估上下文、仅真实RR/Serializable事务可用的SQL围栏。平台→租户范围→owner锁序，以及owner锁后的账号硬状态/设备墓碑当前读。
- V38/V39迁移源码：纯新active/draft=1、精确退役清理、版本/预览/审计表、TOTP认证密文、唯一ACTIVE/PENDING、时间步CAS及TOTP证据约束。真实库未执行。
- 固定RFC6238 TOTP算法与本地二维码，独立可选AES-GCM keyring、完整pending原Session/用途/策略/身份/版本/设备/原ACTIVE快照，登记/确认/取消/命名/撤销/重绑服务和本人API。确认只消费匹配步并启用，不写Token或通用MFA证明。核验服务要求外层事务以便与未来统一证据提交。
- MfaAttemptBudget使用Redis TIME在同owner hash-slot原子预检/预扣owner、challenge、TOTP凭据/登记预算；切换/成功/设施失败不重置或退款。临近到期只限制TTL，不缩短滚动统计窗口。
- Gateway固定TOTP核验协议/原Bearer代理、严格字段及响应白名单和实际过滤链回归；没有TOTP发送动作或业务重放。

已实际执行，均0失败/错误/跳过：
| 范围 | 数量 | 证据 |
|---|---:|---|
| IAM新代码及既有协调器/设备/MFA/补验/个人安全回归 | 134 | /private/tmp/iam-four-factor-regression-test.log |
| 一次性真实MySQL 8.4迁移/密文/时间步并发回滚/有效指针切换/缺失键间隙锁 | 7 | /private/tmp/iam-totp-mysql-test.log |
| Boot Gateway固定协议/HTTP适配/真实安全过滤链 | 17 | /private/tmp/iam-gateway-totp-test.log |
| System现有安全策略回归及测试源码编译 | 4 | /private/tmp/iam-four-factor-system-regression.log |

合计162项。IAM其中包含40项本批新测试及94项既有测试；Redis测试启动本机临时独立端口并在结束后销毁。MySQL测试只访问一次性容器与随机测试schema，测试结束删除容器；未读取或修改部署库。真实SQL与Redis结果不替代浏览器/真实供应商/端到端认证证据。

IAM/API本地package -Plocal -DskipTests=true成功（/private/tmp/iam-four-factor-package.log）；该项只证明打包，测试证据见上表。聚焦Checkstyle检查本批41个Cloud Java及7个Gateway Java，均0违规；日志/private/tmp/iam-four-factor-checkstyle.log及/private/tmp/iam-gateway-totp-checkstyle.log。相关tracked diff whitespace检查和所列Java尾空格检查通过。

过程中发现并修复：utf8mb4过宽VARCHAR在MySQL不成立，改为1024字节ASCII种子信封；Spring 7异常头只读，改为专用429异常提供只读Retry-After；pending截止时间不可缩短滚动预算窗口；取消pending同样核对原ACTIVE快照；RR旧快照不能承担设备墓碑复核，增加owner锁后的SQL当前读。这些是同一批准方案内的实现修正，不更改信任边界或历史迁移。

待完成且不可漏掉：System策略草稿/预览/启用及能力预检/离线切换检查；旧MfaMode/旧MFA表运行读写及旧API退役；IAM统一风险→因素执行链、原上下文权威证据及成果发布；所有因素总体预算接线；登录/敏感挑战TOTP适配；官方WebAuthn；四因素用户选择、个人及后台页面；剩余相应回归和正式Test/人工验收。当前代码不应部署为新旧混跑版本，不得宣称新合同已端到端生效。尚未创建本批镜像、迁移真实库、启用策略、切换容器、提交、推送或发布。


## 2026-11-20 System新策略管理接入与开发证据

继续用户已批准的最终四因素计划。本批实现：
- 新`/api/v1/iam-authentication-policy`读取、风险/因素分区保存、预览及启用。严格字段形状及嵌套解码，版本仅十进制文本，不接受旧mode/adaptive、伪造owner/模板秘密或HTTP打开部署门禁。
- 草稿采用锁内分区合并、不可变版本及CAS；有效指针不被草稿保存改变。预览绑定原owner、原jti/认证会话引用、安全版本、双版本/摘要、五分钟TTL。激活重新检查当前状态和能力，同事务消费预览/切换指针/写脱敏审计；回滚无局部提交。
- 主库复核原AccessToken发布期限、owner/设备/认证会话/版本/issuer/来源Client许可版本；机器/APIKEY、缺引用或陈旧已撤销成果拒绝。平台管理授权使用原已认证角色范围与当前本地直接赋权交集，执行反向与排他角色规则；不以旧ROOT声明绕过当前授权，不能将无法证明当前本地赋权的群组声明推断成平台管理权限。
- 共用能力矩阵检查当前允许的主登录路线、绑定、设施、范围、独立性和强度；SMS+EMAIL或同类持有方式不算独立MFA。已装配且真实UV的Passkey能力可满足强度模型，但当前官方WebAuthn尚未接入时设施明确false。所有登录档BLOCK时拒绝操作者自锁启用。
- System旧`/api/v1/mfa-policy`及preview明确410，删除旧MFA JSON读写、旧服务方法和无引用`vo/MfaPolicyPreview.java`；保留MfaPolicyMapper但仅承载新的布尔能力投影。历史V33测试直接验证历史SQL，不维持旧运行接口。
- V38补齐5个固定新URL权限，精确继承对应旧读/预览/保存URL授权，保留原权限行ID及原角色关系；不授予旧租户安全角色平台权限。IAS TOTP隔离schema补齐权限表以验证同一迁移源码。V31–V37未修改。
- System配置、`.env.example`及Compose仅新增默认false的`IAM_AUTHENTICATION_POLICY_ACTIVATION_ENABLED`，只控制后续activate；初始active=1可读，HTTP不能打开。只改示例/源码，真实.env、配置中心与运行策略未修改。

实际开发验证：
| 范围 | 通过数量 | 证据 |
|---|---:|---|
| System生命周期/严格HTTP/能力矩阵/配置门禁/旧API410/现有安全策略回归 | 21 | /private/tmp/iam-system-policy-unit.log |
| 一次性MySQL实际DAO/新迁移/原发布期限/当前权限与反向排他/URL授权不升级/并发分区保存与显式重载合并/预览竞争及回滚 | 11 | /private/tmp/iam-system-policy-mysql-test.log |
| IAM上一批TOTP迁移及策略围栏回归（含新增URL权限表的最小schema） | 7 | /private/tmp/iam-totp-mysql-test.log |

本轮共39项，0失败/错误/跳过；与先前批次有既有回归重合，不作为新增用例数量累加。MySQL均为自动销毁的一次性容器与随机schema，未读写部署库。单元层可信JWT和端口替身与真实DAO/事务结果分开，不宣称覆盖完整登录/Token验签或真实供应商/设备。

聚焦Checkstyle检查23个本批System Java及IAM fixture，0违规，日志/private/tmp/iam-system-policy-checkstyle.log。System/API `mvn -pl macula-cloud-system -am clean package -Plocal -DskipTests=true`成功（/private/tmp/iam-system-policy-package.log）；干净包确认新controller存在、旧MfaPolicyPreview.class不存在、MfaPolicyMapper不引用退役表、默认false门禁仍在产物。跳过测试打包不代替上表测试证据。仅使用deploy/.env.example执行Compose config --quiet语法检查通过，无容器启动/重建。

本批核对并修正的具体问题：新URL必须进入原Gateway URL授权链；全LOGIN风险档BLOCK不能因有密码/绑定而误报操作者就绪；本地角色查询必须保留反向/排他语义及原已认证角色范围，不能从直接关系行误推ROOT；删除源码后需clean package清除旧响应类生成物。均在已批准实现范围内记录，没有真实角色或策略变更。

整体仍在Build，不提出实现完成或正式Test验收：IAM旧MFA配置/执行链仍未替换，统一评估上下文→权威证据→发布及所有因素预算接线、登录/敏感TOTP适配、官方WebAuthn、四因素用户选择、后台/个人页面和离线切换预检仍未完成。System敏感管理的统一近期强身份证据守卫随该链一起接入，本批原发布状态/权限验证不是完整强认证验收。仅上游群组角色没有当前本地平台直接赋权时失败关闭，不据声明推断平台权限。Gateway现行权限缓存未操作，新URL需在后续授权协调切换中按既有刷新流程验证。当前不可部署新旧合同混跑的过渡源码，未迁移真实库、启用策略、更新镜像/切换容器、提交或推送。


## 2026-11-20 原评估上下文证据与MFA参数接入

- 风险评估从同一批主库锁定行同时生成评分摘要和生效值；显式snapshot重载不再读取另一批缓存/字典，loginFailureCount阈值与评分权重来自同一map。本人因素强身份守卫已采用该路径。
- AuthenticationEvidenceStore新增原context写入/严格读取，由实际RR事务围栏验证平台→租户范围→owner；完成时间使用Boot DateTimeConverter按UTC转换。上下文包含原完整平台文档/版本/摘要、租户评分摘要、owner/安全版本/设备/认证会话/应用/场景/风险及要求。所有四因素走AuthenticationFactor，未将TOTP/PASSKEY伪装为消息Channel。
- V39源迁移追加evaluation_context JSON NULL，保留历史行但不回填、不导入；新证据查询忽略NULL，损坏/不完整JSON失败关闭。原证据与当前要求必须同身份/版本/租户/userType/设备/认证会话/应用/主因素/有效策略/评分摘要，并同时满足当前与原窗口、独立性、因素开关/适用范围、允许因素及强度/UV、环境与风险变化规则。不续期，不将原证据重标当前策略。
- TotpCredentialService.verify在同一外层RR事务消费lastAcceptedStep后写入原context证据；登记confirm仍只激活凭据并消费确认步，不写通用认证证据。FactorManagementGuard改用唯一上下文证据存储，删除重复managementEvidence Mapper/XML投影。
- MessageSecurityGuard的MFA参数读取新因素配置，与租户公共消息约束取严：TTL/尝试较小、冷却较大。禁用因素/新配置缺失拒绝；非MFA用途不读新因素配置；没有增加Redis计数入口。普通防御分区读取不再附带访问退役MFA表。
- 按批准计划删除PlatformMfaPolicyReaderTest旧JSON合同测试；严格新有效读取与防御读取隔离证据合入AuthenticationPolicyReaderTest。新增AuthenticationEvidenceStoreTest，是原计划证据上下文证明的精确测试文件展开；历史AccountSecurityPersistenceIT仅补齐当前构造依赖，不把其旧局部schema冒充新链验证。

开发验证（不是正式Test/Build人审通过）：

|证据|实际结果|
|---|---|
|IAM clean package及20个相关测试类|152项，失败/错误/跳过0；包括原登录/敏感访问/账户/设备与Redis预算回归；/private/tmp/iam-context-final-package.log|
|IAM原context/TOTP隔离MySQL 8.4.6|9项通过，含V38/V39、TOTP步进与证据同事务回滚、同码重放、迟到时间不覆盖、旧NULL行保留不复用、平台切换与缺失评分键间隙锁及旧证据拒绝；/private/tmp/iam-totp-mysql-test.log|
|System隔离MySQL迁移/策略事务|11项通过，包含新增V39字段后再跑；/private/tmp/iam-system-policy-mysql-test.log|
|聚焦Checkstyle|19个相关Java，0违规；/private/tmp/iam-context-checkstyle.log|
|候选产物|IAM/API干净构建通过，包内确认新证据Mapper的saveContext/contextEvidence/evaluation_context/FOR SHARE及TOTP service存在；未构建或切换新镜像|

本批单位测试中的SQL/权限替身与真实MySQL证据分开；Redis预算测试使用本地测试Redis。外部供应商实际投递和真实设备WebAuthn未验证。临时MySQL随机schema/容器已清理；真实数据库仍未迁移、未设置真实TOTP密钥、未启用策略、未切换应用容器。

全计划仍未完成，不能部署过渡源码或请求implementation-complete验收：旧MfaPolicyService/旧配置模型及登录/敏感挑战/Token发布链的整体替换、用户四因素选择、官方WebAuthn、System统一近期强身份守卫/离线预检、后台与个人UI仍待后续实施。旧OTP record/recent端口尚待随调用链一起移除；新路径不会调用其旧配置读取，不宣称旧运行合同已全部退役。


## 2026-11-21 菜单与按用户密码策略工程计划

用户“对的”确认产品方向，已将security-settings-structure.md的产品确认记录同步，新增具体security-settings-plan.md。沿用sdlc-build进入Phase A；codex-plan-review不可用，复用sdlc_reviewer_risk_plan仅做Plan pass，实际2轮。首轮3类Important已修订：两端遗留密码语义/禁用状态、显式RR与排序锁、框架编码升级及原凭据代际。第二轮按精确diff无新增Important/Minor/Nit。详review-findings.md。

本次只修改SDLC文档，无新业务源码/测试执行/真实数据库或环境变更；工程计划Status仍为draft，等待工程师对完整文件清单、目录级密码策略权限、初始化和锁序方案的具体批准。批准后把完整清单并入主plan.md再实施，不只以链接扩大原计划。


2026-11-21实施批准：用户YES批准security-settings-plan.md的具体工程方案，随后明确个人中心同批实现。已将完整Files/顺序/Risks/Proof并入主plan.md；开始Build源码，不进行真实迁移/环境切换。原四因素、个人中心和新密码策略均须完成才可请求整体验收，不能把局部模型/页面当成全链完成。


## 2026-11-21 按用户密码基础链路与个人中心开发增量

本批在用户YES批准的security-settings-plan及“个人中心也一并实现”范围内实施。为响应个人中心同批要求，先接通IAM真实本地凭据和本人改密/恢复，再完成个人页面开发增量；System所有密码写入、新后台菜单及策略管理仍必须后续整体完成。没有把本批源码当成可部署的过渡版本。

已实现：

- API完整不可变密码文档、真实LOCAL_EMPLOYEE凭据owner、唯一默认/指定解析器；缺失/禁用/悬空绑定及摘要损坏失败关闭，不读旧密码字典回退。
- System只读离线预检及V40源迁移：全部实际租户上下文比较IAM与System原密码语义，禁用行/重复/无效/差异在DDL前拒绝；7张表、真实密码代际列及基于原文档的种子。未执行真实迁移。
- 两端新表Mapper；IAM当前读/RR围栏、真实本地来源标记、原SQL哈希与代际携带、框架编码升级CAS（不改代际/年龄/历史）、个人改密/恢复的代际CAS及历史同事务。恢复Operation保留原凭据元数据，不把后读新版本重标旧证明。
- 个人中心`GET /api/v1/me/password-requirements`返回本人实际默认/指定策略的白名单规则与字符串版本，不暴露弱密码名单/owner/哈希，外部同号employee不能查询本地规则。页面据实际长度/复杂度/历史/期限展示，读取失败禁用改密。
- 个人MFA四张卡片共用手机号/邮箱绑定；TOTP接本人登记/确认/取消/命名/撤销真实端点，替换须确认。登记材料仅在当前对话框，成功/取消/过期/pagehide清除；取消后端失败也立即清除。确认保留动态码前导零，不写通用通过证明，提示等待下一组码。
- IAM引入BOM管理的官方spring-security-webauthn 7.0.7（传递webauthn4j-core 0.31.6.RELEASE）；新增官方两类数据库仓库及Mapper，随机稳定handle/alias、公钥/归属不可改、已撤销ID不复活、10个有效凭据上限、原版本CAS。仓库通用delete拒绝，未来本人服务须完成最后因素及安全版本/outbox事务。当前PASSKEY设施能力仍false，卡片明确未开放，不伪报注册/验签或设备支持完成。

本批实际开发验证（不是SDLC正式Test人审或全链完成）：

|证据|实际结果|
|---|---|
|API密码模型/解析|7项通过；/private/tmp/iam-password-model-test.log|
|System密码预检/CLI|6项通过，API另7项；/private/tmp/iam-password-bootstrap-test.log|
|System V40隔离MySQL|7项通过；/private/tmp/iam-user-password-mysql-test.log|
|IAM原来源/读围栏/编码升级|13项通过，API另7项；/private/tmp/iam-password-reader-test.log|
|IAM改密/恢复/账号证明|41项通过，API另7项；/private/tmp/iam-password-change-test.log|
|IAM密码SQL隔离MySQL|9项通过，含真实gate历史失败回滚及策略切换/编码升级竞争；撤销适配器替身不冒充outbox联调；/private/tmp/iam-user-password-reader-mysql-test.log|
|个人端点及安全边界|21项通过；/private/tmp/iam-personal-policy-test.log|
|真实模板DOM|15项通过；/private/tmp/iam-personal-ui-test.log|
|真实Thymeleaf模板+Cypress|8项通过、0跳过；320/390/1440布局、联系方式/解绑/密码要求/TOTP取消；HTTP mock，不调用真实供应商；/private/tmp/iam-personal-cypress.log|
|Admin生产构建|通过，现有bundle大小警告；/private/tmp/iam-personal-admin-build.log|
|官方Passkey仓库隔离MySQL|7项通过，原生记录为持久层fixture，不能作为密码学验签证据；/private/tmp/iam-passkey-repository-mysql-test.log|
|聚焦Checkstyle|个人中心3文件及WebAuthn仓库4文件，均0违规；/private/tmp/iam-personal-checkstyle.log、/private/tmp/iam-passkey-checkstyle.log|

浏览器预览由当前真实Thymeleaf引擎渲染到临时目录，使用本机静态服务器与明确HTTP拦截；已检查手机与桌面截图。Cypress首次使用旧env入口导致8项pending，未计作通过；改用当前expose参数实际执行后，修正隐藏联系方式分区的旧断言，最终8项全部通过。

全计划仍未完成：System密码管理/所有写入及V41三菜单、原凭据/策略贯穿最终Token/刷新/Remember-me发布、旧MFA执行链替换、四因素统一挑战/选择、原生WebAuthn注册/登录/敏感补验、个人Passkey列表/命名/注册/撤销、后台两tab与统一强身份管理守卫/离线认证预检仍待完成。不请求implementation-complete，不执行真实迁移、启用配置、生成生产凭据或更新镜像。


## 2026-11-21 个人中心Passkey原生注册与管理增量

延续已批准Build，实际完成：固定RP/精确Origin只读部署属性（缺失配置只标未就绪）；独立官方Jackson2 codec（不替换业务Jackson3/不开DefaultTyping）；原Session/owner/完整有效策略/RP/唯一Origin绑定的Redis固定DTO与3分钟GETDEL原子claim；官方创建/注册Filter装配于现有浏览器Security链；原生operations局部稳定alias视图，不改变原SecurityContext；每个Origin单独官方验签器；UV/resident required；强身份/CSRF/RR/有界body守卫和提交前响应缓存；本人Passkey列表/命名/最后因素保护/软撤销及页面系统窗口、取消/离开清理。

原生options类没有反序列化creator，实际隔离测试确认需要按既定固定DTO恢复官方builder；完整保留原challenge/算法/超时/排除凭据及当前支持的credProps扩展，未知扩展拒绝而非忽略。注册挑战预算标识改为含用途的整体摘要以符合既有64字符约束，不放宽预算。官方Origin/challenge/UV拒绝映射固定400，限频和设施错误保留429/503及Retry-After；不存在客户端verified=true或裸Session登记捷径。

协议回归发现既有新增发布契约的真实缺口：基础RegisteredClient没有保留authorizationVersion，最终发布读取基础仓库时正常授权码兑换失败。已补原数据库值、缺失仍拒绝；Client衍生缓存独立publication后缀隔离旧投影，应用快照及授权/Session键不变。精确文件及风险写入plan.md。协议测试切片显式适配新增发布端口/回调和原生凭据、设备/认证会话fixture；这些替身不算真实SQL发布线性化证据。

实际开发证据（非正式Test人审/全计划完成）：

|证据|结果|
|---|---|
|Spring原生注册安全/原协议/客户端投影/个人API/TOTP/强身份回归及package|100项通过，0失败/错误/跳过；含IamProtocolTest 53、WebAuthnSecurityTest 9；/private/tmp/iam-passkey-registration-package.log|
|真实MySQL 8.4.6+本机独立Redis+官方过滤器/校验器|8项通过，含合法UV登记、无UV/错Origin/错challenge/重放、跨Session、并发只一赢家、原过期拒绝、真实SQL列表/命名/最后因素规则及软撤销；/private/tmp/iam-passkey-registration-mysql-test.log|
|最后缓存后缀调整后的定向回归及当前package|8项通过；/private/tmp/iam-passkey-cache-final-package.log|
|真实模板DOM与浏览器API合同|23项通过；/private/tmp/iam-passkey-ui-tests.log|
|真实Thymeleaf模板+Cypress|9项通过、0跳过；含320/390/1440布局及系统窗口返回后的登记/命名；HTTP和系统响应为替身；/private/tmp/iam-passkey-account-cypress.log|
|Admin生产构建|通过，现有bundle体积警告；/private/tmp/iam-passkey-admin-build.log|
|聚焦Checkstyle|本轮20个Java相关文件0违规；/private/tmp/iam-passkey-registration-style.log，精确清单/private/tmp/iam-passkey-registration-java-files.txt；包括共享缓存常量及Mapper|

隔离原生注册IT使用P-256公钥及标准none attestation fixture，由真实官方operations验证challenge/RP/Origin/UV并落真实SQL；近期数据库强身份证据守卫与撤销outbox端口为明确替身，因此不宣称完整证据/发布链或真实设备验收。最后因素拒绝调用生产实际规则，DB变化真实；不把outbox替身当作所有Session/Token已联调。缓存、SQL和浏览器证据分开，未创建生产Passkey。临时MySQL/schema/Redis按测试生命周期清理，预览静态服务只为本次测试。

待办仍为全计划必需：原生匿名Passkey认证Provider/Token及assertion选项；登录MFA与原Bearer敏感补验四因素统一执行/预算/证据；最终密码代际与策略贯穿发布/刷新/Remember；System所有本地密码写入/密码策略管理、V41三菜单及后台两tab；统一强身份管理守卫/认证离线预检；虚拟认证器及真实设备/供应商验收。当前Passkey认证能力保持false，只有注册设施就绪状态可显示。全计划未完成，不请求implementation-complete，不部署过渡源码或更新镜像。

补充缓存行为验证：`MaculaRegisteredClientRepositoryTest`新增真实Spring Cache拦截用例，预置缺原版本的旧命名空间对象，当前读取加载含真实版本的新投影且随后命中新缓存；该组8项通过，/private/tmp/iam-passkey-cache-test.log。该用例数据端口为替身，不冒充真实数据库/Redis缓存联调。

## 2026-11-21 Passkey 补验断言校验

IAM 增加登录补验和敏感操作补验共用的内部断言校验。Redis 按原身份、用途、原请求摘要及原挑战隔离，在验签前原子消费。旧请求完成后的清理保留并发请求新签发的挑战。选项期限受原挑战和三分钟上限约束。

实际签名回归确认，限定凭据列表时官方库按凭据定位用户。IAM 现在额外核对响应中可选的 userHandle 与原账号句柄。当前库允许在新挑战上提交相同非零计数，继续采用已批准的库规则。用独立签名验证递增计数落库，并用错挑战和单次消费用例验证重放防护。证据和凭据元数据在同一事务提交。证据写入失败时测试验证元数据回滚，已消费的 nonce 保持失效。

本增量仍需接入登录 MFA、敏感操作的原 Bearer 补验和匿名 Passkey 登录。两条现有 MFA 主链仍引用旧策略，接入时需一起切到原始 PolicyEvaluationContext。后台三菜单、密码策略管理与真实设备验收仍在完整计划范围内。

最终定向验证通过 98 项，无失败或跳过。含 12 项真实签名与 MySQL/Redis 证据测试、9 项 Redis 挑战测试、15 项注册和凭据存储测试、62 项 WebAuthn 安全链和 IAM 协议回归。IAM 打包通过，22 个关联 Java 文件 Checkstyle 零违规。本轮详细日志位于 /private/tmp/iam-passkey-final-regression.log 和 /private/tmp/iam-passkey-assertion-style.log。

## 2026-11-21 普通登录策略、个人中心和风险评分

- 普通登录使用生效策略的放行、补验或拒绝动作。账号强制要求与因素可用性由服务端判断。挑战绑定原账号、设备、认证会话、应用、风险和策略。发送与核验前用原环境重新评分，策略版本变化或风险要求收紧时拒绝原挑战。
- 邮箱、短信、动态口令和 Passkey 接入登录选择页。动态口令与 Passkey 使用专用 Session 和 CSRF 路径。续接同时要求已提交的验证证据和一次性挑战消费，授权码交换不把旧回执视为有效验证证据。个人中心的邮箱和短信偏好读取生效因素文档。
- 租户风险评分移动到多因素认证的「MFA 策略」页签。评分沿用租户接口与授权，写入与平台策略草稿分开。恢复了通用安全已有保存方法的页面入口。
- Maven 定向回归 IAM 140 项、Gateway 34 项。含独立 Redis 投递和授权记录用例，以及使用真实风险规则、模拟策略持久化的普通登录用例。Vitest 8 项、Cypress 21 项，包含实际 Thymeleaf 页面与 HTTP/浏览器凭据接口替身。管理台生产构建通过，Java 定向 Checkstyle 0 违规。日志分别在 /private/tmp/iam-login-consolidation-final.log、/private/tmp/iam-login-gateway-final.log、/private/tmp/iam-mfa-scoring-vitest.log、/private/tmp/iam-mfa-current-cypress.log 和 /private/tmp/iam-login-final-style.log。
- V41 菜单、按用户分配密码策略、匿名 Passkey 主认证、密码代际与认证策略的最终发布事务、旧配置与证据读者退役，以及实际设备和供应商验收仍在实施。

补充验证：`mvn -o -pl macula-cloud-iam -am test` 的 IAM 模块 760 项通过，0 失败、0 错误、0 跳过。真实数据库和设备的端到端验收单独进行。日志在 /private/tmp/iam-login-full-surefire.log。

## 2026-11-21 按用户密码执行和认证成果提交

- 登录、MFA 续接和 Remember-me 使用本人有效密码策略，保留原本地凭据代际、有效策略及实际改密时间的摘要。续接校验原摘要。密码到期与提前提醒采用策略文档的生效值。
- 新用户 Access Token 与续期成果在提交事务重新校验 MFA 原策略、原补验证据及本地密码快照。浏览器会话登记同样执行事务内复核。账号发布锁之前获取密码目录锁，账号锁之后完成凭据当前读。拒绝时不暂存本次 Redis 记录。仅保存既有 Token 元数据仍按原发布账本规则处理。
- System 用户新增、导入、资料显式改密及管理员重置复用新版规则。现有用户密码写入依序锁目录、派生身份和本地凭据，真实写入同时推进密码代际及历史，撤销原账号成果。通用安全移除旧密码表单，旧接口返回 410，历史字典仍作迁移预检数据。
- IAM Surefire 762 项全通过，System Surefire 274 项通过、1 项原迁移测试跳过。隔离 MySQL 的 IAM 28 项与 System 8 项通过，验证规则切换后的原快照拒绝、发布前拒绝保护原 Token、密码与代际同事务提交和回滚。管理台 Vitest 8 项与 Cypress 7 项通过，Cypress 使用了模拟接口。定向 Checkstyle 零违规。
- 本地日志在 /private/tmp/iam-password-consolidation-backend.log、/private/tmp/iam-password-publication-mysql.log、/private/tmp/iam-system-password-generation-mysql.log、/private/tmp/iam-password-menu-vitest.log、/private/tmp/iam-password-menu-cypress.log、/private/tmp/iam-password-consolidation-style.log。
- 独立密码策略管理和分配入口、V41 菜单迁移、独立授权码写入点的最终围栏、Passkey 主登录及真实设备、供应商验收仍需完成。

### 授权码写入点补充

授权码使用已有提交事务的策略及账号锁，复核原认证、密码策略及补验证据后才写入 Redis 记录和索引。拒绝时保留框架授权端点错误类型。回归以受控策略变更拒绝验证授权记录和 code 索引均不可见，并验证通过复核后正常写入、兑换和刷新拒绝行为。IAM 定向 94 项通过，零失败、错误或跳过。该组使用本机 Redis，SQL 策略端口为替身。日志在 /private/tmp/iam-authorization-code-fence-final.log。

## 2026-11-21 密码策略管理和 Passkey 主登录

- 新增独立密码策略页面及 V41 菜单迁移源码。平台管理员管理默认与用户指定策略。规则、分配和显式替换先写草稿，经原操作者预览后同时启用。默认策略不可停用，停用指定策略后原用户回到默认。HTTP 精确权限、主库当前角色、原 Token 发布账本及实际近期 MFA 证据共同决定写权限。
- Passkey 主登录已接通托管登录页及两条匿名 POST。挑战不枚举用户，由原 Session 一次性消费，并校验 Origin、RP、托管应用与版本。官方验签确认 UV，应用、身份、风险和会话围栏继续生效。满足当前规则要求时不追加验证码。
- 后端共享 API 38 项、IAM 764 项、System 274 项通过，另有 1 项原 System 迁移测试跳过。Passkey 到期跳转收尾后 18 项定向通过。V41、密码策略写入及角色证据查询的隔离 MySQL 回归 10 项，官方 ECDSA 断言隔离 MySQL/Redis 回归 14 项。管理台与实际渲染的 IAM 登录页浏览器回归 10 项，使用模拟 HTTP 和设备接口。管理台构建和聚焦 Java Checkstyle 通过。
- 未在部署环境执行迁移或更新镜像。实际 Apple、Windows 和 Android 设备以及短信、邮件供应商联调仍待验收。隔离测试中的权限或目录替身不作为真实环境验收证据。
- 日志位于 /private/tmp/iam-consolidated-final-backend.log、/private/tmp/iam-passkey-expiry-final.log、/private/tmp/iam-password-policy-management-mysql.log、/private/tmp/iam-passkey-primary-mysql.log、/private/tmp/iam-consolidated-final-cypress.log 和 /private/tmp/iam-consolidated-final-style.log。

### 本地镜像构建

重新打包了 IAM、System、Gateway 和 Admin，并更新四个本地 `6.1.0-SNAPSHOT` 镜像标签。校验了镜像中的实际 JAR 或静态资源、Gateway 内嵌 Starter 摘要以及 System V38–V41 迁移产物。开发库当前为 V34，旧 MFA 配置表仍存在。镜像构建未启动新容器或运行数据库迁移，原四个容器继续使用旧镜像并保持健康。
