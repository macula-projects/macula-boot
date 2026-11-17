# 兼容性与安全复查

## 当前有效状态：原 Handler 内最小修复
- Rain 明确 username 只含大小写英文字母、数字、下划线，SELF 完整校验 `[A-Za-z0-9_]+`；空或非法值产生 1=0，不删除字符后转换身份。合法值使用单引号字面量；原 WHERE 和权限条件分别加括号。
- 只有原 Handler 的上述小范围修改，原生 DataPermissionInterceptor、部门/find_in_set、ALL/DEFAULT 保持。参数上下文、参数化插件和词法框架仍未采用；不要求使用方迁移插件。
- 新增五项回归先在恢复后的原实现失败（4 failures），补丁后通过：合法字符及大小写、SQL 关键字作为合法用户名、引号/空白/反斜线/Unicode/空值拒绝、OR 分组、ALL/DEFAULT 和部门语义保持。
- 目标模块及依赖 `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am test`：43 项，0 failures/errors/skips，日志 `/tmp/macula-minimal-username-main-test.log`。
- 在独立临时 Redis 上执行 `IAM_CALLBACK_REDIS_IT_PORT=6379 mvn -fae verify`：426 项，0 failures/errors，3 项原有 Kafka/RocketMQ/TinyID 条件 skip；退出 0，日志 `/tmp/macula-minimal-username-main-verify.log`。
- 实际输出：`[INFO] BUILD SUCCESS`，`[INFO] Total time: 01:36 min`。变更 Java Checkstyle 零违规，git diff --check 通过；受保护配置未改，无适用 eval。Verification is green（当前最小方案，依赖已确认用户名约定）。

## 历史记录说明
以下恢复、参数化、跨方言及词法试验记录对应旧快照，不覆盖当前最小方案，也不构成仍需迁移插件或恢复参数框架的要求。


## 当前有效状态：SQL 重写已按用户要求撤回
- MyDataPermissionHandler 与任务起始基线完全一致；原生 DataPermissionInterceptor 接入恢复，新增参数上下文、参数化插件及 SQL 专用测试删除。ROOT 租户授权、JWT、Token 缓存、请求体 10 MiB、日志清理等其他修复保留。
- 下文所有 SQL 参数化、H2/MySQL、JOIN/CTE 记录均为已撤销实现的历史证据，不代表当前 SQL 漏洞已修复。PR 保持草稿，SQL 部分等待重新评估，不能按旧 Verification is green 进入合并。
- 原实现本轮重新复现：`SELF=create_by = "alice" OR 1 = 1`；`OR=active = 1 OR id = 2 AND dept_id = 12`；`DEPT_SUB` 仍使用 find_in_set。日志 `/tmp/macula-compat-review/OriginalPermissionReevaluation.log`。前两项是需要单独修复的注入与逻辑分组问题；第三项是原 MySQL 方言约束，不再混入安全修复改造。
- Handler SPI 只返回 Expression，没有 BoundSql/ParameterMapping；先评估保留原结构的最小安全改法，不能再默认扩展为 SQL 参数基础设施。此轮只恢复和评估，不新增替代 SQL 实现。


## PR 前完整验证（当前实现）
- 用户已确认实现并要求全面 review、通过后提交主线和 5.x PR。主线使用独立的临时 Redis 7.4.5 与 MySQL 8.4.6，均只绑定回环端口，没有连接业务容器。
- 实际命令：`MACULA_SECURITY_TEST_MYSQL_URL=<isolated-mysql-jdbc-url> MACULA_SECURITY_TEST_MYSQL_USER=root MACULA_SECURITY_TEST_MYSQL_PASSWORD='' IAM_CALLBACK_REDIS_IT_PORT=6379 mvn -fae verify`；退出 0，日志 `/tmp/macula-security-main-final-verify.log`。
- 实际输出：`[INFO] BUILD SUCCESS`，`[INFO] Total time: 01:25 min`。全部启用的单元/集成测试通过；真实 MySQL 权限测试及真实 Redis 回调 IT 已执行，不再缺少这两项证据。仓库原有条件禁用的 Kafka/RocketMQ/TinyID 外部端到端测试仍按其明确条件跳过，不属于本次新增要求。
- 首次完整验证发现 JPA 测试依赖已移除的默认 Secret；JPA/Auditlog 的集成测试改为显式测试专用 Secret 后全仓重跑通过。没有放宽生产校验，也没有以全局测试变量掩盖 Secret 缺失回归。
- AGENTS.md、CLAUDE.md、REVIEW.md、skills/hooks/reviewer 配置没有变化，无适用配置 eval。正式审查针对当前分支工作区完整快照（含新增源码和测试）。
- Verification is green（主线当前接受范围；5.x 单独记录）。没有自批准 PR、合并或部署 gate。

