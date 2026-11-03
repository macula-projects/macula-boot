# Plan: 网关应用接入租户隔离 (from spec.md 2026-11-03)
Status: accepted.

## Files that change
以下路径相对于各仓库根目录；不修改其他在途 SDLC 需求。

Macula Boot：
- macula-boot-commons/src/main/java/dev/macula/boot/constants/CacheConstants.java
- macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/README.md
- 同模块 src/main/java/dev/macula/boot/starter/cloud/gateway/constants/GatewayConstants.java
- 同模块 src/main/java/dev/macula/boot/starter/cloud/gateway/utils/KongApiUtils.java
- 同模块 src/main/java/dev/macula/boot/starter/cloud/gateway/filter/KongApiGlobalFilter.java
- 同模块 src/main/java/dev/macula/boot/starter/cloud/gateway/filter/RmOpaqueTokenEndpointFilter.java
- 同模块 src/main/java/dev/macula/boot/starter/cloud/gateway/filter/AddJwtGlobalFilter.java
- 同模块 src/main/java/dev/macula/boot/starter/cloud/gateway/security/ResourceServerConfiguration.java
- 同模块 src/test/java/dev/macula/boot/starter/cloud/gateway/utils/KongApiUtilsTest.java（新增）
- 同模块 src/test/java/dev/macula/boot/starter/cloud/gateway/filter/AddJwtGlobalFilterTest.java（新增）
- 同模块 src/test/java/dev/macula/boot/starter/cloud/gateway/filter/KongApiGlobalFilterTest.java（新增）
- 同模块 src/test/java/dev/macula/boot/starter/cloud/gateway/filter/RmOpaqueTokenEndpointFilterTest.java（新增）

Macula Cloud：
- macula-cloud-gateway/src/test/java/dev/macula/cloud/gateway/TinyIdGatewayIT.java（既有集成测试夹具适配）
- macula-cloud-system/src/main/java/dev/macula/cloud/system/service/SysTenantService.java
- macula-cloud-system/src/main/java/dev/macula/cloud/system/service/impl/SysTenantServiceImpl.java
- macula-cloud-system/src/main/java/dev/macula/cloud/system/listener/ApplicationListener.java
- macula-cloud-system/src/test/java/dev/macula/cloud/system/service/impl/SysTenantServiceImplTest.java（新增）
- macula-cloud-system/README.md
- macula-cloud-system/src/main/resources/db/migration/V12__application_auth_tenant.sql（新增；实施前重查版本占用）
- macula-cloud-system/src/main/java/dev/macula/cloud/system/pojo/entity/SysApplication.java
- macula-cloud-system/src/main/java/dev/macula/cloud/system/pojo/bo/ApplicationBO.java
- macula-cloud-system/src/main/java/dev/macula/cloud/system/service/impl/SysApplicationServiceImpl.java
- macula-cloud-system/src/main/java/dev/macula/cloud/system/converter/ApplicationConverter.java
- macula-cloud-system/src/main/resources/mapper/SysApplicationMapper.xml
- macula-cloud-api/macula-cloud-system-api/src/main/java/dev/macula/cloud/system/form/ApplicationForm.java
- macula-cloud-api/macula-cloud-system-api/src/main/java/dev/macula/cloud/system/vo/app/ApplicationVO.java
- macula-cloud-system/src/test/java/dev/macula/cloud/system/service/impl/SysApplicationServiceImplTest.java（新增）
- macula-cloud-system/src/test/java/dev/macula/cloud/system/form/ApplicationFormTest.java（新增）
- macula-cloud-admin/src/views/system/application/save.vue
- macula-cloud-admin/tests/unit/application-auth-tenant.test.js（新增，沿用现有测试目录）

Macula Samples：
- /Users/Rain/Documents/workspace/macula/macula-samples/macula-samples-gateway/src/main/resources/application.yml（移除本次新增配置，最终沿用原 application.name）

过程文件：本目录 intent.md、spec.md、plan.md、review-findings.md；必要时在 Cloud .sdlc/gateway-application-tenant-isolation/ 写入指向本规范与计划的跨仓库契约文件，状态仅依据本次会话人工批准更新。

