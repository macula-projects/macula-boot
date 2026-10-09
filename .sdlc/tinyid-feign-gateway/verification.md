# TinyID Feign 网关接入验证报告

## 2026-10-09 最新修订复验（用户 YES 确认进入测试）

本节覆盖下方所有“尚未执行”的历史状态。Verification is green（本任务必需验证完成；全仓仍有既有 Kafka/RocketMQ 两项显式禁用的外部服务测试）。

| 实际命令 | 结果 |
| --- | --- |
| Boot：`mvn -B -ntp -pl macula-boot-starters/macula-boot-starter-tinyid -am verify` | exit 0，TinyID 15 单测 + HTTP IT 通过，外部 TinyIdClientIT 当时跳过 |
| Boot：`mvn -B -ntp -pl macula-boot-starters/macula-boot-starter-tinyid -am install -Pdeploy -DskipTests=true -Dgpg.skip=true` | exit 0，仅本地安装给 Cloud 使用，不是测试证据 |
| Cloud：`mvn -B -ntp -pl macula-cloud-tinyid,macula-cloud-gateway -am verify -Plocal` | exit 0 |
| Boot：`mvn -B -ntp clean verify` | exit 0；268 tests / 0 failures / 0 errors / 3 skipped |
| Cloud：`mvn -B -ntp clean verify -Plocal` | exit 0；52 tests / 0 failures / 0 errors / 0 skipped，含双 MySQL Testcontainers |
| Admin：`npm ci --no-audit --no-fund`、`npm run test:unit -- --run` | 安装 exit 0；最终 Vitest exit 0，10 passed |
| Admin：`npm run build`、`npm run test:e2e:ci` | exit 0；Cypress 全部 9 passed，其中 TinyID 6 项，接口为 mock |
| Boot：`TINYID_E2E=true MACULA_CLOUD_ENDPOINT=http://127.0.0.1:19000 MACULA_CLOUD_APP_KEY=<隔离测试应用> MACULA_CLOUD_SECRET_KEY=<隔离测试密钥> mvn -B -ntp verify` | exit 0；268 tests / 0 failures / 0 errors / 2 skipped，TinyIdClientIT 实际通过 |
| Boot：`mvn -B -ntp -N checkstyle:check '-Dcheckstyle.includes=macula-boot-starters/macula-boot-starter-tinyid/src/main/java/**/*.java,macula-boot-starters/macula-boot-starter-tinyid/src/test/java/**/*.java'` | exit 0，0 violations |
| 两仓：`git diff --check` | exit 0 |

