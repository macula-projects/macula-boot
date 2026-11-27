# Spec: IAM 登录风险、在线会话与安全策略执行 (from intent.md 2026-11-15)

> 2026-11-15 回调协议修订已由用户 YES 批准，以 [callback-revision.md](callback-revision.md) 为准；原文的 JWS/网关版本缓存部分不再实施。
Status: accepted

## Source intent
[intent.md](intent.md)，Status: accepted；用户明确回复 YES，本阶段仅设计。

## Requirements
1. 在线会话按用户名分页查询，或以 POST 请求体中的完整 token 精确查找所属用户；只返回 token 指纹。提供禁用/启用账户、全部踢出、锁定/解锁、启用/停用账户 MFA，操作逐项鉴权和审计。
2. 登录审计保存用户、可信 IP、原始 UA、解析设备/浏览器/系统、地理位置、认证方式、签发 client_id、结果、风险分数和原因；签发前 client_id 可为空，不能伪造。默认保留三个日历月。
3. 所有用户认证、SSO 复用、授权码兑换、external_code 和刷新路径执行风险及账户状态检查；client_credentials 跳过用户风险策略，保留原认证授权。
4. 低风险满足其他策略后放行；中风险完成短信或邮箱 MFA；高风险拒绝、永久风险锁定并在 IAM 立即撤销全部会话及 token，仅管理员可解锁；资源服务器尽力同步踢出，允许有限期不一致。
5. 用户主动或管理员启用 MFA 后，低风险同样必须 MFA；无可用绑定邮箱/手机号则拒绝。因子目的地取服务端绑定，不接受请求临时地址。
6. 全部踢出跨该用户全部客户端，覆盖 access/refresh token、未完成授权码、SSO、记住登录和 MFA 挑战；全部已接入资源服务器均纳入回调，持续重试并可查询逐目标确认状态；不要求所有资源服务器在同一时刻完成。
7. 通用、防御及密码策略在所有相应入口生效；管理员导入、创建、重置密码均不能绕过。

## Non-goals
不新增 TOTP、恢复码或外部风控平台；不开放任意 HTTP 模板映射；不限制 client_credentials 的用户风险；不迁移存量密码，不清理现有应用、凭据和绑定；不部署、不推送。

## Design
### 政策与现状依据
适用两仓库 AGENTS.md、architecture.md，Cloud backend-development.md、frontend-development.md、testing.md，以及 REVIEW.md。未发现本需求专用组织级数据政策。bands.yaml 仍是示例基线，不能当作已批准的上线 SLO。
当前 MfaCompletionService、MfaSessionAuthenticationStrategy、ProtocolCheckingTokenGenerator、MaculaOAuth2AuthorizationService 提供接入点，授权记录已采用 Redis 摘要索引及并发版本；必须扩展而非绕开这些校验。Boot 现有 POST /gateway/rm/opaqueToken 按单 token 清缓存并默认强制 HMAC，不具备用户全量撤销协议。

### 职责与状态
System 管理策略、用户管理、资源服务器登记和 Flyway；IAM 负责风险、挑战、会话索引、撤销与回调调度；System API 放最小内部契约；Admin 提供管理页；Boot 提供通用资源服务器接收及校验能力。接口、impl、dispatch 按既有职责包划分。
账户身份键为 issuer + 目录类型 + tenantId（按目录语义允许空）+ userId，不能用用户名全局关联。禁用沿用用户 status；临时失败锁定、管理员锁定和高风险锁定分别存储，解禁不解除锁定，解锁不启用禁用账户。
每个用户保存单调递增 securityVersion，另存 mfaPolicyVersion。数据库中的账户状态与版本是权威来源，IAM 在用户内省、续接及签发时读取主库；Redis 仅作运行索引，不能用旧值或缺失值覆盖数据库状态。签发、刷新、SSO 与授权存储写入前后校验版本；数据库行版本串行化账户操作，Redis 授权保存沿用 CAS。若写入后账户版本已变化，撤回本次成果；内省始终比较授权携带版本和主库版本，不能因跨库非原子写入放行旧成果。账户启用 MFA 时递增策略版本并全部踢出，停用 MFA 须管理员权限与审计。