## Order of work
1. 保留工作区改动；复核目标模块 POM、README、测试及 Cloud 前后端规则。使用现有 Java 17、Spring、MapStruct、MyBatis-Plus、JUnit/Mockito、Vitest，不新增依赖。
2. Boot 增加缓存字段/请求属性常量及按应用名最长前缀解析网关归属的统一方法；无命中或非法 ID 返回 -1。为验签增加接收网关应用名的重载；旧签名保留无接入限制的调用语义；按 Rain 后续要求，所有入口均仅使用请求属性，不读写 ThreadLocal。
3. 新验签入口先移除同名旧认证属性，校验应用归属、有效认证租户及现有签名/路径规则；只在全部通过后发布每请求独立的可信认证属性。租户不匹配或非空非法租户拒绝，不解析客户端租户头作为网关配置。
4. 两个内置 HMAC 调用点均把 spring.application.name 传给 checkSign，由 KongApiUtils 内部解析并校验归属；AddJwtGlobalFilter 从可信属性读取认证租户，缺少验签成功标记则拒绝 HMAC 转 JWT。保留原上游调用名称行为；统一沿用原 buildKey，使用 principal 中的最终认证租户；不增加 HMAC 专用命名空间或缓存键编码。
5. Cloud 新增可空 auth_tenant_id 迁移及模型字段，补齐 Mapper 查询、MapStruct 转换；Form 通过显式 setter 与非序列化的存在标记区分字段缺省和显式 null。旧请求缺省保留原值，显式 null 才清空；MyBatis 更新显式覆盖该字段，避免默认忽略 null。
6. Service 使用现有 SecurityUtils.isRoot() 校验跨租户设置，仅 ROOT 可显式设置不同于归属租户的认证租户；普通管理者可清空或设为归属租户，不得通过直接 API 绕过。更新先在当前租户权限范围读取原记录；缺省字段不改变原认证租户配置。认证租户非空时校验目标租户存在；无权确认则拒绝。
7. 租户管理在启动及新增/修改/删除事务提交后重建租户编码→ID Hash；应用缓存只保留归属 tenantId，写入 authTenantId（NULL 时写归属租户）；用 try/finally 恢复刷新与编码校验过程中切换的原租户上下文，原值为空时清除。沿用现有刷新触发机制。
8. 管理端增加认证租户下拉选择、回显、清空和回退说明；作为字符串传递 Long ID，避免 JavaScript 精度损失。后台权限校验为最终依据。更新两处模块 README，说明 API Key 不在范围及滚动升级顺序。
9. 移除指定 samples 网关本轮新增的 tenant-id，沿用原 spring.application.name；不改其他样例和部署，不发布到远端。
10. 编写下面 Proof 的针对性测试；Build 完成后报告实际 diff 与偏差，按技能要求交给用户确认，再进入 sdlc-test 执行验证。

## Risks
- 必须先部署数据库新增列再部署新版 System；旧记录保留 NULL。只交付迁移脚本，不执行真实升级。
- 旧网关忽略 authTenantId，须先升级网关，再配置跨租户认证值；回滚旧网关前先恢复认证租户与归属一致。
- 缓存刷新为现有机制，新字段在刷新后生效；本次不承诺多实例配置变更即时一致或修复所有旧缓存失效问题。
- 内置 AK/SK 入口在租户缓存缺失时默认拒绝。启动及租户事务提交后自动刷新映射；旧三参数验签签名及 API Key 不提供新增隔离保证。
- 不改变 forceHmacRmOpaqueTokenEndpoint=false 的既有显式关闭行为；开启 HMAC 时清理入口必须遵循租户限制。
- 所有 HMAC 入口均不读写 ThreadLocal；依赖旧签名设置上下文的自定义调用方需改为读取请求属性。
- 没有提交/推送/部署授权；规范接受仅更新本地文档，不执行技能中提及的自动提交。

## Proof
- 不再修改 GatewayProperties 及其测试；两入口测试覆盖服务端应用名注入及缓存解析行为。
- KongApiUtilsTest：有效签名下归属相同通过、不同/缺失拒绝；最长前缀解析、空/未命中/非法映射返回 -1 并拒绝；authTenantId 优先、缺省回退、非法拒绝；验签/路径失败不发布可信属性，旧成功属性不可复用；并发/交错请求租户独立；旧签名兼容行为。
- KongApiGlobalFilterTest / RmOpaqueTokenEndpointFilterTest：内置入口传递租户策略；失败不调用下游或删除 token；显式关闭 HMAC 保持旧行为。
- AddJwtGlobalFilterTest：捕获 JwtEncoder claims 验证认证租户，缺少可信结果拒绝；认证租户变化时沿用原规则生成不同键，OAuth/API Key 保持原键与行为。
- SysApplicationServiceImplTest：字段新增/编辑/清空、缺省保留、ROOT/普通管理者权限、目标租户不存在、缓存两租户分离及失败恢复上下文。
- ApplicationFormTest：用项目实际 JSON 栈验证字段缺省、null、Long 值与存在标记序列化边界；Converter 编译验证字段流转。
- application-auth-tenant.test.js：表单回显、显式清空提交与长整型字符串保持。
- 测试阶段先运行 Boot Gateway 和 Cloud System 定向单测，再按风险扩大到相关模块；前端运行定向 Vitest、构建。检查 SQL 增量、samples YAML 和 git diff --check。
- 本地 mock/构建与真实数据库升级、真实 Redis、多网关端到端调用分开报告；不把前者作为后者证据。

