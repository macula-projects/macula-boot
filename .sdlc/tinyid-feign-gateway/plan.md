# Plan: TinyID 号段客户端统一网关接入 (from spec.md 2026-10-09)

最新已接受修订：四个发号接口均移除 TinyID token 参数及校验，只保留 bizType 和适用的 batchSize；Server 删除全部发号匿名白名单，网关精确路由四个接口并沿用共享认证，不新增 HMAC/个人 Token 区分。管理端历史 Token 数据及功能保留。本条覆盖下文旧 token 兼容说明，实施依据为 Boot 仓 plan-remove-issuing-token.md。
本轮实现及最终复验已完成；最新测试结果以 Boot 仓 verification.md 的“2026-10-09 最新修订复验”节为准，下文早期状态仅作历史记录。
Status: accepted.

最新用户决定：撤销本次新增的 TinyID 专属 HMAC/个人 Token 区分，删除 TinyIdHmacGlobalFilter 及其专属拒绝测试，沿用共享网关认证授权逻辑；身份边界留待用户统一处理。此决定覆盖下文历史 HMAC-only 约束，不等同于该安全边界已修复；现有 HMAC 验签、URL 授权、JWT 校验及路由前缀处理保留。

Server 异常链路补充已获用户 YES 批准，详见 [plan-server-exceptions.md](plan-server-exceptions.md)，覆盖先前保留新接口吞异常行为的约束。

评审修复与 BizException 补充计划已获用户明确批准，见 [plan-review-fixes.md](plan-review-fixes.md)。

## Files that change
原计划及 MyBatis-Plus 补充细节均已由用户明确批准。以下路径均相对于各自仓库；不修改 system starter、IAM、管理端或数据库。

Macula Boot：以 macula-boot-starters/macula-boot-starter-tinyid/ 为模块根目录。
- 修改 pom.xml、README.md。
- 修改 src/main/java/dev/macula/boot/starter/tinyid/ 下的 service/impl/HttpSegmentIdServiceImpl.java、config/TinyIdAutoConfiguration.java；删除已无配置项的 config/TinyIdProperties.java。
- 新增上述 Java 根目录下 remote/TinyIdFeignClient.java、config/TinyIdFeignClientConfiguration.java；条件装配优先放在现有自动配置的内部配置类中，不新增自动配置入口。
- 删除上述 Java 根目录下 utils/TinyIdHttpUtils.java，删除前确认没有其他调用方。
- 修改 src/test/resources/application.yml、src/test/java/dev/macula/boot/starter/tinyid/config/TinyIdAutoConfigurationTest.java、同测试根目录下 TinyIdClientIT.java。
- 新增同测试根目录下 service/impl/HttpSegmentIdServiceImplTest.java、remote/TinyIdFeignClientIT.java、base/generator/impl/CachedIdGeneratorTest.java。
- 维护 .sdlc/tinyid-feign-gateway/plan.md，记录实际偏差；核对 AutoConfiguration.imports，无入口变化则不改。
- MyBatis-Plus 补充：修改 macula-boot-parent/pom.xml，统一管理 mybatis-plus-core 版本；TinyID 模块 pom.xml 将其声明为 optional。新增模块 Java 根目录下 mybatisplus/TinyIdIdentifierGenerator.java，以及对应测试根目录下 mybatisplus/TinyIdIdentifierGeneratorTest.java；更新 README 使用说明及现有条件装配测试的依赖缺失场景。不新增生成器集成测试或自动配置。

Macula Cloud：
- 新增 .sdlc/tinyid-feign-gateway/ 下 intent.md、spec.md、plan.md，复制已接受契约并注明 Boot 为主记录，确保跨仓实施有对应依据。
- 修改 macula-cloud-tinyid/src/main/java/dev/macula/cloud/tinyid/controller/IdContronller.java。
- 修改 macula-cloud-tinyid/src/main/resources/application.yml、README.md。
- 修改 macula-cloud-tinyid/src/test/java/dev/macula/cloud/tinyid/controller/IdContronllerTest.java；新增同模块测试根目录下 security/TinyIdSegmentSecurityIT.java。
- 修改 macula-cloud-gateway/src/main/resources/application.yml、README.md；新增该模块 src/test/java/dev/macula/cloud/gateway/TinyIdGatewayIT.java。
- 仅在现有测试依赖不足时修改上述两个 Cloud 模块的 pom.xml，不引入新的测试框架；实际新增依赖记入本计划。

