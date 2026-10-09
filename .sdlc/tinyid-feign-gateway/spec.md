# Spec: TinyID 号段客户端统一网关接入 (from intent.md 2026-10-09)

最新已接受修订：四个发号接口均移除 TinyID token 参数及校验，只保留 bizType 和适用的 batchSize；Server 删除全部发号匿名白名单，网关精确路由四个接口并沿用共享认证，不新增 HMAC/个人 Token 区分。管理端历史 Token 数据及功能保留。本条覆盖下文旧 token 兼容说明，实施依据为 Boot 仓 plan-remove-issuing-token.md。
本轮实现及最终复验已完成；最新测试结果以 Boot 仓 verification.md 的“2026-10-09 最新修订复验”节为准，下文早期状态仅作历史记录。
Status: accepted.

最新用户决定：撤销本次新增的 TinyID 专属 HMAC/个人 Token 区分，删除 TinyIdHmacGlobalFilter 及其专属拒绝测试，沿用共享网关认证授权逻辑；身份边界留待用户统一处理。此决定覆盖下文历史 HMAC-only 约束，不等同于该安全边界已修复；现有 HMAC 验签、URL 授权、JWT 校验及路由前缀处理保留。

## 已接受的 Controller 与管理端最终修订

用户 YES 批准：发号 Controller 不声明 Result、不捕获或包装业务异常，Service 承接授权、批量约束及必要异常转换；旧 token 失败也统一错误 JSON。TinyIdAdminController 不使用 NotControllerResponseAdvice，普通管理请求由 Advice 包装 Result，管理前端参照 system 检查 success 并读取 data。此项明确扩展到 TinyID 管理前端，覆盖历史非目标及原响应兼容说明；不改 IAM、数据库或其他管理功能。

## 已接受的 Server 异常链路修订

用户 YES 批准 [plan-server-exceptions.md](plan-server-exceptions.md)：Server 引入 Macula Web 统一处理 BizException，新号段接口成功仍为五字段文本，失败由 HTTP 200 空串改为 HTTP 500 Result JSON；Service 抛出具体 TinyIdSysException 结果码，Feign 与缓存生成器保留 code/msg/cause。此修订覆盖前文历史补充中的“不改新接口捕获行为”；三个旧 token 接口和管理响应保持兼容。

## 已接受的评审修复补充

用户批准网关号段入口仅允许 HMAC 应用调用；普通 Bearer/API Key 不可绕过应用验签和 URL 许可。Server 继续信任有效 JWT，不新增 JWT 来源声明。TinyIdSysException 改为继承 BizException，复用其 ResultCode 构造契约及 code/msg，保留旧构造方法、详情和 cause，默认 SYS_ERROR；不改号段文本协议和现有捕获行为。实施依据见 [plan-review-fixes.md](plan-review-fixes.md)。

## Source intent
[已接受的需求](intent.md)：申请号段改用 Feign 经网关访问，客户端只传 bizType，其余发号机制保持不变。

## Requirements
1. HttpSegmentIdServiceImpl 使用专用 Feign 接口申请号段，不再拼接服务地址或传递 TinyID token。
2. 使用 macula.cloud.endpoint、app-key、secret-key，与 system starter 的 HMAC 认证方式一致；客户端在后台预加载时也可独立认证。
3. 保留号段缓存、预加载、delta/remainder 算法、IdGeneratorFactory 和 SegmentIdService 公共调用契约。
4. Server 接受只含 bizType 的号段请求；网关验签并转发 JWT，Server 拒绝未认证请求。
5. 自定义 SegmentIdService 的应用（包括 TinyID Server）无需配置远程客户端凭据；由自定义号段服务覆盖默认远程实现。
6. 同步更新依赖、配置、README、调用示例与测试，不修改数据库或管理端功能。

## Non-goals
不改本地发号算法，不改成每个 ID 远程获取；不删除应用、授权及审计数据；不改造其他直接取号接口；不部署、不推送、不修改已有 IAM 工作区改动。

## Design
- 调用链：CachedIdGenerator → HttpSegmentIdServiceImpl → TinyIdFeignClient → Gateway → TinyID Server。
- TinyIdFeignClient 使用 name=macula-cloud-tinyid、contextId=tinyIdFeignClient、url=${macula.cloud.endpoint}/tinyid；专属配置复用 KongApiInterceptor，避免拦截器影响其他 Feign 客户端。
- 继续 POST /api/v1/id/nextSegmentIdSimple，唯一业务参数为必填、非空 bizType；保持五字段文本响应及 SegmentId 转换，不新增远程 DTO。
- TinyID starter 引入现有 Feign starter。远程客户端的注册与服务 Bean 放在条件配置中，仅在客户端启用且不存在自定义 SegmentIdService 时装配；检查 Server 自定义 Bean 的装配顺序，避免提前创建 Feign 客户端或强制解析 endpoint。
- 按用户修订移除 TinyID 专用启用开关及 TinyIdProperties，仅根据自定义 SegmentIdService 退让；保留可覆盖的 IdGeneratorFactory。
- 删除专用 server/token 及 connect-timeout/read-timeout 配置；不为 TinyID 单独设置超时，直接沿用项目统一的 Feign 配置或默认值，不保留原 5000 ms 默认值。删除无其他调用方的 TinyIdHttpUtils。
- 网关补充该号段接口的精确路由，使用 StripPrefix=1 移除 /tinyid，Server 去掉 servlet 上下文前缀，与 system 一致。Server 将该接口移出匿名白名单，校验网关签发 JWT；其余旧接口仍保留原 token 校验。
- Feign 失败不得回退生成号段、伪造 ID 或打印凭据；不引入额外自动重试。现有缓存生成器的失败和重试语义保持，并补充回归验证。