真实输出摘录（总数按测试类输出汇总，未重复计算 Maven 汇总行）：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 ... TinyIdClientIT
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 ... TinyIdGatewayIT
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 ... TinyIdSegmentSecurityIT
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 ... TinyIdExceptionIT
BUILD SUCCESS
Test Files  2 passed (2)
Tests  10 passed (10)
All specs passed! ... 9 9
You have 0 Checkstyle violations.
gateway anonymous HTTP 401
server anonymous HTTP 401
invalid signature HTTP 403
expired signature HTTP 403
```

### 证明项与边界

- Starter：条件装配与自定义 Bean 退让、MP optional 与表名_字段名映射、五字段解析、HMAC/无 token 请求、缓存并发与错误码/cause 保留均通过。
- Server：四个接口无专属 token 的有效 JWT 请求通过；匿名全部 401，伪造/过期 JWT 拒绝；批量上限、空 bizType、业务异常 ID503/ID504、统一 JSON/文本响应及 Feign 包装差异通过。管理接口单层 Result 与既有权限测试通过。
- Gateway：精确覆盖四个发号路由、StripPrefix、共享 HMAC/JWT、错误/过期签名、URL 拒绝及下游失败测试通过。不额外增加 HMAC 与个人 Token 的区分，该边界仍按用户决定留待统一处理。
- 前端：success/data 解包、业务失败保持表单/不提示成功、传输错误释放 loading、创建返回 token 和删除失败由 Vitest/Cypress 覆盖。Cypress 是模拟后端浏览器测试，不是管理端真实后端 E2E。
- 真实发号：本次最新 JAR 在隔离 Gateway 19000、Server 19082 运行，临时 MySQL 19306 / Redis 19379；真实签名、JWT/JWKS、数据库号段及 Starter 生成 251 个唯一正数 ID 成功。四个 Server 发号入口额外逐一实测匿名 401。使用静态路由替代 Nacos 服务发现，不覆盖生产 Nacos 发布或个人身份认证流程。
- 未改 CLAUDE.md、技能或 hook，不触发配置 eval；修改过运行配置和 POM，已纳入全量构建。未提交、推送或部署，原 Cloud 工作区保持不动。
- 临时 Java 服务及两个 --rm 容器已停止，临时数据库/Redis 测试数据已随容器删除；未操作已有数据库。

### 本轮失败与修正

- 初次前端测试缺少依赖：`vitest: command not found`；在独立工作树执行 npm ci 后可运行，未修改锁文件。
- 组件测试首次执行：`No "ElAlert" export is defined on the "element-plus" mock`，以及 `wrapper.vm.open is not a function`。改为局部 mock 保留 Element Plus 组件导出，并通过 Vue exposed API 调用 open；全量 Vitest 重跑 10 项通过，生产组件未因此改动。
- 隔离启动时先纠正 JAR 文件名；随后发现单独覆盖 routes[1].uri 无法绑定路由列表，改为测试进程中完整提供 routes[0] 的路径/StripPrefix/静态 URI 后启动成功。未修改生产路由配置。
- 日志：`/tmp/tinyid-current-boot-target.log`、`install.log`（同前缀）、`boot-full.log`、`boot-live.log`、`cloud-target.log`、`cloud-full.log`、`admin-unit.log`、`admin-build.log`、`admin-e2e.log`、`checkstyle.log`、`gateway.log`、`server.log`、`probe.log` 均使用 `/tmp/tinyid-current-` 前缀。

## 以下为历史记录

最新已接受修订：四个发号接口均移除 TinyID token 参数及校验，只保留 bizType 和适用的 batchSize；Server 删除全部发号匿名白名单，网关精确路由四个接口并沿用共享认证，不新增 HMAC/个人 Token 区分。管理端历史 Token 数据及功能保留。本条覆盖下文旧 token 兼容说明，实施依据为 Boot 仓 plan-remove-issuing-token.md。
本轮已同步 Controller、Service、配置、README 与测试用例；正式测试尚未执行，旧验证结果不代表本轮通过。

最新变更：按用户指示移除 TinyID 专属 HMAC-only 过滤器与对应测试，保留共享 HMAC/JWT 及路由用例。本次仅静态差异检查，未运行测试；不再以“拒绝个人 Token”作为 TinyID 的验收条件。

最新待验范围还包括：Controller/Service 分层、旧 token 接口统一失败、管理 Result 响应及 Vue success/data 消费、Vitest/Cypress 回归用例。未运行新版本测试。

补充待验范围：Server 的 Macula Web 异常链路、ID503/ID504、文本/JSON 协商、旧接口及管理响应兼容、客户端保留远程错误码和 cause。代码及测试用例已更新，未执行本次验证。

状态：以下为评审前版本的通过记录。新增 HMAC 入口限制及 BizException 改造已实施，尚未执行新版本测试；当前不能据此认定 Verification is green。未部署到已有环境。

## 执行结果

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| Boot 目标模块及依赖 | `mvn -B -ntp -pl macula-boot-starters/macula-boot-starter-tinyid -am verify -Dit.test=TinyIdFeignClientIT -Dfailsafe.failIfNoSpecifiedTests=false` | 成功，TinyID 11 个单测 + 1 个 HTTP IT |
| Boot 全仓 | `mvn -B -ntp clean verify` | BUILD SUCCESS；264 tests，0 failures，0 errors，3 skipped |
| Boot 最终复跑 | `mvn -B -ntp verify` | 修正测试导入后再次 BUILD SUCCESS；264 tests，0 failures，0 errors，3 skipped |
| Boot 含真实链路复跑 | `TINYID_E2E=true MACULA_CLOUD_ENDPOINT=http://127.0.0.1:19000 MACULA_CLOUD_APP_KEY=<测试应用> MACULA_CLOUD_SECRET_KEY=<临时测试密钥> mvn -B -ntp verify` | BUILD SUCCESS；264 tests，0 failures，0 errors，2 skipped；TinyIdClientIT 已执行 |
| Boot 本地安装 | `mvn -B -ntp -pl macula-boot-starters/macula-boot-starter-tinyid -am install -Pdeploy -DskipTests=true -Dgpg.skip=true` | 成功，仅为 Cloud 提供本次产物，不作为测试证据 |
| Cloud 目标模块 | `mvn -B -ntp -pl macula-cloud-tinyid,macula-cloud-gateway -am verify -Plocal -Dit.test=TinyIdSegmentSecurityIT,TinyIdGatewayIT -Dfailsafe.failIfNoSpecifiedTests=false` | BUILD SUCCESS |
| Cloud 全仓 | `mvn -B -ntp clean verify -Plocal` | BUILD SUCCESS；48 tests，0 failures，0 errors，0 skipped |
| TinyID Checkstyle | `mvn -B -ntp -N checkstyle:check '-Dcheckstyle.includes=macula-boot-starters/macula-boot-starter-tinyid/src/main/java/**/*.java,macula-boot-starters/macula-boot-starter-tinyid/src/test/java/**/*.java'` | BUILD SUCCESS |
| 两仓差异 | `git diff --check` | 通过 |