## Order of work
1. 确认本计划获批后，读取两仓 CLAUDE.md、目标模块 POM/README/相邻实现与测试。Boot 继续使用 feat/tinyid-feign-gateway；Cloud 从核实后的当前 HEAD 建独立 worktree 与同名任务分支，保留原工作区所有改动。记录基线提交，并同步接受的 SDLC 契约。
2. 实现 TinyID 专属 Feign 接口和 HMAC 配置，复用现有 Feign starter 与 KongApiInterceptor。HttpSegmentIdServiceImpl 改为委托 Feign，保留五字段号段转换，移除手写 HTTP 工具。
3. 调整条件装配和配置：按用户要求不提供专用启用开关；自定义 SegmentIdService 存在时不注册远程客户端、不要求网关凭据；IdGeneratorFactory 仍可覆盖。移除 server/token/专用超时属性，不设置 TinyID 专属 Feign 超时或重试策略。
4. Server 将 nextSegmentIdSimple 改为仅接受 POST 和必填非空 bizType，移除该方法 token 校验；匿名白名单缩为其他旧接口的精确路径。网关补充该号段接口路由，配置 StripPrefix=1，Server 移除 /tinyid 上下文；沿用现有 HMAC → JWT 链路及 Server JWT 校验。
5. 补齐通信、条件装配、缓存发号和安全边界测试。同步 README 与测试配置，清除旧示例凭据，注明权限变化、协调升级及回滚方法；不改变其他旧接口与管理数据。
   同时实现用户追加的普通 Java 类 TinyIdIdentifierGenerator(factory)，不提供组件注解、Bean 定义或 MyBatis-Plus 自动配置，注册和接入由使用方负责。nextId(entity) 从 TableInfo 获取映射的逻辑表名与主键列名，以单个下划线拼接 bizType（例如 sys_user_id），再委托 factory.getIdGenerator(bizType).nextId()；校验构造参数及元数据，保留失败传播，不自动创建 bizType。README 说明构造方式、ASSIGN_ID 用途及使用方注册责任。
6. 静态核对文件范围与契约，记录偏差和未验证项，展示实现结果供确认；确认完成后交由 sdlc-test 执行正式验证。本阶段不自行发布、推送、部署或跨过测试门禁。

## Risks
- 已接受不兼容变更：旧 token-only 号段客户端需协调升级；原应用—bizType 授权不再约束新接口，其他旧接口仍受原授权约束。
- 必须真实验证 Server 安全过滤链；不能只通过模拟登录或 Controller 单测证明 JWT 校验有效。
- Feign 子上下文、全局拦截器与 Bean 注册顺序可能影响后台预加载、Server 启动及 HMAC 签名，必须分别覆盖。
- 超时沿用统一配置或默认值，不承诺保留旧 5000 ms 行为；超时失败不得产生伪造号段。
- Cloud 独立 worktree 不包含原工作区未提交的 IAM 变化；测试结果须注明此边界，不复制或提交这些改动。
- 回滚需同步恢复客户端、Server、路由和配置；没有数据库迁移。真实联调依赖缺失应报告阻塞，不能当作通过。
- MyBatis-Plus 可选类型不能进入 TinyID 必需装配路径。全局生成器选用及 Bean 注册由使用方负责，本次不修改 MP starter 或集成其默认生成器配置。
- 表名和主键列名以下划线拼接并非可逆编码，拼接结果相同的映射共享序列；同名表跨库也共享序列。改名会改变 bizType，需说明预建业务配置及迁移时避免 ID 重复的要求。

