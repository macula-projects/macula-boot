# Spec: 应用与 APIKEY 的多行 URL 和允许 IP 配置 (from intent.md 2026-11-10)
Status: accepted

## Source intent
[intent.md](intent.md) 已由 Rain 明确接受：兼容旧逗号 URL；应用 IP 留空不限；APIKEY IP 非空优先、空则继承应用；支持 IPv4、IPv6、CIDR。

## Requirements
1. 应用及 APIKEY 的 URL 输入、保存、回显均为每行一条；兼容旧英文逗号及逗号与换行混合输入，保留 HTTP方法:/路径 和 Ant 通配符语义。
2. LF、CRLF、CR 换行均可解析，去除规则首尾空白、忽略空行；空配置沿用不限语义。非空但无法形成有效规则的异常缓存不得放开访问。
3. 两处新增“允许 IP”多行输入，接受单个 IPv4、IPv6 或 CIDR，每行一条；服务端拒绝主机名、端口、通配符、错误 IP 和越界前缀，不执行 DNS 查询。
4. 应用 IP 留空不限；APIKEY IP 留空继承应用，非空仅使用自身规则。URL 与 IP 分别确定有效配置，不合并、不取交集。
5. 有效 IP 规则任一匹配即通过 IP 检查；配置非空时，IP 不匹配、来源地址缺失或配置损坏均拒绝，且不能发布可信 principal 或继续下游调用。
6. 保存、清空、缓存更新、启动重建采用相同规则；保持现有租户隔离、权限控制和凭据撤销语义。

## Non-goals
不改变 HMAC 签名算法、APIKEY 传递方式、OAuth 授权模型或普通 OAuth 请求权限；不新增凭据有效期、轮换和可信代理配置能力；不执行部署或业务库迁移。

## Design
- 遵循 Cloud/Boot 的 AGENTS.md、两仓 REVIEW.md、架构规则及 Cloud 后端/前端/测试规则，框架公共匹配能力放 Boot，Cloud 负责管理与缓存编排。Boot 新增或修改 Java 方法遵循方法级 Javadoc 要求。未发现额外适用的组织级品牌或合规规则。
- 管理端应用及 APIKEY 表单增加允许 IP 多行框，URL 示例改为多行；历史 URL 编辑时规范化为多行，列表保持换行可读，说明继承关系。
- Cloud 写入边界校验并规范化，URL 同时识别旧逗号与换行，IP 只接受换行分隔。去除空白行，使用 LF 保存；读取历史 URL 时提供多行值，无需改写历史迁移或全量覆盖历史数据。
- Boot 提供无 DNS 的字面量 IP/CIDR 校验及匹配能力供网关与 Cloud 复用，避免两处接受范围不一致；沿用现有依赖能力，不为本功能新增大型依赖。
- 扩展 TenantUtils 的公共应用准入流程，保留现有公开入口兼容性。先验证租户、URL 和 IP，再由原有 HMAC/APIKEY 入口发布可信身份。URL 使用现有 Ant 匹配及路径时点，保留 StripPrefix 语义；强制 HMAC 的 token 清理端点同样执行完整 IP 准入。
- 默认按网关直连对端 IP 匹配，不自行读取 X-Forwarded-For、Forwarded 等头；部署需避免把不可信转发头转换为准入使用的对端地址。IPv4、IPv6 分别按地址族匹配，IPv4-mapped IPv6 采用统一 IPv4 规范化语义并测试。
- 继续复用事务提交后发布、应用行锁、缓存刷新锁及重读数据库流程。APIKEY URL 和 IP 分别优先自身、回退应用；每次发布显式写入空 IP 字符串，保证清空后不会残留旧限制。

## Data and interfaces
- 应用和 APIKEY 的 JSON 新增 allowIps 字符串，数据库新增 allow_ips VARCHAR(2000) NULL；同步 Form、Entity、BO、VO、Converter 和 Mapper。URL 字段继续使用 accessPath，缓存继续使用 permitUrls。
- 新增 Flyway V24（实施前核对版本占用），不修改 V1/V13 等已存在迁移；将应用 access_path 从 VARCHAR(255) 扩为 VARCHAR(2000)，与 APIKEY 一致。URL 和 IP 输入上限均为 2000 字符，由服务端和管理端共同限制。
- 沿用应用及 /api/v1/app/{appId}/api-keys 的既有接口与权限，不新增公开端点。更新时省略 allowIps 保留原值，显式 null/空字符串表示清空；新增时缺省为空。
- 两类凭据 Redis Hash 增加 permitIps，由 Boot CacheConstants 定义稳定字段常量；缓存不存在此字段时保持不限，兼容历史缓存。
- IAM 不新增 IP 配置及 OAuth 限制；核对相关公共模型调用方，避免把应用凭据限制扩展到普通登录。

## Flagged concerns
- 代理地址边界：默认直连对端匹配；经过反向代理时可能匹配代理 IP。部署负责人须确认网络拓扑及服务器转发头处理；如需按代理后的真实调用者 IP 限制，须另行明确可信代理边界后扩展，non-blocking。
- 新旧版本混用：旧网关不能理解多行 URL，且忽略新增 IP。必须先升级所有相关网关至支持版本，再启用新版 System/Admin 的配置写入并重建缓存；启用后不能直接回退旧网关，non-blocking。
- 缓存同步失败：沿用现有可报告失败与重试/重建机制，数据库提交后 Redis 发布失败仍可能暂时保留旧权限，不能将错误响应当成限制已生效，non-blocking。
- 数据库回退：仅追加可空字段和扩列，不自动删列或缩列，以免丢失配置；业务库升级和运行镜像更新仍需单独授权，non-blocking。

## Verification strategy
- Boot 单元及安全链测试覆盖：多行/旧逗号/混合 URL、不同换行、空白、通配符、URL 拒绝；IPv4/IPv6/CIDR、边界前缀、mapped 地址、地址族不匹配、非法缓存、来源缺失、伪造转发头；HMAC、APIKEY 及强制 HMAC 清理端点允许/拒绝，拒绝后无可信身份和下游调用。
- Cloud 定向测试覆盖：两类表单 IP 校验、2000 字符限制、省略/清空语义、URL/IP 独立继承、应用变更刷新所属 Key、启动重建、清空覆盖旧缓存及租户/权限拒绝。
- 管理端 Vitest 覆盖应用/APIKEY 的旧值多行回显、输入与保存、继承提示、错误反馈和清空；执行构建，关键编辑流程补充 Cypress 验证。
- 隔离 MySQL/Redis 条件具备时验证追加迁移、既有逗号数据保留、缓存发布和真实网关允许/拒绝；未执行的集成及代理部署验证单独列明，不以 Mock 或编译通过代替。
- 同步 Cloud System/Admin、Boot Gateway 的 README 与升级顺序说明；实现后执行相关 Maven 测试、前端测试/构建及变更 Java 的 Checkstyle。

用户于 2026-11-10 回复 YES，接受本设计范围及全部 Flagged concerns。
