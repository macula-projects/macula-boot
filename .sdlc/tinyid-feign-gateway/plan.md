# Plan: TinyID 号段客户端统一网关接入 (from spec.md 2026-10-09)
Status: accepted.

## Files that change
原计划及 MyBatis-Plus 补充细节均已由用户明确批准。以下路径均相对于各自仓库；不修改 system starter、IAM、管理端或数据库。

Macula Boot：以 macula-boot-starters/macula-boot-starter-tinyid/ 为模块根目录。
- 修改 pom.xml、README.md。
- 修改 src/main/java/dev/macula/boot/starter/tinyid/ 下的 service/impl/HttpSegmentIdServiceImpl.java、config/TinyIdAutoConfiguration.java、config/TinyIdProperties.java。
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
3. 调整条件装配和配置：enabled 默认 true；自定义 SegmentIdService 存在时不注册远程客户端、不要求网关凭据；IdGeneratorFactory 仍可覆盖。移除 server/token/专用超时属性，不设置 TinyID 专属 Feign 超时或重试策略。
4. Server 将 nextSegmentIdSimple 改为仅接受 POST 和必填非空 bizType，移除该方法 token 校验；匿名白名单缩为其他旧接口的精确路径。网关补充该号段接口路由，不剥离 /tinyid；沿用现有 HMAC → JWT 链路及 Server JWT 校验。
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

## Proof
- TinyIdAutoConfigurationTest：启用、禁用、自定义 SegmentIdService/IdGeneratorFactory、无 endpoint/密钥的 Server 替换场景；TinyIdClientIT 覆盖实际自动配置发现。
- HttpSegmentIdServiceImplTest：五字段转换、空/非法响应、远程异常；确认不添加额外重试或失败回退。
- TinyIdFeignClientIT：本地 HTTP 测试服务核对 POST、完整路径、bizType 编码、无专用 token、HMAC 头及错误响应，验证后台调用不依赖用户请求上下文。
- CachedIdGeneratorTest：现有缓存、预加载、并发唯一性和 delta/remainder 行为回归，生产算法代码不变。
- IdContronllerTest：新接口正常/空/未知 bizType，以及其他三个旧接口 token 校验不变。
- TinyIdSegmentSecurityIT：使用测试签名密钥与实际安全过滤链，覆盖有效 JWT、匿名、伪造和过期 JWT，证明受保护接口不会被旧白名单放行。
- TinyIdGatewayIT：实际网关路由/过滤器测试，覆盖 HMAC 成功/错误/过期、URL 拒绝、下游路径保留和下游不可用；涉及网络或完整上下文的测试使用 IT 命名。模拟 Redis/下游证据与真实端到端联调分开报告。
- 测试阶段先执行 Boot TinyID 目标模块及依赖测试、相关 IT，再按仓库跨模块规则扩大；Cloud 显式使用 -Plocal 对 TinyID/Gateway 验证，确保解析的是本次 Boot 产物。需要安装 Boot 本地产物时先记录准确命令及影响。
- 真实 Gateway → Server 联调仅在数据库、Redis、服务发现、应用密钥和 JWT 配置齐备后执行；否则明确未验证范围。检查源码 diff 确认无 IAM、管理端、数据库或生成算法变更。
- MyBatis-Plus 补充验证：单测覆盖表名_主键列名映射（含 @TableName/@TableId 自定义名称）、元数据缺失、Long 返回值、非法构造参数及异常传播；依赖缺失时 TinyID 正常装配。核对实现类没有组件注解或注册入口，核对 optional 依赖声明及发布 POM 的版本解析；不测试使用方 Bean 注册和全局生成器集成。父 dependency management 变化按仓库规则纳入正式 clean verify 范围。
