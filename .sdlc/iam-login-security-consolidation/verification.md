# Boot配套开发验证记录

日期：2026-11-20。范围仅为已批准计划中的Boot Crypto认证信封与Gateway Passkey控制代理。属于Build开发自测，不能替代Cloud端到端验收或人工完成确认。

## 本轮修正与复现

- 原`HttpMfaChallengeClient`对同一次Passkey断言重复添加`credential`。先将现有测试加强为`containsOnlyOnce`，实际运行失败并观察到两个表单字段；修正后通过。
- 原控制桥对整个Authorization使用忽略大小写比较。改为仅Bearer方案名忽略大小写，Token逐字匹配；测试证明不同大小写Token拒绝且不调用IAM，混合大小写方案名配合正确Token允许。
- 统一断言构造校验，移除重复执行。补充options的错误挑战、嵌套额外字段、未知transport及超长有效期拒绝用例；失败不透传上游数据。
- AES-GCM实现保持原合同，仅修正AAD注释对分隔方式的描述。

## 实际命令与结果

```bash
mvn -pl macula-boot-starters/macula-boot-starter-crypto,macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test
```

最终BUILD SUCCESS。Commons 34、Crypto 32、Redis 5、Gateway 148，共219项；失败0、错误0、跳过0。未运行全仓测试或外部基础设施集成测试。

```bash
mvn -N checkstyle:check '-Dcheckstyle.includes=**/crypto/secret/*.java,**/gateway/security/mfa/MfaFactor*.java,**/gateway/security/mfa/MfaControlEndpoints.java,**/gateway/security/mfa/MfaChallenge*.java,**/gateway/security/mfa/impl/HttpMfaChallengeClient.java,**/gateway/security/mfa/HttpMfaChallengeClientTest.java,**/gateway/filter/MfaChallengeWebFilter*.java'
git diff --check
```

上述范围Checkstyle违规0；diff空白检查通过。原有其他未提交文件未清理、回滚或格式化。

## 证据边界

HTTP客户端与Security链使用可控内存测试；未连接真实IAM、腾讯、IPDataCloud、MySQL或Redis。未执行浏览器WebAuthn注册、登录、MFA或敏感补认证；未使用真实设备或生产RP。Cloud的权威证据存储已有初步实现，但本轮未修改或验证Cloud代码；供应商管理与WebAuthn仍属于未完成范围。

没有迁移真实数据库、启用安全策略、修改凭据、推送、合并、发布或重建镜像。本记录不批准任何SDLC gate。

## 后续：2026-11-20 本地镜像构建

用户要求“先更新镜像”后，已完成IAM、System、Gateway、Admin四个本地镜像构建；本节覆盖前述“未重建镜像”的旧时点状态，不扩大到运行容器切换或数据库授权。

构建使用当前本地Boot Crypto/Gateway及依赖的deploy-profile install，再对Cloud三个后端执行`clean package -Plocal -DskipTests=true`；Admin执行`npm run build -- --mode docker`。普通增量package曾保留Gateway旧Boot依赖，通过嵌入类哈希发现后已改clean重打包，并重新验证6项关键类与当前target/classes一致。

镜像复用更新前本地JRE/Nginx运行层，只替换新JAR或完整Admin dist及现有Nginx配置/入口脚本；未修改仓库Dockerfile。已检查镜像内完整JAR/全部静态文件与构建产物一致，User、Entrypoint、Cmd、WorkingDir、ExposedPorts、Env均与旧镜像一致。

| 服务 | 新镜像ID前缀 |
| --- | --- |
| IAM | 6996cfeafe2a |
| System | 413a880413bf |
| Gateway | a8132532946f |
| Admin | 9ef6371ee900 |

统一镜像标签仍为`macula/<服务>:6.1.0-SNAPSHOT`；旧镜像保存`before-consolidation-20261120-200139`回退标签。构建、元数据与产物核验记录在`/private/tmp/iam-images-refresh-rme9z2hk/`。打包跳过测试，不把打包成功当作新增测试证据；此前三项修复的92项测试见review-findings.md。

全部34个现有容器（含退出的初始化容器）身份未变，四个目标服务仍running/healthy并继续运行旧镜像。没有启动新应用临时实例；镜像核验的临时容器仅create/cp后删除，未start。

只读Flyway历史确认真实本地数据库仍为V34；旧设备配置为deviceMaximum=0、sessionMaximum=2，没有新设备分区或来源选择。新System启用Flyway，重建将应用V35–V37；已批准计划149行明确“实施批准……不包含真实环境迁移”，因此暂停运行切换，等待用户另行确认。迁移新增供应商/凭据/证据账本表及会话字段，不自动启用云服务或登记Passkey；旧Token缺少权威发布证据需重新登录。


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