## Implementation notes
- Rain 于本次会话明确回复 YES 接受实施计划，状态更新为 accepted；源码修改在此之后进行。
- 在既定 SysApplication 文件中增加只读 tenantId 映射，以持久化记录的归属进行更新授权；插入、更新均不接受表单覆盖，由原租户拦截器维护。
- 按 Rain 后续要求，移除 KongApiGlobalFilter 手写兼容构造器，统一使用 @RequiredArgsConstructor 生成的双参数构造器；最终构造器只注入 RedisTemplate；应用名通过 @Value 注入，无手写构造器。
- 清理旧示例凭据注释；缓存空 accessPath 写为空字符串，语义仍为原来的默认允许路径，避免 Redis Hash 写 null。
- 无超出批准文件清单的源码修改；跨仓库过程文件在 Cloud 同步保存。

## Build checks
- Boot Gateway 及依赖：test-compile -DskipTests 成功；已本地 install -DskipTests=true -Dgpg.skip=true -Pdeploy，供跨仓库编译引用。未上传远端。
- Cloud System/API：test-compile -DskipTests -Plocal 成功，包含新增测试代码编译。
- Admin：npm run build 成功；保留现有大分包提示。
- Boot/Cloud 定向 Checkstyle、三仓 git diff --check 通过；samples YAML 经现有 js-yaml 解析通过（Maven profile 占位符按 local 替换），确认网关环境变量覆盖表达式。
- 以上不代表单元测试或真实数据库/Redis/端到端验证通过。测试代码尚未执行，等待 Build 人工完成确认后进入 sdlc-test。

## User-requested adjustment
- Rain 确认使用 exchange 属性后无需再把租户写入 TenantContextHolder；移除旧三参数验签入口的 ThreadLocal 写入，保留方法签名。测试覆盖原上下文为空或非空时均保持不变，更新兼容说明。

## Application-name tenant resolution adjustment
- Rain 已确认以网关 spring.application.name 对租户编码作 startsWith 匹配；多个命中取最长前缀，未匹配为 -1；随后明确回复“好”接受由 System 应用缓存刷新流程维护映射。
- 撤销本轮新增的 GatewayProperties.tenantId 与 samples tenant-id，实际 GatewayProperties 与对应原测试恢复为原始状态。
- 此处原定映射随应用刷新维护，已按后续 Tenant-cache ownership correction 改为租户管理负责启动和 CRUD 提交后刷新；不新增后台任务。
- 本轮修改后重新编译和检查；此前 Build checks 为历史构建记录，新增前缀解析测试仍等待 Test 阶段执行。

## Prefix-resolution build checks
- Boot Gateway 及依赖在更新后 test-compile -DskipTests 通过；本地 install -DskipTests=true -Dgpg.skip=true -Pdeploy 通过，供 Cloud 引用新增缓存常量。
- Cloud System/API 在更新后 test-compile -DskipTests -Plocal 通过。
- 两仓本次 Java 文件的定向 Checkstyle 及三仓 diff --check 通过。
- samples application.yml 已移除本次增加的手工配置，与原文件一致；GatewayProperties 及其既有测试也恢复为原实现。
- 已增加最长前缀、无映射/非法映射拒绝、清理入口未知归属拒绝、缓存重建和空列表清除/重复编码拒绝用例。仅编译测试代码，尚未执行单元测试或外部服务验证。

## Cache-key scope correction
- 按 Rain 明确要求撤销 buildHmacKey、keyPart 和 generateJwtToken 的缓存键重载，AK/SK 恢复使用原 buildKey。原方法实现保持不变，测试改为断言原键格式。应用身份间的独立缓存隔离不属于本次租户改动。

## Verification-entry simplification
- 按 Rain 指示，两个过滤器仅将 applicationName 传入 checkSign；工具类内部调用 resolveGatewayTenantId。数字租户参数的校验方法收为私有实现，原三参数兼容入口保留。同步调整验签测试及 README。

