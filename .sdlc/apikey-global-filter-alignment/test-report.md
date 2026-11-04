# API Key / HMAC 认证链路验证报告

日期：2026-11-04。用户已明确确认实现完成。验证对象为基线 `5ff9b5c8` 上当前未提交的工作区改动。

状态：**Verification is green**。当前配置下全仓单元及集成验证已完成；既有外部服务条件跳过和 Redis 版本差异详列如下，不等同于真实 IAM 或部署验收。

## 审查修复及回归验证

第一轮独立审查及复核发现 token 清理入口既有 DEBUG 日志会泄漏请求体、Header 和非法 JSON 片段。新增日志捕获测试后先复现失败（1 项失败）；删除原文日志、解析异常仅输出类型后，目标类 6 项全部通过。再次执行完整 `mvn verify`，下方结果为修复后的最新证据。Checkstyle 与 `git diff --check` 通过。

## 完整集成验证（最新）

命令：在隔离临时副本中执行 `mvn verify`，退出码 0。

```text
[INFO] BUILD SUCCESS
[INFO] Total time:  01:19 min
```

解析本次新生成的 XML 报告：

| 类型 | 套件 | 测试条目 | 失败 | 错误 | 跳过 |
| --- | --- | --- | --- | --- | --- |
| Surefire | 78 | 310 | 0 | 0 | 0 |
| Failsafe | 18 | 50 | 0 | 0 | 3 |

Failsafe 实际执行并通过 47 项，既有条件跳过为：

- KafkaProducerIT#testSend：需要启动 Kafka 服务。
- OrderServiceIT#testSendTxMsg：需要启动 RocketMQ 服务。
- TinyIdClientIT#testNextId：未设置 TINYID_E2E，需要隔离 Gateway/TinyID Server 及测试应用凭据。

没有新增跳过、禁用测试或改变测试断言。

### 隔离方式及证据边界

- 临时副本：`/private/tmp/macula-apikey-verify-lbqa61wb/repo`。复制当前已跟踪及未忽略的工作区文件，排除已删除文件和 Git 元数据。
- 578 个 Java/POM 文件的 SHA-256 在复制后、测试后均与原工作区一致，漂移 0；生产代码和测试代码未为验证而修改。
- 仅在临时副本的 5 个 test resources 中把 Redis 端口 6379 替换为随机 localhost 端口；涉及 Redis、Cache、Idempotent、LeaderElection 的测试 YAML。
- 临时 Redis 为本机 Redis 8.10.2、仅监听 127.0.0.1、关闭持久化。第二轮测试后已核对专用 PID/随机端口并发送 `SHUTDOWN NOSAVE` 成功退出，未操作原有 6379 服务。
- CI 使用 Redis 7，因此本次证据包含真实 Redis 客户端集成，但不声称与 CI Redis 7 环境完全等价；真实 IAM、Gateway/TinyID 外部 E2E、Kafka/RocketMQ 仍无新证据。
- 原始日志：`/private/tmp/apikey-final-verify-round2.log`。临时目录包含 source-manifest.json、fixture-overrides.json 和 Surefire/Failsafe 汇总，未加入仓库。

保护配置的 eval 状态不变：AGENTS.md 有规则更新，未修改 REVIEW.md、技能、hook、reviewer；未发现适用 eval 套件，不虚构 eval 结果。

下方内容为此前验证过程，旧的 Redis 环境阻塞已通过上述隔离副本方式解决。

## 最新用户修改后的复验

用户再次表示“我改好了”后，已按当前实现同步 TenantUtilsTest 的应用 Map 参数及认证租户常量引用，保留已有负面断言。

第一次模块验证真实结果：

```text
[ERROR] Tests run: 60, Failures: 6, Errors: 0, Skipped: 0
Status expected:<200 OK> but was:<403 FORBIDDEN>
Status expected:<400 BAD_REQUEST> but was:<200 OK>
```

发现并修复两个回归：API Key GlobalFilter 缺少非 API Key 分流，导致合法 OAuth/HMAC 被拒；resolver 简化后不再拒绝重复、冲突和非法凭据。保留用户的 validate(exchange)、租户应用 Map 接口及简洁代码结构，只补回已接受的认证边界。补充 API Key permitUrls 允许/拒绝测试，关闭 API Key 时其他请求继续放行的验证，并同步 README。

最终复验命令：`mvn test`，退出码 0，26 个模块汇总共 **309 项**，失败 0、错误 0、跳过 0；Gateway 61 项。

真实输出节选：

```text
[INFO] Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  02:10 min
```

第一行为 Gateway 模块汇总；309 为日志各模块汇总之和，不是虚构的 Maven 单行输出。全部现存新增/修改 Java 文件运行 Checkstyle：