## Implementation notes
- 2026-10-09 最终复验：用户 YES 确认进入测试；修正 Admin 单测的 Element Plus 局部 mock 与 exposed API 调用，未改生产逻辑。两仓全量、前端单测/构建/Cypress、隔离真实发号完成；命令、跳过项和失败修正见 verification.md 最新节。
- 真实联调补充：TinyIdClientIT 改为 TINYID_E2E=true 时启用，校验单个与批量共 251 个 ID 唯一性；本地 Feign IT 通过临时系统属性隔离外部测试凭据。启动独立 MySQL/Redis 容器及 19000/19082 端口的本次应用 JAR，关闭 Nacos 注册并以静态路由指向隔离 Server，不更改已有部署。完整证据见 verification.md。
- 后续验证已执行，实际结果及失败修正见 verification.md；下述“本轮未执行”描述的是 Build 阶段历史状态。
- 测试阶段修正：默认 Feign 装配实测缺少 Boot 4 ClientHttpMessageConvertersCustomizer；在 TinyID POM 增加 spring-boot-http-converter 运行依赖，未扩大到修改共享 Feign starter。
- Cloud 验证前计划执行 mvn -B -ntp -pl macula-boot-starters/macula-boot-starter-tinyid -am install -Pdeploy -DskipTests=true -Dgpg.skip=true，仅更新本地 Maven 仓库，使用 deploy profile 生成可解析的 revision POM，不发布远端。测试证据独立来自 verify。
- Boot 实施基线为 23d05db（已接受计划提交）；Cloud 基线为 d32fc5f692a12e798050cac1ffa8e38405f31d1d，独立 worktree 为 /Users/Rain/Documents/workspace/macula/macula-cloud-tinyid-feign，分支 feat/tinyid-feign-gateway。Cloud 无 CLAUDE.md，已读取其 AGENTS.md 及适用规则。
- 用户修订：TinyID Server 去掉 /tinyid 上下文，网关增加 StripPrefix=1（含管理路由）。StripPrefix 在 order=200 的 HMAC 验签前执行，签名和 URL 授权使用 /api/v1/...；TinyID 配置直接返回 KongApiInterceptor，删除模板副本包装，不修改共享拦截器。客户端 IT 验证剥离前缀后的签名，Gateway IT 显式执行 StripPrefix 后验签。
- 验证边界：新增 Gateway IT 使用生产路径谓词与 HMAC/JWT 过滤器、真实测试签名密钥，隔离 Redis 和下游；不是完整网络代理或真实端到端联调。Server IT 导入生产 SecurityFilterChain，使用本地签名 JWT 与模拟号段服务。真实联调仍留在测试阶段。
- AutoConfiguration.imports 无需变更；没有新增自动配置入口。没有修改生成算法、MyBatis-Plus starter、IAM、管理端或数据库。删除的 TinyIdHttpUtils 仅被替换后的客户端使用，可由 Git 恢复。
- 本轮只完成实现与静态检查，未执行 Maven 编译/测试或本地安装；测试结果和新增用例的可运行性尚待 sdlc-test 验证。

## Proof
- TinyIdAutoConfigurationTest：默认装配、自定义 SegmentIdService/IdGeneratorFactory、无 endpoint/密钥的 Server 替换场景；TinyIdClientIT 覆盖实际自动配置发现。
- HttpSegmentIdServiceImplTest：五字段转换、空/非法响应、远程异常；确认不添加额外重试或失败回退。
- TinyIdFeignClientIT：本地 HTTP 测试服务核对 POST、完整路径、bizType 编码、无专用 token、HMAC 头及错误响应，验证后台调用不依赖用户请求上下文。
- CachedIdGeneratorTest：现有缓存、预加载、并发唯一性和 delta/remainder 行为回归，生产算法代码不变。
- IdContronllerTest：新接口正常/空/未知 bizType，以及其他三个旧接口 token 校验不变。
- TinyIdSegmentSecurityIT：使用测试签名密钥与实际安全过滤链，覆盖有效 JWT、匿名、伪造和过期 JWT，证明受保护接口不会被旧白名单放行。
- TinyIdGatewayIT：实际网关路由/过滤器测试，覆盖 HMAC 成功/错误/过期、URL 拒绝、验签前剥离前缀及下游路径和下游不可用；涉及网络或完整上下文的测试使用 IT 命名。模拟 Redis/下游证据与真实端到端联调分开报告。
- 测试阶段先执行 Boot TinyID 目标模块及依赖测试、相关 IT，再按仓库跨模块规则扩大；Cloud 显式使用 -Plocal 对 TinyID/Gateway 验证，确保解析的是本次 Boot 产物。需要安装 Boot 本地产物时先记录准确命令及影响。
- 真实 Gateway → Server 联调仅在数据库、Redis、服务发现、应用密钥和 JWT 配置齐备后执行；否则明确未验证范围。检查源码 diff 确认无 IAM、管理端、数据库或生成算法变更。
- MyBatis-Plus 补充验证：单测覆盖表名_主键列名映射（含 @TableName/@TableId 自定义名称）、元数据缺失、Long 返回值、非法构造参数及异常传播；依赖缺失时 TinyID 正常装配。核对实现类没有组件注解或注册入口，核对 optional 依赖声明及发布 POM 的版本解析；不测试使用方 Bean 注册和全局生成器集成。父 dependency management 变化按仓库规则纳入正式 clean verify 范围。