## Case-insensitive tenant-code matching
- 按 Rain 指示，网关应用名与租户编码前缀匹配改为不区分大小写，继续使用最长前缀。同步调整混合大小写测试、README 与规范，其他解析行为不变。

## Auth-tenant selector adjustment
- 按 Rain 指示，认证租户改为可搜索、可清空的 el-select，复用现有租户选项接口并请求 filterMe=1；选项值统一保留为字符串。
- 打开新增/编辑弹窗时加载选项，提供加载状态及错误提示；清空产生的空字符串、null 或 undefined 均显式提交 null，保留原回退语义。服务端 ROOT 跨租户校验不变。

- 本次下拉调整验证：Admin npm run build 通过；npm run test:unit -- --run tests/unit/application-auth-tenant.test.js 通过，1 个文件、7 个测试，0 失败。仅为该 UI 改动的本地验证，不代表后端/真实租户接口或整体 SDLC Test 门禁完成。

## Tenant-cache ownership correction
- Rain 指出租户变更不应依赖应用变更；据此将租户编码映射缓存维护归属 SysTenantService。
- 补充文件范围：macula-cloud-system/src/main/java/dev/macula/cloud/system/service/SysTenantService.java、service/impl/SysTenantServiceImpl.java、listener/ApplicationListener.java，以及 src/test/java/dev/macula/cloud/system/service/impl/SysTenantServiceImplTest.java（新增）。均相对于 System 的 dev/macula/cloud/system 包。
- System 启动时调用租户缓存初始化；租户新增、修改、删除成功写入后注册事务提交回调，提交后重建映射，回滚不发布。无事务调用时直接刷新。
- 从应用刷新移除租户映射读写；原应用测试只校验应用缓存。新增租户测试覆盖启动初始化、创建/改编码/删除后提交刷新、回滚不刷新、空列表清除旧映射、大小写重复编码拒绝。同步修订说明，不新增任务或后台服务。

## Tenant-cache lifecycle verification
- 运行 mvn -o -pl macula-cloud-api,macula-cloud-api/macula-cloud-system-api,macula-cloud-system -am test -Plocal -Dtest=SysTenantServiceImplTest,SysApplicationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false：15 项通过，0 失败/错误/跳过（租户 8 项、应用 7 项）。
- 初次运行发现删除用例缺少 MyBatis-Plus 的测试元数据，补齐 SysTenantInfo/SysTenantUser 元数据后重跑通过；无实际数据库或 Redis 参与。
- 相关文件定向 Checkstyle 和两仓 diff --check 通过。启动初始化为监听器单元测试，提交/回滚为同步回调测试，未宣称真实数据库事务或服务启动集成通过。

## Test-stage fixture correction
- Rain 已回复“OK，代码确认”，进入 sdlc-test。全量 Cloud verify 暴露 TinyIdGatewayIT 的手工构造过滤器缺少 applicationName 和租户映射，成功用例被新规则拒绝。仅适配测试夹具并加强租户拒绝与 JWT 认证租户断言，不放宽生产规则。补充该测试文件到范围，随后重跑相关验证。

## Test-stage evidence
- Rain 已确认代码，执行完整两仓 test/verify 与 Admin 全量单测和构建。默认启用套件通过，外部环境专项合计 13 项按仓库既有条件跳过，未自批完整环境测试门禁；详情见 verification.md。
- Cloud 既有 TinyIdGatewayIT 夹具已适配并重跑全量 verify 成功；生产逻辑未因测试失败修改。

## Review corrections
- 第 1 轮独立审查及 fresh 复核确认 1 项 Important、1 项 Minor。使用既有 RedissonClient/RLock 保护租户映射从读库到发布的全流程，锁由所有 System 实例共享；无新增依赖或服务。
- 编辑界面记录初始认证租户，值未改动时省略字段；不放宽后台 ROOT 校验。
- 修改范围仍为原 SysTenantServiceImpl、SysTenantServiceImplTest、save.vue、application-auth-tenant.test.js，新增并发重现、异常释放锁及编辑保留/清空测试；修复后重跑全量验证，随后仅复审本轮变化。

## Local review completion
- 第 2 轮补丁复审与 fresh verify 均完成：原 Important/Minor 各 1 项全部关闭，无新增发现；证据见 review-findings.md。
- 中断恢复后来源文件快照一致，保留已有测试证据；未提交或执行远端操作。下一门禁为 Rain 对两项审查修复的人工确认。

## Local commit authorization
- Rain 明确要求“提交”，在当前两仓 main 分支本地提交已批准范围；不执行推送。提交前来源文件快照仍匹配已验证版本。