```text
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```

`git diff --check` 退出码 0。最新日志位于 `/private/tmp/apikey-user-final-workspace.log`、`/private/tmp/apikey-user-final-checkstyle.log`；失败复现日志为 `/private/tmp/apikey-user-final-test.log`。

完整 mvn verify 的隔离 Redis 前提仍未解决，本次未连接或修改已有 Redis；不声明全量集成验证完成。下方保留此前验证记录以区分历史证据。

## 此前实际执行及输出

### 全仓单元测试

命令：`mvn test`，退出码 0。

实际输出节选：

```text
[INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  02:17 min
```

第一行为 Gateway 模块汇总。按日志中 26 个模块的汇总行累计：308 项，失败 0、错误 0、跳过 0。不是将历史报告或逐类结果重复计数。

### Checkstyle

命令：`mvn -N checkstyle:check -Dcheckstyle.includes=<当前新增和修改且仍存在的 Java 文件路径列表>`。路径列表从 `git diff --name-only` 和 `git ls-files --others --exclude-standard` 获取；退出码 0。

实际输出：

```text
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```

`git diff --check` 退出码 0，无输出。

原始运行日志位于 `/private/tmp/apikey-final-workspace-test.log` 和 `/private/tmp/apikey-final-checkstyle.log`，未加入仓库。

## 计划 Proof 对应证据

| 验证项 | 实际覆盖及结果 |
| --- | --- |
| API Key 输入边界 | ApiKeyUtilsTest：Header/Query、方法、格式及重复或冲突凭据；通过 |
| 授权分流 | ResourceServerAuthorizationManagerTest：API Key 交给后续验证、普通 OAuth 角色限制及匿名拒绝；通过 |
| API Key 认证 | ApiKeyGlobalFilterTest：密钥/字段、归属准入、业务租户回退、Redis 故障、开关、可信 principal 及下游异常边界；通过 |
| 条件装配 | ApiKeyAutoConfigurationTest：默认、关闭及用户 Bean 覆盖，API Key/HMAC Bean 位于同一配置；通过 |
| 统一 JWT | AddJwtGlobalFilterTest：可信身份、Query 清理、当前 tn 租户段和 API Key 类型段、jti 优先、链路一次及空 OAuth Authentication/principal；通过 |
| 实际过滤链组装 | ApiKeySecurityChainTest：真实 SecurityWebFilterChain 和 FilteringWebHandler，API Key 不调用 introspection、OAuth 角色限制、HMAC 正常路径及忽略路由上的认证拒绝；通过 |
| HMAC 验签与准入职责 | KongApiUtilsTest 验证纯验签及无身份发布副作用；KongApiGlobalFilterTest 验证租户、URL、签名、invoker/jti、请求体恢复、线程切换及异常边界；通过 |
| token 清理入口 | RmOpaqueTokenEndpointFilterTest：匹配/未知/跨租户、URL 拒绝、错误签名及显式关闭 HMAC；拒绝先于 token 缓存操作；通过 |
| 共享租户逻辑 | TenantUtilsTest：最长前缀、大小写、未知网关、归属与认证租户分离、回退及非法 ID；通过 |

身份验证中的 Redis、IAM introspector、JWT encoder 和下游使用模拟对象。异常响应脱敏有断言；日志未新增捕获断言，不将源码检查描述为运行时日志证明。无真实 API Key/HMAC 端到端部署或第三方认证证据。

## 配置与 eval

本轮修改了 AGENTS.md 的注释与常量规则。未修改 CLAUDE.md、REVIEW.md、技能、hook 或 reviewer 定义；未发现项目 eval 套件，未虚构或新增 eval 结果。

## 未完成的集成验证

CI 的 `.github/workflows/verification.yml` 执行 `mvn verify`，并为测试提供 Redis 7。当前只运行 Surefire，未运行 Failsafe。

本地环境检查结果：

```text
PORT_OCCUPIED: existing Redis will not be used or changed
```

Redis IT 的 application.yml 固定访问本机 6379，LeaderElectionIT 也从 redisson.yml 直接读取该地址。这些测试会写入 `key`、`user:*` 或锁键，不能把当前已有服务当作隔离测试实例。未对已有服务执行写入、清理或停机操作。

此前剩余条件已通过上方隔离副本完整 mvn verify 解决；此段保留历史背景，不代表当前阻塞。

## 工作区状态

已记录用户的实现完成确认；intent、spec 和 plan 的接受状态保留。代码与 SDLC 文档均未提交、推送、合并或部署，接受状态不等于 Git 提交或后续门禁批准。