日期：2026-11-17。Rain 回复 OK 后要求再次检查兼容性和漏洞。本轮只读检查实现，新增本验证记录；没有修改业务源码、提交、推送或访问真实业务数据库。

首轮复查结论（修复前）：**验证未通过**。现有自动化测试通过，但补充复现发现两个新增兼容性回归和一个原有身份混用漏洞。下文保留首次证据；Rain 随后授权统一修复，最新结果见文末“授权修复后的验证”。没有自批准 Test 或后续人工 gate。

## 确认的问题

1. **Important / P1，原有安全问题：JWT 缓存键允许跨应用混用身份。** `KongApiGlobalFilter` 将调用方的 `tokenId` 写入 jti；`AddJwtGlobalFilter.buildKey` 对同租户 jti 与主体名使用相同空间，命中后直接返回缓存 JWT。真实 HMAC 验证链复现：B 的合法请求先缓存 B JWT，持 A 凭据且通过 A 签名验证的请求设置 tokenId=B，未使用 invoker，最终转发的 JWT sub 为 B。前提是同租户目标缓存存在。应将缓存键绑定已验证的应用/主体及认证来源，并验证缓存 claims，不能只依赖调用方选择的 jti。复现：`/tmp/macula-compat-review/RealHmacCacheProbe.java`、同名 `.log`。
2. **Important / P1，新增兼容性回归：部分原有 SQL 无法匹配参数。** `DataPermissionParameterContext.apply` 使用的 deparser 对 TOP 和 Oracle 层级表达式未通过参数 visitor 完整遍历；权限新增绑定值后，`TOP (?)`、`START WITH parent_id = ? CONNECT BY ...` 触发“无法完整匹配数据权限 SQL 的参数顺序”。H2 MSSQLServer 模式真实执行确认旧实现返回 1 行、新实现失败；Oracle 仅完成改写复现，未运行 Oracle 数据库。应补全参数遍历并保留完整消费校验，不能通过删除校验规避。复现：`TopQueryProbe.java/.log`、`SqlCompatibilityProbe.java`、`sql-probe.log`。
3. **Important / P1，新增启动回归：自定义 JwtEncoder 无法继续依赖框架 JWKSource。** `JwtConfiguration.jwkSource` 新增同时检查 JwtEncoder 缺失，导致声明 `@Bean JwtEncoder customEncoder(JWKSource<SecurityContext> source)` 时跳过所需 JWKSource。相同 ApplicationContextRunner 配置在 HEAD 原类启动成功，在当前类报缺少 JWKSource。应保留该依赖关系的兼容性并补回归测试。复现：`CustomEncoderCompatibilityProbe.java/.log`、`CustomEncoderBaseline.log`。

复现关键输出：

```text
LEGITIMATE_B_CACHED_SUB=appB
REAL_VERIFIED_A_SUB=appA
A_SET_INVOKER=false
JWT_SUB_FORWARDED_AFTER_A=appB
BASELINE_TOP_ACTUAL_RESULT=1
CURRENT_TOP=FAIL:无法完整匹配数据权限 SQL 的参数顺序
CUSTOM_ENCODER_REUSING_JWK_STARTS=false
CUSTOM_ENCODER_REUSING_JWK_STARTS=true
```

最后两行分别为当前实现与 HEAD 基线。探针在临时目录执行，没有作为已修复回归测试加入仓库。

