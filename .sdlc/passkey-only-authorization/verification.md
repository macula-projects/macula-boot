# Verification: Passkey 请求参数资源凭证校验
Status: main-green; 5x-scoped-green-with-known-tsf-baseline

## 范围与环境
- 主线分支：`codex/passkey-only-authorization`，Tencent JDK 17.0.17，Spring Boot 4.0.8。
- 5.x 分支：`codex/passkey-only-authorization-5x`，真实 Tencent JDK 1.8.0_472，Spring Boot 2.7.18。
- 用户已确认参数级 Controller 校验、默认关闭拒绝受保护参数、保留 TSF/SnakeYAML 既有配置；不再交付方法级 Advisor。
- 本文记录本次真实运行结果；先前方法级中间实现的编译不作为此方案证据。

## 需求到证据
1. 参数级 API、标量与 JavaBean 路径：PasskeyArgumentResolverTest 验证 String/整数、orderId/order.id、原对象返回及原解析器只执行一次；无注解参数不接管。
2. 安全拒绝：单测覆盖 null/空值、缺属性、中间 null、class/SpEL/索引/方法调用、Map/集合/小数、缺少身份/匿名/未认证、ADMIN/ROOT、禁用/缺服务/缺凭证/身份及编号不符。
3. MVC 接入：PasskeyMvcTest 通过真实 MVC 验证 PathVariable、RequestParam、RequestBody、Valid、JSON 对象只实例化一次、嵌套属性、自定义参数解析器及未标注请求。DTO passkey 字段不能作为凭证，缺少 Header/Query 时拒绝。
4. 操作与结果鉴权：有效凭证仍受 PreAuthorize/PMS 和 PostAuthorize 约束；前置拒绝 Controller 计数为零，后置拒绝查询已执行但详情不返回。标准 PostAuthorize 沿用既有异常映射。
5. 生命周期与开关：处理器测试证明原列表保留、安装幂等、不包含自身委托、缺少原解析器明确失败；自动配置验证延迟装配、用户处理器/服务/Clock 回退、默认/显式禁用和非法配置。
6. 凭证兼容：两分支服务测试使用同一个固定 golden token，签发值与验证均通过；请求解析、Commons 和 Web 回归保留。
7. 实现边界：旧 CheckOwner/Advisor 源码已删除；Commons/Web 生产代码、父 POM、TSF 配置、现有方法安全顺序及 imports 无改动。新增实现含中文职责与关键设计说明。

## 实际命令与结果
以下 Maven 命令分别在对应工作树使用上述 JAVA_HOME 执行；汇总数字来自各测试类的 Surefire/Failsafe 输出，不将模块汇总重复相加。

### 首轮定向测试与修正
命令：
`mvn -pl macula-boot-starters/macula-boot-starter-security -am test -Dtest=PasskeyArgumentResolverTest,SecurityAutoConfigurationTest,PasskeyMvcTest -Dsurefire.failIfNoSpecifiedTests=false`

首轮主线 22、5.x 21 个测试各有一个断言失败：
```text
Status expected:<400> but was:<500>
```
失败源于新测试错误假定 Valid 返回 400。现有 ControllerExceptionAdvice 的 MethodArgumentNotValidException 处理明确使用 HTTP 500/A0400。仅修正测试并断言原异常类型，未改生产异常处理。随后原命令复跑：
```text
主线: Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
5.x: Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
两次复跑退出码均为 0。

### 两分支目标模块 clean verify
命令：
`mvn -pl macula-boot-starters/macula-boot-starter-security,macula-boot-starters/macula-boot-starter-web -am clean verify`

主线退出码 0，86 个单元测试及 1 个 WebAutoConfigurationIT，共 87 个，0 failures/errors/skipped。模块原始输出包括：
```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

5.x 退出码 0，Commons 5 + Web 8 + Security 28 = 41 个单元测试，0 failures/errors/skipped；该切片没有额外 Failsafe 用例：
```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 主线全仓
- `mvn test`：退出码 0，72 个测试类，274 个单元测试，0 failures/errors/skipped。
- `mvn clean verify`：退出码 0，90 个测试类，324 个记录，0 failures、0 errors、3 skipped。包括单元测试 274 个和集成测试 50 个记录（47 执行通过、3 跳过）。
- 集成前只读确认文档要求的本地 Redis 返回 PONG；Redis、缓存、选主、H2 等适用集成测试实际执行通过。
- 原有跳过项：OrderServiceIT（已有 Disabled，要求 RocketMQ）、KafkaProducerIT（已有 Disabled，要求 Kafka）、TinyIdClientIT（已有环境条件，要求隔离 Gateway/TinyID 及应用凭据）。本次未添加禁用或跳过参数。
- 原始结果：
```text
BUILD SUCCESS
Total time:  02:08 min
Finished at: 2026-11-03T20:49:54+08:00
```
```text
BUILD SUCCESS
Total time:  01:29 min
Finished at: 2026-11-03T20:52:25+08:00
```

### 5.x 全仓基线限制
`mvn test`：退出码 1，实际执行到 Redis 共 61 个测试记录，0 failures、8 errors、0 skipped；8 errors 来自既有 Redis 测试，其他已执行的 53 个通过，后续 reactor 模块未运行。
```text
java.lang.NoSuchMethodError: org.yaml.snakeyaml.parser.ParserImpl.<init>(Lorg/yaml/snakeyaml/reader/StreamReader;Lorg/yaml/snakeyaml/LoaderOptions;)V
Tests run: 8, Failures: 0, Errors: 8, Skipped: 0
Macula Boot Starter Redis .......................... FAILURE
BUILD FAILURE
```
这与之前已确认的 TSF/SnakeYAML 基线限制一致；用户明确要求保留配置。未更改相关依赖，未为追求全仓绿灯禁用这些测试。5.x 的既有 RocketMQ 测试在全仓过程中实际连接本地服务并执行，不能将本次全仓检查笼统描述为全部 mock 验证；未做部署或生产环境验证。

## 静态检查与 eval
- 两分支真实工具链 `clean test-compile -DskipTests` 退出码均为 0。
- 使用主线版本化 Checkstyle 配置检查变更 Java（包括新增 web 包）：主线及以 checkstyle.source.root 指向 5.x 的扫描均退出码 0，原始输出均为 `You have 0 Checkstyle violations.`。
- 两分支 `git diff --check` 退出码 0，无输出。
- 未修改 AGENTS/CLAUDE、skill、hook、REVIEW 或 reviewer 定义；本次不适用配置 eval。

## 结论
主线：Verification is green，274 个单元测试与全仓 clean verify 成功，3 个既有集成用例跳过已列明。
5.x：本次涉及的 41 个测试及 clean verify 为绿；全仓受用户已确认保留的 TSF/SnakeYAML 基线限制影响，不能声称全仓为绿。测试记录与源代码边界不因此扩大。