### 风险规则（本 spec 接受后的默认值）
分数限定 0–100，低风险 <30，中风险 30–69，高风险 >=70；租户配置版本随审计保存。
- 新设备 +30；同国家城市变化 +30；跨国家 +40，城市与国家分数不叠加；地域缺失/查询失败 +30，不按安全位置放行。
- 在 loginFailureDuration 窗口，失败数达到 loginFailureCount 的一半（向上取整）加 30；达到上限触发普通失败锁定，按 autoUnlockTime 解锁，不冒充高风险永久锁定。
- 初次登录无设备基线按新设备计分；无地域基线不记异地，仅已完成全部认证的登录可更新设备与地域基线，保留最近 90 天基线。
- 登录前按可信 IP 限频，初始 5 分钟 30 次，超过拒绝本次；未完成主认证的外部请求不能仅凭用户名触发永久高风险锁定。不存在用户返回统一错误并按 IP 限流，不创建账户状态。
- 设备使用随机服务端设备标识结合 UA；仍视为弱信号。主认证成功后才执行高风险账户处置，分数、原因分别保留。
IP 默认取直接连接地址，仅配置可信代理 CIDR 时解析转发链。地理服务使用固定适配器、HTTPS 基地址、环境变量 API key、连接/读取超时和响应大小限制；禁止重定向、任意请求字段映射和用户控制 URL。
首期按用户指定接入 IPDataCloud（IP数据云）的 IP 归属地接口，不再实现 IPWHOIS 适配器。官方文档：[用户指定入口](https://www.ipdatacloud.com/doc/ipguishudi/)、[官方帮助文档与示例](https://help.ipdatacloud.com/doc/ipguishudi/?tp=3&type=2)。仅接入归属地查询，不将该供应商其他风险画像/代理识别产品隐含加入本次范围。
- 固定契约：HTTPS GET https://api.ipdatacloud.com/v2/query，查询参数 ip（IPv4/IPv6）及 key（归属地产品密钥），UTF-8 JSON；使用 URI builder 编码参数，不手工拼接凭据。此处明确采用文档已有 GET 示例，不推断未说明的 POST 请求体格式。
- 响应成功须同时满足 HTTP 成功、业务 code 为 200、data.location 为有效对象；文档字段表将 code 标为 String，而示例为数字，兼容整数 200 与严格字符串 "200"，其他类型或值均拒绝。message/msg 仅为诊断信息，不作为放行依据，也不原样写日志。
- 从 data.location 映射 country_code、country、province、city、city_code、ip、isp；ip 若返回必须与查询 IP 在规范化后相符。国家按 country_code，城市优先以国家+province+city_code 判定，缺代码时使用国家+province+city；缺少本次比较需要的地域字段时记 UNKNOWN，不把两个空值当作同地。country_code 缺失不靠展示名称猜国家。
- 只保存归属地与审计所需字段，不存完整供应商响应、街道历史或经纬度。文档示例与解析 fixture 区分，当前仅完成官方文档核对，尚无带产品密钥的真实响应证据；Build 时以脱敏契约 fixture 验证，部署前另行联调 IPv4/IPv6 及所购产品字段。
- 配置前缀 macula.cloud.iam.ip-geolocation，provider=IPDATA_CLOUD，endpoint 默认 https://api.ipdatacloud.com/v2/query；api-key 由环境变量 IAM_IPDATA_CLOUD_API_KEY 或受控配置中心注入。端点仅由受信管理员配置，禁用重定向；真实密钥、完整请求 URL 和原始响应不得进入日志、异常、APM 或仓库，HTTP trace 应屏蔽 key 与 ip 查询参数。
- HTTP 非成功、业务失败（含文档列出的密钥过期/次数不足/IP 类型错误）、空响应、字段缺失、无效 JSON、超时及未配置密钥均记 UNKNOWN 和稳定原因码，进入既定风险规则；仅业务 code=200 但无有效位置也不能放行。供应商失败不更新成功地域基线。
私有/保留 IP 不对外发送，记 UNKNOWN；查询结果按 IP 缓存 24 小时，失败短缓存 30 秒，单次总预算 1 秒。更换 provider、端点或供应商配置版本后隔离缓存，不复用旧供应商数据；不同供应商以后通过独立适配器扩展。

### MFA 与协议恢复
挑战仅在主凭据/来源证明验证后创建，256 位随机句柄，默认 5 分钟，绑定用户、client、原认证事务、目录、来源版本、securityVersion 和绑定信息版本；不是 access token，不产生完整认证 Session。
邮箱/SMS 验证码与登录验证码使用不同 purpose、存储键和模板；复用现有投递接口，新增 MFA 模板场景。6 位随机验证码，仅存带密钥摘要，TTL=min(verifyCodeValidTime,300 秒)，60 秒发送间隔，最多验证 5 次、账户每小时每通道最多发送 10 次，并叠加现有 IP/client 限流。重发作废旧码，成功原子消费；更换绑定、锁定或撤销使挑战失效。
按用户明确要求，已绑定手机短信或邮箱二选一即可，不强制跨通道；即使主认证为短信或邮箱登录，也允许选择同一绑定通道，但必须重新发起独立 MFA 挑战及新验证码，不得复用主登录验证码或成功状态。只有一个绑定通道也可完成；两者均未绑定则拒绝。页面和文档不将同通道再次验证描述为独立认证因子。
浏览器通过受限页面继续原授权请求，MFA 完成才建立认证会话。external_code/刷新若要求 MFA，返回明确 mfa_required 和短期 continuation；服务端保存待完成上下文，完成后仅允许原 client 按原认证方式恢复，公共客户端绑定原证明，不能重复使用已消费的平台 code，不能新增匿名 MFA 换 token 通道。刷新前完成风险检查，未完成挑战不旋转或签发；完成后再次检查状态、版本及来源有效性。
同一认证事务内的会话、生成器、保存检查复用单次判定；新的登录/SSO/刷新重新判定，不能无限复用旧 MFA 成功。浏览器 MFA POST 使用 CSRF，续接操作不接受客户端自报已验证。

### 撤销与资源服务器
撤销先在数据库事务内原子递增 securityVersion，并保存锁定状态（如适用）和撤销事件，使旧授权、挑战、SSO 全部不可用；清理索引与写持久化撤销事件失败时保留待补偿状态，不能恢复旧版本。永久锁定持久化，IAM 重启仍生效；Redis 丢失时从持久化状态恢复完成前拒绝用户认证。
撤销一致性按用户补充要求改为尽力踢出：IAM 提交后立即拒绝旧版本授权的签发、刷新、SSO 续接及新的内省；资源服务器允许有限期持有旧的 active=true 缓存，不承诺全局瞬时失效，也不取消已进入业务处理的请求。
资源服务器 active 正缓存 TTL 默认 60 秒、可配置 0–300 秒，上限不得超过 token 剩余有效期；0 表示关闭。仅权威内省成功才能生成缓存，命中不得滑动续期；TTL 从本次内省请求开始时间计算，避免迟到旧结果延长不一致窗口。JWT 本地验签同样需要受该 TTL 限制的在线状态检查；不接受仅等 JWT 自然到期的无界撤销延迟。
回调到达时立即提高共享撤销版本并清理账户相关缓存；缓存读取和回填均比较该版本，拒绝旧版本结果，防止内省与回调竞态。IAM 暂不可达时仅允许尚在 TTL 内且未被回调判定撤销的缓存；过期、缓存未命中、撤销状态不可读取均拒绝，不使用 stale-if-error 延长有效期。未实现上述协议的外部资源服务器不纳入时间上界承诺，页面显示未完成接入。client_credentials 不执行用户版本检查。
全部 RESOURCE_SERVER 登记必须配置回调端点；多实例使用共享撤销状态，或者逐实例登记，负载均衡单次 200 不等于所有实例已同步。缺少回调的既有接入项明确显示未完成升级，禁止宣称全量同步成功。
新增统一 POST /gateway/iam/revocations，与旧单 token HMAC 端点并存。请求为 IAM 签名 JWS，含 eventId、issuer、aud（资源服务器 client_id）、iat、exp、身份键、securityVersion、撤销原因；不传完整 token。固定 typ=macula-iam-revocation+jwt、RS256、60 秒有效期，最多 30 秒时钟偏差；公钥取预配置可信 IAM JWKS，禁止信任请求提供的 jku。成功返回 eventId、appliedVersion、同步范围。
接收端先持久化最高版本再清缓存；重复 eventId 返回相同结果，旧版本不能覆盖新版本。401/403、错误回执或无能力声明均不能记成功。每次连接超时 1 秒、总超时 3 秒；失败按 1/5/30/120 秒退避后每 5 分钟重试，管理员可重试。新接入实例在提供服务前同步当前版本。
撤销事件与目标回执持久化，操作接口返回 operationId、IAM 撤销状态与资源服务器同步状态；不因回调失败撤销本地锁定。签名用途、issuer/aud/type 验证与普通用户 JWT 验证明确分离。

### 策略及管理页面
会话按闲置 sessionValidTime 过期，记住登录是受版本约束的续接凭据；rememberMeValidTime=0 禁用。sessionMaximum 以用户为粒度，超限默认拒绝新会话；不隐式踢旧会话。token 不延长到超过客户端有效期，并受对应登录授权生命周期约束。有效期配置变短时按新上限重新校验。
普通失败自动锁定按原配置；永久风险锁定优先。CSP 仅用于 IAM HTML 响应并测试第三方跳转及 MFA 页面。密码校验统一服务覆盖创建、导入、修改、重置；历史密码只存哈希，更新原子化，修改后全部踢出。连续序列默认连续 4 个数字/字母（含反向及键盘序列）；0 或关闭值按配置不启用。有效期到期仅允许受限改密，不签业务 token；提前提示按配置天数展示。

IAM 密码策略的「禁止包含单一字符」开关新启用时，阈值为 3，密码任意位置出现 3 个或更多连续相同字符即拒绝，例如 `Xaaa9!`、`A111z!`。`Xaa9!z` 和 `Xa9a!a` 均可通过此项检查，仍须满足其他密码规则。 已启用的策略按其保存的阈值执行。
Admin 增加“在线用户”“登录日志”“撤销同步”页签。列表展示用户名、目录、client、IP、设备、风险、MFA、会话状态；详情展示原始 UA 与判定原因。用户名支持分页筛选；token 输入使用密码框并走 POST、不进路由或持久存储。每个按钮独立权限和二次操作确认；踢出明确提示影响全部客户端。回调未完成展示待同步/失败及重试入口，覆盖空态、加载、错误和重复提交。

## Data and interfaces
- System 新增迁移（当前最新 V29，Build 时重查编号）：账户安全状态、会话/授权关联、登录审计、密码历史、撤销事件与逐目标回执、资源服务器撤销配置。各表含必要目录/租户/用户键、版本、过期时间和索引；禁止完整 token 进入新审计表。
- Redis 保存挑战、设备基线、限流、用户版本及会话/授权摘要索引；数据库保存账户安全版本、永久锁定、MFA 策略及撤销事件。事件与状态同事务保存，提交后推进 Redis 版本；未同步时对应账户签发及内省拒绝，故障恢复禁止以缺失值当版本 0 放行。
- System 对外管理入口 /api/v1/iam-sessions/search（POST，username 或 token）、/api/v1/iam-login-logs（GET）、/api/v1/iam-users/{id}/security（GET）、其下 disable、enable、lock、unlock、revoke、mfa（POST），/api/v1/iam-revocations/{operationId}（GET）、其下 retry（POST）。用户目录及 tenant 由权限上下文校验，不能仅凭路径 ID 更新。
- IAM 提供 /api/v1/mfa/challenges/{id}（GET）、其下 send、verify、continue（POST）。continuation 是不透明句柄，恢复前验证原 client/CSRF/绑定证明及一次性状态。System 到 IAM 通过受认证内部契约调用，不透传管理员身份作为用户登录证明。
- 所有接口时间点使用 Instant 的 ISO 时区格式；分页最大 100。登录审计以 UTC 时间截止 now.minusMonths(3)，每天分批删除，保留未完成撤销任务直到补偿结束；原始 IP/UA/设备仅在授权审计存储及详情可见，运行日志不输出。
- 更新 System/IAM/Gateway/Boot Gateway/Admin README、资源服务器接入说明、菜单与按钮权限、MFA 消息场景和配置文档；新增表只增不删，回滚应用前先撤销全部新授权并关闭入口，不能回退到不检查撤销版本的旧版本继续接流量。

## Flagged concerns
- 用户已选择尽力踢出并允许部分不一致；不再要求关闭全部 active 正缓存或全局瞬时拒绝。TTL 默认 60 秒、最大 300 秒已随本 spec 获用户 OK 接受；callback 不可达期间仍可能短暂访问，non-blocking（用户已接受取舍）。
- 用户已明确已绑定手机或邮箱二选一，取消强制跨通道限制。同通道独立新验证码属于再次验证，无法提供不同因子的独立性，按用户指定行为实施，non-blocking。
- 新设备与定位未知均触发中风险，首次登录也可能需要 MFA；首期供应商由用户指定为 IPDataCloud，外部查询会发送公网 IP；产品密钥、所购产品响应字段、额度和服务承诺须在部署时核验，不能沿用此前 IPWHOIS 的免费额度或 SLA 假设，当前未执行带密钥的实际 API 调用，non-blocking。
- 用户已明确“禁用”即禁用账户，启用不解除锁定，解除锁定也不启用账户。启用 MFA 默认撤销现有会话及初始风险阈值仍按本 spec 提议，已随整体评审获用户 OK 接受，non-blocking。
- 外部资源服务器必须升级统一回调、受限 TTL 内省缓存、撤销版本与缓存回填校验；本仓库只能实现平台端与 Boot 契约，第三方接入未经联调不能声明完成，blocking（上线验收）。
- 原始 IP/UA/设备按用户要求进入受控审计存储，属于对通用“敏感信息不进日志”规则的限定：应用日志仍禁止，审计访问须单独授权及记录；此项依据用户明确要求记录上述信息，不扩展到应用日志，non-blocking。
- 90 天基线及三个日历月审计保留不同；地理识别误差、未知位置和短信/邮箱投递不可用按明确失败路径处理，non-blocking。

## Verification strategy
单元测试验证阈值 29/30/69/70、首次/异地/未知地理、IPDataCloud 的 IPv4/IPv6 响应映射、数字/字符串业务状态码、data.location 缺失、响应 IP 不匹配、额度/密钥失败及敏感查询参数脱敏、失败窗口及锁定优先级、各密码策略边界和月末清理。验证码覆盖错码、过期、重发、并发双消费、跨租户/client/purpose、未绑定、仅绑定一个通道、同通道独立新码成功、复用主登录验证码拒绝及消息失败。
用隔离 MySQL/Redis 和 HTTP stub 验证真实迁移、永久锁重启恢复、并发签发/刷新/撤销、故障恢复、SSO/remember-me、绑定变更、来源失效与授权码重放；不对真实用户发送验证码。
至少两个资源服务器验证全部回调、错签名/aud/type、重复与乱序、一个离线、重启补偿、缓存回填竞态及 JWT 路径；确认 IAM 撤销后立即拒绝旧授权，已确认回调的资源服务器拒绝旧凭据；回调未达时允许缓存 TTL 内短暂访问、超过 TTL 必须重新检查或拒绝。覆盖内省迟到、滑动 TTL、长寿命 JWT、IAM 不可达及自定义 TTL 上限，未确认不显示全量完成。第三方实际端点联调单独记录证据。
Admin 用 Vitest/Cypress 覆盖 token/用户名查询、授权按钮、未绑定拒绝、MFA、永久锁、管理员解锁、全部踢出和同步状态；检查桌面/移动布局。
Build 后按目标模块 mvn test -Plocal 起步，再执行全仓后端 test/verify、Admin build 与相关 e2e、变更 Java Checkstyle 和 diff 检查；记录跳过及环境缺失，不把 stub 结果作为真实供应商或第三方已验证。压测记录默认缓存、关闭缓存及地理查询超时下的 p95/p99，数值上线目标需另行批准。

参考：[IPDataCloud 官方归属地接口](https://www.ipdatacloud.com/doc/ipguishudi/)、[RFC 7662 内省与缓存风险](https://www.rfc-editor.org/rfc/rfc7662.html)、[RFC 8725 JWT 类型与受众校验](https://www.rfc-editor.org/rfc/rfc8725.html)。供应商限额及服务承诺以部署时重新核验为准。