## Data and interfaces
- 号段接口保留路径及 currentId,loadingId,maxId,delta,remainder 文本契约，去除 token 参数与该接口的 canVisit(bizType, token) 校验。
- 新链路采用网关应用身份与 URL 访问策略，不再执行 TinyID 应用—bizType 绑定校验；任何可访问该接口的应用可申请已存在的 bizType。该权限语义变化已获用户明确批准。
- 不迁移数据库，不删除授权表；其他旧接口仍使用这些数据。管理文档必须说明授权记录不约束新号段链路。
- 配置迁移：macula.cloud.tinyid.server/token → macula.cloud.endpoint/app-key/secret-key；删除专用 connect-timeout/read-timeout，不新增 TinyID 专属 Feign 超时配置。
- 升级采取 Server 与客户端协调切换；该号段接口不保留 token-only 兼容模式。其他旧 API 的 token 契约不变，但直接访问 Server 的 URL 同步去掉 /tinyid。回滚需协调恢复 Server、客户端及配置版本。

## Flagged concerns
- 权限粒度变化：网关 URL 授权不等同于应用—bizType 授权。用户已明确接受上述新链路权限语义，non-blocking（原 blocking 决策已解决）。
- 旧客户端兼容：现有 token-only 号段客户端在切换后不能使用该接口。用户已明确接受协调升级、不保留该接口 token-only 兼容模式，non-blocking（原 blocking 决策已解决）。
- Server 认证边界：当前 /api/v1/id/** 匿名白名单必须收窄；实施时必须验证网关 JWT 可用、匿名与伪造 JWT 被拒绝，不能仅删除 token 校验，blocking（实施验收条件）。
- 工作区隔离：Cloud 已有 IAM 和管理端测试改动；实施前新建 Cloud 任务分支或独立 worktree，保留原改动，non-blocking。
- 文档元信息：intent 署名待补充，不影响技术范围，non-blocking。
- 项目规则提到 Boot 3，但仓库基线是 Boot 4；沿用条件装配原则，以 Java 17 / Boot 4 实际依赖为准，non-blocking。

## Verification strategy
- 条件装配测试：默认装配、自定义 SegmentIdService/IdGeneratorFactory、Server 无远程凭据可启动。
- 通信契约测试：POST、路径、bizType 编码、无 TinyID token、HMAC 头、五字段解析、空响应/非法响应/远程错误；真实 HTTP 模拟测试归入 IT。
- 发号回归：缓存与预加载、并发唯一性、delta/remainder 行为不变。
- Server/网关测试：有效应用签名、错误签名、过期签名、URL 不允许、匿名、伪造 JWT、未知/空 bizType、下游不可用；保留其他 token 接口的回归覆盖。
- 实施后先运行目标模块及依赖测试，再按跨模块规则扩大验证；真实网关联调需具备 Redis、数据库及应用密钥配置，模拟测试不冒充真实端到端验证。本阶段不执行构建或测试。

## Governing policies
- Macula Boot AGENTS.md、REVIEW.md 及 architecture、starter-development、dependencies-release、testing 规则。
- Macula Cloud AGENTS.md 及 architecture、backend-development 规则；安全契约默认拒绝未确认授权，破坏兼容变更需明确迁移过程。
- sdlc-design 技能要求显式评审后方可接受；未发现额外适用的组织安全或合规技能。bands.yaml 尚为示例，不据此虚构性能 SLO。

## MyBatis-Plus supplement
用户在接受实施计划时追加：TinyID starter 提供 MyBatis-Plus 自定义 ID 生成器，MyBatis-Plus 依赖必须 optional。以下接入细节已随补充计划获用户明确批准。
- 新增 TinyIdIdentifierGenerator，实现 IdentifierGenerator；构造参数为 IdGeneratorFactory，nextId(entity) 通过 MyBatis-Plus TableInfo 元数据取得映射表名及主键列名，按用户指定规则以单个下划线拼接 bizType：表名_主键列名，例如 sys_user_id、sales_order_order_id，再委托现有本地号段生成器。
- 仅提供普通 Java 实现类，不添加组件注解、Bean 定义或 MyBatis-Plus 自动配置；Bean 注册、生成器选用及接入配置全部由使用方负责。使用元数据中的逻辑表名，不随动态物理分表后缀改变；缺少表或主键元数据时明确报错。
- Server 需预先配置对应 bizType；同名映射及下划线拼接结果相同的映射会共享序列。表或主键列改名会改变 bizType，迁移时须处理序列连续性，不能直接重建从低值开始的序列。
- 用于 ASSIGN_ID；不改变 ASSIGN_UUID 行为，不自动创建服务端业务配置，失败不回退到其他 ID 算法。
- 仅依赖 mybatis-plus-core，声明 optional=true，版本统一由 parent 管理；未引入 MyBatis-Plus 的应用仍可正常使用 TinyID。
- 验证实现类的元数据解析和发号委托，以及缺少 MyBatis-Plus 依赖时 TinyID 正常装配；不修改 MyBatis-Plus starter，不承担使用方 Bean 注册及全局生成器配置集成。