真实输出摘录：

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 ... TinyIdFeignClientIT
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 ... TinyIdGatewayIT
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 ... TinyIdSegmentSecurityIT
BUILD SUCCESS
```

数量按各测试类输出汇总，避免将 Maven 汇总行重复计数。完整日志位于：
- /tmp/tinyid-boot-verify.log
- /tmp/tinyid-boot-full-verify.log
- /tmp/tinyid-boot-final-verify.log
- /tmp/tinyid-cloud-verify.log
- /tmp/tinyid-cloud-full-verify.log
- /tmp/tinyid-checkstyle.log

## 覆盖与边界

- 条件装配：默认 Feign、自定义号段服务/工厂、无网关凭据的 Server 场景及缺少 MyBatis-Plus 依赖均通过。
- 通信：真实本地 HTTP 请求覆盖路径、参数编码、无 TinyID token、按 StripPrefix 后路径计算 HMAC、远程错误传播。
- 本地发号：缓存、预加载、并发唯一性及 delta/remainder 回归通过。
- MyBatis-Plus：映射表名与主键列名拼接、缺少元数据、参数校验、异常传播通过；安装 POM 保留 mybatis-plus-core optional=true。实现类没有 Bean 注册或组件注解。
- Server：实际生产安全过滤链与生产 JWT decoder 覆盖有效、伪造、过期及匿名请求；空参数、请求方法、旧 token 接口回归通过。
- Gateway：生产路径谓词、StripPrefix、HMAC 与 JWT 转换过滤器覆盖成功、错误/过期签名、URL 拒绝、改写路径和下游不可用。Redis 与下游为测试替身，不是完整网络联调。
- Cloud TinyID 双 MySQL Testcontainers 测试执行成功；未修改用户已有数据库。
- 首轮 Boot 的 RocketMQ、Kafka、TinyIdClientIT 三项跳过；随后将 TinyIdClientIT 改为环境变量控制，并在真实隔离环境执行通过。最终仅保留已有 RocketMQ 和 Kafka 两个无关外部服务测试跳过。
- 新版 Gateway/Server JAR 已在隔离端口真实运行，原 Docker 部署没有被替换。服务发现使用静态路由代替 Nacos；本次证据覆盖发号链路，不覆盖 Nacos 配置发布或生产部署。
- 未改 CLAUDE.md、技能或 hook，无需配置 eval。运行期配置与 POM 有变化，已执行上述全仓构建。

## 失败及修正

- 首轮默认客户端装配失败：`Type org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer not present`。增加 TinyID 运行依赖 spring-boot-http-converter 后通过。
- Gateway 新测试使用了错误构造签名；按当前依赖传入 WebFluxProperties 后通过。
- Server 新安全测试重复注册 decoder，失败信息为 `expected single matching bean but found 2: jwtDecoderBySecret,jwtDecoder`。改为使用生产 decoder 与测试密钥属性后通过。
- Checkstyle 报告 `AvoidStarImport: java.util.concurrent.*`，改为显式导入后通过。

Cloud 验证基于独立 worktree 的 d32fc5f 基线，不包含原工作区未提交的 IAM 修改。没有提交源码、推送或部署。

## 隔离环境真实链路补充

- Gateway 19000、TinyID Server 19082，均使用本次构建的 JAR；Server 启动日志确认 context path '/'。
- 新建临时 MySQL 8.4.6 和 Redis 7.4.5 容器；Flyway 在独立 tinyid_e2e 库执行 V1；仅在临时 Redis 写入测试应用认证及 URL 授权数据。
- 禁用 Nacos 配置与注册；网关使用生产相同的 Path 谓词和 StripPrefix=1，静态 URI 指向隔离 Server。真实 HMAC、JWT 签发、JWKS 获取、Server JWT 校验及数据库分配均未 mock。
- TinyIdClientIT 使用实际 Starter 单个取号再批量取号，251 个 ID 均为正数且唯一，跨越多个长度为 100 的号段。
- 同时执行 TinyIdClientIT/TinyIdFeignClientIT 时发现外部环境凭据干扰本地测试，后者增加临时系统属性隔离；定向和全仓复跑均成功。

真实输出：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 ... TinyIdClientIT
BUILD SUCCESS
gateway anonymous HTTP 401
server anonymous HTTP 401
invalid signature HTTP 403
expired signature HTTP 403
```

拒绝路径检查前后 test 业务均为 max_id=4101、version=36，没有消耗号段。
最终全仓汇总为 tests=264 failures=0 errors=0 skipped=2；Checkstyle 再次通过。
完整日志与临时探测源码保存在 /tmp/tinyid-e2e.JrRPQi/（client.log、boot-full.log、gateway.log、server.log、checkstyle.log）。
测试后已停止两个 Java 进程及两个 --rm 容器，确认 19000/19082 无监听，临时数据库/Redis 数据随容器移除；已有环境不受影响。