## 实际验证

| 范围 | 命令或证据 | 结果及边界 |
| --- | --- | --- |
| 全仓单元测试 | `mvn test` | 375 tests，0 failures/errors/skips；`/tmp/macula-compat-full-test.log` |
| 数据权限 IT | `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am verify -Dtest=NoCompatibilityUnitSelected -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=DataPermissionDatabaseIT,MyDataPermissionMysqlIT -Dfailsafe.failIfNoSpecifiedTests=false` | H2 IT 通过，内部使用五种兼容模式；真实 MySQL 1 项因缺少独立 URL 跳过；`/tmp/macula-compat-it.log` |
| 网关真实 HTTP 加解密链 | `mvn -pl macula-boot-examples/macula-example-alibaba-gateway -am verify -Dtest=NoCompatibilityUnitSelected -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=CryptoDemoIT,RmOpaqueTokenCallbackIT -Dfailsafe.failIfNoSpecifiedTests=false` | CryptoDemoIT 7 项通过，实际本机 HTTP 与 Node 协议适配；Redis 回调 IT 1 项环境缺失跳过；`/tmp/macula-compat-http-it-approved.log` |
| 代码规范 | `mvn -N checkstyle:check -Dcheckstyle.includes="$(cat /tmp/macula-compat-java-includes)"`，`git diff --check` | 28 个变更 Java 文件零违规，差异格式通过 |
| 旧序列化兼容性 | RedisTemplate 默认 JDK 序列化 principal 往返探针 | 往返成功，exp 保持 Instant；未连接真实 Redis |
| 可选依赖 | 移除 Spring Security JAR 后运行 MP 工具类探针 | ROOT=false、scope=9、tenant=1，无类加载异常 |
| 密钥暴露检查 | 默认 JWKSet 公共表示探针 | 不包含私钥字段；不代表公开内置签名材料可用于生产 |

关键构建输出：

```text
[INFO] BUILD SUCCESS
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.603 s -- in dev.macula.boot.starter.mp.test.DataPermissionDatabaseIT
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.397 s -- in dev.macula.example.gateway.crypto.CryptoDemoIT
[INFO] You have 0 Checkstyle violations.
```

HTTP IT 初次受沙箱限制无法监听回环随机端口（SocketException: Operation not permitted），未进入业务断言；获得自动审核许可后同命令重跑通过，不能把初次环境失败认定为代码缺陷。

## 需求证据与剩余边界

- SQL 值参数化、OR 分组、原参数交织、部门同 SQL 子查询：现有 handler/interceptor 单测及 H2 IT 覆盖；新增 TOP/层级语法复现证明跨数据库覆盖仍不完整，不再声称支持所有数据库。
- ROOT 租户切换与上下文清理：MyBatisPlusAutoConfigurationTest、SecurityUtilsTest、TenantFilterTest；本轮包含在全仓单元测试。
- JWT 配置化和旧默认材料：GatewayPropertiesTest、JwtConfigurationTest；现有自定义 encoder 测试未覆盖依赖框架 source 的组合，发现上述回归。
- opaque 缓存到期与线程调度：CachingReactiveOpaqueTokenIntrospectorTest；默认序列化往返补充检查通过，真实 Redis 集成仍缺证据。
- BodyGlobalFilter：8 个回归单测保留原分支、非 null Object 缓存和链构造时机；CryptoDemoIT 对真实 HTTP 过滤链补充 7 项验证。新聚合上限默认 1 MiB，属于需要部署确认的行为变化。
- 敏感日志：CryptoGlobalFilterTest；另有旧 `CryptoGlobalFilter` 将异常 getMessage 返回客户端的条件风险，探针确认自定义 CryptoService 异常文本会回传，但没有证据表明默认实现实际泄露私钥。
- 继续保留用户明确接受或暂缓的设计：DEFAULT/缺失 dataScope 策略、旧 JWT 默认材料、@Inner、invoker。它们不应被本轮测试成功解释为无风险。
- Secret 模式需要显式至少 32 字节 HMAC secret、权限列名限制为简单标识符，以及手动装配原生 DataPermissionInterceptor 的迁移要求，仍需按模块 README 检查使用方。
- AGENTS.md、CLAUDE.md、REVIEW.md、.agents、.codex 等受保护配置没有变更，本轮无适用 eval。未运行全仓外部服务 IT、真实厂商数据库全矩阵或完整依赖 CVE 扫描，也没有攻击在线系统。

