# 验证记录

## Build（2026-11-18）
- 环境：Tencent Kona OpenJDK 17.0.17。
- 命令：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test-compile -DskipTests`。
- 结果：退出码 0，BUILD SUCCESS；目标模块及 Reactor 依赖编译完成，新增测试源码编译通过。日志：`/tmp/gateway-cors-build.log`。
- 编译警告涉及既有处理器选项、弃用 API、既有测试的 varargs/unchecked；未出现编译错误。
- `git diff --check`：退出码 0。
- 静态检查：四个本地 Origin、allow-credentials=false、移除来源通配符合设计；用户 defaultUrlRequireCheck=true、内省器 assert 及 Query Token 设置保留。

## Test（2026-11-18）
用户明确确认实现完整后进入 Test。

### 定向测试
命令：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=CorsSecurityWebFilterChainTest,ProtectUrlsEndpointFilterTest,CorsConditionTest -Dsurefire.failIfNoSpecifiedTests=false`。

首轮退出码 1，实际输出：
```text
Tests run: 20, Failures: 0, Errors: 4, Skipped: 0
IllegalStateException: No content was written nor was setComplete() called on this response.
```
原因是直接使用 MockServerWebExchange 时，Security CorsWebFilter 返回 Mono.empty() 后没有外层 HTTP 适配器自动完成响应。测试辅助方法现模拟该完成行为，不改生产代码、不放宽 CORS 断言。

重跑退出码 0，日志 `/tmp/gateway-cors-focused-test.log`，实际输出：
```text
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
其中安全链 11 项、特殊端点 7 项、既有配置前缀条件 2 项。

### Gateway Starter 与依赖回归
命令：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test`。
退出码 0，日志 `/tmp/gateway-cors-module-test.log`。实际模块汇总输出：
```text
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
Tests run: 106, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
分别为 Commons、Redis、Gateway，合计 145 项，0 失败、0 错误、0 跳过。

### 规格与证明映射
- R1：允许来源的实际成功、无效 Token 与缺失凭据请求均有唯一跨域头；原成功/401 状态、错误正文或 Bearer challenge 保留。
- R2：允许预检提前结束；非法来源/方法/请求头拒绝，实际非法来源拒绝；未调用模拟内省器及业务链。
- R3：两个保护配置端点覆盖同源/允许跨域正文、拒绝来源、合法/非法预检及无配置源预检；拒绝和预检不读取配置或公钥。
- R4：无配置源、空配置源、有效源的装配与同源 Bearer 请求均通过；不产生默认跨域允许头。
- R5：静态核对示例四个 Origin、allow-credentials=false 与两处 README；保留既有方法/请求头兼容性。
- R6：保留 CSRF 禁用、Query Token、用户 defaultUrlRequireCheck=true 及内省器断言；git diff --check 通过。
- 响应头去重：模拟下游 HandlerMapping 再用同源配置处理，验证 Allow-Origin 仅一个值。未进行真实 Gateway 路由或浏览器联调，不将此模拟证据等同真实联调。

### 受保护配置与 eval
未修改 CLAUDE.md、技能、hook、REVIEW.md 或 reviewer 定义；无适用的配置/reviewer eval，不编造 eval 执行结果。

### Java 规范检查
命令：`mvn -N checkstyle:check -Dcheckstyle.includes=macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/security/ResourceServerConfiguration.java,macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/ProtectUrlsEndpointFilter.java,macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/security/CorsSecurityWebFilterChainTest.java,macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/ProtectUrlsEndpointFilterTest.java`。
四个文件为 ResourceServerConfiguration.java、ProtectUrlsEndpointFilter.java、CorsSecurityWebFilterChainTest.java、ProtectUrlsEndpointFilterTest.java，具体路径见 plan.md。
退出码 0，日志 `/tmp/gateway-cors-checkstyle.log`，实际输出：
```text
You have 0 Checkstyle violations.
BUILD SUCCESS
```

### 全仓单元测试
命令：`mvn test`。退出码 0，日志 `/tmp/gateway-cors-all-test.log`。54 个 Reactor 项目全部成功；按日志 28 条模块测试汇总行计算，共 526 项，0 失败、0 错误、0 跳过。实际最终输出：
```text
BUILD SUCCESS
Total time:  02:28 min
```
未运行 `mvn verify` 或真实外部服务/浏览器联调；本次证据为单元测试和模拟请求，不包含真实 Gateway 路由集成验证。

## 结论
Verification is green。计划中的定向、模块回归、全仓单元测试、改动 Java 的 Checkstyle 和 diff 检查全部通过；不包含生产部署验证。下一阶段为 sdlc-deploy 代码审查，尚未执行，不代表已批准 PR、提交或发布。

## 发布前增量验证（2026-11-18）
- 用户补充示例 RedisConfiguration 与 System Redis YAML，并明确授权推送发布。修正旧 spring.redis 前缀和两套属性的显式限定注入，补充 RedisConfigurationTest 和 README。
- 首轮新增测试在沙箱中因 Mockito JVM attach 被阻止失败；真实错误为 `Could not self-attach to current VM using external process`。改以允许 JVM attach 的本机权限运行同一测试，未改变测试断言、未连接 Redis。
- 定向命令：`mvn -pl macula-boot-examples/macula-example-alibaba-gateway -am test -Dtest=RedisConfigurationTest,CorsSecurityWebFilterChainTest,ProtectUrlsEndpointFilterTest,CorsConditionTest -Dsurefire.failIfNoSpecifiedTests=false`。退出码 0，21 项，0 失败/错误/跳过，日志 `/tmp/gateway-release-focused.log`。
- 全仓命令：`mvn test`。退出码 0，527 项，0 失败/错误/跳过，日志 `/tmp/gateway-release-all-test.log`。实际输出 `BUILD SUCCESS`，耗时 `28.298 s`。
- 对计划中六个新增/修改 Java 文件执行 `mvn -N checkstyle:check -Dcheckstyle.includes=<六个Java相对路径>`，退出码 0，实际输出 `You have 0 Checkstyle violations.` 与 `BUILD SUCCESS`，日志 `/tmp/gateway-release-checkstyle.log`。
- 最新 Verification is green；仅单元测试和模拟装配证据，真实应用启动与外部服务最终以用户联调及 CI 为准。