下一步先处理上述三项确认问题并加入可复现回归测试，再重跑受影响验证；本记录不批准进入 PR 或部署。

## 授权修复后的验证

Rain 逐项确认解释后回复“OK，统一修复一下”。上述三项已修复，新增回归先在原实现上复现失败，再验证补丁；只更新已授权的相关代码、测试、README 和 SDLC 记录。

- SQL：补齐 TOP 与 Oracle 层级表达式参数访问，并保留 PRIOR 左右位置、NOCYCLE、子句顺序、TOP 选项及原 Oracle 外连接标记。完整消费校验继续生效，没有额外 JDBC 查询。
- JWT 缓存：v2 长度前缀字段绑定认证来源、验签应用、租户、最终主体、issuer、jti；HMAC 原应用仅存在服务端 exchange 属性，不新增 JWT claims、不改变 invoker。旧派生键不回读、不操作实际 Redis；OAuth 回调继续调用公共 buildKey，opaque 缓存格式不变。
- JWT 装配：JWKSource 与 JwtEncoder 分别退让。测试实际验证自定义 encoder 依赖框架 source 启动、签名及验签。

红状态证据：`/tmp/macula-three-fixes-red.log` 中两个 SQL 回归失败；`/tmp/macula-three-fixes-gateway-red.log` 中自定义 encoder 缺少 source、A 收到 signed-1（B）而非 signed-2（A）。首次 SQL 补丁的回归又发现默认 deparser 丢弃 PRIOR，已修正后通过，未通过放宽断言规避。

实际完成的构建检查（属于修复过程证据，没有自批准后续阶段）：

| 命令 | 结果 |
| --- | --- |
| `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus,macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test` | 148 单元测试，0 failures/errors/skips；`/tmp/macula-three-fixes-modules.log` |
| `mvn test` | 380 单元测试，0 failures/errors/skips；`/tmp/macula-three-fixes-all.log` |
| `mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus,macula-boot-examples/macula-example-alibaba-gateway -am verify -Dtest=NoCompatibilityUnitSelected -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=DataPermissionDatabaseIT,MyDataPermissionMysqlIT,CryptoDemoIT,RmOpaqueTokenCallbackIT -Dfailsafe.failIfNoSpecifiedTests=false` | H2 五模式含真实 TOP 执行通过；HTTP 7 项通过；真实 MySQL、Redis 回调各 1 项环境缺失跳过；`/tmp/macula-three-fixes-it.log` |
| `mvn -N checkstyle:check -Dcheckstyle.includes="$(cat /tmp/macula-three-fixes-includes)"`、`git diff --check` | 33 个变更 Java 文件零违规，差异格式通过；`/tmp/macula-three-fixes-style.log` |

补丁对原探针的复验输出（`/tmp/macula-compat-review/*-fixed.log`）：

```text
LEGITIMATE_B_CACHED_SUB=appB
REAL_VERIFIED_A_SUB=appA
A_SET_INVOKER=false
JWT_SUB_FORWARDED_AFTER_A=appA
CUSTOM_ENCODER_REUSING_JWK_STARTS=true
BASELINE=true CURRENT=OK SQL=SELECT TOP (?) id FROM t WHERE active = ? PARAMS=3
BASELINE=true CURRENT=OK SQL=SELECT id FROM t START WITH parent_id = ? CONNECT BY PRIOR id = parent_id PARAMS=2
```

仓库回归还覆盖相同 tokenId、同一 invoker 的不同验签应用隔离、同身份复用、认证来源与租户隔离、字段分隔符碰撞及遗留验签属性清理。SQL 的 TOP/PERCENT/WITH TIES 和层级顺序/NOCYCLE/PRIOR 均断言参数序列与保留语法。

结论：上述三项确认问题的回归已通过。外部数据库、真实 Redis 与全量外部服务 IT 的验证边界依旧存在，不能声明全部数据库兼容或所有漏洞已排除。部署须升级全部网关实例，旧实例仍保留旧缓存风险。没有改变 @Inner、缺失 dataScope、内置默认签名材料等已接受设计；没有提交、推送、部署或实际缓存清理。

## 默认值和文档纠正
- Rain 指定请求体默认上限改为 10 MiB；GatewayProperties 与无参 BodyGlobalFilter 入口保持一致，显式配置覆盖及原过滤器流程不变。历史 1 MiB 记录描述修订前版本。
- JWT 默认材料和环境变量说明集中保留 Gateway Starter README；示例及 Archetype 六个 README/YAML 文件中已无该重复说明。
- `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=GatewayPropertiesTest,BodyGlobalFilterTest -Dsurefire.failIfNoSpecifiedTests=false`：11 项通过，0 failures/errors/skips，日志 `/tmp/macula-body-default-10m-test.log`。
- 三个受影响 Java 文件 Checkstyle 零违规、git diff --check 通过，日志 `/tmp/macula-body-default-10m-style.log`。本轮仅运行相关回归，没有把此前全仓 380 项结果称为本次重新执行。

## 最新状态：按用户要求恢复原 JWT 缓存
- Rain 要求先恢复 AddJwtGlobalFilter 原版本。v2 缓存实现及其配套 HMAC 内部属性、常量和两项测试文件改动已撤销，五个文件与 HEAD 一致，文档同步删除 v2 说明。
- 前文“JWT_SUB_FORWARDED_AFTER_A=appA”和 v2 隔离通过的结果仅对应已撤销实现；当前同租户 tokenId/jti 与主体缓存键碰撞仍未解决。其余 SQL、JWT Bean 装配和 10 MiB 默认值修复保留。
- 恢复后运行 `mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=AddJwtGlobalFilterTest,KongApiGlobalFilterTest,RmOpaqueTokenEndpointFilterTest -Dsurefire.failIfNoSpecifiedTests=false` 成功；日志 `/tmp/macula-restore-jwt-cache-test.log`。五个恢复文件差异为空，git diff --check 通过。没有将既有测试成功视为缓存漏洞已解决。

## 最新状态：用户选择 id: / u: 简单分类
- Rain 明确应用之间使用各网关独立缓存，选择在 prefix 后增加 id: 或 u:。当前实现仅修改 buildKey 的两个返回分支及注释，保留 API Key 和租户段，不恢复 v2、内部应用属性或摘要方案；保留用户已有 tenant 变量更名。
- 相同值的 JWT_ID 与 name 回归先在旧实现失败，添加类型段后通过；12 项 AddJwtGlobalFilterTest、RmOpaqueTokenEndpointFilterTest 全部通过（0 failures/errors/skips）。命令：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=AddJwtGlobalFilterTest,RmOpaqueTokenEndpointFilterTest -Dsurefire.failIfNoSpecifiedTests=false`，日志 `/tmp/macula-jwt-key-prefix-test.log`。
- 两个变更 Java 文件 Checkstyle 零违规，git diff --check 通过；日志 `/tmp/macula-jwt-key-prefix-style.log`。原跨类型碰撞已修复；应用间隔离依据用户给定部署边界，本次没有额外证明外部 Redis 隔离配置。未运行全仓或真实 Redis IT，未清理实际缓存。

## 恢复后实际验证
- 主线 clean test：MyBatisPlusAutoConfigurationTest 3、SecurityUtilsTest 1，全通过；日志 /tmp/macula-original-handler-restore-main.log。
- 两分支 Handler 均与各自任务起始基线逐字节一致；新增 SQL 类与引用已移除。当前变更文件 Checkstyle 零违规，git diff --check 通过。没有运行新的 SQL 修复验证，因为当前没有 SQL 修复实现。
